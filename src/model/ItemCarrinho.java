package model;

/** Um produto dentro do carrinho, com a quantidade que o comprador quer. */
public class ItemCarrinho {

    private final Produto produto;
    private int quantidade;

    public ItemCarrinho(Produto produto, int quantidade) {
        this.produto = produto;
        this.quantidade = quantidade;
    }

    public Produto getProduto() { return produto; }
    public int getQuantidade() { return quantidade; }

    // só o Carrinho mexe na quantidade (mesmo pacote)
    void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public double getPrecoUnitario() {
        return produto.getPreco();
    }

    public double getSubtotal() {
        return Math.round(getPrecoUnitario() * quantidade * 100.0) / 100.0;
    }
}
