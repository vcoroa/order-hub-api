package br.com.vpsconsulting.orderhub.service;

import br.com.vpsconsulting.orderhub.dto.parceiros.CriarParceiroDTO;
import br.com.vpsconsulting.orderhub.dto.parceiros.ParceiroResponseDTO;
import br.com.vpsconsulting.orderhub.entity.Parceiro;
import br.com.vpsconsulting.orderhub.exception.BusinessRuleException;
import br.com.vpsconsulting.orderhub.exception.EntityNotFoundException;
import br.com.vpsconsulting.orderhub.repository.ParceiroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParceiroService - Testes Unitários")
class ParceiroServiceTest {

    @Mock
    private ParceiroRepository parceiroRepository;

    @InjectMocks
    private ParceiroService parceiroService;

    private Parceiro parceiro;
    private String publicId;

    @BeforeEach
    void setUp() {
        publicId = "PARC_ABC123";
        parceiro = new Parceiro("Empresa Teste", "12345678000195", new BigDecimal("10000.00"));
        parceiro.setPublicId(publicId);
    }

    @Test
    @DisplayName("Deve buscar parceiro por publicId com sucesso")
    void deveBuscarParceiroPorPublicIdComSucesso() {
        // Given
        when(parceiroRepository.findByPublicId(publicId)).thenReturn(Optional.of(parceiro));

        // When
        Parceiro resultado = parceiroService.buscarPorPublicId(publicId);

        // Then
        assertNotNull(resultado);
        assertEquals(publicId, resultado.getPublicId());
        assertEquals("Empresa Teste", resultado.getNome());
        verify(parceiroRepository).findByPublicId(publicId);
    }

    @Test
    @DisplayName("Deve lançar exceção quando parceiro não encontrado")
    void deveLancarExcecaoQuandoParceiroNaoEncontrado() {
        // Given
        when(parceiroRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        // When & Then
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> parceiroService.buscarPorPublicId(publicId)
        );

        assertTrue(exception.getMessage().contains(publicId));
        verify(parceiroRepository).findByPublicId(publicId);
    }

    @Test
    @DisplayName("Deve verificar se parceiro tem crédito suficiente")
    void deveVerificarSeParceiroTemCreditoSuficiente() {
        // Given
        BigDecimal valor = new BigDecimal("5000.00");
        // O parceiro já foi criado com limite de 10000.00 no setUp, então tem crédito suficiente
        when(parceiroRepository.findByPublicId(publicId)).thenReturn(Optional.of(parceiro));

        // When
        boolean temCredito = parceiroService.temCreditoSuficiente(publicId, valor);

        // Then
        assertTrue(temCredito);
        verify(parceiroRepository).findByPublicId(publicId);
    }

    @Test
    @DisplayName("Deve retornar false quando parceiro não tem crédito suficiente")
    void deveRetornarFalseQuandoParceiroNaoTemCreditoSuficiente() {
        // Given
        BigDecimal valor = new BigDecimal("15000.00");
        // O parceiro tem limite de 10000.00, então não tem crédito suficiente para 15000.00
        when(parceiroRepository.findByPublicId(publicId)).thenReturn(Optional.of(parceiro));

        // When
        boolean temCredito = parceiroService.temCreditoSuficiente(publicId, valor);

        // Then
        assertFalse(temCredito);
        verify(parceiroRepository).findByPublicId(publicId);
    }

    @Test
    @DisplayName("Deve lançar exceção quando parceiro inativo ao verificar crédito")
    void deveLancarExcecaoQuandoParceiroInativoAoVerificarCredito() {
        // Given
        BigDecimal valor = new BigDecimal("5000.00");
        parceiro.setAtivo(false);
        when(parceiroRepository.findByPublicId(publicId)).thenReturn(Optional.of(parceiro));

        // When & Then
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> parceiroService.temCreditoSuficiente(publicId, valor)
        );

