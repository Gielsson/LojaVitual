package repository;

import exception.PersistenciaException;
import model.Compra;
import model.ItemCompra;
import model.Produto;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Repository/DAO: ÚNICA classe que toca em arquivos. Nenhuma regra de negócio aqui.
 *
 * Arquivos (pasta configurável):
 *   produtos.txt         -> id;nome;preco;estoque
 *   compras.txt          -> id;dataHora;comprador;pagamento;subtotal;desconto;valorFinal;itens
 *                           (itens = prodId,nome,precoUnit,qtd separados por '|')
 *   transacao.pendente   -> diário (journal) da compra em andamento (ver salvarCompraAtomica)
 *
 * Textos livres (nome, comprador, pagamento) são gravados com URL-encoding, assim
 * nunca conflitam com os separadores ';' ',' '|' nem com quebras de linha.
 *
 * ESTRATÉGIA DE CONSISTÊNCIA (resumo; detalhes no arquivo JUSTIFICATIVA.md):
 *   1) toda escrita é feita em arquivo .tmp + fsync + rename atômico (nunca há
 *      arquivo "meio escrito" no nome definitivo);
 *   2) a compra + o novo estoque são gravados primeiro num diário; o rename do
 *      diário é o PONTO DE COMMIT. Se o programa cair antes, nada mudou; se cair
 *      depois, a próxima inicialização conclui a operação (recuperação).
 */
public class GerenciadorArquivos {

    private static final String SEP = ";";
    private static final String SEP_ITENS = "|";
    private static final String SEP_CAMPOS_ITEM = ",";
    private static final String TAG_COMPRA = "COMPRA ";
    private static final String TAG_PRODUTO = "PRODUTO ";
    private static final String COMMIT = "COMMIT";

    private final Path arqProdutos;
    private final Path arqCompras;
    private final Path arqDiario;
    private int ultimoIdCompra;

    public GerenciadorArquivos(Path pasta) throws PersistenciaException {
        try {
            Files.createDirectories(pasta);
        } catch (IOException e) {
            throw new PersistenciaException("Não foi possível criar a pasta de dados '" + pasta + "'.", e);
        }
        this.arqProdutos = pasta.resolve("produtos.txt");
        this.arqCompras = pasta.resolve("compras.txt");
        this.arqDiario = pasta.resolve("transacao.pendente");

        recuperarTransacaoPendente();           // 1º: deixa os arquivos num estado consistente
        for (Compra c : carregarCompras()) {    // 2º: descobre o último ID de compra
            ultimoIdCompra = Math.max(ultimoIdCompra, c.getId());
        }
    }

    // =====================================================================
    // PRODUTOS
    // =====================================================================

    /** Lê todos os produtos do arquivo (lista vazia se o arquivo ainda não existe). */
    public List<Produto> carregarProdutos() throws PersistenciaException {
        List<Produto> produtos = new ArrayList<>();
        if (!Files.exists(arqProdutos)) return produtos;

        List<String> linhas = lerLinhas(arqProdutos);
        for (int n = 0; n < linhas.size(); n++) {
            String linha = linhas.get(n);
            if (linha.isBlank()) continue;
            try {
                produtos.add(parseProduto(linha));
            } catch (RuntimeException e) {
                throw new PersistenciaException("Arquivo " + arqProdutos.getFileName()
                        + " corrompido na linha " + (n + 1) + ": \"" + linha + "\". Corrija ou remova a linha.", e);
            }
        }
        return produtos;
    }

    /** Regrava o arquivo de produtos por inteiro (de forma atômica). Usado pelo CRUD do administrador. */
    public void salvarProdutos(Collection<Produto> produtos) throws PersistenciaException {
        escreverAtomico(arqProdutos, linhasProdutos(produtos));
    }

    // =====================================================================
    // COMPRAS
    // =====================================================================

    /** Lê todo o histórico de compras (lista vazia se o arquivo ainda não existe). */
    public List<Compra> carregarCompras() throws PersistenciaException {
        List<Compra> compras = new ArrayList<>();
        if (!Files.exists(arqCompras)) return compras;

        List<String> linhas = lerLinhas(arqCompras);
        for (int n = 0; n < linhas.size(); n++) {
            String linha = linhas.get(n);
            if (linha.isBlank()) continue;
            try {
                compras.add(parseCompra(linha));
            } catch (RuntimeException e) {
                throw new PersistenciaException("Arquivo " + arqCompras.getFileName()
                        + " corrompido na linha " + (n + 1) + ". O histórico não foi alterado.", e);
            }
        }
        return compras;
    }

