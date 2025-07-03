# OCR Java Library - API Documentation

## Overview

This library provides comprehensive OCR capabilities specifically designed for processing Machine Readable Zones (MRZ) from identification documents such as passports, ID cards, and visas. The library combines advanced OpenCV image preprocessing with Tesseract OCR to achieve high accuracy in data extraction.

## Core Classes

### OcrProcessor

The main entry point for document processing operations.

#### Constructors

```java
public OcrProcessor()
```
Creates an OCR processor with default settings (English language).

```java
public OcrProcessor(String tessDataPath, String language)
```
Creates an OCR processor with custom Tesseract data path and language.

**Parameters:**
- `tessDataPath` - Path to tessdata directory (null for default)
- `language` - OCR language code (e.g., "eng", "fra", "deu")

#### Primary Methods

```java
public IdentityDocument processIdentityDocument(File file) throws Exception
```
Processes an identity document image and extracts MRZ data.

**Parameters:**
- `file` - Input image file containing identity document

**Returns:** `IdentityDocument` object with extracted data

**Throws:** `Exception` if processing fails

**Example:**
```java
OcrProcessor processor = new OcrProcessor();
File passportImage = new File("passport.jpg");
IdentityDocument document = processor.processIdentityDocument(passportImage);
```

```java
public IdentityDocument[] processIdentityDocumentBatch(File[] files)
```
Processes multiple identity document files in batch.

**Parameters:**
- `files` - Array of input image files

**Returns:** Array of `IdentityDocument` objects

**Example:**
```java
File[] documents = new File("documents").listFiles();
IdentityDocument[] results = processor.processIdentityDocumentBatch(documents);
```

#### Text Extraction Methods

```java
public String extractText(File file) throws Exception
```
Extracts raw OCR text from an image with preprocessing.

```java
public String extractTextRaw(File file) throws Exception
```
Extracts raw OCR text from an image without preprocessing.

#### Configuration Methods

```java
public void setPreprocessingConfig(ImagePreprocessor.PreprocessingConfig config)
```
Sets the image preprocessing configuration.

```java
public ImagePreprocessor.PreprocessingConfig getPreprocessingConfig()
```
Gets the current preprocessing configuration.

```java
public void setLanguage(String language)
```
Sets the OCR language.

```java
public void setDebugMode(boolean debugMode)
```
Enables or disables debug mode for detailed output.

```java
public void setDebugOutputDir(String debugOutputDir)
```
Sets the directory for debug output files.

```java
public String[] getAvailableLanguages()
```
Returns array of available OCR languages.

```java
public String getProcessorInfo()
```
Returns processor configuration information.

#### Deprecated Methods

```java
@Deprecated
public InvoiceData processInvoice(File file) throws Exception
```
Legacy method for invoice processing. Use `processIdentityDocument()` instead.

```java
@Deprecated
public InvoiceData[] processInvoiceBatch(File[] files)
```
Legacy method for batch invoice processing. Use `processIdentityDocumentBatch()` instead.

### IdentityDocument

Data model representing extracted identity document information.

#### Core Properties

```java
public DocumentType getDocumentType()
public void setDocumentType(DocumentType documentType)
```
Document type (PASSPORT, ID_CARD, VISA, UNKNOWN).

```java
public String getDocumentNumber()
public void setDocumentNumber(String documentNumber)
```
Document number from MRZ.

```java
public String getIssuingCountry()
public void setIssuingCountry(String issuingCountry)
```
ISO 3166-1 alpha-3 country code of issuing country.

```java
public String getNationality()
public void setNationality(String nationality)
```
ISO 3166-1 alpha-3 country code of holder's nationality.

```java
public String getSurname()
public void setSurname(String surname)
```
Holder's surname/family name.

```java
public String getGivenNames()
public void setGivenNames(String givenNames)
```
Holder's given names.

```java
public LocalDate getDateOfBirth()
public void setDateOfBirth(LocalDate dateOfBirth)
```
Holder's date of birth.

```java
public Gender getGender()
public void setGender(Gender gender)
```
Holder's gender (MALE, FEMALE, UNSPECIFIED).

```java
public LocalDate getExpirationDate()
public void setExpirationDate(LocalDate expirationDate)
```
Document expiration date.

```java
public String getPersonalNumber()
public void setPersonalNumber(String personalNumber)
```
Personal number (optional field, usage varies by country).

#### MRZ Properties

```java
public MrzFormat getMrzFormat()
public void setMrzFormat(MrzFormat mrzFormat)
```
MRZ format type (TD1, TD2, TD3).

