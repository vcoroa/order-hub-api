package br.com.vpsconsulting.orderhub.dto.utils;

import java.util.List;

public record PaginacaoDTO<T>(
        List<T> conteudo,
        int paginaAtual,
        int tamanho,
        long totalElementos,
        int totalPaginas,
        boolean primeira,
        boolean ultima,
        boolean vazia
) {
    // Factory method para criar a partir de Page do Spring
    public static <T> PaginacaoDTO<T> of(org.springframework.data.domain.Page<T> page) {
        return new PaginacaoDTO<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty()
        );
    }
}