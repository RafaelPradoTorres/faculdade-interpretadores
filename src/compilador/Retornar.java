package compilador;

public class Retornar extends RuntimeException {
    final Object valor;

    Retornar(Object valor) {
        super(null, null, false, false);
        this.valor = valor;
    }
}
