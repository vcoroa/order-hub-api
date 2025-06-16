package br.com.vpsconsulting.orderhub.exception;

public class EntityNotFoundException extends OrderHubException {

    public EntityNotFoundException(String entidade, String identificador) {
        super("ENTITY_NOT_FOUND",
                String.format("%s não encontrado(a) com identificador: %s", entidade, identificador),
                entidade, identificador);
    }

    public EntityNotFoundException(String entidade, String campo, Object valor) {
        super("ENTITY_NOT_FOUND",
                String.format("%s não encontrado(a) com %s: %s", entidade, campo, valor),
                entidade, campo, valor);
    }

    // Factory methods para entidades específicas
    public static EntityNotFoundException parceiro(String publicId) {
        return new EntityNotFoundException("Parceiro", "publicId", publicId);
    }

    public static EntityNotFoundException pedido(String publicId) {
        return new EntityNotFoundException("Pedido", "publicId", publicId);
    }

    public static EntityNotFoundException parceiroComCnpj(String cnpj) {
        return new EntityNotFoundException("Parceiro", "CNPJ", cnpj);
    }
}