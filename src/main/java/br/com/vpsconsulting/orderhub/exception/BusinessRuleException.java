package br.com.vpsconsulting.orderhub.exception;

import java.math.BigDecimal;

public class BusinessRuleException extends OrderHubException {

    public BusinessRuleException(String codigo, String mensagem) {
        super(codigo, mensagem);
    }

    public BusinessRuleException(String codigo, String mensagem, Object... parametros) {
        super(codigo, mensagem, parametros);
    }

    // Factory methods para regras específicas
    public static BusinessRuleException creditoInsuficiente(BigDecimal disponivel, BigDecimal solicitado) {
        return new BusinessRuleException(
                "CREDITO_INSUFICIENTE",
                String.format("Crédito insuficiente. Disponível: R$ %.2f, Solicitado: R$ %.2f",
                        disponivel, solicitado),
                disponivel, solicitado
        );
    }

    public static BusinessRuleException parceiroInativo(String publicId) {
        return new BusinessRuleException(
                "PARCEIRO_INATIVO",
                String.format("Parceiro %s está inativo e não pode realizar pedidos", publicId),
                publicId
        );
    }

    public static BusinessRuleException statusInvalido(String statusAtual, String novoStatus, String operacao) {
        return new BusinessRuleException(
                "TRANSICAO_STATUS_INVALIDA",
                String.format("Não é possível %s pedido no status %s para %s", operacao, statusAtual, novoStatus),
                statusAtual, novoStatus, operacao
        );
    }

    public static BusinessRuleException cnpjJaExiste(String cnpj) {
        return new BusinessRuleException(
                "CNPJ_JA_EXISTE",
                String.format("Já existe um parceiro cadastrado com o CNPJ: %s", cnpj),
                cnpj
        );
    }

    public static BusinessRuleException pedidoSemItens() {
        return new BusinessRuleException(
                "PEDIDO_SEM_ITENS",
                "Pedido deve ter pelo menos um item"
        );
    }

    public static BusinessRuleException valorInvalido(String campo, BigDecimal valor) {
        return new BusinessRuleException(
                "VALOR_INVALIDO",
                String.format("%s deve ser maior que zero. Valor informado: R$ %.2f", campo, valor),
                campo, valor
        );
    }

    public static BusinessRuleException quantidadeInvalida(Integer quantidade) {
        return new BusinessRuleException(
                "QUANTIDADE_INVALIDA",
                String.format("Quantidade deve ser maior que zero. Valor informado: %d", quantidade),
                quantidade
        );
    }
}