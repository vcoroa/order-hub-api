package br.com.vpsconsulting.orderhub.entity;

import br.com.vpsconsulting.orderhub.exception.BusinessRuleException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "parceiros", indexes = {
        @Index(name = "idx_parceiro_public_id", columnList = "publicId", unique = true),
        @Index(name = "idx_parceiro_cnpj", columnList = "cnpj", unique = true),
        @Index(name = "idx_parceiro_ativo", columnList = "ativo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"pedidos"}) // Evita problemas de lazy loading
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Parceiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(unique = true, nullable = false, length = 20, updatable = false)
    private String publicId;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, unique = true, length = 20)
    private String cnpj;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal limiteCredito;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal creditoUtilizado = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime dataAtualizacao = LocalDateTime.now();

    @OneToMany(mappedBy = "parceiro", fetch = FetchType.LAZY)
    private List<Pedido> pedidos;

    // Constructor customizado para campos obrigatórios
    public Parceiro(String nome, String cnpj, BigDecimal limiteCredito) {
        this.nome = nome;
        this.cnpj = cnpj;
        this.limiteCredito = limiteCredito;
        this.creditoUtilizado = BigDecimal.ZERO;
        this.ativo = true;
        this.dataCriacao = LocalDateTime.now();
        this.dataAtualizacao = LocalDateTime.now();
    }

    // Business methods
    public BigDecimal getCreditoDisponivel() {
        return limiteCredito.subtract(creditoUtilizado);
    }

    public boolean temCreditoDisponivel(BigDecimal valor) {
        return getCreditoDisponivel().compareTo(valor) >= 0;
    }

    public void utilizarCredito(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("VALOR_INVALIDO", "Valor para utilização de crédito deve ser maior que zero");
        }
        if (!temCreditoDisponivel(valor)) {
            throw BusinessRuleException.creditoInsuficiente(getCreditoDisponivel(), valor);
        }
        this.creditoUtilizado = this.creditoUtilizado.add(valor);
        this.dataAtualizacao = LocalDateTime.now();
    }

    public void liberarCredito(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("VALOR_INVALIDO", "Valor para liberação de crédito deve ser maior que zero");
        }
        this.creditoUtilizado = this.creditoUtilizado.subtract(valor);
        if (this.creditoUtilizado.compareTo(BigDecimal.ZERO) < 0) {
            this.creditoUtilizado = BigDecimal.ZERO;
        }
        this.dataAtualizacao = LocalDateTime.now();
    }

    public void ativar() {
        this.ativo = true;
        this.dataAtualizacao = LocalDateTime.now();
    }

    public void desativar() {
        this.ativo = false;
        this.dataAtualizacao = LocalDateTime.now();
    }

    public void atualizarLimiteCredito(BigDecimal novoLimite) {
        if (novoLimite == null || novoLimite.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessRuleException.valorInvalido("Limite de crédito", novoLimite);
        }
        this.limiteCredito = novoLimite;
        this.dataAtualizacao = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (publicId == null) {
            publicId = "PARC_" + generateRandomString(8);
        }
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
        dataAtualizacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }

    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}