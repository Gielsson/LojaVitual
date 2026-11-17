package pagamento;

import exception.PagamentoRecusadoException;

/** Pix: 5% de desconto. */
public class Pix implements FormaPagamento {

    private static final double DESCONTO = 0.05;

    private final String chave;

    public Pix(String chave) {
        this.chave = (chave == null) ? "" : chave.trim();
    }

    @Override
    public String getNome() {
        return "Pix";
    }

    @Override
    public double calcularValorFinal(double subtotal) {
        return FormaPagamento.arredondar(subtotal * (1 - DESCONTO));
    }

    @Override
    public void processarPagamento(double valor) throws PagamentoRecusadoException {
        if (chave.isEmpty()) {
            throw new PagamentoRecusadoException("Pagamento recusado: informe uma chave Pix.");
        }
        if (valor <= 0) {
            throw new PagamentoRecusadoException("Pagamento recusado: valor inválido.");
        }
    }
}
