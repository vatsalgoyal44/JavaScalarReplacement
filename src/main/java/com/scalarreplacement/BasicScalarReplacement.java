package com.scalarreplacement;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.SourceFileInfoExtractor;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserFieldDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.Pair;
import com.github.javaparser.utils.SourceRoot;
import com.laamella.javacfa.Flow;
import com.scalarreplacement.PointsToAnalysis.ObjectNode;
import com.scalarreplacement.PointsToAnalysis.PointsToGraph;

import io.vavr.Tuple;
import io.vavr.Tuple1;
import io.vavr.Tuple2;
import javassist.Loader.Simple;
import javassist.bytecode.SignatureAttribute.ClassType;
import javassist.bytecode.analysis.ControlFlow.Block;
import javassist.compiler.ast.Expr;
import javassist.expr.Cast;


import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.checkerframework.checker.units.qual.s;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;

import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.*;

public class BasicScalarReplacement {
    public static Map<String, ClassOrInterfaceDeclaration> classMap = new HashMap<String, ClassOrInterfaceDeclaration>();
    public static Map<String, String> replacementMap = new HashMap<String, String>();
    public static Map<String, Integer> escapeStatusMethodMap = new HashMap<String, Integer>();
    public static Map<String, List<String>> objectReferencesMethodMap = new HashMap<String, List<String>>();
    public static List<String> staticFields = new ArrayList<String>();
    public static EqualitySets equalitySets = new EqualitySets();
    public static Map<MethodDeclaration, Map<Flow, PointsToGraph>> pointsToAnalysisGraphs;
    public static Map<SimpleName, Map<String, Pair<Type, SimpleName>>> staticTypes = new HashMap<SimpleName, Map<String, Pair<Type, SimpleName>>>();
    public static Map<SimpleName, List<SimpleName>> childClass = new HashMap<SimpleName, List<SimpleName>>();
    public static Map<SimpleName, Boolean> ignoreClass = new HashMap<SimpleName, Boolean>();
    public static void main(String[] args) {
        
        File src = new File("/Users/vatsalgoyal/Documents/Courses/BTP/java_flow_analyser/src/main/java/");

        try {
            // Parse the Java file
            CombinedTypeSolver typeSolver = new CombinedTypeSolver();

            typeSolver.add(new ReflectionTypeSolver());
            typeSolver.add(new JavaParserTypeSolver(src));
            typeSolver.add(new JarTypeSolver(new File("/Users/vatsalgoyal/Downloads/javaxservlet.jar")));
            typeSolver.add(new JavaParserTypeSolver("/Users/vatsalgoyal/Documents/Courses/BTP/java_flow_analyser/section3"));
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
            SourceFileInfoExtractor sourceFileInfoExtractor = new SourceFileInfoExtractor(typeSolver);
            JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
            StaticJavaParser.getParserConfiguration()
                .setSymbolResolver(symbolSolver);

            
            // Path projectTarget = FileSystems.getDefault().getPath("/Users/vatsalgoyal/Documents/Courses/BTP/java_flow_analyser/h2_new"+args[0]);
            // projectTarget.toFile().mkdirs();
            ThreeACCode.findNonJavaFiles(new File(args[0]));
            String[] roots = new String[] {""};
            for (String root : roots) {
                SourceRoot sourceRoot = new SourceRoot(projectRoot.resolve(root));
                sourceRoot.getParserConfiguration().setSymbolResolver(symbolSolver);
                sourceRoot.tryToParse();
                List<CompilationUnit> compilations = sourceRoot.getCompilationUnits();
                for(CompilationUnit cu : compilations){
                     // Create the visitor and visit the AST
                    Path fileName = cu.getStorage().map(storage -> storage.getPath()).orElse(null);
                    System.out.println(fileName.getFileName());
                    ClassParameterVisitor classParameterVisitor = new ClassParameterVisitor();
                     classParameterVisitor.visit(cu, classMap);
                    //  System.out.println(staticFields);
                    //  System.out.println(classMap.keySet());
                    //  System.out.println(staticTypes);

                
                }   
            }
            for(SimpleName parentClass : childClass.keySet()){
                if(!classMap.containsKey(parentClass.asString())){
                    List<SimpleName> queue = new ArrayList<SimpleName>();
                    queue.add(parentClass);
                    while(!queue.isEmpty()){
                        SimpleName top = queue.remove(0);
                        ignoreClass.put(top, false);
                        if(childClass.containsKey(top)){
                            for(SimpleName childClasses : childClass.get(top)){
                                queue.add(childClasses);
                            }
                        }
                        
                    }
                    

                }
            }
            for (String root : roots) {
                SourceRoot sourceRoot = new SourceRoot(projectRoot.resolve(root));
                sourceRoot.getParserConfiguration().setSymbolResolver(symbolSolver);
                sourceRoot.tryToParse();
                List<CompilationUnit> compilations = sourceRoot.getCompilationUnits();
                for(CompilationUnit cu : compilations){
                     // Create the visitor and visit the AST
                    Path fileName = cu.getStorage().map(storage -> storage.getPath()).orElse(null);
                    System.out.println(fileName.getFileName());
                     ClassTypeVisitor classTypeVisitor = new ClassTypeVisitor();
                    //  System.out.println(staticFields);
                    //  System.out.println(classMap.keySet());
                    //  System.out.println(staticTypes);
                     classTypeVisitor.visit(cu, null);

                
                }   
            }
            for (String root : roots) {
                SourceRoot sourceRoot = new SourceRoot(projectRoot.resolve(root));
                sourceRoot.getParserConfiguration().setSymbolResolver(symbolSolver);
                sourceRoot.tryToParse();
                List<CompilationUnit> compilations = sourceRoot.getCompilationUnits();
                for(CompilationUnit cu : compilations){
                     // Create the visitor and visit the AST
                    Path fileName = cu.getStorage().map(storage -> storage.getPath()).orElse(null);
                    System.out.println(fileName.getFileName());
                    
                     PointsToAnalysis pointsToAnalysis = new PointsToAnalysis(cu, classMap);
                    //  System.out.println("HIHSIAHF");
                    //  System.out.println(pointsToAnalysis.pointsToAnalysis);
                    pointsToAnalysisGraphs = pointsToAnalysis.pointsToAnalysis;
                     // Create the visitor and visit the AST                     
                     MethodVisitor methodVisitor = new MethodVisitor();
                     List<String> objectNames = new ArrayList<>();            
                     // System.out.println(staticFields);
                     methodVisitor.visit(cu, objectNames);
                    //  System.out.println(cu);

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

            // for (String root : roots) {
            //     SourceRoot sourceRoot = new SourceRoot(projectRoot.resolve(root));
            //     sourceRoot.getParserConfiguration().setSymbolResolver(symbolSolver);
            //     sourceRoot.tryToParse();
            //     List<CompilationUnit> compilations = sourceRoot.getCompilationUnits();
            //     for(CompilationUnit cu : compilations){
            //         PointsToAnalysis pointsToAnalysis = new PointsToAnalysis(cu);
            //         pointsToAnalysisGraphs = pointsToAnalysis.pointsToAnalysis;
            //          // Create the visitor and visit the AST
         
            //          ClassParameterVisitor classParameterVisitor = new ClassParameterVisitor();
            //          ClassTypeVisitor classTypeVisitor = new ClassTypeVisitor();
            //          classParameterVisitor.visit(cu, classMap);
            //          classTypeVisitor.visit(cu, null);
            //          System.out.println(staticTypes);
            //          // System.out.println(staticFields);
            //     }   
            //     for(CompilationUnit cu : compilations){
            //         PointsToAnalysis pointsToAnalysis = new PointsToAnalysis(cu);
            //         pointsToAnalysisGraphs = pointsToAnalysis.pointsToAnalysis;
            //          // Create the visitor and visit the AST                     
            //          MethodVisitor methodVisitor = new MethodVisitor();
            //          List<String> objectNames = new ArrayList<>();            
            //          // System.out.println(staticFields);
            //          methodVisitor.visit(cu, objectNames);
            //          System.out.println(cu);
            //     }   
            // }

        } catch (IOException e) {
            e.printStackTrace();
        }
    
    
      
            
            // List<CompilationUnit> allCU= parseResults.stream().filter(ParseResult::isSuccessful).map(r -> r.getResult().get()).collect(Collectors.toList());
//             Iterator<CompilationUnit> cuIter = allCU.iterator();
//             while (cuIter.hasNext()) {
//             CompilationUnit compilationunit = cuIter.next();
//             //work with compilationunit
// }
            
            
            // CompilationUnit cu = StaticJavaParser.parse(Files.newInputStream(Paths.get(FILE_PATH)));
            // System.out.println(cu);
            
        } 

    private static class ClassTypeVisitor extends VoidVisitorAdapter<Void>{
        @Override
        public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Void arg){
            // List<SimpleName> stack = new ArrayList<SimpleName>();
            // List<SimpleName> queue = new ArrayList<SimpleName>(staticTypes.keySet());

            // while(!queue.isEmpty()){
            //     SimpleName front = queue.remove(0);
            //     stack.add(front);
            //     System.out.println(classMap.get(front.asString()).getExtendedTypes());
            //     while(!classMap.get(front.asString()).getExtendedTypes().isEmpty()){
            //         stack.add(classMap.get(front.asString()).getExtendedTypes(0).getName());
            //         front = classMap.get(front.asString()).getExtendedTypes(0).getName();
            //     }
            //     SimpleName top = stack.remove(stack.size()-1);
            //     while(!stack.isEmpty()){
            //         SimpleName secondLast = stack.remove(stack.size()-1);
            //         for(String field : staticTypes.get(top).keySet()){
            //             if(!staticTypes.get(secondLast).containsKey(field)){
            //                 staticTypes.get(secondLast).put(field, staticTypes.get(top).get(field));
            //             }
            //         }
            //         top = secondLast;
            //     }
            // }
            if(ignoreClass.containsKey(classOrInterfaceDeclaration.getName())){
                return;
            }
            List<SimpleName> allAncestors = new ArrayList<SimpleName>();
            List<SimpleName> queueAncestors = new ArrayList<SimpleName>();
            queueAncestors.add(classOrInterfaceDeclaration.getName());
            while(!queueAncestors.isEmpty()){
                SimpleName top = queueAncestors.remove(0);
                allAncestors.add(top);
                if(childClass.containsKey(top))
                    queueAncestors.addAll(childClass.get(top));
            }

            List<SimpleName> queue = new ArrayList<SimpleName>();
            queue.add(classOrInterfaceDeclaration.getName());
            while(!queue.isEmpty()){
                SimpleName top = queue.remove(0);
                for(String field : staticTypes.get(classOrInterfaceDeclaration.getName()).keySet()){
                    if(!staticTypes.get(top).containsKey(field)){
                        staticTypes.get(top).put(field, staticTypes.get(classOrInterfaceDeclaration.getName()).get(field));
                    }
                    else{
                        SimpleName defClass = staticTypes.get(top).get(field).b;
                        if(queueAncestors.contains(defClass)){
                            continue;
                        }
                        else{
                            staticTypes.get(top).put(field, staticTypes.get(classOrInterfaceDeclaration.getName()).get(field));
                        }
                    }
                }
            }
            
        }
    }
    // Custom visitor class to extract method information
    private static class MethodVisitor extends ModifierVisitor<List<String>> {

        public void updateEscapeStatePTG(PointsToGraph ptg, Map<ObjectNode, Integer> escapeState, ObjectNode node, int target){
            // Updates escape states
            int currVal = escapeState.get(node);
            if(currVal >= target){
                return;
            }
            escapeState.put(node, target);
            for(String field : ptg.fields.get(node).keySet()){
                for(ObjectNode nodeChild : ptg.fields.get(node).get(field)){
                    updateEscapeStatePTG(ptg, escapeState, nodeChild, target);
                }
            }
        }

        public void handleExpressionStatement(Node node, PointsToGraph ptg, Map<ObjectNode, Integer> escapeState){
            // System.out.println(ptg);
            /*
             * Handles the derivation of escape states for all objects
             */
            if(node instanceof ExpressionStmt){
                Expression expression = ((ExpressionStmt)node).getExpression();
                if (expression instanceof AssignExpr) {
                    AssignExpr assignExpr = (AssignExpr) expression;
                    Expression value = assignExpr.getValue();
                    Expression target = assignExpr.getTarget();

                    //a.f = b
                    if(target instanceof FieldAccessExpr && value instanceof NameExpr){
                        // SimpleName fieldName = ((FieldAccessExpr)target).getName();
                        Expression scope = ((FieldAccessExpr)target).getScope();
                        int targetState = 0;
                        if(scope instanceof NameExpr){
                            for(ObjectNode nodeChild : ptg.localVariableMaps.get(((NameExpr)scope).getName())){
                                if(escapeState.get(nodeChild) > targetState){
                                    targetState = escapeState.get(nodeChild);
                                }
                            }
                        }
                        // System.out.println(expression);
                        // System.out.println(ptg.localVariableMaps);
                        if(ptg.localVariableMaps.containsKey(((NameExpr)value).getName())){
                            for(ObjectNode nodeChild : ptg.localVariableMaps.get(((NameExpr)value).getName())){
                                updateEscapeStatePTG(ptg, escapeState, nodeChild, targetState);
                            }
                        }
                        
                    }
                    
                    // a[] = b || a.f[] = b
                    if(target instanceof ArrayAccessExpr && value instanceof NameExpr){
                        ArrayAccessExpr arrayAccessExpr = (ArrayAccessExpr)target;
                        Expression arrayParent = arrayAccessExpr.getName();
                        if(arrayParent instanceof NameExpr){
                            
                            int targetState = 0;
                            if(ptg.localVariableMaps.containsKey(arrayParent.asNameExpr().getName())){
                                for(ObjectNode nodeChild : ptg.localVariableMaps.get(((NameExpr)arrayParent).getName())){
                                    if(escapeState.get(nodeChild) > targetState){
                                        targetState = escapeState.get(nodeChild);
                                    }
                                }
                                for(ObjectNode nodeChild : ptg.localVariableMaps.get(((NameExpr)value).getName())){
                                    updateEscapeStatePTG(ptg, escapeState, nodeChild, targetState);
                                }
                            }
                            else{
                                if(ptg.localVariableMaps.containsKey(value.asNameExpr().getName())){
                                    for(ObjectNode nodeChild : ptg.localVariableMaps.get(((NameExpr)value).getName())){
                                        updateEscapeStatePTG(ptg, escapeState, nodeChild, targetState);
                                    }
                                }
                            }
                        }
                        if(arrayParent instanceof FieldAccessExpr){
                            int targetState = 0;
                            // for(ObjectNode nodeChild : ptg.localVariableMaps.get(((NameExpr)value).getName())){
                            //     if(escapeState.get(nodeChild) > targetState){
                            //         targetState = escapeState.get(nodeChild);
                            //     }
                            // }
                            FieldAccessExpr fieldAccessExpr = (FieldAccessExpr)arrayParent;
                            SimpleName fieldName = fieldAccessExpr.getName();
                            Expression scope = fieldAccessExpr.getScope();

                            if(scope instanceof NameExpr){
                                for(ObjectNode nodeChild : ptg.localVariableMaps.get(((NameExpr)scope).getName())){
                                    for(ObjectNode nodeSubChild : ptg.fields.get(nodeChild).get(fieldName.toString())){
                                        if(escapeState.get(nodeSubChild) > targetState){
                                            targetState = escapeState.get(nodeSubChild);
                                        }
                                    }
                                }
    
                                for(ObjectNode nodeChild : ptg.localVariableMaps.get(((NameExpr)value).getName())){
                                    updateEscapeStatePTG(ptg, escapeState, nodeChild, targetState);
                                }
                            }

                            // for(ObjectNode nodeChild : ptg.localVariableMaps.get(((NameExpr)arrayAccessExpr.getName()).getName())){
                            //     updateEscapeStatePTG(ptg, escapeState, nodeChild, targetState);
                            // }
                        }
                    }
                }
            }
            
            else if(node instanceof ReturnStmt){
                Optional<Expression> retOptional = ((ReturnStmt)node).getExpression();
                if(retOptional.isPresent()){
                    Expression expression = retOptional.get();
                    if(expression instanceof NameExpr){
                        SimpleName var = ((NameExpr)expression).getName();
                        if(ptg.localVariableMaps.containsKey(var)){
                            for(ObjectNode nodeChild : ptg.localVariableMaps.get(var)){
                                updateEscapeStatePTG(ptg, escapeState, nodeChild, 2);
                            }
                        }
                    }
                    else if(expression instanceof FieldAccessExpr){
                        SimpleName fieldName = ((FieldAccessExpr)expression).getName();
                        Expression scope = ((FieldAccessExpr)expression).getScope();
                        for(ObjectNode nodeChild : ptg.localVariableMaps.get(((NameExpr)scope).getName())){
                            for(ObjectNode nodeChild2 : ptg.fields.get(nodeChild).get(fieldName.toString())){
                                updateEscapeStatePTG(ptg, escapeState, nodeChild2, 2);
                            }
                        }   
                    }
                    else if(expression instanceof ArrayAccessExpr){
                        ArrayAccessExpr arrayAccessExpr = (ArrayAccessExpr)expression;
                        Expression arrayParent = arrayAccessExpr.getName();
                        
                        if(arrayParent instanceof NameExpr){
                            for(ObjectNode nodeChild : ptg.localVariableMaps.get(((NameExpr)arrayParent).getName())){
                                for(ObjectNode nodeSubChild : ptg.fields.get(nodeChild).get("[]")){
                                    updateEscapeStatePTG(ptg, escapeState, nodeSubChild, 2);
                                }
                            }
                        }
                        if(arrayParent instanceof FieldAccessExpr){
                            // for(ObjectNode nodeChild : ptg.localVariableMaps.get(((NameExpr)value).getName())){
                            //     if(escapeState.get(nodeChild) > targetState){
                            //         targetState = escapeState.get(nodeChild);
                            //     }
                            // }
                            FieldAccessExpr fieldAccessExpr = (FieldAccessExpr)arrayParent;
                            SimpleName fieldName = fieldAccessExpr.getName();
                            Expression scope = fieldAccessExpr.getScope();

                            if(scope instanceof NameExpr){
                                for(ObjectNode nodeChild : ptg.localVariableMaps.get(((NameExpr)scope).getName())){
                                    for(ObjectNode nodeSubChild : ptg.fields.get(nodeChild).get(fieldName.toString())){
                                        for(ObjectNode nodeSubSubChild : ptg.fields.get(nodeSubChild).get("[]")){
                                            updateEscapeStatePTG(ptg, escapeState, nodeSubSubChild, 2);
                                        }
                                    }
                                }
                            }

                            // for(ObjectNode nodeChild : ptg.localVariableMaps.get(((NameExpr)arrayAccessExpr.getName()).getName())){
                            //     updateEscapeStatePTG(ptg, escapeState, nodeChild, targetState);
                            // }
                        }
                    }
                }
            }
            else{
                List<Expression> expressions = node.findAll(Expression.class);
                    
                    for(Expression expression : expressions){
                        ExpressionStmt expressionStmt = expression.findAncestor(ExpressionStmt.class).orElse(null);
                        if(expressionStmt != null){
                            continue;
                        }
                        // Anything in instance of a local variable assumption
                        if(expression instanceof InstanceOfExpr){
                            if(((InstanceOfExpr)expression).getExpression() instanceof NameExpr){
                                NameExpr nameExpr = (NameExpr)((InstanceOfExpr)expression).getExpression();
                                Type classType = ((InstanceOfExpr)expression).getType();
                                // System.out.println(classType);
                                int score = 0;
                                for(ObjectNode childNode : ptg.localVariableMaps.get(nameExpr.getName())){
                                    if(childNode.isArray){
                                        for(ObjectNode nodeSub : ptg.localVariableMaps.get(nameExpr.getName())){
                                            updateEscapeStatePTG(ptg, escapeState, nodeSub, 1);
                                        }
                                    }
                                    SimpleName objectClass = childNode.classType;
                                    Boolean inc = false;
                                    do{
                                        if(objectClass.asString().equals(classType.getElementType().asString())){
                                            score += 1;
                                            inc = true;
                                        }
                                        // TODO : Handle cases when class is imported
                                        if(classMap.get(objectClass.asString()).getExtendedTypes().size()!=0){
                                            objectClass = classMap.get(objectClass.asString()).getExtendedTypes(0).getName();
                                        }
                                        else{
                                            objectClass = null;
                                        }
                                    }while(objectClass!=null);
                                    if(!inc){
                                        score -=1 ;
                                    }
                                }
                                if(score == ptg.localVariableMaps.get(nameExpr.getName()).size()){
                                    expression.replace(new BooleanLiteralExpr(true));
                                }
                                else if(score == -1 *ptg.localVariableMaps.get(nameExpr.getName()).size()){
                                    expression.replace(new BooleanLiteralExpr(false));
                                }
                                else{
                                    for(ObjectNode nodeSub : ptg.localVariableMaps.get(nameExpr.getName())){
                                        updateEscapeStatePTG(ptg, escapeState, nodeSub, 1);
                                    }
                                }
                            }
                        }
                        if (expression instanceof BinaryExpr){
                        BinaryExpr binaryExpr = (BinaryExpr)expression;
                        BinaryExpr.Operator operator = binaryExpr.getOperator();
                        
                        if(operator == BinaryExpr.Operator.EQUALS){
                            Expression left = binaryExpr.getLeft();
                            Expression right = binaryExpr.getRight();

                            Set<ObjectNode> set_left  = new HashSet<ObjectNode>();
                            Set<ObjectNode> set_right = new HashSet<ObjectNode>();
                            if(left instanceof NameExpr){
                                set_left = ptg.localVariableMaps.get(((NameExpr)left).getName());
                            }
                            else if(left instanceof FieldAccessExpr){
                                Expression scope = ((FieldAccessExpr)left).getScope();
                                SimpleName fieldName = ((FieldAccessExpr)left).getName();

                                // set_right = ptg.fields.get()
                                for(ObjectNode objectNode : ptg.localVariableMaps.get(((NameExpr)scope).getName())){
                                    set_left.addAll(ptg.fields.get(objectNode).get(fieldName.toString()));
                                }
                            }
                            else if(left instanceof ArrayAccessExpr){
                                Expression arrayParent = ((ArrayAccessExpr)left).getName();
                                if(arrayParent instanceof NameExpr){
                                    for(ObjectNode objectNode : ptg.localVariableMaps.get(((NameExpr)arrayParent).getName())){
                                        set_left.addAll(ptg.fields.get(objectNode).get("[]"));
                                    }
                                }
                            }
                            if(right instanceof NameExpr){
                                set_right = ptg.localVariableMaps.get(((NameExpr)right).getName());
                            }
                            else if(right instanceof FieldAccessExpr){
                                Expression scope = ((FieldAccessExpr)right).getScope();
                                SimpleName fieldName = ((FieldAccessExpr)right).getName();

                                // set_right = ptg.fields.get()
                                for(ObjectNode objectNode : ptg.localVariableMaps.get(((NameExpr)scope).getName())){
                                    set_right.addAll(ptg.fields.get(objectNode).get(fieldName.toString()));
                                }
                            }
                            else if(right instanceof ArrayAccessExpr){
                                Expression arrayParent = ((ArrayAccessExpr)right).getName();
                                if(arrayParent instanceof NameExpr){
                                    for(ObjectNode objectNode : ptg.localVariableMaps.get(((NameExpr)arrayParent).getName())){
                                        set_right.addAll(ptg.fields.get(objectNode).get("[]"));
                                    }
                                }
                            }
                            //TODO : a.f == b
                            
                            Set<ObjectNode> intersection = new HashSet<ObjectNode>(set_left);
                            intersection.retainAll(set_right);
                                
                                // System.out.println(set_left);
                                // System.out.println(set_right);

                                // if(set_left.equals(set_right)){
                                //     for(ObjectNode setNode : set_left){
                                //         if(ptg.pointsToAnything.get(setNode)){
                                //             return;
                                //         }
                                //     }
                                //     expression.replace(new BooleanLiteralExpr(true));
                                // }
                            if(intersection.isEmpty()){
                                for(ObjectNode setNode : set_left){
                                            if(ptg.pointsToAnything.get(setNode)){
                                                return;
                                            }
                                        }
                                for(ObjectNode setNode : set_right){
                                            if(ptg.pointsToAnything.get(setNode)){
                                                return;
                                            }
                                        }
                                expression.replace(new BooleanLiteralExpr(false));
                            }
                            else{
                                for(ObjectNode nodeSub : set_left){
                                    updateEscapeStatePTG(ptg, escapeState, nodeSub, 1);
                                }
                                for(ObjectNode nodeSub : set_right){
                                    updateEscapeStatePTG(ptg, escapeState, nodeSub, 1);
                                }
                            }
                            
                        }
                    }
                    }
                
            }
        }

        public void handleFlowReplacement(PointsToGraph ptg, Map<ObjectNode, Integer> escapeState, Map<ObjectNode, Boolean> replaceNode){
            SimpleName firstClass = null;
            for(SimpleName localVar : ptg.localVariableMaps.keySet()){
                if(ptg.localVariableMaps.get(localVar).size() > 1){
                    for(ObjectNode node2 : ptg.localVariableMaps.get(localVar)){
                        replaceNode.put(node2, false);
                    }
                }
                // for(ObjectNode node : ptg.localVariableMaps.get(localVar)){
                //     if(firstClass == null){
                //         firstClass = node.classType;
                //     }
                //     if(!replaceNode.get(node) || (firstClass != null && !firstClass.equals(node.classType))){
                //         for(ObjectNode node2 : ptg.localVariableMaps.get(localVar)){
                //             replaceNode.put(node2, false);
                //         }
                //     }
                // }
            }
        }

        public void getReplaceableNodes(Flow methodFlow, Map<ObjectNode, Integer> escapeState, Map<Flow, PointsToGraph> pointsToMap, Map<ObjectNode, Boolean> replaceNode){
            
            Map<ObjectNode, Boolean> replaceNodeOld = new HashMap<ObjectNode, Boolean>(replaceNode);
            Set<Flow> visited = new HashSet<Flow>();
            List<Flow> workList = new ArrayList<Flow>();
            if(methodFlow.getNext()!=null){
                workList.add(methodFlow.getNext());
                visited.add(methodFlow.getNext());
            }
            if(methodFlow.getMayBranchTo()!=null){
                workList.add(methodFlow.getMayBranchTo());
                visited.add(methodFlow.getMayBranchTo());
            }

            while(!workList.isEmpty()){
                Flow currFlow = workList.remove(0);
                handleFlowReplacement(pointsToMap.get(currFlow), escapeState, replaceNode);
                
                if(currFlow.getNext()!=null){
                    if(!visited.contains(currFlow.getNext())){
                        workList.add(currFlow.getNext());
                        visited.add(currFlow.getNext());
                    }
                }
                if(currFlow.getMayBranchTo()!=null){
                    if(!visited.contains(currFlow.getMayBranchTo())){
                        workList.add(currFlow.getMayBranchTo());
                        visited.add(currFlow.getMayBranchTo());
                    }
                }
            }

            if(replaceNode.equals(replaceNodeOld)){
                return;
            }
            else{
                getReplaceableNodes(methodFlow, escapeState, pointsToMap, replaceNode);
            }
        }
        
        public void setEscapeState(Flow methodFlow, Map<ObjectNode, Integer> escapeState, Map<Flow, PointsToGraph> pointsToMap){
            Set<Flow> visited = new HashSet<Flow>();
            List<Flow> workList = new ArrayList<Flow>();
            if(methodFlow.getNext()!=null){
                workList.add(methodFlow.getNext());
                visited.add(methodFlow.getNext());
            }
            if(methodFlow.getMayBranchTo()!=null){
                workList.add(methodFlow.getMayBranchTo());
                visited.add(methodFlow.getMayBranchTo());
            }

            while(!workList.isEmpty()){
                Flow currFlow = workList.remove(0);
                Node node = currFlow.getNode();
                handleExpressionStatement(node, pointsToMap.get(currFlow), escapeState);
                
                if(currFlow.getNext()!=null){
                    if(!visited.contains(currFlow.getNext())){
                        workList.add(currFlow.getNext());
                        visited.add(currFlow.getNext());
                    }
                }
                if(currFlow.getMayBranchTo()!=null){
                    if(!visited.contains(currFlow.getMayBranchTo())){
                        workList.add(currFlow.getMayBranchTo());
                        visited.add(currFlow.getMayBranchTo());
                    }
                }
            }
        }

        public void addDeclaration(NodeList<Statement> nodeList, String className, String parentClass, boolean addParentClass, String varName, Set<String> alreadyDeclared, Map<SimpleName, Type> localClassType){
            for(ResolvedFieldDeclaration fieldDeclaration : classMap.get(className).resolve().getAllFields()){
            String declaringType = fieldDeclaration.declaringType().getClassName();
            if(declaringType.equals(parentClass) && !addParentClass){
                continue;
            }
            Optional<Node> fieldDec = fieldDeclaration.toAst();
            if(fieldDec.isPresent()){
                FieldDeclaration fieldDeclaration2 = (FieldDeclaration)fieldDec.get();
                for (VariableDeclarator variableDeclarator : fieldDeclaration2.getVariables()){
                    VariableDeclarator variableDeclarator_new = variableDeclarator.clone();
                    String originalName = variableDeclarator.getNameAsString();
                    String newName = "";
                    if(parentClass.equals(declaringType)){
                        newName = varName + "_" + originalName;
                    }
                    else{
                        newName = varName + "_" + declaringType + "_" + originalName;
                    }
                    if(alreadyDeclared.contains(newName)){
                        continue;
                    }
                    variableDeclarator_new.setName(newName);
                    String varType = variableDeclarator.getType().asString();
                    if(classMap.containsKey(varType)){
                        variableDeclarator_new.setInitializer(new NullLiteralExpr());
                    }
                    else{
                        variableDeclarator_new.removeInitializer();
                    }
                    
                    VariableDeclarationExpr variableDeclarationExpr_new = new VariableDeclarationExpr(variableDeclarator_new);
                    ExpressionStmt variablExpressionStmt = new ExpressionStmt(variableDeclarationExpr_new);
                    alreadyDeclared.add(newName);
                    localClassType.put(variableDeclarator_new.getName(), variableDeclarator_new.getType());
                    nodeList.add(variablExpressionStmt);
                    // replacementMap.put(oldName + "." + originalName, varName + "_" + originalName);
                    // if(classMap.containsKey(newType)){
                    //     // System.out.println(oldName + "." + originalName);
                    //     if(escapeStatusMethodMap.get(oldName + "." + originalName) <= 1){
                    //         added += replace(variableDeclarator_new.getNameAsString(), newType, statements, index1, oldName + "." + originalName);
                    //     }
                    // }
                }
            }
        }
        }

        // public void replaceWithList(Node node, NodeList<Statement> nodeList){
        //     Optional<Node> parent = node.getParentNode();
        //     if(parent.isPresent()){
        //         List<Node> childList = parent.get().getChildNodes();
        //         int index = 0;
        //         for(Node childNode : childList){
        //             if(childNode.equals(node)){
        //                 childList.remove(index);
        //                 childNode.repl
        //                 for(Node addNode : nodeList){
        //                     childList.add(index, addNode);
        //                     index += 1;
        //                 }
        //             }
        //             index += 1;
        //         }
        //     }
        // }
        public void handleScalarReplaceStatement(Node node, PointsToGraph ptg, Map<ObjectNode, Integer> escapeState, Map<SimpleName, Set<SimpleName>> localReplaceMap, Map<ObjectNode, Boolean> replaceNode, Map<SimpleName, Boolean> noReplaceMap, Map<SimpleName, Type> localClassType){
            if(node instanceof Statement){
                // List<Expression> expressions = ((Statement)node).findAll(Expression.class);
                if(node instanceof ExpressionStmt){
                    Expression expression = ((ExpressionStmt)node).getExpression();
                    if(expression instanceof VariableDeclarationExpr){
                        
                        BlockStmt body = node.findAncestor(BlockStmt.class).orElse(null);
                        List<Statement> statements = body.getStatements();
                        int index = statements.indexOf(node);
                        VariableDeclarationExpr varDeclarationExpr = (VariableDeclarationExpr)expression;
                        VariableDeclarator variableDeclarator = varDeclarationExpr.getVariable(0);
                        String varType = variableDeclarator.getTypeAsString();
                        if(!classMap.containsKey(varType)){
                            return;
                        }
                        SimpleName localVar = variableDeclarator.getName();
                        localClassType.put(localVar, varDeclarationExpr.getCommonType());
                        NodeList<Statement> nodeList = new NodeList<Statement>();
                        Type type = varDeclarationExpr.getCommonType();
                        VariableDeclarator variableDeclarator_new = variableDeclarator.clone();
                        Optional<Expression> initialExpression = variableDeclarator.getInitializer();
                        boolean currObjectReplace = true;
                        for(ObjectNode node2 : ptg.localVariableMaps.get(localVar)){
                            if(!replaceNode.get(node2)){
                                currObjectReplace = false;
                            }
                        }
                        
                        if(currObjectReplace){
                            variableDeclarator_new.setInitializer(new NullLiteralExpr());
                        }
                        
                        statements.remove(index);
                        if(noReplaceMap.containsKey(localVar)){
                            nodeList.add(new ExpressionStmt(new VariableDeclarationExpr(variableDeclarator_new)));
                        }
                        if(localReplaceMap.containsKey(localVar)){
                            Set<String> declaredReplacements = new HashSet<String>();
                            addDeclaration(nodeList, type.asString(), type.asString(), true, localVar.toString(), declaredReplacements, localClassType);
                            
                            for(SimpleName classSimpleName : localReplaceMap.get(localVar)){
                                
                                addDeclaration(nodeList, classSimpleName.asString(), type.asString(), false, localVar.toString(), declaredReplacements, localClassType);
                            }
                        }
                        if(initialExpression.isPresent()){
                            
                            Expression initExpression = initialExpression.get();
                            
                            if(initExpression instanceof ObjectCreationExpr){
                                
                                ObjectCreationExpr objectCreationExpr = (ObjectCreationExpr)initExpression;
                                SimpleName objectClass = objectCreationExpr.getType().getName();
                                List<Type> types = new ArrayList<Type>();
                                for(Expression argument : objectCreationExpr.getArguments()){
                                    if(argument instanceof NameExpr){
                                        Type argType = localClassType.get(((NameExpr)argument).getName());
                                        types.add(argType);
                                    }
                                    if(argument instanceof FieldAccessExpr){
                                        Expression scope = ((FieldAccessExpr)argument).getScope();
                                        SimpleName field = ((FieldAccessExpr)argument).getName();
                                        if(scope instanceof NameExpr){
                                            Type argType = staticTypes.get(((ClassOrInterfaceType)localClassType.get(((NameExpr)scope).getName())).getName()).get(field.toString()).a;
                                            types.add(argType);
                                        }
                                        else if(scope instanceof ArrayAccessExpr){
                                            if(((ArrayAccessExpr)scope).getName() instanceof NameExpr){
                                                Type arrType = localClassType.get(((NameExpr)((ArrayAccessExpr)scope).getName()).getName()).getElementType();
                                                if(arrType instanceof ClassOrInterfaceType){
                                                    Type argType = staticTypes.get((((ClassOrInterfaceType)arrType).getName())).get(field.asString()).a;
                                                    types.add(argType);
                                                }                                                
                                            }
                                        }
                                    }
                                }
                                if(localReplaceMap.containsKey(localVar) && localReplaceMap.get(localVar).contains(objectClass)){
                                    
                                    for(ResolvedFieldDeclaration fieldDeclaration : classMap.get(objectClass.asString()).resolve().getAllFields()){
                                        String declaringType = fieldDeclaration.declaringType().getClassName();
                                        
                                        Optional<Node> fieldDec = fieldDeclaration.toAst();
                                        if(fieldDec.isPresent()){
                                            FieldDeclaration fieldDeclaration2 = (FieldDeclaration)fieldDec.get();
                                            
                                            for (VariableDeclarator variableDeclaratorInChild : fieldDeclaration2.getVariables()){
                                                if(variableDeclaratorInChild.getInitializer().isPresent()){
                                                    // VariableDeclarator variableDeclarator_newChild = variableDeclaratorInChild.clone();
                                                    
                                                    String originalName = variableDeclaratorInChild.getNameAsString();
                                                    String varName = "";
                                                    if(type.asString().equals(declaringType)){
                                                        varName = localVar.asString() + "_" + originalName;
                                                    }
                                                    else{
                                                        varName = localVar.asString() + "_" + declaringType + "_" + originalName;
                                                    }
                                                    AssignExpr assignExpr = new AssignExpr(new NameExpr(varName), variableDeclaratorInChild.getInitializer().get(), AssignExpr.Operator.ASSIGN);
                                                    // variableDeclarator_newChild.setName(varName);
                                                
                                                    // variableDeclarator_newChild.setInitializer(variableDeclaratorInChild.getInitializer().get());
                                                    // VariableDeclarationExpr variableDeclarationExpr_new = new VariableDeclarationExpr(assignExpr);
                                                    ExpressionStmt variablExpressionStmt = new ExpressionStmt(assignExpr);
                                                    nodeList.add(variablExpressionStmt);
                                                }
                                            }
                                        }
                                    }
                                    
                                    for(ConstructorDeclaration constDec : classMap.get(objectClass.asString()).getConstructors()){
                                        int i = 0;
                                        boolean replace = true;
                                        for(Parameter param : constDec.getParameters()){
                                            if(i < types.size()){
                                                if(!param.getType().equals(types.get(i))){
                                                    replace = false;
                                                    break;
                                                }
                                            }
                                            else{
                                                replace = false;
                                                break;
                                            }
                                            i+=1;
                                        }
                                        
                                        if(replace){
                                            System.out.println("REPLACE");
                                            // TODO : Constructor Inlining
                                            int param_num = 0;
                                            // System.out.println(constDec);
                                            Map<SimpleName, SimpleName> paramMap = new HashMap<SimpleName, SimpleName>();
                                        
                                            for(Parameter param : constDec.getParameters()){
                                                VariableDeclarator variableDeclarator_param = new VariableDeclarator(param.getType(), new SimpleName(constDec.getNameAsString() + "_" + param_num), objectCreationExpr.getArgument(param_num));
                                                nodeList.add(new ExpressionStmt(new VariableDeclarationExpr(variableDeclarator_param)));
                                                
                                                paramMap.put(param.getName(), new SimpleName(constDec.getNameAsString() + "_" + param_num));
                                            }
                                            
                                            BlockStmt constBody = constDec.getBody().clone();
                                            
                                            constBody.findAll(NameExpr.class).forEach(paramName -> {
                                                if(paramMap.containsKey(paramName.getName())){
                                                    paramName.replace(new NameExpr(paramMap.get(paramName.getName()).asString()));
                                                }
                                                
                                                
                                                else if(staticTypes.get(objectClass).containsKey(paramName.getName().toString())){
                                                    SimpleName decClass = staticTypes.get(objectClass).get(paramName.getName().toString()).b;
                                                    NameExpr newName;
                                                    if(objectClass.equals(decClass)){
                                                        newName = new NameExpr(localVar.asString() + "_" + paramName.getName().asString());
                                                    }
                                                    else{
                                                        newName = new NameExpr(localVar.asString() + "_" + decClass.asString() + "_" + paramName.getName().asString());
                                                    }
                                                    paramName.replace(newName);
                                                }
                                            });
                                            //replace this.a
                                            constBody.findAll(FieldAccessExpr.class).forEach(fieldAccessExpr -> {
                                                Expression scope = fieldAccessExpr.getScope();
                                                if(scope instanceof ThisExpr){
                                                    SimpleName field = fieldAccessExpr.getName();
                                                    if(staticTypes.get(objectClass).containsKey(field.toString())){
                                                        SimpleName decClass = staticTypes.get(objectClass).get(field.toString()).b;
                                                        NameExpr newName;
                                                        if(objectClass.equals(decClass)){
                                                            newName = new NameExpr(localVar.asString() + "_" + field.asString());
                                                        }
                                                        else{
                                                            newName = new NameExpr(localVar.asString() + "_" + decClass.asString() + "_" + field.asString());
                                                        }
                                                        fieldAccessExpr.replace(newName);
                                                    }
                                                    

                                                }                
                                            });


                                            // constBody.findAll(FieldAccessExpr.class).forEach(fieldAccess -> {
                                            //     // if(paramMap.containsKey(fieldAccess.getScope())){
                                            //     //     fieldAccess.setScope(new NameExpr(paramMap.get(fieldAccess.getScope()).asString()));
                                            //     // }
                                            //     if(fieldAccess.getScope() instanceof NameExpr && paramMap.containsKey(((NameExpr)fieldAccess.getScope()).getName())){
                                            //         fieldAccess.setScope(new NameExpr(paramMap.get(((NameExpr)fieldAccess.getScope()).getName()).asString()));
                                            //     }
                                            //     // else if(fieldAccess.getScope() instanceof ArrayAccessExpr && paramMap.containsKey(((ArrayAccessExpr)fieldAccess.getScope()).getName())){

                                            //     // }
                                            // });
                                            nodeList.addAll(constBody.getStatements());
                                

                                        }
                                    }
                                }
                            }
                            // else if(initExpression instanceof )
                        }
                    for(Statement statement : nodeList){
                                    statements.add(index, statement);
                                    index += 1;
                                }
                    return;
                    }
                    if(expression instanceof AssignExpr){ 
                        BlockStmt body = node.findAncestor(BlockStmt.class).orElse(null);
                        List<Statement> statements = body.getStatements();
                        int index = statements.indexOf(node);
                        AssignExpr assignExpr = (AssignExpr)expression;
                        Expression left = assignExpr.getTarget();
                        Expression right = assignExpr.getValue();
                        Map<SimpleName, SimpleName> replacedPointsTo = new HashMap<SimpleName, SimpleName>();
                        if(right instanceof FieldAccessExpr){
                            boolean replace = true;
                            Expression scope = ((FieldAccessExpr)right).getScope();
                            SimpleName field = ((FieldAccessExpr)right).getName();
                            SimpleName classType = null;
                            System.out.println(expression);
                            if(ptg.localVariableMaps.containsKey(((NameExpr)scope).getName())){
                                for(ObjectNode node2 : ptg.localVariableMaps.get(((NameExpr)scope).getName())){
                                    classType = node2.classType;
                                    if(!replaceNode.get(node2)){
                                        replace = false;
                                    }
                                }
                            }
                            else{
                                replace = false;
                            }
                            
                            if(replace){
                                // System.out.println(right.toString());
                                // ResolvedType resolvedType = scope.calculateResolvedType();
                                // System.out.println(right.toString() + " is a: " + resolvedType);
                                Type className = localClassType.get(((NameExpr)scope).getName());
                                SimpleName classSimpleName = classMap.get(className.asString()).getName();
                                SimpleName decClass = staticTypes.get(classSimpleName).get(field.asString()).b;
                                NameExpr newName;
                                if(classSimpleName.equals(decClass)){
                                    newName = new NameExpr(((NameExpr)scope).getName().asString() + "_" + field.asString());
                                }
                                else{
                                    newName = new NameExpr(((NameExpr)scope).getName().asString() + "_" + decClass.asString() + "_" + field.asString());
                                }
                                replacedPointsTo.put(newName.getName(), classType);
                                right.replace(newName);
                                right = newName;
                            }
                        }
                        if(left instanceof FieldAccessExpr){
                            boolean replace = true;
                            SimpleName classType = null;
                            Expression scope = ((FieldAccessExpr)left).getScope();
                            SimpleName field = ((FieldAccessExpr)left).getName();
                            if(scope instanceof NameExpr){
                                for(ObjectNode node2 : ptg.localVariableMaps.get(((NameExpr)scope).getName())){
                                    classType = node2.classType;
                                    if(!replaceNode.get(node2)){
                                        replace = false;
                                    }
                                }
                            }
                            else{
                                replace = false;
                            }
                            
                            if(replace){
                                NameExpr newName = new NameExpr(((NameExpr)scope).getName().asString() + "_" + field.asString());
                                replacedPointsTo.put(newName.getName(), classType);
                                left.replace(newName);
                                left = newName;
                            }

                            // TODO : Handle aliasing
                            if(!(scope instanceof NameExpr)){
                                return;
                            }
                            if(ptg.localVariableMaps.get(((NameExpr)scope).getName()).size() == 1){
                                SimpleName currVal = ((NameExpr)scope).getName();
                                ObjectNode nodeBeingUpdated = ptg.localVariableMaps.get(((NameExpr)scope).getName()).iterator().next();
                                for(SimpleName localVarName : ptg.localVariableMaps.keySet()){
                                    if(localVarName.equals(currVal)){
                                        continue;
                                    }
                                    if(replace){
                                        if(ptg.localVariableMaps.get(localVarName).contains(nodeBeingUpdated)){
                                            Type className = localClassType.get(localVarName);
                                            SimpleName classSimpleName = classMap.get(className.asString()).getName();
                                            SimpleName decClass = staticTypes.get(classSimpleName).get(field.asString()).b;
                                            NameExpr newName;
                                            if(classSimpleName.equals(decClass)){
                                                newName = new NameExpr(((NameExpr)scope).getName().asString() + "_" + field.asString());
                                            }
                                            else{
                                                newName = new NameExpr(((NameExpr)scope).getName().asString() + "_" + decClass.asString() + "_" + field.asString());
                                            }

                                            statements.add(index, new ExpressionStmt(new AssignExpr(newName, right, assignExpr.getOperator())));
                                            index += 1;
                                        }   
                                    }
                                }
                            }
                        }
                        if(left instanceof NameExpr || right instanceof NameExpr){
                            boolean replaceLeft = true;
                            boolean replaceRight = true;
                            SimpleName leftName = null;
                            SimpleName rightName = null;

                            if(left instanceof NameExpr){
                                 leftName = ((NameExpr)left).getName();
                                if(ptg.localVariableMaps.containsKey(leftName)){
                                    for(ObjectNode node2 : ptg.localVariableMaps.get(leftName)){
                                        if(!replaceNode.get(node2)){
                                            replaceLeft = false;
                                        }
                                    }
                                    if(ptg.localVariableMaps.get(leftName).size() == 0){
                                        replaceLeft = false;
                                    }
                                }
                                else{
                                    replaceLeft = false;
                                }
                            }
                            else{
                                replaceLeft = false;
                            }
                            
                            if(right instanceof NameExpr){
                                
                                 rightName = ((NameExpr)right).getName();
                                if(ptg.localVariableMaps.containsKey(rightName)){
                                    for(ObjectNode node2 : ptg.localVariableMaps.get(rightName)){
                                        if(!replaceNode.get(node2)){
                                            replaceRight = false;
                                        }
                                    }
                                    if(ptg.localVariableMaps.get(rightName).size() == 0){
                                        replaceRight = false;
                                    }
                                }
                                else{
                                    replaceRight = false;
                                }
                            }
                            else{
                                replaceRight = false;
                            }

                            if(!replaceLeft && !replaceRight){
                                return;
                            }
                            NodeList<Statement> nodeList = new NodeList<Statement>();
                            statements.remove(index);
                            //Only other two cases possible are : left and right both replaceable | right replaceable and left not replaceable
                            if(replaceLeft && replaceRight){
                                String leftType = localClassType.get(leftName).asString();
                                String rightType = localClassType.get(rightName).asString();
                                SimpleName objectType = null;
                                for(ObjectNode object : ptg.localVariableMaps.get(rightName)){
                                    objectType = object.classType;
                                }
                                for(ResolvedFieldDeclaration fieldDeclaration : classMap.get(objectType.asString()).resolve().getAllFields()){
                                        String declaringType = fieldDeclaration.declaringType().getClassName();
                                        
                                        Optional<Node> fieldDec = fieldDeclaration.toAst();
                                        if(fieldDec.isPresent()){
                                            FieldDeclaration fieldDeclaration2 = (FieldDeclaration)fieldDec.get();
                                            
                                            for (VariableDeclarator variableDeclaratorInChild : fieldDeclaration2.getVariables()){
                                                    // VariableDeclarator variableDeclarator_newChild = variableDeclaratorInChild.clone();
                                                    String originalName = variableDeclaratorInChild.getNameAsString();
                                                    String LHS = "";
                                                    if(leftType.equals(declaringType)){
                                                        LHS = leftName + "_" + originalName;
                                                    }
                                                    else{
                                                        LHS = leftName + "_" + declaringType + "_" + originalName;
                                                    }

                                                    String RHS = "";
                                                    if(rightType.equals(declaringType)){
                                                        RHS = rightName + "_" + originalName;
                                                    }
                                                    else{
                                                        RHS = rightName + "_" + declaringType + "_" + originalName;
                                                    }
                                                    AssignExpr newAssignExpr = new AssignExpr(new NameExpr(LHS), new NameExpr(RHS), AssignExpr.Operator.ASSIGN);
                                                    ExpressionStmt variablExpressionStmt = new ExpressionStmt(newAssignExpr);
                                                    nodeList.add(variablExpressionStmt);
                                            }
                                        }
                                    }
                            }
                            if(replaceLeft && !replaceRight){
                                String leftType = localClassType.get(leftName).asString();
                                String rightType = null;
                                
                                SimpleName objectType = null;
                                if(right instanceof FieldAccessExpr){
                                    FieldAccessExpr fieldAccessExpr = (FieldAccessExpr)right;
                                    Expression scope = fieldAccessExpr.getScope();
                                    SimpleName field = fieldAccessExpr.getName();
                                    if(scope instanceof NameExpr){
                                        for(FieldDeclaration fieldDeclaration : classMap.get(((NameExpr)scope).getNameAsString()).getFields()){
                                            if(fieldDeclaration.getVariable(0).getName().equals(field)){
                                                rightType = fieldDeclaration.getCommonType().asString();
                                            }
                                        }
                                        for(ObjectNode nodeChild : ptg.localVariableMaps.get(((NameExpr)scope).getName())){
                                            for(ObjectNode nodeChild2 : ptg.fields.get(nodeChild).get(field.asString())){
                                                if(objectType != null && objectType != nodeChild2.classType){
                                                    System.out.println("ERROR");
                                                }
                                                objectType = nodeChild2.classType;
                                            }
                                        }
                                    }
                                }
                                else if(right instanceof NameExpr){
                                    rightType = localClassType.get(((NameExpr)right).getName()).asString();
                                    // if RHS was not a replaced variable and yet non replaeable, then left must also be non replaceable. Hence here RHS must be a replaced variable
                                    objectType = replacedPointsTo.get(((NameExpr)right).getName());
                                }
                                
                                for(ResolvedFieldDeclaration fieldDeclaration : classMap.get(objectType.asString()).resolve().getAllFields()){
                                        String declaringType = fieldDeclaration.declaringType().getClassName();
                                        
                                        Optional<Node> fieldDec = fieldDeclaration.toAst();
                                        if(fieldDec.isPresent()){
                                            FieldDeclaration fieldDeclaration2 = (FieldDeclaration)fieldDec.get();
                                            
                                            for (VariableDeclarator variableDeclaratorInChild : fieldDeclaration2.getVariables()){
                                                    // VariableDeclarator variableDeclarator_newChild = variableDeclaratorInChild.clone();
                                                    String originalName = variableDeclaratorInChild.getNameAsString();
                                                    String LHS = "";
                                                    if(leftType.equals(declaringType)){
                                                        LHS = leftName + "_" + originalName;
                                                    }
                                                    else{
                                                        LHS = leftName + "_" + declaringType + "_" + originalName;
                                                    }

                                                    String RHS = "";
                                                    if(rightType.equals(declaringType)){
                                                        RHS = rightName + "." + originalName;
                                                    }
                                                    else{
                                                        RHS = String.format("((%s)%s).%s", declaringType, rightName, originalName);                                                        ;
                                                    }
                                                    AssignExpr newAssignExpr = new AssignExpr(new NameExpr(LHS), new NameExpr(RHS), AssignExpr.Operator.ASSIGN);
                                                    ExpressionStmt variablExpressionStmt = new ExpressionStmt(newAssignExpr);
                                                    nodeList.add(variablExpressionStmt);
                                            }
                                        }
                                    }
                            }
                             
                            if(replaceRight && !replaceLeft){
                                String rightType = localClassType.get(rightName).asString();
                                SimpleName objectType = null;
                                for(ObjectNode object : ptg.localVariableMaps.get(rightName)){
                                    objectType = object.classType;
                                }
                                String leftType = "";
                                if(left instanceof FieldAccessExpr){
                                    FieldAccessExpr fieldAccessExpr = (FieldAccessExpr)left;
                                    Expression scope = fieldAccessExpr.getScope();
                                    SimpleName field = fieldAccessExpr.getName();
                                    if(scope instanceof NameExpr){
                                        for(FieldDeclaration fieldDeclaration : classMap.get(((NameExpr)scope).getNameAsString()).getFields()){
                                            if(fieldDeclaration.getVariable(0).getName().equals(field)){
                                                leftType = fieldDeclaration.getCommonType().asString();
                                            }
                                        }
                                    }
                                }
                                else{
                                    leftType = localClassType.get(((NameExpr)left).getName()).asString();
                                }
                                for(ResolvedFieldDeclaration fieldDeclaration : classMap.get(objectType.asString()).resolve().getAllFields()){
                                        String declaringType = fieldDeclaration.declaringType().getClassName();
                                        
                                        Optional<Node> fieldDec = fieldDeclaration.toAst();
                                        if(fieldDec.isPresent()){
                                            FieldDeclaration fieldDeclaration2 = (FieldDeclaration)fieldDec.get();
                                            
                                            for (VariableDeclarator variableDeclaratorInChild : fieldDeclaration2.getVariables()){
                                                    // VariableDeclarator variableDeclarator_newChild = variableDeclaratorInChild.clone();
                                                    String originalName = variableDeclaratorInChild.getNameAsString();
                                                    String LHS = "";
                                                    if(leftType.equals(declaringType)){
                                                        LHS = leftName + "." + originalName;
                                                    }
                                                    else{
                                                        LHS = leftName + "_" + declaringType + "_" + originalName;
                                                        LHS = String.format("((%s)%s).%s", declaringType, leftName, originalName);                
                                                    }

                                                    String RHS = "";
                                                    if(rightType.equals(declaringType)){
                                                        RHS = rightName + "_" + originalName;
                                                    }
                                                    else{
                                                        RHS = rightName + "_" + declaringType + "_" + originalName;                                                      ;
                                                    }
                                                    AssignExpr newAssignExpr = new AssignExpr(new NameExpr(LHS), new NameExpr(RHS), AssignExpr.Operator.ASSIGN);
                                                    ExpressionStmt variablExpressionStmt = new ExpressionStmt(newAssignExpr);
                                                    nodeList.add(variablExpressionStmt);
                                            }
                                        }
                                    }
                            }
                            for(Statement statement : nodeList){
                                    statements.add(index, statement);
                                    index += 1;
                                }
                            return;
                        }
                    }
                }
                else{
                    List<Expression> expressions = node.findAll(Expression.class);
                    
                    for(Expression expression : expressions){
                        ExpressionStmt expressionStmt = expression.findAncestor(ExpressionStmt.class).orElse(null);
                        if(expressionStmt != null){
                            return;
                        }
                        if(expression instanceof FieldAccessExpr){
                                boolean replace = true;
                                Expression scope = ((FieldAccessExpr)expression).getScope();
                                SimpleName field = ((FieldAccessExpr)expression).getName();
                                for(ObjectNode node2 : ptg.localVariableMaps.get(((NameExpr)scope).getName())){
                                    if(!replaceNode.get(node2)){
                                        replace = false;
                                    }
                                }
                                if(replace){
                                    NameExpr newName = new NameExpr(((NameExpr)scope).getName().asString() + "_" + field.asString());
                                    expression.replace(newName);
                                    expression = newName;
                                }
                            }
                    }
                }
                
            }
        }

        public void scalarReplace(Flow methodFlow, Map<ObjectNode, Integer> escapeState, Map<Flow, PointsToGraph> pointsToMap, Map<SimpleName, Set<SimpleName>> localReplaceMap, Map<ObjectNode, Boolean> replaceNode, Map<SimpleName, Boolean> noReplaceMap, Map<SimpleName, Type> localClassType){
            Set<Flow> visited = new HashSet<Flow>();
            List<Flow> workList = new ArrayList<Flow>();
            if(methodFlow.getNext()!=null){
                workList.add(methodFlow.getNext());
                visited.add(methodFlow.getNext());
            }
            if(methodFlow.getMayBranchTo()!=null){
                workList.add(methodFlow.getMayBranchTo());
                visited.add(methodFlow.getMayBranchTo());
            }
            // BlockStmt methodBody = ((MethodDeclaration)methodFlow.getNode()).getBody().orElse(null);
            // NodeList<Statement> statements = ((MethodDeclaration)methodFlow.getNode()).getStatements();
            // for(int i = 0; i < statements.size(); i++){
            //     PointsToGraph ptg = null;
            //     for(Flow flow : pointsToMap.keySet()){
            //         if(flow.getNode() == statements.get(i)){
            //             ptg = pointsToMap.get(flow);
            //         }
            //     }
            //     i = handleScalarReplaceStatement(statements.get(i), ptg, escapeState, localReplaceMap, replaceNode, noReplaceMap, statements);
            // }
            while(!workList.isEmpty()){
                Flow currFlow = workList.remove(0);
                Node node = currFlow.getNode();
                handleScalarReplaceStatement(node, pointsToMap.get(currFlow), escapeState, localReplaceMap, replaceNode, noReplaceMap, localClassType);
                
                if(currFlow.getNext()!=null){
                    if(!visited.contains(currFlow.getNext())){
                        workList.add(currFlow.getNext());
                        visited.add(currFlow.getNext());
                    }
                }
                if(currFlow.getMayBranchTo()!=null){
                    if(!visited.contains(currFlow.getMayBranchTo())){
                        workList.add(currFlow.getMayBranchTo());
                        visited.add(currFlow.getMayBranchTo());
                    }
                }
            }
        }

        public Map<SimpleName, Boolean> localGetClasses(Map<ObjectNode, Boolean> replaceNode, Flow methodFlow, Map<Flow, PointsToGraph> pointsToMap, Map<SimpleName, Set<SimpleName>> localReplaceMap){
            // Sets what local could be replaced with which classes
            Set<Flow> visited = new HashSet<Flow>();
            Map<SimpleName, Boolean> noReplace = new HashMap<SimpleName, Boolean>();
            List<Flow> workList = new ArrayList<Flow>();
            if(methodFlow.getNext()!=null){
                workList.add(methodFlow.getNext());
                visited.add(methodFlow.getNext());
            }
            if(methodFlow.getMayBranchTo()!=null){
                workList.add(methodFlow.getMayBranchTo());
                visited.add(methodFlow.getMayBranchTo());
            }

            while(!workList.isEmpty()){
                Flow currFlow = workList.remove(0);
                for(SimpleName localVar : pointsToMap.get(currFlow).localVariableMaps.keySet()){
                    Boolean allReplace = true;
                    for(ObjectNode node : pointsToMap.get(currFlow).localVariableMaps.get(localVar)){
                        if(!replaceNode.get(node)){
                            allReplace = false;
                            noReplace.put(localVar, true);
                            break;
                        }
                    }
                    if(allReplace){
                        for(ObjectNode node : pointsToMap.get(currFlow).localVariableMaps.get(localVar)){
                            localReplaceMap.putIfAbsent(localVar, new HashSet<SimpleName>());
                            localReplaceMap.get(localVar).add(node.classType);
                        }
                    }
                }
                
                
                if(currFlow.getNext()!=null){
                    if(!visited.contains(currFlow.getNext())){
                        workList.add(currFlow.getNext());
                        visited.add(currFlow.getNext());
                    }
                }
                if(currFlow.getMayBranchTo()!=null){
                    if(!visited.contains(currFlow.getMayBranchTo())){
                        workList.add(currFlow.getMayBranchTo());
                        visited.add(currFlow.getMayBranchTo());
                    }
                }
            }
            return noReplace;
        }    

        @Override
        public MethodDeclaration visit(MethodDeclaration method, List<String> collector) {
            
            // Continue visiting other methods
            super.visit(method, collector);
            // System.out.println("FUNCTION");
            // System.out.println(method.getNameAsString());
            Map<Flow, PointsToGraph> pointsToMap = pointsToAnalysisGraphs.get(method);
            
            if(pointsToMap == null){
                return method;
            }
            
            Map<ObjectNode, Integer> escapeState = new HashMap<ObjectNode, Integer>();
            Map<ObjectNode, Boolean> replaceNode;

            Map<SimpleName, Set<SimpleName>> localReplaceMap = new HashMap<SimpleName, Set<SimpleName>>();
            Flow methodFlow = null;
            for(Flow flow : pointsToMap.keySet()){
                if(flow.getNode().equals(method) == true){
                    methodFlow = flow;
                }
                for(ObjectNode node : pointsToMap.get(flow).fields.keySet()){
                    escapeState.put(node, pointsToMap.get(flow).objectEscapeState.get(node));
                }
            }
            setEscapeState(methodFlow, escapeState, pointsToMap);
            replaceNode = new HashMap<ObjectNode, Boolean>();
            int replaceCount = 0;
            for(ObjectNode node : escapeState.keySet()){
                if(escapeState.get(node) > 0){
                    replaceNode.put(node, false);
                }
                else{
                    replaceNode.put(node, true);
                }
            }
            int classCount = method.findAll(ObjectCreationExpr.class).size();
             
            
            Map<SimpleName, Type> localClassType = new HashMap<SimpleName, Type>();
            getReplaceableNodes(methodFlow, escapeState, pointsToMap, replaceNode);
            for(ObjectNode node : replaceNode.keySet()){
                if(replaceNode.get(node) == true){
                    replaceCount++;
                }
            }
            if(classCount > 0){
                System.out.println(method.getName());
                System.out.println(classCount);   
                System.out.println(replaceCount);
            }
            
            Map<SimpleName, Boolean> noReplaceMap = localGetClasses(replaceNode, methodFlow, pointsToMap, localReplaceMap);
            scalarReplace(methodFlow, escapeState, pointsToMap, localReplaceMap, replaceNode, noReplaceMap, localClassType);
            // for(Flow flow : pointsToMap.keySet()){
            // }
            // extractObjectInitializations(method);
            // // FieldAccessVisitor fieldAccessVisitor = new FieldAccessVisitor();

            // // fieldAccessVisitor.visit(method, null);

            // escapeStatusMethodMap.clear();
            // replacementMap.clear();
            // equalitySets.clear();
            return method;
        }
    }

    
    private static class FieldAccessVisitor extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(FieldAccessExpr fieldAccessExpr, Void arg) {
            super.visit(fieldAccessExpr, arg);

            if(replacementMap.containsKey(fieldAccessExpr.toString())){
                NameExpr nameExpr = new NameExpr(replacementMap.get(fieldAccessExpr.toString()));
                fieldAccessExpr.replace(nameExpr);
            }
        }
    }

