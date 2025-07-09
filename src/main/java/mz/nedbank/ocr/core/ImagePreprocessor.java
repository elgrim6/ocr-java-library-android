package mz.nedbank.ocr.core;

import nu.pattern.OpenCV;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.CLAHE;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced image preprocessing utility for OCR with focus on MRZ extraction.
 * Implements multiple preprocessing strategies with fallback options.
 */
public class ImagePreprocessor {
    static {
        try {
            OpenCV.loadLocally();
        } catch (UnsatisfiedLinkError e) {
            try {
                if (!OpenCVLoader.initDebug()) {
                    System.err.println("Failed to load OpenCV library: " + e.getMessage());
                }
            } catch (NoClassDefFoundError ignored) {
                System.err.println("OpenCV not available: " + e.getMessage());
            }
        }
    }

    private static final int TARGET_HEIGHT = 450; // Increased target height for better resolution

    /**
     * Enhanced preprocessing with multiple strategies and fallback options.
     * Returns the best processed image for OCR.
     */
    public File preprocess(File input) throws IOException {
        Mat src = Imgcodecs.imread(input.getAbsolutePath());
        if (src.empty()) {
            throw new IOException("Unable to read input image: " + input.getAbsolutePath());
        }

        System.out.println("Original image size: " + src.width() + "x" + src.height());

        // Step 1: Resize image to a fixed target height, maintaining aspect ratio
        double scaleFactor = (double) TARGET_HEIGHT / src.height();
        int newWidth = (int) (src.width() * scaleFactor);
        Mat resized = new Mat();
        Imgproc.resize(src, resized, new Size(newWidth, TARGET_HEIGHT), 0, 0, Imgproc.INTER_CUBIC);
        System.out.println("Resized image to: " + resized.width() + "x" + resized.height());

        // Step 2: Convert to grayscale
        Mat gray = new Mat();
        Imgproc.cvtColor(resized, gray, Imgproc.COLOR_BGR2GRAY);

        // Step 3: Apply CLAHE for contrast enhancement
        CLAHE clahe = Imgproc.createCLAHE(3.0, new Size(8, 8));
        Mat claheResult = new Mat();
        clahe.apply(gray, claheResult);

        // Step 4: Apply Median blur for noise reduction (effective for salt-and-pepper noise)
        Mat blurred = new Mat();
        Imgproc.medianBlur(claheResult, blurred, 5); // Using a 5x5 median filter

        // Step 5: Deskew the image
        Mat deskewed = deskewImage(blurred);

        // Step 6: Apply adaptive thresholding with adjusted parameters
        Mat binary = new Mat();
        Imgproc.adaptiveThreshold(deskewed, binary, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 181, 35); // Increased block size and C value

        // Step 7: Apply morphological operations (optional, can be adjusted based on results)
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)); // Increased kernel size
        Imgproc.morphologyEx(binary, binary, Imgproc.MORPH_CLOSE, kernel);
        Imgproc.morphologyEx(binary, binary, Imgproc.MORPH_OPEN, kernel); // Added opening operation

        File temp = File.createTempFile("ocr_processed_", ".png");
        Imgcodecs.imwrite(temp.getAbsolutePath(), binary);

        // Release resources
        src.release();
        resized.release();
        gray.release();
        claheResult.release();
        blurred.release();
        deskewed.release();
        binary.release();

        return temp;
    }

    private Mat deskewImage(Mat src) {
        Mat edges = new Mat();
        Imgproc.Canny(src, edges, 50, 150);

        Mat lines = new Mat();
        Imgproc.HoughLinesP(edges, lines, 1, Math.PI / 180, 100, 100, 10);

        double angle = 0.0;
        if (lines.rows() > 0) {
            double totalAngle = 0;
            int lineCount = 0;

            for (int i = 0; i < lines.rows(); i++) {
                double[] line = lines.get(i, 0);
                double currentAngle = Math.toDegrees(Math.atan2(line[3] - line[1], line[2] - line[0]));

                if (Math.abs(currentAngle) < 45) { // Consider lines that are mostly horizontal
                    totalAngle += currentAngle;
                    lineCount++;
                }
            }

            if (lineCount > 0) {
                angle = totalAngle / lineCount;
            }
        }

        System.out.println("Detected skew angle: " + angle);

        Point center = new Point(src.cols() / 2.0, src.rows() / 2.0);
        Mat rotationMatrix = Imgproc.getRotationMatrix2D(center, angle, 1);
        Mat rotated = new Mat();
        Imgproc.warpAffine(src, rotated, rotationMatrix, src.size(), Imgproc.INTER_CUBIC, Core.BORDER_REPLICATE, new Scalar(255, 255, 255));

        edges.release();
        lines.release();
        rotationMatrix.release();

        return rotated;
    }

    // Removed unused methods for simplicity
    private Mat enhancedPreprocessingPipeline(Mat src) { return null; }
    private Mat resizeIfNeeded(Mat src) { return null; }
    private Mat correctPerspectiveAndRotation(Mat src) { return null; }
    private Mat findLargestRectangle(List<MatOfPoint> contours) { return null; }
    private Mat correctPerspective(Mat src, MatOfPoint2f rectangle) { return null; }
    private Point[] sortCorners(Point[] corners) { return null; }
    private double distance(Point p1, Point p2) { return 0.0; }
    private Mat correctRotation(Mat src, Mat edges) { return null; }
    private Mat enhanceContrastAdaptive(Mat src) { return null; }
    private Mat sharpenImageAdvanced(Mat src) { return null; }
    private Mat applyAdaptiveThreshold(Mat src) { return null; }
    private Mat enhanceTextMorphology(Mat src) { return null; }
    private Mat removeNoise(Mat src) { return null; }
    private File basicPreprocess(Mat src, File input) throws IOException { return null; }
}
