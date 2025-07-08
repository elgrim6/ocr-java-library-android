package mz.nedbank.ocr.core;

import nu.pattern.OpenCV;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;

/**
 * Utility class that performs basic OpenCV preprocessing on input images.
 * The processed image is written to a temporary PNG file which can be used
 * as input for OCR engines such as Tesseract.
 */
public class ImagePreprocessor {
    static {
        // Load the bundled OpenCV native library
        try {
            OpenCV.loadLocally();
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Failed to load OpenCV library: " + e.getMessage());
        }
    }

    /**
     * Preprocess the supplied image file. The result is saved to a new
     * temporary file which should be deleted by the caller when no longer
     * needed.
     *
     * @param input original image
     * @return preprocessed image file
     * @throws IOException if the image cannot be read or written
     */
    public File preprocess(File input) throws IOException {
        Mat src = Imgcodecs.imread(input.getAbsolutePath());
        if (src.empty()) {
            throw new IOException("Unable to read input image: " + input.getAbsolutePath());
        }

        // Convert to grayscale
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

        // Reduce noise
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(gray, blurred, new Size(3, 3), 0);

        // Adaptive threshold for consistent binarization
        Mat thresh = new Mat();
        Imgproc.adaptiveThreshold(
                blurred,
                thresh,
                255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY,
                31,
                2);

        File temp = File.createTempFile("ocr_preprocessed_", ".png");
        Imgcodecs.imwrite(temp.getAbsolutePath(), thresh);

        // Release native memory
        src.release();
        gray.release();
        blurred.release();
        thresh.release();

        return temp;
    }
}
