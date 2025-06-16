package br.com.vpsconsulting.orderhub.entity;

import br.com.vpsconsulting.orderhub.exception.BusinessRuleException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "itens_pedido", indexes = {
        @Index(name = "idx_item_pedido_id", columnList = "pedido_id"),
        @Index(name = "idx_item_produto", columnList = "produto")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"pedido"}) // Evita problemas de lazy loading
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(nullable = false, length = 100)
    private String produto;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal precoUnitario;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(length = 200)
    private String descricao;

    @Column(length = 50)
    private String unidadeMedida;

    // Constructor customizado
    public ItemPedido(Pedido pedido, String produto, Integer quantidade, BigDecimal precoUnitario) {
        this.pedido = pedido;
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        calcularSubtotal();
    }

    // Constructor com descrição
    public ItemPedido(Pedido pedido, String produto, String descricao, Integer quantidade,
                      BigDecimal precoUnitario, String unidadeMedida) {
        this.pedido = pedido;
        this.produto = produto;
        this.descricao = descricao;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.unidadeMedida = unidadeMedida;
        calcularSubtotal();
    }

    // Business methods
    public void calcularSubtotal() {
        if (quantidade != null && precoUnitario != null) {
            this.subtotal = precoUnitario
                    .multiply(BigDecimal.valueOf(quantidade))
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }

    public void atualizarQuantidade(Integer novaQuantidade) {
        if (novaQuantidade == null || novaQuantidade <= 0) {
            throw BusinessRuleException.quantidadeInvalida(novaQuantidade);
        }
        this.quantidade = novaQuantidade;
        calcularSubtotal();

        // Atualiza o valor total do pedido se estiver associado
        if (this.pedido != null) {
            this.pedido.calcularValorTotal();
        }
    }

    public void atualizarPrecoUnitario(BigDecimal novoPreco) {
        if (novoPreco == null || novoPreco.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessRuleException.valorInvalido("Preço unitário", novoPreco);
        }
        this.precoUnitario = novoPreco;
        calcularSubtotal();

        // Atualiza o valor total do pedido se estiver associado
        if (this.pedido != null) {
            this.pedido.calcularValorTotal();
        }
    }

    public void atualizarProduto(String novoProduto) {
        if (novoProduto == null || novoProduto.trim().isEmpty()) {
            throw new BusinessRuleException("PRODUTO_INVALIDO", "Nome do produto não pode ser vazio");
        }
        this.produto = novoProduto.trim();
    }

    public BigDecimal getValorTotalItem() {
        return subtotal != null ? subtotal : BigDecimal.ZERO;
    }

    public String getProdutoCompleto() {
        if (descricao != null && !descricao.trim().isEmpty()) {
            return produto + " - " + descricao;
        }
        return produto;
    }

    public String getQuantidadeFormatada() {
        if (unidadeMedida != null && !unidadeMedida.trim().isEmpty()) {
            return quantidade + " " + unidadeMedida;
        }
        return quantidade.toString();
    }

    public boolean isValido() {
        return produto != null && !produto.trim().isEmpty() &&
                quantidade != null && quantidade > 0 &&
                precoUnitario != null && precoUnitario.compareTo(BigDecimal.ZERO) > 0;
    }

    // Custom setters para recalcular subtotal automaticamente
    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
        calcularSubtotal();
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
        calcularSubtotal();
    }

    @PrePersist
    @PreUpdate
    protected void onSave() {
        // Validações antes de salvar
        if (!isValido()) {
            throw new IllegalStateException("Item inválido: " + this);
        }
        calcularSubtotal();
    }
}