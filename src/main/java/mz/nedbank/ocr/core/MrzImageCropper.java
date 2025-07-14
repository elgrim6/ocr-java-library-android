package mz.nedbank.ocr.core;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class that attempts to locate and crop the Machine Readable Zone (MRZ)
 * from a document image. If detection fails, the original image is returned.
 */
public class MrzImageCropper {
    static {
        try {
            OpenCV.loadLocally();
        } catch (UnsatisfiedLinkError e) {
            System.err.println("OpenCV not available: " + e.getMessage());
        }
    }

    /**
     * Crop the MRZ region from the input image file. The returned file contains
     * only the detected MRZ area. If cropping fails, the original file is returned.
     */
    public File crop(File input) throws IOException {
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

    private Rect detectMrz(Mat src) {
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(gray, gray, new Size(3, 3), 0);

        Mat gradX = new Mat();
        Imgproc.Sobel(gray, gradX, CvType.CV_32F, 1, 0, -1);
        Core.convertScaleAbs(gradX, gradX);

        Mat blurred = new Mat();
        Imgproc.GaussianBlur(gradX, blurred, new Size(3, 3), 0);

        Mat thresh = new Mat();
        Imgproc.threshold(blurred, thresh, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(25, 5));
        Imgproc.morphologyEx(thresh, thresh, Imgproc.MORPH_CLOSE, kernel);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        Rect best = null;
        double bestArea = 0;
        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);
            double ratio = rect.width / (double) rect.height;
            double area = rect.area();
            if (ratio > 5.0 && area > bestArea) {
                best = rect;
                bestArea = area;
            }
        }

        gray.release();
        gradX.release();
        blurred.release();
        thresh.release();
        kernel.release();
        hierarchy.release();

        if (best == null) {
            // fallback to bottom 30% of the image
            int y = (int) (src.rows() * 0.7);
            return new Rect(0, y, src.cols(), src.rows() - y);
        }
        return best;
    }
}
