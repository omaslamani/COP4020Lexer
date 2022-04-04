package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;

import java.lang.reflect.Array;
import java.util.ArrayList;


public class Parser implements IParser {

    private final ArrayList<Token> tokens;
    private int current = 0;


    public Parser(ArrayList<Token> tokens) throws PLCException {
        this.tokens = tokens;
    }

    //FOR TESTING PURPOSES
    public static void main (String args []) throws PLCException {
        Lexer lex = new Lexer("""
                image BDP0(int size, int size1)
                image[size,size1] a;
                a[x,y] = <<(x/8*y/8)%(Z+1), 0, 0>>;
                ^ a;
                """);
        Parser parser = new Parser(lex.tokens);

        System.out.println(parser.parse());

    }

    /*Running list of parser errors
    Currently right associative instead of left*/


    @Override
    public ASTNode parse() throws PLCException {

        //check entire token list for any invalid tokens
        for (int i = 0; i < tokens.size(); i++){
            if (tokens.get(i).getKind() == Token.Kind.ERROR){
                throw new LexicalException("Invalid token");
            }
        }

            return program();

        }
    private Program program() throws PLCException{
        Token firstToken = tokens.get(current);
        Types.Type returnType = null;
        String name = "";
        ArrayList<NameDef> params = new ArrayList<>();
        ArrayList<ASTNode> decsAndStatements = new ArrayList<>();
        if (match(Token.Kind.TYPE) || match(Token.Kind.KW_VOID)){
            returnType = Types.Type.toType(tokens.get(current).getText());
            current++;
            if (match(Token.Kind.IDENT)){
                name = tokens.get(current).getText();
                current++;
                if (match(Token.Kind.LPAREN)){
                    current++;
                        if (match(Token.Kind.RPAREN)){
                            current++;
                        }
                        else{
                            params.add(nameDef());
                            while(match(Token.Kind.COMMA)){
                                current++;
                                params.add(nameDef());
                            }
                            if (match(IToken.Kind.RPAREN)) {
                                current++;
                            }
                            else
                                throw new PLCException ("Missing Right Paren");
                        }

                            if (match(Token.Kind.EOF))
                                return new Program(firstToken,returnType,name,params,decsAndStatements);
                            else if(match(Token.Kind.TYPE))
                                decsAndStatements.add(declaration());
                            else
                                decsAndStatements.add(statement());
                            while(match(Token.Kind.SEMI)){
                                current++;
                                if (match(Token.Kind.EOF))
                                    break;
                                else if(match(Token.Kind.TYPE))
                                    decsAndStatements.add(declaration());
                                else
                                    decsAndStatements.add(statement());
                            }
                            if (match(Token.Kind.EOF) && tokens.get(current - 1).getKind() == Token.Kind.SEMI)
                                return new Program(firstToken,returnType,name,params,decsAndStatements);
                            else
                                throw new SyntaxException ("Missing Semicolon");

                }
                else
                    throw new SyntaxException ("Missing Left Paren");
            }
            else
                throw new SyntaxException ("Missing Ident");
        }
        else
            throw new SyntaxException ("Missing Type");
    }

    private VarDeclaration declaration() throws PLCException{
        Token firstToken = tokens.get(current);
        NameDef nameDef = null;
        Token op = null;
        Expr expression = null;
        nameDef = nameDef();
        if (match(Token.Kind.ASSIGN) ||match(Token.Kind.LARROW)){
            op = tokens.get(current);
            current++;
            expression = expr();
        }
        return new VarDeclaration(firstToken,nameDef,op,expression);
    }
    private NameDef nameDef() throws PLCException{

        Token firstToken = tokens.get(current);
        String type;
        String name;
        Dimension dim;

        if (match(Token.Kind.TYPE)){
            type = tokens.get(current).getText();
            current++;}
        else throw new SyntaxException("Missing type");

        //if next kind is square bracket
        if (match(Token.Kind.LSQUARE)){
                current++;
                dim = dimension();
            if (match(Token.Kind.IDENT)){
                name = tokens.get(current).getText();
                current++;
                    return new NameDefWithDim(firstToken, type, name, dim);
            }
            else throw new PLCException("Missing name");
        }

        //if next kind is ident (no dimension)
        if (match(Token.Kind.IDENT)){
            name = tokens.get(current).getText();
            current++;
            return new NameDef(firstToken, type, name);
        }
        else throw new PLCException("Missing name");

    }

