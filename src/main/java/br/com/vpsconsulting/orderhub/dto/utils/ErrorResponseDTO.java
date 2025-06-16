package br.com.vpsconsulting.orderhub.dto.utils;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponseDTO(
        String erro,
        String mensagem,
        List<String> detalhes,
        String path,
        LocalDateTime timestamp
) {
    // Factory method para erro simples
    public static ErrorResponseDTO of(String erro, String mensagem, String path) {
        return new ErrorResponseDTO(erro, mensagem, null, path, LocalDateTime.now());
    }

    // Factory method para erro com detalhes
    public static ErrorResponseDTO of(String erro, String mensagem, List<String> detalhes, String path) {
        return new ErrorResponseDTO(erro, mensagem, detalhes, path, LocalDateTime.now());
    }
}