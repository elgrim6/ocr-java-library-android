package mz.nedbank.ocr.utils;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Image preprocessing utilities using OpenCV to improve OCR accuracy
 */
public class ImagePreprocessor {
    private static final Logger logger = LoggerFactory.getLogger(ImagePreprocessor.class);
    
    static {
        try {
            nu.pattern.OpenCV.loadShared();
            logger.info("OpenCV loaded successfully");
        } catch (Exception e) {
            logger.error("Failed to load OpenCV", e);
            throw new RuntimeException("OpenCV initialization failed", e);
        }
    }

    /**
     * Preprocessing configuration
     */
    public static class PreprocessingConfig {
        private boolean enableDenoising = true;
        private boolean enableDeskewing = true;
        private boolean enableContrastEnhancement = true;
        private boolean enableMorphologicalOps = true;
        private double scaleFactor = 2.0;
        private int blurKernelSize = 3;
        private int morphKernelSize = 2;

        // Getters and setters
        public boolean isEnableDenoising() { return enableDenoising; }
        public void setEnableDenoising(boolean enableDenoising) { this.enableDenoising = enableDenoising; }
        
        public boolean isEnableDeskewing() { return enableDeskewing; }
        public void setEnableDeskewing(boolean enableDeskewing) { this.enableDeskewing = enableDeskewing; }
        
        public boolean isEnableContrastEnhancement() { return enableContrastEnhancement; }
        public void setEnableContrastEnhancement(boolean enableContrastEnhancement) { this.enableContrastEnhancement = enableContrastEnhancement; }
        
        public boolean isEnableMorphologicalOps() { return enableMorphologicalOps; }
        public void setEnableMorphologicalOps(boolean enableMorphologicalOps) { this.enableMorphologicalOps = enableMorphologicalOps; }
        
        public double getScaleFactor() { return scaleFactor; }
        public void setScaleFactor(double scaleFactor) { this.scaleFactor = scaleFactor; }
        
        public int getBlurKernelSize() { return blurKernelSize; }
        public void setBlurKernelSize(int blurKernelSize) { this.blurKernelSize = blurKernelSize; }
        
        public int getMorphKernelSize() { return morphKernelSize; }
        public void setMorphKernelSize(int morphKernelSize) { this.morphKernelSize = morphKernelSize; }
    }

    /**
     * Preprocess image with default configuration
     * @param file Input image file
     * @return Preprocessed image as Mat
     */
    public static Mat preprocess(File file) {
        return preprocess(file, new PreprocessingConfig());
    }

    /**
     * Preprocess image with custom configuration
     * @param file Input image file
     * @param config Preprocessing configuration
     * @return Preprocessed image as Mat
     */
    public static Mat preprocess(File file, PreprocessingConfig config) {
        if (!file.exists()) {
            throw new IllegalArgumentException("Input file does not exist: " + file.getAbsolutePath());
        }

        logger.debug("Preprocessing image: {}", file.getName());
        
        // Load image
        Mat src = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.IMREAD_COLOR);
        if (src.empty()) {
            throw new RuntimeException("Failed to load image: " + file.getAbsolutePath());
        }

