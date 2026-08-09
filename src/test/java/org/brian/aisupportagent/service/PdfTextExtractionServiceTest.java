package org.brian.aisupportagent.service;

import org.brian.aisupportagent.exception.PdfExtractionException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import static org.brian.aisupportagent.util.PdfTestData.pdfWithPages;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PdfTextExtractionServiceTest {

    private final PdfTextExtractionService extractionService =
            new PdfTextExtractionService();

    @Test
    void extractsTextPerPageWhilePreservingOriginalPageNumbers() throws Exception {
        Resource pdf = namedPdfResource(pdfWithPages(
                "Vacation policy: employees receive twenty days.",
                "",
                "Password reset steps are listed here."
        ));

        PdfExtractionResult result = extractionService.extract(pdf);

        assertEquals(3, result.pageCount());
        assertEquals(2, result.pages().size());
        assertEquals(1, result.pages().getFirst().pageNumber());
        assertEquals(
                "Vacation policy: employees receive twenty days.",
                result.pages().getFirst().content()
        );
        assertEquals(3, result.pages().getLast().pageNumber());
        assertEquals(
                "Password reset steps are listed here.",
                result.pages().getLast().content()
        );
    }

    @Test
    void rejectsPdfWithoutExtractableText() throws Exception {
        Resource blankPdf = namedPdfResource(pdfWithPages(""));

        PdfExtractionException exception = assertThrows(
                PdfExtractionException.class,
                () -> extractionService.extract(blankPdf)
        );

        assertEquals(
                "No extractable text was found; this PDF may require OCR",
                exception.getMessage()
        );
    }

    private Resource namedPdfResource(byte[] content) {
        return new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return "test-document.pdf";
            }
        };
    }
}
