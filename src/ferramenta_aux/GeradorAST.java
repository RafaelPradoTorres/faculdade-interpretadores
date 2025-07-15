package ferramenta_aux;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
public class GeradorAST {
    public static void main(String[] args) throws IOException{
        if (args.length != 1) {
            System.err.println("Só aceitamos um argumento");
            System.exit(40028922);
        }
        String diretorioSaida = args[0];
        definirAST(diretorioSaida, "Expr", Arrays.asList(
                "Binary     : Expr left, Token operator, Expr right",
                "Grouping   : Expr expression",
                "Literal    : Object value",
                "Unary      : Token operator, Expr right"
        ));
    }

    private static void definirAST(String diretorioSaida, String nomeArquivo, List<String> tipos) throws IOException {
        String path = diretorioSaida + "/" + nomeArquivo + ".java";
        PrintWriter escritor = new PrintWriter(path, "UTF-8");

        escritor.println("package compilador;");
        escritor.println();
        escritor.println("import java.util.List;");
        escritor.println();
        escritor.println("abstract class " + nomeArquivo + " {");

        definirVisitante(escritor, nomeArquivo, tipos);

        // Classes AST
        for (String tipo : tipos) {
            String nomeClasse = tipo.split(":")[0].trim();
            String campos = tipo.split(":")[1].trim();
            definirTipo(escritor, nomeArquivo, nomeClasse, campos);
        }

        // """" The base accept() method
        escritor.println();
        escritor.println("  abstract <R> R accept(Visitante<R> visitante");

        escritor.println("}");
        escritor.close();
    }

    private static void definirVisitante(PrintWriter escritor, String nomeArquivo, List<String> tipos) {
        escritor.println("  interface Visitante <R> {");

        for (String tipo : tipos) {
            String tipoNome = tipo.split(":")[0].trim();
            escritor.println("  R visitar" + tipoNome + nomeArquivo + "(" +
                    tipoNome + " " + nomeArquivo.toLowerCase() + ");");
        }

        escritor.println(" }");
    }

    private static void definirTipo(PrintWriter escritor, String nomeArquivo, String nomeClasse, String listaCampo) {
        escritor.println(" static class " + nomeClasse + " extends " + nomeArquivo + " {");

        // Construtor
        escritor.println("  " + nomeClasse + "(" + listaCampo + "( {");

        // Armazenar parâmetros em campos
        String[] campos = listaCampo.split(", ");
        for (String campo: campos) {
            String nome = campo.split(" ")[1];
            escritor.println("  this." + nome + " = " + nome + ";");
        }

        escritor.println("  }");

        // Padrões do visitante
        escritor.println();
        escritor.println("  @Override");
        escritor.println("  <R> R accept(Visitante<R> visitante) {");
        escritor.println("      return visitante.visitar" + nomeClasse + nomeArquivo + "(this);");
        escritor.println("  }");

        // Campos
        escritor.println();
        for (String campo : campos) {
            escritor.println("  final " + campo + ";");
        }

        escritor.println("  }");
    }
}
