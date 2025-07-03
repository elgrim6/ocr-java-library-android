package mz.nedbank.ocr.core;

import mz.nedbank.ocr.engine.OcrEngine;
import mz.nedbank.ocr.extractor.MrzDataExtractor;
import mz.nedbank.ocr.extractor.InvoiceDataExtractor;
import mz.nedbank.ocr.model.IdentityDocument;
import mz.nedbank.ocr.model.InvoiceData;
import mz.nedbank.ocr.utils.ImagePreprocessor;
import net.sourceforge.tess4j.TesseractException;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main OCR processor that orchestrates image preprocessing, OCR, and data extraction
 * Now focused on identity document processing with MRZ extraction
 */
public class OcrProcessor {
    private static final Logger logger = LoggerFactory.getLogger(OcrProcessor.class);
    
    private final OcrEngine ocrEngine;
    private final MrzDataExtractor mrzExtractor;
    private final InvoiceDataExtractor invoiceExtractor; // Kept for backward compatibility
    private ImagePreprocessor.PreprocessingConfig preprocessingConfig;
    private boolean debugMode = false;
    private String debugOutputDir = "debug_output";

    /**
     * Create OCR processor with default settings
     */
    public OcrProcessor() {
        this(null, "eng");
    }

    /**
     * Create OCR processor with custom tessdata path and language
     * @param tessDataPath Path to tessdata directory
     * @param language OCR language (e.g., "eng", "fra", "deu")
     */
    public OcrProcessor(String tessDataPath, String language) {
        this.ocrEngine = new OcrEngine(tessDataPath, language);
        this.mrzExtractor = new MrzDataExtractor();
        this.invoiceExtractor = new InvoiceDataExtractor(); // Deprecated but kept
        this.preprocessingConfig = createMrzOptimizedConfig();
        
        logger.info("OCR Processor initialized for identity document processing with language: {}", language);
    }

