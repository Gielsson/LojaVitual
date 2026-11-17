package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Registro IMUTÁVEL de uma compra concluída (snapshot do histórico).
 * Não há setters; a lista de itens é copiada e exposta somente para leitura.
 */
public final class Compra {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final int id;
    private final LocalDateTime dataHora;
    private final String comprador;
    private final String formaPagamento;
    private final List<ItemCompra> itens;
    private final double subtotal;
    private final double desconto;
    private final double valorFinal;

    public Compra(int id, LocalDateTime dataHora, String comprador, String formaPagamento,
                  List<ItemCompra> itens, double subtotal, double desconto, double valorFinal) {
        if (dataHora == null) throw new IllegalArgumentException("Data/hora obrigatória.");
        if (comprador == null || comprador.isBlank()) throw new IllegalArgumentException("Comprador obrigatório.");
        if (formaPagamento == null || formaPagamento.isBlank()) throw new IllegalArgumentException("Forma de pagamento obrigatória.");
        if (itens == null || itens.isEmpty()) throw new IllegalArgumentException("Uma compra precisa ter ao menos um item.");
        this.id = id;
        this.dataHora = dataHora;
        this.comprador = comprador.trim();
        this.formaPagamento = formaPagamento;
        this.itens = Collections.unmodifiableList(new ArrayList<>(itens)); // cópia defensiva
        this.subtotal = subtotal;
        this.desconto = desconto;
        this.valorFinal = valorFinal;
    }

    public int getId()                 { return id; }
    public LocalDateTime getDataHora() { return dataHora; }
    public String getComprador()       { return comprador; }
    public String getFormaPagamento()  { return formaPagamento; }
    public List<ItemCompra> getItens() { return itens; }
    public double getSubtotal()        { return subtotal; }
    public double getDesconto()        { return desconto; }
    public double getValorFinal()      { return valorFinal; }

    /** Texto pronto para exibir o resumo final da compra no terminal. */
    public String resumo() {
        StringBuilder sb = new StringBuilder();
        sb.append("========== RESUMO DA COMPRA ==========\n");
        sb.append(String.format("Compra #%d | %s%n", id, dataHora.format(FMT_DATA)));
        sb.append(String.format("Comprador: %s%n", comprador));
        sb.append("--------------------------------------\n");
        for (ItemCompra i : itens) {
            sb.append(String.format(PT_BR, "%-20s %3d x R$ %9.2f = R$ %10.2f%n",
                    i.getNome(), i.getQuantidade(), i.getPrecoUnitario(), i.getSubtotal()));
        }
        sb.append("--------------------------------------\n");
        sb.append(String.format(PT_BR, "Subtotal:    R$ %10.2f%n", subtotal));
        sb.append(String.format(PT_BR, "Desconto:    R$ %10.2f (%s)%n", desconto, formaPagamento));
        sb.append(String.format(PT_BR, "VALOR FINAL: R$ %10.2f%n", valorFinal));
        sb.append("======================================");
        return sb.toString();
    }
}
