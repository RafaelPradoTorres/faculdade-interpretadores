package compilador;

import java.util.List;

public class FuncaoComDor implements ComDorChamavel {
    private final Environment fechamento;
    private final Inst.Funcao declaracao;
    private final boolean isInicializador;
    FuncaoComDor(Inst.Funcao declaracao, Environment fechamento, boolean isInicializador) {
        this.isInicializador = isInicializador;
        this.fechamento = fechamento;
        this.declaracao = declaracao;
    }

    FuncaoComDor vincular(InstanciaComDor instancia) {
        Environment ambiente = new Environment(fechamento);
        ambiente.definir("this", instancia);
        return new FuncaoComDor(declaracao, ambiente, isInicializador);
    }

    @Override
    public String toString() {
        return "<fn " + declaracao.nome.lexema + ">";
    }

    @Override
    public int aridade() {
        return declaracao.parametros.size();
    }

    @Override
    public Object chamar(Interpreter interpretador, List<Object> argumentos) {
        Environment ambiente = new Environment(fechamento);
        for (int i = 0; i < declaracao.parametros.size(); i++) {
            ambiente.definir(declaracao.parametros.get(i).lexema,
                    argumentos.get(i));
        }

        try {
            interpretador.executarBloco(declaracao.corpo, ambiente);
        } catch (Retornar retornarValor) {
            if (isInicializador) return fechamento.pegarEm(0, "este");

            return retornarValor.valor;
        }
        if (isInicializador) return fechamento.pegarEm(0, "este");
        return null;
    }
}
