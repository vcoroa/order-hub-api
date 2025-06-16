package br.com.vpsconsulting.orderhub.controller;

import br.com.vpsconsulting.orderhub.dto.parceiros.CriarParceiroDTO;
import br.com.vpsconsulting.orderhub.dto.parceiros.ParceiroResponseDTO;
import br.com.vpsconsulting.orderhub.entity.Parceiro;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ParceiroController.class) // Especifica apenas o controller a ser testado
@DisplayName("ParceiroController - Testes Unitários")
class ParceiroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ParceiroService parceiroService;

    // Mock todos os services que são dependências dos controllers carregados
    @MockBean
    private PedidoService pedidoService;

    @Autowired
    private ObjectMapper objectMapper;

    private ParceiroResponseDTO parceiroResponseDTO;
    private CriarParceiroDTO criarParceiroDTO;
    private Parceiro parceiro;

    @BeforeEach
    void setUp() {
        criarParceiroDTO = new CriarParceiroDTO(
                "Empresa Teste",
                "12345678000195",
                new BigDecimal("10000.00")
        );

        parceiro = new Parceiro("Empresa Teste", "12345678000195", new BigDecimal("10000.00"));
        parceiro.setPublicId("PARC_ABC123");

        parceiroResponseDTO = new ParceiroResponseDTO(
                "PARC_ABC123",
                "Empresa Teste",
                "12345678000195",
                new BigDecimal("10000.00"),
                BigDecimal.ZERO,                    // creditoUtilizado
                new BigDecimal("10000.00"),         // creditoDisponivel
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("Deve criar parceiro com sucesso")
    void deveCriarParceiroComSucesso() throws Exception {
        // Given
        when(parceiroService.criarParceiro(any(CriarParceiroDTO.class))).thenReturn(parceiroResponseDTO);

        // When & Then
        mockMvc.perform(post("/parceiros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(criarParceiroDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicId").value("PARC_ABC123"))
                .andExpect(jsonPath("$.nome").value("Empresa Teste"))
                .andExpect(jsonPath("$.cnpj").value("12345678000195"))
                .andExpect(jsonPath("$.limiteCredito").value(10000.00))
                .andExpect(jsonPath("$.creditoUtilizado").value(0.00))
                .andExpect(jsonPath("$.creditoDisponivel").value(10000.00))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    @DisplayName("Deve retornar erro 400 quando dados inválidos para criação")
    void deveRetornarErro400QuandoDadosInvalidosParaCriacao() throws Exception {
        // Given - DTO com dados inválidos
        CriarParceiroDTO dtoInvalido = new CriarParceiroDTO(
                "", // nome vazio
                "cnpj-invalido",
                new BigDecimal("-1000.00") // valor negativo
        );

        // When & Then
        mockMvc.perform(post("/parceiros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoInvalido)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve buscar parceiro por ID com sucesso")
    void deveBuscarParceiroPorIdComSucesso() throws Exception {
        // Given
        String publicId = "PARC_ABC123";
        when(parceiroService.buscarPorPublicId(publicId)).thenReturn(parceiro);

        // When & Then
        mockMvc.perform(get("/parceiros/{publicId}", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value("PARC_ABC123"))
                .andExpect(jsonPath("$.nome").value("Empresa Teste"))
                .andExpect(jsonPath("$.cnpj").value("12345678000195"));
    }

    @Test
    @DisplayName("Deve retornar erro 404 quando parceiro não encontrado")
    void deveRetornarErro404QuandoParceiroNaoEncontrado() throws Exception {
        // Given
        String publicId = "PARC_INEXISTENTE";
        when(parceiroService.buscarPorPublicId(publicId))
                .thenThrow(EntityNotFoundException.parceiro(publicId));

        // When & Then
        mockMvc.perform(get("/parceiros/{publicId}", publicId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("ENTITY_NOT_FOUND"))
                .andExpect(jsonPath("$.mensagem").exists());
    }

    @Test
    @DisplayName("Deve listar todos os parceiros com sucesso")
    void deveListarTodosOsParceirosComSucesso() throws Exception {
        // Given
        ParceiroResponseDTO parceiro2 = new ParceiroResponseDTO(
                "PARC_XYZ789",
                "Empresa Dois",
                "98765432000111",
                new BigDecimal("15000.00"),
                new BigDecimal("5000.00"),           // creditoUtilizado
                new BigDecimal("10000.00"),          // creditoDisponivel (15000 - 5000)
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        List<ParceiroResponseDTO> parceiros = Arrays.asList(parceiroResponseDTO, parceiro2);
        when(parceiroService.listarTodos()).thenReturn(parceiros);

        // When & Then
        mockMvc.perform(get("/parceiros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].publicId").value("PARC_ABC123"))
                .andExpect(jsonPath("$[0].nome").value("Empresa Teste"))
                .andExpect(jsonPath("$[1].publicId").value("PARC_XYZ789"))
                .andExpect(jsonPath("$[1].nome").value("Empresa Dois"));
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há parceiros")
    void deveRetornarListaVaziaQuandoNaoHaParceiros() throws Exception {
        // Given
        when(parceiroService.listarTodos()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/parceiros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Deve retornar erro 400 quando JSON malformado")
    void deveRetornarErro400QuandoJsonMalformado() throws Exception {
        // Given - JSON malformado
        String jsonMalformado = "{ \"nome\": \"Teste\", \"cnpj\": }";

        // When & Then
        mockMvc.perform(post("/parceiros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMalformado))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar erro 415 quando Content-Type incorreto")
    void deveRetornarErro415QuandoContentTypeIncorreto() throws Exception {
        // When & Then
        mockMvc.perform(post("/parceiros")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("dados inválidos"))
                .andExpect(status().isUnsupportedMediaType());
    }
}