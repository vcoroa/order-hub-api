package br.com.vpsconsulting.orderhub.dto.itens;

import java.math.BigDecimal;

public record ItemPedidoResponseDTO(
        Long id,
        String produto,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {}