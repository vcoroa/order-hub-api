package br.com.vpsconsulting.orderhub.dto.pedidos;

import br.com.vpsconsulting.orderhub.dto.itens.ItemPedidoResponseDTO;
import br.com.vpsconsulting.orderhub.enums.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponseDTO(
        String publicId,
        String parceiroPublicId,
        String nomeParceiro,
        List<ItemPedidoResponseDTO> itens,
        BigDecimal valorTotal,
        StatusPedido status,
        String observacoes,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao
) {}