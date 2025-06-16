package br.com.vpsconsulting.orderhub.dto.parceiros;

import java.math.BigDecimal;

public record CreditoConsultaDTO(
        String parceiroPublicId,
        String nomeParceiro,
        BigDecimal limiteCredito,
        BigDecimal creditoUtilizado,
        BigDecimal creditoDisponivel,
        Boolean temCreditoSuficiente,
        BigDecimal valorConsulta
) {
    // Factory method para consulta simples (sem valor específico)
    public static CreditoConsultaDTO of(String publicId, String nome, BigDecimal limite, BigDecimal utilizado) {
        BigDecimal disponivel = limite.subtract(utilizado);
        return new CreditoConsultaDTO(publicId, nome, limite, utilizado, disponivel, null, null);
    }

    // Factory method para consulta com valor específico
    public static CreditoConsultaDTO of(String publicId, String nome, BigDecimal limite,
                                        BigDecimal utilizado, BigDecimal valorConsulta) {
        BigDecimal disponivel = limite.subtract(utilizado);
        Boolean temCredito = disponivel.compareTo(valorConsulta) >= 0;
        return new CreditoConsultaDTO(publicId, nome, limite, utilizado, disponivel, temCredito, valorConsulta);
    }
}