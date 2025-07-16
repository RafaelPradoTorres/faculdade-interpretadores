package compilador;

import java.util.List;

abstract class Inst {
    interface Visitante<R> {
        R visitarInstBloco(Bloco inst);
        R visitarInstClasse(Classe inst);
        R visitarInstExpressao(Expressao inst);
        R visitarInstFuncao(Funcao inst);
        R visitarInstSe(Se inst);
        R visitarInstImprimir(Imprimir inst);
        R visitarInstRetornar(Retornar inst);
        R visitarInstVar(Var inst);
        R visitarInstEnquanto(Enquanto inst);
    }

    static class Bloco extends Inst {
        Bloco(List<Inst> instrucoes) {
            this.instrucoes = instrucoes;
        }

        @Override
        <R> R aceita(Visitante<R> visitante) {
            return visitante.visitarInstBloco(this);
        }

        final List<Inst> instrucoes;
    }
    static class Expressao extends Inst {
        Expressao(Expr expressao) {
            this.expressao = expressao;
        }

        @Override
        <R> R aceita(Visitante<R> visitante) {
            return visitante.visitarInstExpressao(this);
        }

        final Expr expressao;
    }
    static class Imprimir extends Inst {
        Imprimir(Expr expressao) {
            this.expressao = expressao;
        }

        @Override
        <R> R aceita(Visitante<R> visitante) {
            return visitante.visitarInstImprimir(this);
        }

        final Expr expressao;
    }
    static class Var extends Inst {
        Var(Token nome, Expr inicializador) {
            this.nome = nome;
            this.inicializador = inicializador;
        }

        @Override
        <R> R aceita(Visitante<R> visitante) {
            return visitante.visitarInstVar(this);
        }

        final Token nome;
        final Expr inicializador;
    }

    static class Se extends Inst {
        Se(Expr condicao, Inst ramoEntao, Inst ramoSenao) {
            this.condicao = condicao;
            this.ramoEntao = ramoEntao;
            this.ramoSenao = ramoSenao;
        }

        @Override
        <R> R aceita(Visitante<R> visitante) {
            return visitante.visitarInstSe(this);
        }

        final Expr condicao;
        final Inst ramoEntao;
        final Inst ramoSenao;
    }
    static class Logica extends Inst {
        Logica(Expr esquerda, Token operador, Expr direita) {
            this.esquerda = esquerda;
            this.operador = operador;
            this.direita = direita;
        }

        @Override
        <R> R aceita(Visitante<R> visitante) {
            return visitante.visitarExprLogica(this);
        }

        final Expr esquerda;
        final Token operador;
        final Expr direita;
    }
    static class Enquanto extends Inst {
        Enquanto(Expr condicao, Inst corpo) {
            this.condicao = condicao;
            this.corpo = corpo;
        }

        @Override
        <R> R aceita(Visitante<R> visitante) {
            return visitante.visitarInstEnquanto(this);
        }

        final Expr condicao;
        final Inst corpo;
    }

    static class Funcao extends Inst {
        Funcao(Token nome, List<Token> parametros, List<Inst> corpo) {
            this.nome = nome;
            this.parametros = parametros;
            this.corpo = corpo;
        }

        @Override
        <R> R aceita(Visitante<R> visitante) {
            return visitante.visitarInstFuncao(this);
        }

        final Token nome;
        final List<Token> parametros;
        final List<Inst> corpo;
    }
    static class Retornar extends Inst {
        Retornar(Token palavra_chave, Expr valor) {
            this.palavra_chave = palavra_chave;
            this.valor = valor;
        }

        @Override
        <R> R aceita(Visitante<R> visitante) {
            return visitante.visitarInstRetornar(this);
        }

        final Token palavra_chave;
        final Expr valor;
    }

    static class Classe extends Inst {
        Classe(Token nome, Expr.Variavel superclasse, List<Inst.Funcao> metodos) {
            this.nome = nome;
            this.superclasse = superclasse;
            this.metodos = metodos;
        }

        @Override
        <R> R aceita(Visitante<R> visitante) {
            return visitante.visitarInstClasse(this);
        }

        final Token nome;
        final Expr.Variavel superclasse;
        final List<Inst.Funcao> metodos;
    }

    abstract  <R> R aceita(Visitante<R> visitante);
}
