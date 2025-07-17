package mz.nedbank.ocr.model;

import java.time.LocalDate;

/**
 * Data model for extracted identity document information from MRZ
 */
public class IdentityDocument {
    
    // Document type enumeration
    public enum DocumentType {
        PASSPORT("P"),
        ID_CARD("I"),
        VISA("V"),
        UNKNOWN("");
        
        private final String code;
        
        DocumentType(String code) {
            this.code = code;
        }
        
        public String getCode() {
            return code;
        }
        
        public static DocumentType fromCode(String code) {
            for (DocumentType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            return UNKNOWN;
        }
    }
    
    // Gender enumeration
    public enum Gender {
        MALE("M"),
        FEMALE("F"),
        UNSPECIFIED("<");
        
        private final String code;
        
        Gender(String code) {
            this.code = code;
        }
        
        public String getCode() {
            return code;
        }
        
        public static Gender fromCode(String code) {
            for (Gender gender : values()) {
                if (gender.code.equals(code)) {
                    return gender;
                }
            }
            return UNSPECIFIED;
        }
    }
    
    // MRZ format type
    public enum MrzFormat {
        TD1(3, 30),  // ID cards - 3 lines, 30 characters each
        TD2(2, 36),  // Rare format - 2 lines, 36 characters each
        TD3(2, 44);  // Passports - 2 lines, 44 characters each
        
        private final int lines;
        private final int charactersPerLine;
        
        MrzFormat(int lines, int charactersPerLine) {
            this.lines = lines;
            this.charactersPerLine = charactersPerLine;
        }
        
        public int getLines() {
            return lines;
        }
        
        public int getCharactersPerLine() {
            return charactersPerLine;
        }
    }
    
    // Core document fields
    private DocumentType documentType;
    private String documentSubtype;
    private String issuingCountry;
    private String documentNumber;
    private String nationality;
    private String surname;
    private String givenNames;
    private LocalDate dateOfBirth;
    private Gender gender;
    private LocalDate expirationDate;
    private String personalNumber;
    private MrzFormat mrzFormat;
    
    // Raw MRZ data
    private String[] mrzLines;
    private String rawMrzText;
    
    // Validation fields
    private boolean isValid;
    private String validationErrors;
    private boolean checksumValid;
    
    // Additional fields for specific document types
    private String placeOfBirth;
    private String issuingAuthority;
    private LocalDate issueDate;
    
    public IdentityDocument() {
        this.isValid = false;
        this.checksumValid = false;
    }
    
    // Getters and Setters
    public DocumentType getDocumentType() {
        return documentType;
    }
    
    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }
    
    public String getDocumentSubtype() {
        return documentSubtype;
    }
    
    public void setDocumentSubtype(String documentSubtype) {
        this.documentSubtype = documentSubtype;
    }
    
    public String getIssuingCountry() {
        return issuingCountry;
    }
    
    public void setIssuingCountry(String issuingCountry) {
        this.issuingCountry = issuingCountry;
    }
    
    public String getDocumentNumber() {
        return documentNumber;
    }
    
    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }
    
    public String getNationality() {
        return nationality;
    }
    
    public void setNationality(String nationality) {
        this.nationality = nationality;
    }
    
    public String getSurname() {
        return surname;
    }
    
    public void setSurname(String surname) {
        this.surname = surname;
    }
    
    public String getGivenNames() {
        return givenNames;
    }
    
    public void setGivenNames(String givenNames) {
        this.givenNames = givenNames;
    }
    
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
    
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
    
    public Gender getGender() {
        return gender;
    }
    
    public void setGender(Gender gender) {
        this.gender = gender;
    }
    
    public LocalDate getExpirationDate() {
        return expirationDate;
    }
    
    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }
    
    public String getPersonalNumber() {
        return personalNumber;
    }
    
    public void setPersonalNumber(String personalNumber) {
        this.personalNumber = personalNumber;
    }
    
    public MrzFormat getMrzFormat() {
        return mrzFormat;
    }
    
    public void setMrzFormat(MrzFormat mrzFormat) {
        this.mrzFormat = mrzFormat;
    }
    
    public String[] getMrzLines() {
        return mrzLines;
    }
    
    public void setMrzLines(String[] mrzLines) {
        this.mrzLines = mrzLines;
    }
    
    public String getRawMrzText() {
        return rawMrzText;
    }
    
    public void setRawMrzText(String rawMrzText) {
        this.rawMrzText = rawMrzText;
    }
    
    public boolean isValid() {
        return isValid;
    }
    
    public void setValid(boolean valid) {
        isValid = valid;
    }
    
    public String getValidationErrors() {
        return validationErrors;
    }
    
    public void setValidationErrors(String validationErrors) {
        this.validationErrors = validationErrors;
    }
    
    public boolean isChecksumValid() {
        return checksumValid;
    }
    
    public void setChecksumValid(boolean checksumValid) {
        this.checksumValid = checksumValid;
    }
    
    public String getPlaceOfBirth() {
        return placeOfBirth;
    }
    
    public void setPlaceOfBirth(String placeOfBirth) {
        this.placeOfBirth = placeOfBirth;
    }
    
    public String getIssuingAuthority() {
        return issuingAuthority;
    }
    
    public void setIssuingAuthority(String issuingAuthority) {
        this.issuingAuthority = issuingAuthority;
    }
    
    public LocalDate getIssueDate() {
        return issueDate;
    }
    
    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }
    
    /**
     * Get full name (surname + given names)
     * @return Full name
     */
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        if (surname != null && !surname.isEmpty()) {
            fullName.append(surname);
        }
        if (givenNames != null && !givenNames.isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(", ");
            }
            fullName.append(givenNames);
        }
        return fullName.toString();
    }
    
    /**
     * Check if document is expired
     * @return True if document is expired
     */
    public boolean isExpired() {
        if (expirationDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(expirationDate);
    }
    
    /**
     * Get age based on date of birth
     * @return Age in years, or -1 if date of birth is not set
     */
    public int getAge() {
        if (dateOfBirth == null) {
            return -1;
        }
        return LocalDate.now().getYear() - dateOfBirth.getYear();
    }
    
    @Override
    public String toString() {
        return "IdentityDocument{" +
                "documentType=" + documentType +
                ", documentNumber='" + documentNumber + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", nationality='" + nationality + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", gender=" + gender +
                ", expirationDate=" + expirationDate +
                ", isValid=" + isValid +
                ", checksumValid=" + checksumValid +
                ", mrzFormat=" + mrzFormat +
                '}';
    }
}