```java
public String[] getMrzLines()
public void setMrzLines(String[] mrzLines)
```
Raw MRZ lines as extracted from OCR.

```java
public String getRawMrzText()
public void setRawMrzText(String rawMrzText)
```
Complete raw MRZ text.

#### Validation Properties

```java
public boolean isValid()
public void setValid(boolean valid)
```
Overall document validation status.

```java
public boolean isChecksumValid()
public void setChecksumValid(boolean checksumValid)
```
MRZ checksum validation status.

```java
public String getValidationErrors()
public void setValidationErrors(String validationErrors)
```
Validation error messages.

#### Utility Methods

```java
public String getFullName()
```
Returns formatted full name (surname, given names).

```java
public int getAge()
```
Calculates age based on date of birth.

```java
public boolean isExpired()
```
Checks if document is expired.

#### Enumerations

**DocumentType**
- `PASSPORT` - Passport document
- `ID_CARD` - National ID card
- `VISA` - Visa document
- `UNKNOWN` - Unknown or unrecognized type

**Gender**
- `MALE` - Male
- `FEMALE` - Female
- `UNSPECIFIED` - Unspecified or unknown

**MrzFormat**
- `TD1` - 3 lines × 30 characters (ID cards)
- `TD2` - 2 lines × 36 characters (rare format)
- `TD3` - 2 lines × 44 characters (passports)

### MrzDataExtractor

Extracts structured data from MRZ text.

#### Methods

```java
public IdentityDocument extract(String text)
```
Extracts identity document data from OCR text containing MRZ.

**Parameters:**
- `text` - Raw OCR text

**Returns:** `IdentityDocument` object with extracted information

**Example:**
```java
MrzDataExtractor extractor = new MrzDataExtractor();
String mrzText = "P<USADOE<<JOHN<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n123456789<USA8001014M2501017<<<<<<<<<<<<<<<<8";
IdentityDocument document = extractor.extract(mrzText);
```

### ImagePreprocessor

Provides image preprocessing capabilities optimized for MRZ processing.

#### PreprocessingConfig

Configuration class for image preprocessing options.

```java
public class PreprocessingConfig {
    public double getScaleFactor()
    public void setScaleFactor(double scaleFactor)
    
    public boolean isEnableDenoising()
    public void setEnableDenoising(boolean enableDenoising)
    
    public boolean isEnableDeskewing()
    public void setEnableDeskewing(boolean enableDeskewing)
    
    public boolean isEnableContrastEnhancement()
    public void setEnableContrastEnhancement(boolean enableContrastEnhancement)
    
    public boolean isEnableMorphologicalOps()
    public void setEnableMorphologicalOps(boolean enableMorphologicalOps)
    
    public int getBlurKernelSize()
    public void setBlurKernelSize(int blurKernelSize)
    
    public int getMorphKernelSize()
    public void setMorphKernelSize(int morphKernelSize)
}
```

#### Static Methods

```java
public static Mat preprocess(File imageFile, PreprocessingConfig config) throws Exception
```
Preprocesses an image file with the given configuration.

```java
public static void saveDebugImage(Mat image, String outputPath)
```
Saves a debug image to the specified path.

### OcrEngine

Low-level OCR engine wrapper for Tesseract.

#### Methods

```java
public String doOcr(File imageFile) throws TesseractException
```
Performs OCR on an image file.

```java
public String doOcr(Mat image) throws TesseractException
```
Performs OCR on an OpenCV Mat image.

```java
public void setLanguage(String language)
```
Sets the OCR language.

```java
public void setPageSegMode(int mode)
```
Sets the page segmentation mode.

```java
public void setOcrEngineMode(int mode)
```
Sets the OCR engine mode.

```java
public String[] getAvailableLanguages()
```
Returns available OCR languages.

## Usage Examples

### Basic Document Processing

```java
// Initialize processor
OcrProcessor processor = new OcrProcessor();

// Process passport
File passportFile = new File("passport.jpg");
IdentityDocument passport = processor.processIdentityDocument(passportFile);

if (passport.isValid()) {
    System.out.println("Passport Number: " + passport.getDocumentNumber());
    System.out.println("Holder: " + passport.getFullName());
    System.out.println("Nationality: " + passport.getNationality());
    System.out.println("Expires: " + passport.getExpirationDate());
} else {
    System.out.println("Invalid document: " + passport.getValidationErrors());
}
```

### Advanced Configuration

