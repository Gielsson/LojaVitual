package model;

/**
 * Item de uma compra já concluída: guarda nome e preço no momento da compra
 * (snapshot). Se o preço do produto mudar depois, este registro NÃO muda.
 */
public final class ItemCompra {

    private final int produtoId;
    private final String nome;
    private final double precoUnitario;
    private final int quantidade;

    public ItemCompra(int produtoId, String nome, double precoUnitario, int quantidade) {
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("Nome do item obrigatório.");
        if (precoUnitario < 0) throw new IllegalArgumentException("Preço unitário não pode ser negativo.");
        if (quantidade <= 0) throw new IllegalArgumentException("Quantidade do item deve ser maior que zero.");
        this.produtoId = produtoId;
        this.nome = nome;
        this.precoUnitario = precoUnitario;
        this.quantidade = quantidade;
    }

    public int getProdutoId()        { return produtoId; }
    public String getNome()          { return nome; }
    public double getPrecoUnitario() { return precoUnitario; }
    public int getQuantidade()       { return quantidade; }

    public double getSubtotal() {
        return Math.round(precoUnitario * quantidade * 100.0) / 100.0;
    }
}
