package exception;

/**
 * Lançada quando a quantidade pedida de um produto é maior que o estoque disponível.
 * É checked: quem chama é obrigado a decidir o que fazer (normalmente avisar o
 * comprador e deixá-lo ajustar o carrinho).
 */
public class EstoqueInsuficienteException extends Exception {

    private final int produtoId;
    private final int solicitado;
    private final int disponivel;

    public EstoqueInsuficienteException(int produtoId, String nomeProduto, int solicitado, int disponivel) {
        super(String.format("Estoque insuficiente para '%s' (ID %d): solicitado %d, disponível %d.",
                nomeProduto, produtoId, solicitado, disponivel));
        this.produtoId = produtoId;
        this.solicitado = solicitado;
        this.disponivel = disponivel;
    }

    public int getProdutoId()  { return produtoId; }
    public int getSolicitado() { return solicitado; }
    public int getDisponivel() { return disponivel; }
}
