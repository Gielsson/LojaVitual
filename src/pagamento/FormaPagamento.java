package pagamento;

import exception.PagamentoRecusadoException;

/**
 * Contrato de toda forma de pagamento. Cada classe decide sozinha
 * como calcula o valor final e como valida/processa o pagamento,
 * então a lógica da compra não precisa saber qual é o tipo.
 */
public interface FormaPagamento {

    String getNome();

    /** Valor que o cliente realmente paga, já com o desconto (se tiver). */
    double calcularValorFinal(double subtotal);

    /** Simula o pagamento. Se os dados forem inválidos, lança PagamentoRecusadoException. */
    void processarPagamento(double valor) throws PagamentoRecusadoException;

    default double calcularDesconto(double subtotal) {
        return arredondar(subtotal - calcularValorFinal(subtotal));
    }

    static double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
