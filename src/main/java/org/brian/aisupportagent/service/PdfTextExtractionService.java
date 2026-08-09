package org.brian.aisupportagent.service;

import org.brian.aisupportagent.exception.PdfExtractionException;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PdfTextExtractionService {

    private static final Pattern HORIZONTAL_WHITESPACE = Pattern.compile("\\h+");

    private static final PdfDocumentReaderConfig READER_CONFIG =
            PdfDocumentReaderConfig.builder()
                    .withPagesPerDocument(1)
                    .build();

    public PdfExtractionResult extract(Resource pdfResource) {
        try (CloseablePagePdfDocumentReader reader =
                     new CloseablePagePdfDocumentReader(pdfResource, READER_CONFIG)) {
            int pageCount = reader.pageCount();
            List<ExtractedDocumentPage> pages = reader.get().stream()
                    .map(this::toExtractedPage)
                    .toList();

            if (pages.isEmpty()) {
                throw new PdfExtractionException(
                        "No extractable text was found; this PDF may require OCR"
                );
            }

            return new PdfExtractionResult(pageCount, pages);
        } catch (PdfExtractionException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new PdfExtractionException("Could not extract text from the PDF", exception);
        }
    }

    private ExtractedDocumentPage toExtractedPage(Document document) {
        Object pageNumberMetadata = document.getMetadata().get(
                PagePdfDocumentReader.METADATA_START_PAGE_NUMBER
        );
        if (!(pageNumberMetadata instanceof Number pageNumber)) {
            throw new PdfExtractionException("The PDF reader did not provide a page number");
        }

        String text = document.getText();
        if (text == null || text.isBlank()) {
            throw new PdfExtractionException("The PDF reader returned an empty page");
        }

        return new ExtractedDocumentPage(
                pageNumber.intValue(),
                normalizeWhitespace(text)
        );
    }

    private String normalizeWhitespace(String text) {
        return text.lines()
                .map(line -> HORIZONTAL_WHITESPACE.matcher(line).replaceAll(" ").trim())
                .collect(Collectors.joining("\n"))
                .trim();
    }

    private static final class CloseablePagePdfDocumentReader
            extends PagePdfDocumentReader implements AutoCloseable {

        private CloseablePagePdfDocumentReader(
                Resource pdfResource,
                PdfDocumentReaderConfig config
        ) {
            super(pdfResource, config);
        }

        private int pageCount() {
            return document.getNumberOfPages();
        }

        @Override
        public void close() throws IOException {
            document.close();
        }
    }
}
