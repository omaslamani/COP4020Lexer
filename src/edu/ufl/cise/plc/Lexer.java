package edu.ufl.cise.plc;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer implements ILexer {

    public CharSequence inputChars;
    public ArrayList<Token> tokens;
    private Lexer.State state;
    private int lexerLine = 0;
    private int lexerColumn = 0;

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


    public Lexer(String input){

        this.inputChars = input;
        this.tokens = new ArrayList<>();

    }

    public static void main (String args []) { //probably delete later but for testing
        Lexer lex = new Lexer("""
				()  <<  >
				&&
				--0
				0
				**    
				  
				%,,<<<
				""");

        lex.identifyToken(lex.inputChars);

        for (int i = 0; i < lex.tokens.size(); i++) {

            System.out.println(lex.tokens.get(i).getText());
            System.out.println("Kind: " + lex.tokens.get(i).getKind());
            System.out.println("Location: " + lex.tokens.get(i).getSourceLocation() + '\n');

             }

    }

    public void identifyToken(CharSequence inputChars) {

        setState(State.START);
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
                }
                case IN_INT -> {

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
                            //include token after single character
                            i--;
                            decrementLexerColumn();
                        }
                    }
                }
                //default -> throw new IllegalStateException(“lexer bug”);
            }

            //increment column if char is not a newline
           if (inputChars.charAt(i) != '\n')
                incrementLexerColumn();

        }

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
        case '&', ',', '/', '(', '[', '%', '|', '+', '^', ')', ']', ';', '*' -> {
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
        case '1','2','3','4','5','6','7','8','9' -> {}
    }

    return null; //should return null when there is whitespace and newline

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
                        setState(State.START);
                        return token;
                    }
                }
            }
            default -> {return null;} //might switch to throw exception/error
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

            default -> { kind = null;}
        }

        return kind;

    }
    public void addToTokenList(Token token) {

        tokens.add(token);

        //Pattern ignoreChar = Pattern.compile("[\n\t\r\s]+");
        // Matcher ignoreCharMatcher;
        // Token tempToken = new Token();
        //for (int i = 0; i < inputChars.length(); i++) {
        //CharSequence toBeMatched = new StringBuilder(1).append(c);
        //ignoreCharMatcher = ignoreChar.matcher(Character.toString(inputChars.charAt(i)));
        // boolean b = ignoreCharMatcher.matches();

    }


    @Override
    public IToken next() throws LexicalException {
        System.out.println("Testing the stuff working or nawh?? test");

        return null;
    }


    @Override
    public IToken peek() throws LexicalException {
        return null;
    }
}
