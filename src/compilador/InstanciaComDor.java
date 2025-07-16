package compilador;

import java.util.HashMap;
import java.util.Map;

public class InstanciaComDor {
    private ClasseComDor classe;
    private final Map<String, Object> campos = new HashMap<>();

    InstanciaComDor(ClasseComDor classe) {
        this.classe = classe;
    }

    Object pegar(Token nome) {
        if (campos.containsKey(nome.lexema)) {
            return campos.get(nome.lexema);
        }

        FuncaoComDor metodo = classe.buscarMetodo(nome.lexema);
        if (metodo != null) return metodo.vincular(this);

        throw new ErroTempoDeExec(nome, "Propriedade indefinida '" + nome.lexema + "'.");
    }
    void por(Token nome, Object valor) {
        campos.put(nome.lexema, valor);
    }


    @Override
    public String toString() {
        return classe.nome + " instancia";
    }
}
