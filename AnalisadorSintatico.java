
import java.io.*;

public class AnalisadorSintatico extends AnalisadorLexico {

   /*
      PG -> {DEC | CMD} EOF
      DEC -> DEC_V | DEC_C
      DEC_V -> (INTEGER | REAL | STRING | BOOLEAN | CHAR ) ID [ = VALOR ] {, ID [ = VALOR ] } ;
      DEC_C -> CONST ID = [ [-] VALOR];
      CMD -> CMD_A | CMD_R | CMD_T | ; | CMD_L | CMD_E
      CMD_A -> ID ['[' EXP']'] = EXP;
      CMD_R -> WHILE EXP (CMD | 'BEGIN' {CMD} 'END');
      CMD_T -> IF EXP (CMD | 'BEGIN' {CMD} 'END') [else (CMD | 'BEGIN' {CMD} 'END')];
      CMD_L -> READLN '('ID')'';
      CMD_E -> (WRITE | WRITELN) '(' EXP {, EXP } ')';
      EXP -> SEXP [( == | != | < | > | <= | >= ) SEXP]
      SEXP -> [+ | -] T {(+ | - | OR) T}
      T -> F {(* | AND | / | // | %) F}
      F -> NOT F | INTEGER '(' EXP ')' | REAL '(' EXP ')' | '(' EXP ')' | ID ['(' EXP ')'] | CONST
   */

   /* ACOES SEMANTICAS
      PG -> {DEC | CMD} EOF
      DEC -> DEC_V | DEC_C
      DEC_V -> (INTEGER | REAL | STRING | BOOLEAN | CHAR ) ID(1)(2) [ = VALOR ] {, ID [ = VALOR (3)(4)] } ;
      DEC_C -> CONST ID (5) = [ VALOR];
      CMD -> CMD_A | CMD_R | CMD_T | ; | CMD_L | CMD_E
      CMD_A -> ID (7)(8) ['[' EXP (9)']'] = EXP (10)(11);
      CMD_R -> WHILE EXP (CMD | 'BEGIN' {CMD} 'END');
      CMD_T -> IF EXP (CMD | 'BEGIN' {CMD} 'END') [else (CMD | 'BEGIN' {CMD} 'END')];
      CMD_L -> READLN '('ID')'';
      CMD_E -> (WRITE | WRITELN) '(' EXP {, EXP } ')';
      EXP -> SEXP (12) [( == | != | < | > | <= | >= ) SEXP (13)]
      SEXP -> [+ | -] T (3)(14) {(+ | - | OR) T (15)}
      T -> F (16) {(* | AND | / | // | %) F (17)}
      F -> NOT F (18)| INTEGER '(' EXP ')'| REAL '(' EXP ')' | '(' EXP ')'(19) | ID (7)(21) ['(' EXP (9) ')'] | CONST (20)
   */

    public static Token tokenLido;
    public static boolean teste = false;

    //ANALISADOR RECEBE O TOKEN
    public AnalisadorSintatico(){
        tokenLido = new Token();
    }

    public void CASATOKEN(AlfabetoEnum tokenEsperado) throws ErroPersonalizado, IOException {
        /* TESTE CASATOKEN */
        System.out.println("TK L: " + tokenLido.getTipoToken());
        System.out.println("TK E: " + tokenEsperado);
        if (tokenLido.getTipoToken() == tokenEsperado) {
            if (tokenLido.getTipoToken() != AlfabetoEnum.EOF)
                tokenLido = AnalisadorLexico.obterProximoToken();
        } else if (tokenLido.getTipoToken() == AlfabetoEnum.EOF){
            throw new ErroFimDeArquivoNaoEsperado(TPCompiladores.getLinhaPrograma());
        } else{
            throw new ErroTokenNaoEsperado(TPCompiladores.getLinhaPrograma(), tokenLido.getLexema());
        }
    }

    /*
     * Regra 1 - GERACAO DE CODIGO -> Inicializacao do Assembly
     */
    public void inicializador() throws ErroPersonalizado, IOException{
        tokenLido = AnalisadorLexico.obterProximoToken();
        TPCompiladores.assembly += "section .data ; Sessão de dados\n";
        TPCompiladores.assembly += "M: ; Rótulo para demarcar o\n";
        TPCompiladores.assembly += "; início da sessão de dados\n";
        TPCompiladores.assembly += "\tresb 0x10000 ; Reserva de temporários\n";
        TPCompiladores.assembly += "; ***Definições de variáveis e constantes\n";
        TPCompiladores.assembly += "section .text ; Sessão de código\n";
        TPCompiladores.assembly += "global _start ; Ponto inicial do programa\n";
        TPCompiladores.assembly += "_start: ; Início do programa\n";
        TPCompiladores.assembly += "; ***Comandos\n";
        PG();
        TPCompiladores.assembly += "; Halt\n";
        TPCompiladores.assembly += "mov rax, 60 ; Chamada de saída\n";
        TPCompiladores.assembly += "mov rdi, 0 ; Código de saida sem erros\n";
        TPCompiladores.assembly += "syscall ; Chama o kernel\n";
    }

    public boolean verificaCMD() {
        return (tokenLido.getTipoToken() == AlfabetoEnum.IDENTIFICADOR || tokenLido.getTipoToken() == AlfabetoEnum.WHILE || tokenLido.getTipoToken() == AlfabetoEnum.IF ||
                tokenLido.getTipoToken() == AlfabetoEnum.READLN || tokenLido.getTipoToken() == AlfabetoEnum.WRITE || tokenLido.getTipoToken() == AlfabetoEnum.WRITELN ||
                tokenLido.getTipoToken() == AlfabetoEnum.BEGIN || tokenLido.getTipoToken() == AlfabetoEnum.END || tokenLido.getTipoToken() == AlfabetoEnum.ELSE);
    }

    public boolean verificaDEC() {
        return (tokenLido.getTipoToken() == AlfabetoEnum.INTEGER  || tokenLido.getTipoToken() == AlfabetoEnum.REAL || tokenLido.getTipoToken() == AlfabetoEnum.CHAR ||
                tokenLido.getTipoToken() == AlfabetoEnum.STRING || tokenLido.getTipoToken() == AlfabetoEnum.BOOLEAN ||tokenLido.getTipoToken() == AlfabetoEnum.CONST );
    }

    public boolean verificacCONST(){
        return(tokenLido.getTipoToken() == AlfabetoEnum.CONST);
    }

    public boolean verificaCMDA(){
        return(tokenLido.getTipoToken() == AlfabetoEnum.IDENTIFICADOR);
    }

    public boolean verificaCMDR(){
        return(tokenLido.getTipoToken() == AlfabetoEnum.WHILE);
    }

    public boolean verificaCMDT(){
        return(tokenLido.getTipoToken() == AlfabetoEnum.IF);
    }

    public boolean verificaCMDL(){
        return(tokenLido.getTipoToken() == AlfabetoEnum.READLN);
    }

    private boolean verificaCMD1() {
        return (tokenLido.getTipoToken() == AlfabetoEnum.IDENTIFICADOR || tokenLido.getTipoToken() == AlfabetoEnum.WHILE
                || tokenLido.getTipoToken() == AlfabetoEnum.IF || tokenLido.getTipoToken() == AlfabetoEnum.READLN
                || tokenLido.getTipoToken() == AlfabetoEnum.WRITE || tokenLido.getTipoToken() == AlfabetoEnum.WRITELN
                || tokenLido.getTipoToken() == AlfabetoEnum.PONTO_VIRGULA);
    }

    public boolean verificaCMDE(){
        return(tokenLido.getTipoToken() == AlfabetoEnum.WRITE || tokenLido.getTipoToken() == AlfabetoEnum.WRITELN);
    }

    private boolean verificaOPR() {
        return (tokenLido.getTipoToken() == AlfabetoEnum.IGUAL_IGUAL || tokenLido.getTipoToken() == AlfabetoEnum.MENOR || tokenLido.getTipoToken() == AlfabetoEnum.DIFERENTE
                || tokenLido.getTipoToken() == AlfabetoEnum.MAIOR || tokenLido.getTipoToken() == AlfabetoEnum.MENOR_IGUAL || tokenLido.getTipoToken() == AlfabetoEnum.MAIOR_IGUAL);
    }

    private boolean verificaEXPS(){
        return (tokenLido.getTipoToken() == AlfabetoEnum.MAIS || tokenLido.getTipoToken() == AlfabetoEnum.MENOS
                || tokenLido.getTipoToken() == AlfabetoEnum.OR);
    }

    //PG -> {DEC | CMD} EOF
    public void PG() throws ErroPersonalizado, IOException{
        while(verificaDEC() || verificaCMD()){
            if(verificaDEC()){
                DEC();
            }else{
                CMD();
            }
        }
        CASATOKEN(AlfabetoEnum.EOF);
    }

    //DEC -> DEC_V | DEC_C
    public void DEC() throws ErroPersonalizado, IOException{
        if(tokenLido.getTipoToken() == AlfabetoEnum.CONST){
            DEC_C();
        }else {
            DEC_V();
        }
    }

    //DEC_V -> (INTEGER | REAL | STRING | BOOLEAN | CHAR ) ID [ = [-] VALOR ] {, ID [ = [-] VALOR ] } ;
    //DEC_V -> (INTEGER | REAL | STRING | BOOLEAN | CHAR ) ID (1)(2) [ = [-] VALOR ] {, ID [ = [-] VALOR (3)(4) ] } ;
    public void DEC_V() throws ErroPersonalizado, IOException{

        TipoEnum tipoVariavel;
        Token tokenID;
        boolean negacao = false;

        switch (tokenLido.getTipoToken()) {
            case INTEGER:
                tipoVariavel = TipoEnum.INTEGER;
                CASATOKEN(AlfabetoEnum.INTEGER);
                break;

            case REAL:
                tipoVariavel = TipoEnum.REAL;
                CASATOKEN(AlfabetoEnum.REAL);
                break;

            case CHAR:
                tipoVariavel = TipoEnum.CHAR;
                CASATOKEN(AlfabetoEnum.CHAR);
                break;

            case BOOLEAN:
                tipoVariavel = TipoEnum.BOOLEAN;
                CASATOKEN(AlfabetoEnum.BOOLEAN);
                break;

            default:
                tipoVariavel = TipoEnum.STRING;
                CASATOKEN(AlfabetoEnum.STRING);
                break;
        }

        tokenID = tokenLido;
        CASATOKEN(AlfabetoEnum.IDENTIFICADOR);

        /*
        * REALIZA A VERIFICAÇÃO SE O IDENTIFICADOR FOI DECLARADO
         */
        if(tokenID.getSimbolo().getTipoClasse() != null){
            throw new ErroIdentificadorJaDeclarado(TPCompiladores.getLinhaPrograma(), tokenID.getLexema());
        }

        tokenID.getSimbolo().setTipoClasse(ClasseEnum.VARIAVEL);
        tokenID.getSimbolo().setTipoDados(tipoVariavel);

        if(tokenLido.getTipoToken() == AlfabetoEnum.IGUAL) {

            CASATOKEN(AlfabetoEnum.IGUAL);

            if (tokenLido.getTipoToken() == AlfabetoEnum.MENOS) {
                CASATOKEN(AlfabetoEnum.MENOS);
                negacao = true;
            }

            Token tokenConstante = tokenLido;
            CASATOKEN(AlfabetoEnum.VALOR);

            if (negacao && tokenConstante.getTipoConstante() != TipoEnum.INTEGER &&
                    tokenConstante.getTipoConstante() != TipoEnum.REAL) {
                throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());
            }

            tokenID.getSimbolo().setTamanho(tokenConstante.getTamanhoConstante());

            if (!(tipoVariavel == TipoEnum.REAL && tokenConstante.getTipoConstante() == TipoEnum.INTEGER)) {
                if (tipoVariavel != tokenConstante.getTipoConstante()) {
                    throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());
                }

                tokenID.getSimbolo().setEndereco(aloca_bytes(tokenID.getSimbolo().getTamanho()));
            }

