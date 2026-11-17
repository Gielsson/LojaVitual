package exception;

/**
 * Lançada quando um produto (ou item) é inexistente ou possui dados inválidos:
 * ID não encontrado, quantidade <= 0, preço negativo, ID duplicado etc.
 */
public class ProdutoInvalidoException extends Exception {

    public ProdutoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
