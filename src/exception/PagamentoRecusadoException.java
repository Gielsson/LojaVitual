package exception;

/** Lançada quando os dados do pagamento são inválidos e o pagamento (simulado) é recusado. */
public class PagamentoRecusadoException extends Exception {

    public PagamentoRecusadoException(String mensagem) {
        super(mensagem);
    }
}
