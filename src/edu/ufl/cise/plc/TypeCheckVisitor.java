package edu.ufl.cise.plc;

import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.ufl.cise.plc.IToken.Kind;
import edu.ufl.cise.plc.ast.ASTNode;
import edu.ufl.cise.plc.ast.ASTVisitor;
import edu.ufl.cise.plc.ast.AssignmentStatement;
import edu.ufl.cise.plc.ast.BinaryExpr;
import edu.ufl.cise.plc.ast.BooleanLitExpr;
import edu.ufl.cise.plc.ast.ColorConstExpr;
import edu.ufl.cise.plc.ast.ColorExpr;
import edu.ufl.cise.plc.ast.ConditionalExpr;
import edu.ufl.cise.plc.ast.ConsoleExpr;
import edu.ufl.cise.plc.ast.Declaration;
import edu.ufl.cise.plc.ast.Dimension;
import edu.ufl.cise.plc.ast.Expr;
import edu.ufl.cise.plc.ast.FloatLitExpr;
import edu.ufl.cise.plc.ast.IdentExpr;
import edu.ufl.cise.plc.ast.IntLitExpr;
import edu.ufl.cise.plc.ast.NameDef;
import edu.ufl.cise.plc.ast.NameDefWithDim;
import edu.ufl.cise.plc.ast.PixelSelector;
import edu.ufl.cise.plc.ast.Program;
import edu.ufl.cise.plc.ast.ReadStatement;
import edu.ufl.cise.plc.ast.ReturnStatement;
import edu.ufl.cise.plc.ast.StringLitExpr;
import edu.ufl.cise.plc.ast.Types.Type;
import edu.ufl.cise.plc.ast.UnaryExpr;
import edu.ufl.cise.plc.ast.UnaryExprPostfix;
import edu.ufl.cise.plc.ast.VarDeclaration;
import edu.ufl.cise.plc.ast.WriteStatement;

import static edu.ufl.cise.plc.ast.Types.Type.*;

public class TypeCheckVisitor implements ASTVisitor {

	SymbolTable symbolTable = new SymbolTable();  
	Program root;
	
	record Pair<T0,T1>(T0 t0, T1 t1){};  //may be useful for constructing lookup tables.


	//FOR TESTING PURPOSES
	public static void main (String args []) throws Exception {
		Lexer lex = new Lexer("""
				image BDP0()
				int size;
				size = 1;
				int Z = 255;
				image[size,size] a;
				a[x,y] = <<(x/8*y/8)%(Z+1), 0, 0>>;
				^ a;
				            """);
		Parser parser = new Parser(lex.tokens);
		TypeCheckVisitor v = new TypeCheckVisitor();
		ASTNode ast = parser.parse();
		ast.visit(v, null);

		System.out.println(ast);

	}
	
	private void check(boolean condition, ASTNode node, String message) throws TypeCheckException {
		if (!condition) {
			throw new TypeCheckException(message, node.getSourceLoc());
		}
	}

	//for assignment statements
	private boolean assignmentCompatible (Type targetType, AssignmentStatement arg) throws Exception {
		Expr expr = arg.getExpr();
		Type exprType = (Type) expr.visit(this, arg);
		if ( (targetType == exprType) || (targetType == INT && exprType == FLOAT) ||(targetType == FLOAT && exprType == INT) || (targetType == INT && exprType == COLOR) ||(targetType == COLOR && exprType == INT))
			return true;
		if (targetType == IMAGE){
			if (exprType == INT) {expr.setCoerceTo(COLOR); return true;}
			if (exprType == FLOAT) {expr.setCoerceTo(COLORFLOAT); return true;}
			if (exprType == COLOR || exprType == COLORFLOAT) return true;
		}
		return false;
	}


