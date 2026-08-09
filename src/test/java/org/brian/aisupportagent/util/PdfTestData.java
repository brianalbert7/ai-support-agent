package org.brian.aisupportagent.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class PdfTestData {

    private static final PDType1Font FONT = new PDType1Font(
            Standard14Fonts.FontName.HELVETICA
    );

    private PdfTestData() {
    }

    public static byte[] pdfWithPages(String... pageTexts) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (String pageText : pageTexts) {
                PDPage page = new PDPage();
                document.addPage(page);

                if (pageText != null && !pageText.isBlank()) {
                    writeText(document, page, pageText);
                }
            }

            document.save(output);
            return output.toByteArray();
        }
    }

    private static void writeText(
            PDDocument document,
            PDPage page,
            String pageText
    ) throws IOException {
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            content.beginText();
            content.setFont(FONT, 12);
            content.newLineAtOffset(72, 720);
            content.showText(pageText);
            content.endText();
        }
    }
}
