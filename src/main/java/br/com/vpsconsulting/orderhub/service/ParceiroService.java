package br.com.vpsconsulting.orderhub.service;

import br.com.vpsconsulting.orderhub.dto.parceiros.CriarParceiroDTO;
import br.com.vpsconsulting.orderhub.dto.parceiros.ParceiroResponseDTO;
import br.com.vpsconsulting.orderhub.entity.Parceiro;
import br.com.vpsconsulting.orderhub.exception.EntityNotFoundException;
import br.com.vpsconsulting.orderhub.repository.ParceiroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ParceiroService {

    private final ParceiroRepository parceiroRepository;

    // Buscar parceiro por publicId (para consultas read-only - mantém cache)
    @Cacheable("dados-parceiros")
    @Transactional(readOnly = true)
    public Parceiro buscarPorPublicId(String publicId) {
        return parceiroRepository.findByPublicId(publicId)
                .orElseThrow(() -> EntityNotFoundException.parceiro(publicId));
    }

    // Criar novo parceiro
    public ParceiroResponseDTO criarParceiro(CriarParceiroDTO dto) {
        log.info("Criando parceiro: {}", dto.nome());

        Parceiro parceiro = new Parceiro(dto.nome(), dto.cnpj(), dto.limiteCredito());

        parceiro = parceiroRepository.save(parceiro);

        log.info("Parceiro criado com sucesso. PublicId: {}", parceiro.getPublicId());

        return ParceiroResponseDTO.from(parceiro);
    }

    // Listar todos os parceiros
    @Transactional(readOnly = true)
    public List<ParceiroResponseDTO> listarTodos() {
        return parceiroRepository.findAllByOrderByDataCriacaoDesc()
                .stream()
                .map(ParceiroResponseDTO::from)
                .collect(Collectors.toList());
    }
}