            // Declaracao e atribuicao
            TPCompiladores.assembly += "section .data\n";
            TPCompiladores.assembly += obtemComandoAtribuicao(tokenID, tokenConstante, negacao);

        }else {
                // Somente declaracao
                TPCompiladores.assembly += "section .data\n";
                switch (tipoVariavel) {
                    case INTEGER:
                        tokenID.getSimbolo().setTamanho(4);
                        TPCompiladores.assembly += "\tresd 1 ; Declaração inteiro \n ";
                        break;
                    case REAL:
                        tokenID.getSimbolo().setTamanho(4);
                        TPCompiladores.assembly += "\tresd 1 ; Declaração real \n";
                        break;
                    case CHAR:
                        tokenID.getSimbolo().setTamanho(1);
                        TPCompiladores.assembly += "\tresb 1 ; Declaração char\n";
                        break;
					case BOOLEAN:
					    tokenID.getSimbolo().setTamanho(1);
                        TPCompiladores.assembly += "\tresb 1 ; Declaração boolean\n";
                        break;
                    default: // String
                        tokenID.getSimbolo().setTamanho(256);
                        TPCompiladores.assembly += "\tresb 256 ; Declaração String\n";
                }

                // Define o endereco do simbolo e incrementa o proximo endereco livre
                tokenID.getSimbolo().setEndereco(aloca_bytes(tokenID.getSimbolo().getTamanho()));
            }


            while(tokenLido.getTipoToken() == AlfabetoEnum.VIRGULA) {

                negacao = false;

                CASATOKEN(AlfabetoEnum.VIRGULA);

                tokenID = tokenLido;

                CASATOKEN(AlfabetoEnum.IDENTIFICADOR);

                if(tokenID.getSimbolo().getTipoClasse()!= null){
                    throw new ErroIdentificadorJaDeclarado(TPCompiladores.getLinhaPrograma(), tokenID.getLexema());
                }

                //NOVO ID, ATRIBUICAO DE CLASSE E TIPOS
                tokenID.getSimbolo().setTipoClasse(ClasseEnum.VARIAVEL);
                tokenID.getSimbolo().setTipoDados(tipoVariavel);

                if(tokenLido.getTipoToken() == AlfabetoEnum.IGUAL){
                    CASATOKEN(AlfabetoEnum.IGUAL);

                    if(tokenLido.getTipoToken() == AlfabetoEnum.MENOS){
                        CASATOKEN(AlfabetoEnum.MENOS);
                        negacao = true;
                    }

                    Token tokenConstante2 = tokenLido;
                    CASATOKEN(AlfabetoEnum.VALOR);

                    if(negacao && tokenConstante2.getTipoConstante() != TipoEnum.INTEGER &&
                            tokenConstante2.getTipoConstante() != TipoEnum.REAL){
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());
                    }

                    if(!(tipoVariavel == TipoEnum.REAL && tokenConstante2.getTipoConstante() == TipoEnum.INTEGER)){
                        if(tipoVariavel != tokenConstante2.getTipoConstante()){
                            throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());
                        }
                    }
                    tokenID.getSimbolo().setTamanho(tokenConstante2.getTamanhoConstante());

                    // Declaracao e atribuicao
                    // Compilador.assembly += "section .data\n";
                    TPCompiladores.assembly += obtemComandoAtribuicao(tokenID, tokenConstante2, negacao);

                    // Define o endereco do simbolo e incrementa o proximo endereco livre
                    tokenID.getSimbolo().setEndereco(aloca_bytes(tokenID.getSimbolo().getTamanho()));

                } else {
                    // Compilador.assembly += "section .data\n";
                    // Somente declaracao
                    switch (tipoVariavel) {
                        case INTEGER:
                            tokenID.getSimbolo().setTamanho(4);
                            TPCompiladores.assembly += "\tresd 1 ; Declaracao inteiro \n ";
                            break;
                        case REAL:
                            tokenID.getSimbolo().setTamanho(4);
                            TPCompiladores.assembly += "\tresd 1 ; Declaracao float \n";
                            break;
                        case CHAR:
                            tokenID.getSimbolo().setTamanho(1);
                            TPCompiladores.assembly += "\tresb 1 ; Declaracao char\n";
                            break;
                        case BOOLEAN:
                            tokenID.getSimbolo().setTamanho(1);
                            TPCompiladores.assembly += "\tresb 1 ; Declaracao boolean\n";
                            break;
                        default: // String
                            tokenID.getSimbolo().setTamanho(256);
                            TPCompiladores.assembly += "\tresb 256 ; Declaracao String\n";
                    }

                    // Define o endereco do simbolo e incrementa o proximo endereco livre
                    tokenID.getSimbolo().setEndereco(aloca_bytes(tokenID.getSimbolo().getTamanho()));

                }
            }

        CASATOKEN(AlfabetoEnum.PONTO_VIRGULA);
        TPCompiladores.assembly += "section .text ; Sessão de código\n";
    }

    //DEC_C -> CONST ID = [ [-] VALOR];
    //DEC_C -> CONST ID (5) = [ [-] VALOR];
    public void DEC_C() throws ErroPersonalizado, IOException{
        boolean negacao = false;
        Token constante;

        CASATOKEN(AlfabetoEnum.CONST);

        Token tokenID = tokenLido;

        CASATOKEN(AlfabetoEnum.IDENTIFICADOR);

        if(tokenID.getSimbolo().getTipoClasse() != null){
            throw new ErroIdentificadorJaDeclarado(TPCompiladores.getLinhaPrograma(), tokenID.getLexema());
        }

        CASATOKEN(AlfabetoEnum.IGUAL);

        if(tokenLido.getTipoToken() == AlfabetoEnum.MENOS){
            negacao = true;
            CASATOKEN(AlfabetoEnum.MENOS);
        }

        constante = tokenLido;
        CASATOKEN(AlfabetoEnum.VALOR);

        if(negacao && constante.getTipoConstante() != TipoEnum.INTEGER && constante.getTipoConstante() != TipoEnum.REAL){
            throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());
        }

        tokenID.getSimbolo().setTipoClasse(ClasseEnum.CONSTANTE);
        tokenID.getSimbolo().setTipoDados(constante.getTipoConstante());

        tokenID.getSimbolo().setTamanho(constante.getTamanhoConstante());

        // Declaracao e atribuicao
        TPCompiladores.assembly += "section .data\n";
        TPCompiladores.assembly += obtemComandoAtribuicao(tokenID, constante, negacao);

        // Define o endereco do simbolo e incrementa o proximo endereco livre
        tokenID.getSimbolo().setEndereco(aloca_bytes(tokenID.getSimbolo().getTamanho()));

        CASATOKEN(AlfabetoEnum.PONTO_VIRGULA);

        TPCompiladores.assembly += "section .text ; Sessao de codigo\n";
    }

    //CMD -> CMD_A | CMD_R | CMD_T | CMD_L | CMD_E | ;
    public void CMD() throws ErroPersonalizado, IOException{
        if(verificaCMDA()){
            CMD_A();
        } else if(verificaCMDR()){
            CMD_R();
        }else if(verificaCMDT()){
            CMD_T();
        }else if(verificaCMDL()){
            CMD_L();
        }else if(verificaCMDE()){
            CMD_E();
        } else {
            CASATOKEN(AlfabetoEnum.PONTO_VIRGULA);
        }
    }

    //CMD_A -> ID ['[' EXP']'] = EXP;
    //CMD_A -> ID (7)(8) ['[' EXP (9)']'] = EXP (10)(11);
    private void CMD_A() throws ErroPersonalizado, IOException {

        TipoEnum tipoVariavel = tokenLido.getSimbolo().getTipoDados();
        Referencia<TipoEnum> tipoExp = new Referencia<>(TipoEnum.NULL);
        Referencia<Integer> tamanho = new Referencia<>(0);
        Referencia<Long> endereco = new Referencia<>(0L);
        boolean atribuicaoString = false;
        long temporario = 0L;

        // Verificação compatibilidade de classe. O token lido deve ser da classe
        // variavel
        if (tokenLido.getSimbolo().getTipoClasse() == ClasseEnum.CONSTANTE)
            throw new ErroClasseIdentificadorIncompativel(TPCompiladores.getLinhaPrograma(), tokenLido.getLexema());

        // Verifica se o identificador foi declarado
        if (tipoVariavel == TipoEnum.NULL)
            throw new ErroIdentificadorNaoDeclarado(TPCompiladores.getLinhaPrograma(), tokenLido.getLexema());

        // Guarda o token do identificador
        Token tokenIdentificador = tokenLido;

        TPCompiladores.assembly += "\n; Inicio atribuicao: " + tokenLido.getLexema() + "\n";

        CASATOKEN(AlfabetoEnum.IDENTIFICADOR);

        if (tokenLido.getTipoToken() == AlfabetoEnum.ABRE_COLCHETE) {
            atribuicaoString = true;

            if (tipoVariavel != TipoEnum.STRING)
                throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());

            CASATOKEN(AlfabetoEnum.ABRE_COLCHETE);

            TPCompiladores.assembly += "; Inicio do calculo da posicao do vetor\n";

            EXP(tipoExp, tamanho, endereco);

            // Zera temporarios
            TPCompiladores.proximoTemporarioLivre = 0;

            TPCompiladores.assembly += "\n; Fim do calculo da posicao do vetor\n";

            if (tipoExp.referencia != TipoEnum.INTEGER)
                throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());

            CASATOKEN(AlfabetoEnum.FECHA_COLCHETE);

            // Caso ocorra acesso a posicao da string, a expressao devera ter o tipo char
            // (id devera ser do tipo string)
            tipoVariavel = TipoEnum.CHAR;

            temporario = novoEnderecoTemporarios(tamanho.referencia);
            TPCompiladores.assembly += "\tmov EBX, [M+" + enderecoParaHexa(endereco.referencia) + "] ; Recupera o valor da posicao do vetor\n";
            TPCompiladores.assembly += "\tmov [M+" + enderecoParaHexa(temporario) + "], EBX\n";

        }

        CASATOKEN(AlfabetoEnum.IGUAL);
        EXP(tipoExp, tamanho, endereco);

        // Zera temporarios
        TPCompiladores.proximoTemporarioLivre = 0;

        // Identificadores de variáveis reais podem receber números inteiros
        if (tipoExp.referencia != tipoVariavel
                && !(tipoVariavel == TipoEnum.REAL && tipoExp.referencia == TipoEnum.INTEGER)) {
            throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());
        }

        String rotulo;
        if (atribuicaoString) {
            TPCompiladores.assembly += "; Atribuicao posicao da string (char)\n";
            TPCompiladores.assembly += "\tmov RAX, 0 ; Zera RAX\n";
            TPCompiladores.assembly += "\tmov EAX, [M+" + enderecoParaHexa(temporario) + "]\n";
            TPCompiladores.assembly += "\tmov RSI, M+" + enderecoParaHexa(tokenIdentificador.getSimbolo().getEndereco())
                    + "\n";
            TPCompiladores.assembly += "\tadd RSI, RAX ; Soma endereco base + deslocamento\n";
            TPCompiladores.assembly += "\tmov AL, [M+" + enderecoParaHexa(endereco.referencia)
                    + "] ; Obtem o caractere a ser atribuido\n";
            TPCompiladores.assembly += "\tmov [RSI], AL ; Atribui o caractere na posicao de memoria\n";

        } else {
            if (tipoVariavel == TipoEnum.STRING) {
                rotulo = geraRotulo();
                TPCompiladores.assembly += "; Atribuicao string\n";
                TPCompiladores.assembly += "\tmov RSI, M+" + enderecoParaHexa(endereco.referencia) + "\n";
                TPCompiladores.assembly += "\tmov RDI, M+" + enderecoParaHexa(tokenIdentificador.getSimbolo().getEndereco())
                        + "\n";
                TPCompiladores.assembly += rotulo + ":\n";
                TPCompiladores.assembly += "\tmov DL, [RSI]\n";
                TPCompiladores.assembly += "\tmov [RDI], DL\n";
                TPCompiladores.assembly += "\tadd RSI, 1\n";
                TPCompiladores.assembly += "\tadd RDI, 1\n";
                TPCompiladores.assembly += "\tcmp DL, 0\n";
                TPCompiladores.assembly += "\tjne " + rotulo + "\n";
                TPCompiladores.assembly += "\tadd [RDI], 0\n";
                TPCompiladores.assembly += "; Atribuicao string\n";

            } else if (tipoVariavel == TipoEnum.CHAR) {
                TPCompiladores.assembly += "; Atribuicao char\n";
                TPCompiladores.assembly += "\tmov AL, [M+" + enderecoParaHexa(endereco.referencia) + "]\n";
                TPCompiladores.assembly += "\tmov [M+" + enderecoParaHexa(tokenIdentificador.getSimbolo().getEndereco())
                        + "], AL\n";
            } else if (tipoVariavel == TipoEnum.REAL && tipoExp.referencia == TipoEnum.INTEGER) {
                TPCompiladores.assembly += "; Atribuicao inteiro em float\n";
                TPCompiladores.assembly += "\tmov EAX, [M+" + enderecoParaHexa(endereco.referencia) + "] ; Moveu EAX para o inteiro\n";
                TPCompiladores.assembly += "\tcvtsi2ss XMM0, EAX ; Converteu o numero de EAX em float\n";
                TPCompiladores.assembly += "\tmovss [M+" + enderecoParaHexa(tokenIdentificador.getSimbolo().getEndereco())
                        + "], XMM0 ; Coloca no identificador o valor convertido para float\n";

            } else if (tipoVariavel == TipoEnum.REAL && tipoExp.referencia == TipoEnum.REAL) {
                TPCompiladores.assembly += "; Atribuicao float em inteiro\n";
                TPCompiladores.assembly += "\tmovss XMM0, [M+" + enderecoParaHexa(endereco.referencia) + "] ; Moveu valor float para XMM0\n";
                TPCompiladores.assembly += "\tmovss [M+" + enderecoParaHexa(tokenIdentificador.getSimbolo().getEndereco())
                        + "], XMM0 ; Coloca no identificador o valor float\n";
            } else if (tipoVariavel == TipoEnum.INTEGER) {
                TPCompiladores.assembly += "; Atribuicao int\n";
                TPCompiladores.assembly += "\tmov EAX, [M+" + enderecoParaHexa(endereco.referencia) + "]\n";
                TPCompiladores.assembly += "\tmov [M+" + enderecoParaHexa(tokenIdentificador.getSimbolo().getEndereco())
                        + "], EAX\n";
            } else {
                TPCompiladores.assembly += "; Atribuicao boolean\n";
                TPCompiladores.assembly += "\tmov AL, [M+" + enderecoParaHexa(endereco.referencia) + "]\n";
                TPCompiladores.assembly += "\tmov [M+" + enderecoParaHexa(tokenIdentificador.getSimbolo().getEndereco()) +
                    "], AL\n";
         }
        }
        CASATOKEN(AlfabetoEnum.PONTO_VIRGULA);
    }

    //CMD_R -> WHILE EXP (CMD | 'BEGIN' {CMD} 'END')
    public void CMD_R() throws ErroPersonalizado, IOException{
        CASATOKEN(AlfabetoEnum.WHILE);
        Referencia<TipoEnum> tipoExp = new Referencia<>(TipoEnum.NULL);
        Referencia<Integer> tamanho = new Referencia<>(0);
        Referencia<Long> endereco = new Referencia<>(0L);

        String rotuloInicio = geraRotulo();
        String rotuloFim = geraRotulo();

        TPCompiladores.assembly += "; Inicio while\n";
        TPCompiladores.assembly += rotuloInicio + ":\n";

        EXP(tipoExp, tamanho, endereco);

        // Zera temporarios
        TPCompiladores.proximoTemporarioLivre = 0;

        if (tipoExp.referencia != TipoEnum.BOOLEAN)
            throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());

        TPCompiladores.assembly += "\tmov EAX, [M+" + enderecoParaHexa(endereco.referencia)
                + "] ; Carrega para EAX o EXP\n";
        TPCompiladores.assembly += "\tcmp EAX, 0 ; Compara se o resultado da exp é falso\n";
        TPCompiladores.assembly += "\tje " + rotuloFim + "\n";


        if(tokenLido.getTipoToken() == AlfabetoEnum.BEGIN){
            CASATOKEN(AlfabetoEnum.BEGIN);
            while(verificaCMD1()){
                CMD();
            }
            CASATOKEN(AlfabetoEnum.END);
        }else{
            CMD();
        }

        TPCompiladores.assembly += "\tjmp " + rotuloInicio + "\n";
        TPCompiladores.assembly += "; Fim While\n";
        TPCompiladores.assembly += rotuloFim + ":\n";

    }

    //CMD_T -> IF EXP (CMD | 'BEGIN' {CMD} 'END') [else (CMD | 'BEGIN' {CMD} 'END')]
    public void CMD_T() throws ErroPersonalizado, IOException{
        CASATOKEN(AlfabetoEnum.IF);
        Referencia<TipoEnum> tipoExp = new Referencia<>(TipoEnum.NULL);
        Referencia<Integer> tamanho = new Referencia<>(0);
        Referencia<Long> endereco = new Referencia<>(0L);

        EXP(tipoExp, tamanho, endereco);

        // Zera temporarios
        TPCompiladores.proximoTemporarioLivre = 0;

        if (tipoExp.referencia != TipoEnum.BOOLEAN)
            throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());

        String rotuloFalso = geraRotulo();
        String rotuloFim = geraRotulo();

        TPCompiladores.assembly += "; Inicio IF\n";
        TPCompiladores.assembly += "\tmov AL, [M+" + enderecoParaHexa(endereco.referencia) + "]\n";
        TPCompiladores.assembly += "\tcmp AL, 0 ; Compara se a condicao e falsa\n";
        TPCompiladores.assembly += "\tje " + rotuloFalso + " ; Se for falsa salta\n";

        if(tokenLido.getTipoToken() == AlfabetoEnum.BEGIN){
            CASATOKEN(AlfabetoEnum.BEGIN);
            while(verificaCMD1()){
                CMD();
            }

            /*while(tokenLido.getTipoToken() != AlfabetoEnum.END){
                CMD();
            }*/
            CASATOKEN(AlfabetoEnum.END);
        }else{
            CMD();
        }

        if(tokenLido.getTipoToken() == AlfabetoEnum.ELSE){
            CASATOKEN(AlfabetoEnum.ELSE);
            TPCompiladores.assembly += "\tjmp " + rotuloFim + "\n";
            TPCompiladores.assembly += rotuloFalso + ":\n";
            if(tokenLido.getTipoToken() == AlfabetoEnum.BEGIN){
                CASATOKEN(AlfabetoEnum.BEGIN);
                /*CMD();
                while(tokenLido.getTipoToken() != AlfabetoEnum.END){
                    CMD();
                }*/
                while(verificaCMD1()){
                    CMD();
                }
                CASATOKEN(AlfabetoEnum.END);
            }else{
                CMD();
            }
            TPCompiladores.assembly += rotuloFim + ":\n";
        } else {
            TPCompiladores.assembly += rotuloFalso + ":\n";
        }
    }

    //CMD_L -> READLN '('ID')'';
    public void CMD_L() throws ErroPersonalizado, IOException{

        CASATOKEN(AlfabetoEnum.READLN);
        CASATOKEN(AlfabetoEnum.ABRE_PARENTESES);

        TipoEnum tipoIdentificador = tokenLido.getSimbolo().getTipoDados();
        Token tokenIdentificador = tokenLido;

        CASATOKEN(AlfabetoEnum.IDENTIFICADOR);

        if (tokenIdentificador.getSimbolo().getTipoClasse() == ClasseEnum.CONSTANTE)
            throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());

        if (tipoIdentificador == TipoEnum.NULL)
            throw new ErroIdentificadorNaoDeclarado(TPCompiladores.getLinhaPrograma(), tokenIdentificador.getLexema());

        geraAssemblyRead(tokenIdentificador);

        CASATOKEN(AlfabetoEnum.FECHA_PARENTESES);
        CASATOKEN(AlfabetoEnum.PONTO_VIRGULA);
    }

    //CMD_E -> (WRITE | WRITELN) '(' EXP {, EXP } ')';
    public void CMD_E() throws ErroPersonalizado, IOException{

        boolean ehNovaLinha = false;

        if(tokenLido.getTipoToken() == AlfabetoEnum.WRITE){
            CASATOKEN(AlfabetoEnum.WRITE);
        } else {
            CASATOKEN(AlfabetoEnum.WRITELN);
            ehNovaLinha = true;
        }

        String rotulo = "";
        Referencia<TipoEnum> tipoExp = new Referencia<>(TipoEnum.NULL);
        Referencia<Integer> tamanho = new Referencia<>(0);
        Referencia<Long> endereco = new Referencia<>(0L);

        CASATOKEN(AlfabetoEnum.ABRE_PARENTESES);
        EXP(tipoExp, tamanho, endereco);


        // Zera temporarios
        TPCompiladores.proximoTemporarioLivre = 0;

        if (tipoExp.referencia == TipoEnum.BOOLEAN)
            throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());

        if(tokenLido.getTipoToken() == AlfabetoEnum.VIRGULA){

            geraAssemblyWrite(tipoExp, endereco, tamanho);

            while(tokenLido.getTipoToken() == AlfabetoEnum.VIRGULA){
                CASATOKEN(AlfabetoEnum.VIRGULA);
                EXP(tipoExp, tamanho, endereco);

                TPCompiladores.proximoTemporarioLivre = 0;
                if (tipoExp.referencia == TipoEnum.BOOLEAN)
                    throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());

                geraAssemblyWrite(tipoExp, endereco, tamanho);
            }

        } else {
            geraAssemblyWrite(tipoExp, endereco, tamanho);
        }
        if (ehNovaLinha) {
            String buffer = enderecoParaHexa(novoEnderecoTemporarios(1));
            TPCompiladores.assembly += "; Da a quebra de linha\n";
            TPCompiladores.assembly += "\tmov RSI, M+" + buffer + "\n";
            TPCompiladores.assembly += "\tmov [RSI], byte 10\n";
            TPCompiladores.assembly += "\tmov RDX,1 ; ou buffer.tam\n";
            TPCompiladores.assembly += "\tmov RAX, 1 ; chamada para saída\n";
            TPCompiladores.assembly += "\tmov RDI, 1 ; saída para tela\n";
            TPCompiladores.assembly += "\tsyscall\n\n";
        }

        CASATOKEN(AlfabetoEnum.FECHA_PARENTESES);
        CASATOKEN(AlfabetoEnum.PONTO_VIRGULA);
    }

    //EXP -> SEXP [( == | != | < | > | <= | >= ) SEXP]
    //EXP -> SEXP (12) [( == | != | < | > | <= | >= ) SEXP (13)]
    public void EXP(Referencia<TipoEnum> tipo, Referencia<Integer> tamanho, Referencia<Long> endereco) throws ErroPersonalizado, IOException{

        SEXP(tipo, tamanho, endereco);
        String tempExp = enderecoParaHexa(endereco.referencia);

        Referencia<TipoEnum> tipoExpsEsq = new Referencia<>(tipo.referencia);
        Referencia<TipoEnum> tipoExpsDir = new Referencia<>(TipoEnum.NULL);

        AlfabetoEnum operador;
        boolean comparacaoReal = false;
        String rotuloVerdadeiro;
        String rotuloFim;
        String rotuloFalso;

        if(verificaOPR()){
            operador = tokenLido.getTipoToken();

            switch (operador){
                case IGUAL_IGUAL:
                    if (tipoExpsEsq.referencia == TipoEnum.BOOLEAN)
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());
                    CASATOKEN(AlfabetoEnum.IGUAL_IGUAL);
                    break;
                case DIFERENTE:
                    if (tipoExpsEsq.referencia == TipoEnum.STRING || tipoExpsEsq.referencia == TipoEnum.BOOLEAN)
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());
                    CASATOKEN(AlfabetoEnum.DIFERENTE);
                    break;

                case MENOR:
                    if (tipoExpsEsq.referencia == TipoEnum.STRING || tipoExpsEsq.referencia == TipoEnum.BOOLEAN)
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());
                    CASATOKEN(AlfabetoEnum.MENOR);
                    break;

                case MAIOR:
                    if (tipoExpsEsq.referencia == TipoEnum.STRING || tipoExpsEsq.referencia == TipoEnum.BOOLEAN)
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());
                    CASATOKEN(AlfabetoEnum.MAIOR);
                    break;

                case MENOR_IGUAL:
                    if (tipoExpsEsq.referencia == TipoEnum.STRING || tipoExpsEsq.referencia == TipoEnum.BOOLEAN)
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());
                    CASATOKEN(AlfabetoEnum.MENOR_IGUAL);
                    break;

                /*
                 * MAIOR_IGUAL
                 */
                default:
                    if (tipoExpsEsq.referencia == TipoEnum.STRING || tipoExpsEsq.referencia == TipoEnum.BOOLEAN)
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());
                    CASATOKEN(AlfabetoEnum.MAIOR_IGUAL);
                    break;
            }

            EXP(tipoExpsDir, tamanho, endereco);

            if(tipoExpsEsq.referencia == TipoEnum.STRING || tipoExpsEsq.referencia == TipoEnum.CHAR){
                if (tipoExpsEsq.referencia != tipoExpsDir.referencia)
                    throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());
            } else if(tipoExpsEsq.referencia == TipoEnum.INTEGER || tipoExpsEsq.referencia == TipoEnum.REAL){

                if (tipoExpsDir.referencia != TipoEnum.INTEGER && tipoExpsDir.referencia != TipoEnum.REAL)
                    throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());
            }

            if(tipoExpsDir.referencia == TipoEnum.STRING){
                String rotuloLoop = geraRotulo();
                rotuloFalso = geraRotulo();
                rotuloFim = geraRotulo();
                TPCompiladores.assembly += "; Inicio comparação de strings\n";
                TPCompiladores.assembly += "\tmov EDX, 1\n";
                TPCompiladores.assembly += "\tmov RSI, M+" + tempExp + " ; Salva endereço de EXP1\n";
                TPCompiladores.assembly += "\tmov RDI, M+" + enderecoParaHexa(endereco.referencia)
                        + " ; Salva endereço de EXP2\n";
                TPCompiladores.assembly += rotuloLoop + ":\n";
                TPCompiladores.assembly += "\tmov AL, [RSI] ; Carrega 1 caractere de EXP1\n";
                TPCompiladores.assembly += "\tmov BL, [RDI] ; Carrega 1 caractere de EXP2\n";
                TPCompiladores.assembly += "\tcmp AL, BL ; Realiza a comparação do caractere AL com BL\n";
                TPCompiladores.assembly += "\tjne " + rotuloFalso + " ; Se caracteres diferentes jump\n";
                TPCompiladores.assembly += "\tcmp AL, 0 ; Verifica se EXP1 acabou\n";
                TPCompiladores.assembly += "\tje " + rotuloFim + " ; Se EXP1 acabou jump\n";
                TPCompiladores.assembly += "\tadd RSI, 1 ; Anda a posição do endereço EXP1\n";
                TPCompiladores.assembly += "\tadd RDI, 1 ; Anda a posição do endereço EXP2\n";
                TPCompiladores.assembly += "\tjmp " + rotuloLoop + "\n";
                TPCompiladores.assembly += rotuloFalso + ": ; Caracteres diferentes\n";
                TPCompiladores.assembly += "\tmov EDX, 0 ; Resultado da comp é falso\n";
                TPCompiladores.assembly += rotuloFim + ": ; Fim\n";

                // Aloca 4 bytes por conta do booleano
                endereco.referencia = novoEnderecoTemporarios(4);

                TPCompiladores.assembly += "\tmov [M+" + enderecoParaHexa(endereco.referencia)
                        + "], EDX ; Coloca no temporario se eh true ou false\n";
                TPCompiladores.assembly += "; Fim comparação de strings\n";

            }

            else {
                if (tipoExpsDir.referencia == TipoEnum.CHAR) {
                    TPCompiladores.assembly += "\tmov EAX, 0 ; Limpa registrador EAX\n";
                    TPCompiladores.assembly += "\tmov AL, [M+" + tempExp + "] ; Moveu o caractere da EXP1 para AL\n";
                    TPCompiladores.assembly += "\tmov EBX, 0 ; Limpa registrador EBX\n";
                    TPCompiladores.assembly += "\tmov BL, [M+" + enderecoParaHexa(endereco.referencia)
                            + "] ; Moveu o caractere da EXP2 para BL\n";
                    TPCompiladores.assembly += "\tcmp AL, BL ; Realiza a comparacao do conteudo de EAX com EBX\n";
                } else if (tipoExpsEsq.referencia == TipoEnum.INTEGER && tipoExpsDir.referencia == TipoEnum.INTEGER) {
                    TPCompiladores.assembly += "\tmov EAX, [M+" + tempExp + "] ; Moveu a EXP1(int) para EAX\n";
                    TPCompiladores.assembly += "\tmov EBX, [M+" + enderecoParaHexa(endereco.referencia)
                            + "] ; Moveu a EXP2(int) para EBX\n";
                    TPCompiladores.assembly += "\tcmp EAX, EBX ; Realiza a comparacao do conteudo de EAX com EBX\n";
                } else {
                    if (tipoExpsEsq.referencia == TipoEnum.INTEGER && tipoExpsDir.referencia == TipoEnum.REAL) {
                        TPCompiladores.assembly += "\tmov EAX, [M+" + tempExp + "] ; Moveu a EXP1 para EAX\n";
                        TPCompiladores.assembly += "\tcvtsi2ss XMM0, EAX ; Numero convertido em float\n";
                        TPCompiladores.assembly += "\tmovss XMM1, [M+" + enderecoParaHexa(endereco.referencia)
                                + "] ; Moveu a EXP2 para XMM1\n";
                        TPCompiladores.assembly += "\tcomiss XMM0, XMM1 ; Realiza a comparacao do conteudo de XMM0 com XMM1\n";
                    } else if (tipoExpsEsq.referencia == TipoEnum.REAL && tipoExpsDir.referencia == TipoEnum.INTEGER) {
                        TPCompiladores.assembly += "\tmovss XMM0, [M+" + tempExp + "] ; Moveu EXP1 para XMM0\n";
                        TPCompiladores.assembly += "\tmov EAX, [M+" + enderecoParaHexa(endereco.referencia)
                                + "] ; Moveu a EXP2 para RAX\n";
                        TPCompiladores.assembly += "\tcvtsi2ss XMM1, EAX ; Converteu o numero de RAX em float\n";
                        TPCompiladores.assembly += "\tcomiss XMM0, XMM1 ; Realiza a comparacao do conteudo de XMM0 com XMM1\n";
                    } else { // Ambos Float
                        TPCompiladores.assembly += "\tmovss XMM0, [M+" + tempExp + "] ; Moveu EXP1 para XMM0\n";
                        TPCompiladores.assembly += "\tmovss XMM1, [M+" + enderecoParaHexa(endereco.referencia)
                                + "] ; Moveu a EXP2 para XMM1\n";
                        TPCompiladores.assembly += "\tcomiss XMM0, XMM1 ; Realiza a comparacao do conteudo de XMM0 com XMM1\n";
                    }
                    comparacaoReal = true;
                }

                rotuloVerdadeiro = geraRotulo();
                rotuloFim = geraRotulo();

                switch (operador){
                    case MAIOR:
                        if(comparacaoReal)
                            TPCompiladores.assembly += "\tja " + rotuloVerdadeiro + "\n";
                        else
                            TPCompiladores.assembly += "\tjg " + rotuloVerdadeiro + "\n";
                        break;

                    case MAIOR_IGUAL:
                        if(comparacaoReal)
                            TPCompiladores.assembly += "\tjae " + rotuloVerdadeiro + "\n";
                        else
                            TPCompiladores.assembly += "\tjge " + rotuloVerdadeiro + "\n";
                        break;

                    case MENOR:
                        if(comparacaoReal)
                            TPCompiladores.assembly += "\tjb " + rotuloVerdadeiro + "\n";
                        else
                            TPCompiladores.assembly += "\tjl " + rotuloVerdadeiro + "\n";
                        break;

                    case MENOR_IGUAL:
                        if(comparacaoReal)
                            TPCompiladores.assembly += "\tjbe " + rotuloVerdadeiro + "\n";
                        else
                            TPCompiladores.assembly += "\tjle " + rotuloVerdadeiro + "\n";
                        break;

                    case DIFERENTE:
                        TPCompiladores.assembly += "\tjne " + rotuloVerdadeiro + "\n";
                        break;

                    default:
                        TPCompiladores.assembly += "\tje " + rotuloVerdadeiro + "\n";
                        break;
                }

                endereco.referencia = novoEnderecoTemporarios(4);

                TPCompiladores.assembly += "\tmov EAX, 0 ; Caso seja falso\n";
                TPCompiladores.assembly += "\tjmp " + rotuloFim + "\n";
                TPCompiladores.assembly += rotuloVerdadeiro + ":\n";
                TPCompiladores.assembly += "\tmov EAX, 1 ; Caso seja verdadeiro\n";
                TPCompiladores.assembly += rotuloFim + ":\n";
                TPCompiladores.assembly += "\tmov [M+" + enderecoParaHexa(endereco.referencia)
                        + "], EAX ; Coloca no temporario se eh true ou false\n";
                TPCompiladores.assembly += "; Fim exp\n";
            }
            tipo.referencia = TipoEnum.BOOLEAN;
            tamanho.referencia = 4;

        }
    }

    //SEXP -> [+ | -] T {(+ | - | OR) T}
    public void SEXP(Referencia<TipoEnum> tipo, Referencia<Integer> tamanho, Referencia<Long> endereco) throws ErroPersonalizado, IOException{
        boolean ehNumero = false;
        boolean negacao = false;

        if(tokenLido.getTipoToken() == AlfabetoEnum.MENOS){
            CASATOKEN(AlfabetoEnum.MENOS);
            ehNumero = true;
            negacao = true;
        }else if(tokenLido.getTipoToken() == AlfabetoEnum.MAIS){
            CASATOKEN(AlfabetoEnum.MAIS);
            ehNumero = true;
        }

        T(tipo, tamanho, endereco);
        long tempExps = endereco.referencia;
        long enderecoTemp;

        if (ehNumero && tipo.referencia != TipoEnum.INTEGER && tipo.referencia != TipoEnum.REAL)
            throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());

        if (negacao) {
            enderecoTemp = novoEnderecoTemporarios(4);
            if (tipo.referencia == TipoEnum.INTEGER) {
                TPCompiladores.assembly += "; Nega o primeiro número\n";
                TPCompiladores.assembly += "\tmov EAX, [M+" + enderecoParaHexa(tempExps) + "] ; Moveu a EXPS1 para EAX\n";
                TPCompiladores.assembly += "\tneg EAX ; Transforma EAX em negativo\n";
                TPCompiladores.assembly += "\tmov [M+" + enderecoParaHexa(enderecoTemp)
                        + "], EAX ; Carrega EAX para end temporario\n";
            } else {
                TPCompiladores.assembly += "; Nega o primeiro número\n";
                TPCompiladores.assembly += "\tmovss XMM0, [M+" + enderecoParaHexa(tempExps)
                        + "] ; Moveu a EXPS1 para EAX\n";
                TPCompiladores.assembly += "\tmov EAX, -1 ; Coloca -1 em EAX\n";
                TPCompiladores.assembly += "\tcvtsi2ss XMM1, EAX ; Converte o -1 para float\n";
                TPCompiladores.assembly += "\tmulss XMM0, XMM1 ; Tranforma XMM0 em negativo\n";
                TPCompiladores.assembly += "\tmovss [M+" + enderecoParaHexa(enderecoTemp)
                        + "], XMM0 ; Carrega EAX para end temporario\n";
            }

            tempExps = enderecoTemp;
        }

        Referencia<TipoEnum> tipoExpDir = new Referencia<>(TipoEnum.NULL);
        while(verificaEXPS()){
            enderecoTemp = novoEnderecoTemporarios(4);
            switch (tokenLido.getTipoToken()){
                case MAIS:
                    if (tipo.referencia != TipoEnum.INTEGER && tipo.referencia != TipoEnum.REAL)
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());
                    CASATOKEN(AlfabetoEnum.MAIS);
                    T(tipoExpDir, tamanho, endereco);

                    if (tipo.referencia != tipoExpDir.referencia
                            && (tipo.referencia != TipoEnum.INTEGER || tipoExpDir.referencia != TipoEnum.REAL)
                            && (tipo.referencia != TipoEnum.REAL || tipoExpDir.referencia != TipoEnum.INTEGER))
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());

                    geraAssemblyMaisMenos(tipo.referencia, tipoExpDir.referencia, tempExps, endereco.referencia, true,
                            enderecoTemp);

                    if (tipoExpDir.referencia == TipoEnum.REAL)
                        tipo.referencia = TipoEnum.REAL;

                    break;

                case MENOS:
                    if (tipo.referencia != TipoEnum.INTEGER && tipo.referencia != TipoEnum.REAL)
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());
                    CASATOKEN(AlfabetoEnum.MENOS);
                    T(tipoExpDir, tamanho, endereco);

                    if (tipo.referencia != tipoExpDir.referencia
                            && (tipo.referencia != TipoEnum.INTEGER || tipoExpDir.referencia != TipoEnum.REAL)
                            && (tipo.referencia != TipoEnum.REAL || tipoExpDir.referencia != TipoEnum.INTEGER))
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());

                    geraAssemblyMaisMenos(tipo.referencia, tipoExpDir.referencia, tempExps, endereco.referencia, true,
                            enderecoTemp);

                    if (tipoExpDir.referencia == TipoEnum.REAL)
                        tipo.referencia = TipoEnum.REAL;

                    break;

                default:
                    if (tipo.referencia != TipoEnum.BOOLEAN)
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());

                    CASATOKEN(AlfabetoEnum.OR);
                    T(tipoExpDir, tamanho, endereco);

                    if (tipoExpDir.referencia != TipoEnum.BOOLEAN)
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());

                    TPCompiladores.assembly += "; Operação OR\n";
                    TPCompiladores.assembly += "\tmov AL, [M+" + enderecoParaHexa(tempExps) + "] ; Move T1 para AL\n";
                    TPCompiladores.assembly += "\tmov BL, [M+" + enderecoParaHexa(endereco.referencia) + "] ; Move T2 para BL\n\n";

                    TPCompiladores.assembly += "; NOT A\n";
                    TPCompiladores.assembly += "\tneg AL\n";
                    TPCompiladores.assembly += "\tadd AL, 1\n\n";

                    TPCompiladores.assembly += "; NOT B\n";
                    TPCompiladores.assembly += "\tneg BL\n";
                    TPCompiladores.assembly += "\tadd BL, 1\n\n";

                    TPCompiladores.assembly += "\timul BL ; (NOT A) AND (NOT B)\n\n";

                    TPCompiladores.assembly += "; NOT ((NOT A) AND (NOT B))\n";
                    TPCompiladores.assembly += "\tneg AL\n";
                    TPCompiladores.assembly += "\tadd AL, 1\n";

                    TPCompiladores.assembly += "\tmov [M+" + enderecoParaHexa(enderecoTemp)
                            + "], AL ; Coloca no temporario resultado do OR\n";

                    break;
            }
            tempExps = enderecoTemp;
        }
        endereco.referencia = tempExps;
    }

    //T -> F {(* | AND | / | // | %) F}
    public void T(Referencia<TipoEnum> tipo, Referencia<Integer> tamanho, Referencia<Long> endereco) throws ErroPersonalizado, IOException{

        F(tipo, tamanho, endereco);
        long tempExps = endereco.referencia;
        long enderecoTemp;

        Referencia<TipoEnum> tipoExpDir = new Referencia<>(TipoEnum.NULL);

        while(tokenLido.getTipoToken() == AlfabetoEnum.MULTIPLICACAO || tokenLido.getTipoToken() == AlfabetoEnum.AND || tokenLido.getTipoToken() == AlfabetoEnum.DIVISAO
                || tokenLido.getTipoToken() == AlfabetoEnum.BARRA_BARRA || tokenLido.getTipoToken() == AlfabetoEnum.PORCENTAGEM){

            enderecoTemp = novoEnderecoTemporarios(4);

            switch (tokenLido.getTipoToken()){
                case MULTIPLICACAO:

                    if (tipo.referencia != TipoEnum.INTEGER && tipo.referencia != TipoEnum.REAL)
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());

                    CASATOKEN(AlfabetoEnum.MULTIPLICACAO);
                    F(tipoExpDir, tamanho, endereco);

                    if (tipo.referencia != tipoExpDir.referencia
                            && (tipo.referencia != TipoEnum.INTEGER || tipoExpDir.referencia != TipoEnum.REAL)
                            && (tipo.referencia != TipoEnum.REAL || tipoExpDir.referencia != TipoEnum.INTEGER))
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());

                    geraAssemblyMultDivReal(tipo.referencia, tipoExpDir.referencia, tempExps, endereco.referencia, true,
                            enderecoTemp);

                    if (tipoExpDir.referencia == TipoEnum.REAL)
                        tipo.referencia = TipoEnum.REAL;

                    break;

                case AND:
                    if (tipo.referencia != TipoEnum.BOOLEAN)
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());
                    CASATOKEN(AlfabetoEnum.AND);

                    F(tipoExpDir, tamanho, endereco);

                    TPCompiladores.assembly += "; AND\n";
                    TPCompiladores.assembly += "\tmov EAX, [M+" + enderecoParaHexa(tempExps) + "] ; Moveu a F1 para EAX\n";
                    TPCompiladores.assembly += "\tmov EBX, [M+" + enderecoParaHexa(endereco.referencia) + "] ; Moveu a F2 para EBX\n";
                    TPCompiladores.assembly += "\timul EBX ; Realiza EAX AND EBX\n";
                    TPCompiladores.assembly += "\tmov [M+" + enderecoParaHexa(enderecoTemp) + "], EAX ; Coloca no temporario resultado da multiplicação\n";

                    if (tipoExpDir.referencia != TipoEnum.BOOLEAN)
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());

                    break;

                case DIVISAO:
                    if (tipo.referencia != TipoEnum.INTEGER && tipo.referencia != TipoEnum.REAL)
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());

                    CASATOKEN(AlfabetoEnum.DIVISAO);
                    F(tipoExpDir, tamanho, endereco);

                    if (tipo.referencia != tipoExpDir.referencia
                            && (tipo.referencia != TipoEnum.INTEGER || tipoExpDir.referencia != TipoEnum.REAL)
                            && (tipo.referencia != TipoEnum.REAL || tipoExpDir.referencia != TipoEnum.INTEGER))
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());

                    geraAssemblyMultDivReal(tipo.referencia, tipoExpDir.referencia, tempExps, endereco.referencia, false,
                            enderecoTemp);

                    if (tipoExpDir.referencia == TipoEnum.REAL)
                        tipo.referencia = TipoEnum.REAL;

                    if (tipo.referencia == TipoEnum.INTEGER && tipoExpDir.referencia == TipoEnum.INTEGER)
                        tipo.referencia = TipoEnum.REAL;

                    break;

                case BARRA_BARRA:

                    if (tipo.referencia != TipoEnum.INTEGER)
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());

                    CASATOKEN(AlfabetoEnum.BARRA_BARRA);
                    F(tipoExpDir, tamanho, endereco);

                    geraAssemblyDivMod(tempExps, endereco.referencia, true, enderecoTemp);

                    if (tipo.referencia != tipoExpDir.referencia)
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());
                    break;
                /*
                 * %
                 */
                default:
                    if (tipo.referencia != TipoEnum.INTEGER)
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());

                    CASATOKEN(AlfabetoEnum.PORCENTAGEM);
                    F(tipoExpDir, tamanho, endereco);

                    geraAssemblyDivMod(tempExps, endereco.referencia, false, enderecoTemp);

                    if (tipo.referencia != tipoExpDir.referencia)
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());

                    break;
            }

            tempExps = enderecoTemp;

        }
        endereco.referencia = tempExps;
    }

    //F -> NOT F | INTEGER '(' EXP ')' | REAL '(' EXP ')' | '(' EXP ')' | ID ['[' EXP ']'] | VALOR
    public void F(Referencia<TipoEnum> tipo, Referencia<Integer> tamanho, Referencia<Long> endereco) throws ErroPersonalizado, IOException{
        boolean conversao = false;
        Referencia<TipoEnum> tipoExpDir = new Referencia<>(TipoEnum.NULL);
        Token identificador;
        Token valor;
        Referencia<Integer> tamanhoExp = new Referencia<>(0);
        Referencia<Long> enderecoExp = new Referencia<>(0L);

        switch (tokenLido.getTipoToken()){
            case IDENTIFICADOR:
                if (tokenLido.getSimbolo().getTipoClasse() == null)
                    throw new ErroIdentificadorNaoDeclarado(TPCompiladores.getLinhaPrograma(), tokenLido.getLexema());

                identificador = tokenLido;

                tipo.referencia = identificador.getSimbolo().getTipoDados();
                tamanho.referencia = identificador.getSimbolo().getTamanho();
                endereco.referencia = identificador.getSimbolo().getEndereco();

                CASATOKEN(AlfabetoEnum.IDENTIFICADOR);

                if(tokenLido.getTipoToken() == AlfabetoEnum.ABRE_COLCHETE){
                    if (identificador.getSimbolo().getTipoDados() != TipoEnum.STRING)
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());

                    CASATOKEN(AlfabetoEnum.ABRE_COLCHETE);

                    TPCompiladores.assembly += "; Inicio do calculo da posicao do vetor: " + identificador.getLexema() + "\n";

                    EXP(tipoExpDir, tamanhoExp, enderecoExp);

                    TPCompiladores.assembly += "\n; Fim do calculo da posicao do vetor: " + identificador.getLexema() + "\n";

                    if (tipoExpDir.referencia != TipoEnum.INTEGER)
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());

                    long temporario = novoEnderecoTemporarios(tamanhoExp.referencia);
                    TPCompiladores.assembly += "\tmov EBX, [M+" + enderecoParaHexa(enderecoExp.referencia)
                            + "] ; Recupera o valor da posicao do vetor\n";
                    TPCompiladores.assembly += "\tadd EBX, M+" + enderecoParaHexa(endereco.referencia) + " ; Carrega o endereçon para EBX\n";
                    TPCompiladores.assembly += "\tmov EAX, [EBX] ; Carrega o valor de EBX para EAX\n";
                    TPCompiladores.assembly += "\tmov [M+" + enderecoParaHexa(temporario) + "], EAX ; Carrega o EAX para a memória\n";

                    CASATOKEN(AlfabetoEnum.FECHA_COLCHETE);
                    tipo.referencia = TipoEnum.CHAR;
                    tamanho.referencia = 1;
                    endereco.referencia = temporario;
                }
                break;

            case NOT:
                CASATOKEN(AlfabetoEnum.NOT);
                F(tipo, tamanho, endereco);

                if (tipo.referencia != TipoEnum.BOOLEAN)
                    throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());

                TPCompiladores.assembly += "; Nega um fator\n";
                TPCompiladores.assembly += "\tmov EBX, [M+" + enderecoParaHexa(endereco.referencia) + "] ; Move conteudo de F para EBX\n";
                TPCompiladores.assembly += "\tneg EBX ; Multiplica EBX por -1\n";
                TPCompiladores.assembly += "\tadd EBX, 1 ; Soma 1\n";
                endereco.referencia = novoEnderecoTemporarios(4);
                TPCompiladores.assembly += "\tmov [M+" + enderecoParaHexa(endereco.referencia) + "], EBX ; Carrega EBX para end. temporario\n";
                break;

            case VALOR:
                valor = tokenLido;
                tipo.referencia = tokenLido.getTipoConstante();
                tamanho.referencia = tokenLido.getTamanhoConstante();

                CASATOKEN(AlfabetoEnum.VALOR);

                if (tipo.referencia == TipoEnum.STRING || tipo.referencia == TipoEnum.REAL) {
                    endereco.referencia = aloca_bytes(tamanho.referencia);
                    if (tipo.referencia == TipoEnum.STRING) {
                        TPCompiladores.assembly += "section .data\n";
                        TPCompiladores.assembly += "\tdb " + valor.getLexema() + " ; Declaração da string\n";
                        TPCompiladores.assembly += "\tdb 0 ; Declaração do fim da string\n";
                        TPCompiladores.assembly += "section .text\n";
                    } else if (tipo.referencia == TipoEnum.REAL) {
                        TPCompiladores.assembly += "section .data\n";
                        if (valor.getLexema().charAt(0) == '.')
                            TPCompiladores.assembly += "\tdd 0" + valor.getLexema() + " ; Declaração do float\n";
                        else
                            TPCompiladores.assembly += "\tdd " + valor.getLexema() + " ; Declaração do float\n";

                        TPCompiladores.assembly += "section .text\n";
                    }
                } else {
                    endereco.referencia = novoEnderecoTemporarios(valor.getTamanhoConstante());
                    if (tipo.referencia == TipoEnum.INTEGER) {
                        // Int e bool são 4 bytes => EBX tem 32 bits = 4 bytes
                        TPCompiladores.assembly += "\tmov EBX, " + valor.getLexema() + " ; Coloca o inteiro no EBX\n";
                        TPCompiladores.assembly += "\tmov [M+" + enderecoParaHexa(endereco.referencia) + "], EBX ; Carrega para a memória o inteiro\n";
                    } else if (tipo.referencia == TipoEnum.CHAR) {
                        // BL só tem 8 bits = 1 byte
                        // Precisa movimentar somente 1 byte
                        TPCompiladores.assembly += "\tmov BL, " + valor.getLexema() + " ; Coloca o carectere no BL\n";
                        TPCompiladores.assembly += "\tmov [M+" + enderecoParaHexa(endereco.referencia) + "], BL ; Carrega para a memória o caractere\n";
                    } else if (tipo.referencia == TipoEnum.BOOLEAN) {
                        // BL só tem 8 bits = 1 byte
                        // Precisa movimentar somente 1 byte
                        TPCompiladores.assembly += "\tmov BL, " + (valor.getLexema().contentEquals("TRUE") ? 1 : 0) + " ; Coloca o boolean no BL\n";
                        TPCompiladores.assembly += "\tmov [M+" + enderecoParaHexa(endereco.referencia) + "], BL ; Carrega para a memória o boolean\n";
               }
                }
                break;

            default:
                if (tokenLido.getTipoToken() == AlfabetoEnum.INTEGER) {
                    // Define o tipo retornado como int
                    tipo.referencia = TipoEnum.INTEGER;
                    conversao = true;
                    CASATOKEN(AlfabetoEnum.INTEGER);
                } else if (tokenLido.getTipoToken() == AlfabetoEnum.REAL) {
                    // Define o tipo retornado como float
                    tipo.referencia = TipoEnum.REAL;
                    conversao = true;
                    CASATOKEN(AlfabetoEnum.REAL);
                }

                CASATOKEN(AlfabetoEnum.ABRE_PARENTESES);
                EXP(tipoExpDir, tamanho, endereco);
                CASATOKEN(AlfabetoEnum.FECHA_PARENTESES);

                if(conversao){
                    if (tipoExpDir.referencia != TipoEnum.REAL && tipoExpDir.referencia != TipoEnum.INTEGER)
                        throw new ErroTiposIncompativeis(TPCompiladores.getLinhaPrograma());

                    if (tipo.referencia == TipoEnum.INTEGER && tipoExpDir.referencia == TipoEnum.REAL) {
                        TPCompiladores.assembly += "; Inicio conversao para inteiro\n";
                        TPCompiladores.assembly += "\tsubss XMM0, XMM0 ; Zera o XMM0\n";
                        TPCompiladores.assembly += "\tmovss XMM0, [M+" + enderecoParaHexa(endereco.referencia) + "] ; Carrega o valor para XMM0\n";
                        TPCompiladores.assembly += "\tmov RAX, 0 ; Zera o RAX\n";
                        TPCompiladores.assembly += "\tcvtss2si RAX, XMM0 ; Converte o real para inteiro\n";
                        endereco.referencia = novoEnderecoTemporarios(4);
                        TPCompiladores.assembly += "\tmov [M+" + enderecoParaHexa(endereco.referencia) + "], EAX ; Carrega para temporario o valor convertido\n";
                        TPCompiladores.assembly += "; Fim conversao para inteiro\n";
                    } else if (tipo.referencia == TipoEnum.REAL && tipoExpDir.referencia == TipoEnum.INTEGER){ // Conversão de real
                        TPCompiladores.assembly += "; Inicio conversao para real\n";
                        TPCompiladores.assembly += "\tmov RAX, 0 ; Zera o RAX\n";
                        TPCompiladores.assembly += "\tmov RAX, [M+" + enderecoParaHexa(endereco.referencia) + "] ; Carrega o valor para a RAX\n";
                        TPCompiladores.assembly += "\tsubss XMM0, XMM0 ; Zera o XMM0\n";
                        TPCompiladores.assembly += "\tcvtsi2ss XMM0, RAX ; Converte inteiro para real\n";
                        endereco.referencia = novoEnderecoTemporarios(4);
                        TPCompiladores.assembly += "\tmovss [M+" + enderecoParaHexa(endereco.referencia) + "], XMM0 ; Cerrerga para temporario o valor convertido\n";
                        TPCompiladores.assembly += "; Fim conversao para real\n";
                    }


                } else {
                    tipo.referencia = tipoExpDir.referencia;
                }
            break;
        }
    }

    private void geraAssemblyWrite(Referencia<TipoEnum> tipoExp, Referencia<Long> endereco, Referencia<Integer> tamanho) {
        String rotulo;
        String buffer;
        if (tipoExp.referencia == TipoEnum.STRING) {
            TPCompiladores.assembly += "; Impressão String\n";
            TPCompiladores.assembly += "; Calcula o tamanho da string\n";
            TPCompiladores.assembly += "\tmov RSI, M+" + enderecoParaHexa(endereco.referencia)
                    + " ; Move o endereço da string para o registrador fonte (source)\n";
            TPCompiladores.assembly += "\tmov RDX, RSI ; Recupera o endereço inicial da string\n";
            rotulo = geraRotulo();
            TPCompiladores.assembly += rotulo + ": ; Loop para o calculo do tamanho\n";
            TPCompiladores.assembly += "\tmov AL, [RDX] ; Le o caractere atual\n";
            TPCompiladores.assembly += "\tadd RDX, 1 ; Incrementa o ponteiro da string\n";
            TPCompiladores.assembly += "\tcmp AL, 0 ; Verifica se o caractere lido eh o byte 0 (fim de string)\n";
            TPCompiladores.assembly += "\tjne " + rotulo + " ; Caso nao seja, continua o loop\n\n";

            TPCompiladores.assembly += "\tsub RDX, RSI ; Subtrai o endereco inicial da string pelo endereço final, o registrador RDX sera utilizado pela chamada de sistema como o tamanho da string\n";
            TPCompiladores.assembly += "\tsub RDX, 1 ; Desconsidera o byte nulo ao final da string\n\n";

            TPCompiladores.assembly += "; Escreve a string para a saída padrão\n";
            TPCompiladores.assembly += "\tmov RSI, M+" + enderecoParaHexa(endereco.referencia)
                    + " ; Move o endereço da string para o registrador fonte (source)\n";
            TPCompiladores.assembly += "\tmov RAX, 1 ; Chamada de escrita\n";
            TPCompiladores.assembly += "\tmov RDI, 1 ; Escrever para a saida padrao\n";
            TPCompiladores.assembly += "\tsyscall ; Chama o kernel\n";
        } else if (tipoExp.referencia == TipoEnum.CHAR) {
            TPCompiladores.assembly += "; Impressão Char\n";
            TPCompiladores.assembly += "; Escreve o char para a saída padrão\n";
            TPCompiladores.assembly += "\tmov RDX, 1\n";
            TPCompiladores.assembly += "\tmov RAX, 1 ; Chamada de escrita\n";
            TPCompiladores.assembly += "\tmov RDI, 1 ; Escrever para saida padrão\n";
            TPCompiladores.assembly += "\tmov RSI, M+" + enderecoParaHexa(endereco.referencia)
                    + "; Move o endereço do char para o registrador fonte (source)\n";
            TPCompiladores.assembly += "\tsyscall ; Chama o kernel\n\n";

        } else if (tipoExp.referencia == TipoEnum.REAL) {
            rotulo = geraRotulo();
            String rotulo1, rotulo2, rotulo3, rotulo4;
            buffer = enderecoParaHexa(novoEnderecoTemporarios(4));
            TPCompiladores.assembly += "; Impressão REAL\n";
            TPCompiladores.assembly += "\tmovss XMM0, [M+" + enderecoParaHexa(endereco.referencia) + "] ; Real a ser convertido\n";
            TPCompiladores.assembly += "\tmov RSI, M+" + buffer + " ; Endereço Temporário\n";
            TPCompiladores.assembly += "\tmov RCX, 0 ; Contador pilha\n";
            TPCompiladores.assembly += "\tmov RDI, 6 ; Precisão 6 casa compartilhadas\n";
            TPCompiladores.assembly += "\tmov RBX, 10 ; Divisor\n";
            TPCompiladores.assembly += "\tcvtsi2ss XMM2, RBX ; Divisor real\n";
            TPCompiladores.assembly += "\tsubss XMM1, XMM1 ; Zera registrador\n";
            TPCompiladores.assembly += "\tcomiss XMM0, XMM1 ; Verifica sinal\n";
            TPCompiladores.assembly += "\tjae " + rotulo + " ; Salta se número é positivo\n";
            TPCompiladores.assembly += "\tmov DL, '-' ; Senão, escreve sinal -\n";
            TPCompiladores.assembly += "\tmov [RSI], DL ; Carrega na memória o sinal -\n";
            TPCompiladores.assembly += "\tmov RDX, -1 ; Carrega -1 em RDX\n";
            TPCompiladores.assembly += "\tcvtsi2ss XMM1, RDX ; Converte -1 para real\n";
            TPCompiladores.assembly += "\tmulss XMM0, XMM1 ; Tranforma o valor em positivo\n";
            TPCompiladores.assembly += "\tadd RSI, 1 ; Incrementa o ponteiro\n\n";

            TPCompiladores.assembly += rotulo + ":\n";
            TPCompiladores.assembly += "\troundss XMM1, XMM0, 0b0011 ; Parte inteira de XMM1\n";
            TPCompiladores.assembly += "\tsubss XMM0, XMM1 ; Parte fracionária de XMM0\n";
            TPCompiladores.assembly += "\tcvtss2si RAX, XMM1 ; Convertido para int\n";


            rotulo1 = geraRotulo();
            TPCompiladores.assembly += "; Converte parte inteira que está em RAX\n";
            TPCompiladores.assembly += rotulo1 + ":\n";
            TPCompiladores.assembly += "\tadd RCX, 1 ; Incrementa contador\n";
            TPCompiladores.assembly += "\tcdq ; Estende edx:eax para divisão\n";
            TPCompiladores.assembly += "\tidiv EBX ; Divide edx;eax por ebx\n";
            TPCompiladores.assembly += "\tpush DX ; Empilha valor do resto\n";
            TPCompiladores.assembly += "\tcmp EAX, 0 ; Compara se quociente é 0\n";
            TPCompiladores.assembly += "\tjne " + rotulo1 + " ; Se não é 0, continua\n";
            TPCompiladores.assembly += "\tsub RDI, RCX ; Decrementa precisão\n\n";

            rotulo2 = geraRotulo();
            TPCompiladores.assembly += "; Agora, desempilha valores e escreve parte inteira\n";
            TPCompiladores.assembly += rotulo2 + ":\n";
            TPCompiladores.assembly += "\tpop DX ; Desempilha valor\n";
            TPCompiladores.assembly += "\tadd DL, '0' ; Transforma em caractere\n";
            TPCompiladores.assembly += "\tmov [RSI], DL ; Escreve caractere\n";
            TPCompiladores.assembly += "\tadd RSI, 1 ; Incrementa base\n";
            TPCompiladores.assembly += "\tsub RCX, 1 ; Decrementa contador\n";
            TPCompiladores.assembly += "\tcmp RCX, 0 ; Verifica pilha vazia\n";
            TPCompiladores.assembly += "\tjne " + rotulo2 + " ; Se não estiver vazia, loop\n";

            TPCompiladores.assembly += "\tmov DL, '.' ; Escreve ponto decimal\n";
            TPCompiladores.assembly += "\tmov [RSI], DL ; Armazena ponto\n";
            TPCompiladores.assembly += "\tadd RSI, 1 ; Incrementa base\n\n";


            rotulo3 = geraRotulo();
            rotulo4 = geraRotulo();
            TPCompiladores.assembly += "; Converte para fracionária que está em XMM0\n";
            TPCompiladores.assembly += rotulo3 + ":\n";
            TPCompiladores.assembly += "\tcmp RDI, 0 ; Verifica precisão\n";
            TPCompiladores.assembly += "\tjle " + rotulo4 + " ; Terminou precisão ?\n";
            TPCompiladores.assembly += "\tmulss XMM0, XMM2 ; Desloca para esquerda\n";
            TPCompiladores.assembly += "\troundss XMM1, XMM0, 0b0011 ; Parte inteira XMM1\n";
            TPCompiladores.assembly += "\tsubss XMM0, XMM1 ; Atualiza XMM0\n";
            TPCompiladores.assembly += "\tcvtss2si RDX, XMM1 ; Convertido para int\n";
            TPCompiladores.assembly += "\tadd DL, '0' ; Transforma em caractere\n";
            TPCompiladores.assembly += "\tmov [RSI], DL ; Escreve caractere\n";
            TPCompiladores.assembly += "\tadd RSI, 1 ; Incrementa ponteiro\n";
            TPCompiladores.assembly += "\tsub RDI, 1 ; Decrementa precisão\n";
            TPCompiladores.assembly += "\tjmp " + rotulo3 + "; Loop\n\n";

            TPCompiladores.assembly += "; Impressão\n";
            TPCompiladores.assembly += rotulo4 + ":\n";
            TPCompiladores.assembly += "\tmov DL, 0 ; Fim string\n";
            TPCompiladores.assembly += "\tmov [RSI], DL ; Escreve caractere\n";
            TPCompiladores.assembly += "\tmov RDX, RSI ; Calcula tamanho da string convertido\n";
            TPCompiladores.assembly += "\tmov RBX, M+" + buffer + " ; Salva endereço do buffer\n";
            TPCompiladores.assembly += "\tsub RDX, RBX ; Tam = RSI - M - Buffer.end\n";
            TPCompiladores.assembly += "\tmov RSI, M+" + buffer + " ; Endereço do buffer\n\n";

            TPCompiladores.assembly += "; Escreve a string para a saída padrão\n";
            TPCompiladores.assembly += "\tmov RAX, 1 ; Chamada de escrita\n";
            TPCompiladores.assembly += "\tmov RDI, 1 ; Escrever para a saída padrão\n";
            TPCompiladores.assembly += "\tsyscall ; Chama o kernel\n";

        } else if (tipoExp.referencia == TipoEnum.INTEGER) {
            rotulo = geraRotulo();
            buffer = enderecoParaHexa(novoEnderecoTemporarios(4));
            TPCompiladores.assembly += "; Impressão Integer\n";
            TPCompiladores.assembly += "\tmov ECX, 0 ; Inicializa contador para 0\n";
            TPCompiladores.assembly += "\tmov EBX, 10 ; Armazena o divisor\n";
            TPCompiladores.assembly += "\tmov EAX, [M+" + enderecoParaHexa(endereco.referencia)
                    + "] ; Carrega o numero, Endereco da Expressao\n";
            TPCompiladores.assembly += "\tmov RDI, M+" + buffer
                    + " ; Carrega o endereço do buffer (primeiro endereço livre no espaço de temporários)\n";
            TPCompiladores.assembly += "\tcmp EAX, -1 ; Verifica se o número é negativo\n";
            TPCompiladores.assembly += "\tjg " + rotulo + " ; Caso não for, começa a conversão\n";
            TPCompiladores.assembly += "\tmov DL, '-' ; Carrega o caractere - no byte baixo do registrador RDX\n";
            TPCompiladores.assembly += "\tmov [RDI], DL ;Armazena o sinal na primeira posição do buffer\n";
            TPCompiladores.assembly += "\tadd RDI, 1 ; Incrementa o ponteiro para a próxima posição disponível\n";
            TPCompiladores.assembly += "\tneg EAX ; Inverte o número, para deixá-lo positivo\n";

            TPCompiladores.assembly += rotulo + ":\n";
            TPCompiladores.assembly += "\tmov EDX, 0 ; Limpa a parte alta do dividendo\n";
            TPCompiladores.assembly += "\tidiv EBX ; Divide RDX:RAX por RBX (contendo o valor 10) e armazena o resultado em RAX e o resto em RDX\n";
            TPCompiladores.assembly += "\tpush DX ; Empurra o resto na pilha (dígito menos significativo)\n";
            TPCompiladores.assembly += "\tadd ECX, 1 ; Incrementa o contador\n";
            TPCompiladores.assembly += "\tcmp EAX, 0 ; Verifica se ainda resta valor para converter\n";
            TPCompiladores.assembly += "\tjne " + rotulo + " ; Continua a conversão caso necessário\n";
            rotulo = geraRotulo();

            TPCompiladores.assembly += rotulo + ":\n";
            TPCompiladores.assembly += "\tpop AX ; Recupera o próximo dígito\n";
            TPCompiladores.assembly += "\tadd AX, '0' ; Adiciona o valor do caractere '0', para obter o valor do caractere do dígito\n";
            TPCompiladores.assembly += "\tmov [RDI], AL ; Escreve o caractere na próxima posição livre do buffer\n";
            TPCompiladores.assembly += "\tadd RDI, 1 ; Incrementa o ponteiro do buffer\n";
            TPCompiladores.assembly += "\tsub ECX, 1 ; Decrementa o contador\n";
            TPCompiladores.assembly += "\tcmp ECX, 0 ; Verifica se ainda resta dígitos na pilha\n";
            TPCompiladores.assembly += "\tjg " + rotulo + " ; Continua a escrita, caso necessário\n\n";
            TPCompiladores.assembly += "\tmov [RDI], byte 0 ; Escreve o marcador de fim de string no buffer\n\n";
            TPCompiladores.assembly += "\tsub RDI, M+" + buffer + " ; Tamanho da string real\n";

            // Printa na tela
            TPCompiladores.assembly += "\tmov RSI, M+" + buffer + " ; ou buffer.end\n";
            TPCompiladores.assembly += "\tmov RDX,RDI ; ou buffer.tam\n";
            TPCompiladores.assembly += "\tmov RAX, 1 ; chamada para saída\n";
            TPCompiladores.assembly += "\tmov RDI, 1 ; saída para tela\n";
            TPCompiladores.assembly += "\tsyscall\n";
        }
        
    }

    private void geraAssemblyRead(Token identificador) {
        String rotulo;
        long buffer;

        TPCompiladores.assembly += "; Leitura\n\n";


        if (identificador.getSimbolo().getTipoDados() == TipoEnum.STRING) {
            rotulo = geraRotulo();
            String rotuloFim = geraRotulo();

            TPCompiladores.assembly += "\tmov RSI, M+" + enderecoParaHexa(identificador.getSimbolo().getEndereco()) + " ; Salva o endereço do buffer\n";
            TPCompiladores.assembly += "\tmov RDX, " + identificador.getSimbolo().getTamanho() + " ; Tamanho do buffer\n";
            TPCompiladores.assembly += "\tmov RAX, 0 ; Chamada para leitura\n";
            TPCompiladores.assembly += "\tmov RDI, 0 ; Leitura do teclado\n";
            TPCompiladores.assembly += "\tsyscall\n\n";

            TPCompiladores.assembly += "; Leitura string\n";
            TPCompiladores.assembly += "\tadd RAX, RSI\n";
            TPCompiladores.assembly += "\tsub RAX, 1\n";
            TPCompiladores.assembly += rotulo + ":\n";
            TPCompiladores.assembly += "\tmov DL, [RSI] ; Move caractere para DL\n";
            TPCompiladores.assembly += "\tcmp DL, 0Ah ; Compara com quebra de linha\n";
            TPCompiladores.assembly += "\tje " + rotuloFim + " ; Se for quebra de linha salta\n";
            TPCompiladores.assembly += "\tcmp RSI, RAX\n";
            TPCompiladores.assembly += "\tje " + rotuloFim + "\n";
            TPCompiladores.assembly += "\tadd RSI, 1 ; Incrementa endereço da String\n";
            TPCompiladores.assembly += "\tjmp " + rotulo + "\n\n";

            TPCompiladores.assembly += rotuloFim + ":\n";
            TPCompiladores.assembly += "\tmov DL, 0 ; Substitui quebra de linha por fim de string\n";
            TPCompiladores.assembly += "\tmov [RSI], DL ; Move fim de string para o identificador\n";

        } else if (identificador.getSimbolo().getTipoDados() == TipoEnum.CHAR) {
            rotulo = geraRotulo();
            String rotuloFim = geraRotulo();

            TPCompiladores.assembly += "; Leitura char\n";
            TPCompiladores.assembly += "\tmov RSI, M+" + enderecoParaHexa(identificador.getSimbolo().getEndereco()) + " ; Salva o endereço do buffer\n";
            TPCompiladores.assembly += "\tmov RDX, " + identificador.getSimbolo().getTamanho() + " ; Tamanho do buffer\n";
            TPCompiladores.assembly += "\tmov RAX, 0 ; Chamada para leitura\n";
            TPCompiladores.assembly += "\tmov RDI, 0 ; Leitura do teclado\n";
            TPCompiladores.assembly += "\tsyscall\n\n";

        } else if (identificador.getSimbolo().getTipoDados() == TipoEnum.REAL) {
            rotulo = geraRotulo();
            String rotulo2, rotulo3;
            buffer = novoEnderecoTemporarios(27);

            TPCompiladores.assembly += "\tmov RSI, M+" + enderecoParaHexa(buffer) + " ; Salva o endereço do buffer\n";
            TPCompiladores.assembly += "\tmov RDX, 27; Tamanho do buffer\n";
            TPCompiladores.assembly += "\tmov RAX, 0 ; Chamada para leitura\n";
            TPCompiladores.assembly += "\tmov RDI, 0 ; Leitura do teclado\n";
            TPCompiladores.assembly += "\tsyscall\n\n";

            TPCompiladores.assembly += "; Leitura REAL\n";
            TPCompiladores.assembly += "\tmov RAX, 0 ; Zerando acumulador da parte inteira\n";
            TPCompiladores.assembly += "\tsubss XMM0, XMM0 ; Zerando acumulador da parte fracionária\n";
            TPCompiladores.assembly += "\tmov RBX, 0 ; Caractere\n";
            TPCompiladores.assembly += "\tmov RCX, 10 ; Base 10\n";
            TPCompiladores.assembly += "\tcvtsi2ss XMM3, RCX ; Convertendo 10 para REAL\n";
            TPCompiladores.assembly += "\tmovss XMM2, XMM3 ; Potência de 10\n";
            TPCompiladores.assembly += "\tmov RDX, 1 ; Armazena o sinal\n";
            TPCompiladores.assembly += "\tmov RSI, M+" + enderecoParaHexa(buffer) + " ; Endereço do Buffer\n";
            TPCompiladores.assembly += "\tmov BL, [RSI] ; Carrega caractere para BL\n";
            TPCompiladores.assembly += "\tcmp BL, '-' ; Sinal é - ?\n";
            TPCompiladores.assembly += "\tjne " + rotulo + " ; Se é diferente de -, salta\n";
            TPCompiladores.assembly += "\tmov RDX, -1 ; Senão, armazena o -\n";
            TPCompiladores.assembly += "\tadd RSI, 1 ; Incrementa o ponteiro da string, por conta do sinal -\n";
            TPCompiladores.assembly += "\tmov BL, [RSI] ; Carrega o caractere para BL\n\n";

            TPCompiladores.assembly += rotulo + ":\n";
            TPCompiladores.assembly += "\tpush RDX ; Empilha sinal\n";
            TPCompiladores.assembly += "\tmov RDX, 0 ; Registrador da multiplicação\n\n";

            rotulo = geraRotulo();
            rotulo2 = geraRotulo();
            rotulo3 = geraRotulo();

            TPCompiladores.assembly += "; Loop Inteiro - Lendo caractere a carectere do número\n";
            TPCompiladores.assembly += rotulo + ":\n";
            TPCompiladores.assembly += "\tcmp BL, 0Ah ; Verifica fim da string\n";
            TPCompiladores.assembly += "\tje " + rotulo2 + " ; Salta se fim da string\n";
            TPCompiladores.assembly += "\tcmp BL, '.' ; Senão verifica ponto\n";
            TPCompiladores.assembly += "\tje " + rotulo3 + " ; Salta se ponto\n";
            TPCompiladores.assembly += "\t; Pega o ECX e arrasta para a esquerda - Shift de base 10\n";
            TPCompiladores.assembly += "\timul ECX ; Multiplica EAX por 10\n";
            TPCompiladores.assembly += "\tsub BL, '0' ; Converte em caractere\n";
            TPCompiladores.assembly += "\tadd EAX, EBX ; Soma valor caractere\n";
            TPCompiladores.assembly += "\tadd RSI, 1 ; Incrementa ponteiro\n";
            TPCompiladores.assembly += "\tmov BL, [RSI] ; Carrega caractere para BL\n";
            TPCompiladores.assembly += "\tjmp " + rotulo + "; Loop\n\n";

            TPCompiladores.assembly += "; Loop Float - Calcula parte fracionária em XMM0\n";
            TPCompiladores.assembly += rotulo3 + ":\n";
            TPCompiladores.assembly += "\tadd RSI, 1 ; Incrementa ponteiro da string, por conta do '.'\n";
            TPCompiladores.assembly += "\tmov BL, [RSI] ; Carrega caractere para BL\n";
            TPCompiladores.assembly += "\tcmp BL, 0Ah ; Verifica fim string\n";
            TPCompiladores.assembly += "\tje " + rotulo2 + "; Salta se fim da string\n";
            TPCompiladores.assembly += "\tsub BL, '0' ; Converte caractere\n";
            TPCompiladores.assembly += "\tcvtsi2ss XMM1, RBX ; Converte o conteudo de RBX para float\n\n";

            TPCompiladores.assembly += "; XMM2 está com o valor 10, dividindo transforma em uma número com casa decimal - Shift para Direita\n";
            TPCompiladores.assembly += "\tdivss XMM1, XMM2 ; Transforma casa decimal\n\n";

            TPCompiladores.assembly += "; Concatena o carectere obtido com o resto do número\n";
            TPCompiladores.assembly += "\taddss XMM0, XMM1 ; Soma acumulador\n\n";

            TPCompiladores.assembly += "; A medida que vai tendo casas decimais o valor de xmm2 deveria aumentar\n";
            TPCompiladores.assembly += "; para acompanhar as casas decimais que estão sendo lidas\n";
            TPCompiladores.assembly += "\tmulss XMM2, XMM3 ; Atualiza potência\n";
            TPCompiladores.assembly += "\tjmp " + rotulo3 + " ; Loop\n\n";

            TPCompiladores.assembly += rotulo2 + ":\n";
            TPCompiladores.assembly += "\tcvtsi2ss XMM1, RAX ; Converte parte inteira para float\n";
            TPCompiladores.assembly += "\taddss XMM0, XMM1 ; Soma parte fracionária\n";
            TPCompiladores.assembly += "\tpop RCX ; Desempilha sinal\n";
            TPCompiladores.assembly += "\tcvtsi2ss XMM1, RCX ; Converte sinal para float\n";
            TPCompiladores.assembly += "\tmulss XMM0, XMM1 ; Multiplica o sinal\n";

            TPCompiladores.assembly += "\tmovss [M+" + enderecoParaHexa(identificador.getSimbolo().getEndereco()) + "], XMM0 ; Carrega o valor para o indentificador\n";

        } else if (identificador.getSimbolo().getTipoDados() == TipoEnum.INTEGER) {
            rotulo = geraRotulo();
            String rotulo2, rotulo3;
            buffer = novoEnderecoTemporarios(27);

            TPCompiladores.assembly += "\tmov RSI, M+" + enderecoParaHexa(buffer) + " ; Salva o endereço do buffer\n";
            TPCompiladores.assembly += "\tmov RDX, 27; Tamanho do buffer\n";
            TPCompiladores.assembly += "\tmov RAX, 0 ; Chamada para leitura\n";
            TPCompiladores.assembly += "\tmov RDI, 0 ; Leitura do teclado\n";
            TPCompiladores.assembly += "\tsyscall\n\n";

            TPCompiladores.assembly += "; Leitura int\n";
            TPCompiladores.assembly += "\tmov EAX, 0 ; Acumulador\n";
            TPCompiladores.assembly += "\tmov EBX, 0 ; Caractere\n";
            TPCompiladores.assembly += "\tmov ECX, 10 ; Base 10\n";
            TPCompiladores.assembly += "\tmov DX, 1 ; Sinal\n";
            TPCompiladores.assembly += "\tmov RSI, M+" + enderecoParaHexa(buffer) + " ; Endereço do Buffer\n";
            TPCompiladores.assembly += "\tmov BL, [RSI] ; Carrega caractere\n";
            TPCompiladores.assembly += "\tcmp BL, '-' ; Compara se caractere é igual a -\n";
            TPCompiladores.assembly += "\tjne " + rotulo + " ; Se caractere diferente de - pula\n";
            TPCompiladores.assembly += "\tmov DX, -1 ; Armazena sinal -\n";
            TPCompiladores.assembly += "\tadd RSI, 1 ; Incrementa ponteiro string\n";
            TPCompiladores.assembly += "\tmov BL, [RSI] ; Carrega caractere -\n\n";

            TPCompiladores.assembly += "; Armazena o sinal na pilha\n";
            TPCompiladores.assembly += rotulo + ":\n";
            TPCompiladores.assembly += "\tpush DX ; Empilha sinal\n";
            TPCompiladores.assembly += "\tmov EDX, 0 ; Registrador da multiplicação\n\n";

            rotulo = geraRotulo();
            rotulo2 = geraRotulo();
            rotulo3 = geraRotulo();

            TPCompiladores.assembly += "; Loop Inteiro - Lendo caractere a carectere\n";
            TPCompiladores.assembly += rotulo + ":\n";
            TPCompiladores.assembly += "\tcmp BL, 0Ah ; Verifica fim da string\n";
            TPCompiladores.assembly += "\tje " + rotulo2 + " ; Salta se fim da string\n";
            TPCompiladores.assembly += "\timul ECX ; Multiplica EAX por 10\n";
            TPCompiladores.assembly += "\tsub BL, '0' ; Converte caractere\n";
            TPCompiladores.assembly += "\tadd EAX, EBX ; Soma valor caractere\n";
            TPCompiladores.assembly += "\tadd RSI, 1 ; Incrementa ponteiro\n";
            TPCompiladores.assembly += "\tmov BL, [RSI] ; Carrega caractere\n";
            TPCompiladores.assembly += "\tjmp " + rotulo + " ; Loop\n\n";

            TPCompiladores.assembly += rotulo2 + ":\n";
            TPCompiladores.assembly += "\tpop CX ; Desempilha sinal\n";
            TPCompiladores.assembly += "\tcmp CX, 0 ; Compara o sinal com 0\n";
            TPCompiladores.assembly += "\tjg " + rotulo3 + " ; Se o sinal é maior que zero pula\n";
            TPCompiladores.assembly += "\tneg EAX ; Nega número\n";

            TPCompiladores.assembly += rotulo3 + ":\n";
            TPCompiladores.assembly += "\tmov [M+" + enderecoParaHexa(identificador.getSimbolo().getEndereco()) + "], EAX ; Carrega o valor para o indentificador\n";
        }

        String rotuloLimpaBuffer = geraRotulo();
        long bufferAux = novoEnderecoTemporarios(1);
        TPCompiladores.assembly += rotuloLimpaBuffer + ":\n";
        TPCompiladores.assembly += "\tmov RDX, 1; Tamanho do buffer\n";
        TPCompiladores.assembly += "\tmov RSI, M+" + enderecoParaHexa(bufferAux) + " ; Salva o endereço do buffer\n";
        TPCompiladores.assembly += "\tmov RAX, 0 ; Chamada para leitura\n";
        TPCompiladores.assembly += "\tmov RDI, 0 ; Leitura do teclado\n";
        TPCompiladores.assembly += "\tsyscall\n\n";
        TPCompiladores.assembly += "\tmov AL,[M+" + enderecoParaHexa(bufferAux) + "]\n";
        TPCompiladores.assembly += "\tcmp AL, 0xA  ; Verifica se é nova linha\n";
        TPCompiladores.assembly += "\tjne " + rotuloLimpaBuffer + "; Lê o proximo se não for nova linha\n\n";
    }

    private String enderecoParaHexa(long endereco) {
        return "0x" + Long.toHexString(endereco);
    }

    private long novoEnderecoTemporarios(long bytes) {
        TPCompiladores.proximoTemporarioLivre += bytes;
        return TPCompiladores.proximoTemporarioLivre - bytes;
    }

    private String geraRotulo() {
        return "Rot" + TPCompiladores.incrementoRotulo++;
    }

    private long aloca_bytes(int bytes) {
        TPCompiladores.proximoEnderecoLivre += (long) bytes;
        return TPCompiladores.proximoEnderecoLivre - (long) bytes;
    }

    private String obtemComandoAtribuicao(Token tokenIdentificador, Token tokenConstante, boolean negacao) {
        String comando = "";
        switch (tokenIdentificador.getSimbolo().getTipoDados()) {
            case REAL:
                if (tokenConstante.getTipoConstante() == TipoEnum.INTEGER)
                    comando += "\tdd " + tokenConstante.getLexema() + ".0 ; Atribuição do real\n";
                else if (tokenConstante.getLexema().charAt(0) == '.')
                    comando += "\tdd 0" + tokenConstante.getLexema() + " ; Atribuição do real\n";
                else
                    comando += "\tdd " + tokenConstante.getLexema() + " ; Atribuição do real\n";
                break;
            case INTEGER:
                comando = negacao ? "\tdd -" + tokenConstante.getLexema() : "\tdd " + tokenConstante.getLexema();
                comando += " ; Atribuição " + tokenIdentificador.getSimbolo().getTipoDados().name();
                break;
            case CHAR:
                comando += "\tdb " + tokenConstante.getLexema();
                comando += " ;Atribuição char";
                break;
			case BOOLEAN:
				comando += "\tdb " + (tokenConstante.getLexema().contentEquals("TRUE") ? 1 : 0);
				comando += " ;Atribuição boolean";
				break;
            default:// String
                if (tokenConstante.getLexema().length() > 2) {
                    comando += "\tdb " + tokenConstante.getLexema() + ", 0";
                } else {
                    comando += "\tdb 0";
                }

                break;
        }
        comando += "\n";
        return comando;
    }

    private void geraAssemblyMaisMenos(TipoEnum expEsqRef, TipoEnum expDirRef, long expEsqEnd, long expDirEnd,
                                       boolean soma, long enderecoTemp) {
        TPCompiladores.assembly += "; Soma ou subtração de termos\n";

        if (expEsqRef == TipoEnum.INTEGER && expDirRef == TipoEnum.INTEGER) {
            TPCompiladores.assembly += "\tmov EAX, [M+" + enderecoParaHexa(expEsqEnd) + "] ; Moveu a T1(int) para EAX\n";
            TPCompiladores.assembly += "\tmov EBX, [M+" + enderecoParaHexa(expDirEnd) + "] ; Moveu a T2(int) para EBX\n";
            if (soma)
                TPCompiladores.assembly += "\tadd EAX, EBX ; Realiza a soma do conteudo de EAX com EBX\n";
            else
                TPCompiladores.assembly += "\tsub EAX, EBX ; Realiza a subtração do conteudo de EAX com EBX\n";

            TPCompiladores.assembly += "\tmov [M+" + enderecoParaHexa(enderecoTemp)
                    + "], EAX ; Coloca no temporario resultado da soma ou subtração\n";

        } else {
            if (expEsqRef == TipoEnum.INTEGER && expDirRef == TipoEnum.REAL) {
                TPCompiladores.assembly += "\tmovss XMM1, [M+" + enderecoParaHexa(expDirEnd) + "] ; Moveu T1 para XMM0\n";
                TPCompiladores.assembly += "\tmov EAX, [M+" + enderecoParaHexa(expEsqEnd) + "] ; Moveu a T2 para RAX\n";
                TPCompiladores.assembly += "\tcvtsi2ss XMM0, EAX ; Converteu o numero de RAX em REAL\n";

            } else if (expEsqRef == TipoEnum.REAL && expDirRef == TipoEnum.INTEGER) {
                TPCompiladores.assembly += "\tmovss XMM0, [M+" + enderecoParaHexa(expEsqEnd) + "] ; Moveu T1 para XMM0\n";
                TPCompiladores.assembly += "\tmov EAX, [M+" + enderecoParaHexa(expDirEnd) + "] ; Moveu a T2 para RAX\n";
                TPCompiladores.assembly += "\tcvtsi2ss XMM1, EAX ; Converteu o numero de RAX em REAL\n";

            } else { // Ambos REAL
                TPCompiladores.assembly += "\tmovss XMM0, [M+" + enderecoParaHexa(expEsqEnd) + "] ; Moveu T1 para XMM0\n";
                TPCompiladores.assembly += "\tmovss XMM1, [M+" + enderecoParaHexa(expDirEnd) + "] ; Moveu a T2 para XMM1\n";

            }

            if (soma)
                TPCompiladores.assembly += "\taddss XMM0, XMM1 ; Realiza a soma do conteudo de XMM0 com XMM1\n";
            else
                TPCompiladores.assembly += "\tsubss XMM0, XMM1 ; Realiza a subtração do conteudo de XMM0 com XMM1\n";

            TPCompiladores.assembly += "\tmovss [M+" + enderecoParaHexa(enderecoTemp)
                    + "], XMM0 ; Coloca no temporario o resultado da soma ou subtração\n";
        }
    }

    private void geraAssemblyMultDivReal(TipoEnum expEsqRef, TipoEnum expDirRef, long expEsqEnd, long expDirEnd, boolean mult, long enderecoTemp) {
        TPCompiladores.assembly += "; Multiplicação ou Divisão de termos\n";

        if (expEsqRef == TipoEnum.INTEGER && expDirRef == TipoEnum.INTEGER) {
            if(mult) {
                TPCompiladores.assembly += "\tmov EAX, [M+" + enderecoParaHexa(expEsqEnd) + "] ; Moveu a F1(int) para EAX\n";
                TPCompiladores.assembly += "\tmov EBX, [M+" + enderecoParaHexa(expDirEnd) + "] ; Moveu a F2(int) para EBX\n";
                TPCompiladores.assembly += "\timul EBX ; Realiza a multplicação de EAX com EBX\n";
                TPCompiladores.assembly += "\tmov [M+" + enderecoParaHexa(enderecoTemp) + "], EAX ; Coloca no temporario resultado da multiplicação\n";
            } else {
                TPCompiladores.assembly += "\tmov EAX, [M+" + enderecoParaHexa(expEsqEnd) + "] ; Moveu a F1(int) para EAX\n";
                TPCompiladores.assembly += "\tmov EBX, [M+" + enderecoParaHexa(expDirEnd) + "] ; Moveu a F2(int) para EBX\n";
                TPCompiladores.assembly += "\tcvtsi2ss XMM0, EAX ; Converteu o numero de EAX em real\n";
                TPCompiladores.assembly += "\tcvtsi2ss XMM1, EBX ; Converteu o numero de EBX em real\n";
                TPCompiladores.assembly += "\tdivss XMM0, XMM1 ; Realiza a divisão do conteudo de XMM0 com XMM1\n";
                TPCompiladores.assembly += "\tmovss [M+" + enderecoParaHexa(enderecoTemp)+ "], XMM0 ; Coloca no temporario o resultado da divisão\n";
            }

        } else {
            if (expEsqRef == TipoEnum.INTEGER && expDirRef == TipoEnum.REAL) {
                TPCompiladores.assembly += "\tmovss XMM1, [M+" + enderecoParaHexa(expDirEnd) + "] ; Moveu T1 para XMM0\n";
                TPCompiladores.assembly += "\tmov EAX, [M+" + enderecoParaHexa(expEsqEnd) + "] ; Moveu a T2 para RAX\n";
                TPCompiladores.assembly += "\tcvtsi2ss XMM0, EAX ; Converteu o numero de EAX em real\n";

            } else if (expEsqRef == TipoEnum.REAL && expDirRef == TipoEnum.INTEGER) {
                TPCompiladores.assembly += "\tmovss XMM0, [M+" + enderecoParaHexa(expEsqEnd) + "] ; Moveu T1 para XMM0\n";
                TPCompiladores.assembly += "\tmov EAX, [M+" + enderecoParaHexa(expDirEnd) + "] ; Moveu a T2 para RAX\n";
                TPCompiladores.assembly += "\tcvtsi2ss XMM1, EAX ; Converteu o numero de EAX em real\n";

            } else { // Ambos real
                TPCompiladores.assembly += "\tmovss XMM0, [M+" + enderecoParaHexa(expEsqEnd) + "] ; Moveu T1 para XMM0\n";
                TPCompiladores.assembly += "\tmovss XMM1, [M+" + enderecoParaHexa(expDirEnd) + "] ; Moveu a T2 para XMM1\n";

            }

            if (mult)
                TPCompiladores.assembly += "\tmulss XMM0, XMM1 ; Realiza a multiplicação do conteudo de XMM0 com XMM1\n";
            else
                TPCompiladores.assembly += "\tdivss XMM0, XMM1 ; Realiza a divisão do conteudo de XMM0 com XMM1\n";

            TPCompiladores.assembly += "\tmovss [M+" + enderecoParaHexa(enderecoTemp)
                    + "], XMM0 ; Coloca no temporario o resultado da multiplicação ou divisão\n";
        }
    }

    private void geraAssemblyDivMod(long expEsqEnd, long expDirEnd, boolean div, long enderecoTemp) {
        TPCompiladores.assembly += "; Divisao ou Mod de termos\n";
        TPCompiladores.assembly += "\tmov EAX, [M+" + enderecoParaHexa(expEsqEnd) + "] ; Moveu a F1(int) para EAX\n";
        TPCompiladores.assembly += "\tcdq\n";
        TPCompiladores.assembly += "\tmov EBX, [M+" + enderecoParaHexa(expDirEnd) + "] ; Moveu a F2(int) para EBX\n";
        TPCompiladores.assembly += "\tidiv EBX ; Realiza a divisão de EAX com EBX\n";
        if (div)
            TPCompiladores.assembly += "\tmov [M+" + enderecoParaHexa(enderecoTemp) + "], EAX ; Coloca no temporario o quociente da divisão\n";
        else
            TPCompiladores.assembly += "\tmov [M+" + enderecoParaHexa(enderecoTemp) + "], EDX ; Coloca no temporario resto da divisão\n";
    }

}