    private static class ClassParameterVisitor extends VoidVisitorAdapter<Map<String, ClassOrInterfaceDeclaration>> {
        @Override
        public void visit(ClassOrInterfaceDeclaration classDeclaration, Map<String, ClassOrInterfaceDeclaration> collectorMap) {
            super.visit(classDeclaration, collectorMap);
            // Print class name

            // // Visit and print field declarations (class parameters)
            collectorMap.put(classDeclaration.getNameAsString(), classDeclaration);
            // for (com.github.javaparser.ast.body.VariableDeclarator field : classDeclaration.getFields().get(0).getVariables()) {
            //     System.out.println("Parameter: " + field.getNameAsString());
            //     System.out.println("Type: " + field.getType());
            //     if (field.getInitializer().isPresent()) {
            //         System.out.println("Default Value: " + field.getInitializer().get());
            //     } else {
            //         System.out.println("Default Value: N/A");
            //     }
            // }

            // // Visit and print constructor declarations
            // System.out.println("Constructors:");
            // for (ConstructorDeclaration constructor : classDeclaration.getConstructors()) {
            //     System.out.print(constructor.getNameAsString() + "(");
            //     for (com.github.javaparser.ast.body.Parameter parameter : constructor.getParameters()) {
            //         System.out.print(parameter.getType() + " " + parameter.getNameAsString() + ", ");
            //     }
            //     System.out.println(")");
            // }
            if(classDeclaration.getExtendedTypes().isNonEmpty()){
                childClass.putIfAbsent(classDeclaration.getExtendedTypes(0).getName(), new ArrayList<SimpleName>());
                childClass.get(classDeclaration.getExtendedTypes(0).getName()).add(classDeclaration.getName());
            }
            staticTypes.putIfAbsent(classDeclaration.getName(), new HashMap<String, Pair<Type, SimpleName>>());
            classDeclaration.findAll(FieldDeclaration.class).forEach(field -> {
                if(field.getModifiers().contains(Modifier.staticModifier())){
                    for(VariableDeclarator variable : field.getVariables()){
                        staticFields.add(classDeclaration.getNameAsString() + "." + variable.getNameAsString());
                    }
                    // if(staticFields.containsKey(classDeclaration.getNameAsString())){
                    //     for(VariableDeclarator variable : field.getVariables()){
                    //         staticFields.get(classDeclaration.getNameAsString()).add(variable.getNameAsString());
                    //     }
                    // }
                    // else{
                    //     List<String> arrList = new ArrayList<String>();
                    //     for(VariableDeclarator variable : field.getVariables()){
                    //         arrList.add(classDeclaration.getNameAsString() + "." + variable.getNameAsString());
                    //     }
                    //     staticFields.add(classDeclaration.getNameAsString(), arrList);
                    // }
                }
                
                staticTypes.get(classDeclaration.getName()).put(field.getVariable(0).getName().asString(), new Pair<Type, SimpleName>(field.getCommonType(), classDeclaration.getName()));
            });
        }
    }
    
