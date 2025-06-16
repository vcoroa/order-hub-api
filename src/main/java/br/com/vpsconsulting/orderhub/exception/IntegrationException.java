package br.com.vpsconsulting.orderhub.exception;

public class IntegrationException extends OrderHubException {

    public IntegrationException(String servico, String mensagem) {
        super("INTEGRATION_ERROR",
                String.format("Erro na integração com %s: %s", servico, mensagem),
                servico);
    }

    public IntegrationException(String servico, String mensagem, Throwable causa) {
        super("INTEGRATION_ERROR",
                String.format("Erro na integração com %s: %s", servico, mensagem),
                causa, servico);
    }

    // Factory methods para integrações específicas
    public static IntegrationException notificacao(String mensagem, Throwable causa) {
        return new IntegrationException("Sistema de Notificação", mensagem, causa);
    }

    public static IntegrationException pagamento(String mensagem) {
        return new IntegrationException("Sistema de Pagamento", mensagem);
    }

    public static IntegrationException estoque(String mensagem) {
        return new IntegrationException("Sistema de Estoque", mensagem);
    }

    public static IntegrationException database(String operacao, Throwable causa) {
        return new IntegrationException("Banco de Dados",
                String.format("Erro na operação: %s", operacao), causa);
    }
}