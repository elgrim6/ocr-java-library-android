# OCR Java Library for Identity Documents

A comprehensive Java library for extracting structured data from Machine Readable Zones (MRZ) of identification documents including passports, ID cards, and visas. Built with advanced OpenCV preprocessing and Tesseract OCR integration.

## Features

- **MRZ Data Extraction**: Extract structured data from TD1 (ID cards), TD2, and TD3 (passports) formats
- **Enhanced OpenCV Preprocessing**: Automatic contrast adjustment and sharpening before OCR
- **Lightweight Tesseract Integration**: Uses the system `tesseract` binary to avoid heavy dependencies
- **Comprehensive Validation**: MRZ checksum validation and data integrity checks

## Supported Document Types

- **Passports** (TD3 format): 2 lines × 44 characters
- **ID Cards** (TD1 format): 3 lines × 30 characters  
- **Travel Documents** (TD2 format): 2 lines × 36 characters
- **Visas**: Various MRZ formats

## Quick Start

### Basic Usage

```java
import mz.nedbank.ocr.core.OcrProcessor;
import mz.nedbank.ocr.model.IdentityDocument;

// Initialize processor
OcrProcessor processor = new OcrProcessor();

// Process identity document. Images are automatically preprocessed using
// an enhanced OpenCV pipeline before OCR is performed.
File documentImage = new File("passport.jpg");
IdentityDocument document = processor.processIdentityDocument(documentImage);

// Access extracted data
System.out.println("Name: " + document.getFullName());
System.out.println("Document Number: " + document.getDocumentNumber());
System.out.println("Nationality: " + document.getNationality());
System.out.println("Date of Birth: " + document.getDateOfBirth());
System.out.println("Expiration Date: " + document.getExpirationDate());
System.out.println("Document Type: " + document.getDocumentType());
```

## Installation

### Prerequisites

1. **Java 11 or higher**
2. **Tesseract OCR 4.0+**
   ```bash
   # Ubuntu/Debian
   sudo apt-get install tesseract-ocr tesseract-ocr-eng
   
   # Windows: Download from GitHub releases
   # macOS
   brew install tesseract
   ```

### Maven Dependency

```xml
<dependency>
    <groupId>com.yourcompany</groupId>
    <artifactId>ocr-library</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Build from Source

```bash
git clone <repository-url>
cd ocr-java-library
mvn clean install
```

## Configuration

### Preprocessing Options

The library automatically applies an OpenCV-based preprocessing pipeline before
running OCR. Key operations include:

- **Scaling**: Increase image resolution for better OCR accuracy
- **Denoising**: Remove image artifacts and noise
- **Deskewing**: Automatically correct document rotation
- **Contrast Enhancement**: Improve text visibility
- **Morphological Operations**: Clean up character shapes
- **Binarization**: Convert to optimal black/white format
- **Adaptive Brightness**: Histogram-based contrast adjustment
- **Sharpening**: Unsharp masking to highlight characters

### OCR Settings

- **Page Segmentation**: Optimized for MRZ text blocks
- **Engine Mode**: Uses LSTM neural network engine for best accuracy
- **Language Support**: English, French, German, Spanish, and more

## Data Model

The `IdentityDocument` class provides comprehensive access to extracted MRZ data:

```java
public class IdentityDocument {
    // Core fields
    private DocumentType documentType;      // PASSPORT, ID_CARD, VISA
    private String documentNumber;
    private String issuingCountry;
    private String nationality;
    private String surname;
    private String givenNames;
    private LocalDate dateOfBirth;
    private Gender gender;                  // MALE, FEMALE, UNSPECIFIED
    private LocalDate expirationDate;
    private String personalNumber;
    
    // Validation
    private boolean isValid;
    private boolean checksumValid;
    private String validationErrors;
    
    // Utility methods
    public String getFullName();
    public int getAge();
    public boolean isExpired();
}
```

## Error Handling

The library provides comprehensive error handling and validation:

```java
IdentityDocument document = processor.processIdentityDocument(file);

if (document.isValid()) {
    // Process valid document
    if (document.isChecksumValid()) {
        System.out.println("Document passed all validations");
    } else {
        System.out.println("Warning: Checksum validation failed");
    }
} else {
    System.out.println("Errors: " + document.getValidationErrors());
}
```

## Performance

- **Typical Processing Time**: 2-5 seconds per document
- **Memory Usage**: ~50MB base + 10-50MB per document
- **Recommended Heap**: 1-4GB for production use

## Troubleshooting

### Common Issues

1. **Tesseract not found**: Ensure Tesseract is installed and in PATH
2. **Poor OCR accuracy**: Try adjusting preprocessing settings
3. **Memory issues**: Increase JVM heap size if processing large images
4. **Language errors**: Install required Tesseract language packs


## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues and questions:
- Check the troubleshooting guide
- Review the API documentation
- Open an issue on GitHub

## Changelog

### Version 1.0.0
- Initial release focused on identity document processing
- Support for TD1, TD2, and TD3 MRZ formats
- Advanced OpenCV preprocessing pipeline
- Comprehensive validation and error handling
- Batch processing capabilities
- Debug mode with detailed output

