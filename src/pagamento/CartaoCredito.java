package pagamento;

/** Crédito: paga o valor integral. */
public class CartaoCredito extends Cartao {

    public CartaoCredito(String numero) {
        super(numero);
    }

    @Override
    public String getNome() {
        return "Cartão de crédito";
    }

    @Override
    public double calcularValorFinal(double subtotal) {
        return FormaPagamento.arredondar(subtotal);
    }
}
