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
                if (x < 2) x = 5 else x = 6 fi
                """);
        Parser parser = new Parser(lex.tokens);

        System.out.println(parser.parse());

    }

    @Override
    public ASTNode parse() throws PLCException {
       //  try {
            return expr();
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
