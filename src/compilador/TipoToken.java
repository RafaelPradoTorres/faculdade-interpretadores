package compilador;

// DEFINIÇÃO DOS TOKENS QUE SERÃO UTILIZADOS

//package java2

enum TipoToken {
    // UM CARACTERE
    t_openParenthesis,        // (
    t_closeParenthesis,       // )
    t_openBrace,              // [
    t_closeBrace,             // ]
    t_openBracket,            // {
    t_closeBracket,           // }
    t_comma,                  // ,
    t_dot,                    // .
    t_sub,                    // -
    t_sum,                    // +
    t_semicolon,              // ;
    t_slash,                   // /
    t_mult,                   // *
    t_not,                    // !
    t_different,              // #

    // UM E DOIS CARACTERES
    t_equal_equal,                  // ==
    t_equal,            // =
    t_more,                   // >¨
    t_more_equal,             // >=
    t_less,                   // ¨<
    t_less_equal,             // <=
    t_and,                    // ¬¬
    t_or,                     // §§

    // Palavras reservadas
    t_main,
    t_if,
    t_else,
    t_while,
    t_print,
    t_int,
    t_float,
    t_char,
    t_string,
    t_for,
    t_true,
    t_false,
    t_class,
    t_function,
    t_return,
    t_null,
    t_super,
    t_this,
    t_var,

    t_void,

    // Literais e identificadores
    ter_identifier,            // ^[a-zA-Z_][a-zA-Z0-9_]*$
    ter_integer,               // ^[0-9]+$
    ter_float,                 // ^[0-9]+\.[0-9]+$
    ter_char,                  // ^'(\\.|[^\\'])'$
    ter_string,                // ^"(\\.|[^\\"])*"$

    // Fim do arquivo
    t_eof

    }