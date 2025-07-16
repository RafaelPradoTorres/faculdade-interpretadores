package compilador;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import static compilador.TipoToken.*;

public class Parser {
    private static class ErroNoParser extends RuntimeException {}

    private final List<Token> tokens;
    private int atual = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Inst> parse() {
        List<Inst> instrucoes = new ArrayList<>();
        while (!taNoFim()) {
            instrucoes.add(declaracao());
        }
        return instrucoes;
    }

    private Expr expressao() {
        return atribuicao();
    }

    private Inst instrucao() {
        if (corresponde(t_for)) return instrucaoPara();
        if (corresponde(t_if)) return instrucaoSe();
        if (corresponde(t_print)) return instrucaoImprimir();
        if (corresponde(t_return)) return instrucaoRetornar();
        if (corresponde(t_while)) return instrucaoEnquanto();
        if (corresponde(t_openBrace)) return new Inst.Bloco(bloco());

        return instrucaoExpressao();
    }

    private Inst instrucaoPara() {
        consumir(t_openParenthesis, "Espero '( depois do 'for'");

        Inst inicializador;
        if (corresponde(t_semicolon)) {
            inicializador = null;
        } else if (corresponde(t_var)) {
            inicializador = declaracaoVariavel();
        } else {
            inicializador = instrucaoExpressao();
        }

        Expr condicao = null;
        if (!checar(t_semicolon)) {
            condicao = expressao();
        }
        consumir(t_semicolon, "Espero ';' depois da condicao do loop");

        Expr incrementar = null;
        if (!checar(t_closeParenthesis)) {
            incrementar = expressao();
        }
        consumir(t_closeParenthesis, "Eu esperava um ')' depois da clausula do for");
        Inst corpo = instrucao();

        if (incrementar != null) {
            corpo = new Inst.Bloco(Arrays.asList(corpo, new Inst.Expressao(incrementar)));
        }

        if (condicao == null) condicao = new Expr.Literal(true);
        corpo = new Inst.Enquanto(condicao, corpo);

        if (inicializador != null) {
            corpo = new Inst.Bloco(Arrays.asList(inicializador, corpo));
        }

        return corpo;
    }

    private Inst instrucaoEnquanto() {
        consumir(t_openParenthesis, "Quero '(' depois do 'enquanto'");
        Expr condicao = expressao();
        consumir(t_closeParenthesis, "Quero ')' depois da condicao");
        Inst corpo = instrucao();

        return new Inst.Enquanto(condicao, corpo);
    }

    private Inst instrucaoSe() {
        consumir(t_openParenthesis, "Espero '(' depois do *se*.");
        Expr condicao = expressao();
        consumir(t_closeParenthesis, "Espero ')' depois da condicao do *se*.");

        Inst ramoEntao = instrucao();
        Inst ramoSenao = null;
        if (corresponde(t_else)) {
            ramoSenao = instrucao();
        }

        return new Inst.Se(condicao, ramoEntao, ramoSenao);
    }

    private Inst declaracao() {
        try {
            if (corresponde(t_class)) return declaracaoClasse();
            if (corresponde(t_function)) return funcao("funcao");
            if (corresponde(t_var)) return declaracaoVariavel();

            return instrucao();
        } catch (ErroNoParser erro) {
            sincronizar();
            return null;
        }
    }


    private Inst declaracaoClasse() {
        Token nome = consumir(ter_identifier, "Experado o nome da classe.");

        Expr.Variavel superclasse = null;
        if (corresponde(t_less)) {
            consumir(ter_identifier, "Esperado o nome da superclasse.");
            superclasse = new Expr.Variavel(anterior());
        }

        consumir(t_openBrace, "Espera '{' antes do corpo da classe.");

        List<Inst.Funcao> metodos = new ArrayList<>();
        while (!checar(t_closeBrace) && !taNoFim()) {
            metodos.add(funcao("metodo"));
        }

        consumir(t_closeBrace, "Espero '}' depois do corpo da classe.");

        return new Inst.Classe(nome, superclasse, metodos);
    }
    private Inst declaracaoVariavel() {
        Token nome = consumir(ter_identifier, "Espero um nome de variavel.");

        Expr inicializador = null;
        if (corresponde(t_equal)) {
            inicializador = expressao();
        }

        consumir(t_semicolon, "Espero ';' apos a declaracao da variavel.");
        return new Inst.Var(nome, inicializador);
    }

    private Inst instrucaoImprimir() {
        Expr valor = expressao();
        consumir(t_semicolon, "Eu quero ';' após o valor.");
        return new Inst.Imprimir(valor);
    }