	//for declarations
	private boolean assignmentCompatible (Type targetType, VarDeclaration arg) throws Exception {
		Expr expr = arg.getExpr();
		Type exprType = (Type) expr.visit(this, arg);
		if ( (targetType == exprType) || (targetType == INT && exprType == FLOAT) ||(targetType == FLOAT && exprType == INT) || (targetType == INT && exprType == COLOR) ||(targetType == COLOR && exprType == INT))
			return true;
		if (targetType == IMAGE){
			if (exprType == INT) {expr.setCoerceTo(COLOR); return true;}
			if (exprType == FLOAT) {expr.setCoerceTo(COLORFLOAT); return true;}
			if (exprType == COLOR || exprType == COLORFLOAT) return true;
		}
		return false;
	}


	//Stuff left to do
	/*
	Visit program
	Clean up var declaration maybe
	Thursday todo:
	fix varDeclaration assignmentCompatible
	testing

	 */
	
	//The type of a BooleanLitExpr is always BOOLEAN.  
	//Set the type in AST Node for later passes (code generation)
	//Return the type for convenience in this visitor.  
	@Override
	public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
		booleanLitExpr.setType(Type.BOOLEAN);
		return Type.BOOLEAN;
	}

	@Override
	public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
		stringLitExpr.setType(Type.STRING);
		return Type.STRING;
	}

	@Override
	public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
		intLitExpr.setType(Type.INT);
		return Type.INT;
	}

	@Override
	public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {
		floatLitExpr.setType(Type.FLOAT);
		return Type.FLOAT;
	}

	@Override
	public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
		colorConstExpr.setType(COLOR);
		return Type.COLOR;
	}

	@Override
	public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
		consoleExpr.setType(Type.CONSOLE);
		return Type.CONSOLE;
	}
	
	//Visits the child expressions to get their type (and ensure they are correctly typed)
	//then checks the given conditions.
	@Override
	public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
		Type redType = (Type) colorExpr.getRed().visit(this, arg);
		Type greenType = (Type) colorExpr.getGreen().visit(this, arg);
		Type blueType = (Type) colorExpr.getBlue().visit(this, arg);
		check(redType == greenType && redType == blueType, colorExpr, "color components must have same type");
		check(redType == Type.INT || redType == Type.FLOAT, colorExpr, "color component type must be int or float");
		Type exprType = (redType == Type.INT) ? Type.COLOR : Type.COLORFLOAT;
		colorExpr.setType(exprType);
		return exprType;
	}	

	
	
	//Maps forms a lookup table that maps an operator expression pair into result type.  
	//This more convenient than a long chain of if-else statements. 
	//Given combinations are legal; if the operator expression pair is not in the map, it is an error. 
	Map<Pair<Kind,Type>, Type> unaryExprs = Map.of(
			new Pair<Kind,Type>(Kind.BANG,BOOLEAN), BOOLEAN,
			new Pair<Kind,Type>(Kind.MINUS, FLOAT), FLOAT,
			new Pair<Kind,Type>(Kind.MINUS, INT),INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,INT), INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,COLOR), INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,IMAGE), IMAGE,
			new Pair<Kind,Type>(Kind.IMAGE_OP,IMAGE), INT
			);
	
	//Visits the child expression to get the type, then uses the above table to determine the result type
	//and check that this node represents a legal combination of operator and expression type. 
	@Override
	public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws Exception {
		// !, -, getRed, getGreen, getBlue
		Kind op = unaryExpr.getOp().getKind();
		Type exprType = (Type) unaryExpr.getExpr().visit(this, arg);
		//Use the lookup table above to both check for a legal combination of operator and expression, and to get result type.
		Type resultType = unaryExprs.get(new Pair<Kind,Type>(op,exprType));
		check(resultType != null, unaryExpr, "incompatible types for unaryExpr");
		//Save the type of the unary expression in the AST node for use in code generation later. 
		unaryExpr.setType(resultType);
		//return the type for convenience in this visitor.
		return resultType;
	}


	//This method has several cases. Work incrementally and test as you go. 
	@Override
	public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
		Kind op = binaryExpr.getOp().getKind();
		Type leftType = (Type) binaryExpr.getLeft().visit(this, arg);
		Type rightType = (Type) binaryExpr.getRight().visit(this, arg);
		Type resultType = null;
		switch(op) {
			case AND,OR ->{
				check(leftType == BOOLEAN && rightType == BOOLEAN,binaryExpr,"One/Both of the type(s) is not boolean!");
				resultType = BOOLEAN;
			}
			case EQUALS,NOT_EQUALS -> {
				check(leftType == rightType, binaryExpr, "incompatible types for comparison");
				resultType = Type.BOOLEAN;
			}
			case PLUS, MINUS -> {
				if (leftType == Type.INT && rightType == Type.INT) resultType = Type.INT;
				else if (leftType == FLOAT && rightType == FLOAT) resultType = FLOAT;
				else if (leftType == COLOR && rightType == COLOR) resultType = COLOR;
				else if (leftType == COLORFLOAT && rightType == COLORFLOAT) resultType = COLORFLOAT;
				else if (leftType == IMAGE && rightType == IMAGE) resultType = IMAGE;
				else if (leftType == INT && rightType == FLOAT) {binaryExpr.getLeft().setCoerceTo(FLOAT);
				resultType = FLOAT;}
				else if (leftType == FLOAT && rightType == INT) {binaryExpr.getRight().setCoerceTo(FLOAT);
					resultType = FLOAT;}
				else if (leftType == COLOR && rightType == COLORFLOAT) {binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;}
				else if (leftType == COLORFLOAT && rightType == COLOR) {binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;}
				else if (leftType == Type.BOOLEAN && rightType == Type.BOOLEAN) resultType = Type.BOOLEAN;
				else check(false, binaryExpr, "incompatible types for operator");
			}
			case TIMES, DIV, MOD -> {
				if (leftType == Type.INT && rightType == Type.INT) resultType = Type.INT;
				else if (leftType == FLOAT && rightType == FLOAT) resultType = FLOAT;
				else if (leftType == COLOR && rightType == COLOR) resultType = COLOR;
				else if (leftType == COLORFLOAT && rightType == COLORFLOAT) resultType = COLORFLOAT;
				else if (leftType == IMAGE && rightType == IMAGE) resultType = IMAGE;
				else if (leftType == INT && rightType == FLOAT) {binaryExpr.getLeft().setCoerceTo(FLOAT);
					resultType = FLOAT;}
				else if (leftType == FLOAT && rightType == INT) {binaryExpr.getRight().setCoerceTo(FLOAT);
					resultType = FLOAT;}
				else if (leftType == COLOR && rightType == COLORFLOAT) {binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;}
				else if (leftType == COLORFLOAT && rightType == COLOR) {binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;}
				else if (leftType == Type.BOOLEAN && rightType == Type.BOOLEAN) resultType = Type.BOOLEAN;
				else if (leftType == IMAGE && rightType == Type.INT) resultType = IMAGE;
				else if (leftType == IMAGE && rightType == Type.FLOAT) resultType = IMAGE;
				else if (leftType == INT && rightType == COLOR){binaryExpr.getLeft().setCoerceTo(COLOR);
					resultType = COLOR;
				}
				else if (leftType == FLOAT && rightType == COLOR){binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;
				}
				else if (leftType == COLOR && rightType == FLOAT){binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;
				}
				else if (leftType == COLOR && rightType == INT){binaryExpr.getRight().setCoerceTo(COLOR);
					resultType = COLOR;
				}
				else check(false, binaryExpr, "incompatible types for operator");
			}
			case LT, LE, GT, GE -> {
				if (leftType == Type.INT && rightType == Type.INT) resultType = BOOLEAN;
				else if (leftType == FLOAT && rightType == FLOAT) resultType = BOOLEAN;
				else if (leftType == INT && rightType == FLOAT) {binaryExpr.getLeft().setCoerceTo(FLOAT);
					resultType = BOOLEAN;}
				else if (leftType == FLOAT && rightType == INT) {binaryExpr.getRight().setCoerceTo(FLOAT);
					resultType = BOOLEAN;}
				else check(false, binaryExpr, "incompatible types for operator");
			}
			default -> {
				throw new Exception("compiler error");
			}
		}
		binaryExpr.setType(resultType);
		return resultType;

	}

	@Override
	public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
		String name = identExpr.getText();
		Declaration declaration = symbolTable.lookup(name);
		check(declaration != null, identExpr, "Undefined identifier " + name);
		check(declaration.isInitialized(), identExpr, "Using uninitialized variable " + name);
		identExpr.setDec(declaration);
		Type type = declaration.getType();
		identExpr.setType(type);
		return type;
	}

	@Override
	public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {

		Type conditionType = (Type) conditionalExpr.getCondition().visit(this, arg);
		Type trueCaseType = (Type) conditionalExpr.getTrueCase().visit(this, arg);
		Type falseCaseType = (Type) conditionalExpr.getFalseCase().visit(this, arg);

		check(conditionType == BOOLEAN, conditionalExpr, "Condition type must be boolean");
		check(trueCaseType == falseCaseType, conditionalExpr, "True and false cases must be of the same type");

		conditionalExpr.setType(trueCaseType);
		return trueCaseType;
	}

	@Override
	public Object visitDimension(Dimension dimension, Object arg) throws Exception {

		Type widthType = (Type) dimension.getWidth().visit(this, arg);
		Type heightType = (Type) dimension.getHeight().visit(this, arg);

		check(widthType == INT, dimension, "Width must be type int");
		check(heightType == INT, dimension, "Height must be type int");

		return Type.INT;
	}

	@Override
	//This method can only be used to check PixelSelector objects on the right hand side of an assignment. 
	//Either modify to pass in context info and add code to handle both cases, or when on left side
	//of assignment, check fields from parent assignment statement.
	public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
		Type xType = (Type) pixelSelector.getX().visit(this, arg);
		check(xType == Type.INT, pixelSelector.getX(), "only ints as pixel selector components");
		Type yType = (Type) pixelSelector.getY().visit(this, arg);
		check(yType == Type.INT, pixelSelector.getY(), "only ints as pixel selector components");
		return null;
	}

	@Override
	//This method several cases--you don't have to implement them all at once.
	//Work incrementally and systematically, testing as you go.  
	public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
		//TODO:  implement this method
		String name = assignmentStatement.getName();
		Declaration dec = symbolTable.lookup(name);
		String msg = "Undeclared variable " + name;
		check(dec!=null,assignmentStatement,msg);
		symbolTable.lookup(name).setInitialized(true);
		assignmentStatement.setTargetDec(dec);
		//save type of target var (ignore for now)
		Type targetType = dec.getType();


		//make possible x and y for pixel selector
		NameDef defX = null;
		NameDef defY = null;

		if (targetType ==IMAGE){
			if (assignmentStatement.getSelector() == null){
				check(assignmentCompatible(targetType,assignmentStatement),assignmentStatement,"Target type and expression type are not compatible");
			}
			else{
				Token xToken = (Token)assignmentStatement.getSelector().getX().getFirstToken();
				Token yToken = (Token)assignmentStatement.getSelector().getY().getFirstToken();
				check((xToken.getKind() == Kind.IDENT && yToken.getKind() == Kind.IDENT),assignmentStatement, "Kind must be Ident");
				check((symbolTable.lookup(xToken.getText())==null) && (symbolTable.lookup(xToken.getText())==null),assignmentStatement,"PixelSelector vars cannot be global");
				defX = new NameDef(xToken,"int",xToken.getText());
				defY = new NameDef(yToken,"int",yToken.getText());
				VarDeclaration decX = new VarDeclaration(xToken, defX,null,null);
				VarDeclaration decY = new VarDeclaration(yToken, defY,null,null);
				//set type to int
				assignmentStatement.getSelector().getX().setType(INT);
				assignmentStatement.getSelector().getY().setType(INT);

				Type xType = (Type) defX.visit(this, arg);
				Type yType = (Type) defY.visit(this, arg);

				//mark x and y as initialized
				symbolTable.lookup(xToken.getText()).setInitialized(true);
				symbolTable.lookup(yToken.getText()).setInitialized(true);


				assignmentStatement.getSelector().visit(this,arg);
				check(assignmentCompatible(targetType,assignmentStatement),assignmentStatement,"Not compatible");
				assignmentStatement.getExpr().setCoerceTo(COLOR);
				//possibly need to put things in symbol table
			}
		}
		else{
			check(assignmentStatement.getSelector()==null, assignmentStatement,"No pixelSelector allowed here!");
			check(assignmentCompatible(targetType,assignmentStatement),assignmentStatement,"Target type and expression type are not compatible");
		}

		Type exprType = (Type) assignmentStatement.getExpr().visit(this,arg);
		if (defX != null){
			symbolTable.delete(defX.getName());
			symbolTable.delete(defY.getName());
		}


		return exprType;
	}


	@Override
	public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
		Type sourceType = (Type) writeStatement.getSource().visit(this, arg);
		Type destType = (Type) writeStatement.getDest().visit(this, arg);
		check(destType == Type.STRING || destType == Type.CONSOLE, writeStatement,
				"illegal destination type for write");
		check(sourceType != Type.CONSOLE, writeStatement, "illegal source type for write");
		return null;
	}

	@Override
	public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {

		String name = readStatement.getName();
		Type targetType = symbolTable.lookup(name).getType();
		String selectorError = "Read statement cannot have a pixelSelector!";
		check(readStatement.getSelector() == null, readStatement, selectorError);
		boolean rhs = ((Type)readStatement.getSource().visit(this,arg) == CONSOLE) || ((Type)readStatement.getSource().visit(this,arg) == STRING);
		String rhsMsg = "Source must yield a type console or type string";
		check(rhs, readStatement,rhsMsg);
		symbolTable.lookup(name).setInitialized(true);

		return null;
	}

	@Override
	public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {

		String name = declaration.getName();
		String message = "Variable " + name + " already declared!";
		check(symbolTable.lookup(name) == null, declaration,message);

		Type nameDefType = (Type) declaration.getNameDef().visit(this, arg);

		//if type is image
		if (declaration.getType() == IMAGE) {
			if (declaration.getDim() == null)
				check(declaration.getExpr().getType() == IMAGE, declaration, "Initializer expression is not image");
			else {
				Type dimType = (Type) declaration.getDim().visit(this, arg);
				check(dimType == INT, declaration, "Dim type must be int");
			}
		}

		//has initializer
		if (declaration.getOp() != null){
		if (declaration.getOp().getKind() == Kind.ASSIGN){
				check(assignmentCompatible(nameDefType, declaration), declaration, "Type of expression and declared type do not match");
				symbolTable.lookup(name).setInitialized(true);
		}
		else if (declaration.getOp().getKind() == Kind.LARROW){
			declaration.getExpr().visit(this, arg);
			//check(declaration.getExpr() == )
			check(assignmentCompatible(declaration.getType(), declaration), declaration, "Type of expression and declared type do not match");
			symbolTable.lookup(name).setInitialized(true);
		}
		}

		return null;
	}


	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {		
		//TODO:  this method is incomplete, finish it.  
		
		//Save root of AST so return type can be accessed in return statements
		root = program;
		
		//Check declarations and statements
		List<ASTNode> decsAndStatements = program.getDecsAndStatements();
		for (ASTNode node : decsAndStatements) {
			node.visit(this, arg);
		}
		return program;
	}

	@Override
	public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
		symbolTable.insert(nameDef.getName(), nameDef);
		return nameDef.getType();
	}

	@Override
	public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
		symbolTable.insert(nameDefWithDim.getName(), nameDefWithDim);
		return nameDefWithDim.getType();
	}
 
	@Override
	public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
		Type returnType = root.getReturnType();  //This is why we save program in visitProgram.
		Type expressionType = (Type) returnStatement.getExpr().visit(this, arg);
		check(returnType == expressionType, returnStatement, "return statement with invalid type");
		return null;
	}

	@Override
	public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
		Type expType = (Type) unaryExprPostfix.getExpr().visit(this, arg);
		check(expType == Type.IMAGE, unaryExprPostfix, "pixel selector can only be applied to image");
		unaryExprPostfix.getSelector().visit(this, arg);
		unaryExprPostfix.setType(Type.INT);
		unaryExprPostfix.setCoerceTo(COLOR);
		return Type.COLOR;
	}

}
