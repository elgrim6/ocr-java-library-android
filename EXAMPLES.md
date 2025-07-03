# OCR Java Library - Usage Examples

This document provides comprehensive examples for using the OCR Java Library to process identity documents and extract MRZ data.

## Table of Contents

1. [Basic Examples](#basic-examples)
2. [Advanced Configuration](#advanced-configuration)
3. [Batch Processing](#batch-processing)
4. [Error Handling](#error-handling)
5. [Integration Examples](#integration-examples)
6. [Performance Optimization](#performance-optimization)
7. [Troubleshooting Examples](#troubleshooting-examples)

## Basic Examples

### Simple Passport Processing

```java
import mz.nedbank.ocr.core.ocr.OcrProcessor;
import mz.nedbank.ocr.model.ocr.IdentityDocument;
import java.io.File;

public class BasicPassportExample {
    public static void main(String[] args) {
        try {
            // Initialize the OCR processor
            OcrProcessor processor = new OcrProcessor();
            
            // Process a passport image
            File passportImage = new File("passport.jpg");
            IdentityDocument document = processor.processIdentityDocument(passportImage);
            
            // Display extracted information
            if (document.isValid()) {
                System.out.println("=== Passport Information ===");
                System.out.println("Full Name: " + document.getFullName());
                System.out.println("Document Number: " + document.getDocumentNumber());
                System.out.println("Nationality: " + document.getNationality());
                System.out.println("Date of Birth: " + document.getDateOfBirth());
                System.out.println("Gender: " + document.getGender());
                System.out.println("Expiration Date: " + document.getExpirationDate());
                System.out.println("Issuing Country: " + document.getIssuingCountry());
                System.out.println("Age: " + document.getAge() + " years");
                System.out.println("Expired: " + (document.isExpired() ? "Yes" : "No"));
            } else {
                System.out.println("Document processing failed: " + document.getValidationErrors());
            }
            
        } catch (Exception e) {
            System.err.println("Error processing passport: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

### ID Card Processing

```java
import mz.nedbank.ocr.core.ocr.OcrProcessor;
import mz.nedbank.ocr.model.ocr.IdentityDocument;
import java.io.File;

public class IdCardExample {
    public static void main(String[] args) {
        try {
            OcrProcessor processor = new OcrProcessor();
            
            // Process an ID card (TD1 format)
            File idCardImage = new File("id_card.jpg");
            IdentityDocument document = processor.processIdentityDocument(idCardImage);
            
            if (document.isValid()) {
                System.out.println("=== ID Card Information ===");
                System.out.println("Document Type: " + document.getDocumentType());
                System.out.println("MRZ Format: " + document.getMrzFormat());
                System.out.println("Surname: " + document.getSurname());
                System.out.println("Given Names: " + document.getGivenNames());
                System.out.println("Document Number: " + document.getDocumentNumber());
                System.out.println("Personal Number: " + document.getPersonalNumber());
                System.out.println("Nationality: " + document.getNationality());
                System.out.println("Date of Birth: " + document.getDateOfBirth());
                System.out.println("Gender: " + document.getGender());
                System.out.println("Expiration Date: " + document.getExpirationDate());
                
                // Display raw MRZ lines
                System.out.println("\n=== Raw MRZ Lines ===");
                String[] mrzLines = document.getMrzLines();
                for (int i = 0; i < mrzLines.length; i++) {
                    System.out.println("Line " + (i + 1) + ": " + mrzLines[i]);
                }
            } else {
                System.out.println("ID card processing failed: " + document.getValidationErrors());
            }
            
        } catch (Exception e) {
            System.err.println("Error processing ID card: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

### Direct MRZ Text Processing

```java
import mz.nedbank.ocr.extractor.ocr.MrzDataExtractor;
import mz.nedbank.ocr.model.ocr.IdentityDocument;

public class DirectMrzExample {
    public static void main(String[] args) {
        // Sample MRZ text (TD3 passport format)
        String mrzText = "P<USADOE<<JOHN<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n" +
                        "123456789<USA8001014M2501017<<<<<<<<<<<<<<<<8";
        
        // Extract data directly from MRZ text
        MrzDataExtractor extractor = new MrzDataExtractor();
        IdentityDocument document = extractor.extract(mrzText);
        
        if (document.isValid()) {
            System.out.println("=== MRZ Extraction Results ===");
            System.out.println("Document Type: " + document.getDocumentType());
            System.out.println("MRZ Format: " + document.getMrzFormat());
            System.out.println("Full Name: " + document.getFullName());
            System.out.println("Document Number: " + document.getDocumentNumber());
            System.out.println("Nationality: " + document.getNationality());
            System.out.println("Checksum Valid: " + document.isChecksumValid());
        } else {
            System.out.println("MRZ extraction failed: " + document.getValidationErrors());
        }
    }
}
```

## Advanced Configuration

### Custom Preprocessing Configuration

```java
import mz.nedbank.ocr.core.ocr.OcrProcessor;
import mz.nedbank.ocr.utils.ocr.ImagePreprocessor;
import mz.nedbank.ocr.model.ocr.IdentityDocument;
import java.io.File;

public class AdvancedConfigExample {
    public static void main(String[] args) {
        try {
            // Create custom preprocessing configuration
            ImagePreprocessor.PreprocessingConfig config = new ImagePreprocessor.PreprocessingConfig();
            
            // Optimize for high-resolution images
            config.setScaleFactor(4.0);              // Higher scaling for very small MRZ text
            config.setEnableDenoising(true);         // Remove image noise
            config.setEnableDeskewing(true);         // Correct document rotation
            config.setEnableContrastEnhancement(true); // Improve text visibility
            config.setEnableMorphologicalOps(true);  // Clean up character shapes
            config.setBlurKernelSize(1);             // Minimal blur
            config.setMorphKernelSize(2);            // Small morphological kernel
            
            // Initialize processor with custom configuration
            OcrProcessor processor = new OcrProcessor(null, "eng");
            processor.setPreprocessingConfig(config);
            
            // Enable debug mode for troubleshooting
            processor.setDebugMode(true);
            processor.setDebugOutputDir("debug_output");
            
            // Process document
            File documentImage = new File("difficult_document.jpg");
            IdentityDocument document = processor.processIdentityDocument(documentImage);
            
            System.out.println("Processing completed. Check debug_output/ for intermediate files.");
            
            if (document.isValid()) {
                System.out.println("Successfully extracted: " + document.getFullName());
            } else {
                System.out.println("Processing failed: " + document.getValidationErrors());
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

### Multi-language Processing

```java
import mz.nedbank.ocr.core.ocr.OcrProcessor;
import mz.nedbank.ocr.model.ocr.IdentityDocument;
import java.io.File;

public class MultiLanguageExample {
    public static void main(String[] args) {
        try {
            // Process documents in different languages
            String[] languages = {"eng", "fra", "deu", "spa"};
            String[] documentFiles = {"passport_en.jpg", "passport_fr.jpg", "passport_de.jpg", "passport_es.jpg"};
            
            for (int i = 0; i < languages.length; i++) {
                System.out.println("\n=== Processing " + languages[i].toUpperCase() + " Document ===");
                
                // Create processor for specific language
                OcrProcessor processor = new OcrProcessor(null, languages[i]);
                
                // Check if language is available
                String[] availableLanguages = processor.getAvailableLanguages();
                boolean languageAvailable = false;
                for (String lang : availableLanguages) {
                    if (lang.equals(languages[i])) {
                        languageAvailable = true;
                        break;
                    }
                }
                
                if (!languageAvailable) {
                    System.out.println("Language " + languages[i] + " not available. Skipping.");
                    continue;
                }
                
                // Process document
                File documentFile = new File(documentFiles[i]);
                if (documentFile.exists()) {
                    IdentityDocument document = processor.processIdentityDocument(documentFile);
                    
                    if (document.isValid()) {
                        System.out.println("Name: " + document.getFullName());
                        System.out.println("Nationality: " + document.getNationality());
                        System.out.println("Document Number: " + document.getDocumentNumber());
                    } else {
                        System.out.println("Failed to process: " + document.getValidationErrors());
                    }
                } else {
                    System.out.println("File not found: " + documentFiles[i]);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

## Batch Processing

### Directory Processing

```java
import mz.nedbank.ocr.core.ocr.OcrProcessor;
import mz.nedbank.ocr.model.ocr.IdentityDocument;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class BatchProcessingExample {
    public static void main(String[] args) {
        try {
            // Initialize processor
            OcrProcessor processor = new OcrProcessor();
            processor.setDebugMode(false); // Disable debug for batch processing
            
            // Get all image files from directory
            File documentsDir = new File("documents");
            File[] imageFiles = documentsDir.listFiles((dir, name) -> {
                String lowerName = name.toLowerCase();
                return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || 
                       lowerName.endsWith(".png") || lowerName.endsWith(".tiff");
            });
            
            if (imageFiles == null || imageFiles.length == 0) {
                System.out.println("No image files found in documents directory");
                return;
            }
            
            System.out.println("Processing " + imageFiles.length + " documents...");
            
            // Process all documents
            long startTime = System.currentTimeMillis();
            IdentityDocument[] results = processor.processIdentityDocumentBatch(imageFiles);
            long endTime = System.currentTimeMillis();
            
            // Analyze results
            int validDocuments = 0;
            int invalidDocuments = 0;
            
            // Create CSV output
            FileWriter csvWriter = new FileWriter("batch_results.csv");
            csvWriter.append("Filename,Valid,DocumentType,FullName,DocumentNumber,Nationality,DateOfBirth,ExpirationDate,Errors\n");
            
            for (int i = 0; i < results.length; i++) {
                IdentityDocument doc = results[i];
                String filename = imageFiles[i].getName();
                
                if (doc.isValid()) {
                    validDocuments++;
                    System.out.println("✓ " + filename + " - " + doc.getFullName());
                    
                    // Write to CSV
                    csvWriter.append(String.format("%s,true,%s,\"%s\",%s,%s,%s,%s,\n",
                        filename,
                        doc.getDocumentType(),
                        doc.getFullName(),
                        doc.getDocumentNumber(),
                        doc.getNationality(),
                        doc.getDateOfBirth(),
                        doc.getExpirationDate()));
                } else {
                    invalidDocuments++;
                    System.out.println("✗ " + filename + " - " + doc.getValidationErrors());
                    
                    // Write to CSV
                    csvWriter.append(String.format("%s,false,,,,,,,\"%s\"\n",
                        filename,
                        doc.getValidationErrors().replace("\"", "\"\"")));
                }
            }
            
            csvWriter.close();
            
            // Print summary
            System.out.println("\n=== Batch Processing Summary ===");
            System.out.println("Total documents: " + imageFiles.length);
            System.out.println("Valid documents: " + validDocuments);
            System.out.println("Invalid documents: " + invalidDocuments);
            System.out.println("Success rate: " + String.format("%.1f%%", (double)validDocuments / imageFiles.length * 100));
            System.out.println("Processing time: " + (endTime - startTime) + " ms");
            System.out.println("Average time per document: " + (endTime - startTime) / imageFiles.length + " ms");
            System.out.println("Results saved to: batch_results.csv");
            
        } catch (Exception e) {
            System.err.println("Batch processing error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

### Parallel Processing

```java
import mz.nedbank.ocr.core.ocr.OcrProcessor;
import mz.nedbank.ocr.model.ocr.IdentityDocument;
import java.io.File;
import java.util.concurrent.*;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class ParallelProcessingExample {
    public static void main(String[] args) {
        try {
            // Get files to process
            File documentsDir = new File("documents");
            File[] imageFiles = documentsDir.listFiles((dir, name) -> 
                name.toLowerCase().matches(".*\\.(jpg|jpeg|png|tiff)$"));
            
            if (imageFiles == null || imageFiles.length == 0) {
                System.out.println("No image files found");
                return;
            }
            
            // Create thread pool
            int numThreads = Runtime.getRuntime().availableProcessors();
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            
            System.out.println("Processing " + imageFiles.length + " documents using " + numThreads + " threads...");
            
            // Submit processing tasks
            List<Future<ProcessingResult>> futures = new ArrayList<>();
            long startTime = System.currentTimeMillis();
            
            for (File file : imageFiles) {
                Future<ProcessingResult> future = executor.submit(() -> {
                    try {
                        // Each thread gets its own processor instance
                        OcrProcessor processor = new OcrProcessor();
                        IdentityDocument document = processor.processIdentityDocument(file);
                        return new ProcessingResult(file.getName(), document, null);
                    } catch (Exception e) {
                        return new ProcessingResult(file.getName(), null, e);
                    }
                });
                futures.add(future);
            }
            
            // Collect results
            int validCount = 0;
            int errorCount = 0;
            
            for (Future<ProcessingResult> future : futures) {
                try {
                    ProcessingResult result = future.get();
                    
                    if (result.exception != null) {
                        System.out.println("✗ " + result.filename + " - Exception: " + result.exception.getMessage());
                        errorCount++;
                    } else if (result.document.isValid()) {
                        System.out.println("✓ " + result.filename + " - " + result.document.getFullName());
                        validCount++;
                    } else {
                        System.out.println("✗ " + result.filename + " - " + result.document.getValidationErrors());
                        errorCount++;
                    }
                } catch (ExecutionException e) {
                    System.out.println("✗ Execution error: " + e.getMessage());
                    errorCount++;
                }
            }
            
            long endTime = System.currentTimeMillis();
            
            // Shutdown executor
            executor.shutdown();
            
            // Print results
            System.out.println("\n=== Parallel Processing Summary ===");
            System.out.println("Total files: " + imageFiles.length);
            System.out.println("Valid documents: " + validCount);
            System.out.println("Errors: " + errorCount);
            System.out.println("Processing time: " + (endTime - startTime) + " ms");
            System.out.println("Threads used: " + numThreads);
            
        } catch (Exception e) {
            System.err.println("Parallel processing error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    static class ProcessingResult {
        final String filename;
        final IdentityDocument document;
        final Exception exception;
        
        ProcessingResult(String filename, IdentityDocument document, Exception exception) {
            this.filename = filename;
            this.document = document;
            this.exception = exception;
        }
    }
}
```

## Error Handling

### Comprehensive Error Handling

```java
import mz.nedbank.ocr.core.ocr.OcrProcessor;
import mz.nedbank.ocr.model.ocr.IdentityDocument;
import java.io.File;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ErrorHandlingExample {
    private static final Logger logger = Logger.getLogger(ErrorHandlingExample.class.getName());
    
    public static void main(String[] args) {
        OcrProcessor processor = new OcrProcessor();
        
        // Test various error scenarios
        testFileNotFound(processor);
        testInvalidImage(processor);
        testCorruptedMrz(processor);
        testValidDocument(processor);
    }
    
    private static void testFileNotFound(OcrProcessor processor) {
        System.out.println("\n=== Testing File Not Found ===");
        try {
            File nonExistentFile = new File("nonexistent.jpg");
            IdentityDocument document = processor.processIdentityDocument(nonExistentFile);
        } catch (IllegalArgumentException e) {
            System.out.println("Expected error caught: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
            logger.log(Level.SEVERE, "Unexpected error in file not found test", e);
        }
    }
    
    private static void testInvalidImage(OcrProcessor processor) {
        System.out.println("\n=== Testing Invalid Image ===");
        try {
            // Create a text file with .jpg extension
            File invalidFile = new File("invalid.jpg");
            if (!invalidFile.exists()) {
                System.out.println("Skipping invalid image test - file doesn't exist");
                return;
            }
            
            IdentityDocument document = processor.processIdentityDocument(invalidFile);
            
            if (!document.isValid()) {
                System.out.println("Invalid document detected correctly");
                System.out.println("Validation errors: " + document.getValidationErrors());
            } else {
                System.out.println("Unexpected: Document marked as valid");
            }
            
        } catch (Exception e) {
            System.out.println("Exception during invalid image processing: " + e.getMessage());
            // This might be expected depending on the image format
        }
    }
    
    private static void testCorruptedMrz(OcrProcessor processor) {
        System.out.println("\n=== Testing Corrupted MRZ ===");
        
        // Test with corrupted MRZ text
        String corruptedMrz = "P<USA###CORRUPTED###DATA###\n" +
                             "INVALID123<USA8001014M2501017<<<<<<<<<<<<<<<<8";
        
        mz.nedbank.ocr.extractor.ocr.MrzDataExtractor extractor = 
            new mz.nedbank.ocr.extractor.ocr.MrzDataExtractor();
        
        IdentityDocument document = extractor.extract(corruptedMrz);
        
        if (!document.isValid()) {
            System.out.println("Corrupted MRZ detected correctly");
            System.out.println("Validation errors: " + document.getValidationErrors());
        } else {
            System.out.println("Warning: Corrupted MRZ not detected");
        }
    }
    
    private static void testValidDocument(OcrProcessor processor) {
        System.out.println("\n=== Testing Valid Document ===");
        
        // Test with valid sample files if they exist
        String[] sampleFiles = {
            "src/test/resources/sample_passport_mrz.png",
            "src/test/resources/sample_id_card_mrz.png"
        };
        
        for (String filename : sampleFiles) {
            File sampleFile = new File(filename);
            if (sampleFile.exists()) {
                try {
                    IdentityDocument document = processor.processIdentityDocument(sampleFile);
                    
                    if (document.isValid()) {
                        System.out.println("✓ " + filename + " processed successfully");
                        System.out.println("  Document Type: " + document.getDocumentType());
                        System.out.println("  MRZ Format: " + document.getMrzFormat());
                        System.out.println("  Checksum Valid: " + document.isChecksumValid());
                    } else {
                        System.out.println("✗ " + filename + " validation failed");
                        System.out.println("  Errors: " + document.getValidationErrors());
                    }
                    
                } catch (Exception e) {
                    System.out.println("✗ " + filename + " processing failed: " + e.getMessage());
                    logger.log(Level.WARNING, "Sample file processing failed", e);
                }
            } else {
                System.out.println("Sample file not found: " + filename);
            }
        }
    }
}
```

## Integration Examples

### Spring Boot Integration

```java
// DocumentProcessingService.java
import mz.nedbank.ocr.core.ocr.OcrProcessor;
import mz.nedbank.ocr.model.ocr.IdentityDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class DocumentProcessingService {
    
    private OcrProcessor ocrProcessor;
    
    @PostConstruct
    public void init() {
        this.ocrProcessor = new OcrProcessor();
        // Configure for production use
        this.ocrProcessor.setDebugMode(false);
    }
    
    public IdentityDocument processDocument(MultipartFile file) throws IOException {
        // Save uploaded file temporarily
        Path tempFile = Files.createTempFile("document", ".tmp");
        try {
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            
            // Process the document
            IdentityDocument document = ocrProcessor.processIdentityDocument(tempFile.toFile());
            
            return document;
            
        } catch (Exception e) {
            throw new RuntimeException("Document processing failed", e);
        } finally {
            // Clean up temporary file
            Files.deleteIfExists(tempFile);
        }
    }
    
    public IdentityDocument[] processBatch(MultipartFile[] files) throws IOException {
        File[] tempFiles = new File[files.length];
        
        try {
            // Save all files temporarily
            for (int i = 0; i < files.length; i++) {
                Path tempFile = Files.createTempFile("document" + i, ".tmp");
                Files.copy(files[i].getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
                tempFiles[i] = tempFile.toFile();
            }
            
            // Process batch
            return ocrProcessor.processIdentityDocumentBatch(tempFiles);
            
        } catch (Exception e) {
            throw new RuntimeException("Batch processing failed", e);
        } finally {
            // Clean up temporary files
            for (File tempFile : tempFiles) {
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                }
            }
        }
    }
}

// DocumentController.java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    
    @Autowired
    private DocumentProcessingService documentService;
    
    @PostMapping("/process")
    public ResponseEntity<IdentityDocument> processDocument(
            @RequestParam("file") MultipartFile file) {
        try {
            IdentityDocument document = documentService.processDocument(file);
            return ResponseEntity.ok(document);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/batch")
    public ResponseEntity<IdentityDocument[]> processBatch(
            @RequestParam("files") MultipartFile[] files) {
        try {
            IdentityDocument[] documents = documentService.processBatch(files);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
```

### Command Line Tool

```java
import mz.nedbank.ocr.core.ocr.OcrProcessor;
import mz.nedbank.ocr.model.ocr.IdentityDocument;
import mz.nedbank.ocr.utils.ocr.ImagePreprocessor;
import java.io.File;
import java.util.Arrays;

public class DocumentProcessorCLI {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }
        
        // Parse command line arguments
        CLIOptions options = parseArguments(args);
        
        if (options == null) {
            System.exit(1);
        }
        
        try {
            // Initialize processor
            OcrProcessor processor = new OcrProcessor(options.tessDataPath, options.language);
            
            // Configure preprocessing
            if (options.scaleFactor != null || options.enableDenoising != null) {
                ImagePreprocessor.PreprocessingConfig config = processor.getPreprocessingConfig();
                if (options.scaleFactor != null) {
                    config.setScaleFactor(options.scaleFactor);
                }
                if (options.enableDenoising != null) {
                    config.setEnableDenoising(options.enableDenoising);
                }
                processor.setPreprocessingConfig(config);
            }
            
            // Set debug mode
            if (options.debugMode) {
                processor.setDebugMode(true);
                if (options.debugOutputDir != null) {
                    processor.setDebugOutputDir(options.debugOutputDir);
                }
            }
            
            // Process files
            if (options.batchMode) {
                processBatch(processor, options.inputFiles);
            } else {
                processSingle(processor, options.inputFiles[0]);
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (options.debugMode) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }
    
    private static void processSingle(OcrProcessor processor, String filename) throws Exception {
        File file = new File(filename);
        if (!file.exists()) {
            throw new IllegalArgumentException("File not found: " + filename);
        }
        
        System.out.println("Processing: " + filename);
        IdentityDocument document = processor.processIdentityDocument(file);
        
        if (document.isValid()) {
            printDocumentInfo(document);
        } else {
            System.out.println("Processing failed: " + document.getValidationErrors());
        }
    }
    
    private static void processBatch(OcrProcessor processor, String[] filenames) throws Exception {
        File[] files = new File[filenames.length];
        for (int i = 0; i < filenames.length; i++) {
            files[i] = new File(filenames[i]);
            if (!files[i].exists()) {
                throw new IllegalArgumentException("File not found: " + filenames[i]);
            }
        }
        
        System.out.println("Processing " + files.length + " files...");
        IdentityDocument[] documents = processor.processIdentityDocumentBatch(files);
        
        int validCount = 0;
        for (int i = 0; i < documents.length; i++) {
            System.out.println("\n--- " + filenames[i] + " ---");
            if (documents[i].isValid()) {
                printDocumentInfo(documents[i]);
                validCount++;
            } else {
                System.out.println("Processing failed: " + documents[i].getValidationErrors());
            }
        }
        
        System.out.println("\nSummary: " + validCount + "/" + files.length + " documents processed successfully");
    }
    
    private static void printDocumentInfo(IdentityDocument document) {
        System.out.println("Document Type: " + document.getDocumentType());
        System.out.println("MRZ Format: " + document.getMrzFormat());
        System.out.println("Full Name: " + document.getFullName());
        System.out.println("Document Number: " + document.getDocumentNumber());
        System.out.println("Nationality: " + document.getNationality());
        System.out.println("Date of Birth: " + document.getDateOfBirth());
        System.out.println("Gender: " + document.getGender());
        System.out.println("Expiration Date: " + document.getExpirationDate());
        System.out.println("Issuing Country: " + document.getIssuingCountry());
        System.out.println("Valid: " + document.isValid());
        System.out.println("Checksum Valid: " + document.isChecksumValid());
    }
    
    private static void printUsage() {
        System.out.println("Usage: java DocumentProcessorCLI [options] <file1> [file2] ...");
        System.out.println("Options:");
        System.out.println("  --language <lang>     OCR language (default: eng)");
        System.out.println("  --tessdata <path>     Path to tessdata directory");
        System.out.println("  --scale <factor>      Image scaling factor (default: 3.0)");
        System.out.println("  --denoise             Enable denoising");
        System.out.println("  --debug               Enable debug mode");
        System.out.println("  --debug-dir <dir>     Debug output directory");
        System.out.println("  --batch               Process multiple files");
        System.out.println("  --help                Show this help");
    }
    
    private static CLIOptions parseArguments(String[] args) {
        CLIOptions options = new CLIOptions();
        
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help":
                    printUsage();
                    return null;
                case "--language":
                    if (i + 1 < args.length) {
                        options.language = args[++i];
                    }
                    break;
                case "--tessdata":
                    if (i + 1 < args.length) {
                        options.tessDataPath = args[++i];
                    }
                    break;
                case "--scale":
                    if (i + 1 < args.length) {
                        options.scaleFactor = Double.parseDouble(args[++i]);
                    }
                    break;
                case "--denoise":
                    options.enableDenoising = true;
                    break;
                case "--debug":
                    options.debugMode = true;
                    break;
                case "--debug-dir":
                    if (i + 1 < args.length) {
                        options.debugOutputDir = args[++i];
                    }
                    break;
                case "--batch":
                    options.batchMode = true;
                    break;
                default:
                    if (!args[i].startsWith("--")) {
                        // Collect remaining arguments as input files
                        options.inputFiles = Arrays.copyOfRange(args, i, args.length);
                        return options;
                    }
                    break;
            }
        }
        
        if (options.inputFiles == null || options.inputFiles.length == 0) {
            System.err.println("Error: No input files specified");
            printUsage();
            return null;
        }
        
        return options;
    }
    
    static class CLIOptions {
        String language = "eng";
        String tessDataPath = null;
        Double scaleFactor = null;
        Boolean enableDenoising = null;
        boolean debugMode = false;
        String debugOutputDir = null;
        boolean batchMode = false;
        String[] inputFiles = null;
    }
}
```

## Performance Optimization

### Memory-Efficient Batch Processing

```java
import mz.nedbank.ocr.core.ocr.OcrProcessor;
import mz.nedbank.ocr.model.ocr.IdentityDocument;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MemoryEfficientBatchExample {
    private static final int BATCH_SIZE = 10; // Process in smaller batches
    
    public static void main(String[] args) {
        try {
            // Get all files
            File documentsDir = new File("documents");
            File[] allFiles = documentsDir.listFiles((dir, name) -> 
                name.toLowerCase().matches(".*\\.(jpg|jpeg|png|tiff)$"));
            
            if (allFiles == null || allFiles.length == 0) {
                System.out.println("No files found");
                return;
            }
            
            // Initialize processor with memory-optimized settings
            OcrProcessor processor = new OcrProcessor();
            processor.setDebugMode(false); // Disable debug to save memory
            
            // Process in batches
            List<IdentityDocument> allResults = new ArrayList<>();
            int totalFiles = allFiles.length;
            int processedFiles = 0;
            
            for (int i = 0; i < totalFiles; i += BATCH_SIZE) {
                int endIndex = Math.min(i + BATCH_SIZE, totalFiles);
                File[] batchFiles = new File[endIndex - i];
                System.arraycopy(allFiles, i, batchFiles, 0, batchFiles.length);
                
                System.out.println("Processing batch " + (i / BATCH_SIZE + 1) + 
                                 " (" + batchFiles.length + " files)...");
                
                // Process batch
                IdentityDocument[] batchResults = processor.processIdentityDocumentBatch(batchFiles);
                
                // Process results immediately to save memory
                for (int j = 0; j < batchResults.length; j++) {
                    IdentityDocument doc = batchResults[j];
                    String filename = batchFiles[j].getName();
                    
                    if (doc.isValid()) {
                        System.out.println("✓ " + filename + " - " + doc.getFullName());
                        // Store only essential data
                        allResults.add(doc);
                    } else {
                        System.out.println("✗ " + filename + " - Failed");
                    }
                    
                    processedFiles++;
                }
                
                // Force garbage collection between batches
                System.gc();
                
                // Progress update
                double progress = (double) processedFiles / totalFiles * 100;
                System.out.printf("Progress: %.1f%% (%d/%d)\n", progress, processedFiles, totalFiles);
            }
            
            System.out.println("\nProcessing completed. Valid documents: " + allResults.size());
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

### Performance Monitoring

```java
import mz.nedbank.ocr.core.ocr.OcrProcessor;
import mz.nedbank.ocr.model.ocr.IdentityDocument;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

public class PerformanceMonitoringExample {
    
    public static void main(String[] args) {
        try {
            // Initialize monitoring
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            PerformanceMonitor monitor = new PerformanceMonitor();
            
            // Initialize processor
            OcrProcessor processor = new OcrProcessor();
            
            // Test files
            File[] testFiles = {
                new File("src/test/resources/sample_passport_mrz.png"),
                new File("src/test/resources/sample_id_card_mrz.png")
            };
            
            // Warmup
            System.out.println("Warming up...");
            for (File file : testFiles) {
                if (file.exists()) {
                    processor.processIdentityDocument(file);
                }
            }
            
            // Performance test
            System.out.println("\nStarting performance test...");
            monitor.startTest();
            
            for (int i = 0; i < 10; i++) {
                for (File file : testFiles) {
                    if (file.exists()) {
                        long startTime = System.nanoTime();
                        
                        IdentityDocument document = processor.processIdentityDocument(file);
                        
                        long endTime = System.nanoTime();
                        double processingTime = (endTime - startTime) / 1_000_000.0; // Convert to milliseconds
                        
                        monitor.recordProcessing(file.getName(), processingTime, document.isValid());
                        
                        // Memory usage
                        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
                        monitor.recordMemoryUsage(heapUsage.getUsed());
                    }
                }
            }
            
            monitor.endTest();
            monitor.printReport();
            
        } catch (Exception e) {
            System.err.println("Performance test error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    static class PerformanceMonitor {
        private long testStartTime;
        private long testEndTime;
        private final List<ProcessingRecord> records = new ArrayList<>();
        private final List<Long> memoryUsages = new ArrayList<>();
        
        void startTest() {
            testStartTime = System.currentTimeMillis();
        }
        
        void endTest() {
            testEndTime = System.currentTimeMillis();
        }
        
        void recordProcessing(String filename, double processingTime, boolean success) {
            records.add(new ProcessingRecord(filename, processingTime, success));
        }
        
        void recordMemoryUsage(long memoryUsed) {
            memoryUsages.add(memoryUsed);
        }
        
        void printReport() {
            System.out.println("\n=== Performance Report ===");
            System.out.println("Total test time: " + (testEndTime - testStartTime) + " ms");
            System.out.println("Total documents processed: " + records.size());
            
            // Processing time statistics
            double totalTime = records.stream().mapToDouble(r -> r.processingTime).sum();
            double avgTime = totalTime / records.size();
            double minTime = records.stream().mapToDouble(r -> r.processingTime).min().orElse(0);
            double maxTime = records.stream().mapToDouble(r -> r.processingTime).max().orElse(0);
            
            System.out.printf("Average processing time: %.2f ms\n", avgTime);
            System.out.printf("Min processing time: %.2f ms\n", minTime);
            System.out.printf("Max processing time: %.2f ms\n", maxTime);
            
            // Success rate
            long successCount = records.stream().mapToLong(r -> r.success ? 1 : 0).sum();
            double successRate = (double) successCount / records.size() * 100;
            System.out.printf("Success rate: %.1f%%\n", successRate);
            
            // Memory usage
            if (!memoryUsages.isEmpty()) {
                long avgMemory = memoryUsages.stream().mapToLong(Long::longValue).sum() / memoryUsages.size();
                long maxMemory = memoryUsages.stream().mapToLong(Long::longValue).max().orElse(0);
                
                System.out.printf("Average memory usage: %.2f MB\n", avgMemory / 1024.0 / 1024.0);
                System.out.printf("Peak memory usage: %.2f MB\n", maxMemory / 1024.0 / 1024.0);
            }
            
            // Throughput
            double throughput = records.size() / ((testEndTime - testStartTime) / 1000.0);
            System.out.printf("Throughput: %.2f documents/second\n", throughput);
        }
        
        static class ProcessingRecord {
            final String filename;
            final double processingTime;
            final boolean success;
            
            ProcessingRecord(String filename, double processingTime, boolean success) {
                this.filename = filename;
                this.processingTime = processingTime;
                this.success = success;
            }
        }
    }
}
```

## Troubleshooting Examples

### Debug Mode Analysis

```java
import mz.nedbank.ocr.core.ocr.OcrProcessor;
import mz.nedbank.ocr.model.ocr.IdentityDocument;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DebugAnalysisExample {
    
    public static void main(String[] args) {
        try {
            // Initialize processor with debug mode
            OcrProcessor processor = new OcrProcessor();
            processor.setDebugMode(true);
            processor.setDebugOutputDir("debug_analysis");
            
            // Test problematic document
            File problemDocument = new File("problematic_document.jpg");
            
            if (!problemDocument.exists()) {
                System.out.println("Creating sample problematic document for testing...");
                // In real scenario, you would have an actual problematic document
                return;
            }
            
            System.out.println("Processing problematic document with debug mode...");
            
            IdentityDocument document = processor.processIdentityDocument(problemDocument);
            
            // Analyze results
            System.out.println("\n=== Processing Results ===");
            System.out.println("Valid: " + document.isValid());
            System.out.println("Checksum Valid: " + document.isChecksumValid());
            
            if (!document.isValid()) {
                System.out.println("Validation Errors: " + document.getValidationErrors());
            }
            
            // Analyze debug output
            analyzeDebugOutput("debug_analysis", problemDocument.getName());
            
        } catch (Exception e) {
            System.err.println("Debug analysis error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void analyzeDebugOutput(String debugDir, String originalFilename) {
        try {
            System.out.println("\n=== Debug Output Analysis ===");
            
            // Check for preprocessed image
            String preprocessedImage = debugDir + "/" + originalFilename + "_preprocessed.png";
            if (Files.exists(Paths.get(preprocessedImage))) {
                System.out.println("✓ Preprocessed image saved: " + preprocessedImage);
                System.out.println("  Recommendation: Check if preprocessing improved image quality");
            } else {
                System.out.println("✗ Preprocessed image not found");
            }
            
            // Check for OCR text output
            String ocrTextFile = debugDir + "/" + originalFilename + "_ocr.txt";
            if (Files.exists(Paths.get(ocrTextFile))) {
                System.out.println("✓ OCR text output saved: " + ocrTextFile);
                
                // Analyze OCR text
                String ocrText = new String(Files.readAllBytes(Paths.get(ocrTextFile)));
                analyzeOcrText(ocrText);
            } else {
                System.out.println("✗ OCR text output not found");
            }
            
        } catch (Exception e) {
            System.err.println("Debug output analysis failed: " + e.getMessage());
        }
    }
    
    private static void analyzeOcrText(String ocrText) {
        System.out.println("\n=== OCR Text Analysis ===");
        System.out.println("OCR Text Length: " + ocrText.length() + " characters");
        
        // Check for MRZ-like patterns
        String[] lines = ocrText.split("\n");
        System.out.println("Number of lines: " + lines.length);
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.length() >= 30) {
                System.out.println("Line " + (i + 1) + " (" + line.length() + " chars): " + line);
                
                // Analyze line characteristics
                int alphaCount = 0;
                int digitCount = 0;
                int fillerCount = 0;
                
                for (char c : line.toCharArray()) {
                    if (Character.isLetter(c)) alphaCount++;
                    else if (Character.isDigit(c)) digitCount++;
                    else if (c == '<') fillerCount++;
                }
                
                System.out.printf("  Letters: %d, Digits: %d, Fillers: %d\n", 
                                alphaCount, digitCount, fillerCount);
                
                // Check if it looks like MRZ
                if (line.length() == 30 || line.length() == 36 || line.length() == 44) {
                    if (alphaCount + digitCount + fillerCount > line.length() * 0.8) {
                        System.out.println("  ✓ Looks like valid MRZ line");
                    } else {
                        System.out.println("  ✗ Doesn't look like MRZ (too many invalid characters)");
                    }
                }
            }
        }
        
        // Suggestions
        System.out.println("\n=== Suggestions ===");
        if (lines.length < 2) {
            System.out.println("- Too few lines detected. Try improving image quality or preprocessing.");
        }
        
        boolean hasMrzLengthLines = false;
        for (String line : lines) {
            if (line.trim().length() == 30 || line.trim().length() == 36 || line.trim().length() == 44) {
                hasMrzLengthLines = true;
                break;
            }
        }
        
        if (!hasMrzLengthLines) {
            System.out.println("- No lines with MRZ-standard lengths detected.");
            System.out.println("- Try adjusting image scaling or OCR settings.");
        }
    }
}
```

This comprehensive examples document provides practical guidance for using the OCR Java Library effectively in various scenarios, from basic document processing to advanced integration and troubleshooting.

