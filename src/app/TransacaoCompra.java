package app;

import exception.EstoqueInsuficienteException;
import exception.PersistenciaException;
import exception.ProdutoInvalidoException;
import model.Compra;
import model.ItemCompra;
import model.Produto;
import repository.GerenciadorArquivos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Confirma uma compra de forma consistente.
 *
 * Regra central: o estoque em MEMÓRIA só é reduzido DEPOIS que a compra e o novo
 * estoque foram gravados com sucesso em arquivo. Se qualquer passo falhar, o
 * catálogo em memória e os arquivos continuam exatamente como estavam.
 */
public class TransacaoCompra {

    private final GerenciadorArquivos repositorio;

    public TransacaoCompra(GerenciadorArquivos repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * @param compra   snapshot da compra (já com desconto calculado e pagamento aprovado)
     * @param catalogo lista VIVA de produtos mantida pela loja (será atualizada só no sucesso)
     * @throws ProdutoInvalidoException      item referencia produto inexistente
     * @throws EstoqueInsuficienteException  algum item excede o estoque atual
     * @throws PersistenciaException         falha ao gravar; nada foi alterado
     */
    public void confirmar(Compra compra, List<Produto> catalogo)
            throws ProdutoInvalidoException, EstoqueInsuficienteException, PersistenciaException {

        Map<Integer, Produto> porId = new HashMap<>();
        for (Produto p : catalogo) porId.put(p.getId(), p);

        // 1) Revalida estoque (soma quantidades caso o mesmo produto apareça em 2 itens)
        Map<Integer, Integer> qtdPorProduto = new HashMap<>();
        for (ItemCompra item : compra.getItens()) {
            qtdPorProduto.merge(item.getProdutoId(), item.getQuantidade(), Integer::sum);
        }
        for (Map.Entry<Integer, Integer> e : qtdPorProduto.entrySet()) {
            Produto p = porId.get(e.getKey());
            if (p == null) {
                throw new ProdutoInvalidoException("Produto de ID " + e.getKey() + " não existe mais no catálogo.");
            }
            if (p.getEstoque() < e.getValue()) {
                throw new EstoqueInsuficienteException(p.getId(), p.getNome(), e.getValue(), p.getEstoque());
            }
        }

        // 2) Monta o NOVO estado em cópias (o catálogo vivo ainda não foi tocado)
        List<Produto> novoEstado = new ArrayList<>();
        for (Produto p : catalogo) {
            int baixa = qtdPorProduto.getOrDefault(p.getId(), 0);
            novoEstado.add(new Produto(p.getId(), p.getNome(), p.getPreco(), p.getEstoque() - baixa));
        }

        // 3) Grava compra + estoque de forma atômica (pode lançar PersistenciaException)
        repositorio.salvarCompraAtomica(compra, novoEstado);

        // 4) Só agora, com tudo gravado, atualiza a memória
        for (Map.Entry<Integer, Integer> e : qtdPorProduto.entrySet()) {
            Produto p = porId.get(e.getKey());
            p.setEstoque(p.getEstoque() - e.getValue());
        }
    }
}
