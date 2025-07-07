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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Enhanced OCR processor with improved performance, error handling, and multi-threading support
 * Optimized for identity document processing with MRZ extraction
 */
public class OcrProcessor {
    private static final Logger logger = LoggerFactory.getLogger(OcrProcessor.class);

    // Configuration constants
    private static final int DEFAULT_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long PROCESSING_TIMEOUT_MS = 30000; // 30 seconds
    private static final String[] SUPPORTED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".tiff", ".tif", ".bmp", ".gif"};

    // Core components
    private final OcrEngine ocrEngine;
    private final MrzDataExtractor mrzExtractor;
    private final InvoiceDataExtractor invoiceExtractor;
    private final ExecutorService executorService;

    // Configuration
    private ImagePreprocessor.PreprocessingConfig preprocessingConfig;
    private ProcessingMode processingMode = ProcessingMode.IDENTITY_DOCUMENT;
    private boolean debugMode = false;
    private String debugOutputDir = "debug_output";
    private boolean enableParallelProcessing = true;
    private int maxRetryAttempts = MAX_RETRY_ATTEMPTS;

    // Performance tracking
    private final AtomicInteger totalProcessed = new AtomicInteger(0);
    private final AtomicInteger totalSuccessful = new AtomicInteger(0);
    private final AtomicInteger totalFailed = new AtomicInteger(0);
    private volatile long totalProcessingTime = 0;

    // Image validation patterns
    private static final Pattern MRZ_PATTERN = Pattern.compile(".*[A-Z0-9<]{44}.*", Pattern.DOTALL);
    private static final long MIN_FILE_SIZE = 1024; // 1KB minimum
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB maximum

    /**
     * Processing modes
     */
    public enum ProcessingMode {
        IDENTITY_DOCUMENT,
        INVOICE,
        GENERAL_TEXT,
        AUTO_DETECT
    }

    /**
     * Processing result wrapper
     */
    public static class ProcessingResult<T> {
        private final T result;
        private final boolean successful;
        private final String errorMessage;
        private final long processingTime;
        private final ImagePreprocessor.ImageQuality imageQuality;

        public ProcessingResult(T result, boolean successful, String errorMessage,
                                long processingTime, ImagePreprocessor.ImageQuality imageQuality) {
            this.result = result;
            this.successful = successful;
            this.errorMessage = errorMessage;
            this.processingTime = processingTime;
            this.imageQuality = imageQuality;
        }

        public T getResult() { return result; }
        public boolean isSuccessful() { return successful; }
        public String getErrorMessage() { return errorMessage; }
        public long getProcessingTime() { return processingTime; }
        public ImagePreprocessor.ImageQuality getImageQuality() { return imageQuality; }
    }

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
        this.invoiceExtractor = new InvoiceDataExtractor();
        this.executorService = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE);
        this.preprocessingConfig = createOptimizedConfig(ProcessingMode.IDENTITY_DOCUMENT);

        logger.info("Enhanced OCR Processor initialized with language: {}, threads: {}",
                language, DEFAULT_THREAD_POOL_SIZE);
    }

    /**
     * Process an identity document image with enhanced error handling and retries
     * @param file Input image file
     * @return Processing result with extracted identity document data
     */
    public ProcessingResult<IdentityDocument> processIdentityDocument(File file) {
        return processIdentityDocumentWithRetry(file, maxRetryAttempts);
    }

    /**
     * Process identity document with retry logic
     */
    private ProcessingResult<IdentityDocument> processIdentityDocumentWithRetry(File file, int remainingAttempts) {
        long startTime = System.currentTimeMillis();

        try {
            // Validate input file
            ValidationResult validation = validateInputFile(file);
            if (!validation.isValid()) {
                return new ProcessingResult<>(new IdentityDocument(), false,
                        validation.getErrorMessage(), System.currentTimeMillis() - startTime, null);
            }

            logger.info("Processing identity document: {} (attempt: {})",
                    file.getName(), maxRetryAttempts - remainingAttempts + 1);

            // Step 1: Preprocess image with optimized settings
            Mat processedImage = ImagePreprocessor.preprocess(file, preprocessingConfig);
            ImagePreprocessor.ImageQuality imageQuality = ImagePreprocessor.analyzeImageQuality(processedImage);

            logger.debug("Image preprocessing completed. {}", imageQuality.toString());

            // Save debug image if enabled
            if (debugMode) {
                saveDebugImage(processedImage, file.getName() + "_preprocessed.png");
            }

            // Step 2: Auto-adjust OCR settings based on image quality
            adjustOcrSettingsForQuality(imageQuality);

            // Step 3: Perform OCR with timeout
            String ocrText = performOcrWithTimeout(processedImage, PROCESSING_TIMEOUT_MS);

            if (ocrText == null || ocrText.trim().isEmpty()) {
                processedImage.release();
                throw new RuntimeException("OCR returned empty text");
            }

            logger.debug("OCR completed, extracted {} characters", ocrText.length());

            // Save OCR text if debug mode is enabled
            if (debugMode) {
                saveDebugText(ocrText, file.getName() + "_ocr.txt");
            }

            // Step 4: Validate OCR result quality
            if (!isValidMrzText(ocrText) && remainingAttempts > 1) {
                logger.warn("OCR result quality poor, retrying with different settings");
                processedImage.release();

                // Adjust preprocessing config for retry
                adjustConfigForRetry();
                return processIdentityDocumentWithRetry(file, remainingAttempts - 1);
            }

            // Step 5: Extract structured MRZ data
            IdentityDocument identityDocument = mrzExtractor.extract(ocrText);

            // Step 6: Validate extraction result
            if (!isValidExtractionResult(identityDocument) && remainingAttempts > 1) {
                logger.warn("Extraction result incomplete, retrying");
                processedImage.release();
                return processIdentityDocumentWithRetry(file, remainingAttempts - 1);
            }

            // Clean up
            processedImage.release();

            long processingTime = System.currentTimeMillis() - startTime;
            updateStatistics(true, processingTime);

            logger.info("Identity document processing completed successfully in {} ms", processingTime);
            return new ProcessingResult<>(identityDocument, true, null, processingTime, imageQuality);

        } catch (Exception e) {
            if (remainingAttempts > 1) {
                logger.warn("Processing failed, retrying. Error: {}", e.getMessage());
                return processIdentityDocumentWithRetry(file, remainingAttempts - 1);
            } else {
                logger.error("Identity document processing failed after {} attempts for file: {}",
                        maxRetryAttempts, file.getName(), e);

                long processingTime = System.currentTimeMillis() - startTime;
                updateStatistics(false, processingTime);

                return new ProcessingResult<>(new IdentityDocument(), false,
                        "Processing failed: " + e.getMessage(), processingTime, null);
            }
        }
    }

    /**
     * Enhanced batch processing with parallel execution
     * @param files Array of input files
     * @return Array of processing results
     */
    public ProcessingResult<IdentityDocument>[] processIdentityDocumentBatch(File[] files) {
        if (files == null || files.length == 0) {
            return new ProcessingResult[0];
        }

        logger.info("Processing batch of {} identity documents (parallel: {})",
                files.length, enableParallelProcessing);

        List<ProcessingResult<IdentityDocument>> results = new ArrayList<>();

        if (enableParallelProcessing && files.length > 1) {
            // Parallel processing
            List<Future<ProcessingResult<IdentityDocument>>> futures = new ArrayList<>();

            for (File file : files) {
                Future<ProcessingResult<IdentityDocument>> future = executorService.submit(() ->
                        processIdentityDocument(file));
                futures.add(future);
            }

            // Collect results
            for (int i = 0; i < futures.size(); i++) {
                try {
                    ProcessingResult<IdentityDocument> result = futures.get(i).get(PROCESSING_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    results.add(result);
                    logger.debug("Processed file {}/{}: {}", i + 1, files.length, files[i].getName());
                } catch (Exception e) {
                    logger.error("Failed to process file: {}", files[i].getName(), e);
                    results.add(new ProcessingResult<>(new IdentityDocument(), false,
                            "Batch processing failed: " + e.getMessage(), 0, null));
                }
            }
        } else {
            // Sequential processing
            for (int i = 0; i < files.length; i++) {
                ProcessingResult<IdentityDocument> result = processIdentityDocument(files[i]);
                results.add(result);
                logger.debug("Processed file {}/{}: {}", i + 1, files.length, files[i].getName());
            }
        }

        logger.info("Batch processing completed. Success: {}, Failed: {}",
                results.stream().mapToInt(r -> r.isSuccessful() ? 1 : 0).sum(),
                results.stream().mapToInt(r -> r.isSuccessful() ? 0 : 1).sum());

        return results.toArray(new ProcessingResult[0]);
    }

    /**
     * Enhanced text extraction with quality assessment
     * @param file Input image file
     * @return Processing result with extracted text
     */
    public ProcessingResult<String> extractText(File file) {
        long startTime = System.currentTimeMillis();

        try {
            ValidationResult validation = validateInputFile(file);
            if (!validation.isValid()) {
                return new ProcessingResult<>("", false, validation.getErrorMessage(),
                        System.currentTimeMillis() - startTime, null);
            }

            logger.info("Extracting text from: {}", file.getName());

            Mat processedImage = ImagePreprocessor.preprocess(file, preprocessingConfig);
            ImagePreprocessor.ImageQuality imageQuality = ImagePreprocessor.analyzeImageQuality(processedImage);

            adjustOcrSettingsForQuality(imageQuality);
            String text = performOcrWithTimeout(processedImage, PROCESSING_TIMEOUT_MS);

            processedImage.release();

            long processingTime = System.currentTimeMillis() - startTime;
            logger.info("Text extraction completed, {} characters extracted in {} ms",
                    text.length(), processingTime);

            return new ProcessingResult<>(text, true, null, processingTime, imageQuality);

        } catch (Exception e) {
            logger.error("Text extraction failed for file: {}", file.getName(), e);
            long processingTime = System.currentTimeMillis() - startTime;
            return new ProcessingResult<>("", false, "Text extraction failed: " + e.getMessage(),
                    processingTime, null);
        }
    }

    /**
     * Auto-detect document type and process accordingly
     * @param file Input image file
     * @return Processing result with detected type and extracted data
     */
    public ProcessingResult<Object> processDocumentAuto(File file) {
        long startTime = System.currentTimeMillis();

        try {
            // First, extract raw text to analyze document type
            String rawText = ocrEngine.doOcr(file);
            ProcessingMode detectedMode = detectDocumentType(rawText);

            logger.info("Auto-detected document type: {} for file: {}", detectedMode, file.getName());

            // Process based on detected type
            switch (detectedMode) {
                case IDENTITY_DOCUMENT:
                    ProcessingResult<IdentityDocument> idResult = processIdentityDocument(file);
                    return new ProcessingResult<>(idResult.getResult(), idResult.isSuccessful(),
                            idResult.getErrorMessage(), idResult.getProcessingTime(), idResult.getImageQuality());

                case INVOICE:
                    ProcessingResult<String> textResult = extractText(file);
                    InvoiceData invoiceData = invoiceExtractor.extract(textResult.getResult());
                    return new ProcessingResult<>(invoiceData, textResult.isSuccessful(),
                            textResult.getErrorMessage(), textResult.getProcessingTime(), textResult.getImageQuality());

                default:
                    ProcessingResult<String> generalResult = extractText(file);
                    return new ProcessingResult<>(generalResult.getResult(), generalResult.isSuccessful(),
                            generalResult.getErrorMessage(), generalResult.getProcessingTime(), generalResult.getImageQuality());
            }

        } catch (Exception e) {
            logger.error("Auto-processing failed for file: {}", file.getName(), e);
            long processingTime = System.currentTimeMillis() - startTime;
            return new ProcessingResult<>(null, false, "Auto-processing failed: " + e.getMessage(),
                    processingTime, null);
        }
    }

    /**
     * Validate input file
     */
    private ValidationResult validateInputFile(File file) {
        if (file == null) {
            return new ValidationResult(false, "Input file is null");
        }

        if (!file.exists()) {
            return new ValidationResult(false, "Input file does not exist: " + file.getAbsolutePath());
        }

        if (!file.isFile()) {
            return new ValidationResult(false, "Input is not a file: " + file.getAbsolutePath());
        }

        if (file.length() < MIN_FILE_SIZE) {
            return new ValidationResult(false, "File too small: " + file.length() + " bytes");
        }

        if (file.length() > MAX_FILE_SIZE) {
            return new ValidationResult(false, "File too large: " + file.length() + " bytes");
        }

        String fileName = file.getName().toLowerCase();
        boolean validExtension = Arrays.stream(SUPPORTED_EXTENSIONS)
                .anyMatch(ext -> fileName.endsWith(ext));

        if (!validExtension) {
            return new ValidationResult(false, "Unsupported file format: " + fileName);
        }

        return new ValidationResult(true, null);
    }

    /**
     * Detect document type from OCR text
     */
    private ProcessingMode detectDocumentType(String text) {
        if (text == null || text.trim().isEmpty()) {
            return ProcessingMode.GENERAL_TEXT;
        }

        // Check for MRZ patterns
        if (MRZ_PATTERN.matcher(text).matches() ||
                text.contains("P<") || text.contains("ID<") ||
                text.matches(".*[A-Z0-9<]{30,}.*")) {
            return ProcessingMode.IDENTITY_DOCUMENT;
        }

        // Check for invoice patterns
        if (text.toLowerCase().contains("invoice") ||
                text.toLowerCase().contains("bill") ||
                text.toLowerCase().contains("total") ||
                text.toLowerCase().contains("amount")) {
            return ProcessingMode.INVOICE;
        }

        return ProcessingMode.GENERAL_TEXT;
    }

    /**
     * Perform OCR with timeout
     */
    private String performOcrWithTimeout(Mat image, long timeoutMs) throws Exception {
        Future<String> future = executorService.submit(() -> {
            try {
                return ocrEngine.doOcr(image);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new RuntimeException("OCR processing timed out after " + timeoutMs + "ms");
        }
    }

    /**
     * Adjust OCR settings based on image quality
     */
    private void adjustOcrSettingsForQuality(ImagePreprocessor.ImageQuality quality) {
        if (quality.sharpness < 50) {
            // Low sharpness - use more aggressive settings
            ocrEngine.setPageSegMode(6); // Single uniform block
            ocrEngine.setOcrEngineMode(1); // LSTM only
        } else if (quality.contrast < 50) {
            // Low contrast - adjust for better text detection
            ocrEngine.setPageSegMode(3); // Fully automatic
            ocrEngine.setOcrEngineMode(1); // LSTM only
        } else {
            // Good quality - use optimal settings
            ocrEngine.setPageSegMode(6); // Single uniform block
            ocrEngine.setOcrEngineMode(1); // LSTM only
        }
    }

    /**
     * Validate MRZ text quality
     */
    private boolean isValidMrzText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        // Check for minimum character count
        if (text.length() < 44) {
            return false;
        }

        // Check for MRZ-like patterns
        long uppercaseCount = text.chars().filter(c -> Character.isUpperCase(c)).count();
        long digitCount = text.chars().filter(c -> Character.isDigit(c)).count();
        long symbolCount = text.chars().filter(c -> c == '<').count();

        return (uppercaseCount + digitCount + symbolCount) > text.length() * 0.7;
    }

    /**
     * Validate extraction result
     */
    private boolean isValidExtractionResult(IdentityDocument document) {
        if (document == null) {
            return false;
        }

        // Check if at least some key fields are extracted
        return document.getDocumentNumber() != null && !document.getDocumentNumber().isEmpty() ||
                document.getGivenNames() != null && !document.getGivenNames().isEmpty() ||
                document.getSurname() != null && !document.getSurname().isEmpty();
    }

    /**
     * Adjust preprocessing config for retry
     */
    private void adjustConfigForRetry() {
        // Increase scale factor for better character recognition
        if (preprocessingConfig.getScaleFactor() < 4.0) {
            preprocessingConfig.setScaleFactor(preprocessingConfig.getScaleFactor() + 0.5);
        }

        // Try different threshold method
        if (preprocessingConfig.getThresholdMethod() == ImagePreprocessor.PreprocessingConfig.ThresholdMethod.ADAPTIVE_GAUSSIAN) {
            preprocessingConfig.setThresholdMethod(ImagePreprocessor.PreprocessingConfig.ThresholdMethod.COMBINED);
        }
    }

    /**
     * Create processing configuration optimized for specific mode
     */
    private ImagePreprocessor.PreprocessingConfig createOptimizedConfig(ProcessingMode mode) {
        ImagePreprocessor.PreprocessingConfig config = new ImagePreprocessor.PreprocessingConfig();

        switch (mode) {
            case IDENTITY_DOCUMENT:
                // MRZ-specific optimizations
                config.setScaleFactor(3.0);
                config.setEnableDenoising(true);
                config.setEnableDeskewing(true);
                config.setEnableContrastEnhancement(true);
                config.setEnableUnsharpMask(true);
                config.setEnableBorderRemoval(true);
                config.setThresholdMethod(ImagePreprocessor.PreprocessingConfig.ThresholdMethod.ADAPTIVE_GAUSSIAN);
                config.setBlurKernelSize(1);
                config.setMorphKernelSize(1);
                break;

            case INVOICE:
                // Invoice-specific optimizations
                config.setScaleFactor(2.0);
                config.setEnableDenoising(true);
                config.setEnableDeskewing(true);
                config.setEnableContrastEnhancement(true);
                config.setThresholdMethod(ImagePreprocessor.PreprocessingConfig.ThresholdMethod.COMBINED);
                config.setBlurKernelSize(0);
                config.setMorphKernelSize(2);
                break;

            default:
                // General text optimizations
                config.setScaleFactor(2.0);
                config.setEnableDenoising(true);
                config.setEnableContrastEnhancement(true);
                config.setThresholdMethod(ImagePreprocessor.PreprocessingConfig.ThresholdMethod.ADAPTIVE_GAUSSIAN);
                break;
        }

        return config;
    }

    /**
     * Update processing statistics
     */
    private void updateStatistics(boolean successful, long processingTime) {
        totalProcessed.incrementAndGet();
        if (successful) {
            totalSuccessful.incrementAndGet();
        } else {
            totalFailed.incrementAndGet();
        }
        totalProcessingTime += processingTime;
    }

    /**
     * Get processing statistics
     */
    public ProcessingStatistics getProcessingStatistics() {
        return new ProcessingStatistics(
                totalProcessed.get(),
                totalSuccessful.get(),
                totalFailed.get(),
                totalProcessingTime,
                totalProcessed.get() > 0 ? (double) totalProcessingTime / totalProcessed.get() : 0
        );
    }

    /**
     * Configuration and utility methods
     */
    public void setProcessingMode(ProcessingMode mode) {
        this.processingMode = mode;
        this.preprocessingConfig = createOptimizedConfig(mode);
        logger.info("Processing mode set to: {}", mode);
    }

    public void setPreprocessingConfig(ImagePreprocessor.PreprocessingConfig config) {
        this.preprocessingConfig = config;
        logger.debug("Preprocessing configuration updated");
    }

    public void setLanguage(String language) {
        ocrEngine.setLanguage(language);
        logger.info("OCR language set to: {}", language);
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        if (debugMode) {
            createDebugDirectory();
        }
        logger.info("Debug mode {}", debugMode ? "enabled" : "disabled");
    }

    public void setEnableParallelProcessing(boolean enable) {
        this.enableParallelProcessing = enable;
        logger.info("Parallel processing {}", enable ? "enabled" : "disabled");
    }

    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = Math.max(1, maxRetryAttempts);
        logger.info("Max retry attempts set to: {}", this.maxRetryAttempts);
    }

    public String[] getAvailableLanguages() {
        return ocrEngine.getAvailableLanguages();
    }

    /**
     * Cleanup resources
     */
    public void shutdown() {
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            logger.info("OCR Processor shut down successfully");
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
            logger.warn("OCR Processor shutdown interrupted");
        }
    }

    /**
     * Utility classes
     */
    private static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class ProcessingStatistics {
        private final int totalProcessed;
        private final int totalSuccessful;
        private final int totalFailed;
        private final long totalProcessingTime;
        private final double averageProcessingTime;

        public ProcessingStatistics(int totalProcessed, int totalSuccessful, int totalFailed,
                                    long totalProcessingTime, double averageProcessingTime) {
            this.totalProcessed = totalProcessed;
            this.totalSuccessful = totalSuccessful;
            this.totalFailed = totalFailed;
            this.totalProcessingTime = totalProcessingTime;
            this.averageProcessingTime = averageProcessingTime;
        }

        public int getTotalProcessed() { return totalProcessed; }
        public int getTotalSuccessful() { return totalSuccessful; }
        public int getTotalFailed() { return totalFailed; }
        public long getTotalProcessingTime() { return totalProcessingTime; }
        public double getAverageProcessingTime() { return averageProcessingTime; }
        public double getSuccessRate() {
            return totalProcessed > 0 ? (double) totalSuccessful / totalProcessed * 100 : 0;
        }

        @Override
        public String toString() {
            return String.format("Processing Statistics - Total: %d, Successful: %d, Failed: %d, " +
                            "Success Rate: %.2f%%, Average Time: %.2f ms",
                    totalProcessed, totalSuccessful, totalFailed,
                    getSuccessRate(), averageProcessingTime);
        }
    }

    // Debug and utility methods
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

    private void saveDebugImage(Mat image, String filename) {
        try {
            String outputPath = Paths.get(debugOutputDir, filename).toString();
            ImagePreprocessor.saveDebugImage(image, outputPath);
        } catch (Exception e) {
            logger.warn("Failed to save debug image: {}", filename, e);
        }
    }

    private void saveDebugText(String text, String filename) {
        try {
            Path outputPath = Paths.get(debugOutputDir, filename);
            Files.write(outputPath, text.getBytes());
            logger.debug("Debug text saved to: {}", outputPath);
        } catch (IOException e) {
            logger.warn("Failed to save debug text: {}", filename, e);
        }
    }

    public String getProcessorInfo() {
        return String.format("Enhanced OCR Processor - Mode: %s, Languages: %s, Debug: %s, " +
                        "Parallel: %s, Threads: %d, Statistics: %s",
                processingMode,
                String.join(", ", getAvailableLanguages()),
                debugMode ? "enabled" : "disabled",
                enableParallelProcessing ? "enabled" : "disabled",
                DEFAULT_THREAD_POOL_SIZE,
                getProcessingStatistics().toString());
    }

    // Deprecated methods for backward compatibility
    @Deprecated
    public InvoiceData processInvoice(File file) throws Exception {
        logger.warn("processInvoice() is deprecated. Use processDocumentAuto() for automatic detection.");
        ProcessingResult<String> result = extractText(file);
        if (result.isSuccessful()) {
            return invoiceExtractor.extract(result.getResult());
        } else {
            throw new Exception(result.getErrorMessage());
        }
    }

    @Deprecated
    public String extractTextRaw(File file) throws Exception {
        logger.warn("extractTextRaw() is deprecated. Use extractText() for better error handling.");
        ProcessingResult<String> result = extractText(file);
        if (result.isSuccessful()) {
            return result.getResult();
        } else {
            throw new Exception(result.getErrorMessage());
        }
    }
}