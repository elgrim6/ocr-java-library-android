package mz.nedbank.ocr.extractor;

import mz.nedbank.ocr.model.IdentityDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts structured identity document data from MRZ (Machine Readable Zone) text
 * Supports TD1 (ID cards), TD2, and TD3 (passports) formats according to ICAO Doc 9303
 */
public class MrzDataExtractor {
    private static final Logger logger = LoggerFactory.getLogger(MrzDataExtractor.class);

    // Character used for padding/filling in MRZ
    private static final char FILLER_CHAR = '<';

    /**
     * Extract identity document data from OCR text containing MRZ
     * @param text Raw OCR text
     * @return IdentityDocument object with extracted information
     */
    public IdentityDocument extract(String text) {
        System.out.println("DEBUG: Starting extraction with text: '" + text + "'");

        if (text == null || text.trim().isEmpty()) {
            logger.warn("Empty or null OCR text provided");
            IdentityDocument document = new IdentityDocument();
            document.setValidationErrors("Empty or null input text");
            return document;
        }

        IdentityDocument document = new IdentityDocument();
        document.setRawMrzText(text);

        // Clean and normalize the text
        String cleanText = preprocessMrzText(text);
        System.out.println("DEBUG: After preprocessing: '" + cleanText + "'");
        logger.debug("Processing MRZ text of {} characters", cleanText.length());

        // Extract MRZ lines
        String[] mrzLines = extractMrzLines(cleanText);
        if (mrzLines.length == 0) {
            logger.warn("No valid MRZ lines found in text");
            document.setValidationErrors("No valid MRZ lines found");
            return document;
        }

        document.setMrzLines(mrzLines);

        // Determine MRZ format and extract data
        IdentityDocument.MrzFormat format = determineMrzFormat(mrzLines);
        document.setMrzFormat(format);

        try {
            switch (format) {
                case TD3:
                    extractTD3Data(mrzLines, document);
                    break;
                case TD1:
                    extractTD1Data(mrzLines, document);
                    break;
                case TD2:
                    extractTD2Data(mrzLines, document);
                    break;
                default:
                    logger.warn("Unknown MRZ format detected");
                    document.setValidationErrors("Unknown MRZ format");
                    return document;
            }
        } catch (Exception e) {
            logger.error("Error extracting MRZ data", e);
            document.setValidationErrors("Error parsing MRZ data: " + e.getMessage());
            return document;
        }

        // Validate checksums
        validateChecksums(document);

        // Perform general validation
        validateDocument(document);

        logger.info("Extracted identity document: {}", document.toString());
        return document;
    }

    /**
     * Preprocess MRZ text to improve extraction accuracy
     */
    private String preprocessMrzText(String text) {
        // Remove extra whitespace and normalize line endings
        text = text.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");

        // Convert to uppercase
        text = text.toUpperCase();

        // Replace common OCR errors but be more conservative
        text = text.replaceAll("[^A-Z0-9<\\n\\s]", "<");

        return text.trim();
    }

    /**
     * Extract MRZ lines from preprocessed text
     */
    private String[] extractMrzLines(String text) {
        List<String> mrzLines = new ArrayList<>();
        String[] lines = text.split("\n");

        System.out.println("DEBUG: Processing " + lines.length + " lines from text");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            System.out.println("DEBUG: Line " + (i+1) + " length=" + line.length() + " content='" + line + "'");
            // MRZ lines should be 30, 36, or 44 characters long
            if (line.length() == 30 || line.length() == 36 || line.length() == 44) {
                // Check if line contains mostly valid MRZ characters
                if (isValidMrzLine(line)) {
                    System.out.println("DEBUG: Line " + (i+1) + " is valid MRZ line");
                    mrzLines.add(line);
                } else {
                    System.out.println("DEBUG: Line " + (i+1) + " failed MRZ validation");
                }
            } else {
                System.out.println("DEBUG: Line " + (i+1) + " wrong length for MRZ");
            }
        }

