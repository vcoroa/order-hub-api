// src/main/java/br/com/vpsconsulting/orderhub/controller/ParceirosController.java
package br.com.vpsconsulting.orderhub.controller;

import br.com.vpsconsulting.orderhub.dto.parceiros.CriarParceiroDTO;
import br.com.vpsconsulting.orderhub.dto.parceiros.ParceiroResponseDTO;
import br.com.vpsconsulting.orderhub.entity.Parceiro;
import br.com.vpsconsulting.orderhub.service.ParceiroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/parceiros")
@RequiredArgsConstructor
@Tag(name = "Parceiros", description = "API para cadastro de parceiros")
public class ParceiroController {

    private final ParceiroService parceiroService;

    @PostMapping
    @Operation(
            summary = "Criar parceiro",
            description = "Cria um novo parceiro no sistema. Exemplo: nome='Empresa XYZ', cnpj='12345678000195', limiteCredito=50000.00"
    )
    public ResponseEntity<ParceiroResponseDTO> criarParceiro(@Valid @RequestBody CriarParceiroDTO dto) {
        log.info("Criando parceiro: {}", dto.nome());

        ParceiroResponseDTO parceiro = parceiroService.criarParceiro(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(parceiro);
    }

    @GetMapping("/{publicId}")
    @Operation(summary = "Buscar parceiro por ID", description = "Busca um parceiro específico pelo seu ID")
    public ResponseEntity<ParceiroResponseDTO> buscarPorId(
            @Parameter(description = "ID do parceiro", example = "PARC_A1B2C3D4")
            @PathVariable String publicId) {

        Parceiro parceiro = parceiroService.buscarPorPublicId(publicId);
        ParceiroResponseDTO response = ParceiroResponseDTO.from(parceiro);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Listar todos os parceiros", description = "Lista todos os parceiros cadastrados")
    public ResponseEntity<List<ParceiroResponseDTO>> listarTodos() {

        List<ParceiroResponseDTO> parceiros = parceiroService.listarTodos();

        return ResponseEntity.ok(parceiros);
    }
}