    /**
     * Process an identity document image and extract MRZ data
     * @param file Input image file
     * @return Extracted identity document data
     * @throws Exception if processing fails
     */
    public IdentityDocument processIdentityDocument(File file) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("Input file does not exist or is null");
        }

        logger.info("Processing identity document: {}", file.getName());
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Preprocess image with MRZ-optimized settings
            Mat processedImage = ImagePreprocessor.preprocess(file, preprocessingConfig);
            logger.debug("Image preprocessing completed");

            // Save debug image if debug mode is enabled
            if (debugMode) {
                saveDebugImage(processedImage, file.getName() + "_preprocessed.png");
            }

            // Step 2: Perform OCR with settings optimized for MRZ
            configureOcrForMrz();
            String ocrText = ocrEngine.doOcr(processedImage);
            logger.debug("OCR completed, extracted {} characters", ocrText.length());

            // Save OCR text if debug mode is enabled
            if (debugMode) {
                saveDebugText(ocrText, file.getName() + "_ocr.txt");
            }

            // Step 3: Extract structured MRZ data
            IdentityDocument identityDocument = mrzExtractor.extract(ocrText);
            
            // Clean up
            processedImage.release();

            long processingTime = System.currentTimeMillis() - startTime;
            logger.info("Identity document processing completed in {} ms", processingTime);

            return identityDocument;

        } catch (TesseractException e) {
            logger.error("OCR processing failed for file: {}", file.getName(), e);
            throw new Exception("OCR processing failed", e);
        } catch (Exception e) {
            logger.error("Identity document processing failed for file: {}", file.getName(), e);
            throw new Exception("Identity document processing failed", e);
        }
    }

    /**
     * @deprecated Use processIdentityDocument() instead. Kept for backward compatibility.
     */
    @Deprecated
    public InvoiceData processInvoice(File file) throws Exception {
        logger.warn("processInvoice() is deprecated. Use processIdentityDocument() for MRZ processing.");
        return invoiceExtractor.extract(extractText(file));
    }

    /**
     * Process image and return raw OCR text
     * @param file Input image file
     * @return Raw OCR text
     * @throws Exception if processing fails
     */
    public String extractText(File file) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("Input file does not exist or is null");
        }

        logger.info("Extracting text from: {}", file.getName());

        try {
            Mat processedImage = ImagePreprocessor.preprocess(file, preprocessingConfig);
            String text = ocrEngine.doOcr(processedImage);
            processedImage.release();
            
            logger.info("Text extraction completed, {} characters extracted", text.length());
            return text;

        } catch (Exception e) {
            logger.error("Text extraction failed for file: {}", file.getName(), e);
            throw new Exception("Text extraction failed", e);
        }
    }

    /**
     * Process image without preprocessing
     * @param file Input image file
     * @return Raw OCR text
     * @throws Exception if processing fails
     */
    public String extractTextRaw(File file) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("Input file does not exist or is null");
        }

        logger.info("Extracting raw text from: {}", file.getName());

        try {
            String text = ocrEngine.doOcr(file);
            logger.info("Raw text extraction completed, {} characters extracted", text.length());
            return text;

        } catch (Exception e) {
            logger.error("Raw text extraction failed for file: {}", file.getName(), e);
            throw new Exception("Raw text extraction failed", e);
        }
    }

    /**
     * Batch process multiple identity document files
     * @param files Array of input files
     * @return Array of extracted identity document data
     */
    public IdentityDocument[] processIdentityDocumentBatch(File[] files) {
        if (files == null || files.length == 0) {
            return new IdentityDocument[0];
        }

        logger.info("Processing batch of {} identity documents", files.length);
        IdentityDocument[] results = new IdentityDocument[files.length];

        for (int i = 0; i < files.length; i++) {
            try {
                results[i] = processIdentityDocument(files[i]);
                logger.debug("Processed file {}/{}: {}", i + 1, files.length, files[i].getName());
            } catch (Exception e) {
                logger.error("Failed to process file: {}", files[i].getName(), e);
                results[i] = new IdentityDocument(); // Return empty data on failure
            }
        }

        logger.info("Batch processing completed");
        return results;
    }

    /**
     * @deprecated Use processIdentityDocumentBatch() instead
     */
    @Deprecated
    public InvoiceData[] processInvoiceBatch(File[] files) {
        logger.warn("processInvoiceBatch() is deprecated. Use processIdentityDocumentBatch() for MRZ processing.");
        if (files == null || files.length == 0) {
            return new InvoiceData[0];
        }
        
        InvoiceData[] results = new InvoiceData[files.length];
        for (int i = 0; i < files.length; i++) {
            results[i] = new InvoiceData();
        }
        return results;
    }

    /**
     * Create preprocessing configuration optimized for MRZ processing
     */
    private ImagePreprocessor.PreprocessingConfig createMrzOptimizedConfig() {
        ImagePreprocessor.PreprocessingConfig config = new ImagePreprocessor.PreprocessingConfig();
        
        // MRZ-specific optimizations
        config.setScaleFactor(3.0);              // Higher scaling for small MRZ text
        config.setEnableDenoising(true);         // Important for clean MRZ reading
        config.setEnableDeskewing(true);         // Critical for MRZ alignment
        config.setEnableContrastEnhancement(true); // Helps with faded documents
        config.setEnableMorphologicalOps(true);  // Cleans up character shapes
        config.setBlurKernelSize(1);             // Minimal blur to preserve sharp edges
        config.setMorphKernelSize(1);            // Small kernel for fine text
        
        return config;
    }

    /**
     * Configure OCR engine settings optimized for MRZ processing
     */
    private void configureOcrForMrz() {
        // Set page segmentation mode for single text block (MRZ)
        ocrEngine.setPageSegMode(6); // Uniform block of text
        
        // Use LSTM engine for better accuracy
        ocrEngine.setOcrEngineMode(1); // Neural nets LSTM engine only
    }

    /**
     * Set preprocessing configuration
     * @param config Preprocessing configuration
     */
    public void setPreprocessingConfig(ImagePreprocessor.PreprocessingConfig config) {
        this.preprocessingConfig = config;
        logger.debug("Preprocessing configuration updated");
    }

    /**
     * Get current preprocessing configuration
     * @return Current preprocessing configuration
     */
    public ImagePreprocessor.PreprocessingConfig getPreprocessingConfig() {
        return preprocessingConfig;
    }

    /**
     * Set OCR language
     * @param language Language code (e.g., "eng", "fra", "deu")
     */
    public void setLanguage(String language) {
        ocrEngine.setLanguage(language);
        logger.info("OCR language set to: {}", language);
    }

    /**
     * Enable or disable debug mode
     * @param debugMode True to enable debug mode
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        if (debugMode) {
            createDebugDirectory();
        }
        logger.info("Debug mode {}", debugMode ? "enabled" : "disabled");
    }

    /**
     * Set debug output directory
     * @param debugOutputDir Directory path for debug output
     */
    public void setDebugOutputDir(String debugOutputDir) {
        this.debugOutputDir = debugOutputDir;
        if (debugMode) {
            createDebugDirectory();
        }
    }

    /**
     * Get available OCR languages
     * @return Array of available language codes
     */
    public String[] getAvailableLanguages() {
        return ocrEngine.getAvailableLanguages();
    }

    /**
     * Create debug output directory
     */
    private void createDebugDirectory() {
        try {
            Path debugPath = Paths.get(debugOutputDir);
            if (!Files.exists(debugPath)) {
                Files.createDirectories(debugPath);
                logger.debug("Created debug directory: {}", debugOutputDir);
            }
        } catch (IOException e) {
            logger.warn("Failed to create debug directory: {}", debugOutputDir, e);
        }
    }

    /**
     * Save debug image
     */
    private void saveDebugImage(Mat image, String filename) {
        try {
            String outputPath = Paths.get(debugOutputDir, filename).toString();
            ImagePreprocessor.saveDebugImage(image, outputPath);
        } catch (Exception e) {
            logger.warn("Failed to save debug image: {}", filename, e);
        }
    }

    /**
     * Save debug text
     */
    private void saveDebugText(String text, String filename) {
        try {
            Path outputPath = Paths.get(debugOutputDir, filename);
            Files.write(outputPath, text.getBytes());
            logger.debug("Debug text saved to: {}", outputPath);
        } catch (IOException e) {
            logger.warn("Failed to save debug text: {}", filename, e);
        }
    }

    /**
     * Get processor statistics
     * @return String containing processor information
     */
    public String getProcessorInfo() {
        return String.format("OCR Processor (Identity Documents/MRZ) - Languages: %s, Debug: %s, Config: MRZ-optimized",
                           String.join(", ", getAvailableLanguages()),
                           debugMode ? "enabled" : "disabled");
    }
}

