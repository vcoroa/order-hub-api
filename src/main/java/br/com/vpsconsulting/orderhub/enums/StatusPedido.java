package br.com.vpsconsulting.orderhub.enums;

import lombok.Getter;

@Getter
public enum StatusPedido {
    PENDENTE("Pendente"),
    APROVADO("Aprovado"),
    EM_PROCESSAMENTO("Em Processamento"),
    ENVIADO("Enviado"),
    ENTREGUE("Entregue"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusPedido(String descricao) {
        this.descricao = descricao;
    }

    // Métodos utilitários
    public boolean isFinalizado() {
        return this == ENTREGUE || this == CANCELADO;
    }

    public boolean isAtivo() {
        return this != CANCELADO;
    }

    public boolean podeTransicionarPara(StatusPedido novoStatus) {
        if (novoStatus == null) {
            return false;
        }

        return switch (this) {
            case PENDENTE -> novoStatus == APROVADO || novoStatus == CANCELADO;
            case APROVADO -> novoStatus == EM_PROCESSAMENTO || novoStatus == CANCELADO;
            case EM_PROCESSAMENTO -> novoStatus == ENVIADO || novoStatus == CANCELADO;
            case ENVIADO -> novoStatus == ENTREGUE;
            case ENTREGUE, CANCELADO -> false; // Estados finais
        };
    }
}