    private Expr expr() throws PLCException {

        if (match(Token.Kind.KW_IF)){
            current++;
            return conditionalExpr();
        }
        else
            return logicalOrExpr();
    }

    private Expr conditionalExpr() throws PLCException {

        Token firstToken = tokens.get(current);
        Expr condition;
        Expr trueCase;
        Expr falseCase;

        //if kind is left parenthesis
        if (match(Token.Kind.LPAREN)) {
            current++;
            condition = expr();
        }
        else { throw new PLCException("Expression needs left parenthesis"); }


        //if next kind is right parenthesis
        if (match(Token.Kind.RPAREN)) {
            current++;
            trueCase = expr();
            }
        else { throw new PLCException("Expression needs right parenthesis"); }


        //if next kind is else
        if (match(Token.Kind.KW_ELSE)) {
            current++;
            falseCase = expr();
            }
        else { throw new PLCException("Expression missing keyword else"); }


        //if next kind is fi
        if (match(Token.Kind.KW_FI)) {
            current++;
            return new ConditionalExpr(firstToken, condition, trueCase, falseCase);
                    }
        else { throw new PLCException("Expression missing keyword fi");}

    }



    private Expr logicalOrExpr() throws PLCException {

        Token firstToken = tokens.get(current);
        Expr left;
        Token op;
        Expr right;

        left = logicalAndExpr();
        if (match(Token.Kind.OR)){
            op = tokens.get(current);
            current++;
            right = logicalOrExpr();
            return new BinaryExpr(firstToken, left, op, right);
        }

        else {
            return left;
        }

    }

    private Expr logicalAndExpr() throws PLCException {

        Token firstToken = tokens.get(current);
        Expr left;
        Token op;
        Expr right;

        left = comparisonExpr();
        if (match(Token.Kind.AND)){
            op = tokens.get(current);
            current++;
            right = logicalAndExpr();
            return new BinaryExpr(firstToken, left, op, right);
        }

        else {
            return left;
        }

    }

    private Expr comparisonExpr() throws PLCException {

        Token firstToken = tokens.get(current);
        Expr left;
        Token op;
        Expr right;

        left = additiveExpr();
        if (match(Token.Kind.LT) | match(Token.Kind.GT) | match(Token.Kind.EQUALS) | match(Token.Kind.NOT_EQUALS) | match(Token.Kind.LE) | match(Token.Kind.GE)){
            op = tokens.get(current);
            current++;
            right = comparisonExpr();
            return new BinaryExpr(firstToken, left, op, right);
        }

        else {
            return left;
        }

    }

    private Expr additiveExpr() throws PLCException {

        Token firstToken = tokens.get(current);
        Expr left;
        Token op;
        Expr right;

        left = multiplicativeExpr();

            if (match(Token.Kind.PLUS) | match(Token.Kind.MINUS)) {
                op = tokens.get(current);
                current++;
                right = additiveExpr();

                return new BinaryExpr(firstToken, left, op, right);
            } else {
                return left;
            }
    }

    private Expr multiplicativeExpr() throws PLCException {

        Token firstToken = tokens.get(current);
        Expr left;
        Token op;
        Expr right;

        left = unaryExpr();
        if (match(Token.Kind.TIMES) | match(Token.Kind.DIV) | match(Token.Kind.MOD)){
            op = tokens.get(current);
            current++;
            right = multiplicativeExpr();
            return new BinaryExpr(firstToken, left, op, right);
        }

        else {
            return left;
        }

    }

    private Expr unaryExpr() throws PLCException {
        Token firstToken  = tokens.get(current);
        Token op;
        Expr e;
        if (match(Token.Kind.COLOR_OP) || match(Token.Kind.IMAGE_OP) || match(Token.Kind.BANG) || match(Token.Kind.MINUS)){
            op = tokens.get(current);
            current++;
            e = unaryExpr();
        }
        else{
            return unaryExprPostfix();
        }
        return new UnaryExpr(firstToken,op,e);
    }

    private Expr unaryExprPostfix() throws PLCException {

        Token firstToken = tokens.get(current);
        Expr e;
        PixelSelector selector;

        e = primaryExpr();
        //current++;
        if (match(Token.Kind.LSQUARE)){
            current++;
            selector = pixelSelector();
        }
        else return e;

        return new UnaryExprPostfix(firstToken, e, selector);

    }


