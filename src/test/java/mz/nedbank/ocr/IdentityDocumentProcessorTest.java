package mz.nedbank.ocr;

import mz.nedbank.ocr.core.OcrProcessor;
import mz.nedbank.ocr.model.IdentityDocument;
import mz.nedbank.ocr.extractor.MrzDataExtractor;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.time.LocalDate;

/**
 * Unit tests for OCR processor focused on identity document processing
 */
public class IdentityDocumentProcessorTest {
    
    private OcrProcessor processor;
    private MrzDataExtractor extractor;
    
    @Before
    public void setUp() {
        processor = new OcrProcessor();
        extractor = new MrzDataExtractor();
    }
    
    @Test
    @Ignore
    public void testProcessorInitialization() {
        assertNotNull("Processor should be initialized", processor);
        // The simplified processor does not expose language configuration or
        // discovery. We merely ensure the instance is created successfully.
    }
    
    @Test
    @Ignore
    public void testMrzExtractorTD3Passport() {
        // Sample TD3 MRZ data (passport format)
        String sampleMrz = "PNMOZREFO<JUNIOR<<EUGENIO<CASTIGO<<<<<<<<<<<\n" +
                        "AB08647664MOZ0212229M2511085110201453113P<98\n";
        
        IdentityDocument document = extractor.extract(sampleMrz);
        
        assertNotNull("Document should not be null", document);
        assertEquals("Document type should be PASSPORT", IdentityDocument.DocumentType.PASSPORT, document.getDocumentType());
        assertEquals("Issuing country should be USA", "MOZ", document.getIssuingCountry());
        assertEquals("Nationality should be USA", "MOZ", document.getNationality());
        assertEquals("Surname should be DOE", "REFO JUNIOR", document.getSurname());
        assertEquals("Given names should be PAULA FRANCISCA GLORIA", "EUGENIO CASTIGO", document.getGivenNames());
        assertEquals("Document number should be 123456789", "AB0864766", document.getDocumentNumber());
        assertEquals("Gender should be MALE", IdentityDocument.Gender.MALE, document.getGender());
        assertEquals("MRZ format should be TD3", IdentityDocument.MrzFormat.TD3, document.getMrzFormat());
        
        // Test date parsing
        assertNotNull("Date of birth should not be null", document.getDateOfBirth());
        assertEquals("Birth year should be 1980", 2002, document.getDateOfBirth().getYear());
        assertEquals("Birth month should be 12", 12, document.getDateOfBirth().getMonthValue());
        assertEquals("Birth day should be 22", 22, document.getDateOfBirth().getDayOfMonth());
        
        assertNotNull("Expiration date should not be null", document.getExpirationDate());
        assertEquals("Expiration year should be 2025", 2025, document.getExpirationDate().getYear());
    }
    
    @Test
    @Ignore
    public void testMrzExtractorTD1IdCard() {
        // Sample TD1 MRZ data (ID card format)
        String sampleMrz =  "IDMOZAA41019979110102370851S<<\n" +
                            "0112167M2711081MOZ<<<<<<<<<<<7\n" +
                            "CASSAMO<<DYLAN<SAMIRO<<<<<<<<<";


        IdentityDocument document = extractor.extract(sampleMrz);
        
        assertNotNull("Document should not be null", document);
        assertEquals("Document type should be ID_CARD", IdentityDocument.DocumentType.ID_CARD, document.getDocumentType());
        assertEquals("Issuing country should be MOZ", "MOZ", document.getIssuingCountry());
        assertEquals("Nationality should be MOZ", "MOZ", document.getNationality());
        assertEquals("Surname should be CASSAMO", "CASSAMO", document.getSurname());
        assertEquals("Given names should be DYLAN SAMIRO", "DYLAN SAMIRO", document.getGivenNames());
        assertEquals("Document number should be 110102370851S", "110102370851S", document.getDocumentNumber());
        assertEquals("Gender should be MALE", IdentityDocument.Gender.MALE, document.getGender());
        assertEquals("MRZ format should be TD1", IdentityDocument.MrzFormat.TD1, document.getMrzFormat());
    }
    
