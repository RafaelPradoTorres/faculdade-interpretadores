package compilador;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.List;

public class LLVMgenerator implements Expr.Visitante<String>, Inst.Visitante<Void> {

    private final StringBuilder codigo = new StringBuilder();
    private int registradorContador = 1;

    // Mapeia o nome da variável na linguagem para o nome do ponteiro na memória LLVM
    // Ex: "x" -> "%x"
    private final Map<String, String> variaveis = new HashMap<>();

    public String gerar(List<Inst> instrucoes) {
        // Preâmbulo do LLVM: declarações e preparação da função main
        codigo.append("; --- Declarações Externas (ex: funções da biblioteca C) ---\n");
        codigo.append("declare i32 @printf(i8*, ...)\n\n"); // Declarar printf

        // Strings de formato para o printf
        codigo.append("@.formato_int = private unnamed_addr constant [4 x i8] c\"%d\\0A\\00\"\n");
        codigo.append("@.formato_str = private unnamed_addr constant [4 x i8] c\"%s\\0A\\00\"\n\n");

        codigo.append("; --- Início do Código ---\n");
        codigo.append("define i32 @main() {\n");
        codigo.append("entry:\n");

        // Visita cada instrução da AST para gerar o código
        for (Inst instrucao : instrucoes) {
            aceitar(instrucao);
        }

        // Final da função main
        codigo.append("  ret i32 0\n");
        codigo.append("}\n");

        return codigo.toString();
    }

    // Métodos aceita para o Visitante
    private String aceitar(Expr expr) {
        return expr.aceita(this);
    }

    private void aceitar(Inst inst) {
        inst.aceita(this);
    }

    // Coloque estes métodos dentro da classe GeradorLLVM

    @Override
    public Void visitarInstExpressao(Inst.Expressao inst) {
        aceitar(inst.expressao); // Apenas avalia a expressão, o resultado é descartado
        return null;
    }

    @Override
    public Void visitarInstImprimir(Inst.Imprimir inst) {
        String valorReg = aceitar(inst.expressao);

        // Simplificação de iniciante: vamos assumir que se não for string, é int
        // Em um compilador real, teríamos tipos mais robustos
        if (valorReg.startsWith("@")) { // Se for um ponteiro de string global
            String formatoPtr = "@.formato_str";
            codigo.append(String.format("  call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* %s, i32 0, i32 0), i8* %s)\n", formatoPtr, valorReg));
        } else { // Senão, trata como inteiro
            String formatoPtr = "@.formato_int";
            codigo.append(String.format("  call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* %s, i32 0, i32 0), i32 %s)\n", formatoPtr, valorReg));
        }

        return null;
    }

    @Override
    public Void visitarInstVar(Inst.Var inst) {
        String nomeVar = inst.nome.lexema;
        // Para simplificar, vamos tratar todas as variáveis como globais no LLVM.
        String ponteiroVar = "@" + nomeVar;

        // Aloca a variável como um ponteiro global, inicializada com 0.
        codigo.append(String.format("%s = common global i32 0, align 4\n", ponteiroVar));
        variaveis.put(nomeVar, ponteiroVar);

        // CORREÇÃO: Apenas gere a instrução 'store' se houver um inicializador.
        if (inst.inicializador != null) {
            String valorInicialReg = aceitar(inst.inicializador);
            // Armazena o valor inicial na variável alocada.
            codigo.append(String.format("  store i32 %s, i32* %s, align 4\n", valorInicialReg, ponteiroVar));
        }

        return null;
    }


    @Override public Void visitarInstBloco(Inst.Bloco inst) { return null; }
    @Override public Void visitarInstClasse(Inst.Classe inst) { return null; }
    @Override public Void visitarInstFuncao(Inst.Funcao inst) { return null; }
    @Override public Void visitarInstSe(Inst.Se inst) { return null; }
    @Override public Void visitarInstRetornar(Inst.Retornar inst) { return null; }
    @Override public Void visitarInstEnquanto(Inst.Enquanto inst) { return null; }

    // Coloque estes métodos também dentro da classe GeradorLLVM