```java
// Create custom preprocessing configuration
ImagePreprocessor.PreprocessingConfig config = new ImagePreprocessor.PreprocessingConfig();
config.setScaleFactor(3.0);              // Higher scaling for small text
config.setEnableDenoising(true);         // Remove noise
config.setEnableDeskewing(true);         // Correct rotation
config.setEnableContrastEnhancement(true); // Improve contrast

// Configure processor
OcrProcessor processor = new OcrProcessor();
processor.setPreprocessingConfig(config);
processor.setDebugMode(true);
processor.setDebugOutputDir("debug_output");
processor.setLanguage("eng");

// Process document
IdentityDocument document = processor.processIdentityDocument(new File("id_card.jpg"));
```

### Batch Processing

```java
// Process multiple documents
File documentsDir = new File("documents");
File[] documentFiles = documentsDir.listFiles((dir, name) -> 
    name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png"));

IdentityDocument[] results = processor.processIdentityDocumentBatch(documentFiles);

// Process results
for (int i = 0; i < results.length; i++) {
    IdentityDocument doc = results[i];
    System.out.printf("File: %s, Valid: %s, Name: %s%n", 
                     documentFiles[i].getName(), 
                     doc.isValid(), 
                     doc.getFullName());
}
```

### Error Handling

```java
try {
    IdentityDocument document = processor.processIdentityDocument(file);
    
    if (document.isValid()) {
        if (document.isChecksumValid()) {
            // Document is fully valid
            processValidDocument(document);
        } else {
            // Document data extracted but checksum failed
            System.out.println("Warning: Checksum validation failed");
            processDocumentWithWarning(document);
        }
    } else {
        // Document could not be processed
        System.out.println("Document processing failed: " + document.getValidationErrors());
    }
    
} catch (Exception e) {
    System.err.println("Processing error: " + e.getMessage());
    e.printStackTrace();
}
```

### Working with Different Document Types

```java
IdentityDocument document = processor.processIdentityDocument(file);

switch (document.getDocumentType()) {
    case PASSPORT:
        System.out.println("Processing passport from " + document.getIssuingCountry());
        break;
    case ID_CARD:
        System.out.println("Processing ID card");
        break;
    case VISA:
        System.out.println("Processing visa document");
        break;
    default:
        System.out.println("Unknown document type");
}

// Check MRZ format
switch (document.getMrzFormat()) {
    case TD1:
        System.out.println("ID card format (3 lines)");
        break;
    case TD2:
        System.out.println("Rare format (2 lines, 36 chars)");
        break;
    case TD3:
        System.out.println("Passport format (2 lines, 44 chars)");
        break;
}
```

## Performance Considerations

### Memory Management

- The library automatically releases OpenCV Mat objects
- For batch processing, consider processing in smaller chunks for large datasets
- Recommended JVM heap size: 1-4GB for production use

### Processing Speed

- Typical processing time: 2-5 seconds per document
- Batch processing is more efficient due to warmup effects
- Debug mode adds overhead but provides valuable troubleshooting information

### Accuracy Optimization

- Use appropriate scaling factor (2.0-4.0) based on image resolution
- Enable denoising for poor quality images
- Enable deskewing for rotated documents
- Adjust preprocessing settings based on document quality

## Error Codes and Troubleshooting

### Common Validation Errors

- "No valid MRZ lines found" - MRZ not detected in OCR text
- "Unknown MRZ format" - MRZ format not recognized
- "Document type is missing" - First character of MRZ not recognized
- "Date of birth is missing" - Date parsing failed
- "Checksum validation failed" - MRZ checksum doesn't match

### Performance Issues

- OutOfMemoryError: Increase JVM heap size
- Slow processing: Reduce image size or adjust preprocessing settings
- Poor accuracy: Try different preprocessing configurations

### Integration Issues

- TesseractException: Ensure Tesseract is properly installed
- UnsatisfiedLinkError: Check OpenCV native library installation
- FileNotFoundException: Verify input file paths and permissions

## Thread Safety

The library is designed to be thread-safe for read operations:

- `OcrProcessor` instances can be shared across threads for processing
- `IdentityDocument` objects are immutable after creation
- Preprocessing configurations should not be modified during processing

For high-concurrency scenarios, consider creating separate `OcrProcessor` instances per thread or using a thread pool with processor pooling.

## Supported Languages

The library supports all Tesseract languages. Common language codes:

- `eng` - English
- `fra` - French  
- `deu` - German
- `spa` - Spanish
- `ita` - Italian
- `por` - Portuguese
- `rus` - Russian
- `chi_sim` - Chinese Simplified
- `chi_tra` - Chinese Traditional
- `jpn` - Japanese
- `kor` - Korean

Install additional language packs as needed:
```bash
sudo apt-get install tesseract-ocr-fra tesseract-ocr-deu
```

