import edu.ufl.cise.plc.ILexer;
import edu.ufl.cise.plc.IToken;
import edu.ufl.cise.plc.LexicalException;

public class Lexer implements ILexer {
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
