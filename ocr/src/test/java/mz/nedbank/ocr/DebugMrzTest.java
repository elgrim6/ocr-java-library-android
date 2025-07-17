package mz.nedbank.ocr;

import mz.nedbank.ocr.extractor.MrzDataExtractor;
import mz.nedbank.ocr.model.IdentityDocument;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DebugMrzTest {
    
    private MrzDataExtractor extractor;
    
    @Before
    public void setUp() {
        extractor = new MrzDataExtractor();
    }
    
    @Test
    public void debugTD3Extraction() {
        // Test TD3 format
        String sampleMrz = "PPMOZMENDES<<PAULA<FRANCISCA<GLORIA<<<<<<<<<\n" +
                "SAMPLE0240MOZ7012141F150317399999939<<<<<<68\n";
        
        System.out.println("=== DEBUG TD3 EXTRACTION ===");
        System.out.println("Input MRZ:");
        System.out.println("'" + sampleMrz + "'");
        
        String[] lines = sampleMrz.split("\n");
        System.out.println("Number of lines: " + lines.length);
        for (int i = 0; i < lines.length; i++) {
            System.out.println("Line " + (i+1) + " length: " + lines[i].length() + " content: '" + lines[i] + "'");
        }
        
        IdentityDocument document = extractor.extract(sampleMrz);
        
        System.out.println("=== EXTRACTION RESULTS ===");
        System.out.println("Document type: " + document.getDocumentType());
        System.out.println("MRZ format: " + document.getMrzFormat());
        System.out.println("MRZ lines: " + java.util.Arrays.toString(document.getMrzLines()));
        System.out.println("Validation errors: " + document.getValidationErrors());
        System.out.println("Raw MRZ text: '" + document.getRawMrzText() + "'");
        System.out.println("Surname: " + document.getSurname());
        System.out.println("Given names: " + document.getGivenNames());
        System.out.println("Document number: " + document.getDocumentNumber());
        System.out.println("Issuing country: " + document.getIssuingCountry());
        System.out.println("Nationality: " + document.getNationality());
        System.out.println("Date of birth: " + document.getDateOfBirth());
        System.out.println("Gender: " + document.getGender());
        System.out.println("Expiration date: " + document.getExpirationDate());
        
        // Basic assertions
        assertNotNull("Document should not be null", document);
        assertNotNull("Document type should not be null", document.getDocumentType());
    }
    
    @Test
    public void debugTD1Extraction() {
        // Test TD1 format
        String sampleMrz = "IDMOZAA41019979110102370851S<<\n" +
                "0112167M2711081MOZ<<<<<<<<<<<7\n" +
                "CASSAMO<<DYLAN<SAMIRO<<<<<<<<<";

        System.out.println("=== DEBUG TD1 EXTRACTION ===");
        System.out.println("Input MRZ:");
        System.out.println("'" + sampleMrz + "'");
        
        String[] lines = sampleMrz.split("\n");
        System.out.println("Number of lines: " + lines.length);
        for (int i = 0; i < lines.length; i++) {
            System.out.println("Line " + (i+1) + " length: " + lines[i].length() + " content: '" + lines[i] + "'");
        }
        
        IdentityDocument document = extractor.extract(sampleMrz);
        
        System.out.println("=== EXTRACTION RESULTS ===");
        System.out.println("Document type: " + document.getDocumentType());
        System.out.println("MRZ format: " + document.getMrzFormat());
        System.out.println("MRZ lines: " + java.util.Arrays.toString(document.getMrzLines()));
        System.out.println("Validation errors: " + document.getValidationErrors());
        System.out.println("Raw MRZ text: '" + document.getRawMrzText() + "'");
        System.out.println("Surname: " + document.getSurname());
        System.out.println("Given names: " + document.getGivenNames());
        System.out.println("Document number: " + document.getDocumentNumber());
        System.out.println("Issuing country: " + document.getIssuingCountry());
        System.out.println("Nationality: " + document.getNationality());
        System.out.println("Date of birth: " + document.getDateOfBirth());
        System.out.println("Gender: " + document.getGender());
        System.out.println("Expiration date: " + document.getExpirationDate());
        
        // Basic assertions
        assertNotNull("Document should not be null", document);
        assertNotNull("Document type should not be null", document.getDocumentType());
    }
}

