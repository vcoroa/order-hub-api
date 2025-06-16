package br.com.vpsconsulting.orderhub.service;

import br.com.vpsconsulting.orderhub.entity.Parceiro;
import br.com.vpsconsulting.orderhub.entity.Pedido;
import br.com.vpsconsulting.orderhub.enums.StatusPedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacaoService - Testes Unitários")
class NotificacaoServiceTest {

    @InjectMocks
    private NotificacaoService notificacaoService;

    private Parceiro parceiro;
    private Pedido pedido;

    @BeforeEach
    void setUp() {
        parceiro = new Parceiro("Empresa Teste", "12345678000195", new BigDecimal("10000.00"));
        pedido = new Pedido(parceiro);
        pedido.setValorTotal(new BigDecimal("1500.00"));
    }

    @Test
    @DisplayName("Deve notificar mudança de status com sucesso")
    void deveNotificarMudancaStatusComSucesso() {
        // Given
        StatusPedido statusAnterior = StatusPedido.PENDENTE;
        StatusPedido novoStatus = StatusPedido.APROVADO;

        // When & Then - Não deve lançar exceção
        assertDoesNotThrow(() ->
                notificacaoService.notificarMudancaStatus(pedido, statusAnterior, novoStatus)
        );
    }

    @Test
    @DisplayName("Deve notificar mudança de status para cancelado")
    void deveNotificarMudancaStatusParaCancelado() {
        // Given
        StatusPedido statusAnterior = StatusPedido.APROVADO;
        StatusPedido novoStatus = StatusPedido.CANCELADO;

        // When & Then - Não deve lançar exceção
        assertDoesNotThrow(() ->
                notificacaoService.notificarMudancaStatus(pedido, statusAnterior, novoStatus)
        );
    }

    @Test
    @DisplayName("Deve notificar mudança de status para entregue")
    void deveNotificarMudancaStatusParaEntregue() {
        // Given
        StatusPedido statusAnterior = StatusPedido.APROVADO;
        StatusPedido novoStatus = StatusPedido.ENTREGUE;

        // When & Then - Não deve lançar exceção
        assertDoesNotThrow(() ->
                notificacaoService.notificarMudancaStatus(pedido, statusAnterior, novoStatus)
        );
    }

    @Test
    @DisplayName("Deve lidar com pedido com valor zero")
    void deveLidarComPedidoComValorZero() {
        // Given
        pedido.setValorTotal(BigDecimal.ZERO);
        StatusPedido statusAnterior = StatusPedido.PENDENTE;
        StatusPedido novoStatus = StatusPedido.APROVADO;

        // When & Then - Não deve lançar exceção
        assertDoesNotThrow(() ->
                notificacaoService.notificarMudancaStatus(pedido, statusAnterior, novoStatus)
        );
    }

    @Test
    @DisplayName("Deve lidar com parceiro com nome especial")
    void deveLidarComParceiroComNomeEspecial() {
        // Given
        Parceiro parceiroEspecial = new Parceiro("Empresa & Cia Ltda.", "98765432000111", new BigDecimal("5000.00"));
        Pedido pedidoEspecial = new Pedido(parceiroEspecial);
        pedidoEspecial.setValorTotal(new BigDecimal("2500.00"));

        StatusPedido statusAnterior = StatusPedido.PENDENTE;
        StatusPedido novoStatus = StatusPedido.APROVADO;

        // When & Then - Não deve lançar exceção
        assertDoesNotThrow(() ->
                notificacaoService.notificarMudancaStatus(pedidoEspecial, statusAnterior, novoStatus)
        );
    }

    @Test
    @DisplayName("Deve processar notificação mesmo com status iguais")
    void deveProcessarNotificacaoMesmoComStatusIguais() {
        // Given
        StatusPedido statusAnterior = StatusPedido.PENDENTE;
        StatusPedido novoStatus = StatusPedido.PENDENTE;

        // When & Then - Não deve lançar exceção
        assertDoesNotThrow(() ->
                notificacaoService.notificarMudancaStatus(pedido, statusAnterior, novoStatus)
        );
    }
}