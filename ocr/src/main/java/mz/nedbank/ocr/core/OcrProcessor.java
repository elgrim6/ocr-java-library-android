package mz.nedbank.ocr.core;

import mz.nedbank.ocr.extractor.MrzDataExtractor;
import mz.nedbank.ocr.model.IdentityDocument;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.IOException;

import android.util.Log;

/**
 * Android OCR processor that uses tess-two (Tesseract for Android)
 * to extract text from an image and then parses MRZ information.
 */
public class OcrProcessor {

    private static final String TAG = "OCRProcessor";
    private TessBaseAPI mTess;
    private File tessDataDir;

    /** Result wrapper used by the examples. */
    public static class ProcessingResult<T> {
        private final T result;
        private final boolean successful;
        private final String errorMessage;

        public ProcessingResult(T result, boolean successful, String errorMessage) {
            this.result = result;
            this.successful = successful;
            this.errorMessage = errorMessage;
        }

        public T getResult() {
            return result;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    private final MrzDataExtractor extractor = new MrzDataExtractor();
    private final ImagePreprocessor preprocessor = new ImagePreprocessor();
    private final MrzImageCropper cropper = new MrzImageCropper();
    private final String language;

    /** Create a processor using English OCR. */
    public OcrProcessor() {
        this("mrz");
    }

    /** Create a processor with a custom tesseract language. */
    public OcrProcessor(String language) {
        this.language = language;
    }

    /**
     * Set the tesseract data directory (required for Android)
     */
    public void setTessDataDir(File dataDir) {
        this.tessDataDir = dataDir;
    }

    /**
     * Initialize Tesseract (must be called before processing)
     */
    private void initTesseract() throws IOException {
        if (mTess != null) {
            return; // Already initialized
        }

        if (tessDataDir == null) {
            throw new IOException("Tesseract data directory not set. Call setTessDataDir() first.");
        }

        mTess = new TessBaseAPI();
        String dataPath = tessDataDir.getAbsolutePath();

        Log.d(TAG, "Initializing Tesseract with data path: " + dataPath);

        // Try MRZ language first
        boolean success = mTess.init(dataPath, language);

        // If MRZ fails, try English
        if (!success && !language.equals("eng")) {
            Log.w(TAG, "Failed to initialize Tesseract with language: " + language + ", trying English");
            success = mTess.init(dataPath, "eng");
        }

        if (!success) {
            throw new IOException("Failed to initialize Tesseract with any language");
        }

        Log.d(TAG, "Tesseract initialized successfully");
    }

    /**
     * Process an identity document image using Tesseract and parse the
     * resulting MRZ text.
     */
    public ProcessingResult<IdentityDocument> processIdentityDocument(File image) {
        try {
            // Initialize Tesseract if not already done
            initTesseract();

            // First crop the MRZ region if possible
            File cropped = image;
            try {
                cropped = cropper.crop(image);
            } catch (IOException e) {
                System.err.println("MRZ cropping failed: " + e.getMessage());
                Log.e(TAG, "MRZ cropping failed: " + e.getMessage());
            }

            // Preprocess image using OpenCV. If preprocessing fails, fall back to
            // the cropped (or original) image.
            File processed = cropped;
            try {
                processed = preprocessor.preprocess(cropped);
            } catch (IOException e) {
                System.err.println("Preprocessing failed: " + e.getMessage());
                Log.e(TAG, "Preprocessing failed: " + e.getMessage());
            }

            // Run OCR using Tesseract
            Log.d(TAG, "Running OCR on image: " + processed.getAbsolutePath());
            mTess.setImage(processed);
            String ocrText = mTess.getUTF8Text();
            Log.d(TAG, "Raw OCR text: " + ocrText);

            // Remove temporary files
            if (!processed.equals(cropped)) {
                processed.delete();
            }
            if (!cropped.equals(image)) {
                cropped.delete();
            }

            // OCR Cleanup - keep lines that look like MRZ (44+ chars)
            StringBuilder cleaned = new StringBuilder();
            for (String line : ocrText.split("\n")) {
                line = line.trim();
                if (line.length() > 44) {
                    line = line.substring(line.length() - 44); // keep right-most chars
                }
                if (!line.isEmpty()) {
                    cleaned.append(line).append("\n");
                }
            }

            Log.d(TAG, "Cleaned OCR text: " + cleaned.toString());

            IdentityDocument doc = extractor.extract(cleaned.toString());
            return new ProcessingResult<>(doc, true, null);
        } catch (Exception e) {
            Log.e(TAG, "Processing failed: " + e.getMessage(), e);
            IdentityDocument doc = new IdentityDocument();
            doc.setValidationErrors(e.getMessage());
            return new ProcessingResult<>(doc, false, e.getMessage());
        }
    }

    /**
     * Release Tesseract resources
     */
    public void release() {
        if (mTess != null) {
            try {
                mTess.end();
            } catch (Exception e) {
                Log.w(TAG, "Error releasing Tesseract: " + e.getMessage());
            }
            mTess = null;
        }
    }
}