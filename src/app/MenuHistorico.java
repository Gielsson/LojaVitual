package app;

import exception.PersistenciaException;
import model.Compra;
import repository.GerenciadorArquivos;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Tela de terminal: histórico de compras do comprador e total gasto.
 * Chamada pelo menu do comprador:  MenuHistorico.exibir(repositorio, nomeComprador);
 */
public class MenuHistorico {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private MenuHistorico() { }

    public static void exibir(GerenciadorArquivos repositorio, String comprador) {
        List<Compra> compras;
        try {
            compras = repositorio.carregarComprasDoComprador(comprador);
        } catch (PersistenciaException e) {
            System.out.println("Não foi possível consultar o histórico: " + e.getMessage());
            return; // o programa continua; o usuário volta ao menu
        }

        System.out.println("\n===== HISTÓRICO DE COMPRAS - " + comprador + " =====");
        if (compras.isEmpty()) {
            System.out.println("Você ainda não realizou nenhuma compra.");
            return;
        }

        System.out.printf("%-6s %-17s %-18s %12s%n", "ID", "Data/Hora", "Pagamento", "Valor pago");
        System.out.println("-----------------------------------------------------");
        double total = 0;
        for (Compra c : compras) {
            System.out.printf(PT_BR, "%-6d %-17s %-18s R$ %9.2f%n",
                    c.getId(), c.getDataHora().format(FMT_DATA), c.getFormaPagamento(), c.getValorFinal());
            total += c.getValorFinal();
        }
        System.out.println("-----------------------------------------------------");
        System.out.printf(PT_BR, "TOTAL GASTO EM %d COMPRA(S): R$ %.2f%n", compras.size(), total);
    }
}
