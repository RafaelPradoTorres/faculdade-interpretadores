package compilador;

public class ErroTempoDeExec extends RuntimeException {
    final Token token;

    ErroTempoDeExec(Token token, String mensagem) {
        super(mensagem);
        this.token = token;
    }
}
