package br.com.vpsconsulting.orderhub.controller;

import br.com.vpsconsulting.orderhub.dto.pedidos.AtualizarStatusDTO;
import br.com.vpsconsulting.orderhub.dto.pedidos.CriarPedidoDTO;
import br.com.vpsconsulting.orderhub.dto.pedidos.PedidoResponseDTO;
import br.com.vpsconsulting.orderhub.enums.StatusPedido;
import br.com.vpsconsulting.orderhub.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "API para gerenciamento de pedidos B2B")
public class PedidosController {

    private final PedidoService pedidoService;

    @PostMapping
    @Operation(summary = "Cadastro de pedidos", description = "Cria um novo pedido para um parceiro e aprova automaticamente se há crédito suficiente")
    public ResponseEntity<PedidoResponseDTO> criarPedido(@Valid @RequestBody CriarPedidoDTO dto) {
        log.info("Criando pedido para parceiro: {}", dto.parceiroPublicId());

        PedidoResponseDTO pedido = pedidoService.criarPedido(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
    }

    @GetMapping("/{publicId}")
    @Operation(summary = "Consulta de pedidos por ID", description = "Busca um pedido específico pelo seu ID")
    public ResponseEntity<PedidoResponseDTO> buscarPorId(
            @Parameter(description = "ID do pedido") @PathVariable String publicId) {

        PedidoResponseDTO pedido = pedidoService.buscarPorId(publicId);

        return ResponseEntity.ok(pedido);
    }

    @GetMapping
    @Operation(
            summary = "Consulta de pedidos",
            description = "Busca pedidos por período ou status. Exemplos: " +
                    "?status=PENDENTE | " +
                    "?dataInicio=2025-01-01T00:00:00&dataFim=2025-01-31T23:59:59 | " +
                    "sem parâmetros retorna todos os pedidos"
    )
    public ResponseEntity<List<PedidoResponseDTO>> buscarPedidos(
            @Parameter(
                    description = "Data de início do período",
                    example = "2025-01-01T00:00:00"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,

            @Parameter(
                    description = "Data de fim do período",
                    example = "2025-01-31T23:59:59"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,

            @Parameter(
                    description = "Status do pedido",
                    example = "PENDENTE"
            )
            @RequestParam(required = false) StatusPedido status) {

        List<PedidoResponseDTO> pedidos = pedidoService.buscarPedidos(dataInicio, dataFim, status);

        return ResponseEntity.ok(pedidos);
    }

    @PutMapping("/{publicId}/status")
    @Operation(summary = "Atualização de status de pedidos", description = "Atualiza o status de um pedido (APROVADO, EM_PROCESSAMENTO, ENVIADO, ENTREGUE, CANCELADO)")
    public ResponseEntity<PedidoResponseDTO> atualizarStatus(
            @Parameter(description = "ID do pedido") @PathVariable String publicId,
            @Valid @RequestBody AtualizarStatusDTO dto) {

        log.info("Atualizando status do pedido {} para {}", publicId, dto.status());

        PedidoResponseDTO pedido = pedidoService.atualizarStatus(publicId, dto);

        return ResponseEntity.ok(pedido);
    }

    @PutMapping("/{publicId}/cancelar")
    @Operation(summary = "Cancelamento de pedidos", description = "Cancela um pedido e libera crédito se necessário")
    public ResponseEntity<PedidoResponseDTO> cancelarPedido(
            @Parameter(description = "ID do pedido") @PathVariable String publicId) {

        log.info("Cancelando pedido: {}", publicId);

        PedidoResponseDTO pedido = pedidoService.cancelarPedido(publicId);

        return ResponseEntity.ok(pedido);
    }
}