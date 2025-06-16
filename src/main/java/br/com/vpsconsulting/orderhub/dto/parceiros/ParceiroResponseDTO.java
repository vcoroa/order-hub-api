package br.com.vpsconsulting.orderhub.dto.parceiros;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ParceiroResponseDTO(
        String publicId,
        String nome,
        String cnpj,
        BigDecimal limiteCredito,
        BigDecimal creditoUtilizado,
        BigDecimal creditoDisponivel,
        Boolean ativo,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao
) {
    public static ParceiroResponseDTO from(br.com.vpsconsulting.orderhub.entity.Parceiro parceiro) {
        return new ParceiroResponseDTO(
                parceiro.getPublicId(),
                parceiro.getNome(),
                parceiro.getCnpj(),
                parceiro.getLimiteCredito(),
                parceiro.getCreditoUtilizado(),
                parceiro.getCreditoDisponivel(),
                parceiro.getAtivo(),
                parceiro.getDataCriacao(),
                parceiro.getDataAtualizacao()
        );
    }
}