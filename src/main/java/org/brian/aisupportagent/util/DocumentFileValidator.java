package org.brian.aisupportagent.util;

import lombok.RequiredArgsConstructor;
import org.brian.aisupportagent.config.DocumentStorageProperties;
import org.brian.aisupportagent.exception.InvalidDocumentException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class DocumentFileValidator {

    private static final String PDF_SIGNATURE = "%PDF-";

    private final DocumentStorageProperties properties;

    public String validateAndGetOriginalFilename(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidDocumentException("A non-empty PDF file is required");
        }

        if (file.getSize() > properties.maxFileSize().toBytes()) {
            throw new InvalidDocumentException("The PDF exceeds the maximum allowed file size");
        }

        if (!MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(file.getContentType())) {
            throw new InvalidDocumentException("Only PDF files are supported");
        }

        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        if (!originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new InvalidDocumentException("The uploaded file must have a .pdf extension");
        }

        validatePdfSignature(file);
        return originalFilename;
    }

    private String sanitizeFilename(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            throw new InvalidDocumentException("The uploaded file must have a filename");
        }

        String normalizedFilename = originalFilename.replace('\\', '/');
        String filename = normalizedFilename.substring(normalizedFilename.lastIndexOf('/') + 1).trim();
        if (!StringUtils.hasText(filename) || filename.length() > 255) {
            throw new InvalidDocumentException("The PDF filename must be between 1 and 255 characters");
        }

        return filename;
    }

    private void validatePdfSignature(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            byte[] signatureBytes = input.readNBytes(PDF_SIGNATURE.length());
            String signature = new String(signatureBytes, StandardCharsets.US_ASCII);
            if (!PDF_SIGNATURE.equals(signature)) {
                throw new InvalidDocumentException("The uploaded file is not a valid PDF");
            }
        } catch (IOException exception) {
            throw new InvalidDocumentException("The uploaded PDF could not be read");
        }
    }
}