    private Expr primaryExpr() throws PLCException {
        Expr finalExpr;
        Token firstToken = tokens.get(current);
        Token.Kind kind = tokens.get(current).getKind();
        switch (kind) {
            case BOOLEAN_LIT -> {
                finalExpr = new BooleanLitExpr(firstToken);
            }
            case STRING_LIT -> {
                finalExpr = new StringLitExpr(firstToken);
            }
            case INT_LIT -> {
                finalExpr = new IntLitExpr(firstToken);
            }
            case FLOAT_LIT -> {
                finalExpr = new FloatLitExpr(firstToken);
            }
            case IDENT -> {
                finalExpr = new IdentExpr(firstToken);
            }
            case LPAREN -> {
                current++;
                Expr expr = expr();
                if (match(Token.Kind.RPAREN)){
                    finalExpr = expr;
                }
                else throw new PLCException("Missing right parenthesis");
            }
            case COLOR_CONST -> {
                finalExpr = new ColorConstExpr(firstToken);
            }
            case LANGLE -> {
                Expr red, green, blue;
                current++;
                red = expr();
                if (match(Token.Kind.COMMA)){
                    current++;
                    green = expr();
                    if (match(Token.Kind.COMMA)){
                        current++;
                        blue = expr();
                    }
                    else throw new PLCException("Missing comma");
                }
                else throw new PLCException("Missing comma");
                if (match(Token.Kind.RANGLE)){
                    finalExpr = new ColorExpr(firstToken, red, green, blue);
                }
                else throw new PLCException("Missing right angle");

            }
            case KW_CONSOLE -> {
                finalExpr = new ConsoleExpr(firstToken);
            }
            default -> throw new SyntaxException("Invalid expression");

        }
        current++;
        return finalExpr;
    }


    private PixelSelector pixelSelector() throws PLCException {

        Token firstToken = tokens.get(current);
        Expr x;
        Expr y;
        x = expr();
        if (match(Token.Kind.COMMA)){
            current++;
            y = expr();
        }
        else throw new PLCException("Missing comma");
        if (match(Token.Kind.RSQUARE)){
            current++;
            return new PixelSelector(firstToken, x, y);
        }
        else throw new PLCException("Missing right bracket");
    }

    private Dimension dimension() throws PLCException {

        Token firstToken = tokens.get(current);
        Expr width;
        Expr height;
        width = expr();
        if (match(Token.Kind.COMMA)){
            current++;
            height = expr();
        }
        else throw new PLCException("Missing comma");
        if (match(Token.Kind.RSQUARE)){
            current++;
            return new Dimension(firstToken, width, height);
        }
        else throw new PLCException("Missing right bracket");
    }

    private Statement statement() throws PLCException {

         Token firstToken = tokens.get(current);
         String name;
         PixelSelector selector = null;
            if (match(Token.Kind.IDENT)){
                name = tokens.get(current).getText();
                current++;
                if (match(Token.Kind.LSQUARE)){
                    current++;
                    selector = pixelSelector();
                }
                if (match(Token.Kind.ASSIGN)){
                        current++;
                        Expr expr = expr();
                        return new AssignmentStatement(firstToken, name, selector, expr);
                }
                else if (match(Token.Kind.LARROW)){
                    current++;
                    Expr source = expr();
                    return new ReadStatement(firstToken, name, selector, source);
                }
                else { throw new SyntaxException("Invalid expression"); }
            }
            else if (match(Token.Kind.KW_WRITE)){
                Expr source;
                Expr dest;
                current++;
                source = expr();
                if (match(Token.Kind.RARROW)){
                    current++;
                    dest = expr();
                    return new WriteStatement(firstToken, source, dest);
                }
                else { throw new PLCException("Expression missing right arrow"); }


            }
            else if (match(Token.Kind.RETURN)){
                current ++;
                Expr expr = expr();
                return new ReturnStatement(firstToken, expr);
            }
            else { throw new SyntaxException("Invalid expression"); }
    }



    public boolean match(Token.Kind kind) {
        if (tokens.get(current).getKind() == kind)
            return true;
        else
            return false;
    }

}
