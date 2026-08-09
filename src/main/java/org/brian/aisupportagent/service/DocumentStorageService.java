package org.brian.aisupportagent.service;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentStorageService {

    StoredDocumentFile store(MultipartFile file);

    void delete(String storageKey);
}
