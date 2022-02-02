import edu.ufl.cise.plc.IToken;

public class Token implements IToken {

    private Kind kind;
    private String rawText;
    private SourceLocation sourceLocation;
    private int intValue = 0;
    private float floatValue = 0;
    private boolean booleanValue;
    private String stringValue;

    public Token(Kind kind, String rawText){
        this.kind = kind;
        this.rawText = rawText;
    }



    @Override
    public Kind getKind() {return kind; }

    @Override
    public String getText() {
        return rawText;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public int getIntValue() { return intValue; }

    @Override
    public float getFloatValue() {
        return floatValue;
    }

    @Override
    public boolean getBooleanValue() {
        return booleanValue;
    }

    @Override
    public String getStringValue() {
        return stringValue;
    }
}