    @Override
    public String visitarExprAtribuicao(Expr.Atribuicao expr) {
        String nomeVar = expr.nome.lexema;
        String ponteiroVar = variaveis.get(nomeVar);
        String valorReg = aceitar(expr.valor);

        codigo.append(String.format("  store i32 %s, i32* %s, align 4\n", valorReg, ponteiroVar));
        return valorReg;
    }

    @Override
    public String visitarExprBinaria(Expr.Binario expr) {
        String regEsquerda = aceitar(expr.esquerda);
        String regDireita = aceitar(expr.direita);

        String regResultado = "%" + registradorContador++;

        String instrucao = switch (expr.operador.tipo) {
            case t_sum -> "add nsw";
            case t_sub -> "sub nsw";
            case t_mult -> "mul nsw";
            case t_slash -> "sdiv";
            default -> ""; // Outros operadores não implementados
        };

        codigo.append(String.format("  %s = %s i32 %s, %s\n", regResultado, instrucao, regEsquerda, regDireita));
        return regResultado;
    }

    @Override
    public String visitarExprLiteral(Expr.Literal expr) {
        // CORREÇÃO: Adiciona uma verificação para o valor nulo primeiro.
        if (expr.valor == null) {
            return "0"; // Representa 'NULO' como o inteiro 0 em LLVM.
        }

        if (expr.valor instanceof Double) {
            // LLVM usa inteiros, vamos converter o double para int (simplificação de iniciante)
            return String.valueOf(((Double) expr.valor).intValue());
        }
        if (expr.valor instanceof String) {
            // Cria uma constante de string global
            String strConteudo = (String)expr.valor;
            // Remove as aspas que o scanner pode ter incluído
            strConteudo = strConteudo.replace("\"", "");
            int strLen = strConteudo.length() + 1; // +1 para o \0 no final
            String strNome = "@.str" + registradorContador++;

            StringBuilder preambulo = new StringBuilder();
            preambulo.append(String.format("%s = private unnamed_addr constant [%d x i8] c\"%s\\00\"\n", strNome, strLen, strConteudo));
            codigo.insert(0, preambulo.toString());

            // Retorna um ponteiro para a string para o printf
            String ponteiroReg = "%" + registradorContador++;
            codigo.append(String.format("  %s = getelementptr inbounds [%d x i8], [%d x i8]* %s, i32 0, i32 0\n", ponteiroReg, strLen, strLen, strNome));
            return ponteiroReg;
        }
        // Adicionado para lidar com booleanos
        if (expr.valor instanceof Boolean) {
            return ((Boolean) expr.valor) ? "1" : "0"; // VERDADEIRO é 1, FALSO é 0
        }


        return expr.valor.toString();
    }

    @Override
    public String visitarExprVariavel(Expr.Variavel expr) {
        String nomeVar = expr.nome.lexema;
        String ponteiroVar = variaveis.get(nomeVar);
        String regDestino = "%" + registradorContador++;

        // Carrega o valor da memória para um registrador
        codigo.append(String.format("  %s = load i32, i32* %s, align 4\n", regDestino, ponteiroVar));
        return regDestino;
    }

    // Métodos não implementados
    @Override public String visitarExprAgrupamento(Expr.Agrupamento expr) { return aceitar(expr.expressao); }
    @Override public String visitarExprChamar(Expr.Chamar expr) { return ""; }
    @Override public String visitarExprPegar(Expr.Pegar expr) { return ""; }
    @Override public String visitarExprLogica(Expr.Logica expr) { return ""; }
    @Override public String visitarExprPor(Expr.Por expr) { return ""; }
    @Override public String visitarExprSuper(Expr.Super expr) { return ""; }
    @Override public String visitarExprEste(Expr.Este expr) { return ""; }
    @Override public String visitarExprUnaria(Expr.Unario expr) { return ""; }
    @Override public String visitarExprVetor(Expr.Vetor expr) { return ""; }
    @Override public String visitarExprAcessoIndice(Expr.AcessoIndice expr) { return ""; }
    @Override public String visitarExprAtribuicaoIndice(Expr.AtribuicaoIndice expr) { return ""; }
}