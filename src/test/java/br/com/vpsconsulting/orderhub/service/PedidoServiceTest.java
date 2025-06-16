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
import br.com.vpsconsulting.orderhub.repository.ParceiroRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoService - Testes Unitários")
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ParceiroRepository parceiroRepository;

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
        pedido.setStatus(StatusPedido.APROVADO); // Pedidos são criados já aprovados na nova arquitetura

        // Adicionar item para que o pedido seja válido
        ItemPedido item = new ItemPedido(pedido, "Produto Teste", 2, new BigDecimal("750.00"));
        pedido.adicionarItem(item);
    }

    @Test
    @DisplayName("Deve criar pedido com sucesso e aprovar automaticamente")
    void deveCriarPedidoComSucessoEAprovarAutomaticamente() {
        // Given
        List<ItemPedidoDTO> itens = Arrays.asList(
                new ItemPedidoDTO("Produto A", 2, new BigDecimal("500.00")),
                new ItemPedidoDTO("Produto B", 1, new BigDecimal("500.00"))
        );

        CriarPedidoDTO dto = new CriarPedidoDTO(parceiroPublicId, itens, "Observações do pedido");

        // Mock para buscar parceiro com lock
        when(parceiroRepository.findByPublicIdWithLock(parceiroPublicId)).thenReturn(Optional.of(parceiro));
        when(parceiroRepository.save(any(Parceiro.class))).thenReturn(parceiro);
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        // When
        PedidoResponseDTO resultado = pedidoService.criarPedido(dto);

        // Then
        assertNotNull(resultado);
        assertEquals(publicId, resultado.publicId());
        assertEquals(parceiroPublicId, resultado.parceiroPublicId());
        assertEquals("Empresa Teste", resultado.nomeParceiro());
        assertEquals(StatusPedido.APROVADO, resultado.status()); // Pedido criado já aprovado

        verify(parceiroRepository).findByPublicIdWithLock(parceiroPublicId);
        verify(parceiroRepository).save(parceiro);
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

        // Parceiro com crédito insuficiente
        parceiro.utilizarCredito(new BigDecimal("9500.00")); // Sobram apenas 500

        when(parceiroRepository.findByPublicIdWithLock(parceiroPublicId)).thenReturn(Optional.of(parceiro));

        // When & Then
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> pedidoService.criarPedido(dto)
        );

        assertNotNull(exception);
        assertEquals("CREDITO_INSUFICIENTE", exception.getCodigo());
        assertTrue(exception.getMessage().toLowerCase().contains("crédito"));

        verify(parceiroRepository).findByPublicIdWithLock(parceiroPublicId);
        verify(parceiroRepository, never()).save(any());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando parceiro está inativo")
    void deveLancarExcecaoQuandoParceiroEstaInativo() {
        // Given
        List<ItemPedidoDTO> itens = Arrays.asList(
                new ItemPedidoDTO("Produto", 1, new BigDecimal("1000.00"))
        );

        CriarPedidoDTO dto = new CriarPedidoDTO(parceiroPublicId, itens, "Pedido");

        parceiro.setAtivo(false);
        when(parceiroRepository.findByPublicIdWithLock(parceiroPublicId)).thenReturn(Optional.of(parceiro));

        // When & Then
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> pedidoService.criarPedido(dto)
        );

        assertTrue(exception.getMessage().toLowerCase().contains("inativo"));
        verify(parceiroRepository).findByPublicIdWithLock(parceiroPublicId);
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
    @DisplayName("Deve buscar pedidos por status")
    void deveBuscarPedidosPorStatus() {
        // Given
        StatusPedido status = StatusPedido.APROVADO;
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
    @DisplayName("Deve atualizar status para EM_PROCESSAMENTO sem operações de crédito")
    void deveAtualizarStatusParaEmProcessamentoSemOperacoesDeCredito() {
        // Given
        AtualizarStatusDTO dto = new AtualizarStatusDTO(StatusPedido.EM_PROCESSAMENTO);
        pedido.setStatus(StatusPedido.APROVADO);

        when(pedidoRepository.findByPublicId(publicId)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        // When
        PedidoResponseDTO resultado = pedidoService.atualizarStatus(publicId, dto);

        // Then
        assertNotNull(resultado);
        assertEquals(StatusPedido.EM_PROCESSAMENTO, resultado.status());

        verify(pedidoRepository).findByPublicId(publicId);
        verify(pedidoRepository).save(pedido);
        verify(parceiroRepository, never()).findByPublicIdWithLock(any());
        verify(notificacaoService).notificarMudancaStatus(pedido, StatusPedido.APROVADO, StatusPedido.EM_PROCESSAMENTO);
    }

    @Test
    @DisplayName("Deve cancelar pedido aprovado e liberar crédito")
    void deveCancelarPedidoAprovadoELiberarCredito() {
        // Given
        pedido.setStatus(StatusPedido.APROVADO);

        when(pedidoRepository.findByPublicId(publicId)).thenReturn(Optional.of(pedido));
        when(parceiroRepository.findByPublicIdWithLock(parceiroPublicId)).thenReturn(Optional.of(parceiro));
        when(parceiroRepository.save(any(Parceiro.class))).thenReturn(parceiro);
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        // When
        PedidoResponseDTO resultado = pedidoService.cancelarPedido(publicId);

        // Then
        assertNotNull(resultado);
        assertEquals(StatusPedido.CANCELADO, resultado.status());

        verify(pedidoRepository).findByPublicId(publicId);
        verify(parceiroRepository).findByPublicIdWithLock(parceiroPublicId);
        verify(parceiroRepository).save(parceiro);
        verify(pedidoRepository).save(pedido);
        verify(notificacaoService).notificarMudancaStatus(pedido, StatusPedido.APROVADO, StatusPedido.CANCELADO);
    }

    @Test
    @DisplayName("Deve cancelar pedido pendente sem liberar crédito")
    void deveCancelarPedidoPendenteSemLiberarCredito() {
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
        verify(parceiroRepository, never()).findByPublicIdWithLock(any());
        verify(pedidoRepository).save(pedido);
        verify(notificacaoService).notificarMudancaStatus(pedido, StatusPedido.PENDENTE, StatusPedido.CANCELADO);
    }

    @Test
    @DisplayName("Deve lançar exceção para pedido não encontrado")
    void deveLancarExcecaoParaPedidoNaoEncontrado() {
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
}