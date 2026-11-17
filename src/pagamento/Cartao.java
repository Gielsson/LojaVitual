package pagamento;

import exception.PagamentoRecusadoException;

/** Parte comum dos cartões: guardar o número e validar. */
public abstract class Cartao implements FormaPagamento {

    private final String numero;

    protected Cartao(String numero) {
        this.numero = (numero == null) ? "" : numero.replaceAll("[\\s-]", "");
    }

    @Override
    public void processarPagamento(double valor) throws PagamentoRecusadoException {
        if (!numero.matches("\\d{16}")) {
            throw new PagamentoRecusadoException("Pagamento recusado: o número do cartão deve ter 16 dígitos.");
        }
        if (valor <= 0) {
            throw new PagamentoRecusadoException("Pagamento recusado: valor inválido.");
        }
        // pagamento simulado: dados válidos = aprovado
    }
}
