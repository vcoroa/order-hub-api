package br.com.vpsconsulting.orderhub.exception;

import java.util.List;
import java.util.Map;

public class ValidationException extends OrderHubException {

    private final List<String> erros;
    private final Map<String, String> camposComErro;

    public ValidationException(String mensagem, List<String> erros) {
        super("VALIDATION_ERROR", mensagem);
        this.erros = erros;
        this.camposComErro = null;
    }

    public ValidationException(String mensagem, Map<String, String> camposComErro) {
        super("VALIDATION_ERROR", mensagem);
        this.erros = null;
        this.camposComErro = camposComErro;
    }

    public ValidationException(List<String> erros) {
        super("VALIDATION_ERROR", "Dados de entrada inválidos");
        this.erros = erros;
        this.camposComErro = null;
    }

    public List<String> getErros() {
        return erros;
    }

    public Map<String, String> getCamposComErro() {
        return camposComErro;
    }

    // Factory methods
    public static ValidationException campoObrigatorio(String campo) {
        return new ValidationException(
                String.format("Campo obrigatório: %s", campo),
                List.of(String.format("%s é obrigatório", campo))
        );
    }

    public static ValidationException formatoInvalido(String campo, String valorEsperado) {
        return new ValidationException(
                String.format("Formato inválido para o campo %s", campo),
                List.of(String.format("%s deve estar no formato: %s", campo, valorEsperado))
        );
    }

    public static ValidationException cnpjInvalido(String cnpj) {
        return new ValidationException(
                "CNPJ inválido",
                List.of(String.format("CNPJ %s não é válido", cnpj))
        );
    }
}