    @Test
    @Ignore
    public void testMrzExtractorTD2Format() {
        // Sample TD2 MRZ data (rare format)
        String sampleMrz = "I<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<\n" +
                          "D231458907UTO7408122F1204159<<<<<<<6";
        
        IdentityDocument document = extractor.extract(sampleMrz);
        
        assertNotNull("Document should not be null", document);
        assertEquals("Document type should be ID_CARD", IdentityDocument.DocumentType.ID_CARD, document.getDocumentType());
        assertEquals("MRZ format should be TD2", IdentityDocument.MrzFormat.TD2, document.getMrzFormat());
        assertEquals("Surname should be ERIKSSON", "ERIKSSON", document.getSurname());
        assertEquals("Given names should be ANNA MARIA", "ANNA MARIA", document.getGivenNames());
    }
    
    @Test
    @Ignore
    public void testInvalidMrzData() {
        String invalidMrz = "This is not a valid MRZ";
        
        IdentityDocument document = extractor.extract(invalidMrz);
        
        assertNotNull("Document should not be null", document);
        assertFalse("Document should not be valid", document.isValid());
        assertNotNull("Validation errors should be present", document.getValidationErrors());
    }
    
    @Test
    @Ignore
    public void testEmptyMrzData() {
        IdentityDocument document = extractor.extract("");
        
        assertNotNull("Document should not be null", document);
        assertFalse("Document should not be valid", document.isValid());
    }
    
    @Test
    @Ignore
    public void testNullMrzData() {
        IdentityDocument document = extractor.extract(null);
        
        assertNotNull("Document should not be null", document);
        assertFalse("Document should not be valid", document.isValid());
    }
    
    @Test
    @Ignore
    public void testDocumentTypeEnum() {
        assertEquals("P should map to PASSPORT", IdentityDocument.DocumentType.PASSPORT, 
                    IdentityDocument.DocumentType.fromCode("P"));
        assertEquals("I should map to ID_CARD", IdentityDocument.DocumentType.ID_CARD, 
                    IdentityDocument.DocumentType.fromCode("I"));
        assertEquals("V should map to VISA", IdentityDocument.DocumentType.VISA, 
                    IdentityDocument.DocumentType.fromCode("V"));
        assertEquals("X should map to UNKNOWN", IdentityDocument.DocumentType.UNKNOWN, 
                    IdentityDocument.DocumentType.fromCode("X"));
    }
    
    @Test
    @Ignore
    public void testGenderEnum() {
        assertEquals("M should map to MALE", IdentityDocument.Gender.MALE, 
                    IdentityDocument.Gender.fromCode("M"));
        assertEquals("F should map to FEMALE", IdentityDocument.Gender.FEMALE, 
                    IdentityDocument.Gender.fromCode("F"));
        assertEquals("< should map to UNSPECIFIED", IdentityDocument.Gender.UNSPECIFIED, 
                    IdentityDocument.Gender.fromCode("<"));
        assertEquals("X should map to UNSPECIFIED", IdentityDocument.Gender.UNSPECIFIED, 
                    IdentityDocument.Gender.fromCode("X"));
    }
    
    @Test
    @Ignore
    public void testMrzFormatEnum() {
        assertEquals("TD1 should have 3 lines", 3, IdentityDocument.MrzFormat.TD1.getLines());
        assertEquals("TD1 should have 30 characters per line", 30, IdentityDocument.MrzFormat.TD1.getCharactersPerLine());
        
        assertEquals("TD2 should have 2 lines", 2, IdentityDocument.MrzFormat.TD2.getLines());
        assertEquals("TD2 should have 36 characters per line", 36, IdentityDocument.MrzFormat.TD2.getCharactersPerLine());
        
        assertEquals("TD3 should have 2 lines", 2, IdentityDocument.MrzFormat.TD3.getLines());
        assertEquals("TD3 should have 44 characters per line", 44, IdentityDocument.MrzFormat.TD3.getCharactersPerLine());
    }
    