    /** Histórico de um comprador específico (ignora maiúsculas/minúsculas). */
    public List<Compra> carregarComprasDoComprador(String comprador) throws PersistenciaException {
        List<Compra> resultado = new ArrayList<>();
        for (Compra c : carregarCompras()) {
            if (c.getComprador().equalsIgnoreCase(comprador.trim())) resultado.add(c);
        }
        return resultado;
    }

    /** Próximo identificador único de compra (sequencial, nunca repete). */
    public synchronized int proximoIdCompra() {
        return ++ultimoIdCompra;
    }

    // =====================================================================
    // TRANSAÇÃO ATÔMICA: compra + novo estoque
    // =====================================================================

    /**
     * Grava, "tudo ou nada", a compra e o estoque resultante.
     *
     * Passos:
     *   A) escreve o diário (compra + produtos já com estoque reduzido + linha COMMIT)
     *      em .tmp, faz fsync e renomeia atomicamente -> PONTO DE COMMIT.
     *      Se algo falhar aqui, lança PersistenciaException e NADA foi alterado.
     *   B) aplica o diário: acrescenta a compra em compras.txt e regrava produtos.txt
     *      (cada um via .tmp + rename atômico).
     *   C) apaga o diário.
     *
     * Depois do commit, uma falha no passo B/C não perde a compra: a próxima
     * inicialização reaplica o diário (operação idempotente).
     *
     * @param estoqueFinal catálogo COMPLETO de produtos já com o estoque reduzido
     */
    public synchronized void salvarCompraAtomica(Compra compra, Collection<Produto> estoqueFinal)
            throws PersistenciaException {

        List<String> produtosLinhas = linhasProdutos(estoqueFinal);

        List<String> diario = new ArrayList<>();
        diario.add(TAG_COMPRA + serializarCompra(compra));
        for (String p : produtosLinhas) diario.add(TAG_PRODUTO + p);
        diario.add(COMMIT);

        escreverAtomico(arqDiario, diario);               // A) ponto de commit
        ultimoIdCompra = Math.max(ultimoIdCompra, compra.getId());

        try {
            aplicar(serializarCompra(compra), produtosLinhas); // B)
            Files.deleteIfExists(arqDiario);                    // C)
        } catch (IOException | PersistenciaException e) {
            // A compra JÁ está confirmada no diário; será concluída na próxima inicialização.
            System.err.println("[Aviso] Compra #" + compra.getId() + " confirmada, mas a gravação final falhou ("
                    + e.getMessage() + "). Será concluída automaticamente na próxima inicialização.");
        }
    }

    // =====================================================================
    // RECUPERAÇÃO
    // =====================================================================

    private void recuperarTransacaoPendente() throws PersistenciaException {
        if (!Files.exists(arqDiario)) return;

        List<String> linhas = lerLinhas(arqDiario);
        boolean completo = !linhas.isEmpty() && linhas.get(linhas.size() - 1).equals(COMMIT);

        try {
            if (completo) {
                String compraLinha = null;
                List<String> produtosLinhas = new ArrayList<>();
                for (String l : linhas) {
                    if (l.startsWith(TAG_COMPRA)) compraLinha = l.substring(TAG_COMPRA.length());
                    else if (l.startsWith(TAG_PRODUTO)) produtosLinhas.add(l.substring(TAG_PRODUTO.length()));
                }
                if (compraLinha == null) throw new PersistenciaException("Diário sem registro de compra.");
                aplicar(compraLinha, produtosLinhas);
                System.out.println("[Recuperação] Compra pendente concluída com sucesso.");
            } else {
                System.out.println("[Recuperação] Transação incompleta descartada (nenhum dado foi alterado).");
            }
            Files.deleteIfExists(arqDiario);
        } catch (IOException e) {
            throw new PersistenciaException("Falha ao recuperar a transação pendente.", e);
        }
    }

