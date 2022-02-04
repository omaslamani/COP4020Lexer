package edu.ufl.cise.plc;

public class Token implements IToken {

    private Kind kind;
    private String rawText = "";
    private SourceLocation sourceLocation;
    private int intValue = 0;
    private float floatValue = 0;
    private boolean booleanValue;
    private String stringValue;
    private int length;
    private boolean complete = false;

    public Token(Kind kind, String rawText, int length, SourceLocation sourceLocation){
        this.kind = kind;
        this.rawText = rawText;
        this.length = length;
        this.sourceLocation = sourceLocation;
    }

    public Token(){
    }

    public Token(SourceLocation sourceLocation){
        this.sourceLocation = sourceLocation;
    }


    @Override
    public Kind getKind() {return kind; }

    @Override
    public String getText() {
        return rawText;
    }

    @Override
    public SourceLocation getSourceLocation() { return sourceLocation; }

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

    public int getLength() { return length; }

    public void setSourceLocation(int line, int column){
        sourceLocation = new SourceLocation(line, column);
    }

    public void setKind(Kind kind){ this.kind = kind; }

    public void concatText (char letter){this.rawText+=letter;}

    public void addLength(){this.length++;}

    public boolean getComplete (){return this.complete;}

    public void setComplete (){this.complete = true;}

    public void setIntValue(int val){this.intValue = val; }

    public void setFloatValue(float val){this.floatValue = val; }

    public void setStringValue(String val){this.stringValue = val; }

    public void setIncomplete (){this.complete = false;}
}
