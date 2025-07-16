package compilador;

import java.util.List;

interface ComDorChamavel {
    int aridade();
    Object chamar(Interpreter interpretador, List<Object> argumentos);

}
