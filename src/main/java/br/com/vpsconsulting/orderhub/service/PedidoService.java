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
import br.com.vpsconsulting.orderhub.repository.ParceiroRepository;
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
    private final ParceiroRepository parceiroRepository;
    private final ParceiroService parceiroService;
    private final NotificacaoService notificacaoService;

    public PedidoResponseDTO criarPedido(CriarPedidoDTO dto) {
        log.info("Criando pedido para parceiro: {}", dto.parceiroPublicId());

        // OPERAÇÃO ATÔMICA: buscar parceiro com lock para operação de crédito
        Parceiro parceiro = parceiroRepository.findByPublicIdWithLock(dto.parceiroPublicId())
                .orElseThrow(() -> EntityNotFoundException.parceiro(dto.parceiroPublicId()));

        // Verificar se parceiro está ativo
        if (!parceiro.getAtivo()) {
            throw BusinessRuleException.parceiroInativo(dto.parceiroPublicId());
        }

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

        // VERIFICAÇÃO E DÉBITO ATÔMICOS (com o parceiro já locked)
        if (!parceiro.temCreditoDisponivel(pedido.getValorTotal())) {
            throw BusinessRuleException.creditoInsuficiente(
                    parceiro.getCreditoDisponivel(),
                    pedido.getValorTotal()
            );
        }

        // Debitar crédito imediatamente (operação atômica)
        parceiro.utilizarCredito(pedido.getValorTotal());
        parceiroRepository.save(parceiro);

        // Definir pedido como APROVADO já que o crédito foi debitado
        pedido.atualizarStatus(StatusPedido.APROVADO);

        // Salvar pedido
        pedido = pedidoRepository.save(pedido);

        log.info("Pedido criado e aprovado com sucesso. PublicId: {} - Crédito restante: {}",
                pedido.getPublicId(), parceiro.getCreditoDisponivel());

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

    public PedidoResponseDTO atualizarStatus(String publicId, AtualizarStatusDTO dto) {
        log.info("Atualizando status do pedido {} para {}", publicId, dto.status());

        Pedido pedido = pedidoRepository.findByPublicId(publicId)
                .orElseThrow(() -> EntityNotFoundException.pedido(publicId));

        StatusPedido statusAnterior = pedido.getStatus();

        // Para mudanças que não envolvem crédito, não precisa de lock
        if (dto.status() != StatusPedido.APROVADO && dto.status() != StatusPedido.CANCELADO) {
            // Atualizar status simples (EM_PROCESSAMENTO, ENVIADO, ENTREGUE)
            pedido.atualizarStatus(dto.status());
            pedido = pedidoRepository.save(pedido);
        } else {
            // Para operações que envolvem crédito, usar lock
            return atualizarStatusComCredito(publicId, dto, statusAnterior);
        }

        // Notificação para mudanças de status
        notificacaoService.notificarMudancaStatus(pedido, statusAnterior, dto.status());

        return convertToResponseDTO(pedido);
    }

    private PedidoResponseDTO atualizarStatusComCredito(String publicId, AtualizarStatusDTO dto, StatusPedido statusAnterior) {
        // Buscar pedido novamente dentro da transação com operações de crédito
        Pedido pedido = pedidoRepository.findByPublicId(publicId)
                .orElseThrow(() -> EntityNotFoundException.pedido(publicId));

        // Extrair publicId do parceiro para usar na lambda
        String parceiroPublicId = pedido.getParceiro().getPublicId();

        // Buscar parceiro com lock para operações de crédito
        Parceiro parceiro = parceiroRepository.findByPublicIdWithLock(parceiroPublicId)
                .orElseThrow(() -> EntityNotFoundException.parceiro(parceiroPublicId));

        // Aprovar pedido pendente
        if (dto.status() == StatusPedido.APROVADO && statusAnterior == StatusPedido.PENDENTE) {
            // Verificar crédito e debitar
            if (!parceiro.temCreditoDisponivel(pedido.getValorTotal())) {
                throw BusinessRuleException.creditoInsuficiente(
                        parceiro.getCreditoDisponivel(),
                        pedido.getValorTotal()
                );
            }
            parceiro.utilizarCredito(pedido.getValorTotal());
            parceiroRepository.save(parceiro);
        }

        // Cancelar pedido aprovado
        if (dto.status() == StatusPedido.CANCELADO && statusAnterior == StatusPedido.APROVADO) {
            // Liberar crédito
            parceiro.liberarCredito(pedido.getValorTotal());
            parceiroRepository.save(parceiro);
        }

        // Atualizar status do pedido
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

        // Se pedido foi aprovado, precisa liberar crédito (usar lock)
        if (statusAnterior == StatusPedido.APROVADO) {
            // Extrair publicId do parceiro para usar na lambda
            String parceiroPublicId = pedido.getParceiro().getPublicId();

            // Buscar parceiro com lock para operação de crédito
            Parceiro parceiro = parceiroRepository.findByPublicIdWithLock(parceiroPublicId)
                    .orElseThrow(() -> EntityNotFoundException.parceiro(parceiroPublicId));

            // Liberar crédito
            parceiro.liberarCredito(pedido.getValorTotal());
            parceiroRepository.save(parceiro);

            log.info("Crédito liberado no cancelamento - Parceiro: {} - Valor: {} - Crédito disponível: {}",
                    parceiro.getPublicId(), pedido.getValorTotal(), parceiro.getCreditoDisponivel());
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