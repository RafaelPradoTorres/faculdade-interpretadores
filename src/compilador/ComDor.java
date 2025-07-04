package compilador;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.List;


public class ComDor {

    public static void main(String[] args) throws IOException{

        Scanner scanner;

        //Ler o arquivo inteiro
        if (args.length == 1){
            System.out.println("Vou ler o documento completo para você");

            String arquivo = toString(args[0]);
            scanner = new Scanner(arquivo);

            List<Token> tokens = scanner.escanearTokens();

            for (Token token : tokens)
                System.out.println(token.tipo + " - " + token.lexema + " - " + token.literal);
            System.out.println("Linhas totais: " + scanner.getLinha());
        } else if (args.length == 0){
            System.out.println("Hum... talvez eu leia linha por linha... digite no prompt");
            // Rodar por prompt
        } else {
            System.out.println("kkkkkk, ô emocionado!!! só aceitamos 0 ou 1 argumento para executar o compilador.");
            System.exit(24);
        }
    }

    // Função para ler o arquivo
    // Receber o nome do arquivo como string, encontrar o endereço e ler o arquivo
    private static void rodar(String enderecoArquivo) throws IOException{
        String conteudo = new String(Files.readAllBytes(Paths.get(enderecoArquivo)));
        System.out.println(conteudo);
    }

    private static String toString(String enderecoArquivo) throws IOException{
        return new String(Files.readAllBytes(Paths.get(enderecoArquivo)));
    }


    //sinalizar reportar erro


}
