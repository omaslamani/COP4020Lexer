package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;

import java.util.Locale;

public class CodeGenVisitor implements ASTVisitor {

    private String packageName;

    public CodeGenVisitor(String packageName) {
        this.packageName = packageName;
    }



    public String boxed(Types.Type type){
        if (type == Types.Type.INT)
            return "Integer";
        if (type == Types.Type.FLOAT)
            return "Float";
        if (type == Types.Type.BOOLEAN)
            return "Boolean";
        if (type == Types.Type.STRING)
            return "String";
        else return null;
    }

    public String lowerCaseString(Types.Type type){
        if (type == Types.Type.STRING)
            return "String";
        else
            return type.toString().toLowerCase();
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        sb.append(booleanLitExpr.getValue());
        return sb;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        sb.append("\"\"\"\n");
        sb.append(stringLitExpr.getValue());
        sb.append("\"\"\"");
        return sb;
    }

    @Override
    public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        Types.Type type = (intLitExpr.getCoerceTo() != null && intLitExpr.getCoerceTo() != Types.Type.INT) ? intLitExpr.getCoerceTo() : intLitExpr.getType();
        if (intLitExpr.getCoerceTo() != null && intLitExpr.getCoerceTo() != Types.Type.INT)
            sb.append("(").append(lowerCaseString(type)).append(") ");
        sb.append(intLitExpr.getValue());
        return sb;
    }

    @Override
    public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        Types.Type type = (floatLitExpr.getCoerceTo() != null && floatLitExpr.getCoerceTo() != Types.Type.FLOAT) ? floatLitExpr.getCoerceTo() : floatLitExpr.getType();
        if (floatLitExpr.getCoerceTo() != null && floatLitExpr.getCoerceTo() != Types.Type.FLOAT)
            sb.append("(").append(lowerCaseString(type)).append(") ");
        sb.append(floatLitExpr.getValue());
        sb.append("f");
        return sb;
    }

    @Override
    public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        sb.append("ConsoleIO.readValueFromConsole(\"");
        //currently handling the rest in read statement
        return sb;
    }

    @Override
    public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpression, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        sb.append(unaryExpression.getOp());
        unaryExpression.getExpr().visit(this, sb);
        return sb;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        Types.Type type = binaryExpr.getType();

        if(type == Types.Type.IMAGE) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
        else {
            sb.append("(").append(lowerCaseString(type)).append(")");
            sb.append("(");
            binaryExpr.getLeft().visit(this, sb);
            sb.append(binaryExpr.getOp().getText());
            binaryExpr.getRight().visit(this, sb);
            sb.append(")");
        }

        return sb;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        Types.Type type = identExpr.getCoerceTo() != null ? identExpr.getCoerceTo() : identExpr.getType();
        //add cast type if applicable
        if (identExpr.getCoerceTo() != null && identExpr.getCoerceTo() != type) {
            sb.append("(").append(lowerCaseString(identExpr.getCoerceTo())).append(")");
        }
        sb.append(identExpr.getText());
        return sb;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        conditionalExpr.getCondition().visit(this, sb);
        sb.append("?");
        conditionalExpr.getTrueCase().visit(this, sb);
        sb.append(":");
        conditionalExpr.getFalseCase().visit(this, sb);
        return sb;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        sb.append(assignmentStatement.getName());
        sb.append("=");
        assignmentStatement.getExpr().visit(this, sb);
        return sb;
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        sb.append("ConsoleIO.console.println(");
        sb.append(writeStatement.getSource().getText()).append(")");
        return sb;
    }

    @Override
    public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        sb.append(readStatement.getName()).append("=");
        //if reading from console then append (object version of type)
        sb.append(" (").append(boxed(readStatement.getTargetDec().getType())).append(") ");
        readStatement.getSource().visit(this, sb);
        //if reading from console
        Types.Type targetType = readStatement.getTargetDec().getType();
        sb.append(targetType).append("\",");
        sb.append("\"Enter ").append(lowerCaseString(targetType)).append(":\")");
        return sb;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws Exception {
        StringBuilder sb = new StringBuilder();
        String typeLowerCase = lowerCaseString(program.getReturnType());
        sb.append("package ").append(packageName).append(";\n");
        sb.append("import edu.ufl.cise.plc.runtime.*; \n");
        sb.append("public class ").append(program.getName()).append("{\n");
        sb.append("public static ").append(typeLowerCase).append(" apply(");
        //append parameters
        for (int i = 0; i < program.getParams().size(); i++){
            program.getParams().get(i).visit(this, sb);
            if(i != program.getParams().size() -1) sb.append(", ");
        }
        sb.append("){\n");
        //append declarations and statements
        for (int i = 0; i < program.getDecsAndStatements().size(); i++){
            sb.append("\t");
            program.getDecsAndStatements().get(i).visit(this, sb);
            sb.append(";");
            if(i != program.getDecsAndStatements().size() -1) sb.append("\n");
        }
        sb.append("\n\t}\n}");

        return sb.toString();
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        String typeLowerCase = lowerCaseString(nameDef.getType());
        sb.append(typeLowerCase).append(" ").append(nameDef.getName());
        return sb;
    }

    @Override
    public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
       throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        Expr expr = returnStatement.getExpr();
        sb.append("return ");
        expr.visit(this, sb);
        return sb;
    }

    @Override
    public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        declaration.getNameDef().visit(this, sb);
        if (declaration.getExpr() != null) {
            sb.append("=");
            declaration.getExpr().visit(this, sb);
        }
        return sb;
    }

    @Override
    public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
