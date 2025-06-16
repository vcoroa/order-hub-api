package br.com.vpsconsulting.orderhub.exception;

import lombok.Getter;

@Getter
public abstract class OrderHubException extends RuntimeException {

    private final String codigo;
    private final Object[] parametros;

    protected OrderHubException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
        this.parametros = null;
    }

    protected OrderHubException(String codigo, String mensagem, Object... parametros) {
        super(mensagem);
        this.codigo = codigo;
        this.parametros = parametros;
    }

    protected OrderHubException(String codigo, String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.codigo = codigo;
        this.parametros = null;
    }

    protected OrderHubException(String codigo, String mensagem, Throwable causa, Object... parametros) {
        super(mensagem, causa);
        this.codigo = codigo;
        this.parametros = parametros;
    }
}