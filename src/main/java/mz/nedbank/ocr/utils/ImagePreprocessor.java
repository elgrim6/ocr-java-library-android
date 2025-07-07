package mz.nedbank.ocr.utils;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;
import org.opencv.imgproc.CLAHE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced image preprocessing utilities using OpenCV to improve OCR accuracy
 */
public class ImagePreprocessor {
    private static final Logger logger = LoggerFactory.getLogger(ImagePreprocessor.class);

    // Constants for better OCR results
    private static final double MIN_SCALE_FACTOR = 1.5;
    private static final double MAX_SCALE_FACTOR = 4.0;
    private static final int TARGET_HEIGHT = 800; // Optimal height for most OCR engines

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
     * Enhanced preprocessing configuration
     */
    public static class PreprocessingConfig {
        private boolean enableDenoising = true;
        private boolean enableDeskewing = true;
        private boolean enableContrastEnhancement = true;
        private boolean enableMorphologicalOps = true;
        private boolean enableUnsharpMask = true;
        private boolean enableBorderRemoval = true;
        private boolean enableTextRegionDetection = true;
        private boolean enableAdaptiveScaling = true;

        private double scaleFactor = 2.0;
        private int blurKernelSize = 1; // Reduced for better text clarity
        private int morphKernelSize = 1; // Reduced to preserve text details
        private double claheClipLimit = 2.0;
        private int claheTileSize = 8;
        private ThresholdMethod thresholdMethod = ThresholdMethod.ADAPTIVE_GAUSSIAN;
        private double unsharpAmount = 1.5;
        private double unsharpRadius = 1.0;
        private double unsharpThreshold = 0.0;

        public enum ThresholdMethod {
            ADAPTIVE_GAUSSIAN,
            ADAPTIVE_MEAN,
            OTSU,
            TRIANGLE,
            COMBINED
        }

        // Getters and setters
        public boolean isEnableDenoising() { return enableDenoising; }
        public void setEnableDenoising(boolean enableDenoising) { this.enableDenoising = enableDenoising; }

        public boolean isEnableDeskewing() { return enableDeskewing; }
        public void setEnableDeskewing(boolean enableDeskewing) { this.enableDeskewing = enableDeskewing; }

        public boolean isEnableContrastEnhancement() { return enableContrastEnhancement; }
        public void setEnableContrastEnhancement(boolean enableContrastEnhancement) { this.enableContrastEnhancement = enableContrastEnhancement; }

        public boolean isEnableMorphologicalOps() { return enableMorphologicalOps; }
        public void setEnableMorphologicalOps(boolean enableMorphologicalOps) { this.enableMorphologicalOps = enableMorphologicalOps; }

        public boolean isEnableUnsharpMask() { return enableUnsharpMask; }
        public void setEnableUnsharpMask(boolean enableUnsharpMask) { this.enableUnsharpMask = enableUnsharpMask; }

        public boolean isEnableBorderRemoval() { return enableBorderRemoval; }
        public void setEnableBorderRemoval(boolean enableBorderRemoval) { this.enableBorderRemoval = enableBorderRemoval; }

        public boolean isEnableTextRegionDetection() { return enableTextRegionDetection; }
        public void setEnableTextRegionDetection(boolean enableTextRegionDetection) { this.enableTextRegionDetection = enableTextRegionDetection; }

        public boolean isEnableAdaptiveScaling() { return enableAdaptiveScaling; }
        public void setEnableAdaptiveScaling(boolean enableAdaptiveScaling) { this.enableAdaptiveScaling = enableAdaptiveScaling; }

        public double getScaleFactor() { return scaleFactor; }
        public void setScaleFactor(double scaleFactor) { this.scaleFactor = scaleFactor; }

        public int getBlurKernelSize() { return blurKernelSize; }
        public void setBlurKernelSize(int blurKernelSize) { this.blurKernelSize = blurKernelSize; }

        public int getMorphKernelSize() { return morphKernelSize; }
        public void setMorphKernelSize(int morphKernelSize) { this.morphKernelSize = morphKernelSize; }

