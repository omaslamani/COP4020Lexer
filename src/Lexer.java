import edu.ufl.cise.plc.ILexer;
import edu.ufl.cise.plc.IToken;
import edu.ufl.cise.plc.LexicalException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer implements ILexer {

    public char[] inputChars;

    public Lexer(String input){

        char [] inputChars = new char [input.length()]; //turns input into char array
        for (int i = 0; i < input.length(); i++) {
            inputChars[i] = input.charAt(i);
        }

        this.inputChars = inputChars;

    }

    public static void main (String args []) { //probably delete later but for testing

        Lexer lex = new Lexer("hello world");

    }

    public Token[] lex(char[] inputChars) {

        for (char c : inputChars) {
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
