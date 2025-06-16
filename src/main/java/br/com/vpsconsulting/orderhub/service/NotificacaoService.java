package br.com.vpsconsulting.orderhub.service;

import br.com.vpsconsulting.orderhub.entity.Pedido;
import br.com.vpsconsulting.orderhub.enums.StatusPedido;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class NotificacaoService {

    // Simula integração com sistema de mensageria
    public void notificarMudancaStatus(Pedido pedido, StatusPedido statusAnterior, StatusPedido novoStatus) {
        log.info("=== NOTIFICAÇÃO DE MUDANÇA DE STATUS ===");
        log.info("Pedido: {}", pedido.getPublicId());
        log.info("Parceiro: {} - {}", pedido.getParceiro().getPublicId(), pedido.getParceiro().getNome());
        log.info("Status Anterior: {}", statusAnterior.getDescricao());
        log.info("Novo Status: {}", novoStatus.getDescricao());
        log.info("Valor do Pedido: R$ {}", pedido.getValorTotal());
        log.info("Data/Hora: {}", LocalDateTime.now());
        log.info("==========================================");

        // Aqui seria a integração real com:
        // - RabbitMQ
        // - Apache Kafka
        // - AWS SQS
        // - Azure Service Bus
        // etc.

        try {
            // Simula envio para fila de mensageria
            enviarParaFilaMensageria(criarMensagemNotificacao(pedido, statusAnterior, novoStatus));

            // Simula envio de email
            enviarEmailNotificacao(pedido, novoStatus);

            // Simula webhook para sistemas externos
            enviarWebhook(pedido, statusAnterior, novoStatus);

        } catch (Exception e) {
            log.error("Erro ao enviar notificação para pedido {}: {}", pedido.getPublicId(), e.getMessage());
            // Em um sistema real, aqui seria implementado:
            // - Retry automático
            // - Dead letter queue
            // - Alertas para monitoramento
        }
    }

    private void enviarParaFilaMensageria(String mensagem) {
        // Simulação de envio para fila
        log.info("📨 Enviando para fila 'pedidos.status.changed': {}", mensagem);

        // Simula possível falha de rede (5% de chance)
        if (Math.random() < 0.05) {
            throw new RuntimeException("Falha simulada na comunicação com broker de mensageria");
        }

        log.info("✅ Mensagem enviada com sucesso para a fila");
    }

    private void enviarEmailNotificacao(Pedido pedido, StatusPedido novoStatus) {
        log.info("📧 Enviando email de notificação:");
        log.info("   Para: vendas@{}.com", pedido.getParceiro().getNome().toLowerCase().replace(" ", ""));
        log.info("   Assunto: Pedido {} - Status atualizado para {}",
                pedido.getPublicId(), novoStatus.getDescricao());
        log.info("✅ Email enviado com sucesso");
    }

    private void enviarWebhook(Pedido pedido, StatusPedido statusAnterior, StatusPedido novoStatus) {
        String webhookUrl = "https://webhook.site/parceiro-" + pedido.getParceiro().getPublicId();

        log.info("🔗 Enviando webhook para: {}", webhookUrl);
        log.info("   Payload: {{\"pedidoId\":\"{}\", \"statusAnterior\":\"{}\", \"novoStatus\":\"{}\"}}",
                pedido.getPublicId(), statusAnterior, novoStatus);
        log.info("✅ Webhook enviado com sucesso");
    }

    private String criarMensagemNotificacao(Pedido pedido, StatusPedido statusAnterior, StatusPedido novoStatus) {
        return String.format(
                "{\"evento\":\"pedido.status.alterado\",\"pedidoId\":\"%s\",\"parceiroId\":\"%s\"," +
                        "\"statusAnterior\":\"%s\",\"novoStatus\":\"%s\",\"valorTotal\":%s,\"timestamp\":\"%s\"}",
                pedido.getPublicId(),
                pedido.getParceiro().getPublicId(),
                statusAnterior,
                novoStatus,
                pedido.getValorTotal(),
                LocalDateTime.now()
        );
    }
}