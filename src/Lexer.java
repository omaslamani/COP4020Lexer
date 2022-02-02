import edu.ufl.cise.plc.ILexer;
import edu.ufl.cise.plc.IToken;
import edu.ufl.cise.plc.LexicalException;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer implements ILexer {

    public char[] inputChars;

    public Lexer(String input){

        char [] inputChars = new char [input.length()]; //turns input into char array
        CharSequence newChars = Arrays.toString(inputChars);
        for (int i = 0; i < input.length(); i++) {
            inputChars[i] = input.charAt(i);
        }

        this.inputChars = inputChars;

    }

    public static void main (String args []) { //probably delete later but for testing

        Lexer lex = new Lexer("hello world");

    }

    public Token[] lex(char[] inputChars) {
        Pattern ignoreChar = Pattern.compile("[\n\t\r\s]+");
        Matcher ignoreCharMatcher;
        for (char c : inputChars) {
        CharSequence toBeMatched = new StringBuilder(1).append(c);
        ignoreCharMatcher = ignoreChar.matcher(toBeMatched);
            //while char is not whitespace, newline, etc.
            //do stuff
        }

        return null;


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
