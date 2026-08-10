package org.brian.aisupportagent.dto;

import java.util.List;

public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public PagedResponse {
        content = List.copyOf(content);
    }
}