    private static void updateEscapeStateHelper(String varName, Integer target, List<String> visited){
        if(visited.contains(varName)){
            return;
        }
        escapeStatusMethodMap.put(varName, target);
        visited.add(varName);
        if (objectReferencesMethodMap.containsKey(varName)){
            for (String varReferenced : objectReferencesMethodMap.get(varName)){
                if(target > escapeStatusMethodMap.get(varReferenced)){
                    updateEscapeStateHelper(varReferenced, target, visited);
                }
            }
        }
    }
    private static void updateEscapeState(String varName, Integer target){
        List<String> visited = new ArrayList<String>();
        updateEscapeStateHelper(varName, target, visited);
    }

    private static void handleNewDeclaration(String name, String type, int target){        
        escapeStatusMethodMap.put(name, target);
        
        if (classMap.containsKey(type)){
            ClassOrInterfaceDeclaration classDeclaration = classMap.get(type);
            objectReferencesMethodMap.putIfAbsent(name, new ArrayList<String>());
            for(ResolvedFieldDeclaration fieldDeclaration : classDeclaration.resolve().getAllFields()){
                String declaringType = fieldDeclaration.declaringType().getClassName();
                Optional<Node> fieldDec = fieldDeclaration.toAst();
                if(fieldDec.isPresent()){
                    FieldDeclaration fieldDeclaration2 = (FieldDeclaration)fieldDec.get();
                    for (VariableDeclarator variableDeclarator : fieldDeclaration2.getVariables()){
                        String typeChild = variableDeclarator.getTypeAsString();
                        String tempname = name;
                        if(!declaringType.equals(type)){
                            tempname = "(("+declaringType+")"+tempname + ")";
                        }
                        String nameChild = tempname + "." + variableDeclarator.getNameAsString();
                        // System.out.println(nameChild);;
                        handleNewDeclaration(nameChild, typeChild, target);
                        objectReferencesMethodMap.get(name).add(nameChild);
                    }
                }
        }
            // for (FieldDeclaration fieldDeclaration : classDeclaration.getFields()){            
            //     for (VariableDeclarator variableDeclarator : fieldDeclaration.getVariables()){
            //         String typeChild = variableDeclarator.getTypeAsString();
            //         String nameChild = name + "." + variableDeclarator.getNameAsString();
            //         handleNewDeclaration(nameChild, typeChild, target);
            //         objectReferencesMethodMap.get(name).add(nameChild);
            //     }
            // }
        }
        
    }