        return preprocessMat(src, config);
    }

    /**
     * Preprocess a Mat image
     * @param src Source image
     * @param config Preprocessing configuration
     * @return Preprocessed image
     */
    public static Mat preprocessMat(Mat src, PreprocessingConfig config) {
        Mat processed = src.clone();

        try {
            // Step 1: Scale up image for better OCR accuracy
            if (config.getScaleFactor() > 1.0) {
                processed = scaleImage(processed, config.getScaleFactor());
                logger.debug("Scaled image by factor: {}", config.getScaleFactor());
            }

            // Step 2: Convert to grayscale
            if (processed.channels() > 1) {
                Mat gray = new Mat();
                Imgproc.cvtColor(processed, gray, Imgproc.COLOR_BGR2GRAY);
                processed.release();
                processed = gray;
                logger.debug("Converted to grayscale");
            }

            // Step 3: Denoise image
            if (config.isEnableDenoising()) {
                processed = denoiseImage(processed);
                logger.debug("Applied denoising");
            }

            // Step 4: Enhance contrast
            if (config.isEnableContrastEnhancement()) {
                processed = enhanceContrast(processed);
                logger.debug("Enhanced contrast");
            }

            // Step 5: Deskew image
            if (config.isEnableDeskewing()) {
                processed = deskewImage(processed);
                logger.debug("Applied deskewing");
            }

            // Step 6: Apply Gaussian blur
            if (config.getBlurKernelSize() > 0) {
                Mat blurred = new Mat();
                Size kernelSize = new Size(config.getBlurKernelSize(), config.getBlurKernelSize());
                Imgproc.GaussianBlur(processed, blurred, kernelSize, 0);
                processed.release();
                processed = blurred;
                logger.debug("Applied Gaussian blur with kernel size: {}", config.getBlurKernelSize());
            }

            // Step 7: Apply adaptive threshold for binarization
            processed = binarizeImage(processed);
            logger.debug("Applied adaptive thresholding");

            // Step 8: Morphological operations
            if (config.isEnableMorphologicalOps()) {
                processed = applyMorphologicalOps(processed, config.getMorphKernelSize());
                logger.debug("Applied morphological operations");
            }

            logger.debug("Image preprocessing completed");
            return processed;

        } catch (Exception e) {
            logger.error("Error during image preprocessing", e);
            processed.release();
            throw new RuntimeException("Image preprocessing failed", e);
        }
    }

    /**
     * Scale image by given factor
     */
    private static Mat scaleImage(Mat src, double scaleFactor) {
        Mat scaled = new Mat();
        Size newSize = new Size(src.width() * scaleFactor, src.height() * scaleFactor);
        Imgproc.resize(src, scaled, newSize, 0, 0, Imgproc.INTER_CUBIC);
        return scaled;
    }

    /**
     * Denoise image using Non-local Means Denoising
     */
    private static Mat denoiseImage(Mat src) {
        Mat denoised = new Mat();
        Photo.fastNlMeansDenoising(src, denoised, 10, 7, 21);
        return denoised;
    }

    /**
     * Enhance contrast using histogram equalization
     */
    private static Mat enhanceContrast(Mat src) {
        Mat enhanced = new Mat();
        // Use regular histogram equalization instead of CLAHE for compatibility
        Imgproc.equalizeHist(src, enhanced);
        return enhanced;
    }

    /**
     * Deskew image by detecting and correcting skew angle
     */
    private static Mat deskewImage(Mat src) {
        try {
            // Find contours to detect text lines
            Mat edges = new Mat();
            Imgproc.Canny(src, edges, 50, 150);

            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            if (contours.isEmpty()) {
                edges.release();
                hierarchy.release();
                return src;
            }

            // Find the largest contour (likely to be text)
            double maxArea = 0;
            MatOfPoint largestContour = null;
            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);
                if (area > maxArea) {
                    maxArea = area;
                    largestContour = contour;
                }
            }

            if (largestContour != null) {
                // Get minimum area rectangle to find skew angle
                RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(largestContour.toArray()));
                double angle = rect.angle;

                // Correct angle if necessary
                if (angle < -45) {
                    angle += 90;
                }

                // Only correct if angle is significant
                if (Math.abs(angle) > 0.5) {
                    Mat rotated = rotateImage(src, angle);
                    edges.release();
                    hierarchy.release();
                    return rotated;
                }
            }

            edges.release();
            hierarchy.release();
            return src;

        } catch (Exception e) {
            logger.warn("Deskewing failed, returning original image", e);
            return src;
        }
    }

    /**
     * Rotate image by given angle
     */
    private static Mat rotateImage(Mat src, double angle) {
        Point center = new Point(src.width() / 2.0, src.height() / 2.0);
        Mat rotationMatrix = Imgproc.getRotationMatrix2D(center, angle, 1.0);
        
        Mat rotated = new Mat();
        Imgproc.warpAffine(src, rotated, rotationMatrix, src.size(), Imgproc.INTER_CUBIC);
        
        rotationMatrix.release();
        return rotated;
    }

    /**
     * Apply adaptive thresholding for binarization
     */
    private static Mat binarizeImage(Mat src) {
        Mat binary = new Mat();
        Imgproc.adaptiveThreshold(src, binary, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, 
                                 Imgproc.THRESH_BINARY, 11, 2);
        return binary;
    }

    /**
     * Apply morphological operations to clean up the image
     */
    private static Mat applyMorphologicalOps(Mat src, int kernelSize) {
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, 
                                                  new Size(kernelSize, kernelSize));
        
        Mat opened = new Mat();
        Imgproc.morphologyEx(src, opened, Imgproc.MORPH_OPEN, kernel);
        
        Mat closed = new Mat();
        Imgproc.morphologyEx(opened, closed, Imgproc.MORPH_CLOSE, kernel);
        
        kernel.release();
        opened.release();
        return closed;
    }

    /**
     * Save preprocessed image for debugging
     * @param image Image to save
     * @param outputPath Output file path
     */
    public static void saveDebugImage(Mat image, String outputPath) {
        boolean success = Imgcodecs.imwrite(outputPath, image);
        if (success) {
            logger.debug("Debug image saved to: {}", outputPath);
        } else {
            logger.warn("Failed to save debug image to: {}", outputPath);
        }
    }

    /**
     * Get image information
     * @param image Input image
     * @return String containing image dimensions and type
     */
    public static String getImageInfo(Mat image) {
        return String.format("Image: %dx%d, channels: %d, depth: %d", 
                           image.width(), image.height(), image.channels(), image.depth());
    }
}