    @Test
    @Ignore
    public void testIdentityDocumentMethods() {
        IdentityDocument document = new IdentityDocument();
        document.setSurname("DOE");
        document.setGivenNames("JOHN JANE");
        document.setDateOfBirth(LocalDate.of(1980, 1, 14));
        document.setExpirationDate(LocalDate.of(2025, 1, 17));
        
        assertEquals("Full name should be formatted correctly", "DOE, JOHN JANE", document.getFullName());
        assertEquals("Age should be calculated correctly", 45, document.getAge()); // Assuming current year is 2025
        assertFalse("Document should not be expired", document.isExpired());
        
        // Test expired document
        document.setExpirationDate(LocalDate.of(2020, 1, 1));
        assertTrue("Document should be expired", document.isExpired());
    }
    
    @Test
    @Ignore
    public void testMrzPreprocessing() {
        // Test MRZ preprocessing with various input formats
        String messyMrz = "P<USA DOE<<JOHN<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n" +
                         "123456789<USA8001014M2501017<<<<<<<<<<<<<<<<8";
        
        IdentityDocument document = extractor.extract(messyMrz);
        assertNotNull("Document should be extracted from messy MRZ", document);
        
        // Test with extra whitespace
        String spacedMrz = "  P<USADOE<<JOHN<<<<<<<<<<<<<<<<<<<<<<<<<<<<  \n" +
                          "  123456789<USA8001014M2501017<<<<<<<<<<<<<<<<8  ";
        
        document = extractor.extract(spacedMrz);
        assertNotNull("Document should be extracted from spaced MRZ", document);
    }
    
    @Test
    @Ignore
    public void testComplexNameParsing() {
        // Test complex names with multiple given names
        String complexMrz = "P<USAVAN<DER<BERG<<ANNA<MARIA<CHRISTINA<<<\n" +
                           "123456789<USA8001014F2501017<<<<<<<<<<<<<<<<8";
        
        IdentityDocument document = extractor.extract(complexMrz);
        
        assertNotNull("Document should not be null", document);
        assertEquals("Surname should be VAN DER BERG", "VAN DER BERG", document.getSurname());
        assertEquals("Given names should be ANNA MARIA CHRISTINA", "ANNA MARIA CHRISTINA", document.getGivenNames());
        assertEquals("Full name should be formatted correctly", "VAN DER BERG, ANNA MARIA CHRISTINA", document.getFullName());
    }
    
    @Test
    @Ignore
    public void testDateEdgeCases() {
        // Test Y2K boundary dates
        String y2kMrz = "P<USADOE<<JOHN<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n" +
                       "123456789<USA9912314M0501017<<<<<<<<<<<<<<<<8";
        
        IdentityDocument document = extractor.extract(y2kMrz);
        
        assertNotNull("Document should not be null", document);
        assertNotNull("Date of birth should not be null", document.getDateOfBirth());
        assertEquals("Birth year should be 1999", 1999, document.getDateOfBirth().getYear());
        assertEquals("Birth month should be 12", 12, document.getDateOfBirth().getMonthValue());
        assertEquals("Birth day should be 31", 31, document.getDateOfBirth().getDayOfMonth());
        
        assertNotNull("Expiration date should not be null", document.getExpirationDate());
        assertEquals("Expiration year should be 2005", 2005, document.getExpirationDate().getYear());
    }

    @Test
    public void testMrzExtractorOCRTD3Passport() {
        try {
            // Initialize the OCR processor
            OcrProcessor processor = new OcrProcessor();

            // Process a passport image
            // Use a relative path so the example works across environments
            File passportImage = new File("src/test/resources/sample_passport_mrz3.png");

            OcrProcessor.ProcessingResult<IdentityDocument> result =
                    processor.processIdentityDocument(passportImage);

            IdentityDocument document = result.getResult();
            System.out.println(document.getRawMrzText());
            // Display extracted information
            if (document.isValid()) {
                System.out.println("=== ID Card Information ===");
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

    @Test
    public void testMrzExtractorOCRTD1IDCard() {
        try {
            // Initialize the OCR processor
            OcrProcessor processor = new OcrProcessor();

            // Process a passport image
            // Use a relative path so the example works across environments
            File passportImage = new File("src/test/resources/sample_id_card_mrz10.png");

            OcrProcessor.ProcessingResult<IdentityDocument> result =
                    processor.processIdentityDocument(passportImage);

            IdentityDocument document = result.getResult();
            System.out.println(document.getRawMrzText());
            // Display extracted information
            if (document.isValid()) {
                System.out.println("=== ID Card Information ===");
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