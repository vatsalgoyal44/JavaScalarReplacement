package com.scalarreplacement;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import org.apache.commons.lang3.ObjectUtils.Null;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.ast.type.ArrayType.Origin;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.MethodAmbiguityException;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedWildcard;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.SourceFileInfoExtractor;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.symbolsolver.utils.FileUtils;
import com.github.javaparser.utils.Pair;
import com.github.javaparser.utils.SourceRoot;
import com.google.errorprone.annotations.Var;

import javafx.scene.control.Label;
import javassist.bytecode.analysis.ControlFlow.Block;
import javassist.compiler.ast.Expr;

public class ThreeACCode{
    public static int count = 0;
    public static CompilationUnit currCu = null;
    public static CombinedTypeSolver currTypeSolver = null;
    public static NodeList<ImportDeclaration> currImports = null;

    public static void findNonJavaFiles(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        findNonJavaFiles(file); // Recursive call for subdirectories
                    } else {
                        String fileName = file.getName();
                        if (!fileName.endsWith(".java")) {
                            String target = file.getAbsolutePath().toString().replace("h2/src", "h2_new/src");
                            String dir = Paths.get(target).getParent().toString();
                            File directoryNew = new File(dir);
        
                            if (!directoryNew.exists()) {
                                // Create the directory if it doesn't exist
                                boolean created = directoryNew.mkdirs();
                                if (!created) {
                                    System.out.println("Failed to create directory!");
                                    return;
                                }
                            }
                            String source = file.getAbsolutePath().toString();
                            try{
                                Files.copy(file.toPath(), (new File(target)).toPath(), StandardCopyOption.REPLACE_EXISTING);
                            }
                            catch(IOException e){

                            }
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws IOException, ParseException {
        File src = new File("/Users/vatsalgoyal/Documents/Courses/BTP/java_flow_analyser/src/main/java/");

        
        try {
            // Parse the Java file
            CombinedTypeSolver typeSolver = new CombinedTypeSolver();

            typeSolver.add(new ReflectionTypeSolver());
            typeSolver.add(new JavaParserTypeSolver(src));
            typeSolver.add(new JarTypeSolver(new File("/Users/vatsalgoyal/Downloads/javaxservlet.jar")));
            typeSolver.add(new JavaParserTypeSolver("/Users/vatsalgoyal/Documents/Courses/BTP/java_flow_analyser/"+args[0]));
            typeSolver.add(new JavaParserTypeSolver("/Users/vatsalgoyal/Documents/Courses/BTP/java_flow_analyser/jgfutil"));
            typeSolver.add(new JarTypeSolver(new File("/Users/vatsalgoyal/Downloads/javax.xml-1.3.4.jar")));
            Path projectRoot = FileSystems.getDefault().getPath("/Users/vatsalgoyal/Documents/Courses/BTP/java_flow_analyser/"+args[0]);
            typeSolver.add(new JavaParserTypeSolver("/Users/vatsalgoyal/Documents/Courses/BTP/java_flow_analyser/h2/src/main"));
            typeSolver.add(new JarTypeSolver(new File("/Users/vatsalgoyal/.m2/repository/com/github/javaparser/javaparser-core/3.25.5/javaparser-core-3.25.5.jar")));
            typeSolver.add(new JarTypeSolver(new File("/Users/vatsalgoyal/.m2/repository/org/locationtech/jts/jts-core/1.17.0/jts-core-1.17.0.jar")));
            typeSolver.add(new JarTypeSolver(new File("/Users/vatsalgoyal/.m2/repository/jakarta/servlet/jakarta.servlet-api/5.0.0/jakarta.servlet-api-5.0.0.jar")));
            typeSolver.add(new JarTypeSolver(new File("/Users/vatsalgoyal/.m2/repository/org/osgi/org.osgi.service.jdbc/1.1.0/org.osgi.service.jdbc-1.1.0.jar")));
            typeSolver.add(new JarTypeSolver(new File("/Users/vatsalgoyal/.m2/repository/org/apache/lucene/lucene-core/8.5.2/lucene-core-8.5.2.jar")));
            typeSolver.add(new JarTypeSolver(new File("/Library/Java/JavaVirtualMachines/jdk1.8.0_202.jdk/Contents/Home/jre/lib/rt.jar")));
            currTypeSolver = typeSolver;
            SourceFileInfoExtractor sourceFileInfoExtractor = new SourceFileInfoExtractor(typeSolver);
            JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
            StaticJavaParser.getParserConfiguration()
                .setSymbolResolver(symbolSolver);

            
            // Path projectTarget = FileSystems.getDefault().getPath("/Users/vatsalgoyal/Documents/Courses/BTP/java_flow_analyser/h2_new"+args[0]);
            // projectTarget.toFile().mkdirs();
            // findNonJavaFiles(new File(args[0]));
            String[] roots = new String[] {""};
            for (String root : roots) {
                System.out.println(root);
                SourceRoot sourceRoot = new SourceRoot(projectRoot.resolve(root));
                sourceRoot.getParserConfiguration().setSymbolResolver(symbolSolver);
                sourceRoot.tryToParse();
                List<CompilationUnit> compilations = sourceRoot.getCompilationUnits();
                for(CompilationUnit cu : compilations){
                     // Create the visitor and visit the AST
                     currCu = cu;
                    currImports = cu.getImports();
                    Path fileName = cu.getStorage().map(storage -> storage.getPath()).orElse(null);
                    System.out.println(fileName.getFileName());
                    MethodVisitor methodVisitor = new MethodVisitor();
                    ConditionalExprRemover conditionalExprRemover = new ConditionalExprRemover();
                    ConditionalExprRemover1 conditionalExprRemover1 = new ConditionalExprRemover1();
                    ForLoopHandle forLoopHandle = new ForLoopHandle();
                    forLoopHandle.visit(cu, null);
                    conditionalExprRemover1.visit(cu, null);
                    conditionalExprRemover.visit(cu, null);
                    methodVisitor.visit(cu, null);
                    String target = fileName.toString().replace(args[0]+"/", args[0]+"_new/");
                    String dir = Paths.get(target).getParent().toString();
                    File directory = new File(dir);
        
                    if (!directory.exists()) {
                        // Create the directory if it doesn't exist
                        boolean created = directory.mkdirs();
                        if (!created) {
                            System.out.println("Failed to create directory!");
                            return;
                        }
                    }

                    FileWriter myWriter = new FileWriter(target);
                    myWriter.write(cu.toString());
                    myWriter.close();
                }   
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static Type resolveTypetoType(ResolvedType resolvedType){
        // System.out.println(resolvedType);
        if(resolvedType.isArray()){
            ResolvedType componentType = resolvedType.asArrayType().getComponentType();
            Type componentJPType = resolveTypetoType(componentType);
            return new ArrayType(componentJPType, Origin.NAME, new NodeList<>());

        }
        if(resolvedType.isTypeVariable()){
            TypeParameter typeParameter = new TypeParameter(resolvedType.describe());

            return typeParameter;
        }
        if(resolvedType.isReferenceType()){ 
            // System.out.println(resolvedType.asReferenceType().getTypeParametersMap());           
            ClassOrInterfaceType type = (new JavaParser()).parseClassOrInterfaceType(resolvedType.asReferenceType().getQualifiedName()).getResult().orElse(null);
            NodeList<Type> typeArg = new NodeList<Type>();
            for(Pair<ResolvedTypeParameterDeclaration,ResolvedType> pair : resolvedType.asReferenceType().getTypeParametersMap()){
                typeArg.add(resolveTypetoType(pair.b));
            }   
            if(typeArg.isNonEmpty()){
                type.setTypeArguments(typeArg);
            }
            
            return type;
        }
        if(resolvedType.isPrimitive()){
            switch (resolvedType.asPrimitive().describe()) {
                case "int":
                    return PrimitiveType.intType();
                case "boolean":
                    return PrimitiveType.booleanType();
                case "byte":
                    return PrimitiveType.byteType();
                case "double":
                    return PrimitiveType.doubleType();
                case "long":
                    return PrimitiveType.longType();
                case "char":
                    return PrimitiveType.charType();
                case "float":
                    return PrimitiveType.floatType();
                case "short":
                    return PrimitiveType.shortType();
                // Add cases for other primitive types as needed
                default:
                    // Handle unsupported primitive types
                    throw new IllegalArgumentException("Unsupported primitive type: " + resolvedType.asPrimitive().describe());
            }
        }
        if(resolvedType.isWildcard()){
            Type type;
            ResolvedWildcard resolvedWildcard = resolvedType.asWildcard();
            // if (resolvedWildcard.isBounded()) {
            //     if (resolvedWildcard.isUpperBounded()) {
            //         type = new WildcardType(resolveTypetoType(resolvedWildcard.getBoundedType()), WildcardType.BoundType.EXTENDS, resolvedWildcard.getBoundedType().describe());
            //     } else {
            //         type = new WildcardType(null, WildcardType.BoundType.SUPER, resolvedWildcard.getBoundedType().describe());
            //     }
            // } else {
        //     type = new WildcardType();
        //     return type;
        // }
                return new WildcardType();
        }
        return null;
    }

    private static class ForLoopHandle extends VoidVisitorAdapter<Void>{
        public void visit(ForStmt forStmt, Void arg){
            BlockStmt blockStmt = new BlockStmt();
            String label = null;
            if(forStmt.getParentNode().get() instanceof LabeledStmt){
                label = ((LabeledStmt)forStmt.getParentNode().get()).getLabel().asString();
            }
            
            for(Expression expr : forStmt.getInitialization()){
                blockStmt.addStatement(new ExpressionStmt(expr.clone()));
            }
            BlockStmt body = new BlockStmt();
            if(forStmt.getBody() instanceof BlockStmt){
                for(Statement stmt : forStmt.getBody().asBlockStmt().getStatements()){
                    body.addStatement(stmt.clone());
                }
            }
            else{
                body.addStatement(forStmt.getBody().clone());
            }
            for(Expression expr : forStmt.getUpdate()){
                body.addStatement(new ExpressionStmt(expr.clone()));
            }
            WhileStmt whileStmt = null;
            if(forStmt.getCompare().isPresent()){
                whileStmt = new WhileStmt(forStmt.getCompare().orElse(null).clone(), body);
            }
            else{
                whileStmt = new WhileStmt(new BooleanLiteralExpr(true), body);
            }
            if(forStmt.getParentNode().get() instanceof LabeledStmt){
                blockStmt.addStatement(new LabeledStmt(new SimpleName(label), whileStmt));
                if(((LabeledStmt)forStmt.getParentNode().get()).getLabel().asString().equals("outerLoop")){
                    
                }
            }
            else{
                blockStmt.addStatement(whileStmt);
            }
            List<ContinueStmt> continueStmts= body.findAll(ContinueStmt.class);
            for(ContinueStmt continueStmt : continueStmts){
                if(continueStmt.getLabel().isPresent()){
                    if(continueStmt.getLabel().get().asString().equals(label)){
                        for(Expression expr : forStmt.getUpdate()){
                            MethodVisitor.addStatement(continueStmt, new ExpressionStmt(expr.clone()));
                        }
                    }
                    continue;
                }
                Node parent = continueStmt.getParentNode().get();
                while (parent != null && !(parent instanceof ForStmt) && !(parent instanceof WhileStmt) && !(parent instanceof ForEachStmt) && !(parent instanceof DoStmt)) {
                    parent = parent.getParentNode().orElse(null);
                }
                if(parent != whileStmt){
                    continue;
                }
                for(Expression expr : forStmt.getUpdate()){
                    MethodVisitor.addStatement(continueStmt, new ExpressionStmt(expr.clone()));
                }
            }
            if(forStmt.getParentNode().get() instanceof LabeledStmt){
                forStmt.getParentNode().get().replace(blockStmt);
            }
            else{
                forStmt.replace(blockStmt);
            }
            
            super.visit(blockStmt, arg);
            
            
            return;
        }
    }
    private static class ConditionalExprRemover1 extends ModifierVisitor<Void> {
        public LambdaExpr visit(LambdaExpr lambdaExpr, Void arg){
            return lambdaExpr;
        }
        
        public VariableDeclarator visit(VariableDeclarator variableDeclarator, Void arg){
            if(!variableDeclarator.findAll(ConditionalExpr.class).isEmpty()){
                
                Statement statement = variableDeclarator.findAncestor(Statement.class).orElse(null);
                if(statement instanceof TryStmt){
                    return variableDeclarator;
                }
                SimpleName varName = variableDeclarator.getName();
                if(statement == null){
                    return variableDeclarator;
                }
                AssignExpr assignExpr = new AssignExpr(new NameExpr(varName), variableDeclarator.getInitializer().get().clone(), Operator.ASSIGN);
                variableDeclarator.setInitializer(new NullLiteralExpr());
                VariableDeclarationExpr variableDeclarationExpr = variableDeclarator.findAncestor(VariableDeclarationExpr.class).orElse(null);
                Statement newVariableDecl = new ExpressionStmt(assignExpr);
                // variableDeclarationExpr.replace(assignExpr);
                
                boolean added = MethodVisitor.addStatementAfter(statement, newVariableDecl);
                if(added){
                    variableDeclarator.removeInitializer();
                }
                
                return variableDeclarator;
            }
            return variableDeclarator;
        }
    
     }
    private static class ConditionalExprRemover extends VoidVisitorAdapter<Void>  {
        public void visit(LambdaExpr lambdaExpr, Void arg){
            return;
        }
        public void visit(ConditionalExpr conditionalExpr, Void arg){
            Statement statement = conditionalExpr.findAncestor(Statement.class).orElse(null);
            
            if(statement==null || !statement.hasParentNode()){
                return;
            }

            Statement elseStatement = statement.clone();
            Statement thenStatement = statement.clone();
            
            Expression condition = conditionalExpr.getCondition();
            
            for(Expression expr : elseStatement.findAll(Expression.class)){
                if(expr.equals(conditionalExpr)){
                    expr.replace(conditionalExpr.getElseExpr().clone());
                }
            }
            for(Expression expr : thenStatement.findAll(Expression.class)){
                if(expr.equals(conditionalExpr)){
                    expr.replace(conditionalExpr.getThenExpr().clone());
                }
            }

            IfStmt ifStmt = new IfStmt(condition, thenStatement, elseStatement);
            BlockStmt blockStmt = new BlockStmt();
            blockStmt.addStatement(ifStmt);


            // Node parent = statement.getParentNode().get();
            statement.replace(blockStmt);
            
            blockStmt.accept(this, arg);
            return;
           
        }
        
    }

    private static class MethodVisitor extends ModifierVisitor<Void>{

        public static void addStatement(Statement stmt, Statement newStmt){
            Statement parStatement = stmt.findAncestor(Statement.class).orElse(null);
            SwitchEntry switchEntry = stmt.findAncestor(SwitchEntry.class).orElse(null);
            
            if(switchEntry != null && parStatement.isAncestorOf(switchEntry)){
                int index = switchEntry.getStatements().indexOf(stmt);
                switchEntry.addStatement(index, newStmt);
                return;
            }
            if(parStatement instanceof BlockStmt){
                
                
                int index = parStatement.asBlockStmt().getStatements().indexOf(stmt);
                parStatement.asBlockStmt().addStatement(index, newStmt);
            }
            else if(parStatement.getParentNode().get() instanceof BlockStmt){
                BlockStmt blockStmt = new BlockStmt();
                BlockStmt parentBlock = ((BlockStmt)parStatement.getParentNode().get());
                blockStmt.addStatement(newStmt);
                blockStmt.getStatements().add(parStatement);
                
                
                int index = parentBlock.getStatements().indexOf(parStatement);
    
    // Remove the statement to replace
                parentBlock.getStatements().remove(index);
    
    // Add the new block statement at the same index
                parentBlock.getStatements().add(index, blockStmt);
                // System.out.println(parStatement.getParentNode());
                
                parStatement.setParentNode(blockStmt);
                // System.out.println(blockStmt.getParentNode());
            }
            else{
                addStatement(parStatement, newStmt);
            }
            return;
        }


        public static boolean addStatementAfter(Statement stmt, Statement newStmt){
            Statement parStatement = stmt.findAncestor(Statement.class).orElse(null);
            SwitchEntry switchEntry = stmt.findAncestor(SwitchEntry.class).orElse(null);

            // if(parStatement.findAll(Bl))
            
            if(switchEntry != null && parStatement.isAncestorOf(switchEntry)){
                int index = switchEntry.getStatements().indexOf(stmt);
                switchEntry.addStatement(index+1, newStmt);
                return true;
            }
            if(parStatement instanceof BlockStmt){
                
                
                int index = parStatement.asBlockStmt().getStatements().indexOf(stmt);
                parStatement.asBlockStmt().addStatement(index+1, newStmt);
                return true;
                // if(switchEntry!=null){
                //     System.out.println("HIHI");
                //     System.out.println(parStatement.findRootNode());
                // }
                // retStatement = 
            }
             
            return false;
        }
        
        @Override
        public ReturnStmt visit(ReturnStmt returnStmt, Void arg) {
            Expression expression = returnStmt.getExpression().orElse(null);
            if(expression!=null && !(expression instanceof NullLiteralExpr)){
                if(expression.findFirst(ConditionalExpr.class).isPresent()){
                    super.visit(returnStmt, arg);
                    return returnStmt;
                }
                // System.out.println(returnStmt);
                // System.out.println(returnStmt.getParentNode());
                MethodDeclaration methodDeclaration = returnStmt.findAncestor(MethodDeclaration.class).orElse(null);
                LambdaExpr lambdaExpr = returnStmt.findAncestor(LambdaExpr.class).orElse(null);
                Type retType = null;
                if(methodDeclaration == null){
                    ResolvedType resolvedType = expression.calculateResolvedType();
                    retType = ThreeACCode.resolveTypetoType(resolvedType);
                }
                else if(lambdaExpr == null){
                    retType = methodDeclaration.getType();
                }
                else{
                    if(lambdaExpr.isAncestorOf(methodDeclaration)){
                        retType = methodDeclaration.getType();
                    }
                    else{
                        ResolvedType resolvedType = expression.calculateResolvedType();
                        retType = ThreeACCode.resolveTypetoType(resolvedType);
                    }
                }
                VariableDeclarator variableDeclarator = new VariableDeclarator(retType, "t_"+count, expression.clone());
                VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr(variableDeclarator);
                ExpressionStmt expressionStmt = new ExpressionStmt(variableDeclarationExpr);
                // BlockStmt body = returnStmt.findAncestor(BlockStmt.class).orElse(null);
                // List<Statement> statements = body.getStatements();
                // int index = statements.indexOf(returnStmt);
                // body.addStatement(index, expressionStmt); 
                returnStmt.setExpression(new NameExpr("t_"+count));
                count += 1;
                addStatement(returnStmt, expressionStmt);
                super.visit(expressionStmt, arg);
                
            }
            super.visit(returnStmt, arg);
            return returnStmt;
        }

        public LambdaExpr visit(LambdaExpr lambdaExpr, Void arg){
            return lambdaExpr;
        }

        public ThrowStmt visit(ThrowStmt throwStmt, Void arg) {
            Expression expression = throwStmt.getExpression();
            if(expression!=null){
                if(expression.findFirst(ConditionalExpr.class).isPresent()){
                    super.visit(throwStmt, arg);
                    return throwStmt;
                }
                Type type = calculateType(expression);
                VariableDeclarator variableDeclarator = new VariableDeclarator(type, "t_"+count, expression);
                VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr(variableDeclarator);
                ExpressionStmt expressionStmt = new ExpressionStmt(variableDeclarationExpr);
                // BlockStmt body = throwStmt.findAncestor(BlockStmt.class).orElse(null);
                // List<Statement> statements = body.getStatements();
                // int index = statements.indexOf(throwStmt);
                // body.addStatement(index, expressionStmt);
                throwStmt.setExpression(new NameExpr("t_"+count));
                count += 1;
                addStatement(throwStmt, expressionStmt);
                super.visit(expressionStmt, arg);
                
            }
            super.visit(throwStmt, arg);
            return throwStmt;
        }

        public ForEachStmt visit(ForEachStmt forEachStmt, Void arg) {
            
            Expression expression = forEachStmt.getIterable();
            VariableDeclarator variableDeclarator = new VariableDeclarator(calculateType(expression), "t_"+count, expression.clone());
            VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr(variableDeclarator);
            ExpressionStmt expressionStmt = new ExpressionStmt(variableDeclarationExpr);
            // BlockStmt body = forEachStmt.findAncestor(BlockStmt.class).orElse(null);
            // List<Statement> statements = body.getStatements();
            // int index = statements.indexOf(forEachStmt);
            // body.addStatement(index, expressionStmt);
            
            forEachStmt.setIterable(new NameExpr("t_"+count));
            count += 1;
            addStatement(forEachStmt, expressionStmt);
            super.visit(forEachStmt, arg);
            super.visit(expressionStmt, arg);
            return forEachStmt;
        }

        
        public WhileStmt visit(WhileStmt whileStmt, Void arg){
            BlockStmt body;
            if(whileStmt.getBody() instanceof BlockStmt){
                body = whileStmt.getBody().asBlockStmt();
            }
            else{
                body = new BlockStmt();
                body.addStatement(whileStmt.getBody().clone());
            }
            IfStmt ifStmt = new IfStmt(new UnaryExpr(new EnclosedExpr(whileStmt.getCondition().clone()), UnaryExpr.Operator.LOGICAL_COMPLEMENT), new BreakStmt(), null);
            body.addStatement(0, ifStmt);
            whileStmt.setCondition(new BooleanLiteralExpr(true));
            whileStmt.setBody(body);
            super.visit(whileStmt, arg);
            return whileStmt;
        }

        public DoStmt visit(DoStmt doStmt, Void arg){
            BlockStmt body;
            if(doStmt.getBody() instanceof BlockStmt){
                body = doStmt.getBody().asBlockStmt();
            }
            else{
                body = new BlockStmt();
                body.addStatement(doStmt.getBody().clone());
            }
            IfStmt ifStmt = new IfStmt(new UnaryExpr(new EnclosedExpr(doStmt.getCondition().clone()), UnaryExpr.Operator.LOGICAL_COMPLEMENT), new BreakStmt(), null);
            body.addStatement(ifStmt);
            doStmt.setCondition(new BooleanLiteralExpr(true));
            doStmt.setBody(body);
            super.visit(doStmt, arg);
            return doStmt;
        }

        public IfStmt visit(IfStmt ifStmt, Void arg) {
            Expression expression = ifStmt.getCondition();
            VariableDeclarator variableDeclarator = new VariableDeclarator(calculateType(expression), "t_"+count, expression.clone());
            VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr(variableDeclarator);
            ExpressionStmt expressionStmt = new ExpressionStmt(variableDeclarationExpr);
            // BlockStmt body = ifStmt.findAncestor(BlockStmt.class).orElse(null);
            // List<Statement> statements = body.getStatements();
            // int index = statements.indexOf(ifStmt);
            // body.addStatement(index, expressionStmt);
            
            ifStmt.setCondition(new NameExpr("t_"+count));
            addStatement(ifStmt, expressionStmt);
            count += 1;
            super.visit(expressionStmt, arg);
            super.visit(ifStmt, arg);
            
            return ifStmt;
        }

        public AssignExpr visit(AssignExpr assignExpr, Void arg) {
            
            Expression lhs = assignExpr.getTarget();
            Expression rhs = assignExpr.getValue();

            if(rhs instanceof AssignExpr){
                // BlockStmt body = assignExpr.findAncestor(BlockStmt.class).orElse(null);
                Statement statement = assignExpr.findAncestor(Statement.class).orElse(null);
                // List<Statement> statements = body.getStatements();
                // int index = statements.indexOf(statement);
                assignExpr.setValue(((AssignExpr)rhs).getTarget());
                ExpressionStmt newStmt = new ExpressionStmt(rhs);
                addStatement(statement, newStmt);
                // body.addStatement(index, newStmt);
                super.visit(newStmt, arg);
            }
            super.visit(assignExpr, arg);
            return assignExpr;
        }

        // public BlockStmt visit(BlockStmt blockStmt, Void arg){
        //     System.out.println(blockStmt);
        //     super.visit(blockStmt, arg);
        //     return blockStmt;
        // }

        public FieldAccessExpr visit(FieldAccessExpr fieldAccessExpr, Void arg){
            Expression scope = fieldAccessExpr.getScope();
            if(!fieldAccessExpr.findAncestor(BlockStmt.class).isPresent()){
                return fieldAccessExpr;
            }
            
            if(!(scope instanceof NameExpr) && !(scope instanceof ThisExpr)){
                // calculateType(scope).get
                // if(calculateType(scope).asString().equals("org.h2.constraint.Constraint.Type")){
                //     System.out.println(calculateType(scope).asClassOrInterfaceType().get);
                // }
                if(scope.toString().equals(calculateType(scope).toString())){
                    return fieldAccessExpr;
                }
                VariableDeclarator variableDeclarator = new VariableDeclarator(calculateType(scope), "t_"+count, scope.clone());
                VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr(variableDeclarator);
                ExpressionStmt expressionStmt = new ExpressionStmt(variableDeclarationExpr);
                // System.out.println(fieldAccessExpr);
                Statement statement = fieldAccessExpr.findAncestor(Statement.class).orElse(null);
                // BlockStmt body = fieldAccessExpr.findAncestor(BlockStmt.class).orElse(null);
                
                // List<Statement> statements = body.getStatements();
                
                // int index = statements.indexOf(statement);
                // body.addStatement(index, expressionStmt);
                
                fieldAccessExpr.setScope(new NameExpr("t_"+count));
                count+=1;

                addStatement(statement, expressionStmt); 
                
                super.visit(expressionStmt, arg);
                

            }else{
                super.visit(fieldAccessExpr, arg);
            }

            return fieldAccessExpr;
        }
        public ArrayAccessExpr visit(ArrayAccessExpr arrayAccessExpr, Void arg){
            Expression scope = arrayAccessExpr.getName();
            
            if(!(scope instanceof NameExpr)){
                VariableDeclarator variableDeclarator = new VariableDeclarator(calculateType(scope), "t_"+count, scope.clone());
                VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr(variableDeclarator);
                ExpressionStmt expressionStmt = new ExpressionStmt(variableDeclarationExpr);
                Statement statement = arrayAccessExpr.findAncestor(Statement.class).orElse(null);
                // BlockStmt body = arrayAccessExpr.findAncestor(BlockStmt.class).orElse(null);
                
                // List<Statement> statements = body.getStatements();
                
                // int index = statements.indexOf(statement);
                // body.addStatement(index, expressionStmt);
                
                arrayAccessExpr.setName(new NameExpr("t_"+count));
                count+=1;
                addStatement(statement, expressionStmt);
                super.visit(expressionStmt, arg);
                super.visit(arrayAccessExpr, arg);
            }
            
            return arrayAccessExpr;
        }

        

        public MethodCallExpr visit(MethodCallExpr methodCallExpr, Void arg){
            Expression scope = methodCallExpr.getScope().orElse(null);
            // System.out.println(methodCallExpr);
            if(scope!=null && !(scope instanceof NameExpr) && !(scope instanceof ThisExpr) && !(scope instanceof SuperExpr)){
                Type scopeType = calculateType(scope);
                if(scope.toString().equals(scopeType.toString())){
                    return methodCallExpr;
                }
                
                VariableDeclarator variableDeclarator = new VariableDeclarator(scopeType, "t_"+count, scope.clone());
                VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr(variableDeclarator);
                ExpressionStmt expressionStmt = new ExpressionStmt(variableDeclarationExpr);
                Statement statement = methodCallExpr.findAncestor(Statement.class).orElse(null);
                // BlockStmt body = methodCallExpr.findAncestor(BlockStmt.class).orElse(null);
                // System.out.println(statement);
                // List<Statement> statements = body.getStatements();
                // int index = statements.indexOf(statement);
                // body.addStatement(index, expressionStmt);
                // System.out.println(methodCallExpr);
                
                methodCallExpr.setScope(new NameExpr("t_"+count));
                count+=1;
                if(statement != null){
                    addStatement(statement, expressionStmt);
                    super.visit(expressionStmt, arg);
                    for(Expression argExpr : methodCallExpr.getArguments()){
                        if(!(argExpr instanceof NameExpr) || !(argExpr instanceof ThisExpr)){
                            Type argType = calculateType(argExpr);
                            VariableDeclarator variableDeclarator2 = new VariableDeclarator(argType, "t_"+count, argExpr.clone());
                            VariableDeclarationExpr variableDeclarationExpr2 = new VariableDeclarationExpr(variableDeclarator2);
                            ExpressionStmt expressionStmt2 = new ExpressionStmt(variableDeclarationExpr2);
                            argExpr.replace(new NameExpr("t_"+count));
                            count+=1;
                            
                            addStatement(statement, expressionStmt2);
                            
                            super.visit(expressionStmt2, arg);
                        }
                    }
                }
                
                //TODO handle other cases like class declaration
                else{
                    if(methodCallExpr.findAncestor(FieldDeclaration.class).orElse(null)!=null){
                        FieldDeclaration fieldDeclaration = methodCallExpr.findAncestor(FieldDeclaration.class).get();
                        if(fieldDeclaration.getParentNode().get() instanceof NodeWithMembers){
                            FieldDeclaration fieldDeclaration2 = fieldDeclaration.clone();
                            fieldDeclaration2.setVariables(new NodeList<>(variableDeclarator));
                            NodeWithMembers nodeWithMembers = (NodeWithMembers)fieldDeclaration.getParentNode().get();
                            NodeList<BodyDeclaration<?>> fieldDeclarations = nodeWithMembers.getMembers();
                            fieldDeclarations.add(0, fieldDeclaration2);
                            fieldDeclaration2.accept(this, arg);
                        }
                    }
                }
                // if(methodCallExpr.getParentNode().get().get)
                
            }
            super.visit(methodCallExpr, arg);
            return methodCallExpr;
        }

        public InstanceOfExpr visit(InstanceOfExpr instanceOfExpr, Void arg){
            Expression checkExpr = instanceOfExpr.getExpression();
            if(!(checkExpr instanceof NameExpr) && !(checkExpr instanceof ThisExpr)){
                VariableDeclarator variableDeclarator = new VariableDeclarator(calculateType(checkExpr), "t_"+count, checkExpr.clone());
                VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr(variableDeclarator);
                ExpressionStmt expressionStmt = new ExpressionStmt(variableDeclarationExpr);
                // System.out.println(fieldAccessExpr);
                Statement statement = instanceOfExpr.findAncestor(Statement.class).orElse(null);
                // BlockStmt body = fieldAccessExpr.findAncestor(BlockStmt.class).orElse(null);
                
                // List<Statement> statements = body.getStatements();
                
                // int index = statements.indexOf(statement);
                // body.addStatement(index, expressionStmt);
                
                instanceOfExpr.setExpression(new NameExpr("t_"+count));
                count+=1;

                addStatement(statement, expressionStmt); 
                
                super.visit(expressionStmt, arg);
                

            }else{
                super.visit(instanceOfExpr, arg);
            }
            return instanceOfExpr;
        }

        public FieldDeclaration visit(FieldDeclaration fieldDeclaration, Void arg){
            int n = fieldDeclaration.getVariables().size();
            if(n==1){
                super.visit(fieldDeclaration, arg);
                return fieldDeclaration;
            }
            NodeWithMembers nodeWithMembers = (NodeWithMembers)fieldDeclaration.getParentNode().get();
            NodeList<BodyDeclaration<?>> fieldDeclarations = nodeWithMembers.getMembers();
            List<VariableDeclarator> variables = fieldDeclaration.getVariables();
            fieldDeclaration.setVariables(new NodeList<>(fieldDeclaration.getVariable(n-1).clone()));
            super.visit(fieldDeclaration, arg);
            for(int i = 0; i < n-1; i++){
                FieldDeclaration fieldDeclaration2 = fieldDeclaration.clone();
                fieldDeclaration2.setVariables(new NodeList<>(variables.get(i).clone()));
                fieldDeclarations.add(0, fieldDeclaration2);
                fieldDeclaration2.accept(this, arg);
                    
                }
            return fieldDeclaration;
        }

        public VariableDeclarationExpr visit(VariableDeclarationExpr variableDeclarationExpr, Void arg){
            int n = variableDeclarationExpr.getVariables().size();
            if(n == 1){
                super.visit(variableDeclarationExpr, arg);
                return variableDeclarationExpr;
            }
            Statement statement = variableDeclarationExpr.findAncestor(Statement.class).orElse(null);
            // BlockStmt body = variableDeclarationExpr.findAncestor(BlockStmt.class).orElse(null);
                
            // List<Statement> statements = body.getStatements();
            List<VariableDeclarator> variables = variableDeclarationExpr.getVariables();
            variableDeclarationExpr.setVariables(new NodeList<>(variableDeclarationExpr.getVariable(n-1).clone()));
            
            super.visit(variableDeclarationExpr, arg);
            // int index = statements.indexOf(statement);
            for(int i = 0; i < n-1; i++){
                VariableDeclarationExpr variableDeclarationExpr2 = variableDeclarationExpr.clone();
                Statement statement2 = statement.clone();
                for(VariableDeclarationExpr temDeclarationExpr : statement2.findAll(VariableDeclarationExpr.class)){
                    if(temDeclarationExpr.equals(variableDeclarationExpr)){
                        variableDeclarationExpr2.setVariables(new NodeList<>(variables.get(i).clone()));
                        temDeclarationExpr.replace(variableDeclarationExpr2);
                        MethodVisitor.addStatement(statement, statement2);
                        statement2.accept(this, arg);
                        break;
                    }
                }
            }

            return variableDeclarationExpr;
        }


        private static Type calculateType(Expression expr){
            System.out.println(expr);
            if(expr instanceof ConditionalExpr){
                return getConditionalExprReturnType(expr.asConditionalExpr());
            }
            else{
                try{
                
                    return resolveTypetoType(expr.calculateResolvedType());
                }
            
                
                catch(java.lang.IllegalStateException e){
                    return new UnknownType();
                }       
            }
        }

        private static Type getConditionalExprReturnType(ConditionalExpr expr) {
        // Resolve the types of then and else expressions
        ResolvedType thenType = expr.getThenExpr().calculateResolvedType();
        ResolvedType elseType = expr.getElseExpr().calculateResolvedType();
        
        // Determine the common type
        if (thenType.isAssignableBy(elseType)) {
            return resolveTypetoType(thenType);
        } else if (elseType.isAssignableBy(thenType)) {
            return resolveTypetoType(elseType);
        } else {
            // In case no common type found, return Object

            return new UnknownType();
        }
    }


        
    }
}