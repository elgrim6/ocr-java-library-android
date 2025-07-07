package mz.nedbank.ocr.core;

import mz.nedbank.ocr.extractor.MrzDataExtractor;
import mz.nedbank.ocr.model.IdentityDocument;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * Simplified OCR processor that invokes the system tesseract command
 * to extract text from an image and then parses MRZ information.
 * This avoids heavy dependencies like OpenCV and tess4j which are
 * not available in this environment.
 */
public class OcrProcessor {

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
    private final String language;

    /** Create a processor using English OCR. */
    public OcrProcessor() {
        this("eng");
    }

    /** Create a processor with a custom tesseract language. */
    public OcrProcessor(String language) {
        this.language = language;
    }

    /**
     * Process an identity document image using tesseract CLI and parse the
     * resulting MRZ text.
     */
    public ProcessingResult<IdentityDocument> processIdentityDocument(File image) {
        try {
            // Allow overriding the tesseract binary via environment variable
            String tesseractCmd = System.getenv().getOrDefault("TESSERACT_PATH", "tesseract");

            ProcessBuilder pb = new ProcessBuilder(
                    tesseractCmd,
                    image.getAbsolutePath(),
                    "stdout",
                    "-l", language,
                    "--oem", "1",
                    "--psm", "6");
            Process proc = pb.start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            proc.waitFor();

            // Basic cleanup of OCR output. Some engines may prepend or append
            // stray characters which can prevent MRZ detection. We trim each
            // line and limit its length to 44 characters which is the longest
            // standard MRZ line length.
            StringBuilder cleaned = new StringBuilder();
            for (String l : sb.toString().split("\n")) {
                l = l.trim();
                if (l.length() > 44) {
                    l = l.substring(l.length() - 44); // keep right-most chars
                }
                cleaned.append(l).append("\n");
            }

            IdentityDocument doc = extractor.extract(cleaned.toString());
            return new ProcessingResult<>(doc, true, null);
        } catch (Exception e) {
            IdentityDocument doc = new IdentityDocument();
            doc.setValidationErrors(e.getMessage());
            return new ProcessingResult<>(doc, false, e.getMessage());
        }
    }
}
