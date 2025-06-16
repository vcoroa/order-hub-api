package br.com.vpsconsulting.orderhub.controller;

import br.com.vpsconsulting.orderhub.dto.itens.ItemPedidoDTO;
import br.com.vpsconsulting.orderhub.dto.itens.ItemPedidoResponseDTO;
import br.com.vpsconsulting.orderhub.dto.pedidos.AtualizarStatusDTO;
import br.com.vpsconsulting.orderhub.dto.pedidos.CriarPedidoDTO;
import br.com.vpsconsulting.orderhub.dto.pedidos.PedidoResponseDTO;
import br.com.vpsconsulting.orderhub.enums.StatusPedido;
import br.com.vpsconsulting.orderhub.exception.BusinessRuleException;
import br.com.vpsconsulting.orderhub.exception.EntityNotFoundException;
import br.com.vpsconsulting.orderhub.service.ParceiroService;
import br.com.vpsconsulting.orderhub.service.PedidoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PedidosController.class)
@DisplayName("PedidosController - Testes Unitários")
class PedidoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PedidoService pedidoService;

    @MockBean
    private ParceiroService parceiroService;

    @Autowired
    private ObjectMapper objectMapper;

    private PedidoResponseDTO pedidoResponseDTO;
    private CriarPedidoDTO criarPedidoDTO;
    private AtualizarStatusDTO atualizarStatusDTO;
    private String publicId;
    private String parceiroPublicId;

    @BeforeEach
    void setUp() {
        publicId = "PED_ABC123";
        parceiroPublicId = "PARC_XYZ789";

        // DTOs de entrada
        List<ItemPedidoDTO> itens = Arrays.asList(
                new ItemPedidoDTO("Produto A", 2, new BigDecimal("500.00")),
                new ItemPedidoDTO("Produto B", 1, new BigDecimal("300.00"))
        );

        criarPedidoDTO = new CriarPedidoDTO(parceiroPublicId, itens, "Observações do pedido");
        atualizarStatusDTO = new AtualizarStatusDTO(StatusPedido.EM_PROCESSAMENTO);

        // DTO de resposta
        List<ItemPedidoResponseDTO> itensResponse = Arrays.asList(
                new ItemPedidoResponseDTO(1L, "Produto A", 2, new BigDecimal("500.00"), new BigDecimal("1000.00")),
                new ItemPedidoResponseDTO(2L, "Produto B", 1, new BigDecimal("300.00"), new BigDecimal("300.00"))
        );

        pedidoResponseDTO = new PedidoResponseDTO(
                publicId,
                parceiroPublicId,
                "Empresa Teste",
                itensResponse,
                new BigDecimal("1300.00"),
                StatusPedido.APROVADO,
                "Observações do pedido",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("Deve criar pedido com sucesso")
    void deveCriarPedidoComSucesso() throws Exception {
        // Given
        when(pedidoService.criarPedido(any(CriarPedidoDTO.class))).thenReturn(pedidoResponseDTO);

        // When & Then
        mockMvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(criarPedidoDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicId").value(publicId))
                .andExpect(jsonPath("$.parceiroPublicId").value(parceiroPublicId))
                .andExpect(jsonPath("$.nomeParceiro").value("Empresa Teste"))
                .andExpect(jsonPath("$.valorTotal").value(1300.00))
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.observacoes").value("Observações do pedido"))
                .andExpect(jsonPath("$.itens").isArray())
                .andExpect(jsonPath("$.itens.length()").value(2));
    }

    @Test
    @DisplayName("Deve retornar erro 400 quando dados inválidos para criação")
    void deveRetornarErro400QuandoDadosInvalidosParaCriacao() throws Exception {
        // Given - DTO com dados inválidos
        List<ItemPedidoDTO> itensInvalidos = Arrays.asList(
                new ItemPedidoDTO("", 0, new BigDecimal("-100.00")) // dados inválidos
        );
        CriarPedidoDTO dtoInvalido = new CriarPedidoDTO("", itensInvalidos, null);

        // When & Then
        mockMvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoInvalido)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar erro 400 quando parceiro não tem crédito suficiente")
    void deveRetornarErro400QuandoParceiroNaoTemCreditoSuficiente() throws Exception {
        // Given
        when(pedidoService.criarPedido(any(CriarPedidoDTO.class)))
                .thenThrow(new BusinessRuleException("CREDITO_INSUFICIENTE", "Crédito insuficiente"));

        // When & Then
        mockMvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(criarPedidoDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("CREDITO_INSUFICIENTE"))
                .andExpect(jsonPath("$.mensagem").value("Crédito insuficiente"));
    }

    @Test
    @DisplayName("Deve buscar pedido por ID com sucesso")
    void deveBuscarPedidoPorIdComSucesso() throws Exception {
        // Given
        when(pedidoService.buscarPorId(publicId)).thenReturn(pedidoResponseDTO);

        // When & Then
        mockMvc.perform(get("/pedidos/{publicId}", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(publicId))
                .andExpect(jsonPath("$.parceiroPublicId").value(parceiroPublicId))
                .andExpect(jsonPath("$.nomeParceiro").value("Empresa Teste"))
                .andExpect(jsonPath("$.valorTotal").value(1300.00));
    }

    @Test
    @DisplayName("Deve retornar erro 404 quando pedido não encontrado")
    void deveRetornarErro404QuandoPedidoNaoEncontrado() throws Exception {
        // Given
        String publicIdInexistente = "PED_INEXISTENTE";
        when(pedidoService.buscarPorId(publicIdInexistente))
                .thenThrow(EntityNotFoundException.pedido(publicIdInexistente));

        // When & Then
        mockMvc.perform(get("/pedidos/{publicId}", publicIdInexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("ENTITY_NOT_FOUND"))
                .andExpect(jsonPath("$.mensagem").exists());
    }

    @Test
    @DisplayName("Deve listar pedidos com sucesso")
    void deveListarPedidosComSucesso() throws Exception {
        // Given
        List<PedidoResponseDTO> pedidos = Arrays.asList(pedidoResponseDTO);
        when(pedidoService.buscarPedidos(any(), any(), any())).thenReturn(pedidos);

        // When & Then
        mockMvc.perform(get("/pedidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].publicId").value(publicId));
    }

    @Test
    @DisplayName("Deve buscar pedidos por status")
    void deveBuscarPedidosPorStatus() throws Exception {
        // Given
        List<PedidoResponseDTO> pedidos = Arrays.asList(pedidoResponseDTO);
        when(pedidoService.buscarPedidos(any(), any(), eq(StatusPedido.APROVADO))).thenReturn(pedidos);

        // When & Then
        mockMvc.perform(get("/pedidos")
                        .param("status", "APROVADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Deve atualizar status do pedido com sucesso")
    void deveAtualizarStatusDoPedidoComSucesso() throws Exception {
        // Given
        PedidoResponseDTO pedidoAtualizado = new PedidoResponseDTO(
                publicId, parceiroPublicId, "Empresa Teste", pedidoResponseDTO.itens(),
                pedidoResponseDTO.valorTotal(), StatusPedido.EM_PROCESSAMENTO,
                pedidoResponseDTO.observacoes(), pedidoResponseDTO.dataCriacao(), LocalDateTime.now()
        );

        when(pedidoService.atualizarStatus(eq(publicId), any(AtualizarStatusDTO.class)))
                .thenReturn(pedidoAtualizado);

        // When & Then
        mockMvc.perform(put("/pedidos/{publicId}/status", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(atualizarStatusDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(publicId))
                .andExpect(jsonPath("$.status").value("EM_PROCESSAMENTO"));
    }

    @Test
    @DisplayName("Deve cancelar pedido com sucesso")
    void deveCancelarPedidoComSucesso() throws Exception {
        // Given
        PedidoResponseDTO pedidoCancelado = new PedidoResponseDTO(
                publicId, parceiroPublicId, "Empresa Teste", pedidoResponseDTO.itens(),
                pedidoResponseDTO.valorTotal(), StatusPedido.CANCELADO,
                pedidoResponseDTO.observacoes(), pedidoResponseDTO.dataCriacao(), LocalDateTime.now()
        );

        when(pedidoService.cancelarPedido(publicId)).thenReturn(pedidoCancelado);

        // When & Then
        mockMvc.perform(put("/pedidos/{publicId}/cancelar", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(publicId))
                .andExpect(jsonPath("$.status").value("CANCELADO"));
    }

    @Test
    @DisplayName("Deve retornar erro 422 para transição de status inválida")
    void deveRetornarErro422ParaTransicaoDeStatusInvalida() throws Exception {
        // Given
        when(pedidoService.atualizarStatus(eq(publicId), any(AtualizarStatusDTO.class)))
                .thenThrow(new BusinessRuleException("TRANSICAO_STATUS_INVALIDA", "Transição de status inválida"));

        // When & Then
        mockMvc.perform(put("/pedidos/{publicId}/status", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(atualizarStatusDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("TRANSICAO_STATUS_INVALIDA"))
                .andExpect(jsonPath("$.mensagem").value("Transição de status inválida"));
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há pedidos")
    void deveRetornarListaVaziaQuandoNaoHaPedidos() throws Exception {
        // Given
        when(pedidoService.buscarPedidos(any(), any(), any())).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/pedidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Deve retornar erro 400 quando JSON malformado")
    void deveRetornarErro400QuandoJsonMalformado() throws Exception {
        // Given - JSON malformado
        String jsonMalformado = "{ \"parceiroPublicId\": \"PARC_123\", \"itens\": }";

        // When & Then
        mockMvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMalformado))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar erro 415 quando Content-Type incorreto")
    void deveRetornarErro415QuandoContentTypeIncorreto() throws Exception {
        // When & Then
        mockMvc.perform(post("/pedidos")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("dados inválidos"))
                .andExpect(status().isUnsupportedMediaType());
    }
}