package model;

import exception.EstoqueInsuficienteException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carrinho de compras. Guarda no máximo um item por produto
 * (adicionar de novo o mesmo produto só soma a quantidade).
 * Importante: o carrinho NUNCA mexe no estoque do produto, só confere.
 */
public class Carrinho {

    private final Map<Integer, ItemCarrinho> itens = new LinkedHashMap<>();

    public void adicionar(Produto produto, int quantidade) throws EstoqueInsuficienteException {
        validarQuantidade(quantidade);

        ItemCarrinho existente = itens.get(produto.getId());
        int jaNoCarrinho = (existente == null) ? 0 : existente.getQuantidade();
        int total = jaNoCarrinho + quantidade;

        if (total > produto.getEstoque()) {
            // "solicitado" = o que ficaria no carrinho (o que já tinha + o novo pedido)
            throw new EstoqueInsuficienteException(produto.getId(), produto.getNome(),
                    total, produto.getEstoque());
        }

        if (existente == null) {
            itens.put(produto.getId(), new ItemCarrinho(produto, quantidade));
        } else {
            existente.setQuantidade(total);
        }
    }

    public void remover(int idProduto) {
        if (itens.remove(idProduto) == null) {
            throw new IllegalArgumentException("Esse produto não está no carrinho.");
        }
    }

    public void alterarQuantidade(int idProduto, int novaQuantidade) throws EstoqueInsuficienteException {
        ItemCarrinho item = itens.get(idProduto);
        if (item == null) {
            throw new IllegalArgumentException("Esse produto não está no carrinho.");
        }
        validarQuantidade(novaQuantidade);

        Produto p = item.getProduto();
        if (novaQuantidade > p.getEstoque()) {
            throw new EstoqueInsuficienteException(p.getId(), p.getNome(), novaQuantidade, p.getEstoque());
        }
        item.setQuantidade(novaQuantidade);
    }

    /** Confere de novo se o estoque atual cobre todos os itens (usado ao finalizar). */
    public void validarEstoque() throws EstoqueInsuficienteException {
        for (ItemCarrinho item : itens.values()) {
            Produto p = item.getProduto();
            if (item.getQuantidade() > p.getEstoque()) {
                throw new EstoqueInsuficienteException(p.getId(), p.getNome(),
                        item.getQuantidade(), p.getEstoque());
            }
        }
    }

    public double getSubtotal() {
        double soma = 0;
        for (ItemCarrinho item : itens.values()) {
            soma += item.getSubtotal();
        }
        return Math.round(soma * 100.0) / 100.0;
    }

    public List<ItemCarrinho> getItens() {
        return List.copyOf(itens.values());
    }

    public boolean isVazio() {
        return itens.isEmpty();
    }

    public void limpar() {
        itens.clear();
    }

    private void validarQuantidade(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
    }
}
