package compilador;

public class Token{
    // Atributos:
    TipoToken tipo;
    String lexema;
    int linha;
    Object literal; // Pode ser qq coisa, numero, string, null, ou mesmo nada

    // Construtor
    Token(TipoToken tipo, String lexema, int linha, Object literal) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linha = linha;
        this.literal = literal;
    }

    // Métodos:
    //  (opcional) toString() para imprimir o token
}
