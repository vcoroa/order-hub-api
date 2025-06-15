package br.com.vpsconsulting.orderhub.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/pedidos")
public class PedidosController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "Pedidos B2B API");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "API Pedidos B2B está funcionando!");
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> listarPedidos() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Endpoint para listar pedidos - Em desenvolvimento");
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> criarPedido(@RequestBody Map<String, Object> pedido) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Endpoint para criar pedido - Em desenvolvimento");
        response.put("received", pedido.toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, String>> buscarPedido(@PathVariable String id) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Endpoint para buscar pedido por ID - Em desenvolvimento");
        response.put("id", id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> atualizarStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> statusUpdate) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Endpoint para atualizar status - Em desenvolvimento");
        response.put("id", id);
        response.put("novoStatus", statusUpdate.get("status"));
        return ResponseEntity.ok(response);
    }

}
