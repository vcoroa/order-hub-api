package br.com.vpsconsulting.orderhub.exception;

import br.com.vpsconsulting.orderhub.dto.utils.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler - Testes Unitários")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private HttpServletRequest request;

    @Mock
    private BindingResult bindingResult;

    private final String REQUEST_URI = "/test/endpoint";

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn(REQUEST_URI);
    }

    @Test
    @DisplayName("Deve tratar EntityNotFoundException corretamente")
    void deveTratarEntityNotFoundExceptionCorretamente() {
        // Given
        EntityNotFoundException exception = EntityNotFoundException.parceiro("PARC_123");

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleEntityNotFound(exception, request);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ENTITY_NOT_FOUND", response.getBody().erro());
        assertTrue(response.getBody().mensagem().contains("PARC_123"));
        assertEquals(REQUEST_URI, response.getBody().path());
    }

    @Test
    @DisplayName("Deve tratar EntityNotFoundException para pedido")
    void deveTratarEntityNotFoundExceptionParaPedido() {
        // Given
        EntityNotFoundException exception = EntityNotFoundException.pedido("PED_123");

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleEntityNotFound(exception, request);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ENTITY_NOT_FOUND", response.getBody().erro());
        assertTrue(response.getBody().mensagem().contains("Pedido"));
        assertTrue(response.getBody().mensagem().contains("PED_123"));
    }

    @Test
    @DisplayName("Deve tratar BusinessRuleException - Crédito insuficiente")
    void deveTratarBusinessRuleExceptionCreditoInsuficiente() {
        // Given
        BusinessRuleException exception = BusinessRuleException.creditoInsuficiente(
                new BigDecimal("1000.00"),
                new BigDecimal("1500.00")
        );

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleBusinessRule(exception, request);

        // Then
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("CREDITO_INSUFICIENTE", response.getBody().erro());

        // Testa os valores sem depender do formato de localização
        String mensagem = response.getBody().mensagem();
        assertTrue(mensagem.contains("1000") || mensagem.contains("1.000"),
                "Mensagem deveria conter o valor 1000. Mensagem atual: " + mensagem);
        assertTrue(mensagem.contains("1500") || mensagem.contains("1.500"),
                "Mensagem deveria conter o valor 1500. Mensagem atual: " + mensagem);
        assertTrue(mensagem.toLowerCase().contains("crédito"),
                "Mensagem deveria conter 'crédito'. Mensagem atual: " + mensagem);

        assertEquals(REQUEST_URI, response.getBody().path());
    }

    @Test
    @DisplayName("Deve tratar BusinessRuleException - Parceiro inativo")
    void deveTratarBusinessRuleExceptionParceiroInativo() {
        // Given
        BusinessRuleException exception = BusinessRuleException.parceiroInativo("PARC_123");

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleBusinessRule(exception, request);

        // Then
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("PARCEIRO_INATIVO", response.getBody().erro());
        assertTrue(response.getBody().mensagem().contains("PARC_123"));
        assertTrue(response.getBody().mensagem().contains("inativo"));
    }

    @Test
    @DisplayName("Deve tratar BusinessRuleException - Status inválido")
    void deveTratarBusinessRuleExceptionStatusInvalido() {
        // Given
        BusinessRuleException exception = BusinessRuleException.statusInvalido("APROVADO", "PENDENTE", "alterar para");

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleBusinessRule(exception, request);

        // Then
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("TRANSICAO_STATUS_INVALIDA", response.getBody().erro());
        assertTrue(response.getBody().mensagem().contains("APROVADO"));
        assertTrue(response.getBody().mensagem().contains("PENDENTE"));
    }

    @Test
    @DisplayName("Deve tratar BusinessRuleException - Pedido sem itens")
    void deveTratarBusinessRuleExceptionPedidoSemItens() {
        // Given
        BusinessRuleException exception = BusinessRuleException.pedidoSemItens();

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleBusinessRule(exception, request);

        // Then
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("PEDIDO_SEM_ITENS", response.getBody().erro());
        assertTrue(response.getBody().mensagem().contains("pelo menos um item"));
    }

    @Test
    @DisplayName("Deve tratar BusinessRuleException - CNPJ já existe")
    void deveTratarBusinessRuleExceptionCnpjJaExiste() {
        // Given
        BusinessRuleException exception = BusinessRuleException.cnpjJaExiste("12345678000195");

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleBusinessRule(exception, request);

        // Then
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("CNPJ_JA_EXISTE", response.getBody().erro());
        assertTrue(response.getBody().mensagem().contains("12345678000195"));
    }

    @Test
    @DisplayName("Deve tratar ValidationException corretamente")
    void deveTratarValidationExceptionCorretamente() {
        // Given
        List<String> erros = Arrays.asList("Nome é obrigatório", "CNPJ inválido");
        ValidationException exception = new ValidationException("Dados inválidos", erros);

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleValidation(exception, request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().erro());
        assertEquals("Dados inválidos", response.getBody().mensagem());
        assertEquals(erros, response.getBody().detalhes());
        assertEquals(REQUEST_URI, response.getBody().path());
    }

    @Test
    @DisplayName("Deve tratar ValidationException - Campo obrigatório")
    void deveTratarValidationExceptionCampoObrigatorio() {
        // Given
        ValidationException exception = ValidationException.campoObrigatorio("nome");

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleValidation(exception, request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().erro());
        assertTrue(response.getBody().mensagem().contains("nome"));
    }

    @Test
    @DisplayName("Deve tratar ValidationException - CNPJ inválido")
    void deveTratarValidationExceptionCnpjInvalido() {
        // Given
        ValidationException exception = ValidationException.cnpjInvalido("12345678000");

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleValidation(exception, request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().erro());
        assertTrue(response.getBody().mensagem().contains("CNPJ"));
    }

    @Test
    @DisplayName("Deve tratar MethodArgumentNotValidException corretamente")
    void deveTratarMethodArgumentNotValidExceptionCorretamente() throws NoSuchMethodException {
        // Given
        FieldError fieldError1 = new FieldError("parceiro", "nome", "Nome é obrigatório");
        FieldError fieldError2 = new FieldError("parceiro", "cnpj", "CNPJ deve ter 14 dígitos");

        when(bindingResult.getFieldErrors()).thenReturn(Arrays.asList(fieldError1, fieldError2));

        // Criando um MethodParameter real usando reflexão
        Method dummyMethod = this.getClass().getDeclaredMethod("dummyMethod", String.class);
        MethodParameter realMethodParameter = new MethodParameter(dummyMethod, 0);

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(realMethodParameter, bindingResult);

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleMethodArgumentNotValid(exception, request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().erro());
        assertEquals("Dados de entrada inválidos", response.getBody().mensagem());
        assertEquals(2, response.getBody().detalhes().size());
        assertTrue(response.getBody().detalhes().contains("Nome é obrigatório"));
        assertTrue(response.getBody().detalhes().contains("CNPJ deve ter 14 dígitos"));
        assertEquals(REQUEST_URI, response.getBody().path());
    }

    @Test
    @DisplayName("Deve tratar IntegrationException corretamente")
    void deveTratarIntegrationExceptionCorretamente() {
        // Given
        IntegrationException exception = IntegrationException.notificacao("Falha ao enviar notificação", new RuntimeException());

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleIntegration(exception, request);

        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTEGRATION_ERROR", response.getBody().erro());
        assertEquals("Erro interno do sistema. Tente novamente mais tarde.", response.getBody().mensagem());
        assertEquals(REQUEST_URI, response.getBody().path());
    }

    @Test
    @DisplayName("Deve tratar IntegrationException - Sistema de Pagamento")
    void deveTratarIntegrationExceptionSistemaPagamento() {
        // Given
        IntegrationException exception = IntegrationException.pagamento("Gateway indisponível");

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleIntegration(exception, request);

        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTEGRATION_ERROR", response.getBody().erro());
        assertEquals("Erro interno do sistema. Tente novamente mais tarde.", response.getBody().mensagem());
    }

    @Test
    @DisplayName("Deve tratar IntegrationException - Banco de Dados")
    void deveTratarIntegrationExceptionBancoDados() {
        // Given
        IntegrationException exception = IntegrationException.database("inserir parceiro", new RuntimeException("Connection timeout"));

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleIntegration(exception, request);

        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTEGRATION_ERROR", response.getBody().erro());
    }

    @Test
    @DisplayName("Deve tratar IllegalArgumentException corretamente")
    void deveTratarIllegalArgumentExceptionCorretamente() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Argumento inválido fornecido");

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleIllegalArgument(exception, request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_ARGUMENT", response.getBody().erro());
        assertEquals("Argumento inválido fornecido", response.getBody().mensagem());
        assertEquals(REQUEST_URI, response.getBody().path());
    }

    @Test
    @DisplayName("Deve tratar NoResourceFoundException corretamente")
    void deveTratarNoResourceFoundExceptionCorretamente() {
        // Given
        NoResourceFoundException exception = new NoResourceFoundException(
                org.springframework.http.HttpMethod.GET,
                REQUEST_URI
        );

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleNoResourceFound(exception, request);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ENDPOINT_NOT_FOUND", response.getBody().erro());
        assertTrue(response.getBody().mensagem().contains(REQUEST_URI));
        assertEquals(REQUEST_URI, response.getBody().path());
    }

    @Test
    @DisplayName("Deve tratar NoHandlerFoundException corretamente")
    void deveTratarNoHandlerFoundExceptionCorretamente() {
        // Given
        NoHandlerFoundException exception = new NoHandlerFoundException("GET", REQUEST_URI, null);

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleNoHandlerFound(exception, request);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ENDPOINT_NOT_FOUND", response.getBody().erro());
        assertTrue(response.getBody().mensagem().contains("GET"));
        assertTrue(response.getBody().mensagem().contains(REQUEST_URI));
        assertEquals(REQUEST_URI, response.getBody().path());
    }

    @Test
    @DisplayName("Deve tratar HttpRequestMethodNotSupportedException corretamente")
    void deveTratarHttpRequestMethodNotSupportedExceptionCorretamente() {
        // Given
        List<String> supportedMethods = Arrays.asList("GET", "POST");
        HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException("DELETE", supportedMethods);

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleMethodNotSupported(exception, request);

        // Then
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("METHOD_NOT_SUPPORTED", response.getBody().erro());
        assertTrue(response.getBody().mensagem().contains("DELETE"));
        assertTrue(response.getBody().mensagem().contains("GET, POST"));
        assertEquals(REQUEST_URI, response.getBody().path());
    }

    @Test
    @DisplayName("Deve tratar HttpMediaTypeNotSupportedException corretamente")
    void deveTratarHttpMediaTypeNotSupportedExceptionCorretamente() {
        // Given
        List<MediaType> supportedTypes = Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML);
        HttpMediaTypeNotSupportedException exception = new HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, supportedTypes);

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleHttpMediaTypeNotSupported(exception, request);

        // Then
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UNSUPPORTED_MEDIA_TYPE", response.getBody().erro());
        assertTrue(response.getBody().mensagem().contains("text/plain"));
        assertTrue(response.getBody().mensagem().contains("application/json"));
        assertEquals(REQUEST_URI, response.getBody().path());
    }

    @Test
    @DisplayName("Deve tratar HttpMessageNotReadableException corretamente")
    void deveTratarHttpMessageNotReadableExceptionCorretamente() {
        // Given
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException("JSON parse error");

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleHttpMessageNotReadable(exception, request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_JSON", response.getBody().erro());
        assertEquals("Formato de JSON inválido. Verifique a sintaxe do JSON enviado.", response.getBody().mensagem());
        assertEquals(REQUEST_URI, response.getBody().path());
    }

    @Test
    @DisplayName("Deve tratar Exception genérica corretamente")
    void deveTratarExceptionGenericaCorretamente() {
        // Given
        Exception exception = new RuntimeException("Erro inesperado");

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleGeneral(exception, request);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_ERROR", response.getBody().erro());
        assertEquals("Erro interno do sistema", response.getBody().mensagem());
        assertEquals(REQUEST_URI, response.getBody().path());
    }

    @Test
    @DisplayName("Deve validar estrutura do ErrorResponseDTO")
    void deveValidarEstruturaDoErrorResponseDTO() {
        // Given
        BusinessRuleException exception = BusinessRuleException.valorInvalido("limite", new BigDecimal("-100"));

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleBusinessRule(exception, request);

        // Then
        ErrorResponseDTO errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertNotNull(errorResponse.erro());
        assertNotNull(errorResponse.mensagem());
        assertNotNull(errorResponse.path());
        assertNotNull(errorResponse.timestamp());
        assertEquals("VALOR_INVALIDO", errorResponse.erro());
    }

    @Test
    @DisplayName("Deve preservar códigos específicos das exceções de negócio")
    void devePreservarCodigosEspecificosDasExcecoesDeNegocio() {
        // Given & When & Then - Testando diferentes factory methods

        // BusinessRuleException - Quantidade inválida
        BusinessRuleException quantidadeEx = BusinessRuleException.quantidadeInvalida(-5);
        ResponseEntity<ErrorResponseDTO> quantidadeResponse = globalExceptionHandler.handleBusinessRule(quantidadeEx, request);
        assertEquals("QUANTIDADE_INVALIDA", quantidadeResponse.getBody().erro());
        assertTrue(quantidadeResponse.getBody().mensagem().contains("-5"));

        // BusinessRuleException - Valor inválido
        BusinessRuleException valorEx = BusinessRuleException.valorInvalido("preço", new BigDecimal("-50.00"));
        ResponseEntity<ErrorResponseDTO> valorResponse = globalExceptionHandler.handleBusinessRule(valorEx, request);
        assertEquals("VALOR_INVALIDO", valorResponse.getBody().erro());
        assertTrue(valorResponse.getBody().mensagem().contains("preço"));

        // ValidationException - Formato inválido
        ValidationException formatoEx = ValidationException.formatoInvalido("email", "usuario@dominio.com");
        ResponseEntity<ErrorResponseDTO> formatoResponse = globalExceptionHandler.handleValidation(formatoEx, request);
        assertEquals("VALIDATION_ERROR", formatoResponse.getBody().erro());
        assertTrue(formatoResponse.getBody().mensagem().contains("email"));
    }

    @Test
    @DisplayName("Deve tratar IntegrationException - Sistema de Estoque")
    void deveTratarIntegrationExceptionSistemaEstoque() {
        // Given
        IntegrationException exception = IntegrationException.estoque("Produto não encontrado no estoque");

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleIntegration(exception, request);

        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("INTEGRATION_ERROR", response.getBody().erro());
        assertEquals("Erro interno do sistema. Tente novamente mais tarde.", response.getBody().mensagem());
    }

    // Método dummy para criar o MethodParameter
    private void dummyMethod(String param) {
        // Método vazio apenas para teste
    }
}