    private Inst instrucaoRetornar() {
        Token palavra_chave = anterior();
        Expr valor = null;
        if (!checar(t_semicolon)) {
            valor = expressao();
        }

        consumir(t_semicolon, "Espero ';' apos o valor de return.");
        return new Inst.Retornar(palavra_chave, valor);
    }

    private Inst instrucaoExpressao() {
        Expr expr = expressao();
        consumir(t_semicolon, "Eu quero ';' após o valor.");
        return new Inst.Expressao(expr);
    }

    private  Inst.Funcao funcao(String tipo) {
        Token nome = consumir(ter_identifier, "Esperado " + tipo + " nome.");
        consumir(t_openParenthesis, "Espero '(' apos " + tipo + " nome.");
        List<Token> parametros = new ArrayList<>();
        if (!checar(t_closeParenthesis)) {
            do {
                if (parametros.size() >= 255) {
                    erro(espiar(), "nao pode ter mais que 255 parametros.");
                }

                parametros.add(
                        consumir(ter_identifier, "Esperado nome de parametro"));
            } while (corresponde(t_comma));
        }
        consumir(t_closeParenthesis, "Esperado ')' apos parametros");

        consumir(t_openBrace, "Esperado '{' antes " + tipo + " corpo.");
        List<Inst> corpo = bloco();
        return new Inst.Funcao(nome, parametros, corpo);
    }

    private List<Inst> bloco() {
        List<Inst> instrucoes = new ArrayList<>();

        while (!checar(t_closeBrace) && !taNoFim()) {
            instrucoes.add(declaracao());
        }

        consumir(t_closeBrace, "Espero um '}' depois do bloco");
        return instrucoes;
    }

    private Expr atribuicao() {
        Expr expr = ou();

        if (corresponde(t_equal)) {
            Token iguais = anterior();
            Expr valor = atribuicao();

            if (expr instanceof Expr.Variavel) {
                Token nome = ((Expr.Variavel)expr).nome;
                return new Expr.Atribuicao(nome, valor);
            } else if (expr instanceof Expr.Pegar) {
                Expr.Pegar pegar = (Expr.Pegar)expr;
                return new Expr.Por(pegar.objeto, pegar.nome, valor);
            }

            erro(iguais, "Atribuicao invalida.");
        }

        return expr;
    }

    private Expr ou() {
        Expr expr = e();

        while (corresponde(t_or)) {
            Token operador = anterior();
            Expr direita = e();
            expr = new Expr.Logica(expr, operador, direita);
        }

        return expr;
    }

    private Expr e() {
        Expr expr = igualdade();

        while (corresponde(t_and)) {
            Token operador = anterior();
            Expr direita = igualdade();
            expr = new Expr.Logica(expr, operador, direita);
        }

        return expr;
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

        return chamar();
    }

    private Expr finalizarChamada(Expr receptor) {
        List<Expr> argumentos = new ArrayList<>();
        if (!checar(t_closeParenthesis)) {
            do {
                if (argumentos.size() >= 255) {
                    erro(espiar(), "Nao pode ter mais que 255 argumentos.");
                }
                argumentos.add(expressao());
            } while (corresponde(t_comma));
        }

        Token parenteses = consumir(t_closeParenthesis, "Esperado ')' depois dos argumentos.");

        return new Expr.Chamar(receptor, parenteses, argumentos);
    }

    private Expr chamar() {
        Expr expr = primario();

        while (true) {
            if (corresponde(t_openParenthesis)) {
                expr = finalizarChamada(expr);
            } else if (corresponde(t_dot)) {
                Token nome = consumir(ter_identifier, "Esperava o nome de propriedade depois do '.'.");
                expr = new Expr.Pegar(expr, nome);
            }else {
                break;
            }
        }

        return expr;
    }

    private Expr primario() {
        if (corresponde(t_false)) return new Expr.Literal(false);
        if (corresponde(t_true)) return new Expr.Literal(true);
        if (corresponde(t_null)) return new Expr.Literal(null);

        if (corresponde(ter_integer)) {
            return new Expr.Literal(((String)anterior().literal).contains(".")
                    ? Double.parseDouble(anterior().literal.toString())
                    : Integer.parseInt(anterior().literal.toString()));
        }
        if (corresponde(ter_float)) {
            return new Expr.Literal(Double.parseDouble(anterior().lexema));
        }
        if (corresponde(ter_string)) {
            return new Expr.Literal(anterior().literal);
        }

        if (corresponde(t_super)) {
            Token palavra_chave = anterior();
            consumir(t_dot, "Esperado '.' apos 'super'.");
            Token metodo = consumir(ter_identifier, "Esperado o nome do metodo da superclasse");
            return new Expr.Super(palavra_chave, metodo);
        }

        if (corresponde(t_this)) {return new Expr.Este(anterior());};

        if (corresponde(ter_identifier)) {
            return new Expr.Variavel(anterior());
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
