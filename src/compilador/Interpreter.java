package compilador;

public class Interpreter implements Expr.Visitante<Object>{

    @Override
    public Object visitarExprLiteral(Expr.Literal expr) {
        return expr.valor;
    }

    @Override
    public Object visitarExprAgrupamento(Expr.Agrupamento expr) {
        return aval
    }

}