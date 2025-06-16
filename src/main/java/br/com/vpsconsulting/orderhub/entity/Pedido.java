package br.com.vpsconsulting.orderhub.entity;

import br.com.vpsconsulting.orderhub.enums.StatusPedido;
import br.com.vpsconsulting.orderhub.exception.BusinessRuleException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos", indexes = {
        @Index(name = "idx_pedido_public_id", columnList = "publicId", unique = true),
        @Index(name = "idx_pedido_parceiro_id", columnList = "parceiro_id"),
        @Index(name = "idx_pedido_status", columnList = "status"),
        @Index(name = "idx_pedido_data_criacao", columnList = "dataCriacao")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"parceiro", "itens"}) // Evita problemas de lazy loading
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(unique = true, nullable = false, length = 20, updatable = false)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parceiro_id", nullable = false)
    private Parceiro parceiro;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<ItemPedido> itens = new ArrayList<>();

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusPedido status = StatusPedido.PENDENTE;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime dataAtualizacao = LocalDateTime.now();

    @Column(length = 500)
    private String observacoes;

    // Constructor customizado
    public Pedido(Parceiro parceiro) {
        this.parceiro = parceiro;
        this.itens = new ArrayList<>();
        this.valorTotal = BigDecimal.ZERO;
        this.status = StatusPedido.PENDENTE;
        this.dataCriacao = LocalDateTime.now();
        this.dataAtualizacao = LocalDateTime.now();
    }

    // Business methods
    public void calcularValorTotal() {
        if (itens != null && !itens.isEmpty()) {
            this.valorTotal = itens.stream()
                    .map(ItemPedido::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            this.valorTotal = BigDecimal.ZERO;
        }
        this.dataAtualizacao = LocalDateTime.now();
    }

    public void adicionarItem(ItemPedido item) {
        if (this.itens == null) {
            this.itens = new ArrayList<>();
        }
        item.setPedido(this);
        this.itens.add(item);
        calcularValorTotal();
    }

    public void removerItem(ItemPedido item) {
        if (this.itens != null) {
            this.itens.remove(item);
            calcularValorTotal();
        }
    }

    public void limparItens() {
        if (this.itens != null) {
            this.itens.clear();
            calcularValorTotal();
        }
    }

    public boolean podeSerCancelado() {
        return status == StatusPedido.PENDENTE || status == StatusPedido.APROVADO;
    }

    public boolean podeSerAprovado() {
        return status == StatusPedido.PENDENTE;
    }

    public boolean podeSerProcessado() {
        return status == StatusPedido.APROVADO;
    }

    public boolean podeSerEnviado() {
        return status == StatusPedido.EM_PROCESSAMENTO;
    }

    public boolean podeSerEntregue() {
        return status == StatusPedido.ENVIADO;
    }

    public void aprovar() {
        if (!podeSerAprovado()) {
            throw BusinessRuleException.statusInvalido(status.name(), StatusPedido.APROVADO.name(), "aprovar");
        }
        if (!temItens()) {
            throw BusinessRuleException.pedidoSemItens();
        }
        this.status = StatusPedido.APROVADO;
        this.dataAtualizacao = LocalDateTime.now();
    }

    public void cancelar() {
        if (!podeSerCancelado()) {
            throw BusinessRuleException.statusInvalido(status.name(), StatusPedido.CANCELADO.name(), "cancelar");
        }
        this.status = StatusPedido.CANCELADO;
        this.dataAtualizacao = LocalDateTime.now();
    }

    public void iniciarProcessamento() {
        if (!podeSerProcessado()) {
            throw BusinessRuleException.statusInvalido(status.name(), StatusPedido.EM_PROCESSAMENTO.name(), "processar");
        }
        this.status = StatusPedido.EM_PROCESSAMENTO;
        this.dataAtualizacao = LocalDateTime.now();
    }

    public void enviar() {
        if (!podeSerEnviado()) {
            throw BusinessRuleException.statusInvalido(status.name(), StatusPedido.ENVIADO.name(), "enviar");
        }
        this.status = StatusPedido.ENVIADO;
        this.dataAtualizacao = LocalDateTime.now();
    }

    public void entregar() {
        if (!podeSerEntregue()) {
            throw BusinessRuleException.statusInvalido(status.name(), StatusPedido.ENTREGUE.name(), "entregar");
        }
        this.status = StatusPedido.ENTREGUE;
        this.dataAtualizacao = LocalDateTime.now();
    }

    public void atualizarStatus(StatusPedido novoStatus) {
        if (!status.podeTransicionarPara(novoStatus)) {
            throw BusinessRuleException.statusInvalido(status.name(), novoStatus.name(), "alterar para");
        }

        switch (novoStatus) {
            case APROVADO -> aprovar();
            case CANCELADO -> cancelar();
            case EM_PROCESSAMENTO -> iniciarProcessamento();
            case ENVIADO -> enviar();
            case ENTREGUE -> entregar();
            default -> throw new IllegalArgumentException("Transição de status não suportada: " + novoStatus);
        }
    }

    public boolean temItens() {
        return itens != null && !itens.isEmpty();
    }

    public int getQuantidadeItens() {
        return itens != null ? itens.size() : 0;
    }

    public boolean isAtivo() {
        return status != StatusPedido.CANCELADO;
    }

    public boolean isFinalizado() {
        return status == StatusPedido.ENTREGUE || status == StatusPedido.CANCELADO;
    }

    @PrePersist
    protected void onCreate() {
        if (publicId == null) {
            publicId = "PED_" + generateRandomString(8);
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