    private static int replace(String varName, String classType, List<Statement> statements, int index1, String oldName){
        int added = 0;
        statements.remove(index1);
        added -= 1;
        for(ResolvedFieldDeclaration fieldDeclaration : classMap.get(classType).resolve().getAllFields()){
            String declaringType = fieldDeclaration.declaringType().getClassName();
            Optional<Node> fieldDec = fieldDeclaration.toAst();
            if(fieldDec.isPresent()){
                FieldDeclaration fieldDeclaration2 = (FieldDeclaration)fieldDec.get();
                for (VariableDeclarator variableDeclarator : fieldDeclaration2.getVariables()){
                    VariableDeclarator variableDeclarator_new = variableDeclarator.clone();
                    String originalName = variableDeclarator.getNameAsString();
                    String newType = variableDeclarator.getTypeAsString();
                    String newName = "";
                    if(classType.equals(declaringType)){
                        newName = varName + "_" + originalName;
                    }
                    else{
                        newName = varName + "_" + declaringType + "_" + originalName;
                    }
                    variableDeclarator_new.setName(newName);
                    VariableDeclarationExpr variableDeclarationExpr_new = new VariableDeclarationExpr(variableDeclarator_new);
                    ExpressionStmt variablExpressionStmt = new ExpressionStmt(variableDeclarationExpr_new);
                    statements.add(index1, variablExpressionStmt);
                    added += 1;
                    replacementMap.put(oldName + "." + originalName, varName + "_" + originalName);
                    if(classMap.containsKey(newType)){
                        // System.out.println(oldName + "." + originalName);
                        if(escapeStatusMethodMap.get(oldName + "." + originalName) <= 1){
                            added += replace(variableDeclarator_new.getNameAsString(), newType, statements, index1, oldName + "." + originalName);
                        }
                    }
                }
            }
        }
        return added;
    }
    private static int scalarReplace(List<Statement> statements, int index){
        Statement statement = statements.get(index);
        if(statement instanceof ExpressionStmt){
            Expression expression = ((ExpressionStmt)statement).getExpression();
            if(expression instanceof VariableDeclarationExpr){
                VariableDeclarationExpr variableDeclarationExpr = (VariableDeclarationExpr)expression;
                for(int i = 0; i < variableDeclarationExpr.getVariables().size(); i++){
                    VariableDeclarator variableDeclarator = variableDeclarationExpr.getVariable(i);
                    if(variableDeclarator.getInitializer().isPresent() && variableDeclarator.getInitializer().get() instanceof ObjectCreationExpr){
                        ObjectCreationExpr objectCreationExpr = (ObjectCreationExpr)variableDeclarator.getInitializer().get();
                        String classType = variableDeclarator.getTypeAsString();
                        String varName = variableDeclarator.getNameAsString();
                        if(escapeStatusMethodMap.get(varName) <= 1){
                            return replace(varName, classType, statements, index, varName);
                        }
                    }
                }
            }
        }
        List<Expression> expressions = statement.findAll(Expression.class);
        for (Expression expression : expressions){
            if(expression instanceof FieldAccessExpr){
                FieldAccessExpr fieldAccessExpr = (FieldAccessExpr)expression;
                if(replacementMap.containsKey(fieldAccessExpr.toString())){
                    NameExpr nameExpr = new NameExpr(replacementMap.get(fieldAccessExpr.toString()));
                    fieldAccessExpr.replace(nameExpr);
                }
            }
            if(expression instanceof CastExpr){
                CastExpr castExpr = (CastExpr) expression;
                Expression castSubExpression = castExpr.getExpression();
        //     if(castSubExpression instanceof ArrayAccessExpr){
        //         castSubExpression = ((ArrayAccessExpr)castSubExpression).getName();
        //     }
            if (escapeStatusMethodMap.containsKey(castSubExpression.toString())){
                // System.out.println(castSubExpression);
            }}
        }
        return 0;
    }

