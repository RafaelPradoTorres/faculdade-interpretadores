package compilador;

public class ImpressorAST implements Expr.Visitante<String> {
    String imprimir(Expr expr) {
        return expr.aceita(this);
    }

    @Override
    public String visitarExprAtribuicao(Expr.Atribuicao expr) {
        return null;
    }

    @Override
    public String visitarExprBinaria(Expr.Binario expr) {
        return enfiarParenteses(expr.operador.lexema, expr.esquerda, expr.direita);
    }

    @Override
    public String visitarExprChamada(Expr.Chamada expr) {
        return null;
    }

    @Override
    public String visitarExprPegar(Expr.Pegar expr) {
        return null;
    }

    @Override
    public String visitarExprAgrupamento(Expr.Agrupamento expr) {
        return enfiarParenteses("grupo", expr.expressao);
    }

    @Override
    public String visitarExprLiteral(Expr.Literal expr) {
        if (expr.valor == null) return "nil";
        return expr.valor.toString();
    }

    @Override
    public String visitarExprLogica(Expr.Logica expr) {
        return null;
    }

    @Override
    public String visitarExprPor(Expr.Por expr) {
        return null;
    }

    @Override
    public String visitarExprSuper(Expr.Super expr) {
        return null;
    }

    @Override
    public String visitarExprEste(Expr.Este expr) {
        return null;
    }

    @Override
    public String visitarExprUnaria(Expr.Unario expr) {
        return enfiarParenteses(expr.operador.lexema, expr.direita);
    }

    @Override
    public String visitarExprVariavel(Expr.Variavel expr) {
        return null;
    }

    private String enfiarParenteses(String nome, Expr... exprs) {
        StringBuilder construtor = new StringBuilder();

        construtor.append("(").append(nome);
        for (Expr expr : exprs) {
            construtor.append(" ");
            construtor.append(expr.aceita(this));
        }
        construtor.append(")");

        return construtor.toString();
    }

    public static void main(String[] args) {
        Expr expressao = new Expr.Binario(
                new Expr.Unario(
                        new Token(TipoToken.t_sub, "-", 1, null),
                        new Expr.Literal(866)),
                new Token(TipoToken.t_mult, "*", 1, null),
                new Expr.Agrupamento(
                        new Expr.Literal(87.64)));

        System.out.println(new ImpressorAST().imprimir(expressao));
    }
}
