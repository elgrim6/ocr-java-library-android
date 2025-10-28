package mz.nedbank.ocr.core;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.CLAHE;
import org.opencv.photo.Photo;

import java.io.File;
import java.io.IOException;

/**
 * Enhanced image preprocessing utility for OCR with focus on MRZ extraction.
 * Implements multiple preprocessing strategies with fallback options.
 */
public class ImagePreprocessor {
    static {
        try {
            OpenCV.loadLocally();
        } catch (UnsatisfiedLinkError e) {
            System.err.println("OpenCV not available: " + e.getMessage());
        }
    }

    //650 best result so far
    //879 & 898 around the same
    //667 best result pt2

    //v2
    //660 best so far
    //669 best
    //682 PERFECT on test with id card 10 sample

    private static final int TARGET_HEIGHT =550; // Increased target height for better resolution

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

        // Step 3: Apply Non-local Means Denoising for better noise reduction
        Mat denoised = new Mat();
        Photo.fastNlMeansDenoising(gray, denoised, 30, 7, 21); // Increased h parameter for stronger denoising

        // Step 4: Apply CLAHE for contrast enhancement
        CLAHE clahe = Imgproc.createCLAHE(3.0, new Size(8, 8));
        Mat claheResult = new Mat();
        clahe.apply(denoised, claheResult);

        // Step 4.5: Sharpen before thresholding to make < clearer
        Imgproc.GaussianBlur(claheResult, claheResult, new Size(0, 0), 3);
        Core.addWeighted(claheResult, 1.5, claheResult, -0.5, 0, claheResult);

        // Step 5: Deskew the image
        Mat deskewed = deskewImage(claheResult);

// Step 6: Adaptive thresholding
        Mat binary = new Mat();
        Imgproc.adaptiveThreshold(
                deskewed,
                binary,
                255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY,
                199,
                25
        );
        // Step 7: Morphological operations
// Reduce kernel size so < doesn't get closed into a blob
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, 1));
        Imgproc.morphologyEx(binary, binary, Imgproc.MORPH_OPEN, kernel);

        File temp = File.createTempFile("C:\\ocr_processed_", ".jpg");
        Imgcodecs.imwrite(temp.getAbsolutePath(), binary);

        //debug (show preprocessed images)
        /*
        // Create output directory if it doesn't exist
        File outputDir = new File( "C:\\ocr_processed\\");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Create output file with timestamp to avoid conflicts
        String timestamp = String.valueOf(System.currentTimeMillis());
        String inputName = input.getName();
        String baseName = inputName.substring(0, inputName.lastIndexOf('.'));
        String outputFileName =  "C:\\ocr_processed\\processed_" + baseName + "_" + timestamp + ".jpg";
        File outputFile = new File(outputFileName);

        // Save the processed image
        boolean success = Imgcodecs.imwrite(outputFile.getAbsolutePath(), binary);
        if (!success) {
            throw new IOException("Failed to save processed image to: " + outputFile.getAbsolutePath());
        }

        System.out.println("Processed image saved to: " + outputFile.getAbsolutePath());
*/


        // Release resources
        src.release();
        resized.release();
        gray.release();
        denoised.release();
        claheResult.release();
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

                // Filter out near-vertical lines and lines with extreme angles
                if (Math.abs(currentAngle) < 45 && Math.abs(currentAngle) > 0.5) { // Only consider angles significant enough for deskewing
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
}