package org.brian.aisupportagent.service;

import java.util.List;

public record PdfExtractionResult(
        int pageCount,
        List<ExtractedDocumentPage> pages
) {

    public PdfExtractionResult {
        pages = List.copyOf(pages);
    }
}
