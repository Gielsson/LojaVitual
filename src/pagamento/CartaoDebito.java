package pagamento;

/** Débito: 10% de desconto. */
public class CartaoDebito extends Cartao {

    private static final double DESCONTO = 0.10;

    public CartaoDebito(String numero) {
        super(numero);
    }

    @Override
    public String getNome() {
        return "Cartão de débito";
    }

    @Override
    public double calcularValorFinal(double subtotal) {
        return FormaPagamento.arredondar(subtotal * (1 - DESCONTO));
    }
}
