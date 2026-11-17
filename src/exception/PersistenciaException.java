package exception;

/**
 * Lançada em qualquer falha de leitura/escrita de arquivo (arquivo corrompido,
 * disco cheio, sem permissão...). Encapsula a IOException original como causa,
 * para que as camadas superiores não dependam de detalhes de I/O.
 */
public class PersistenciaException extends Exception {

    public PersistenciaException(String mensagem) {
        super(mensagem);
    }

    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
