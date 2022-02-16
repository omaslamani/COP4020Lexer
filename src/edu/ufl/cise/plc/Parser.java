package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;
import java.util.ArrayList;


public class Parser implements IParser {

    private final ArrayList<Token> tokens;
    private int current = 0;


    public Parser(ArrayList<Token> tokens) throws PLCException {
        this.tokens = tokens;
        //parse();

    }

    //FOR TESTING PURPOSES
    public static void main (String args []) throws PLCException {
        Lexer lex = new Lexer("""
                true
                """);
        Parser parser = new Parser(lex.tokens);

        System.out.println(parser.parse());

    }

    @Override
    public ASTNode parse() throws PLCException {
       //  try {
            return unaryExprPostfix();
       //   } catch (ParseError error) {
       //     return null;
       //     }

        }


    private Expr expr() throws PLCException {

        Token.Kind kind = tokens.get(current).getKind();
        switch (kind) {
            //case ConditionalExpr
            case KW_IF -> {
                current++;
                return conditionalExpr();
            }
            // case LogicalOrExpr
            // not sure what kind that would be
            default -> throw new PLCException("Invalid expression");

        }
    }

    private Expr conditionalExpr() throws PLCException {

        Token firstToken = tokens.get(current);
        Expr condition;
        Expr trueCase;
        Expr falseCase;

        //there is probably a cleaner way to do this

        //if kind is left parenthesis
        if (match(Token.Kind.LPAREN)) {
            condition = expr();
            current++;
        }
        else { throw new PLCException("Expression needs left parenthesis"); }


        //if next kind is right parenthesis
        if (match(Token.Kind.RPAREN)) {
            trueCase = expr();
            current++;}
        else { throw new PLCException("Expression needs right parenthesis"); }


        //if next kind is else
        if (match(Token.Kind.KW_ELSE)) {
            falseCase = expr();
            current++;}
        else { throw new PLCException("Expression missing keyword else"); }


        //if next kind is fi
        if (match(Token.Kind.KW_FI)) {
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
            op = tokens.get(current); //this may error because match does current++
            right = logicalAndExpr();
            return new BinaryExpr(firstToken, left, op, right);
        }

        else {
            //not sure what to return if there is no | symbol
        }

        return null;

    }

    private Expr logicalAndExpr() throws PLCException {

        Token firstToken = tokens.get(current);
        Expr left;
        Token op;
        Expr right;

        left = comparisonExpr();
        if (match(Token.Kind.AND)){
            op = tokens.get(current); //this may error because match does current++
            right = comparisonExpr();
            return new BinaryExpr(firstToken, left, op, right);
        }

        else {
            //not sure what to return if there is no & symbol
        }

        return null;

    }

    private Expr comparisonExpr() throws PLCException {

        Token firstToken = tokens.get(current);
        Expr left;
        Token op;
        Expr right;

        left = additiveExpr();
        if (match(Token.Kind.LT) | match(Token.Kind.GT) | match(Token.Kind.EQUALS) | match(Token.Kind.NOT_EQUALS) | match(Token.Kind.LE) | match(Token.Kind.GE)){
            op = tokens.get(current); //this may error because match does current++
            right = additiveExpr();
            return new BinaryExpr(firstToken, left, op, right);
        }

        else {
            //not sure what to return if there is no comparison symbol
        }

        return null;

    }

    private Expr additiveExpr() throws PLCException {

        Token firstToken = tokens.get(current);
        Expr left;
        Token op;
        Expr right;

        left = multiplicativeExpr();
        if (match(Token.Kind.PLUS) | match(Token.Kind.MINUS)){
            op = tokens.get(current); //this may error because match does current++
            right = multiplicativeExpr();
            return new BinaryExpr(firstToken, left, op, right);
        }

        else {
            //not sure what to return if there is no comparison symbol
        }

        return null;

    }

    private Expr multiplicativeExpr() throws PLCException {

        Token firstToken = tokens.get(current);
        Expr left;
        Token op;
        Expr right;

        left = unaryExpr();
        if (match(Token.Kind.TIMES) | match(Token.Kind.DIV) | match(Token.Kind.MOD)){
            op = tokens.get(current); //this may error because match does current++
            right = unaryExpr();
            return new BinaryExpr(firstToken, left, op, right);
        }

        else {
            //not sure what to return if there is no comparison symbol
        }

        return null;

    }

    private Expr unaryExpr() throws PLCException {
        return null;
    }

    private Expr unaryExprPostfix() throws PLCException {

        Token firstToken = tokens.get(current);
        Expr e;
        PixelSelector selector;

        e = primaryExpr();
        current++;
        if (match(Token.Kind.LSQUARE)){
            selector = pixelSelector();
        }
        else return e;

        return new UnaryExprPostfix(firstToken, e, selector);

    }


    private Expr primaryExpr() throws PLCException {
        Token.Kind kind = tokens.get(current).getKind();
        switch (kind) {
            case BOOLEAN_LIT -> {
                return new BooleanLitExpr(tokens.get(current));
            }
            case STRING_LIT -> {
                return new StringLitExpr(tokens.get(current));
            }
            case INT_LIT -> {
                return new IntLitExpr(tokens.get(current));
            }
            case FLOAT_LIT -> {
                return new FloatLitExpr(tokens.get(current));
            }
            case IDENT -> {
                return new IdentExpr(tokens.get(current));
            }
            case LPAREN -> {
                current++;
                Expr expr = expr();
                current++;
                if (match(Token.Kind.RPAREN)){
                        //not sure what to return here
                }
                else throw new PLCException("Missing right parenthesis");
            }

        }

        return null;
    }


    private PixelSelector pixelSelector() throws PLCException {

        Token firstToken = tokens.get(current);
        Expr x;
        Expr y;
        x = expr();
        if (match(Token.Kind.COMMA)){
            y = expr();
        }
        else throw new PLCException("Missing comma");

        return new PixelSelector(firstToken, x, y);
    }


    public boolean match(Token.Kind kind) {
        boolean bool;
        if (tokens.get(current).getKind() == kind)
            bool = true;
        else
            bool = false;
        current++;
        return bool;
    }

}
