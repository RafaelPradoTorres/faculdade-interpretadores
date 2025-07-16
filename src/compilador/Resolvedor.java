package compilador;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Resolvedor implements Expr.Visitante<Void>, Inst.Visitante<Void> {
    private final Interpreter interpretador;
    private final Stack<Map<String, Boolean>> escopos = new Stack<>();
    private TipoFuncao funcaoAtual = TipoFuncao.NADA;

    Resolvedor(Interpreter interpretador) {
        this.interpretador = interpretador;
    }
    private enum TipoFuncao {
        NADA,
        FUNCAO,
        INICIALIZADOR,
        METODO
    }
    private enum TipoClasse {
        NADA,
        CLASSE,
        SUBCLASSE
    }

    private TipoClasse classeAtual = TipoClasse.NADA;

    void resolver(List<Inst> instancias) {
        // Empurra o escopo “global” vazio para que
        // escopos.peek() nunca esteja em uma pilha vazia
        iniciarEscopo();

        for (Inst instancia : instancias) {
            resolver(instancia);
        }

        // Fecha o escopo global (opcional)
        finalizarEscopo();
    }
    private void resolver(Inst inst) {
        inst.aceita(this);
    }
    private void resolver(Expr expr) {
        expr.aceita(this);
    }

    private void resolverFuncao(Inst.Funcao funcao, TipoFuncao tipo) {
        TipoFuncao funcaoEncapsuladora = funcaoAtual;
        funcaoAtual = tipo;

        iniciarEscopo();
        for (Token parametro : funcao.parametros) {
            declarar(parametro);
            definir(parametro);
        }
        resolver(funcao.corpo);
        finalizarEscopo();
        funcaoAtual = funcaoEncapsuladora;
    }

    @Override
    public Void visitarInstBloco(Inst.Bloco inst) {
        iniciarEscopo();
        resolver(inst.instrucoes);
        finalizarEscopo();
        return null;
    }

    @Override
    public Void visitarInstClasse(Inst.Classe inst) {
        TipoClasse classeEncapsuladora = classeAtual;
        classeAtual = TipoClasse.CLASSE;

        declarar(inst.nome);
        definir(inst.nome);

        if (inst.superclasse != null && inst.nome.equals(inst.superclasse.nome.lexema)) {
            ComDor.erro(inst.superclasse.nome, "Uma classe nao pode herdar dela mesma.");
        }

        if (inst.superclasse != null) {
            classeAtual = TipoClasse.SUBCLASSE;
            resolver(inst.superclasse);
        }

        if (inst.superclasse != null) {
            iniciarEscopo();
            escopos.peek().put("super", true);
        }

        iniciarEscopo();
        escopos.peek().put("este", true);

        for (Inst.Funcao metodo : inst.metodos) {
            TipoFuncao declaracao = TipoFuncao.METODO;
            if (metodo.nome.lexema.equals("init")) {
                declaracao = TipoFuncao.INICIALIZADOR;
            }

            resolverFuncao(metodo, declaracao);
        }

        finalizarEscopo();

        if (inst.superclasse != null) finalizarEscopo();

        classeAtual = classeEncapsuladora;
        return null;
    }

    @Override
    public Void visitarInstExpressao(Inst.Expressao inst) {
        resolver(inst.expressao);
        return null;
    }
    @Override
    public Void visitarInstFuncao(Inst.Funcao inst) {
        declarar(inst.nome);
        definir(inst.nome);

        resolverFuncao(inst, TipoFuncao.FUNCAO);
        return null;
    }
    @Override
    public Void visitarInstSe(Inst.Se inst) {
        resolver(inst.condicao);
        resolver(inst.ramoEntao);
        if (inst.ramoSenao != null) resolver(inst.ramoSenao);
        return null;
    }
    @Override
    public Void visitarInstImprimir(Inst.Imprimir inst) {
        resolver(inst.expressao);
        return null;
    }
    @Override
    public Void visitarInstRetornar(Inst.Retornar inst) {
        if (funcaoAtual == TipoFuncao.NADA) {
            ComDor.erro(inst.palavra_chave, "Não é possivel usar return no codigo de nivel superior.");
        }

        if (inst.valor != null) {
            if (funcaoAtual == TipoFuncao.INICIALIZADOR) {
                ComDor.erro(inst.palavra_chave,
                        "nao pode retornar uma valor de um inicializador.");
            }

            resolver(inst.valor);
        }
        return null;
    }
    @Override
    public Void visitarInstVar(Inst.Var inst) {
        declarar(inst.nome);
        if (inst.inicializador != null) {
            resolver(inst.inicializador);
        }
        definir(inst.nome);
        return null;
    }
    @Override
    public Void visitarInstEnquanto(Inst.Enquanto inst) {
        resolver(inst.condicao);
        resolver(inst.corpo);
        return null;
    }


    @Override
    public Void visitarExprVariavel(Expr.Variavel expr) {
        if (escopos.peek().containsKey(expr.nome.lexema) &&
                escopos.peek().get(expr.nome.lexema) == Boolean.FALSE) {
            ComDor.erro(expr.nome, "Nao posso ler variavel local em seu proprio inicializador");
        }

        resolverLocal(expr, expr.nome);
        return null;
    }
    @Override
    public Void visitarExprAtribuicao(Expr.Atribuicao expr) {
        resolver(expr.valor);
        resolverLocal(expr, expr.nome);
        return null;
    }
    @Override
    public Void visitarExprBinaria(Expr.Binario expr) {
        resolver(expr.esquerda);
        resolver(expr.direita);
        return null;
    }
    @Override
    public Void visitarExprChamar(Expr.Chamar expr) {
        resolver(expr.receptor);

        for (Expr argumento : expr.argumentos) {
            resolver(argumento);
        }

        return null;
    }

    @Override
    public Void visitarExprPegar(Expr.Pegar expr) {
        resolver(expr.objeto);
        return null;
    }
    @Override
    public Void visitarExprAgrupamento(Expr.Agrupamento expr) {
        resolver(expr.expressao);
        return null;
    }
    @Override
    public Void visitarExprLiteral(Expr.Literal expr) {
        return null;
    }
    @Override
    public Void visitarExprLogica(Expr.Logica expr) {
        resolver(expr.esquerda);
        resolver(expr.direita);
        return null;
    }
    @Override
    public Void visitarExprPor(Expr.Por expr) {
        resolver(expr.valor);
        resolver(expr.objeto);
        return null;
    }
    @Override
    public Void visitarExprSuper(Expr.Super expr) {
        if (classeAtual == TipoClasse.NADA) {
            ComDor.erro(expr.palavra_chave, "nao pode usar 'super' fora de uma classe.");
        } else if (classeAtual != TipoClasse.SUBCLASSE) {
            ComDor.erro(expr.palavra_chave, "nao pode usar 'super' em uma classe sem superclasses.");
        }

        resolverLocal(expr, expr.palavra_chave);
        return null;
    }
    @Override
    public Void visitarExprEste(Expr.Este expr) {
        if (classeAtual == TipoClasse.NADA) {
            ComDor.erro(expr.palavra_chave, "nao utilizar 'this' fora da classe.");
            return null;
        }

        resolverLocal(expr, expr.palavra_chave);
        return null;
    }
    @Override
    public Void visitarExprUnaria(Expr.Unario expr) {
        resolver(expr.direita);
        return null;
    }




    private void iniciarEscopo() {
         escopos.push(new HashMap<String, Boolean>());
    }
    private void finalizarEscopo() {
        escopos.pop();
    }

    private void declarar(Token nome) {
        if (escopos.isEmpty()) return;

        Map<String, Boolean> escopo = escopos.peek();
        if (escopo.containsKey(nome.lexema)) {
            ComDor.erro(nome, "ja temos variavel com esse nome no escopo.");
        }

        escopo.put(nome.lexema, false);
    }
    private void definir(Token nome) {
        if (escopos.isEmpty()) return;
        escopos.peek().put(nome.lexema, true);
    }
    private void resolverLocal(Expr expr, Token nome) {
        for (int i = escopos.size() - 1; i >= 0; i--) {
            if (escopos.get(i).containsKey(nome.lexema)) {
                interpretador.resolver(expr, escopos.size() - 1 - i);
                return;
            }
        }
    }
}