        public double getClaheClipLimit() { return claheClipLimit; }
        public void setClaheClipLimit(double claheClipLimit) { this.claheClipLimit = claheClipLimit; }

        public int getClaheTileSize() { return claheTileSize; }
        public void setClaheTileSize(int claheTileSize) { this.claheTileSize = claheTileSize; }

        public ThresholdMethod getThresholdMethod() { return thresholdMethod; }
        public void setThresholdMethod(ThresholdMethod thresholdMethod) { this.thresholdMethod = thresholdMethod; }

        public double getUnsharpAmount() { return unsharpAmount; }
        public void setUnsharpAmount(double unsharpAmount) { this.unsharpAmount = unsharpAmount; }

        public double getUnsharpRadius() { return unsharpRadius; }
        public void setUnsharpRadius(double unsharpRadius) { this.unsharpRadius = unsharpRadius; }

        public double getUnsharpThreshold() { return unsharpThreshold; }
        public void setUnsharpThreshold(double unsharpThreshold) { this.unsharpThreshold = unsharpThreshold; }
    }

    /**
     * Preprocess image with default configuration
     */
    public static Mat preprocess(File file) {
        return preprocess(file, new PreprocessingConfig());
    }

    /**
     * Preprocess image with custom configuration
     */
    public static Mat preprocess(File file, PreprocessingConfig config) {
        if (!file.exists()) {
            throw new IllegalArgumentException("Input file does not exist: " + file.getAbsolutePath());
        }
        logger.info("Preprocessing image: {}", file.getName());

        Mat src = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.IMREAD_COLOR);
        if (src.empty()) {
            throw new RuntimeException("Failed to load image: " + file.getAbsolutePath());
        }

