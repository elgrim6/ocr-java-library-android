# OCR Java Library - Troubleshooting Guide

This guide helps resolve common issues when using the OCR Java Library.

## Table of Contents

1. [Installation Issues](#installation-issues)
2. [OCR Processing Problems](#ocr-processing-problems)
3. [Image Preprocessing Issues](#image-preprocessing-issues)
4. [Performance Problems](#performance-problems)
5. [Language Support Issues](#language-support-issues)
6. [Memory and Resource Issues](#memory-and-resource-issues)
7. [Common Error Messages](#common-error-messages)

## Installation Issues

### Maven Build Fails

**Problem:** Maven cannot resolve dependencies or build fails.

**Solutions:**
1. Check internet connection for dependency downloads
2. Clear Maven cache: `mvn clean install -U`
3. Verify Java version (requires Java 11+): `java -version`
4. Check Maven version (requires 3.6+): `mvn -version`

### OpenCV Loading Error

**Problem:** `UnsatisfiedLinkError` or OpenCV initialization fails.

**Solutions:**
1. Ensure OpenCV dependency is correctly included in pom.xml
2. Check system architecture compatibility (x64 vs x86)
3. Verify no conflicting OpenCV installations
4. Try different OpenCV version in pom.xml

```xml
<dependency>
    <groupId>org.openpnp</groupId>
    <artifactId>opencv</artifactId>
    <version>4.6.0-0</version>
</dependency>
```

### Tesseract Not Found

**Problem:** `TesseractException` with "Tesseract not found" message.

**Solutions:**
1. Install Tesseract OCR engine:
   - Ubuntu/Debian: `sudo apt-get install tesseract-ocr`
   - Windows: Download from GitHub releases
   - macOS: `brew install tesseract`

2. Set tessdata path explicitly:
```java
OcrProcessor processor = new OcrProcessor("/usr/share/tesseract-ocr/tessdata", "eng");
```

3. Download language data files if missing

## OCR Processing Problems

### Poor OCR Accuracy

**Problem:** Extracted text contains many errors or is incomplete.

**Solutions:**
1. **Improve image quality:**
   - Use higher resolution images (300+ DPI)
   - Ensure good contrast between text and background
   - Remove noise and artifacts

2. **Adjust preprocessing settings:**
```java
ImagePreprocessor.PreprocessingConfig config = new ImagePreprocessor.PreprocessingConfig();
config.setScaleFactor(3.0);  // Increase for small text
config.setEnableDenoising(true);
config.setEnableContrastEnhancement(true);
processor.setPreprocessingConfig(config);
```

3. **Use correct language:**
```java
processor.setLanguage("eng");  // Set appropriate language
```

4. **Enable debug mode to analyze preprocessing:**
```java
processor.setDebugMode(true);
processor.setDebugOutputDir("debug_images");
```

### No Text Extracted

**Problem:** OCR returns empty string or null.

**Solutions:**
1. Check if image file exists and is readable
2. Verify image format is supported (PNG, JPG, TIFF, BMP)
3. Ensure image contains actual text (not just graphics)
4. Try processing without preprocessing:
```java
String rawText = processor.extractTextRaw(file);
```

### Incorrect Data Extraction

**Problem:** OCR text is correct but structured data extraction fails.

**Solutions:**
1. **Check OCR text format:**
```java
String text = processor.extractText(file);
System.out.println("OCR Text:\n" + text);
```

2. **Customize extraction patterns:**
   - Extend `InvoiceDataExtractor` class
   - Add custom regex patterns for your document format

3. **Verify document language and format compatibility**

## Image Preprocessing Issues

### Preprocessing Crashes

**Problem:** Application crashes during image preprocessing.

**Solutions:**
1. **Check image file integrity:**
```java
Mat image = Imgcodecs.imread(file.getAbsolutePath());
if (image.empty()) {
    System.err.println("Failed to load image");
}
```

2. **Reduce preprocessing complexity:**
```java
ImagePreprocessor.PreprocessingConfig config = new ImagePreprocessor.PreprocessingConfig();
config.setEnableDenoising(false);
config.setEnableDeskewing(false);
config.setScaleFactor(1.0);
```

3. **Check available memory for large images**

### Deskewing Problems

**Problem:** Deskewing produces distorted images or fails.

**Solutions:**
1. **Disable deskewing for already straight documents:**
```java
config.setEnableDeskewing(false);
```

2. **Check if document has clear text lines for angle detection**

3. **Manually specify rotation if known:**
```java
// Custom rotation implementation needed
```

### Slow Preprocessing

**Problem:** Image preprocessing takes too long.

**Solutions:**
1. **Reduce scale factor:**
```java
config.setScaleFactor(1.5);  // Instead of 3.0
```

2. **Disable expensive operations:**
```java
config.setEnableDenoising(false);
config.setEnableContrastEnhancement(false);
```

3. **Resize large images before processing**

## Performance Problems

### High Memory Usage

**Problem:** Application consumes excessive memory.

**Solutions:**
1. **Process images in batches instead of all at once**
2. **Explicitly release Mat objects:**
```java
Mat image = ImagePreprocessor.preprocess(file);
try {
    // Use image
} finally {
    image.release();
}
```

3. **Reduce image scale factor**
4. **Monitor memory usage and implement cleanup**

### Slow Processing

**Problem:** OCR processing is too slow for production use.

**Solutions:**
1. **Use optimized preprocessing configuration:**
```java
// Fast configuration
ImagePreprocessor.PreprocessingConfig fastConfig = new ImagePreprocessor.PreprocessingConfig();
fastConfig.setScaleFactor(1.5);
fastConfig.setEnableDenoising(false);
fastConfig.setEnableDeskewing(false);
```

2. **Implement parallel processing:**
```java
// Process multiple files concurrently
ExecutorService executor = Executors.newFixedThreadPool(4);
```

3. **Cache processor instances instead of creating new ones**

4. **Use appropriate page segmentation mode:**
```java
// For single text block
processor.setPageSegMode(6);
```

## Language Support Issues

### Language Not Available

**Problem:** Desired language is not available for OCR.

**Solutions:**
1. **Check available languages:**
```java
String[] languages = processor.getAvailableLanguages();
System.out.println("Available: " + String.join(", ", languages));
```

2. **Install additional language packs:**
   - Ubuntu: `sudo apt-get install tesseract-ocr-fra` (for French)
   - Download from Tesseract GitHub repository

3. **Verify tessdata path contains language files**

### Mixed Language Documents

**Problem:** Document contains multiple languages.

**Solutions:**
1. **Use combined language codes:**
```java
processor.setLanguage("eng+fra+deu");  // English + French + German
```

2. **Process different sections separately with appropriate languages**

3. **Use script-based detection if available**

## Memory and Resource Issues

### OutOfMemoryError

**Problem:** Application runs out of memory during processing.

**Solutions:**
1. **Increase JVM heap size:**
```bash
java -Xmx4g -jar your-application.jar
```

2. **Process smaller batches:**
```java
// Process 10 files at a time instead of 100
```

3. **Implement proper resource cleanup:**
```java
try (FileInputStream fis = new FileInputStream(file)) {
    // Process file
} // Automatic cleanup
```

### File Handle Leaks

**Problem:** Too many open files error.

**Solutions:**
1. **Ensure proper file closure:**
```java
// Use try-with-resources
try (InputStream is = new FileInputStream(file)) {
    // Process
}
```

2. **Clean up temporary files:**
```java
File tempFile = File.createTempFile("ocr", ".png");
try {
    // Use temp file
} finally {
    tempFile.delete();
}
```

## Common Error Messages

### "Failed to load OpenCV native library"

**Cause:** OpenCV native libraries not found or incompatible.

**Solution:**
1. Check system architecture (32-bit vs 64-bit)
2. Verify OpenCV dependency version
3. Try different OpenCV artifact:
```xml
<dependency>
    <groupId>org.bytedeco</groupId>
    <artifactId>opencv-platform</artifactId>
    <version>4.6.0-1.5.8</version>
</dependency>
```

### "TesseractException: Tesseract not found"

**Cause:** Tesseract OCR engine not installed or not in PATH.

**Solution:**
1. Install Tesseract OCR
2. Add to system PATH
3. Set explicit path in code

### "IllegalArgumentException: Input file does not exist"

**Cause:** File path is incorrect or file doesn't exist.

**Solution:**
1. Verify file path is correct
2. Check file permissions
3. Use absolute paths instead of relative paths

### "Image preprocessing failed"

**Cause:** OpenCV cannot process the image.

**Solution:**
1. Check image format compatibility
2. Verify image is not corrupted
3. Try with simpler preprocessing configuration

### "java.lang.UnsatisfiedLinkError"

**Cause:** Native library loading issues.

**Solution:**
1. Check Java architecture matches native libraries
2. Verify no conflicting native libraries
3. Try different dependency versions

## Debug Mode Usage

Enable debug mode to diagnose issues:

```java
processor.setDebugMode(true);
processor.setDebugOutputDir("debug_output");
```

This will save:
- Preprocessed images for visual inspection
- OCR text output for analysis
- Processing logs for troubleshooting

## Getting Help

If you continue to experience issues:

1. **Check logs:** Enable debug mode and examine output
2. **Test with sample images:** Use provided test images first
3. **Verify environment:** Ensure all dependencies are correctly installed
4. **Simplify configuration:** Start with minimal settings and add complexity gradually
5. **Check versions:** Ensure compatible versions of Java, Maven, Tesseract, and OpenCV

## Performance Benchmarks

Typical processing times on modern hardware:

- Simple invoice (300 DPI, A4): 2-5 seconds
- Complex document with preprocessing: 5-15 seconds
- Batch processing: 1-3 seconds per document (after warmup)

If your processing times significantly exceed these, review the performance optimization suggestions above.

