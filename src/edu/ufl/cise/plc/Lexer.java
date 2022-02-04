package edu.ufl.cise.plc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer implements ILexer {

    public CharSequence inputChars;
    public ArrayList<Token> tokens;
    private Lexer.State state;
    private int lexerLine = 0;
    private int lexerColumn = 0;
    private int currToken = -1;
    private HashMap<String, Token.Kind> reservedMap=  new HashMap<>();

    private enum State {
        START,
        IN_IDENT,
        IN_STRING,
        IN_COMMENT,
        IN_FLOAT,
        IN_INT,
        HAVE_DOT,
        HAVE_EQ,
        HAVE_MINUS,
        HAVE_GREATER,
        HAVE_LESS,
        HAVE_EXCLAMATION,
        START_ZERO
    }

    public void setState (State newState){
        this.state = newState;
    }

    public void incrementLexerLine (){
        lexerLine++;
    }

    public void resetLexerColumn (){
        lexerColumn = 0;
    }

    public void incrementLexerColumn (){
        lexerColumn++;
    }

    public void decrementLexerLine(){
        lexerLine--;
    }

    public void decrementLexerColumn (){
        lexerColumn--;
    }


    public Lexer(String input) {

        this.inputChars = input;
        this.tokens = new ArrayList<>();
        identifyToken(this.inputChars);

    }

    //FOR TESTING PURPOSES

    /*public static void main (String args []) {
        Lexer lex = new Lexer("""
                "This is a string"
                #this is a comment
                *
                """);

        //lex.identifyToken(lex.inputChars);

        for (int i = 0; i < lex.tokens.size(); i++) {

            System.out.println(lex.tokens.get(i).getText());
            System.out.println("Kind: " + lex.tokens.get(i).getKind());
            System.out.println("Location: " + lex.tokens.get(i).getSourceLocation());
            System.out.println("Length: " + lex.tokens.get(i).getLength());
            if (lex.tokens.get(i).getKind() == IToken.Kind.INT_LIT)
                System.out.println("Value: " + lex.tokens.get(i).getIntValue() + '\n');
            if (lex.tokens.get(i).getKind() == IToken.Kind.FLOAT_LIT)
                System.out.println("Value: " + lex.tokens.get(i).getFloatValue() + '\n');
            if (lex.tokens.get(i).getKind() == IToken.Kind.STRING_LIT)
                System.out.println("Value: " + lex.tokens.get(i).getStringValue() + '\n');
             }

    }//*/

    public void identifyToken(CharSequence inputChars) {

        setState(State.START);
        buildMap();
        Token tempToken = new Token();
        for (int i = 0; i < inputChars.length(); i++) {

            //reset column to 0 on new line
            if (inputChars.charAt(i) == '\n') {
                resetLexerColumn();
            }

            char c = inputChars.charAt(i); // get current char

            switch (state) {
                case START -> {
                    Token token = start(c, lexerLine, lexerColumn);
                    if (token != null && token.getComplete())
                        tokens.add(token);
                    else
                        tempToken = token;
                }
                case IN_IDENT -> {
                    tempToken = possibleToken(tempToken, c);
                    if (tempToken.getComplete()){
                        tokens.add(tempToken);
                        if (!reservedMap.containsKey(tempToken.getText())) {
                            i--;
                            decrementLexerColumn();
                        }
                        }
                }
                case IN_STRING -> {
                    tempToken = possibleToken(tempToken, c);
                    if (tempToken.getComplete())
                        tokens.add(tempToken);
                }
                case IN_INT -> {
                    tempToken = possibleToken(tempToken, c);
                    if (tempToken.getComplete()){
                        tokens.add(tempToken);
                        i--;
                        decrementLexerColumn();}
                }
                case START_ZERO -> {
                    tempToken = possibleToken(tempToken, c);
                    if (tempToken.getComplete()){
                        tokens.add(tempToken);
                        if (tempToken.getLength() == 1){
                            //include token after single character
                            i--;
                            decrementLexerColumn();
                        }
                    }
                }
                case HAVE_LESS, HAVE_GREATER, HAVE_EQ, HAVE_MINUS, HAVE_EXCLAMATION -> {
                    tempToken = possibleToken (tempToken, c);
                    if (tempToken.getComplete()){
                        tokens.add(tempToken);
                        if (tempToken.getLength() == 1){
                            //include token after single characteR
                            i--;
                            decrementLexerColumn();
                        }
                    }
                }
                case HAVE_DOT -> {
                    tempToken = possibleToken (tempToken, c);
                    if (tempToken.getComplete())
                        tokens.add(tempToken);

                }
                case IN_FLOAT -> {
                    tempToken = possibleToken (tempToken, c);
                    if (tempToken.getComplete()){
                        tokens.add(tempToken);
                        i--;
                        decrementLexerColumn();}

                }
                case IN_COMMENT -> {
                    if (c == '\n'){
                        setState(State.START);
                        resetLexerColumn();
                        incrementLexerLine();
                    }
                }
            }

            //increment column if char is not a newline
           if (inputChars.charAt(i) != '\n')
                incrementLexerColumn();

        }
        Token finalToken = new Token();
        finalToken.setKind(Token.Kind.EOF);
        tokens.add(finalToken);
    }

public Token start(char c, int line, int column) {

    IToken.SourceLocation startPos = new IToken.SourceLocation(line, column);  //save position of first char in token\
    Token token = new Token(startPos);
    switch (c) {
        //new line character
        case '\n' -> {
            incrementLexerLine();
        }
        // all single chars
        case '&', ',', '/', '(', '[', '%', '|', '+', '^', ')', ']', ';', '*','.' -> {
            token.setKind(findKind(c));
            token.concatText(c);
            token.addLength();
            token.setComplete();
            setState(State.START);
            return token;
        }
        //State with less than
        case '<' -> {
            setState(State.HAVE_LESS);
            token.concatText(c);
            token.addLength();
            return token;
        }
        //Greater than
        case '>' -> {
            setState(State.HAVE_GREATER);
            token.concatText(c);
            token.addLength();
            return token;
        }
        //Equals
        case '=' -> {
            setState(State.HAVE_EQ);
            token.concatText(c);
            token.addLength();
            return token;
        }
        //Exclamation
        case '!' -> {
            setState(State.HAVE_EXCLAMATION);
            token.concatText(c);
            token.addLength();
            return token;
        }
        //Minus
        case '-' -> {
            setState(State.HAVE_MINUS);
            token.concatText(c);
            token.addLength();
            return token;
        }
        case '0' -> {
            setState(State.START_ZERO);
            token.concatText(c);
            token.addLength();
            return token;
        }

        case '1','2','3','4','5','6','7','8','9' -> {
            setState(State.IN_INT);
            token.concatText(c);
            token.addLength();
            return token;
        }
        case '"' -> {
            setState(State.IN_STRING);
            token.concatText(c);
            token.addLength();
            return token;
        }
        case 'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z','_','$' ->{
            setState(State.IN_IDENT);
            token.concatText(c);
            token.addLength();
            return token;
        }
        case '#'->{
            setState(State.IN_COMMENT);
            return token;
        }
        case ' ', '\t','\r'->{return null;}
        default -> {
            token.concatText(c);
            token.setKind(Token.Kind.ERROR);
            token.setComplete();
            token.addLength();
            setState(State.START);
            return token;}

        }
        return null;
    }

public Token possibleToken (Token token, char c){
        switch (state){
            case HAVE_LESS ->{
                switch (c){
                    case '<' ->{token.concatText(c);
                        token.addLength();
                        token.setKind(IToken.Kind.LANGLE);
                        token.setComplete();
                        setState(State.START);
                        return token;}
                    case '=' ->{token.concatText(c);
                        token.addLength();
                        token.setKind(IToken.Kind.LE);
                        token.setComplete();
                        setState(State.START);
                        return token;}
                    case '-' ->{token.concatText(c);
                        token.addLength();
                        token.setKind(IToken.Kind.LARROW);
                        token.setComplete();
                        setState(State.START);
                        return token;}
                    default -> {token.setKind(IToken.Kind.LT);
                        token.setComplete();
                        setState(State.START);
                        return token;}
                }
            }
            case HAVE_GREATER ->{

                switch (c){
                    case '>' ->{token.concatText(c);
                        token.addLength();
                        token.setKind(IToken.Kind.RANGLE);
                        token.setComplete();
                        setState(State.START);
                        return token;}
                    case '=' ->{token.concatText(c);
                        token.addLength();
                        token.setKind(IToken.Kind.GE);
                        token.setComplete();
                        setState(State.START);
                        return token;}
                    default -> {token.setKind(IToken.Kind.GT);
                        token.setComplete();
                        setState(State.START);
                        return token;}
                }
            }
            case HAVE_EXCLAMATION -> {

                switch (c){
                    case '=' ->{token.concatText(c);
                        token.addLength();
                        token.setKind(IToken.Kind.NOT_EQUALS);
                        token.setComplete();
                        setState(State.START);
                        return token;}
                    default -> {token.setKind(IToken.Kind.BANG);
                        token.setComplete();
                        setState(State.START);
                        return token;}
                }
            }
            case HAVE_EQ -> {

                switch (c){
                    case '=' ->{token.concatText(c);
                        token.addLength();
                        token.setKind(IToken.Kind.EQUALS);
                        token.setComplete();
                        setState(State.START);
                        return token;}
                    default -> {token.setKind(IToken.Kind.ASSIGN);
                        token.setComplete();
                        setState(State.START);
                        return token;}
                }
            }
            case HAVE_MINUS -> {

                switch (c){
                    case '>' ->{token.concatText(c);
                        token.addLength();
                        token.setKind(IToken.Kind.RARROW);
                        token.setComplete();
                        setState(State.START);
                        return token;}
                    default -> {token.setKind(IToken.Kind.MINUS);
                        token.setComplete();
                        setState(State.START);
                        return token;}
                }

            }
            case START_ZERO -> {
                switch(c) {
                    case '.' -> {
                        token.concatText(c);
                        token.addLength();
                        setState(State.HAVE_DOT);
                        return token;
                    }
                    default -> {
                        token.setKind(IToken.Kind.INT_LIT);
                        token.setComplete();
                        try{token.setIntValue(Integer.parseInt(token.getText()));}catch(NumberFormatException e){token.setKind(IToken.Kind.ERROR);}
                        setState(State.START);
                        return token;
                    }
                }
            }
            case HAVE_DOT -> {
                switch(c){
                    case '0','1','2','3','4','5','6','7','8','9' ->{
                        token.concatText(c);
                        token.addLength();
                        setState(State.IN_FLOAT);
                        return token;
                    }
                    default ->{token.setKind(IToken.Kind.ERROR);
                        token.setComplete();
                        setState(State.START);
                        return token;}
                }
            }
            case IN_FLOAT -> {
                switch (c){
                    case '0','1','2','3','4','5','6','7','8','9' ->{
                        token.concatText(c);
                        token.addLength();
                        return token;
                    }
                    default -> {
                        token.setKind(IToken.Kind.FLOAT_LIT);
                        token.setComplete();
                        try{token.setFloatValue(Float.parseFloat(token.getText()));}catch(NumberFormatException e){token.setKind(IToken.Kind.ERROR);}
                        setState(State.START);
                        return token;}
                }
            }
            case IN_INT -> {
                switch (c) {
                    case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                        token.concatText(c);
                        token.addLength();
                        return token;
                    }
                    case '.' -> {
                        token.concatText(c);
                        token.addLength();
                        setState(State.HAVE_DOT);
                        return token;
                    }
                    default -> {
                        token.setKind(IToken.Kind.INT_LIT);
                        token.setComplete();
                        token.setIntValue(Integer.parseInt(token.getText()));
                        setState(State.START);
                        return token;
                    }
                }
            }
            case IN_STRING -> {
                switch(c) {
                    case '\b',  '\n', '\t', '\f', '\r', '\\', '\'' -> {
                        token.concatText(c);
                        token.addLength();
                        return token;
                    }
                    case '"' -> {
                        token.setKind(IToken.Kind.STRING_LIT);
                        token.concatText(c);
                        token.setComplete();
                        token.setStringValue(token.getText());
                        setState(State.START);
                        return token;
                    }
                    default -> {
                        token.concatText(c);
                        token.addLength();
                        return token;
                    }
                }
            }
            case IN_IDENT -> {
                switch (c){
                    case 'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z','_','$', '0','1','2','3','4','5','6','7','8','9' ->{
                        token.concatText(c);
                        token.addLength();
                        if (reservedMap.containsKey(token.getText())){
                            token.setKind(reservedMap.get(token.getText()));
                            if (token.getKind() == Token.Kind.BOOLEAN_LIT){
                                token.setBooleanValue(token.getText() == "true");
                            }
                            token.setComplete();
                            setState(State.START);
                        }
                        return token;
                    }
                    default -> {
                        token.setKind(IToken.Kind.IDENT);
                        token.setComplete();
                        setState(State.START);
                        return token;
                    }
                }

            }
            default -> {return null;}
        }
}

    public Token.Kind findKind(char c){
        Token.Kind kind;
        switch(c){
            case '&' -> { kind = Token.Kind.AND;}
            case ',' -> { kind = Token.Kind.COMMA;}
            case '/' -> { kind = Token.Kind.DIV;}
            case '(' -> { kind = Token.Kind.LPAREN;}
            case '[' -> { kind = Token.Kind.LSQUARE;}
            case '%' -> { kind = Token.Kind.MOD;}
            case '|' -> { kind = Token.Kind.OR;}
            case '+' -> { kind = Token.Kind.PLUS;}
            case '^' -> { kind = Token.Kind.RETURN;}
            case ')' -> { kind = Token.Kind.RPAREN;}
            case ']' -> { kind = Token.Kind.RSQUARE;}
            case ';' -> { kind = Token.Kind.SEMI;}
            case '*' -> { kind = Token.Kind.TIMES;}
            case '.' -> {kind = Token.Kind.ERROR;}

            default -> { kind = null;}
        }

        return kind;

    }

    public void buildMap(){
        HashMap<String, Token.Kind> map = new HashMap<>();
        map.put("BLACK", Token.Kind.COLOR_CONST);
        map.put("BLUE", Token.Kind.COLOR_CONST);
        map.put("CYAN", Token.Kind.COLOR_CONST);
        map.put("DARK_GRAY", Token.Kind.COLOR_CONST);
        map.put("GREEN", Token.Kind.COLOR_CONST);
        map.put("GRAY", Token.Kind.COLOR_CONST);
        map.put("LIGHT_GRAY", Token.Kind.COLOR_CONST);
        map.put("MAGENTA", Token.Kind.COLOR_CONST);
        map.put("ORANGE", Token.Kind.COLOR_CONST);
        map.put("PINK", Token.Kind.COLOR_CONST);
        map.put("RED", Token.Kind.COLOR_CONST);
        map.put("WHITE", Token.Kind.COLOR_CONST);
        map.put("YELLOW", Token.Kind.COLOR_CONST);
        map.put("string", Token.Kind.TYPE);
        map.put("int", Token.Kind.TYPE);
        map.put("boolean", Token.Kind.TYPE);
        map.put("float", Token.Kind.TYPE);
        map.put("color", Token.Kind.TYPE);
        map.put("image", Token.Kind.TYPE);
        map.put("void", Token.Kind.KW_VOID);
        map.put("getWidth", Token.Kind.IMAGE_OP);
        map.put("getHeight", Token.Kind.IMAGE_OP);
        map.put("getRed", Token.Kind.COLOR_OP);
        map.put("getGreen", Token.Kind.COLOR_OP);
        map.put("getBlue", Token.Kind.COLOR_OP);
        map.put("true", Token.Kind.BOOLEAN_LIT);
        map.put("false", Token.Kind.BOOLEAN_LIT);
        map.put("if", Token.Kind.KW_IF);
        map.put("else", Token.Kind.KW_ELSE);
        map.put("fi", Token.Kind.KW_FI);
        map.put("write", Token.Kind.KW_WRITE);
        map.put("console", Token.Kind.KW_CONSOLE);
       this.reservedMap = map;

    }


    @Override
    public IToken next() throws LexicalException {
        currToken++;
        if (tokens.get(currToken).getKind() == Token.Kind.ERROR)
            throw new LexicalException("Error - Invalid Token: " + tokens.get(currToken).getSourceLocation());
        return tokens.get(currToken);
    }


    @Override
    public IToken peek() throws LexicalException {
        if (tokens.get(currToken + 1).getKind() == Token.Kind.ERROR)
            throw new LexicalException("Error - Invalid Token: " + tokens.get(currToken + 1).getSourceLocation());
        return tokens.get(currToken + 1);
    }
}
