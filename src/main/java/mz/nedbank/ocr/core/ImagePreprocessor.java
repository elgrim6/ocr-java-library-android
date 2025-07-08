package mz.nedbank.ocr.core;

import nu.pattern.OpenCV;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Size;
import org.opencv.core.Core;
import org.opencv.core.Scalar;
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

        // Adjust contrast/brightness and sharpen the image as suggested by
        // the CVProcessor reference implementation. These operations help
        // emphasise text regions before thresholding.
        Mat adjusted = adjustBrightnessAndContrast(src, 3);
        Mat sharpened = sharpenImage(adjusted);

        // Convert to grayscale
        Mat gray = new Mat();
        Imgproc.cvtColor(sharpened, gray, Imgproc.COLOR_BGR2GRAY);

        // Reduce noise before thresholding
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

        // Morphological closing/dilation to connect character strokes
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.morphologyEx(thresh, thresh, Imgproc.MORPH_CLOSE, kernel);
        Imgproc.dilate(thresh, thresh, kernel);

        File temp = File.createTempFile("ocr_preprocessed_", ".png");
        Imgcodecs.imwrite(temp.getAbsolutePath(), thresh);

        // Release native memory
        src.release();
        adjusted.release();
        sharpened.release();
        gray.release();
        blurred.release();
        thresh.release();
        kernel.release();

        return temp;
    }

    /**
     * Contrast stretching and brightness adjustment based on histogram
     * clipping. Implementation inspired by the original CVProcessor class.
     */
    private Mat adjustBrightnessAndContrast(Mat src, double clipPercentage) {
        int histSize = 256;
        double minGray;
        double maxGray;

        Mat gray = new Mat();
        if (src.channels() == 1) {
            gray = src.clone();
        } else {
            Imgproc.cvtColor(src, gray,
                    src.channels() == 3 ? Imgproc.COLOR_RGB2GRAY : Imgproc.COLOR_RGBA2GRAY);
        }

        if (clipPercentage <= 0) {
            Core.MinMaxLocResult minMax = Core.minMaxLoc(gray);
            minGray = minMax.minVal;
            maxGray = minMax.maxVal;
        } else {
            Mat hist = new Mat();
            Imgproc.calcHist(java.util.Collections.singletonList(gray), new MatOfInt(0),
                    new Mat(), hist, new MatOfInt(histSize), new MatOfFloat(0, 256));

            double[] accumulator = new double[histSize];
            accumulator[0] = hist.get(0, 0)[0];
            for (int i = 1; i < histSize; i++) {
                accumulator[i] = accumulator[i - 1] + hist.get(i, 0)[0];
            }
            double max = accumulator[histSize - 1];
            double clip = clipPercentage * (max / 100.0);
            clip /= 2.0;
            minGray = 0;
            while (minGray < histSize && accumulator[(int) minGray] < clip) {
                minGray++;
            }
            maxGray = histSize - 1;
            while (maxGray >= 0 && accumulator[(int) maxGray] >= (max - clip)) {
                maxGray--;
            }
            hist.release();
        }

        double inputRange = maxGray - minGray;
        double alpha = (histSize - 1) / inputRange;
        double beta = -minGray * alpha;

        Mat result = new Mat();
        src.convertTo(result, -1, alpha, beta);
        gray.release();
        return result;
    }

    /**
     * Simple sharpening using unsharp masking.
     */
    private Mat sharpenImage(Mat src) {
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(src, blurred, new Size(0, 0), 3);
        Mat result = new Mat();
        Core.addWeighted(src, 1.5, blurred, -0.5, 0, result);
        blurred.release();
        return result;
    }
}
