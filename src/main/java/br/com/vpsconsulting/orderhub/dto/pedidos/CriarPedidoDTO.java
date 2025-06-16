package br.com.vpsconsulting.orderhub.dto.pedidos;

import br.com.vpsconsulting.orderhub.dto.itens.ItemPedidoDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CriarPedidoDTO(
        @NotBlank(message = "ID público do parceiro é obrigatório")
        String parceiroPublicId,

        @NotEmpty(message = "Lista de itens não pode estar vazia")
        @Valid
        List<ItemPedidoDTO> itens,

        @Size(max = 500, message = "Observações não podem exceder 500 caracteres")
        String observacoes
) {}