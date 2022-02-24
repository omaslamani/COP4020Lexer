package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;
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
                a[x,y] = b
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

            return statement();

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
        Token.Kind kind = tokens.get(current).getKind();
        switch (kind) {
            case BOOLEAN_LIT -> {
                finalExpr = new BooleanLitExpr(tokens.get(current));
            }
            case STRING_LIT -> {
                finalExpr = new StringLitExpr(tokens.get(current));
            }
            case INT_LIT -> {
                finalExpr = new IntLitExpr(tokens.get(current));
            }
            case FLOAT_LIT -> {
                finalExpr = new FloatLitExpr(tokens.get(current));
            }
            case IDENT -> {
                finalExpr = new IdentExpr(tokens.get(current));
            }
            case LPAREN -> {
                current++;
                Expr expr = expr();
                if (match(Token.Kind.RPAREN)){
                    finalExpr = expr;
                }
                else throw new PLCException("Missing right parenthesis");
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
                else { throw new PLCException("Invalid expression"); }
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
            else { throw new PLCException("Invalid expression"); }
    }



    public boolean match(Token.Kind kind) {
        if (tokens.get(current).getKind() == kind)
            return true;
        else
            return false;
    }

}
