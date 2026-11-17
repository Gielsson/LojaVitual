package model;

import exception.ProdutoInvalidoException;

/**
 * Produto da loja. O id nunca muda depois de criado.
 * Todas as regras de validação (preço, estoque, nome) ficam aqui,
 * assim nenhum produto inválido consegue existir no sistema.
 *
 * Obs.: o construtor lança IllegalArgumentException (sem ser "checked") para poder
 * ser usado direto na leitura do arquivo. Já o LojaService chama validar() antes,
 * que lança a ProdutoInvalidoException de domínio com a mensagem para o usuário.
 */
public class Produto {

    private final int id;
    private String nome;
    private double preco;
    private int estoque;

    public Produto(int id, String nome, double preco, int estoque) {
        try {
            validar(id, nome, preco, estoque);
        } catch (ProdutoInvalidoException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        this.id = id;
        this.nome = nome.trim();
        this.preco = preco;
        this.estoque = estoque;
    }

    // construtor interno usado só na cópia (os dados já vieram de um produto válido)
    private Produto(Produto original) {
        this.id = original.id;
        this.nome = original.nome;
        this.preco = original.preco;
        this.estoque = original.estoque;
    }

    public static void validar(int id, String nome, double preco, int estoque)
            throws ProdutoInvalidoException {
        if (id <= 0) {
            throw new ProdutoInvalidoException("O ID do produto deve ser um número positivo.");
        }
        if (nome == null || nome.isBlank()) {
            throw new ProdutoInvalidoException("O nome do produto não pode ficar vazio.");
        }
        if (Double.isNaN(preco) || Double.isInfinite(preco) || preco <= 0) {
            throw new ProdutoInvalidoException("O preço deve ser maior que zero.");
        }
        if (estoque < 0) {
            throw new ProdutoInvalidoException("A quantidade em estoque não pode ser negativa.");
        }
    }

    /** Edita nome, preço e estoque de uma vez. Se algo for inválido, nada é alterado. */
    public void atualizar(String nome, double preco, int estoque) throws ProdutoInvalidoException {
        validar(this.id, nome, preco, estoque);
        this.nome = nome.trim();
        this.preco = preco;
        this.estoque = estoque;
    }

    /** Usado pelo TransacaoCompra para dar baixa no estoque depois que a compra foi gravada. */
    public void setEstoque(int estoque) {
        if (estoque < 0) {
            throw new IllegalArgumentException("A quantidade em estoque não pode ser negativa.");
        }
        this.estoque = estoque;
    }

    public Produto copia() {
        return new Produto(this);
    }

    /** Copia os dados de outro produto (usado para desfazer uma edição quando a gravação falha). */
    public void copiarDe(Produto outro) {
        this.nome = outro.nome;
        this.preco = outro.preco;
        this.estoque = outro.estoque;
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public double getPreco() { return preco; }
    public int getEstoque() { return estoque; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Produto)) return false;
        return id == ((Produto) o).id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
