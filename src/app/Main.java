package app;

import exception.EstoqueInsuficienteException;
import exception.PagamentoRecusadoException;
import exception.PersistenciaException;
import exception.ProdutoInvalidoException;
import model.Carrinho;
import model.Compra;
import model.ItemCarrinho;
import model.Produto;
import pagamento.CartaoCredito;
import pagamento.CartaoDebito;
import pagamento.FormaPagamento;
import pagamento.Pix;
import repository.GerenciadorArquivos;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/** Menus do terminal. Só cuida de ler o que o usuário digita e mostrar resultados. */
public class Main {

    private static final Scanner teclado = new Scanner(System.in);
    private static GerenciadorArquivos repositorio;
    private static LojaService loja;

    public static void main(String[] args) {
        try {
            repositorio = new GerenciadorArquivos(Path.of("dados"));
            loja = new LojaService(repositorio);
        } catch (PersistenciaException e) {
            System.out.println("Não foi possível carregar os dados da loja: " + e.getMessage());
            return;
        }

        int opcao;
        do {
            System.out.println("\n===== LOJA VIRTUAL =====");
            System.out.println("1 - Administrador");
            System.out.println("2 - Comprador");
            System.out.println("0 - Sair");
            opcao = lerInt("Escolha: ");

            switch (opcao) {
                case 1: menuAdmin(); break;
                case 2: menuComprador(); break;
                case 0: System.out.println("Até logo!"); break;
                default: System.out.println("Opção inválida.");
            }
        } while (opcao != 0);
    }

    // ============================ ADMIN ============================

    private static void menuAdmin() {
        int opcao;
        do {
            System.out.println("\n--- Administrador ---");
            System.out.println("1 - Cadastrar produto");
            System.out.println("2 - Listar produtos");
            System.out.println("3 - Consultar produto por ID");
            System.out.println("4 - Editar produto");
            System.out.println("5 - Remover produto");
            System.out.println("0 - Voltar");
            opcao = lerInt("Escolha: ");

            try {
                switch (opcao) {
                    case 1: cadastrarProduto(); break;
                    case 2: listarProdutos(); break;
                    case 3: consultarProduto(); break;
                    case 4: editarProduto(); break;
                    case 5: removerProduto(); break;
                    case 0: break;
                    default: System.out.println("Opção inválida.");
                }
            } catch (ProdutoInvalidoException e) {
                System.out.println("Produto inválido: " + e.getMessage());
            } catch (PersistenciaException e) {
                System.out.println("Erro ao gravar os dados, a operação foi desfeita: " + e.getMessage());
            }
        } while (opcao != 0);
    }

    private static void cadastrarProduto() throws ProdutoInvalidoException, PersistenciaException {
        int id = lerInt("ID: ");
        String nome = lerTexto("Nome: ");
        double preco = lerDouble("Preço: ");
        int estoque = lerInt("Estoque: ");

        loja.cadastrarProduto(id, nome, preco, estoque);
        System.out.println("Produto cadastrado com sucesso!");
    }

    private static void consultarProduto() throws ProdutoInvalidoException {
        Produto p = loja.buscarPorId(lerInt("ID do produto: "));
        System.out.println("ID: " + p.getId());
        System.out.println("Nome: " + p.getNome());
        System.out.println("Preço: " + dinheiro(p.getPreco()));
        System.out.println("Estoque: " + p.getEstoque());
    }

    private static void editarProduto() throws ProdutoInvalidoException, PersistenciaException {
        Produto atual = loja.buscarPorId(lerInt("ID do produto a editar: "));
        System.out.println("Editando: " + atual.getNome() + " (" + dinheiro(atual.getPreco())
                + ", estoque " + atual.getEstoque() + ")");

        String nome = lerTexto("Novo nome: ");
        double preco = lerDouble("Novo preço: ");
        int estoque = lerInt("Novo estoque: ");

        loja.editarProduto(atual.getId(), nome, preco, estoque);
        System.out.println("Produto atualizado!");
    }

    private static void removerProduto() throws ProdutoInvalidoException, PersistenciaException {
        Produto p = loja.buscarPorId(lerInt("ID do produto a remover: "));
        String resposta = lerTexto("Remover '" + p.getNome() + "'? (s/n): ");
        if (resposta.equalsIgnoreCase("s")) {
            loja.removerProduto(p.getId());
            System.out.println("Produto removido.");
        } else {
            System.out.println("Nada foi removido.");
        }
    }

    // ============================ COMPRADOR ============================

    private static void menuComprador() {
        String nome = lerTexto("Seu nome: ");
        Carrinho carrinho = new Carrinho();

        int opcao;
        do {
            System.out.println("\n--- Comprador: " + nome + " ---");
            System.out.println("1 - Ver produtos");
            System.out.println("2 - Adicionar produto ao carrinho");
            System.out.println("3 - Remover produto do carrinho");
            System.out.println("4 - Alterar quantidade de um item");
            System.out.println("5 - Ver carrinho e subtotal");
            System.out.println("6 - Finalizar compra");
            System.out.println("7 - Meu histórico de compras");
            System.out.println("0 - Voltar");
            opcao = lerInt("Escolha: ");

            try {
                switch (opcao) {
                    case 1: listarProdutos(); break;
                    case 2: adicionarAoCarrinho(carrinho); break;
                    case 3: removerDoCarrinho(carrinho); break;
                    case 4: alterarQuantidade(carrinho); break;
                    case 5: mostrarCarrinho(carrinho); break;
                    case 6: finalizarCompra(nome, carrinho); break;
                    case 7: MenuHistorico.exibir(repositorio, nome); break;
                    case 0: break;
                    default: System.out.println("Opção inválida.");
                }
            } catch (ProdutoInvalidoException | EstoqueInsuficienteException
                     | PagamentoRecusadoException e) {
                System.out.println(e.getMessage());
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println(e.getMessage());
            } catch (PersistenciaException e) {
                System.out.println("Erro ao acessar os arquivos (a compra não foi registrada): " + e.getMessage());
            }
        } while (opcao != 0);
    }

