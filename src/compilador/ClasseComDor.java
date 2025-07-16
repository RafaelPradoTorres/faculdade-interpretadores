package compilador;

import java.util.List;
import java.util.Map;

public class ClasseComDor implements ComDorChamavel {
    final ClasseComDor superclasse;
    final String nome;
    private final Map<String, FuncaoComDor> metodos;

    ClasseComDor(String nome, ClasseComDor superclasse, Map<String, FuncaoComDor> metodos) {
        this.superclasse = superclasse;
        this.nome = nome;
        this.metodos = metodos;
    }

    FuncaoComDor buscarMetodo(String nome) {
        if (metodos.containsKey(nome)) {
            return metodos.get(nome);
        }

        if (superclasse != null) {
            return superclasse.buscarMetodo(nome);
        }

        return null;
    }

    @Override
    public String toString() {
        return nome;
    }
    @Override
    public Object chamar(Interpreter interpretador, List<Object> argumentos) {
        InstanciaComDor instancia = new InstanciaComDor(this);
        FuncaoComDor inicializador = buscarMetodo("init");
        if (inicializador != null) {
            inicializador.vincular(instancia).chamar(interpretador, argumentos);
        }

        return  instancia;
    }

    @Override
    public int aridade() {
        FuncaoComDor inicializador = buscarMetodo("init");
        if (inicializador == null) return 0;
        return inicializador.aridade();
    }
}