    private static void handleExpression(Expression expression){
        if (expression instanceof VariableDeclarationExpr) {
            VariableDeclarationExpr assignExpr = (VariableDeclarationExpr) expression;
            // if(assignExpr.getVariable(0).getInitializer().get() instanceof ObjectCreationExpr){

            //     ObjectCreationExpr objectCreationExpr = (ObjectCreationExpr)assignExpr.getVariable(0).getInitializer().get();
            //     // methodBody.getStatements().remove(i); //Removing the object creating statement
            //     // classReplaced.put(assignExpr.getVariable(0).getNameAsString(), objectCreationExpr.getTypeAsString());
                
            //     // for(FieldDeclaration fieldDeclaration : classMap.get(objectCreationExpr.getTypeAsString()).getFields()){                        
            //         // for (VariableDeclarator variableDeclarator : fieldDeclaration.getVariables()){
            //         //     // VariableDeclarator variableDeclarator_new = variableDeclarator.clone();
            //         //     // String originalName = variableDeclarator.getNameAsString();
            //         //     // variableDeclarator_new.setName(assignExpr.getVariable(0).getNameAsString() + "_" + originalName);
            //         //     // System.out.println(variableDeclarator_new);
            //         //     // VariableDeclarationExpr variableDeclarationExpr_new = new VariableDeclarationExpr(variableDeclarator_new);
            //         //     // ExpressionStmt variablExpressionStmt = new ExpressionStmt(variableDeclarationExpr_new);
            //         //     // expressions.add(i, variablExpressionStmt)
            //         //     
            //         // }
            //     // }
            // escapeStatusMethodMap.put(assignExpr.getVariable(0).getNameAsString(), 0);
            // handleNewDeclaration(assignExpr.getVariable(0).getNameAsString(), objectCreationExpr.getTypeAsString());
            // }
            for (VariableDeclarator variableDeclarator : assignExpr.getVariables()){
                Optional<Expression> initializerExpression = variableDeclarator.getInitializer();
                int target = 0;
                if (initializerExpression.isPresent()){
                    Expression initializer = initializerExpression.get();
                    if(escapeStatusMethodMap.containsKey(initializer.toString())){
                        objectReferencesMethodMap.putIfAbsent(variableDeclarator.getNameAsString(), new ArrayList<String>());
                        objectReferencesMethodMap.putIfAbsent(initializer.toString(), new ArrayList<String>());
                        objectReferencesMethodMap.get(variableDeclarator.getNameAsString()).add(initializer.toString());
                        objectReferencesMethodMap.get(initializer.toString()).add(variableDeclarator.getNameAsString());
                        target = escapeStatusMethodMap.get(initializer.toString());
                        equalitySets.addPair(variableDeclarator.getNameAsString(), initializer.toString());
                    }
                    else if(initializer instanceof ArrayCreationExpr){
                        Optional<ArrayInitializerExpr> optionalArrayInitializer = ((ArrayCreationExpr)initializer).getInitializer();
                        if(!optionalArrayInitializer.isPresent()){
                            NodeList<ArrayCreationLevel> arrayLevels = ((ArrayCreationExpr)initializer).getLevels();
                            for (ArrayCreationLevel level : arrayLevels){
                                Optional<Expression> creationExpression = level.getDimension();
                                if(!creationExpression.isPresent()){
                                    target = 2;
                                }
                            }
                        }
                        equalitySets.addPair(variableDeclarator.getNameAsString(), variableDeclarator.getNameAsString());
                    }
                    else if(initializer instanceof ObjectCreationExpr){
                        equalitySets.addPair(variableDeclarator.getNameAsString(), variableDeclarator.getNameAsString());
                    }
                }
                escapeStatusMethodMap.put(variableDeclarator.getNameAsString(), target);
                handleNewDeclaration(variableDeclarator.getNameAsString(), assignExpr.getCommonType().asString(), target);
            }
        }

        if (expression instanceof AssignExpr) {
            AssignExpr assignExpr = (AssignExpr) expression;
            Expression value = assignExpr.getValue();
            Expression target = assignExpr.getTarget();

            if (escapeStatusMethodMap.containsKey(value.toString()) && escapeStatusMethodMap.containsKey(target.toString())){
                String rhs = value.toString();
                String lhs = target.toString();
                equalitySets.addPair(lhs, rhs);
            }
            if(value instanceof ArrayAccessExpr){
                value = ((ArrayAccessExpr)value).getName();
                if(escapeStatusMethodMap.containsKey(target.toString())){
                    String lhs = target.toString();
                    equalitySets.addPair(lhs, null);
                }
            }

            if(target instanceof ArrayAccessExpr){
                target = ((ArrayAccessExpr)target).getName();
                if(escapeStatusMethodMap.containsKey(value.toString())){
                    String rhs = value.toString();
                    equalitySets.addPair(rhs, null);
                }
            }

            if (escapeStatusMethodMap.containsKey(value.toString()) && escapeStatusMethodMap.containsKey(target.toString())){
                String rhs = value.toString();
                String lhs = target.toString();
                objectReferencesMethodMap.putIfAbsent(lhs, new ArrayList<String>());
                objectReferencesMethodMap.putIfAbsent(rhs, new ArrayList<String>());
                objectReferencesMethodMap.get(lhs).add(rhs);
                objectReferencesMethodMap.get(rhs).add(lhs);
            }
            else if(escapeStatusMethodMap.containsKey(value.toString()) && staticFields.contains(target.toString())){
                updateEscapeState(value.toString(), 2);
            }
        }

        // else if (expression instanceof CastExpr){
        //     CastExpr castExpr = (CastExpr) expression;
        //     Expression castSubExpression = castExpr.getExpression();
        //     if(castSubExpression instanceof ArrayAccessExpr){
        //         castSubExpression = ((ArrayAccessExpr)castSubExpression).getName();
        //     }
        //     if (escapeStatusMethodMap.containsKey(castSubExpression.toString())){
        //         if(escapeStatusMethodMap.get(castSubExpression.toString()) < 1){
        //             updateEscapeState(castSubExpression.toString(), 1);
        //         }
        //     }
        // }

        else if (expression instanceof MethodCallExpr){
            MethodCallExpr methodCallExpr = (MethodCallExpr) expression;
            for(Expression argument : methodCallExpr.getArguments()){
                if(argument instanceof ArrayAccessExpr){
                    argument = ((ArrayAccessExpr)argument).getName();
                }
                if(escapeStatusMethodMap.containsKey(argument.toString())){
                    if(escapeStatusMethodMap.get(argument.toString()) < 2){
                        updateEscapeState(argument.toString(), 2);
                    }
                }
            }
        }
        else if (expression instanceof BinaryExpr){
            BinaryExpr binaryExpr = (BinaryExpr)expression;
            BinaryExpr.Operator operator = binaryExpr.getOperator();
            if(operator == BinaryExpr.Operator.EQUALS || operator == BinaryExpr.Operator.GREATER || operator == BinaryExpr.Operator.LESS || operator == BinaryExpr.Operator.GREATER_EQUALS || operator == BinaryExpr.Operator.LESS_EQUALS){
                Expression left = binaryExpr.getLeft();
                Expression right = binaryExpr.getRight();

                if(operator == BinaryExpr.Operator.EQUALS && escapeStatusMethodMap.containsKey(right.toString()) && escapeStatusMethodMap.containsKey(left.toString())){
                    
                    if(equalitySets.getParent(right.toString()) == equalitySets.getParent(left.toString())){
                        // System.out.println("FDAIHBHADSF");
                        
                        expression.replace(new BooleanLiteralExpr(true));
                    }
                    else if(equalitySets.getParent(right.toString()) != null || equalitySets.getParent(left.toString()) != null){
                        expression.replace(new BooleanLiteralExpr(false)); 
                    }
                }

                if(left instanceof ArrayAccessExpr){
                    left = ((ArrayAccessExpr)left).getName();
                }

                if(right instanceof ArrayAccessExpr){
                    right = ((ArrayAccessExpr)right).getName();
                }

                if(left instanceof NullLiteralExpr){
                    if (escapeStatusMethodMap.containsKey(right.toString()) && escapeStatusMethodMap.get(right.toString()) < 1){
                        updateEscapeState(right.toString(), 1);
                    }
                }
                else if(right instanceof NullLiteralExpr){
                    if (escapeStatusMethodMap.containsKey(left.toString()) && escapeStatusMethodMap.get(left.toString()) < 1){
                        updateEscapeState(left.toString(), 1);
                    }
                }
                else {
                    if (escapeStatusMethodMap.containsKey(left.toString()) && escapeStatusMethodMap.containsKey(right.toString())){
                        if (escapeStatusMethodMap.get(left.toString()) < 1){
                            updateEscapeState(left.toString(), 1);
                        }
                        if (escapeStatusMethodMap.get(right.toString()) < 1){
                            updateEscapeState(right.toString(), 1);
                        }
                    }
                }
            }
        }
    }

