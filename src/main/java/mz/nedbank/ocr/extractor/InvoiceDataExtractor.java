package mz.nedbank.ocr.extractor;

import mz.nedbank.ocr.model.InvoiceData;

/**
 * @deprecated This class is deprecated. Use MrzDataExtractor for identity document processing.
 * Kept for backward compatibility.
 */
@Deprecated
public class InvoiceDataExtractor {
    
    /**
     * @deprecated Use MrzDataExtractor.extract() instead
     */
    @Deprecated
    public InvoiceData extract(String text) {
        // This method is deprecated but kept for backward compatibility
        return new InvoiceData();
    }
}

