package br.com.vpsconsulting.orderhub.dto.pedidos;

import br.com.vpsconsulting.orderhub.enums.StatusPedido;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusDTO(
        @NotNull(message = "Status é obrigatório")
        StatusPedido status
) {}