package compilador;

import java.util.List;


import static compilador.TipoToken.*;

public class Parser {
    private static class ErroNoParser extends RuntimeException {}

    private final List<Token> tokens;
    private int atual = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parse() {
        try {
            return expressao();
        } catch (ErroNoParser erro) {
            return null;
        }
    }

    private Expr expressao() {
        return igualdade();
    }

    private Expr igualdade() {
        Expr expr = comparacao();

        while (corresponde(t_different, t_equal_equal)) {
            Token operador = anterior();
            Expr direito = comparacao();
            expr = new Expr.Binario(expr, operador, direito);
        }

        return expr;
    }

    private Expr comparacao() {
        Expr expr = termo();

        while (corresponde(t_more, t_more_equal, t_less, t_less_equal)) {
            Token operador = anterior();
            Expr direita = termo();
            expr = new Expr.Binario(expr, operador, direita);
        }

        return expr;
    }

    private Expr termo() {
        Expr expr = fator();

        while (corresponde(t_sub, t_sum)) {
            Token operador = anterior();
            Expr direita = fator();
            expr = new Expr.Binario(expr, operador, direita);
        }

        return expr;
    }

    private Expr fator() {
        Expr expr = unario();

        while (corresponde(t_slash, t_mult)) {
            Token operador = anterior();
            Expr direita = unario();
            expr = new Expr.Binario(expr, operador, direita);
        }

        return expr;
    }

    private Expr unario() {
        if (corresponde(t_sub, t_not)) {
            Token operador = anterior();
            Expr direita = unario();
            return new Expr.Unario(operador, direita);
        }

        return primario();
    }

    private Expr primario() {
        if (corresponde(t_false)) return new Expr.Literal(false);
        if (corresponde(t_true)) return new Expr.Literal(true);
        if (corresponde(t_null)) return new Expr.Literal(null);

        if (corresponde(ter_integer, ter_string)) {
            return new Expr.Literal(anterior().literal);
        }

        if (corresponde(t_openParenthesis)) {
            Expr expr = expressao();
            consumir(t_closeParenthesis, "Mano, coloca um ')' para fechar a expressão!!");
            return new Expr.Agrupamento(expr);
        }

        throw erro(espiar(), "Esperava uma expressão em!!");
    }

    private boolean corresponde(TipoToken... tipos) {
        for (TipoToken tipo : tipos) {
            if (checar(tipo)) {
                avancar();
                return true;
            }
        }

        return false;
    }



    private Token consumir(TipoToken tipo, String mensagem) {
        if (checar(tipo)) return avancar();

        throw erro(espiar(), mensagem); // Função error ainda sera declarada
    }

    private boolean checar(TipoToken tipo) {
        if (taNoFim()) return false;
        return espiar().tipo == tipo;
    }

    private Token avancar() {
        if (!taNoFim()) atual++;
        return anterior();
    }

    private boolean taNoFim() {
        return espiar().tipo == t_eof;
    }

    private Token espiar() {
        return tokens.get(atual);
    }

    private Token anterior() {
        return tokens.get(atual - 1);
    }

    private ErroNoParser erro(Token token, String mensagem) {
        ComDor.erro(token, mensagem);
        return new ErroNoParser();
    }

    private void sincronizar() {
        avancar();

        while (!taNoFim()) {
            if (anterior().tipo == t_semicolon) return;

            switch (espiar().tipo) {
                case t_class:
                case t_function:
                case t_var:
                case t_for:
                case t_if:
                case t_while:
                case t_print:
                case t_return:
                    return;
            }

            avancar();
        }
    }



}
