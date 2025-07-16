package compilador;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.List;


public class ComDor {
    private static final Interpreter interpretador = new Interpreter();
    static boolean teveErro = false;
    static boolean teveErroTempoDeExec = false;

    public static void main(String[] args) throws IOException{

        Scanner scanner;

        //Ler o arquivo inteiro
        if (args.length == 1){
            System.out.println("1 argumento - vou ler arquivo");
            rodarArquivo(args[0]);
        } else if (args.length == 0){
            System.out.println("0 argumentos - iniciarei o prompt");
            rodarPrompt();
        } else {
            System.out.println("kkkkkk, ô emocionado!!! só aceitamos 0 ou 1 argumento para executar o compilador.");
            System.exit(24);
        }
    }

    // Função para ler o arquivo
    // Receber o nome do arquivo como string, encontrar o endereço e ler o arquivo
    private static void rodar(String fonte) {
        Scanner escaner = new Scanner(fonte);
        List<Token> tokens = escaner.escanearTokens();
        Parser parser = new Parser(tokens);
        List<Inst> instrucoes = parser.parse();

        if (teveErro) return;

        Resolvedor resolvedor = new Resolvedor(interpretador);
        resolvedor.resolver(instrucoes);

        if (teveErro) return;

        interpretador.interpretar(instrucoes);
    }

    private static void rodarArquivo(String enderecoArquivo) throws IOException{
        byte[] bytes = Files.readAllBytes(Paths.get(enderecoArquivo));
        rodar(new String(bytes, Charset.defaultCharset()));

        if (teveErro) System.exit(444);
        if (teveErroTempoDeExec) System.exit(888);
    }

    private static void rodarPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader leitor = new BufferedReader(input);

        for (;;) {
            System.out.print("(｡•̀ᴗ-)✧ ");
            String linha = leitor.readLine();
            if (linha == null) break;
            rodar(linha);
            teveErro = false;
        }
    }

    static void erro(int linha, String mensagem) {
        reportar(linha, "", mensagem);
    }

    private  static void reportar(int linha, String onde, String mensagem) {
        System.err.println("[Linha " + linha + "] " + "Erro " + onde + ": " + mensagem);
        teveErro = true;
    }

    static void erro(Token token, String mensagem) {
        if (token.tipo == TipoToken.t_eof) {
            reportar(token.linha, " no fim", mensagem);
        } else {
            reportar(token.linha, " em '" + token.lexema + "'", mensagem);
        }
    }

    static  void erroTempoDeExecucao(ErroTempoDeExec erro) {
        System.err.println(erro.getMessage() + "\n[linha " + erro.token.linha + "]");
        teveErroTempoDeExec = true;
    }

    private static String toString(String enderecoArquivo) throws IOException{
        return new String(Files.readAllBytes(Paths.get(enderecoArquivo)));
    }


    //sinalizar reportar erro


}
