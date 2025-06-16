package br.com.vpsconsulting.orderhub.service;

import br.com.vpsconsulting.orderhub.dto.parceiros.CriarParceiroDTO;
import br.com.vpsconsulting.orderhub.dto.parceiros.ParceiroResponseDTO;
import br.com.vpsconsulting.orderhub.entity.Parceiro;
import br.com.vpsconsulting.orderhub.exception.BusinessRuleException;
import br.com.vpsconsulting.orderhub.exception.EntityNotFoundException;
import br.com.vpsconsulting.orderhub.repository.ParceiroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ParceiroService {

    private final ParceiroRepository parceiroRepository;

    // Buscar parceiro por publicId (usado pelos outros services)
    @Cacheable("dados-parceiros")
    @Transactional(readOnly = true)
    public Parceiro buscarPorPublicId(String publicId) {
        return parceiroRepository.findByPublicId(publicId)
                .orElseThrow(() -> EntityNotFoundException.parceiro(publicId));
    }

    // Verificar se parceiro tem crédito suficiente (usado na criação de pedidos)
    @Transactional(readOnly = true)
    public boolean temCreditoSuficiente(String parceiroPublicId, BigDecimal valor) {
        Parceiro parceiro = parceiroRepository.findByPublicId(parceiroPublicId)
            .orElseThrow(() -> EntityNotFoundException.parceiro(parceiroPublicId));

        if (!parceiro.getAtivo()) {
            throw BusinessRuleException.parceiroInativo(parceiroPublicId);
        }

        return parceiro.temCreditoDisponivel(valor);
    }

    // Debitar crédito do parceiro (usado quando pedido é aprovado)
    @CacheEvict(value = "dados-parceiros", key = "#parceiroPublicId")
    public void debitarCredito(String parceiroPublicId, BigDecimal valor) {
        log.info("Debitando crédito - Parceiro: {} - Valor: {}", parceiroPublicId, valor);

        Parceiro parceiro = parceiroRepository.findByPublicId(parceiroPublicId)
                .orElseThrow(() -> EntityNotFoundException.parceiro(parceiroPublicId));

        if (!parceiro.getAtivo()) {
            throw BusinessRuleException.parceiroInativo(parceiroPublicId);
        }

        parceiro.utilizarCredito(valor);
        parceiroRepository.save(parceiro);

        log.info("Crédito debitado com sucesso - Parceiro: {} - Crédito restante: {}",
                parceiroPublicId, parceiro.getCreditoDisponivel());
    }

    // Liberar crédito do parceiro (usado quando pedido é cancelado)
    @CacheEvict(value = "dados-parceiros", key = "#parceiroPublicId")
    public void liberarCredito(String parceiroPublicId, BigDecimal valor) {
        log.info("Liberando crédito - Parceiro: {} - Valor: {}", parceiroPublicId, valor);

        Parceiro parceiro = parceiroRepository.findByPublicId(parceiroPublicId)
                .orElseThrow(() -> EntityNotFoundException.parceiro(parceiroPublicId));

        parceiro.liberarCredito(valor);
        parceiroRepository.save(parceiro);

        log.info("Crédito liberado com sucesso - Parceiro: {} - Crédito disponível: {}",
                parceiroPublicId, parceiro.getCreditoDisponivel());
    }

    public ParceiroResponseDTO criarParceiro(CriarParceiroDTO dto) {
        log.info("Criando parceiro: {}", dto.nome());

        Parceiro parceiro = new Parceiro(dto.nome(), dto.cnpj(), dto.limiteCredito());

        parceiro = parceiroRepository.save(parceiro);

        log.info("Parceiro criado com sucesso. PublicId: {}", parceiro.getPublicId());

        return ParceiroResponseDTO.from(parceiro);
    }

    @Transactional(readOnly = true)
    public List<ParceiroResponseDTO> listarTodos() {
        return parceiroRepository.findAllByOrderByDataCriacaoDesc()
                .stream()
                .map(ParceiroResponseDTO::from)
                .collect(Collectors.toList());
    }
}