    private static void handleStatement(Statement statement){
        if(statement instanceof ReturnStmt){
            ReturnStmt returnStmt = (ReturnStmt) statement;
            Optional<Expression> retExpression = returnStmt.getExpression();
            if (retExpression.isPresent()){
                Expression expression = retExpression.get();
                if(expression instanceof ArrayAccessExpr){
                    expression = ((ArrayAccessExpr)expression).getName();
                }
                if(escapeStatusMethodMap.containsKey(expression.toString())){
                    if (escapeStatusMethodMap.get(expression.toString()) < 2){
                        updateEscapeState(expression.toString(), 2);
                    }
                }
            }
        }
        else if(statement instanceof ThrowStmt){
            ThrowStmt throwStmt = (ThrowStmt) statement;
            Expression expression = throwStmt.getExpression();
            if(expression instanceof ArrayAccessExpr){
                expression = ((ArrayAccessExpr)expression).getName();
            }
            if(escapeStatusMethodMap.containsKey(expression.toString())){
                if (escapeStatusMethodMap.get(expression.toString()) < 2){
                    updateEscapeState(expression.toString(), 2);
                }
            }
        }
    }
    private static void extractObjectInitializations(MethodDeclaration method) {
        // Get the method body
        BlockStmt methodBody = method.getBody().orElse(null);

        if (methodBody == null) {
            return;
        }
        NodeList<Statement> statements = methodBody.getStatements();
        
        // Iterate over the statements inside the method body
        
        for (int i = 0; i < statements.size(); i++) {
            Statement statement = statements.get(i);
            List<Expression> expressions = statement.findAll(Expression.class);
            List<Statement> subStatements = statement.findAll(Statement.class);
            // if (statement instanceof ExpressionStmt) {
            //     ExpressionStmt expressionStmt = (ExpressionStmt) statement;
            //     Expression expression = expressionStmt.getExpression();
            //     // Check if the statement is an object creation expression
                
            // }

            if(statement instanceof ReturnStmt){
                handleStatement(statement);
            }
            else{
                for (Expression expression : expressions){
                    handleExpression(expression);
                }
                for (Statement subStatement : subStatements){
                    handleStatement(subStatement);
                }
            }
        }
        // System.out.println(escapeStatusMethodMap);
        for(int i = 0; i < statements.size(); i++){
            i += scalarReplace(statements, i);
        }
    }
}

class EqualitySets{
    Map<String, String> parent = new HashMap<String, String>();

    String getParent(String q){
        if(parent.get(q) == q){
            return q;
        }
        else{
            return getParent(parent.get(q));
        }
    }
    int checkEqual(String a, String b){
        if(getParent(b)!=null && getParent(b).equals(getParent(a))){
            return 0;
        }
        else if(getParent(b)!=null && getParent(a)!=null){
            return 1;
        }
        else{
            return -1;
        }
    }
    void addPair(String a, String b){
        for (Map.Entry<String,String> entry : parent.entrySet()){
            if(entry.getValue().equals(a)){
                if(getParent(a).equals(a)){
                    entry.setValue(entry.getKey());
                }
                else{
                    entry.setValue(getParent(a));
                }
            }
        }
        parent.put(a,b);
        if(b!=null && !parent.containsKey(b)){
            parent.put(b,b);
        }
    }
    void clear(){
        parent.clear();
    }
}