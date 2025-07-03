package mz.nedbank.ocr.engine;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * OCR Engine wrapper for Tesseract
 */
public class OcrEngine {
    private static final Logger logger = LoggerFactory.getLogger(OcrEngine.class);
    private final Tesseract tesseract;
    private static final String TEMP_IMAGE_PREFIX = "temp_ocr_image_";
    private static final String TEMP_IMAGE_SUFFIX = ".png";

    public OcrEngine() {
        this(null, "eng");
    }

    public OcrEngine(String tessDataPath, String language) {
        tesseract = new Tesseract();
        
        // Set tessdata path
        if (tessDataPath != null && !tessDataPath.isEmpty()) {
            tesseract.setDatapath(tessDataPath);
        } else {
            // Try to find tessdata in common locations
            String[] possiblePaths = {
                "tessdata/",
                "./tessdata/",
                "/usr/share/tesseract-ocr/4.00/tessdata/",
                "/usr/share/tesseract-ocr/tessdata/",
                System.getProperty("user.dir") + "/tessdata/"
            };
            
            for (String path : possiblePaths) {
                if (Files.exists(Paths.get(path))) {
                    tesseract.setDatapath(path);
                    logger.info("Using tessdata path: {}", path);
                    break;
                }
            }
        }
        
        // Set language
        tesseract.setLanguage(language);
        
        // Configure OCR settings for better accuracy
        tesseract.setPageSegMode(1); // Automatic page segmentation with OSD
        tesseract.setOcrEngineMode(1); // Neural nets LSTM engine only
        
        logger.info("OCR Engine initialized with language: {}", language);
    }

    /**
     * Perform OCR on a preprocessed image matrix
     * @param image OpenCV Mat containing the preprocessed image
     * @return Extracted text from the image
     * @throws TesseractException if OCR processing fails
     */
    public String doOcr(Mat image) throws TesseractException {
        File tempFile = null;
        try {
            // Create temporary file
            tempFile = createTempImageFile();
            
            // Write Mat to temporary file
            boolean success = Imgcodecs.imwrite(tempFile.getAbsolutePath(), image);
            if (!success) {
                throw new TesseractException("Failed to write image to temporary file");
            }
            
            // Perform OCR
            String result = tesseract.doOCR(tempFile);
            logger.debug("OCR completed, extracted {} characters", result.length());
            
            return result != null ? result.trim() : "";
            
        } catch (IOException e) {
            throw new TesseractException("Failed to create temporary file for OCR", e);
        } finally {
            // Clean up temporary file
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    logger.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Perform OCR directly on a file
     * @param imageFile File containing the image to process
     * @return Extracted text from the image
     * @throws TesseractException if OCR processing fails
     */
    public String doOcr(File imageFile) throws TesseractException {
        if (!imageFile.exists()) {
            throw new TesseractException("Image file does not exist: " + imageFile.getAbsolutePath());
        }
        
        String result = tesseract.doOCR(imageFile);
        logger.debug("OCR completed on file {}, extracted {} characters", 
                    imageFile.getName(), result.length());
        
        return result != null ? result.trim() : "";
    }

    /**
     * Set OCR language
     * @param language Language code (e.g., "eng", "fra", "deu")
     */
    public void setLanguage(String language) {
        tesseract.setLanguage(language);
        logger.info("OCR language set to: {}", language);
    }

    /**
     * Set page segmentation mode
     * @param mode Page segmentation mode (0-13)
     */
    public void setPageSegMode(int mode) {
        tesseract.setPageSegMode(mode);
        logger.debug("Page segmentation mode set to: {}", mode);
    }

    /**
     * Set OCR engine mode
     * @param mode OCR engine mode (0-3)
     */
    public void setOcrEngineMode(int mode) {
        tesseract.setOcrEngineMode(mode);
        logger.debug("OCR engine mode set to: {}", mode);
    }

    /**
     * Create a temporary file for image processing
     * @return Temporary file
     * @throws IOException if file creation fails
     */
    private File createTempImageFile() throws IOException {
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Path tempFile = Files.createTempFile(tempDir, TEMP_IMAGE_PREFIX, TEMP_IMAGE_SUFFIX);
        return tempFile.toFile();
    }

    /**
     * Get available languages
     * @return Array of available language codes
     */
    public String[] getAvailableLanguages() {
        // Note: Tesseract4J doesn't provide a direct method to get available languages
        // This is a simplified implementation that returns common languages
        // In practice, you would scan the tessdata directory for .traineddata files
        return new String[]{"eng", "fra", "deu", "spa", "ita", "por", "rus", "chi_sim", "chi_tra", "jpn", "kor"};
    }
}

