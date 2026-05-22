package com.stockfolio.global.common;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class PageResponse<T> {

    private final List<T> content;
    private final PageMeta meta;

    private PageResponse(Page<T> page) {
        this.content = page.getContent();
        this.meta = PageMeta.of(page);
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page);
    }

    @Getter
    public static class PageMeta {
        private final int page;
        private final int size;
        private final long totalElements;
        private final int totalPages;

        private PageMeta(Page<?> page) {
            this.page = page.getNumber();
            this.size = page.getSize();
            this.totalElements = page.getTotalElements();
            this.totalPages = page.getTotalPages();
        }

        public static PageMeta of(Page<?> page) {
            return new PageMeta(page);
        }
    }
}
