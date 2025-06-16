package br.com.vpsconsulting.orderhub.service;

import br.com.vpsconsulting.orderhub.dto.itens.ItemPedidoDTO;
import br.com.vpsconsulting.orderhub.dto.pedidos.AtualizarStatusDTO;
import br.com.vpsconsulting.orderhub.dto.pedidos.CriarPedidoDTO;
import br.com.vpsconsulting.orderhub.dto.pedidos.PedidoResponseDTO;
import br.com.vpsconsulting.orderhub.entity.ItemPedido;
import br.com.vpsconsulting.orderhub.entity.Parceiro;
import br.com.vpsconsulting.orderhub.entity.Pedido;
import br.com.vpsconsulting.orderhub.enums.StatusPedido;
import br.com.vpsconsulting.orderhub.exception.BusinessRuleException;
import br.com.vpsconsulting.orderhub.exception.EntityNotFoundException;
import br.com.vpsconsulting.orderhub.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoService - Testes Unitários")
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ParceiroService parceiroService;

    @Mock
    private NotificacaoService notificacaoService;

    @InjectMocks
    private PedidoService pedidoService;

    private Parceiro parceiro;
    private Pedido pedido;
    private String publicId;
    private String parceiroPublicId;

    @BeforeEach
    void setUp() {
        publicId = "PED_ABC123";
        parceiroPublicId = "PARC_XYZ789";

        parceiro = new Parceiro("Empresa Teste", "12345678000195", new BigDecimal("10000.00"));
        parceiro.setPublicId(parceiroPublicId);

        pedido = new Pedido(parceiro);
        pedido.setPublicId(publicId);
        pedido.setValorTotal(new BigDecimal("1500.00"));
        pedido.setObservacoes("Pedido de teste");

        // IMPORTANTE: Adicionar item para que o pedido possa ser aprovado
        ItemPedido item = new ItemPedido(pedido, "Produto Teste", 2, new BigDecimal("750.00"));
        pedido.adicionarItem(item);
    }

    @Test
    @DisplayName("Deve criar pedido com sucesso")
    void deveCriarPedidoComSucesso() {
        // Given
        List<ItemPedidoDTO> itens = Arrays.asList(
                new ItemPedidoDTO("Produto A", 2, new BigDecimal("500.00")),
                new ItemPedidoDTO("Produto B", 1, new BigDecimal("500.00"))
        );

        CriarPedidoDTO dto = new CriarPedidoDTO(parceiroPublicId, itens, "Observações do pedido");

        when(parceiroService.buscarPorPublicId(parceiroPublicId)).thenReturn(parceiro);
        when(parceiroService.temCreditoSuficiente(eq(parceiroPublicId), any(BigDecimal.class))).thenReturn(true);
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        // When
        PedidoResponseDTO resultado = pedidoService.criarPedido(dto);

        // Then
        assertNotNull(resultado);
        assertEquals(publicId, resultado.publicId());
        assertEquals(parceiroPublicId, resultado.parceiroPublicId());
        assertEquals("Empresa Teste", resultado.nomeParceiro());
        assertEquals("Pedido de teste", resultado.observacoes()); // observação do pedido mock

        verify(parceiroService).buscarPorPublicId(parceiroPublicId);
        verify(parceiroService).temCreditoSuficiente(eq(parceiroPublicId), any(BigDecimal.class));
        verify(pedidoRepository).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Deve lançar BusinessRuleException quando parceiro não tem crédito suficiente")
    void deveLancarExcecaoQuandoParceiroNaoTemCreditoSuficiente() {
        // Given
        List<ItemPedidoDTO> itens = Arrays.asList(
                new ItemPedidoDTO("Produto Caro", 1, new BigDecimal("15000.00"))
        );

        CriarPedidoDTO dto = new CriarPedidoDTO(parceiroPublicId, itens, "Pedido caro");

        when(parceiroService.buscarPorPublicId(parceiroPublicId)).thenReturn(parceiro);
        when(parceiroService.temCreditoSuficiente(eq(parceiroPublicId), any(BigDecimal.class))).thenReturn(false);

        // When & Then
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> pedidoService.criarPedido(dto)
        );

        // Verificar que a exceção foi lançada com a estrutura correta
        assertNotNull(exception);
        assertNotNull(exception.getMessage());
        assertNotNull(exception.getCodigo());

        // A mensagem deve mencionar crédito insuficiente (baseado no método creditoInsuficiente)
        String mensagem = exception.getMessage().toLowerCase();
        assertTrue(
                mensagem.contains("crédito") && mensagem.contains("insuficiente"),
                "Mensagem deveria mencionar 'crédito insuficiente'. Mensagem atual: " + exception.getMessage()
        );

        // Verificar código da exceção
        assertEquals("CREDITO_INSUFICIENTE", exception.getCodigo());

        verify(parceiroService).buscarPorPublicId(parceiroPublicId);
        verify(parceiroService).temCreditoSuficiente(eq(parceiroPublicId), any(BigDecimal.class));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve buscar pedido por ID com sucesso")
    void deveBuscarPedidoPorIdComSucesso() {
        // Given
        when(pedidoRepository.findByPublicIdWithItens(publicId)).thenReturn(Optional.of(pedido));

        // When
        PedidoResponseDTO resultado = pedidoService.buscarPorId(publicId);

        // Then
        assertNotNull(resultado);
        assertEquals(publicId, resultado.publicId());
        assertEquals(parceiroPublicId, resultado.parceiroPublicId());
        assertEquals("Empresa Teste", resultado.nomeParceiro());
        verify(pedidoRepository).findByPublicIdWithItens(publicId);
    }

    @Test
    @DisplayName("Deve lançar exceção quando pedido não encontrado")
    void deveLancarExcecaoQuandoPedidoNaoEncontrado() {
        // Given
        when(pedidoRepository.findByPublicIdWithItens(publicId)).thenReturn(Optional.empty());

        // When & Then
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> pedidoService.buscarPorId(publicId)
        );

        assertTrue(exception.getMessage().contains(publicId));
        verify(pedidoRepository).findByPublicIdWithItens(publicId);
    }

    @Test
    @DisplayName("Deve buscar pedidos por período")
    void deveBuscarPedidosPorPeriodo() {
        // Given
        LocalDateTime dataInicio = LocalDateTime.now().minusDays(7);
        LocalDateTime dataFim = LocalDateTime.now();

        List<Pedido> pedidos = Arrays.asList(pedido);
        when(pedidoRepository.findByDataCriacaoBetweenOrderByDataCriacaoDesc(dataInicio, dataFim))
                .thenReturn(pedidos);

        // When
        List<PedidoResponseDTO> resultado = pedidoService.buscarPedidos(dataInicio, dataFim, null);

        // Then
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(publicId, resultado.get(0).publicId());
        verify(pedidoRepository).findByDataCriacaoBetweenOrderByDataCriacaoDesc(dataInicio, dataFim);
    }

    @Test
    @DisplayName("Deve buscar pedidos por status")
    void deveBuscarPedidosPorStatus() {
        // Given
        StatusPedido status = StatusPedido.PENDENTE;
        List<Pedido> pedidos = Arrays.asList(pedido);
        when(pedidoRepository.findByStatusOrderByDataCriacaoDesc(status)).thenReturn(pedidos);

        // When
        List<PedidoResponseDTO> resultado = pedidoService.buscarPedidos(null, null, status);

        // Then
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(publicId, resultado.get(0).publicId());
        verify(pedidoRepository).findByStatusOrderByDataCriacaoDesc(status);
    }

    @Test
    @DisplayName("Deve buscar todos os pedidos quando não há filtros")
    void deveBuscarTodosOsPedidosQuandoNaoHaFiltros() {
        // Given
        List<Pedido> pedidos = Arrays.asList(pedido);
        when(pedidoRepository.findAllByOrderByDataCriacaoDesc()).thenReturn(pedidos);

        // When
        List<PedidoResponseDTO> resultado = pedidoService.buscarPedidos(null, null, null);

        // Then
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(publicId, resultado.get(0).publicId());
        verify(pedidoRepository).findAllByOrderByDataCriacaoDesc();
    }

    @Test
    @DisplayName("Deve aprovar pedido e debitar crédito")
    void deveAprovarPedidoEDebitarCredito() {
        // Given
        AtualizarStatusDTO dto = new AtualizarStatusDTO(StatusPedido.APROVADO);
        pedido.setStatus(StatusPedido.PENDENTE);

        when(pedidoRepository.findByPublicId(publicId)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        // When
        PedidoResponseDTO resultado = pedidoService.aprovarStatus(publicId, dto);

        // Then
        assertNotNull(resultado);
        assertEquals(StatusPedido.APROVADO, resultado.status());

        verify(pedidoRepository).findByPublicId(publicId);
        verify(parceiroService).debitarCredito(parceiroPublicId, pedido.getValorTotal());
        verify(pedidoRepository).save(pedido);
        verify(notificacaoService).notificarMudancaStatus(pedido, StatusPedido.PENDENTE, StatusPedido.APROVADO);
    }

    @Test
    @DisplayName("Deve atualizar status sem debitar crédito quando não é aprovação")
    void deveAtualizarStatusSemDebitarCreditoQuandoNaoEAprovacao() {
        // Given - Testando transição de APROVADO para EM_PROCESSAMENTO (válida e não é aprovação)
        AtualizarStatusDTO dto = new AtualizarStatusDTO(StatusPedido.EM_PROCESSAMENTO);
        pedido.setStatus(StatusPedido.APROVADO);

        when(pedidoRepository.findByPublicId(publicId)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        // When
        PedidoResponseDTO resultado = pedidoService.aprovarStatus(publicId, dto);

        // Then
        assertNotNull(resultado);
        assertEquals(StatusPedido.EM_PROCESSAMENTO, resultado.status());

        verify(pedidoRepository).findByPublicId(publicId);
        verify(parceiroService, never()).debitarCredito(anyString(), any(BigDecimal.class));
        verify(pedidoRepository).save(pedido);
        verify(notificacaoService).notificarMudancaStatus(pedido, StatusPedido.APROVADO, StatusPedido.EM_PROCESSAMENTO);
    }

    @Test
    @DisplayName("Deve atualizar status para entregue seguindo fluxo completo")
    void deveAtualizarStatusParaEntregueSegundoFluxoCompleto() {
        // Given - Testando transição de ENVIADO para ENTREGUE (fluxo correto)
        AtualizarStatusDTO dto = new AtualizarStatusDTO(StatusPedido.ENTREGUE);
        pedido.setStatus(StatusPedido.ENVIADO); // Status correto para ir para ENTREGUE

        when(pedidoRepository.findByPublicId(publicId)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        // When
        PedidoResponseDTO resultado = pedidoService.aprovarStatus(publicId, dto);

        // Then
        assertNotNull(resultado);
        assertEquals(StatusPedido.ENTREGUE, resultado.status());

        verify(pedidoRepository).findByPublicId(publicId);
        verify(parceiroService, never()).debitarCredito(anyString(), any(BigDecimal.class));
        verify(pedidoRepository).save(pedido);
        verify(notificacaoService).notificarMudancaStatus(pedido, StatusPedido.ENVIADO, StatusPedido.ENTREGUE);
    }

    @Test
    @DisplayName("Deve cancelar pedido e liberar crédito quando estava aprovado")
    void deveCancelarPedidoELiberarCreditoQuandoEstavaAprovado() {
        // Given
        pedido.setStatus(StatusPedido.APROVADO);

        when(pedidoRepository.findByPublicId(publicId)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        // When
        PedidoResponseDTO resultado = pedidoService.cancelarPedido(publicId);

        // Then
        assertNotNull(resultado);
        assertEquals(StatusPedido.CANCELADO, resultado.status());

        verify(pedidoRepository).findByPublicId(publicId);
        verify(parceiroService).liberarCredito(parceiroPublicId, pedido.getValorTotal());
        verify(pedidoRepository).save(pedido);
        verify(notificacaoService).notificarMudancaStatus(pedido, StatusPedido.APROVADO, StatusPedido.CANCELADO);
    }

    @Test
    @DisplayName("Deve cancelar pedido sem liberar crédito quando estava pendente")
    void deveCancelarPedidoSemLiberarCreditoQuandoEstavaPendente() {
        // Given
        pedido.setStatus(StatusPedido.PENDENTE);

        when(pedidoRepository.findByPublicId(publicId)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        // When
        PedidoResponseDTO resultado = pedidoService.cancelarPedido(publicId);

        // Then
        assertNotNull(resultado);
        assertEquals(StatusPedido.CANCELADO, resultado.status());

        verify(pedidoRepository).findByPublicId(publicId);
        verify(parceiroService, never()).liberarCredito(anyString(), any(BigDecimal.class));
        verify(pedidoRepository).save(pedido);
        verify(notificacaoService).notificarMudancaStatus(pedido, StatusPedido.PENDENTE, StatusPedido.CANCELADO);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar aprovar pedido não encontrado")
    void deveLancarExcecaoAoTentarAprovarPedidoNaoEncontrado() {
        // Given
        AtualizarStatusDTO dto = new AtualizarStatusDTO(StatusPedido.APROVADO);
        when(pedidoRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        // When & Then
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> pedidoService.aprovarStatus(publicId, dto)
        );

        assertTrue(exception.getMessage().contains(publicId));
        verify(pedidoRepository).findByPublicId(publicId);
        verify(parceiroService, never()).debitarCredito(anyString(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar cancelar pedido não encontrado")
    void deveLancarExcecaoAoTentarCancelarPedidoNaoEncontrado() {
        // Given
        when(pedidoRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        // When & Then
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> pedidoService.cancelarPedido(publicId)
        );

        assertTrue(exception.getMessage().contains(publicId));
        verify(pedidoRepository).findByPublicId(publicId);
        verify(parceiroService, never()).liberarCredito(anyString(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Deve lançar exceção para transição de status inválida")
    void deveLancarExcecaoParaTransicaoDeStatusInvalida() {
        // Given - Testando uma transição que sabemos ser inválida
        AtualizarStatusDTO dto = new AtualizarStatusDTO(StatusPedido.PENDENTE);
        pedido.setStatus(StatusPedido.APROVADO); // Não deveria poder voltar para PENDENTE

        when(pedidoRepository.findByPublicId(publicId)).thenReturn(Optional.of(pedido));

        // When & Then
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> pedidoService.aprovarStatus(publicId, dto)
        );

        assertTrue(exception.getMessage().contains("status"));
        verify(pedidoRepository).findByPublicId(publicId);
        verify(parceiroService, never()).debitarCredito(anyString(), any(BigDecimal.class));
        verify(parceiroService, never()).liberarCredito(anyString(), any(BigDecimal.class));
    }
}