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
            // Use a relative path so the example works across environments
            File passportImage = new File("src/test/resources/sample_id_card_mrz5.png");

        OcrProcessor.ProcessingResult<IdentityDocument> result =
                processor.processIdentityDocument(passportImage);

        IdentityDocument document = result.getResult();
            System.out.println(document.getRawMrzText());
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