    /** Aplica compra + produtos. Idempotente: não duplica a compra se ela já estiver no arquivo. */
    private void aplicar(String compraLinha, List<String> produtosLinhas) throws PersistenciaException {
        String idPrefixo = compraLinha.substring(0, compraLinha.indexOf(SEP) + 1);

        List<String> compras = Files.exists(arqCompras) ? lerLinhas(arqCompras) : new ArrayList<>();
        boolean jaGravada = compras.stream().anyMatch(l -> l.startsWith(idPrefixo));
        if (!jaGravada) {
            compras.add(compraLinha);
            escreverAtomico(arqCompras, compras);
        }
        escreverAtomico(arqProdutos, produtosLinhas);
    }

    // =====================================================================
    // SERIALIZAÇÃO
    // =====================================================================

    private List<String> linhasProdutos(Collection<Produto> produtos) {
        List<String> linhas = new ArrayList<>();
        for (Produto p : produtos) {
            linhas.add(p.getId() + SEP + enc(p.getNome()) + SEP + p.getPreco() + SEP + p.getEstoque());
        }
        return linhas;
    }

    private Produto parseProduto(String linha) {
        String[] c = linha.split(SEP, -1);
        if (c.length != 4) throw new IllegalArgumentException("Esperados 4 campos, encontrados " + c.length);
        return new Produto(Integer.parseInt(c[0]), dec(c[1]), Double.parseDouble(c[2]), Integer.parseInt(c[3]));
    }

    private String serializarCompra(Compra c) {
        StringBuilder itens = new StringBuilder();
        for (ItemCompra i : c.getItens()) {
            if (itens.length() > 0) itens.append(SEP_ITENS);
            itens.append(i.getProdutoId()).append(SEP_CAMPOS_ITEM)
                 .append(enc(i.getNome())).append(SEP_CAMPOS_ITEM)
                 .append(i.getPrecoUnitario()).append(SEP_CAMPOS_ITEM)
                 .append(i.getQuantidade());
        }
        return c.getId() + SEP + c.getDataHora() + SEP + enc(c.getComprador()) + SEP
                + enc(c.getFormaPagamento()) + SEP + c.getSubtotal() + SEP + c.getDesconto() + SEP
                + c.getValorFinal() + SEP + itens;
    }

    private Compra parseCompra(String linha) {
        String[] c = linha.split(SEP, -1);
        if (c.length != 8) throw new IllegalArgumentException("Esperados 8 campos, encontrados " + c.length);

        List<ItemCompra> itens = new ArrayList<>();
        for (String it : c[7].split("\\" + SEP_ITENS)) {
            String[] f = it.split(SEP_CAMPOS_ITEM, -1);
            if (f.length != 4) throw new IllegalArgumentException("Item malformado: " + it);
            itens.add(new ItemCompra(Integer.parseInt(f[0]), dec(f[1]), Double.parseDouble(f[2]), Integer.parseInt(f[3])));
        }
        return new Compra(Integer.parseInt(c[0]), LocalDateTime.parse(c[1]), dec(c[2]), dec(c[3]),
                itens, Double.parseDouble(c[4]), Double.parseDouble(c[5]), Double.parseDouble(c[6]));
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
    private static String dec(String s) { return URLDecoder.decode(s, StandardCharsets.UTF_8); }

    // =====================================================================
    // E/S DE BAIXO NÍVEL
    // =====================================================================

    private List<String> lerLinhas(Path arquivo) throws PersistenciaException {
        try {
            return new ArrayList<>(Files.readAllLines(arquivo, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new PersistenciaException("Erro ao ler o arquivo '" + arquivo.getFileName() + "'.", e);
        }
    }

    /** Escreve em .tmp, força gravação em disco (fsync) e renomeia atomicamente sobre o destino. */
    private void escreverAtomico(Path destino, List<String> linhas) throws PersistenciaException {
        Path tmp = destino.resolveSibling(destino.getFileName() + ".tmp");
        try {
            try (FileOutputStream fos = new FileOutputStream(tmp.toFile());
                 BufferedWriter w = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {
                for (String l : linhas) {
                    w.write(l);
                    w.newLine();
                }
                w.flush();
                fos.getFD().sync();
            }
            try {
                Files.move(tmp, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            try {
                if (Files.isRegularFile(tmp)) Files.delete(tmp);
            } catch (IOException ignorado) {
                // limpeza best-effort: o erro relevante é o original, lançado abaixo
            }
            throw new PersistenciaException("Erro ao gravar o arquivo '" + destino.getFileName() + "'.", e);
        }
    }
}
