package br.com.vpsconsulting.orderhub.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/parceiros")
public class ParceirosController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Parceiros Service");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/credito")
    public ResponseEntity<Map<String, Object>> consultarCredito(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        response.put("parceiroId", id);
        response.put("creditoDisponivel", 10000.00);
        response.put("message", "Endpoint para consultar crédito - Em desenvolvimento");
        return ResponseEntity.ok(response);
    }

}
