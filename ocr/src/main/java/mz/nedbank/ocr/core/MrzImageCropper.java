package mz.nedbank.ocr.core;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Utility class that attempts to locate and crop the Machine Readable Zone (MRZ)
 * from a document image. If detection fails, the original image is returned.
 */
public class MrzImageCropper {
    private static boolean openCVLoaded = false;

    static {
        try {
            System.loadLibrary("opencv_java4");
            openCVLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            System.err.println("OpenCV not available: " + e.getMessage());
            openCVLoaded = false;
        }
    }

    /**
     * Crop the MRZ region from the input image file. The returned file contains
     * only the detected MRZ area. If cropping fails, the original file is returned.
     */
    public File crop(File input) throws IOException {
        if (!openCVLoaded) {
            throw new IOException("OpenCV library not loaded");
        }

        Mat src = Imgcodecs.imread(input.getAbsolutePath());
        if (src.empty()) {
            throw new IOException("Unable to read input image: " + input.getAbsolutePath());
        }

        Rect mrzRect = detectMrz(src);
        if (mrzRect == null) {
            src.release();
            return input;
        }

        Mat mrz = new Mat(src, mrzRect);
        File temp = File.createTempFile("ocr_mrz_", ".png");
        Imgcodecs.imwrite(temp.getAbsolutePath(), mrz);

        src.release();
        mrz.release();
        return temp;
    }

    /**
     * Crop the full document region from the input image file. Removes borders and
     * background to isolate the document boundaries. If detection fails, the original
     * file is returned.
     */
    public File cropDocument(File input) throws IOException {
        if (!openCVLoaded) {
            throw new IOException("OpenCV library not loaded");
        }

        Mat src = Imgcodecs.imread(input.getAbsolutePath());
        if (src.empty()) {
            throw new IOException("Unable to read input image: " + input.getAbsolutePath());
        }

        Rect docRect = detectDocument(src);
        if (docRect == null) {
            src.release();
            return input;
        }

        Mat doc = new Mat(src, docRect);
        File temp = File.createTempFile("ocr_doc_", ".jpg");
        Imgcodecs.imwrite(temp.getAbsolutePath(), doc);

        src.release();
        doc.release();
        return temp;
    }

    private Rect detectDocument(Mat src) {
        // Convert to grayscale
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

        // Apply Gaussian blur to reduce noise
        Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);

        // Use Canny edge detection to find edges
        Mat edges = new Mat();
        Imgproc.Canny(gray, edges, 50, 150);

        // Dilate edges to connect broken segments
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
        Imgproc.dilate(edges, edges, kernel, new Point(-1, -1), 2);

        // Find contours
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Sort contours by area (largest first)
        contours.sort(new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint c1, MatOfPoint c2) {
                double area1 = Imgproc.contourArea(c1);
                double area2 = Imgproc.contourArea(c2);
                return Double.compare(area2, area1);
            }
        });

        gray.release();
        edges.release();
        kernel.release();
        hierarchy.release();

        // Filter and find document contour
        double minArea = (src.rows() * src.cols()) * 0.15; // Document should be at least 15% of image
        double maxArea = (src.rows() * src.cols()) * 0.99; // Document should be less than 99% of image

        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);

            // Skip contours that are too small or too large
            if (area < minArea || area > maxArea) {
                continue;
            }

            Rect rect = Imgproc.boundingRect(contour);

            // Document should have reasonable aspect ratio (not too extreme)
            double aspectRatio = (double) rect.width / rect.height;
            if (aspectRatio < 0.5 || aspectRatio > 2.0) {
                continue;
            }

            // Add a small margin around the detected document
            int marginY = Math.max(1, (int) (src.rows() * 0.01));
            int marginX = Math.max(1, (int) (src.cols() * 0.01));
            int x1 = Math.max(0, rect.x - marginX);
            int y1 = Math.max(0, rect.y - marginY);
            int x2 = Math.min(src.cols(), rect.x + rect.width + marginX);
            int y2 = Math.min(src.rows(), rect.y + rect.height + marginY);

            return new Rect(x1, y1, x2 - x1, y2 - y1);
        }

        // Fallback: if no suitable contour found, return null to use original image
        return null;
    }

    private Rect detectMrz(Mat src) {
        // Convert to grayscale and remove small noise
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(gray, gray, new Size(3, 3), 0);

        // Use a blackhat operation to emphasize dark text on a light background
        Mat blackhat = new Mat();
        Mat rectKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(13, 5));
        Imgproc.morphologyEx(gray, blackhat, Imgproc.MORPH_BLACKHAT, rectKernel);

        // Compute the gradient in the x direction and enhance it
        Mat gradX = new Mat();
        Imgproc.Sobel(blackhat, gradX, CvType.CV_32F, 1, 0, -1);
        Core.convertScaleAbs(gradX, gradX);
        Imgproc.morphologyEx(gradX, gradX, Imgproc.MORPH_CLOSE, rectKernel);

        // Threshold the gradient image to get binary regions of text
        Mat thresh = new Mat();
        Imgproc.threshold(gradX, thresh, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

        // Merge neighboring text lines to form one large block
        Mat sqKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(21, 21));
        Imgproc.morphologyEx(thresh, thresh, Imgproc.MORPH_CLOSE, sqKernel);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        int x1 = src.cols(), y1 = src.rows(), x2 = 0, y2 = 0;
        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);
            double ratio = rect.width / (double) rect.height;
            if (ratio > 4.0 && rect.height > src.rows() * 0.05) {
                x1 = Math.min(x1, rect.x);
                y1 = Math.min(y1, rect.y);
                x2 = Math.max(x2, rect.x + rect.width);
                y2 = Math.max(y2, rect.y + rect.height);
            }
        }

        gray.release();
        blackhat.release();
        gradX.release();
        thresh.release();
        rectKernel.release();
        sqKernel.release();
        hierarchy.release();

        if (x2 <= x1 || y2 <= y1) {
            // fallback to bottom 35% of the image when detection fails
            int y = (int) (src.rows() * 0.65);
            return new Rect(0, y, src.cols(), src.rows() - y);
        }

        // Add a small margin around the detected block
        int marginY = (int) (src.rows() * 0.02);
        int marginX = (int) (src.cols() * 0.02);
        x1 = Math.max(0, x1 - marginX);
        y1 = Math.max(0, y1 - marginY);
        x2 = Math.min(src.cols(), x2 + marginX);
        y2 = Math.min(src.rows(), y2 + marginY);

        return new Rect(x1, y1, x2 - x1, y2 - y1);
    }
}