        logger.debug("Original image: {}", getImageInfo(src));
        return preprocessMat(src, config);
    }

    /**
     * Enhanced preprocessing pipeline
     */
    public static Mat preprocessMat(Mat src, PreprocessingConfig config) {
        Mat processed = src.clone();

        try {
            // Step 1: Remove borders and crop to content
            if (config.isEnableBorderRemoval()) {
                processed = removeBorders(processed);
                logger.debug("Removed borders");
            }

            // Step 2: Adaptive scaling based on image size
            if (config.isEnableAdaptiveScaling()) {
                processed = adaptiveScale(processed);
                logger.debug("Applied adaptive scaling");
            } else if (config.getScaleFactor() > 1.0) {
                processed = scaleImage(processed, config.getScaleFactor());
                logger.debug("Scaled image by factor: {}", config.getScaleFactor());
            }

            // Step 3: Convert to grayscale
            if (processed.channels() > 1) {
                Mat gray = new Mat();
                Imgproc.cvtColor(processed, gray, Imgproc.COLOR_BGR2GRAY);
                processed.release();
                processed = gray;
                logger.debug("Converted to grayscale");
            }

            // Step 4: Light denoising (preserve text details)
            if (config.isEnableDenoising()) {
                processed = denoiseImageAdvanced(processed);
                logger.debug("Applied advanced denoising");
            }

            // Step 5: Enhance contrast using CLAHE
            if (config.isEnableContrastEnhancement()) {
                processed = enhanceContrastCLAHE(processed, config.getClaheClipLimit(), config.getClaheTileSize());
                logger.debug("Enhanced contrast with CLAHE");
            }

            // Step 6: Unsharp masking for text sharpening
            if (config.isEnableUnsharpMask()) {
                processed = applyUnsharpMask(processed, config.getUnsharpAmount(),
                        config.getUnsharpRadius(), config.getUnsharpThreshold());
                logger.debug("Applied unsharp masking");
            }

            // Step 7: Deskew image
            if (config.isEnableDeskewing()) {
                processed = deskewImageAdvanced(processed);
                logger.debug("Applied advanced deskewing");
            }

            // Step 8: Minimal blur only if needed
            if (config.getBlurKernelSize() > 0) {
                processed = applySelectiveBlur(processed, config.getBlurKernelSize());
                logger.debug("Applied selective blur");
            }

            // Step 9: Advanced binarization
            processed = binarizeImageAdvanced(processed, config.getThresholdMethod());
            logger.debug("Applied advanced binarization: {}", config.getThresholdMethod());

            // Step 10: Minimal morphological operations
            if (config.isEnableMorphologicalOps()) {
                processed = applyMorphologicalOpsAdvanced(processed, config.getMorphKernelSize());
                logger.debug("Applied advanced morphological operations");
            }

            // Step 11: Text region enhancement
            if (config.isEnableTextRegionDetection()) {
                processed = enhanceTextRegions(processed);
                logger.debug("Enhanced text regions");
            }

            logger.debug("Final processed image: {}", getImageInfo(processed));
            return processed;

        } catch (Exception e) {
            logger.error("Error during image preprocessing", e);
            processed.release();
            throw new RuntimeException("Image preprocessing failed", e);
        }
    }

    /**
     * Remove borders and crop to content
     */
    private static Mat removeBorders(Mat src) {
        Mat gray = new Mat();
        if (src.channels() > 1) {
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        } else {
            gray = src.clone();
        }

        // Find content boundaries
        Mat binary = new Mat();
        Imgproc.threshold(gray, binary, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);

        // Find contours
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        if (!contours.isEmpty()) {
            // Find bounding box of all contours
            Rect boundingBox = Imgproc.boundingRect(contours.get(0));
            for (int i = 1; i < contours.size(); i++) {
                Rect rect = Imgproc.boundingRect(contours.get(i));
                boundingBox = new Rect(
                        Math.min(boundingBox.x, rect.x),
                        Math.min(boundingBox.y, rect.y),
                        Math.max(boundingBox.x + boundingBox.width, rect.x + rect.width) - Math.min(boundingBox.x, rect.x),
                        Math.max(boundingBox.y + boundingBox.height, rect.y + rect.height) - Math.min(boundingBox.y, rect.y)
                );
            }

            // Add small padding
            int padding = 10;
            boundingBox.x = Math.max(0, boundingBox.x - padding);
            boundingBox.y = Math.max(0, boundingBox.y - padding);
            boundingBox.width = Math.min(src.width() - boundingBox.x, boundingBox.width + 2 * padding);
            boundingBox.height = Math.min(src.height() - boundingBox.y, boundingBox.height + 2 * padding);

            Mat cropped = new Mat(src, boundingBox);
            gray.release();
            binary.release();
            hierarchy.release();
            return cropped;
        }

        gray.release();
        binary.release();
        hierarchy.release();
        return src;
    }

    /**
     * Adaptive scaling based on image dimensions
     */
    private static Mat adaptiveScale(Mat src) {
        double currentHeight = src.height();
        double scaleFactor = 1.0;

        if (currentHeight < TARGET_HEIGHT) {
            scaleFactor = Math.min(MAX_SCALE_FACTOR, (double) TARGET_HEIGHT / currentHeight);
            scaleFactor = Math.max(MIN_SCALE_FACTOR, scaleFactor);
        }

        if (scaleFactor > 1.0) {
            return scaleImage(src, scaleFactor);
        }

        return src;
    }

    /**
     * Scale image with high-quality interpolation
     */
    private static Mat scaleImage(Mat src, double scaleFactor) {
        Mat scaled = new Mat();
        Size newSize = new Size(src.width() * scaleFactor, src.height() * scaleFactor);
        Imgproc.resize(src, scaled, newSize, 0, 0, Imgproc.INTER_CUBIC);
        return scaled;
    }

    /**
     * Advanced denoising with edge preservation
     */
    private static Mat denoiseImageAdvanced(Mat src) {
        Mat denoised = new Mat();
        // Use lighter denoising to preserve text details
        Photo.fastNlMeansDenoising(src, denoised, 3, 7, 21);
        return denoised;
    }

    /**
     * Enhanced contrast using CLAHE (Contrast Limited Adaptive Histogram Equalization)
     */
    private static Mat enhanceContrastCLAHE(Mat src, double clipLimit, int tileSize) {
        Mat enhanced = new Mat();
        try {
            // Create CLAHE object
            CLAHE clahe = Imgproc.createCLAHE(clipLimit, new Size(tileSize, tileSize));
            clahe.apply(src, enhanced);
            return enhanced;
        } catch (Exception e) {
            logger.warn("CLAHE failed, falling back to regular histogram equalization", e);
            Imgproc.equalizeHist(src, enhanced);
            return enhanced;
        }
    }

    /**
     * Apply unsharp masking for text sharpening
     */
    private static Mat applyUnsharpMask(Mat src, double amount, double radius, double threshold) {
        Mat blurred = new Mat();
        Mat unsharpMask = new Mat();
        Mat result = new Mat();

        // Create Gaussian blur
        int kernelSize = (int) Math.round(radius * 2) * 2 + 1;
        Imgproc.GaussianBlur(src, blurred, new Size(kernelSize, kernelSize), radius);

        // Create unsharp mask
        Core.subtract(src, blurred, unsharpMask);

        // Apply threshold if specified
        if (threshold > 0) {
            Mat mask = new Mat();
            Imgproc.threshold(unsharpMask, mask, threshold, 255, Imgproc.THRESH_BINARY);
            unsharpMask.copyTo(unsharpMask, mask);
            mask.release();
        }

        // Apply unsharp mask
        Core.addWeighted(src, 1.0, unsharpMask, amount, 0, result);

        blurred.release();
        unsharpMask.release();
        return result;
    }

    /**
     * Advanced deskewing with improved angle detection
     */
    private static Mat deskewImageAdvanced(Mat src) {
        try {
            // Use morphological operations to enhance text lines
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(20, 1));
            Mat morphed = new Mat();
            Imgproc.morphologyEx(src, morphed, Imgproc.MORPH_CLOSE, kernel);

            // Find edges
            Mat edges = new Mat();
            Imgproc.Canny(morphed, edges, 50, 150);

            // Use HoughLines to detect lines
            Mat lines = new Mat();
            Imgproc.HoughLines(edges, lines, 1, Math.PI / 180, 100);

            if (lines.rows() > 0) {
                // Calculate average angle
                double angleSum = 0;
                int validLines = 0;

                for (int i = 0; i < lines.rows(); i++) {
                    double[] data = lines.get(i, 0);
                    double theta = data[1];
                    double angle = theta * 180 / Math.PI - 90;

                    // Filter out vertical lines
                    if (Math.abs(angle) < 45) {
                        angleSum += angle;
                        validLines++;
                    }
                }

                if (validLines > 0) {
                    double avgAngle = angleSum / validLines;

                    // Only rotate if angle is significant
                    if (Math.abs(avgAngle) > 0.5) {
                        Mat rotated = rotateImage(src, avgAngle);
                        kernel.release();
                        morphed.release();
                        edges.release();
                        lines.release();
                        return rotated;
                    }
                }
            }

            kernel.release();
            morphed.release();
            edges.release();
            lines.release();
            return src;

        } catch (Exception e) {
            logger.warn("Advanced deskewing failed, returning original image", e);
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
        Imgproc.warpAffine(src, rotated, rotationMatrix, src.size(),
                Imgproc.INTER_CUBIC, Core.BORDER_CONSTANT, new Scalar(255, 255, 255));

        rotationMatrix.release();
        return rotated;
    }

    /**
     * Apply selective blur to smooth noise while preserving text edges
     */
    private static Mat applySelectiveBlur(Mat src, int kernelSize) {
        if (kernelSize <= 1) return src;

        Mat blurred = new Mat();
        Size kernel = new Size(kernelSize, kernelSize);

        // Use bilateral filter for edge-preserving smoothing
        try {
            Imgproc.bilateralFilter(src, blurred, kernelSize, kernelSize * 2, kernelSize / 2);
            return blurred;
        } catch (Exception e) {
            // Fallback to Gaussian blur
            Imgproc.GaussianBlur(src, blurred, kernel, 0);
            return blurred;
        }
    }

    /**
     * Advanced binarization with multiple methods
     */
    private static Mat binarizeImageAdvanced(Mat src, PreprocessingConfig.ThresholdMethod method) {
        Mat binary = new Mat();

        switch (method) {
            case ADAPTIVE_GAUSSIAN:
                Imgproc.adaptiveThreshold(src, binary, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                        Imgproc.THRESH_BINARY, 15, 4);
                break;

            case ADAPTIVE_MEAN:
                Imgproc.adaptiveThreshold(src, binary, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C,
                        Imgproc.THRESH_BINARY, 15, 4);
                break;

            case OTSU:
                Imgproc.threshold(src, binary, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
                break;

            case TRIANGLE:
                Imgproc.threshold(src, binary, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_TRIANGLE);
                break;

            case COMBINED:
                // Combine OTSU and adaptive thresholding
                Mat otsu = new Mat();
                Mat adaptive = new Mat();

                Imgproc.threshold(src, otsu, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
                Imgproc.adaptiveThreshold(src, adaptive, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                        Imgproc.THRESH_BINARY, 15, 4);

                Core.bitwise_and(otsu, adaptive, binary);
                otsu.release();
                adaptive.release();
                break;

            default:
                Imgproc.adaptiveThreshold(src, binary, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                        Imgproc.THRESH_BINARY, 15, 4);
        }

        return binary;
    }

    /**
     * Advanced morphological operations with minimal text distortion
     */
    private static Mat applyMorphologicalOpsAdvanced(Mat src, int kernelSize) {
        if (kernelSize <= 0) return src;

        // Use very small kernel to avoid text distortion
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE,
                new Size(kernelSize, kernelSize));

        // Light opening to remove small noise
        Mat opened = new Mat();
        Imgproc.morphologyEx(src, opened, Imgproc.MORPH_OPEN, kernel, new Point(-1, -1), 1);

        kernel.release();
        return opened;
    }

    /**
     * Enhance text regions using connected components
     */
    private static Mat enhanceTextRegions(Mat src) {
        try {
            Mat labels = new Mat();
            Mat stats = new Mat();
            Mat centroids = new Mat();

            int numComponents = Imgproc.connectedComponentsWithStats(src, labels, stats, centroids);

            Mat enhanced = src.clone();

            // Filter components by size and aspect ratio
            for (int i = 1; i < numComponents; i++) {
                double[] stat = stats.get(i, 0);
                int width = (int) stat[2];
                int height = (int) stat[3];
                int area = (int) stat[4];

                // Remove very small or very large components
                if (area < 10 || area > src.total() * 0.1) {
                    Mat mask = new Mat();
                    Core.compare(labels, new Scalar(i), mask, Core.CMP_EQ);
                    enhanced.setTo(new Scalar(255), mask);
                    mask.release();
                }
            }

            labels.release();
            stats.release();
            centroids.release();
            return enhanced;

        } catch (Exception e) {
            logger.warn("Text region enhancement failed, returning original", e);
            return src;
        }
    }

    /**
     * Save preprocessed image for debugging
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
     * Get detailed image information
     */
    public static String getImageInfo(Mat image) {
        return String.format("Image: %dx%d, channels: %d, depth: %d, type: %d",
                image.width(), image.height(), image.channels(), image.depth(), image.type());
    }

    /**
     * Analyze image quality for OCR
     */
    public static ImageQuality analyzeImageQuality(Mat image) {
        ImageQuality quality = new ImageQuality();

        // Calculate sharpness using Laplacian variance
        Mat laplacian = new Mat();
        Imgproc.Laplacian(image, laplacian, CvType.CV_64F);
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stddev = new MatOfDouble();
        Core.meanStdDev(laplacian, mean, stddev);
        quality.sharpness = stddev.get(0, 0)[0] * stddev.get(0, 0)[0];

        // Calculate contrast
        Core.meanStdDev(image, mean, stddev);
        quality.contrast = stddev.get(0, 0)[0];

        // Calculate brightness
        quality.brightness = mean.get(0, 0)[0];

        laplacian.release();
        return quality;
    }

    public static class ImageQuality {
        public double sharpness;
        public double contrast;
        public double brightness;

        @Override
        public String toString() {
            return String.format("Quality - Sharpness: %.2f, Contrast: %.2f, Brightness: %.2f",
                    sharpness, contrast, brightness);
        }
    }
}