package compilador;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter implements Expr.Visitante<Object>, Inst.Visitante<Void> {

    final Environment globais = new Environment();
    private Environment ambiente = globais;
    private final Map<Expr, Integer> locais = new HashMap<>();

    Interpreter() {
        globais.definir("clock", new ComDorChamavel() {
            @Override
            public int aridade() { return 0; }

            @Override
            public Object chamar(Interpreter interpretador, List<Object> argumentos) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() {
                return "<native fn>";
            }
        });
    }

    void interpretar(List<Inst> instrucoes) {
        try {
            for (Inst instrucao : instrucoes) {
                executar(instrucao);
            }
        } catch (ErroTempoDeExec erro) {
            ComDor.erroTempoDeExecucao(erro);
        }
    }

    @Override
    public Object visitarExprLiteral(Expr.Literal expr) {
        return expr.valor;
    }

    @Override
    public Object visitarExprLogica(Expr.Logica expr) {
        Object esquerda = avaliar(expr.esquerda);

        if (expr.operador.tipo == TipoToken.t_or) {
            if (isVerdade(esquerda)) return esquerda;
        } else {
            if (isVerdade(esquerda)) return esquerda;
        }

        return avaliar(expr.direita);
    }

    @Override
    public Object visitarExprPor(Expr.Por expr) {
        Object objeto = avaliar(expr.objeto);

        if (!(objeto instanceof  InstanciaComDor)) {
            throw new ErroTempoDeExec(expr.nome, "Apenas instancias apresentam campos.");
        }

        Object valor = avaliar(expr.valor);
        ((InstanciaComDor)objeto).por(expr.nome, valor);
        return valor;
    }
    @Override
    public Object visitarExprEste(Expr.Este expr) {
        return buscarVariavel(expr.palavra_chave, expr);
    }

    @Override
    public Object visitarExprUnaria(Expr.Unario expr) {
        Object direita = avaliar(expr.direita);

        return switch (expr.operador.tipo) {
            case t_not -> !isVerdade(direita);
            case t_sub -> {
                checarOperando(expr.operador, direita);
                yield -(double) direita;
            }
            default -> null;
        };

    }

    @Override
    public Object visitarExprVariavel (Expr.Variavel expr) {
        return buscarVariavel(expr.nome, expr);
    }
    private Object buscarVariavel(Token nome, Expr expr) {
        Integer distancia = locais.get(expr);
        if (distancia != null) {
            return ambiente.pegarEm(distancia, nome.lexema);
        } else {
            return globais.pegar(nome);
        }
    }

    @Override
    public Void visitarInstClasse(Inst.Classe inst) {
        Object superclasse = null;
        if (inst.superclasse != null) {
            superclasse = avaliar(inst.superclasse);
            if (!(superclasse instanceof ClasseComDor)) {
                throw new ErroTempoDeExec(inst.superclasse.nome, "Superclasse deve ser uma classe.");
            }
        }

        ambiente.definir(inst.nome.lexema, null);

        Map<String, FuncaoComDor> metodos = new HashMap<>();
        for (Inst.Funcao metodo : inst.metodos) {
            FuncaoComDor funcao = new FuncaoComDor(metodo, ambiente,
                    metodo.nome.lexema.equals("init"));

            metodos.put(metodo.nome.lexema, funcao);
        }

        ClasseComDor classe = new ClasseComDor(inst.nome.lexema, (ClasseComDor)superclasse, metodos);

        ambiente.atribuir(inst.nome, classe);
        return null;
    }

    @Override
    public Void visitarInstEnquanto(Inst.Enquanto inst) {
        while (isVerdade(avaliar(inst.condicao))) {
            executar(inst.corpo);
        }
        return null;
    }

    @Override public Object visitarExprAtribuicao(Expr.Atribuicao expr) {
        Object valor = avaliar(expr.valor);

        Integer distancia = locais.get(expr);
        if (distancia != null) {
            ambiente.atribuirEm(distancia, expr.nome, valor);
        } else {
            globais.atribuir(expr.nome, valor);
        }

        return valor;
    }

    private void checarOperando(Token operador, Object operando) {
        if (operando instanceof Double) return;
        throw new ErroTempoDeExec(operador, "O operando deve ser um número!!");
    }

    private void checarOperandos(Token operador, Object esquerda, Object direita) {
        if (esquerda instanceof Double && direita instanceof Double) return;

        throw new ErroTempoDeExec(operador, "O operando deve ser numero");
    }

    private boolean isVerdade(Object objeto) {
        if (objeto == null) return false;
        if (objeto instanceof Boolean) return (boolean)objeto;
        return true;
    }

    private boolean isIgual(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null) return true;
        if (obj1 == null) return false;

        return obj1.equals(obj2);
    }

    private String comdorToString(Object objeto) {
        if (objeto == null) return "NULO";

        if (objeto instanceof Double) {
            String texto = objeto.toString();
            if (texto.endsWith(".0")) {
                texto = texto.substring(0, texto.length() - 2);
            }
            return texto;
        }

        return objeto.toString();
    }



    @Override
    public Object visitarExprAgrupamento(Expr.Agrupamento expr) {
        return avaliar(expr.expressao);
    }

    private Object avaliar(Expr expr) {
        return expr.aceita(this);
    }

    private void executar(Inst inst) {
        inst.aceita(this);
    }
    void resolver(Expr expr, int profundidade) {
        locais.put(expr, profundidade);
    }

    void executarBloco(List<Inst> instrucoes, Environment ambiente) {
        Environment anterior = this.ambiente;
        try {
            this.ambiente = ambiente;

            for (Inst instrucao : instrucoes) {
                executar(instrucao);
            }
        } finally {
            this.ambiente = anterior;
        }
    }

    @Override
    public Void visitarInstBloco(Inst.Bloco inst) {
        executarBloco(inst.instrucoes, new Environment(ambiente));
        return null;
    }

    @Override
    public Void visitarInstExpressao(Inst.Expressao inst) {
        avaliar(inst.expressao);
        return null;
    }

    public Void visitarInstFuncao(Inst.Funcao inst) {
        FuncaoComDor funcao = new FuncaoComDor(inst, ambiente, false);
        ambiente.definir(inst.nome.lexema, funcao);
        return null;
    }

    @Override
    public Void visitarInstImprimir(Inst.Imprimir inst) {
        Object valor = avaliar(inst.expressao);
        System.out.println(comdorToString(valor));
        return null;
    }

    @Override
    public Void visitarInstRetornar(Inst.Retornar inst) {
        Object valor = null;
        if (inst.valor != null) valor = avaliar(inst.valor);

        throw new Retornar(valor);
    }

    @Override
    public Void visitarInstVar(Inst.Var inst) {
        Object valor = null;
        if (inst.inicializador != null) {
            valor = avaliar(inst.inicializador);
        }

        ambiente.definir(inst.nome.lexema, valor);
        return null;
    }

    @Override
    public Object visitarExprBinaria(Expr.Binario expr) {
        Object esquerda = avaliar(expr.esquerda);
        Object direita = avaliar(expr.direita);

        switch (expr.operador.tipo) {
            case t_more:
                checarOperandos(expr.operador, esquerda, direita);
                return (double)esquerda > (double)direita;
            case t_more_equal:
                checarOperandos(expr.operador, esquerda, direita);
                return (double)esquerda >= (double)direita;
            case t_less:
                checarOperandos(expr.operador, esquerda, direita);
                return (double)esquerda < (double)direita;
            case t_less_equal:
                checarOperandos(expr.operador, esquerda, direita);
                return (double)esquerda <= (double)direita;
            case t_equal_equal:
                return isIgual(esquerda, direita);
            case t_different:
                return !isIgual(esquerda, direita);


            case t_sub:
                checarOperandos(expr.operador, esquerda, direita);
                return (double)esquerda - (double)direita;
            case t_sum:
                if (esquerda instanceof Double && direita instanceof Double) {
                    return (double)esquerda + (double)direita;
                }

                if (esquerda instanceof String && direita instanceof String) {
                    return (String)esquerda + (String)direita;
                }

                throw new ErroTempoDeExec(expr.operador, "Só aceito (num+num) ou (str+str)");
            case t_slash:
                checarOperandos(expr.operador, esquerda, direita);
                return (double)esquerda / (double)direita;
            case t_mult:
                checarOperandos(expr.operador, esquerda, direita);
                return (double)esquerda * (double)direita;
        }

        return null;
    }

    @Override
    public Object visitarExprChamar(Expr.Chamar expr) {
        Object receptor = avaliar(expr.receptor);

        List<Object> argumentos = new ArrayList<>();
        for (Expr argumento : expr.argumentos) {
            argumentos.add(avaliar(argumento));
        }

        if (!(receptor instanceof ComDorChamavel)) {
            throw new ErroTempoDeExec(expr.parenteses, "Apenas chame funcoes e classes");
        }

        ComDorChamavel funcao = (ComDorChamavel)receptor;
        if (argumentos.size() != funcao.aridade()) {
            throw new ErroTempoDeExec(expr.parenteses, "Esperado" +
                    funcao.aridade() + " argumentos, mas tem " + argumentos.size() + ".");
        }

        return funcao.chamar(this, argumentos);
    }
    @Override
    public Object visitarExprPegar(Expr.Pegar expr) {
        Object objeto = avaliar(expr.objeto);
        if (objeto instanceof InstanciaComDor) {
            return ((InstanciaComDor) objeto).pegar(expr.nome);
        }

        throw new ErroTempoDeExec(expr.nome, "Apenas instancias apresentam propriedades.");
    }

}