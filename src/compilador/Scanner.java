// Falta:
// Organizar o código
// Fazer um método para printar os tokens de forma bonita

package compilador;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.InputStreamReader;

import static compilador.TipoToken.*;


public class Scanner {
    // ######## ATRIBUTOS ########

    private static final Map<String, TipoToken> palavrasReservadas = Map.ofEntries(
            Map.entry("PRINCIPAL",  t_main),
            Map.entry("SE",         t_if),
            Map.entry("SENAO",      t_else),
            Map.entry("ENQUANTO",   t_while),
            Map.entry("IMPRIMA",    t_print),
            Map.entry("INT",        t_int),
            Map.entry("FLUT",       t_float),
            Map.entry("CAR",        t_char),
            Map.entry("TEXTO",      t_string),
            Map.entry("DURANTE",    t_string),
            Map.entry("VERDADEIRO", t_true),
            Map.entry("FALSO",      t_false),
            Map.entry("CLASSE",     t_class),
            Map.entry("FUNCAO",     t_function),
            Map.entry("RETORNA",    t_return),
            Map.entry("NULO",       t_null),
            Map.entry("SUPER",      t_super),
            Map.entry("ESTE",       t_this),
            Map.entry("VAZIO",      t_void),
            Map.entry("VAR",        t_var)
    );

    private final String codigo_fonte;
    private List<Token> tokens = new ArrayList<>();

    private int inicio = 0;
    private int atual = 0;
    private int linha = 1;


    //######## CONSTRUTOR ########

    Scanner(String codigo_fonte){
        this.codigo_fonte = codigo_fonte;
    }

    public void imprimirTokens() {
        System.out.println("=== TOKENS ===");
        for (Token t : tokens) {
            System.out.printf("%-15s %-10s literal=%s linha=%d\n",
                    t.tipo, t.lexema,
                    t.literal != null ? t.literal : "",
                    t.linha);
        }
        System.out.println("==============");
    }


    //######## FUNÇÕES PRINCIPAIS ########

    //  1.Um para ler o arquivo todo e separá-lo em tokens
    public List<Token> escanearTokens()
    {
        // Enquanto nao ta no fim
        while (!isEOF()) {
            // Atualizar o "inicio de uma palavra"
            inicio = atual;
            escanear1Token();
        }
        // Token(TokenType tipo, String lexema, int linha, Object literal)
        tokens.add(new Token(t_eof, "", linha, null));

        return tokens;
    }


    private void escanear1Token()
    {
        // Le o caractere e ja incrementa
        char c = codigo_fonte.charAt(atual++);
        //source[atual]

        //      3.1 Ver tokens simples
        switch (c){
            case '(': inserirToken(t_openParenthesis, null); break;
            case ')': inserirToken(t_closeParenthesis, null); break;
            case '[': inserirToken(t_openBracket, null); break;
            case ']': inserirToken(t_closeBracket, null); break;
            case '{': inserirToken(t_openBrace, null); break;
            case '}': inserirToken(t_closeBrace, null); break;
            case ',': inserirToken(t_comma, null); break;
            case '.': inserirToken(t_dot, null);  break;
            case '-': inserirToken(t_sub, null); break;
            case '+': inserirToken(t_sum, null); break;
            case ';': inserirToken(t_semicolon, null); break;
            case '*': inserirToken(t_mult, null);  break;
            case '!': inserirToken(t_not, null);  break;
            case '#': inserirToken(t_different, null);  break;

            case '<':
                if (corresponde('='))
                    inserirToken(t_more_equal, null);
                break;
            case '>':
                if (corresponde('='))
                    inserirToken(t_more_equal, null);
                else if (corresponde('¨'))
                    inserirToken(t_more, null);
                break;
            case '¨':
                if (corresponde('<'))
                    inserirToken(t_less, null);
                break;
            case '=':
                if (corresponde('='))
                    inserirToken(t_equal_equal, null);
                else
                    inserirToken(t_equal, null);
                break;
            case '¬':
                if (corresponde('¬'))
                    inserirToken(t_and, null);
                break;
            case '§':
                if (corresponde('§'))
                    inserirToken(t_or, null);
                break;

            case '/':
                if (corresponde('/'))
                    while (!isEOF() && codigo_fonte.charAt(atual) != '\n') atual++;
                else
                    inserirToken(t_slash, null);
                break;

            case ' ':
            case '\r':
            case '\t':
                break;

            case '\n':
                linha++;
                break;

            case '"':
                string();
                break;


            default:
                if (isDigito(c)) {
                    inteiro(); // Falta identificar ponto flutuante
                } else if (isAlfa(c)) {
                    identificador();
                } else {
                    System.err.println("ERRO LEXICO");
                    System.err.println("Atenção: caractere não reconhecido na linha " + linha);
//                    System.exit(69);
                }


        }
    }



    //######## FUNÇÕES AUXILIARES ########

    private void identificador() {
        inicio = atual - 1;
        while (!isEOF() && isAlfaNumerico(codigo_fonte.charAt(atual))){
            atual++;
        }

        String texto = codigo_fonte.substring(inicio, atual);
        TipoToken tipo = palavrasReservadas.get(texto);
        if (tipo == null) tipo = ter_identifier;
        inserirToken(tipo, null);
    }
    private void string(){
        inicio = atual - 1;
        while(!isEOF() && codigo_fonte.charAt(atual) != '"'){
            atual++;
            if (codigo_fonte.charAt(atual) == '\n')
                linha++;
        }

        if (isEOF()){
            // Aqui precisamos gerar algum erro!!!
            return;
        }

        atual++;
        String valor = codigo_fonte.substring(inicio, atual);
        inserirToken(ter_string, valor);

    }

    private void inteiro() {
        inicio = atual-1;
        while (!isEOF() && isDigito(codigo_fonte.charAt(atual))){
            atual++;
        }
        if (!isEOF() && codigo_fonte.charAt(atual) == '.') {
            if(isDigito(codigo_fonte.charAt(atual+1))) {
                atual++;
                while (!isEOF() && isDigito(codigo_fonte.charAt(atual))) {
                    atual++;
                }
                inserirToken(ter_float, codigo_fonte.substring(inicio, atual));

                return;
            }
            //Retornar erro
            System.err.println("Coloque algo depois do ponto");
            System.exit(2345678);
        }


        inserirToken(ter_integer, codigo_fonte.substring(inicio, atual));
    }

    private boolean isAlfaNumerico(char c) {
        return isAlfa(c) || isDigito(c);
    }

    private boolean isAlfa(char c) {
        return  (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                 c == '_';
    }

    private boolean isDigito(char c) {
        return c >= '0' && c <= '9';
    }


    private boolean corresponde(char caractere)
    {
        if (isEOF())
            return false;
        if (codigo_fonte.charAt(atual) != caractere)
            return false;

        atual++;
        return true;
    }

    private void inserirToken(TipoToken tipo, Object literal)
    {
        String palavra = codigo_fonte.substring(inicio, atual);

        tokens.add(new Token(tipo, palavra, linha, literal));
    }


    public int getLinha(){
        return linha;
    }

    public boolean isEOF() {
        return atual >= codigo_fonte.length();
    }


}