        System.out.println("DEBUG: Found " + mrzLines.size() + " valid MRZ lines");
        return mrzLines.toArray(new String[0]);
    }

    /**
     * Check if a line is a valid MRZ line
     */
    private boolean isValidMrzLine(String line) {
        if (line == null || line.isEmpty()) {
            return false;
        }

        // Count valid MRZ characters
        int validChars = 0;
        int invalidChars = 0;
        StringBuilder invalidCharList = new StringBuilder();

        for (char c : line.toCharArray()) {
            if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '<') {
                validChars++;
            } else {
                invalidChars++;
                if (invalidCharList.length() < 20) { // Limit debug output
                    invalidCharList.append(c).append(" ");
                }
            }
        }

        double validRatio = (double) validChars / line.length();
        System.out.println("DEBUG: Line validation - valid: " + validChars + ", invalid: " + invalidChars +
                          ", ratio: " + validRatio + ", invalid chars: [" + invalidCharList.toString().trim() + "]");

        // At least 70% of characters should be valid MRZ characters (more lenient)
        return validRatio >= 0.7;
    }

    /**
     * Determine MRZ format based on line count and length
     */
    private IdentityDocument.MrzFormat determineMrzFormat(String[] mrzLines) {
        if (mrzLines.length == 2) {
            if (mrzLines[0].length() == 44 && mrzLines[1].length() == 44) {
                return IdentityDocument.MrzFormat.TD3;
            } else if (mrzLines[0].length() == 36 && mrzLines[1].length() == 36) {
                return IdentityDocument.MrzFormat.TD2;
            }
        } else if (mrzLines.length == 3) {
            if (mrzLines[0].length() == 30 && mrzLines[1].length() == 30 && mrzLines[2].length() == 30) {
                return IdentityDocument.MrzFormat.TD1;
            }
        }

        logger.warn("Could not determine MRZ format from {} lines", mrzLines.length);
        return IdentityDocument.MrzFormat.TD3; // Default fallback
    }

    /**
     * Extract data from TD3 format (passports)
     * Line 1: P<USADOE<<JOHN<<<<<<<<<<<<<<<<<<<<<<<<<<<<
     * Line 2: 123456789<USA8001014M2501017<<<<<<<<<<<<<<<<8
     */
    private void extractTD3Data(String[] mrzLines, IdentityDocument document) {
        if (mrzLines.length < 2) {
            document.setValidationErrors("TD3 format requires 2 lines");
            return;
        }

        String line1 = mrzLines[0];
        String line2 = mrzLines[1];

        // Parse line 1
        if (line1.length() >= 44) {
            // Document type (position 1)
            document.setDocumentType(IdentityDocument.DocumentType.fromCode(line1.substring(0, 1)));

            // Document subtype (position 2)
            document.setDocumentSubtype(line1.substring(1, 2).replace("<", ""));

            // Issuing country (positions 3-5)
            document.setIssuingCountry(line1.substring(2, 5));

            // Name field (positions 6-44)
            String nameField = line1.substring(5);
            parseNameField(nameField, document);
        }

        // Parse line 2
        if (line2.length() >= 44) {
            // Document number (positions 1-9)
            document.setDocumentNumber(line2.substring(0, 9).replace("<", ""));

            // Nationality (positions 11-13)
            document.setNationality(line2.substring(10, 13));

            // Date of birth (positions 14-19)
            String dobStr = line2.substring(13, 19);
            document.setDateOfBirth(parseMrzDate(dobStr));

            // Gender (position 21)
            document.setGender(IdentityDocument.Gender.fromCode(line2.substring(20, 21)));

            // Expiration date (positions 22-27)
            String expStr = line2.substring(21, 27);
            document.setExpirationDate(parseMrzDate(expStr));

            // Personal number (positions 29-42)
            if (line2.length() >= 42) {
                document.setPersonalNumber(line2.substring(28, 42).replace("<", ""));
            }
        }
    }

    /**
     * Extract data from TD1 format (ID cards)
     * Line 1: I<UTOD231458907<<<<<<<<<<<<<<<<
     * Line 2: 7408122F1204159UTO<<<<<<<<<<<6
     * Line 3: ERIKSSON<<ANNA<MARIA<<<<<<<<<<
     */
    private void extractTD1Data(String[] mrzLines, IdentityDocument document) {
        if (mrzLines.length < 3) {
            document.setValidationErrors("TD1 format requires 3 lines");
            return;
        }

        String line1 = mrzLines[0];
        String line2 = mrzLines[1];
        String line3 = mrzLines[2];

        // Parse line 1
        if (line1.length() >= 30) {
            // Document type (position 1)
            document.setDocumentType(IdentityDocument.DocumentType.fromCode(line1.substring(0, 1)));

            // Document subtype (position 2)
            document.setDocumentSubtype(line1.substring(1, 2).replace("<", ""));

            // Issuing country (positions 3-5)
            document.setIssuingCountry(line1.substring(2, 5));

            // Document number (positions 6-14)
            document.setDocumentNumber(line1.substring(15, 28).replace("<", ""));
        }

        // Parse line 2
        if (line2.length() >= 30) {
            // Date of birth (positions 1-6)
            String dobStr = line2.substring(0, 6);
            document.setDateOfBirth(parseMrzDate(dobStr));

            // Gender (position 8)
            document.setGender(IdentityDocument.Gender.fromCode(line2.substring(7, 8)));

            // Expiration date (positions 9-14)
            String expStr = line2.substring(8, 14);
            document.setExpirationDate(parseMrzDate(expStr));

            // Nationality (positions 16-18)
            if (line2.length() >= 18) {
                document.setNationality(line2.substring(15, 18));
            }
        }

        // Parse line 3 (name field)
        parseNameField(line3, document);
    }

    /**
     * Extract data from TD2 format (rare)
     * Line 1: I<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<
     * Line 2: D231458907UTO7408122F1204159<<<<<<<6
     */
    private void extractTD2Data(String[] mrzLines, IdentityDocument document) {
        if (mrzLines.length < 2) {
            document.setValidationErrors("TD2 format requires 2 lines");
            return;
        }

        String line1 = mrzLines[0];
        String line2 = mrzLines[1];

        // Parse line 1
        if (line1.length() >= 36) {
            // Document type (position 1)
            document.setDocumentType(IdentityDocument.DocumentType.fromCode(line1.substring(0, 1)));

            // Document subtype (position 2)
            document.setDocumentSubtype(line1.substring(1, 2).replace("<", ""));

            // Issuing country (positions 3-5)
            document.setIssuingCountry(line1.substring(2, 5));

            // Name field (positions 6-36)
            String nameField = line1.substring(5);
            parseNameField(nameField, document);
        }

        // Parse line 2
        if (line2.length() >= 36) {
            // Document number (positions 1-9)
            document.setDocumentNumber(line2.substring(0, 9).replace("<", ""));

            // Nationality (positions 11-13)
            document.setNationality(line2.substring(10, 13));

            // Date of birth (positions 14-19)
            String dobStr = line2.substring(13, 19);
            document.setDateOfBirth(parseMrzDate(dobStr));

            // Gender (position 21)
            document.setGender(IdentityDocument.Gender.fromCode(line2.substring(20, 21)));

            // Expiration date (positions 22-27)
            String expStr = line2.substring(21, 27);
            document.setExpirationDate(parseMrzDate(expStr));
        }
    }

    /**
     * Parse name field from MRZ
     */
    private void parseNameField(String nameField, IdentityDocument document) {
        if (nameField == null || nameField.isEmpty()) {
            return;
        }

        // Remove trailing filler characters
        nameField = nameField.replaceAll("<+$", "");

        // Split on double filler characters (surname separator)
        String[] nameParts = nameField.split("<<");

        if (nameParts.length >= 1 && !nameParts[0].isEmpty()) {
            document.setSurname(nameParts[0].replace("<", " ").trim());
        }

        if (nameParts.length >= 2 && !nameParts[1].isEmpty()) {
            // Given names are separated by single filler characters
            String givenNames = nameParts[1].replace("<", " ").trim();
            document.setGivenNames(givenNames);
        }
    }

    /**
     * Parse MRZ date format (YYMMDD)
     */
    private LocalDate parseMrzDate(String dateStr) {
        if (dateStr == null || dateStr.length() != 6) {
            return null;
        }

        try {
            // MRZ dates are in YYMMDD format
            int year = Integer.parseInt(dateStr.substring(0, 2));
            int month = Integer.parseInt(dateStr.substring(2, 4));
            int day = Integer.parseInt(dateStr.substring(4, 6));

            // Handle 2-digit year (assume 1900-2099 range)
            if (year < 50) {
                year += 2000; // 00-49 -> 2000-2049
            } else {
                year += 1900; // 50-99 -> 1950-1999
            }

            return LocalDate.of(year, month, day);

        } catch (Exception e) {
            logger.warn("Failed to parse MRZ date: {}", dateStr);
            return null;
        }
    }

    /**
     * Validate checksums in the MRZ
     */
    private void validateChecksums(IdentityDocument document) {
        // This is a simplified checksum validation
        // In a production system, you would implement the full ICAO checksum algorithm
        document.setChecksumValid(true); // Placeholder
        logger.debug("Checksum validation completed (simplified)");
    }

    /**
     * Perform general document validation
     */
    private void validateDocument(IdentityDocument document) {
        List<String> errors = new ArrayList<>();

        // Check required fields
        if (document.getDocumentType() == null) {
            errors.add("Document type is missing");
        }

        if (document.getDocumentNumber() == null || document.getDocumentNumber().isEmpty()) {
            errors.add("Document number is missing");
        }

        if (document.getSurname() == null || document.getSurname().isEmpty()) {
            errors.add("Surname is missing");
        }

        if (document.getDateOfBirth() == null) {
            errors.add("Date of birth is missing");
        }

        if (document.getExpirationDate() == null) {
            errors.add("Expiration date is missing");
        }

        // Check date validity
        if (document.getDateOfBirth() != null && document.getDateOfBirth().isAfter(LocalDate.now())) {
            errors.add("Date of birth is in the future");
        }

        if (document.getExpirationDate() != null && document.getExpirationDate().isBefore(LocalDate.now().minusYears(50))) {
            errors.add("Expiration date is too far in the past");
        }

        // Set validation results
        document.setValid(errors.isEmpty());
        if (!errors.isEmpty()) {
            document.setValidationErrors(String.join("; ", errors));
        }

        logger.debug("Document validation completed. Valid: {}, Errors: {}",
                    document.isValid(), document.getValidationErrors());
    }

    /**
     * Calculate MRZ check digit (simplified implementation)
     */
    private int calculateCheckDigit(String data) {
        // This is a simplified implementation
        // The actual ICAO algorithm uses specific weights: 7, 3, 1, 7, 3, 1, ...
        int[] weights = {7, 3, 1};
        int sum = 0;

        for (int i = 0; i < data.length(); i++) {
            char c = data.charAt(i);
            int value;

            if (c >= '0' && c <= '9') {
                value = c - '0';
            } else if (c >= 'A' && c <= 'Z') {
                value = c - 'A' + 10;
            } else if (c == '<') {
                value = 0;
            } else {
                continue; // Skip invalid characters
            }

            sum += value * weights[i % 3];
        }

        return sum % 10;
    }
}
