package br.com.vpsconsulting.orderhub.dto.itens;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ItemPedidoDTO(
        @NotBlank(message = "Nome do produto é obrigatório")
        String produto,

        @NotNull(message = "Quantidade é obrigatória")
        @Min(value = 1, message = "Quantidade deve ser maior que zero")
        Integer quantidade,

        @NotNull(message = "Preço unitário é obrigatório")
        @DecimalMin(value = "0.01", message = "Preço unitário deve ser maior que zero")
        BigDecimal precoUnitario
) {}