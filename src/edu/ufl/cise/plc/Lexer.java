package edu.ufl.cise.plc;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer implements ILexer {

    public CharSequence inputChars;
    public ArrayList<Token> tokens;

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

    public Lexer(String input){

        this.inputChars = input;
        this.tokens = new ArrayList<>();

    }

    public static void main (String args []) { //probably delete later but for testing

        Lexer lex = new Lexer("""
                +&%  
		        **/
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

        State state = Lexer.State.START;

        for (int i = 0; i < inputChars.length(); i++) {

            if (inputChars.charAt(i) == '\n') {
                line++;
                column = 0;
            }
            else
                column++;

            char c = inputChars.charAt(i); // get current char

            switch (state) {
                case START -> {
                    Token token = start(c, line, column, state);
                    if (token != null)
                        tokens.add(token);
                }
                case IN_IDENT -> {
                }
                //case HAVE_DOT -> {
                //}
                //default -> throw new IllegalStateException(“lexer bug”);
            }

        }

    }

public Token start(char c, int line, int column, State state) {

    IToken.SourceLocation startPos = new IToken.SourceLocation(line, column);  //save position of first char in token\

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
         Token token = new Token(findKind(c), Character.toString(c), 1, startPos);
         state = State.START;
         column++;
         return token;
        }
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
