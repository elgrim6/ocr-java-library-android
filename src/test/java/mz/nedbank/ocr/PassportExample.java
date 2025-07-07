package mz.nedbank.ocr;

import mz.nedbank.ocr.core.OcrProcessor;
import mz.nedbank.ocr.model.IdentityDocument;
import java.io.File;

public class PassportExample {
    public static void main(String[] args) {
        try {
            // Initialize the OCR processor
            OcrProcessor processor = new OcrProcessor();

            // Process a passport image
            File passportImage = new File("C:\\Users\\dysha\\Downloads\\ocr-identity-documents-library\\ocr-java-library\\src\\test\\resources\\sample_passport_mrz.png");
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

