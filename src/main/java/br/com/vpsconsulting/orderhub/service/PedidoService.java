// src/main/java/br/com/vpsconsulting/orderhub/service/PedidoService.java
package br.com.vpsconsulting.orderhub.service;

import br.com.vpsconsulting.orderhub.dto.pedidos.AtualizarStatusDTO;
import br.com.vpsconsulting.orderhub.dto.pedidos.CriarPedidoDTO;
import br.com.vpsconsulting.orderhub.dto.pedidos.PedidoResponseDTO;
import br.com.vpsconsulting.orderhub.dto.itens.ItemPedidoDTO;
import br.com.vpsconsulting.orderhub.dto.itens.ItemPedidoResponseDTO;
import br.com.vpsconsulting.orderhub.entity.ItemPedido;
import br.com.vpsconsulting.orderhub.entity.Parceiro;
import br.com.vpsconsulting.orderhub.entity.Pedido;
import br.com.vpsconsulting.orderhub.enums.StatusPedido;
import br.com.vpsconsulting.orderhub.exception.BusinessRuleException;
import br.com.vpsconsulting.orderhub.exception.EntityNotFoundException;
import br.com.vpsconsulting.orderhub.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ParceiroService parceiroService;
    private final NotificacaoService notificacaoService;

    public PedidoResponseDTO criarPedido(CriarPedidoDTO dto) {
        log.info("Criando pedido para parceiro: {}", dto.parceiroPublicId());

        // Buscar parceiro
        Parceiro parceiro = parceiroService.buscarPorPublicId(dto.parceiroPublicId());

        // Criar pedido
        Pedido pedido = new Pedido(parceiro);
        pedido.setObservacoes(dto.observacoes());

        // Adicionar itens
        for (ItemPedidoDTO itemDto : dto.itens()) {
            ItemPedido item = new ItemPedido(
                    pedido,
                    itemDto.produto(),
                    itemDto.quantidade(),
                    itemDto.precoUnitario()
            );
            pedido.adicionarItem(item);
        }

        // Calcular valor total
        pedido.calcularValorTotal();

        // Verificar se parceiro tem crédito suficiente
        if (!parceiroService.temCreditoSuficiente(dto.parceiroPublicId(), pedido.getValorTotal())) {
            throw BusinessRuleException.creditoInsuficiente(
                    parceiro.getCreditoDisponivel(),
                    pedido.getValorTotal()
            );
        }

        // Salvar pedido
        pedido = pedidoRepository.save(pedido);

        log.info("Pedido criado com sucesso. PublicId: {}", pedido.getPublicId());

        return convertToResponseDTO(pedido);
    }

    @Transactional(readOnly = true)
    public PedidoResponseDTO buscarPorId(String publicId) {
        Pedido pedido = pedidoRepository.findByPublicIdWithItens(publicId)
                .orElseThrow(() -> EntityNotFoundException.pedido(publicId));

        return convertToResponseDTO(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> buscarPedidos(LocalDateTime dataInicio, LocalDateTime dataFim, StatusPedido status) {
        List<Pedido> pedidos;

        if (dataInicio != null && dataFim != null) {
            log.info("Buscando pedidos por período: {} até {}", dataInicio, dataFim);
            pedidos = pedidoRepository.findByDataCriacaoBetweenOrderByDataCriacaoDesc(dataInicio, dataFim);
        } else if (status != null) {
            log.info("Buscando pedidos por status: {}", status);
            pedidos = pedidoRepository.findByStatusOrderByDataCriacaoDesc(status);
        } else {
            log.info("Buscando todos os pedidos");
            pedidos = pedidoRepository.findAllByOrderByDataCriacaoDesc();
        }

        return pedidos.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public PedidoResponseDTO aprovarStatus(String publicId, AtualizarStatusDTO dto) {
        log.info("Atualizando status do pedido {} para {}", publicId, dto.status());

        Pedido pedido = pedidoRepository.findByPublicId(publicId)
                .orElseThrow(() -> EntityNotFoundException.pedido(publicId));

        StatusPedido statusAnterior = pedido.getStatus();

        // Ao aprovar um pedido, debitar o valor do crédito do parceiro
        if (dto.status() == StatusPedido.APROVADO && statusAnterior == StatusPedido.PENDENTE) {
            parceiroService.debitarCredito(pedido.getParceiro().getPublicId(), pedido.getValorTotal());
        }

        // Atualizar status
        pedido.atualizarStatus(dto.status());
        pedido = pedidoRepository.save(pedido);

        // Notificação para mudanças de status
        notificacaoService.notificarMudancaStatus(pedido, statusAnterior, dto.status());

        return convertToResponseDTO(pedido);
    }

    public PedidoResponseDTO cancelarPedido(String publicId) {
        log.info("Cancelando pedido: {}", publicId);

        Pedido pedido = pedidoRepository.findByPublicId(publicId)
                .orElseThrow(() -> EntityNotFoundException.pedido(publicId));

        StatusPedido statusAnterior = pedido.getStatus();

        // Liberar crédito se pedido foi aprovado
        if (statusAnterior == StatusPedido.APROVADO) {
            parceiroService.liberarCredito(pedido.getParceiro().getPublicId(), pedido.getValorTotal());
        }

        // Cancelar pedido
        pedido.cancelar();
        pedido = pedidoRepository.save(pedido);

        // Notificação para mudanças de status
        notificacaoService.notificarMudancaStatus(pedido, statusAnterior, StatusPedido.CANCELADO);

        return convertToResponseDTO(pedido);
    }

    private PedidoResponseDTO convertToResponseDTO(Pedido pedido) {
        List<ItemPedidoResponseDTO> itensDto = pedido.getItens().stream()
                .map(item -> new ItemPedidoResponseDTO(
                        item.getId(),
                        item.getProduto(),
                        item.getQuantidade(),
                        item.getPrecoUnitario(),
                        item.getSubtotal()
                ))
                .collect(Collectors.toList());

        return new PedidoResponseDTO(
                pedido.getPublicId(),
                pedido.getParceiro().getPublicId(),
                pedido.getParceiro().getNome(),
                itensDto,
                pedido.getValorTotal(),
                pedido.getStatus(),
                pedido.getObservacoes(),
                pedido.getDataCriacao(),
                pedido.getDataAtualizacao()
        );
    }
}