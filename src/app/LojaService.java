package app;

import exception.EstoqueInsuficienteException;
import exception.PagamentoRecusadoException;
import exception.PersistenciaException;
import exception.ProdutoInvalidoException;
import model.Carrinho;
import model.Compra;
import model.ItemCarrinho;
import model.ItemCompra;
import model.Produto;
import pagamento.FormaPagamento;
import repository.GerenciadorArquivos;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Regras de negócio da loja. Aqui não tem Scanner nem System.out (isso é do Main)
 * e também não tem leitura/escrita de arquivo direta (isso é do GerenciadorArquivos).
 */
public class LojaService {

    private final GerenciadorArquivos repositorio;
    private final TransacaoCompra transacao;
    private final List<Produto> produtos;

    public LojaService(GerenciadorArquivos repositorio) throws PersistenciaException {
        this.repositorio = repositorio;
        this.transacao = new TransacaoCompra(repositorio);
        this.produtos = new ArrayList<>(repositorio.carregarProdutos());
    }

    // ---------------------- Produtos (área do administrador) ----------------------

    public List<Produto> listarProdutos() {
        return List.copyOf(produtos);
    }

    public Produto buscarPorId(int id) throws ProdutoInvalidoException {
        for (Produto p : produtos) {
            if (p.getId() == id) {
                return p;
            }
        }
        throw new ProdutoInvalidoException("Não existe produto com o ID " + id + ".");
    }

    public void cadastrarProduto(int id, String nome, double preco, int estoque)
            throws ProdutoInvalidoException, PersistenciaException {
        Produto.validar(id, nome, preco, estoque);
        if (existe(id)) {
            throw new ProdutoInvalidoException("Já existe um produto com o ID " + id + ".");
        }
        Produto novo = new Produto(id, nome, preco, estoque);

        produtos.add(novo);
        try {
            repositorio.salvarProdutos(produtos);
        } catch (PersistenciaException e) {
            produtos.remove(novo); // não deixa na memória algo que não foi salvo
            throw e;
        }
    }

    public void editarProduto(int id, String nome, double preco, int estoque)
            throws ProdutoInvalidoException, PersistenciaException {
        Produto produto = buscarPorId(id);
        Produto antes = produto.copia();

        produto.atualizar(nome, preco, estoque); // se for inválido, não altera nada
        try {
            repositorio.salvarProdutos(produtos);
        } catch (PersistenciaException e) {
            produto.copiarDe(antes); // desfaz a edição
            throw e;
        }
    }

    public void removerProduto(int id) throws ProdutoInvalidoException, PersistenciaException {
        Produto produto = buscarPorId(id);
        int posicao = produtos.indexOf(produto);

        produtos.remove(posicao);
        try {
            repositorio.salvarProdutos(produtos);
        } catch (PersistenciaException e) {
            produtos.add(posicao, produto); // desfaz a remoção
            throw e;
        }
    }

    // ---------------------- Compra ----------------------

    /**
     * Finaliza a compra seguindo a ordem do enunciado. A parte de gravar a compra e
     * baixar o estoque (tudo ou nada) é feita pelo TransacaoCompra, que só mexe no
     * estoque em memória depois que o arquivo foi gravado com sucesso.
     */
    public Compra finalizarCompra(String comprador, Carrinho carrinho, FormaPagamento pagamento)
            throws EstoqueInsuficienteException, ProdutoInvalidoException,
                   PagamentoRecusadoException, PersistenciaException {

        // 1. carrinho com itens?
        if (carrinho.isVazio()) {
            throw new IllegalStateException("O carrinho está vazio.");
        }

        // o admin pode ter removido um produto depois de ele entrar no carrinho
        for (ItemCarrinho item : carrinho.getItens()) {
            if (!produtos.contains(item.getProduto())) {
                throw new ProdutoInvalidoException("O produto '" + item.getProduto().getNome()
                        + "' não está mais disponível. Remova-o do carrinho.");
            }
        }

        // 2. estoque suficiente (confere de novo)
        carrinho.validarEstoque();

        // 3 e 4. subtotal e desconto (cada forma de pagamento sabe calcular o seu)
        double subtotal = carrinho.getSubtotal();
        double valorFinal = pagamento.calcularValorFinal(subtotal);
        double desconto = pagamento.calcularDesconto(subtotal);

        // 5. pagamento
        pagamento.processarPagamento(valorFinal);

        // snapshot dos itens: nome e preço de AGORA ficam congelados na compra
        List<ItemCompra> itens = new ArrayList<>();
        for (ItemCarrinho item : carrinho.getItens()) {
            Produto p = item.getProduto();
            itens.add(new ItemCompra(p.getId(), p.getNome(), item.getPrecoUnitario(), item.getQuantidade()));
        }
        Compra compra = new Compra(repositorio.proximoIdCompra(), LocalDateTime.now(), comprador,
                pagamento.getNome(), itens, subtotal, desconto, valorFinal);

        // 6 e 7. grava compra + estoque e só então baixa o estoque em memória
        transacao.confirmar(compra, produtos);
        return compra;
    }

    private boolean existe(int id) {
        for (Produto p : produtos) {
            if (p.getId() == id) {
                return true;
            }
        }
        return false;
    }
}
