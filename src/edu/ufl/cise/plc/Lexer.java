package edu.ufl.cise.plc;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer implements ILexer {

    public CharSequence inputChars;
    public ArrayList<Token> tokens;
    private Lexer.State state;

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
        HAVE_EXCLAMATION
    }
    public void setState (State newState){
        this.state = newState;
    }
    public Lexer(String input){

        this.inputChars = input;
        this.tokens = new ArrayList<>();

    }

    public static void main (String args []) { //probably delete later but for testing
        Lexer lex = new Lexer("""
				<=
				&
				<<
				<
				%
				***
				<=<<<
				""");

        lex.identifyToken(lex.inputChars);

        for (int i = 0; i < lex.tokens.size(); i++) {

            System.out.println(lex.tokens.get(i).getText());
            System.out.println("Kind: " + lex.tokens.get(i).getKind());
            System.out.println("Location: " + lex.tokens.get(i).getSourceLocation() + '\n');

             }

    }

    public void identifyToken(CharSequence inputChars) {

        int line = 0;
        int column = 0;

        setState(State.START);
        Token tempToken = new Token();
        for (int i = 0; i < inputChars.length(); i++) {

            if (inputChars.charAt(i) == '\n') {
                line++;
                column = 0;
            }

            char c = inputChars.charAt(i); // get current char

            switch (state) {
                case START -> {
                    Token token = start(c, line, column);
                    if (token != null && token.getComplete())
                        tokens.add(token);
                    else
                        tempToken = token;
                }
                case IN_IDENT -> {
                }
                case HAVE_LESS -> {
                    tempToken = possibleToken (tempToken, c);
                    if (tempToken.getComplete())
                        tokens.add(tempToken);
                }
                //default -> throw new IllegalStateException(“lexer bug”);
            }

            column++;

        }

    }

public Token start(char c, int line, int column) {

    IToken.SourceLocation startPos = new IToken.SourceLocation(line, column);  //save position of first char in token\
    Token token = new Token(startPos);
    switch (c) {
        //white space
        case ' ','\t','\r' -> {
            column++;
        }
        //new line character
        case '\n' -> {
            column = 0;
            line++;
        }
        // all single chars
        case '&', ',', '/', '(', '[', '%', '|', '+', '^', ')', ']', ';', '*' -> {
            token.setKind(findKind(c));
            token.concatText(c);
            token.addLength();
            token.setComplete();
            setState(State.START);
            column++;
            return token;
        }
        //State with less than
        case '<' -> {
            setState(State.HAVE_LESS);
            token.concatText(c);
            token.addLength();
            column++;
            return token;
        }
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
            case HAVE_GREATER ->{}
            case HAVE_EXCLAMATION -> {}
            case HAVE_EQ -> {}
            case HAVE_MINUS -> {}
            default -> {return null;}
        }
    return null;
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