    private static void adicionarAoCarrinho(Carrinho carrinho)
            throws ProdutoInvalidoException, EstoqueInsuficienteException {
        listarProdutos();
        Produto p = loja.buscarPorId(lerInt("ID do produto: "));
        int qtd = lerInt("Quantidade: ");

        carrinho.adicionar(p, qtd);
        System.out.println(qtd + "x " + p.getNome() + " adicionado(s) ao carrinho.");
    }

    private static void removerDoCarrinho(Carrinho carrinho) {
        if (carrinho.isVazio()) {
            System.out.println("O carrinho está vazio.");
            return;
        }
        mostrarCarrinho(carrinho);
        carrinho.remover(lerInt("ID do produto a remover do carrinho: "));
        System.out.println("Item removido do carrinho.");
    }

    private static void alterarQuantidade(Carrinho carrinho) throws EstoqueInsuficienteException {
        if (carrinho.isVazio()) {
            System.out.println("O carrinho está vazio.");
            return;
        }
        mostrarCarrinho(carrinho);
        int id = lerInt("ID do produto: ");
        int qtd = lerInt("Nova quantidade: ");

        carrinho.alterarQuantidade(id, qtd);
        System.out.println("Quantidade alterada.");
    }

    private static void mostrarCarrinho(Carrinho carrinho) {
        if (carrinho.isVazio()) {
            System.out.println("O carrinho está vazio.");
            return;
        }
        System.out.println("\n--- Seu carrinho ---");
        System.out.printf("%-5s %-25s %5s %12s %12s%n", "ID", "Produto", "Qtd", "Preço un.", "Total");
        for (ItemCarrinho item : carrinho.getItens()) {
            System.out.printf("%-5d %-25s %5d %12s %12s%n",
                    item.getProduto().getId(), item.getProduto().getNome(), item.getQuantidade(),
                    dinheiro(item.getPrecoUnitario()), dinheiro(item.getSubtotal()));
        }
        System.out.println("Subtotal: " + dinheiro(carrinho.getSubtotal()));
    }

    private static void finalizarCompra(String comprador, Carrinho carrinho)
            throws EstoqueInsuficienteException, ProdutoInvalidoException,
                   PagamentoRecusadoException, PersistenciaException {
        if (carrinho.isVazio()) {
            System.out.println("O carrinho está vazio, adicione produtos antes de finalizar.");
            return;
        }
        mostrarCarrinho(carrinho);

        FormaPagamento pagamento = escolherPagamento();
        if (pagamento == null) {
            System.out.println("Forma de pagamento inválida. Compra não finalizada.");
            return;
        }

        Compra compra = loja.finalizarCompra(comprador, carrinho, pagamento);
        System.out.println("\n" + compra.resumo());
        carrinho.limpar(); // só limpa depois de a compra ter dado certo
    }

    // Aqui o "switch" só escolhe qual objeto criar; a compra em si não sabe o tipo.
    private static FormaPagamento escolherPagamento() {
        System.out.println("\nForma de pagamento:");
        System.out.println("1 - Cartão de crédito (valor integral)");
        System.out.println("2 - Cartão de débito (10% de desconto)");
        System.out.println("3 - Pix (5% de desconto)");

        switch (lerInt("Escolha: ")) {
            case 1: return new CartaoCredito(lerTexto("Número do cartão (16 dígitos): "));
            case 2: return new CartaoDebito(lerTexto("Número do cartão (16 dígitos): "));
            case 3: return new Pix(lerTexto("Chave Pix: "));
            default: return null;
        }
    }

    // ============================ COMUNS ============================

    private static void listarProdutos() {
        List<Produto> lista = loja.listarProdutos();
        if (lista.isEmpty()) {
            System.out.println("Nenhum produto cadastrado.");
            return;
        }
        System.out.println("\n--- Produtos ---");
        System.out.printf("%-5s %-25s %12s %8s%n", "ID", "Nome", "Preço", "Estoque");
        for (Produto p : lista) {
            System.out.printf("%-5d %-25s %12s %8d%n", p.getId(), p.getNome(), dinheiro(p.getPreco()), p.getEstoque());
        }
    }

    private static String dinheiro(double valor) {
        return String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", valor);
    }

    // Leitura de dados: repete até o usuário digitar algo válido (não deixa o programa cair)

    private static int lerInt(String mensagem) {
        while (true) {
            System.out.print(mensagem);
            try {
                return Integer.parseInt(teclado.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Digite um número inteiro válido.");
            }
        }
    }

    private static double lerDouble(String mensagem) {
        while (true) {
            System.out.print(mensagem);
            try {
                return Double.parseDouble(teclado.nextLine().trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                System.out.println("Digite um valor numérico válido (ex: 19,90).");
            }
        }
    }

    private static String lerTexto(String mensagem) {
        String texto;
        do {
            System.out.print(mensagem);
            texto = teclado.nextLine().trim();
            if (texto.isEmpty()) {
                System.out.println("Esse campo não pode ficar vazio.");
            }
        } while (texto.isEmpty());
        return texto;
    }
}
