package br.com.vpsconsulting.orderhub.dto.parceiros;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record CriarParceiroDTO(
        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @NotBlank(message = "CNPJ é obrigatório")
        @Pattern(regexp = "\\d{14}", message = "CNPJ deve ter 14 dígitos")
        String cnpj,

        @NotNull(message = "Limite de crédito é obrigatório")
        @DecimalMin(value = "0.01", message = "Limite de crédito deve ser maior que zero")
        BigDecimal limiteCredito
) {}