        assertTrue(exception.getMessage().contains("inativo"));
        verify(parceiroRepository).findByPublicId(publicId);
    }

    @Test
    @DisplayName("Deve debitar crédito do parceiro com sucesso")
    void deveDebitarCreditoDoParceiroComSucesso() {
        // Given
        BigDecimal valor = new BigDecimal("3000.00");
        BigDecimal creditoAnterior = parceiro.getCreditoDisponivel();

        when(parceiroRepository.findByPublicId(publicId)).thenReturn(Optional.of(parceiro));
        when(parceiroRepository.save(any(Parceiro.class))).thenReturn(parceiro);

        // When
        parceiroService.debitarCredito(publicId, valor);

        // Then
        verify(parceiroRepository).findByPublicId(publicId);
        verify(parceiroRepository).save(parceiro);
        // Verifica se o método utilizarCredito foi chamado através da mudança no saldo
        assertEquals(creditoAnterior.subtract(valor), parceiro.getCreditoDisponivel());
    }

    @Test
    @DisplayName("Deve lançar exceção ao debitar crédito de parceiro inativo")
    void deveLancarExcecaoAoDebitarCreditoDeParceiroInativo() {
        // Given
        BigDecimal valor = new BigDecimal("3000.00");
        parceiro.setAtivo(false);
        when(parceiroRepository.findByPublicId(publicId)).thenReturn(Optional.of(parceiro));

        // When & Then
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> parceiroService.debitarCredito(publicId, valor)
        );

        assertTrue(exception.getMessage().contains("inativo"));
        verify(parceiroRepository).findByPublicId(publicId);
        verify(parceiroRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve liberar crédito do parceiro com sucesso")
    void deveLiberarCreditoDoParceiroComSucesso() {
        // Given
        BigDecimal valor = new BigDecimal("2000.00");
        // Primeiro vamos usar parte do crédito para simular um cenário realista
        parceiro.utilizarCredito(new BigDecimal("5000.00"));
        BigDecimal creditoAntes = parceiro.getCreditoDisponivel();

        when(parceiroRepository.findByPublicId(publicId)).thenReturn(Optional.of(parceiro));
        when(parceiroRepository.save(any(Parceiro.class))).thenReturn(parceiro);

        // When
        parceiroService.liberarCredito(publicId, valor);

        // Then
        verify(parceiroRepository).findByPublicId(publicId);
        verify(parceiroRepository).save(parceiro);
        assertEquals(creditoAntes.add(valor), parceiro.getCreditoDisponivel());
    }

    @Test
    @DisplayName("Deve criar parceiro com sucesso")
    void deveCriarParceiroComSucesso() {
        // Given
        CriarParceiroDTO dto = new CriarParceiroDTO(
                "Nova Empresa",
                "98765432000111",
                new BigDecimal("15000.00")
        );

        Parceiro novoParceiroSalvo = new Parceiro(dto.nome(), dto.cnpj(), dto.limiteCredito());
        novoParceiroSalvo.setPublicId("PARC_NEW123");

        when(parceiroRepository.save(any(Parceiro.class))).thenReturn(novoParceiroSalvo);

        // When
        ParceiroResponseDTO resultado = parceiroService.criarParceiro(dto);

        // Then
        assertNotNull(resultado);
        assertEquals("PARC_NEW123", resultado.publicId());
        assertEquals("Nova Empresa", resultado.nome());
        assertEquals("98765432000111", resultado.cnpj());
        assertEquals(new BigDecimal("15000.00"), resultado.limiteCredito());
        verify(parceiroRepository).save(any(Parceiro.class));
    }

    @Test
    @DisplayName("Deve listar todos os parceiros")
    void deveListarTodosOsParceiros() {
        // Given
        Parceiro parceiro1 = new Parceiro("Empresa 1", "11111111000111", new BigDecimal("10000.00"));
        Parceiro parceiro2 = new Parceiro("Empresa 2", "22222222000222", new BigDecimal("20000.00"));

        List<Parceiro> parceiros = Arrays.asList(parceiro1, parceiro2);
        when(parceiroRepository.findAllByOrderByDataCriacaoDesc()).thenReturn(parceiros);

        // When
        List<ParceiroResponseDTO> resultado = parceiroService.listarTodos();

        // Then
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals("Empresa 1", resultado.get(0).nome());
        assertEquals("Empresa 2", resultado.get(1).nome());
        verify(parceiroRepository).findAllByOrderByDataCriacaoDesc();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há parceiros")
    void deveRetornarListaVaziaQuandoNaoHaParceiros() {
        // Given
        when(parceiroRepository.findAllByOrderByDataCriacaoDesc()).thenReturn(Arrays.asList());

        // When
        List<ParceiroResponseDTO> resultado = parceiroService.listarTodos();

        // Then
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
        verify(parceiroRepository).findAllByOrderByDataCriacaoDesc();
    }
}