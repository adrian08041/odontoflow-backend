package com.odontoflow.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Wrapper de paginação com contrato JSON estável.
 * Substitui a serialização direta de {@link Page} (PageImpl), cujo formato
 * interno não é garantido entre versões do Spring Data.
 *
 * Shape espelha {@code lib/types/index.ts:PageResponse<T>} do frontend.
 */
public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int number,
        int size
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }

    public <R> PageResponse<R> map(Function<T, R> mapper) {
        return new PageResponse<>(
                content.stream().map(mapper).toList(),
                totalElements, totalPages, number, size
        );
    }
}
