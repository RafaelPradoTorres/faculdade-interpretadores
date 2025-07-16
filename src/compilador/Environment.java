package compilador;

import  java.util.HashMap;
import java.util.Map;

public class Environment {
    final Environment fechamento;
    private final Map<String, Object> valores = new HashMap<>();

    Environment() {
        fechamento = null;
    }

    Environment(Environment fechamento) {
        this.fechamento = fechamento;
    }

    Object pegar(Token nome) {
        if (valores.containsKey(nome.lexema)) {
            return valores.get(nome.lexema);
        }

        if (fechamento != null) return fechamento.pegar(nome);

        throw new ErroTempoDeExec(nome, "Variavel nao definida '" + nome.lexema + "'.");
    }

    void atribuir(Token nome, Object valor) {
        if (valores.containsKey(nome.lexema)) {
            valores.put(nome.lexema, valor);
            return;
        }

        if (fechamento != null) {
            fechamento.atribuir(nome, valor);
            return;
        }

        throw new ErroTempoDeExec(nome, "Variavel Indefinida '" + nome.lexema + "'.");
    }

    void definir(String nome, Object valor) {
        valores.put(nome, valor);
    }
    Environment ancestral(int distancia) {
        Environment ambiente = this;
        for (int i = 0; i < distancia; i++) {
            if (ambiente.fechamento == null) {
                // Não existe escopo tão “acima” — pare aqui
                return ambiente;
            }
            ambiente = ambiente.fechamento;
        }
        return ambiente;
    }
    Object pegarEm(int distancia, String nome) {
        Environment alvo = ancestral(distancia);
        // Agora `alvo` nunca será null.
        return alvo.valores.get(nome);
    }
    void atribuirEm(int distancia, Token nome, Object valor) {
        ancestral(distancia).valores.put(nome.lexema, valor);
    }


}
