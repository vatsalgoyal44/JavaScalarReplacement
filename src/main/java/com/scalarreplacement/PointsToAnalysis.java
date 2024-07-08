package com.scalarreplacement;

import com.laamella.javacfa.CompilationUnitFlows;
import com.laamella.javacfa.ControlFlowAnalyser;
import com.laamella.javacfa.Flow;

import javassist.Loader.Simple;
import javassist.compiler.ast.Variable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import org.checkerframework.checker.units.qual.g;

import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.symbolsolver.*;
// import com.github.javaparser.symbolsolver.JavaSymbolSolver;
// import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserFieldDeclaration;
// import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
// import com.github.javaparser.symbolsolver.*;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

public class PointsToAnalysis {
    public static Map<String, ClassOrInterfaceDeclaration> classMap = new HashMap<String, ClassOrInterfaceDeclaration>();
    public Map<MethodDeclaration, Map<Flow, PointsToGraph>> pointsToAnalysis = new HashMap<MethodDeclaration, Map<Flow, PointsToGraph>>();
    // public Map<MethodDeclaration, Flow> methodToFlow = new HashMap<MethodDeclaration, Flow>();
    

    PointsToAnalysis(CompilationUnit cu) {
        
            // Parse the Java file
            
            ClassParameterVisitor classParameterVisitor = new ClassParameterVisitor();
            classParameterVisitor.visit(cu, classMap);
            
            MethodVisitor methodVisitor = new MethodVisitor();
            methodVisitor.visit(cu, pointsToAnalysis);
            
            // Create the visitor and visit the AST
            
        
    }
    PointsToAnalysis(CompilationUnit cu, Map<String, ClassOrInterfaceDeclaration> map) {
        
        // Parse the Java file
        
        // ClassParameterVisitor classParameterVisitor = new ClassParameterVisitor();
        // classParameterVisitor.visit(cu, classMap);
        classMap = map;
        
        MethodVisitor methodVisitor = new MethodVisitor();
        methodVisitor.visit(cu, pointsToAnalysis);
        
        // Create the visitor and visit the AST
        
    
}
    private static class ClassParameterVisitor extends VoidVisitorAdapter<Map<String, ClassOrInterfaceDeclaration>> {
        @Override
        public void visit(ClassOrInterfaceDeclaration classDeclaration, Map<String, ClassOrInterfaceDeclaration> collectorMap) {
            super.visit(classDeclaration, collectorMap);

            // Print class name

            // // Visit and print field declarations (class parameters)
            collectorMap.put(classDeclaration.getNameAsString(), classDeclaration);
            
            // classDeclaration.findAll(FieldDeclaration.class).forEach(field -> {
            //     if(field.getModifiers().contains(Modifier.staticModifier())){
            //         for(VariableDeclarator variable : field.getVariables()){
            //             staticFields.add(classDeclaration.getNameAsString() + "." + variable.getNameAsString());
            //         }
            //     }
            // });
        }
    }

    private static class MethodVisitor extends ModifierVisitor<Map<MethodDeclaration, Map<Flow, PointsToGraph>>>{
        Map<Flow, PointsToGraph> pointsToMapIn = new HashMap<Flow, PointsToGraph>();
        Map<Flow, PointsToGraph> pointsToMapOut = new HashMap<Flow, PointsToGraph>();
        
        
        public Set<ObjectNode> accessObjectNode(Flow flow, FieldAccessExpr fieldAccessExpr, PointsToGraph currPtg){
            //TODO : Handle case of typecasts
            Expression scope = fieldAccessExpr.getScope();
            SimpleName object = fieldAccessExpr.getName();
            Set<ObjectNode> ret = new HashSet<ObjectNode>();
            if(currPtg.localVariableMaps.containsKey(((NameExpr)scope).getName())){
                for(ObjectNode objectNode : currPtg.localVariableMaps.get(((NameExpr)scope).getName())){
                    ret.addAll(currPtg.fields.get(objectNode).get(object.toString()));
                }
            }
            
            return ret;

        }
        public Map<Flow, Set<Flow>> getPreds(Flow method){
            Map<Flow, Set<Flow>> predMap = new HashMap<Flow, Set<Flow>>();

            List<Flow> workList = new ArrayList<Flow>();
            // System.out.println(method.getNode());
            if(method.getNext()!=null){
                workList.add(method.getNext());
                predMap.putIfAbsent(method.getNext(), new HashSet<Flow>());
                predMap.get(method.getNext()).add(method);
            }
            if(method.getMayBranchTo()!=null){
                workList.add(method.getMayBranchTo());
                predMap.putIfAbsent(method.getMayBranchTo(), new HashSet<Flow>());
                predMap.get(method.getMayBranchTo()).add(method);
            }

            while(!workList.isEmpty()){
                Flow currFlow = workList.remove(0);
                
                if(currFlow.getNext()!=null){
                    predMap.putIfAbsent(currFlow.getNext(), new HashSet<Flow>());
                    if(!predMap.get(currFlow.getNext()).contains(currFlow)){
                        predMap.get(currFlow.getNext()).add(currFlow);
                        workList.add(currFlow.getNext());
                    }
                    
                }
                if(currFlow.getMayBranchTo()!=null){
                    predMap.putIfAbsent(currFlow.getMayBranchTo(), new HashSet<Flow>());
                    if(!predMap.get(currFlow.getMayBranchTo()).contains(currFlow)){
                        predMap.get(currFlow.getMayBranchTo()).add(currFlow);
                        workList.add(currFlow.getMayBranchTo());
                    }
                }
            }
            return predMap;
        }

        public void getValueAfter(Flow flow, PointsToGraph currPtg){
            Node node = flow.getNode();
            if(node instanceof ExpressionStmt){
                    if(((ExpressionStmt)node).getExpression() instanceof AssignExpr){
                        AssignExpr assignExpr = (AssignExpr)((ExpressionStmt)node).getExpression();
                        Expression lhs = assignExpr.getTarget();
                        Expression rhs = assignExpr.getValue();

                        if(lhs instanceof NameExpr){
                            // a = b
                            if(rhs instanceof NameExpr){
                                if(currPtg.localVariableMaps.containsKey(((NameExpr)lhs).getName())){
                                    if(currPtg.localVariableMaps.containsKey(((NameExpr)rhs).getName())){
                                        currPtg.localVariableMaps.replace(((NameExpr)lhs).getName(), new HashSet<ObjectNode>(currPtg.localVariableMaps.get(((NameExpr)rhs).getName())));
                                        // currPtg.pointsToAnything.put(((NameExpr)lhs).getName(), currPtg.pointsToAnything.get(((NameExpr)rhs).getName());
                                    }
                                }
                                // else{
                                //     if(currPtg.fields.get(currPtg.selfObject).containsKey(((NameExpr)lhs).getName().toString())){
                                //         currPtg.fields.get(currPtg.selfObject).replace(((NameExpr)lhs).getName().toString(), new HashSet<ObjectNode>(currPtg.localVariableMaps.get(((NameExpr)rhs).getName())));
                                //     }
                                // }
                            }
                            // a = b.f
                            else if(rhs instanceof FieldAccessExpr){
                                if(currPtg.localVariableMaps.containsKey(((NameExpr)lhs).getName())){
                                    currPtg.localVariableMaps.get(((NameExpr)lhs).getName()).addAll(accessObjectNode(flow, (FieldAccessExpr)rhs, currPtg));
                                }
                            }
                            // a = new b()
                            else if(rhs instanceof ObjectCreationExpr){
                                ObjectCreationExpr objectCreationExpr = (ObjectCreationExpr) rhs;
                                if(currPtg.localVariableMaps.containsKey(((NameExpr)lhs).getName())){
                                    ObjectNode newObjectNode = currPtg.nodeCreation(objectCreationExpr);
                                    currPtg.localVariableMaps.get(((NameExpr)lhs).getName()).add(newObjectNode);
                                }
                            }
                            else if(rhs instanceof ArrayCreationExpr){
                                ArrayCreationExpr arrayCreationExpr = (ArrayCreationExpr) rhs;
                                if(currPtg.localVariableMaps.containsKey(((NameExpr)lhs).getName())){
                                    ObjectNode newObjectNode = currPtg.arrayCreation(arrayCreationExpr);
                                    currPtg.localVariableMaps.get(((NameExpr)lhs).getName()).add(newObjectNode);
                                }
                                else{

                                }
                            }
                            // a = b.foo()
                            else if(rhs instanceof MethodCallExpr){
                                currPtg.pointsToAnythingLocalVar.put(((NameExpr)lhs).getName(), true);
                                currPtg.pointsToAnything((MethodCallExpr)rhs);
                            }
                        }
                        else if(lhs instanceof FieldAccessExpr){
                            Expression scope = ((FieldAccessExpr)lhs).getScope();
                            SimpleName objectVar = ((FieldAccessExpr)lhs).getName();
                            //a.f = b
                            if(rhs instanceof NameExpr && scope instanceof NameExpr){
                                if(currPtg.localVariableMaps.containsKey(((NameExpr)rhs).getName())){
                                    
                                    boolean pointsToAny = false;
                                    for(ObjectNode objectNode : currPtg.localVariableMaps.get(((NameExpr)rhs).getName())){
                                        
                                        if(currPtg.pointsToAnything.containsKey(objectNode)){
                                            pointsToAny = true;
                                            break;
                                        }
                                    }
                                    for(ObjectNode objectNode : currPtg.localVariableMaps.get(((NameExpr)scope).getName())){
                                        currPtg.fields.get(objectNode).put((objectVar.toString()),new HashSet<ObjectNode>(currPtg.localVariableMaps.get(((NameExpr)rhs).getName())));
                                        currPtg.pointsToAnything.put(objectNode, pointsToAny);
                                    }
                                }
                            }
                            else if(scope instanceof NameExpr && rhs instanceof ObjectCreationExpr){
                                ObjectCreationExpr objectCreationExpr = (ObjectCreationExpr) rhs;
                                for(ObjectNode objectNode : currPtg.localVariableMaps.get(((NameExpr)scope).getName())){
                                    currPtg.fields.get(objectNode).put((objectVar.toString()),new HashSet<ObjectNode>(currPtg.localVariableMaps.get(((NameExpr)rhs).getName())));
                                    ObjectNode newObjectNode = currPtg.nodeCreation(objectCreationExpr);
                                    currPtg.pointsToAnything.put(newObjectNode, false);
                                }
                            }
                        }
                    }
                    if(((ExpressionStmt)node).getExpression() instanceof MethodCallExpr){
                        currPtg.pointsToAnything((MethodCallExpr)((ExpressionStmt)node).getExpression());
                    }
                    if(((ExpressionStmt)node).getExpression() instanceof VariableDeclarationExpr){
                        VariableDeclarationExpr variableDeclarationExpr = (VariableDeclarationExpr)((ExpressionStmt)node).getExpression();
                        for(VariableDeclarator variableDeclarator : variableDeclarationExpr.getVariables()){
                            SimpleName varName = variableDeclarator.getName();
                            Type type = variableDeclarator.getType();
                                if(true){
                                    currPtg.localVariableMaps.put(varName, new HashSet<ObjectNode>());
                                    Optional<Expression> optExp = variableDeclarator.getInitializer();
                                    if(optExp.isPresent()){
                                        Expression rhs = optExp.get();
                                        //A a = b;
                                        if(rhs instanceof NameExpr){
                                                if(currPtg.localVariableMaps.containsKey(((NameExpr)rhs).getName())){
                                                    currPtg.localVariableMaps.replace(varName, new HashSet<ObjectNode>(currPtg.localVariableMaps.get(((NameExpr)rhs).getName())));
                                                }
                                            }
                                        //A a = b.f
                                        else if(rhs instanceof FieldAccessExpr){
                                                currPtg.localVariableMaps.get(varName).addAll(accessObjectNode(flow, (FieldAccessExpr)rhs, currPtg));
                                        }
                                        //A a = new A()
                                        else if(rhs instanceof ObjectCreationExpr){
                                            ObjectCreationExpr objectCreationExpr = (ObjectCreationExpr) rhs;
                                            ObjectNode newObjectNode = currPtg.nodeCreation(objectCreationExpr);
                                            currPtg.localVariableMaps.get(varName).add(newObjectNode);
                                        }
                                        //A a[] = new A[] 
                                        else if(rhs instanceof ArrayCreationExpr){
                                            currPtg.localVariableMaps.get(varName).add(currPtg.arrayCreation((ArrayCreationExpr)rhs));
                                        }
                                }
                                
                                }

                            
                        }
                }
                }
        }

        @Override
        public MethodDeclaration visit(MethodDeclaration method, Map<MethodDeclaration, Map<Flow, PointsToGraph>> pointsToAnalysis) {
            
            // Continue visiting other methods
            super.visit(method, pointsToAnalysis);
            
            List<Flow> workList = new ArrayList<Flow>();
            ControlFlowAnalyser controlFlowAnalyser = new ControlFlowAnalyser();
            Flow methodFlow = controlFlowAnalyser.analyse(method);
            
            // System.out.println(methodFlow.getNode());
            if(methodFlow!= null){
                Map<Flow, Set<Flow>> predMap = getPreds(methodFlow);
                PointsToGraph currPointsToGraph = new PointsToGraph();
                currPointsToGraph.selfObject = currPointsToGraph.selfNodeCreation(method.findAncestor(ClassOrInterfaceDeclaration.class).get());
                pointsToMapOut.put(methodFlow, currPointsToGraph);
                if(methodFlow.getNext()!=null){
                    workList.add(methodFlow.getNext());
                }
                if(methodFlow.getMayBranchTo()!=null){
                    workList.add(methodFlow.getMayBranchTo());
                }

                for(Flow flow : predMap.keySet()){
                    pointsToMapIn.put(flow, new PointsToGraph());
                    pointsToMapOut.put(flow, new PointsToGraph());
                }
                while(!workList.isEmpty()){
                    
                    Flow currFlow = workList.remove(0);
                    
                    for(Flow pred : predMap.get(currFlow)){
                        pointsToMapIn.get(currFlow).meet(pointsToMapOut.get(pred));
                    }

                    PointsToGraph oldOut = pointsToMapOut.get(currFlow);
                    PointsToGraph currPtg = pointsToMapIn.get(currFlow);

                    getValueAfter(currFlow, currPtg);
                    pointsToMapOut.put(currFlow, currPtg);
                    if(!currPtg.compare(oldOut)){
                        if(currFlow.getNext()!=null){
                            workList.add(currFlow.getNext());
                        }
                        if(currFlow.getMayBranchTo()!=null){
                            workList.add(currFlow.getMayBranchTo());
                        }
                    }
                }
                // extractObjectInitializations(method);
                // FieldAccessVisitor fieldAccessVisitor = new FieldAccessVisitor();

                // fieldAccessVisitor.visit(method, null);

                // 
                pointsToAnalysis.put(method, new HashMap<Flow, PointsToGraph>(pointsToMapOut));
                
                pointsToMapIn.clear();
                pointsToMapOut.clear();
            }
            
            return method;
        }
    }
    static class ObjectNode{
        SimpleName classType;
        Boolean isArray = false;

        // ObjectNode(ClassOrInterfaceDeclaration classDeclaration){
        //     classType = classDeclaration.getName();
        //     for(ResolvedFieldDeclaration fieldDeclaration : classDeclaration.resolve().getAllFields()){
        //         String declaringType = fieldDeclaration.declaringType().getClassName();
        //         Optional<Node> fieldDec = fieldDeclaration.toAst();
        //         if(fieldDec.isPresent()){
        //             FieldDeclaration fieldDeclaration2 = (FieldDeclaration)fieldDec.get();
        //             for (VariableDeclarator variableDeclarator : fieldDeclaration2.getVariables()){
        //                 // String typeChild = variableDeclarator.getTypeAsString();
        //                 Optional<Expression> initializerOptional = variableDeclarator.getInitializer();
        //                 if(initializerOptional.isPresent()){
        //                     Expression initializer = initializerOptional.get();
        //                     if(initializer instanceof ObjectCreationExpr){
        //                         ObjectCreationExpr objectCreationExpr = (ObjectCreationExpr)initializer;
        //                         String edgeName = variableDeclarator.getNameAsString();
        //                         if(!declaringType.equals(classDeclaration.getNameAsString())){
        //                             edgeName = declaringType+"_"+edgeName;
        //                         }
        //                         Set<ObjectNode> newSet = new HashSet<ObjectNode>();
        //                         newSet.add(new ObjectNode(classMap.get(objectCreationExpr.getTypeAsString())));
        //                         edgesFromNode.put(edgeName, newSet);
        //                     }
        //                 }
        //             }
        //         }
        //     }
        // }   
        ObjectNode(SimpleName name){
            classType = name;
        }
        ObjectNode(SimpleName name, Boolean array){
            classType = name;
            isArray = array;
        }
    }
    static class PointsToGraph{
        Map<ObjectNode, Integer> objectEscapeState;
        Map<SimpleName, Set<ObjectNode>> localVariableMaps;
        Map<ObjectNode, Map<String, Set<ObjectNode>>> fields;
        Map<ObjectNode, Boolean> pointsToAnything;
        Map<SimpleName, Boolean> pointsToAnythingLocalVar;
        public ObjectNode selfObject;


        PointsToGraph(){
            objectEscapeState = new HashMap<ObjectNode, Integer>();
            localVariableMaps = new HashMap<SimpleName, Set<ObjectNode>>();
            fields = new HashMap<ObjectNode, Map<String, Set<ObjectNode>>>();
            pointsToAnything = new HashMap<ObjectNode, Boolean>();
            pointsToAnythingLocalVar = new HashMap<SimpleName, Boolean>();
            
        }
        PointsToGraph(Map<ObjectNode, Integer> objectEscapeState, Map<SimpleName, Set<ObjectNode>> localVariableMaps, Map<ObjectNode, Map<String, Set<ObjectNode>>> fields, Map<ObjectNode, Boolean> pointsToAnything, Map<SimpleName, Boolean> pointsToAnythingLocarVar, ObjectNode selfObject){
            this.objectEscapeState = new HashMap<ObjectNode, Integer>(objectEscapeState);
            this.localVariableMaps = new HashMap<SimpleName, Set<ObjectNode>>();
            this.pointsToAnything = new HashMap<ObjectNode, Boolean>(pointsToAnything);
            this.pointsToAnythingLocalVar = new HashMap<SimpleName, Boolean>(pointsToAnythingLocalVar);
            this.selfObject = selfObject;

            for(SimpleName localVar : localVariableMaps.keySet()){
                this.localVariableMaps.put(localVar, new HashSet<ObjectNode>(localVariableMaps.get(localVar)));
            }
            
            this.fields = new HashMap<ObjectNode, Map<String, Set<ObjectNode>>>();

            for(ObjectNode node : fields.keySet()){
                Map<String, Set<ObjectNode>> nodeFields = new HashMap<String, Set<ObjectNode>>();
                for(String fieldName : fields.get(node).keySet()){
                    nodeFields.put(fieldName, new HashSet<ObjectNode>(fields.get(node).get(fieldName)));
                }
                this.fields.put(node, nodeFields);
            }
        }

        public String toString() {
            String output = "";
            Map<ObjectNode, Integer> nodeVal = new HashMap<ObjectNode, Integer>();
            int currval = 0;
            for(SimpleName localVar : localVariableMaps.keySet()){
                output += localVar + " -> {";
                for(ObjectNode node : localVariableMaps.get(localVar)){
                    if(!nodeVal.containsKey(node)){
                        nodeVal.put(node, currval);
                        currval += 1;
                    }
                    output += "O"+nodeVal.get(node) + ",";
                }
                output += "}\n";
            }
            for(ObjectNode node : fields.keySet()){
                if(!nodeVal.containsKey(node)){
                    nodeVal.put(node, currval);
                    currval += 1;
                }
                output += "O"+nodeVal.get(node) + " -> ";
                for(String field : fields.get(node).keySet()){
                    output += "\t" + field + " -> {";
                    for(ObjectNode nodeChild : fields.get(node).get(field)){
                        if(!nodeVal.containsKey(nodeChild)){
                            nodeVal.put(nodeChild, currval);
                            currval += 1;
                        }
                        output += "O"+nodeVal.get(nodeChild) + ",";
                    }
                    output += "}\n";
                }
            }
            return output;
        }
        
        public ObjectNode arrayCreation(ArrayCreationExpr arrayCreationExpr){
            ObjectNode objectNode = new ObjectNode(null, true);
            Map<String, Set<ObjectNode>> currFields = new HashMap<String, Set<ObjectNode>>();
            currFields.put("[]", new HashSet<ObjectNode>());
            fields.put(objectNode, currFields);
            objectEscapeState.put(objectNode, 0);
            return objectNode;
        }

        public ObjectNode selfNodeCreation(ClassOrInterfaceDeclaration classDeclaration){
                if(classDeclaration == null){
                ObjectNode node = new ObjectNode(null);
                Map<String, Set<ObjectNode>> currFields = new HashMap<String, Set<ObjectNode>>();
                this.fields.put(node, currFields);
                this.pointsToAnything.put(node, false);
                this.objectEscapeState.put(node, 2);
                return node;
            }
            SimpleName classType = classDeclaration.getName();
            Map<String, Set<ObjectNode>> currFields = new HashMap<String, Set<ObjectNode>>();
            for(ResolvedFieldDeclaration fieldDeclaration : classDeclaration.resolve().getAllFields()){
                String declaringType = fieldDeclaration.declaringType().getClassName();
                Optional<Node> fieldDec = fieldDeclaration.toAst();
                if(fieldDec.isPresent()){
                    FieldDeclaration fieldDeclaration2 = (FieldDeclaration)fieldDec.get();
                    for (VariableDeclarator variableDeclarator : fieldDeclaration2.getVariables()){
                        // String typeChild = variableDeclarator.getTypeAsString();
                        Optional<Expression> initializerOptional = variableDeclarator.getInitializer();
                        if(!variableDeclarator.getType().isArrayType()){
                            if(initializerOptional.isPresent()){
                                Expression initializer = initializerOptional.get();    
                                if(initializer instanceof ObjectCreationExpr){
                                    ObjectCreationExpr objectCreationExprField = (ObjectCreationExpr)initializer;
                                    String edgeName = variableDeclarator.getNameAsString();
                                    if(!declaringType.equals(classDeclaration.getNameAsString())){
                                        edgeName = declaringType+"_"+edgeName;
                                    }
                                    Set<ObjectNode> newSet = new HashSet<ObjectNode>();
                                    newSet.add(nodeCreation(objectCreationExprField));
                                    currFields.put(edgeName, newSet);
                                }
                            }
                            else{
                                String edgeName = variableDeclarator.getNameAsString();
                                if(!declaringType.equals(classDeclaration.getNameAsString())){
                                    edgeName = declaringType+"_"+edgeName;
                                }

                                Set<ObjectNode> newSet = new HashSet<ObjectNode>();
                                currFields.put(edgeName, newSet);
                            }
                        }
                        else{
                            if(initializerOptional.isPresent()){
                                Expression initializer = initializerOptional.get();    
                                if(initializer instanceof ArrayCreationExpr){
                                    // ArrayCreationExpr arrayCreationExpr = (ArrayCreationExpr)initializer;
                                    String edgeName = variableDeclarator.getNameAsString();
                                    if(!declaringType.equals(classDeclaration.getNameAsString())){
                                        edgeName = declaringType+"_"+edgeName;
                                    }
                                    Set<ObjectNode> newSet = new HashSet<ObjectNode>();
                                    newSet.add(arrayCreation((ArrayCreationExpr)initializer));
                                    // newSet.add(nodeCreation(objectCreationExprField));
                                    currFields.put(edgeName, newSet);
                                }
                            }
                            else{
                                String edgeName = variableDeclarator.getNameAsString();
                                if(!declaringType.equals(classDeclaration.getNameAsString())){
                                    edgeName = declaringType+"_"+edgeName;
                                }

                                Set<ObjectNode> newSet = new HashSet<ObjectNode>();
                                currFields.put(edgeName, newSet);
                            }
                        }
                    }
                }
            }
            ObjectNode node = new ObjectNode(classType);
            this.fields.put(node, currFields);
            this.pointsToAnything.put(node, false);
            this.objectEscapeState.put(node, 2);
            for(String fieldName : currFields.keySet()){
                for(ObjectNode childNode : currFields.get(fieldName)){
                    this.objectEscapeState.put(childNode, 2);
                }
            }
            return node;
        }
        public ObjectNode nodeCreation(ObjectCreationExpr objectCreationExpr){
            
            ClassOrInterfaceDeclaration classDeclaration = classMap.get(objectCreationExpr.getTypeAsString());
            if(classDeclaration == null){
                ObjectNode node = new ObjectNode(null);
                Map<String, Set<ObjectNode>> currFields = new HashMap<String, Set<ObjectNode>>();
                this.fields.put(node, currFields);
                this.pointsToAnything.put(node, false);
                this.objectEscapeState.put(node, 2);
                return node;
            }
            // System.out.println(classDeclaration);
            SimpleName classType = classDeclaration.getName();
            Map<String, Set<ObjectNode>> currFields = new HashMap<String, Set<ObjectNode>>();
            for(ResolvedFieldDeclaration fieldDeclaration : classDeclaration.resolve().getAllFields()){
                String declaringType = fieldDeclaration.declaringType().getClassName();
                Optional<Node> fieldDec = fieldDeclaration.toAst();
                if(fieldDec.isPresent()){
                    FieldDeclaration fieldDeclaration2 = (FieldDeclaration)fieldDec.get();
                    for (VariableDeclarator variableDeclarator : fieldDeclaration2.getVariables()){
                        // String typeChild = variableDeclarator.getTypeAsString();
                        Optional<Expression> initializerOptional = variableDeclarator.getInitializer();
                        if(!variableDeclarator.getType().isArrayType()){
                            if(initializerOptional.isPresent()){
                                Expression initializer = initializerOptional.get();    
                                if(initializer instanceof ObjectCreationExpr){
                                    ObjectCreationExpr objectCreationExprField = (ObjectCreationExpr)initializer;
                                    String edgeName = variableDeclarator.getNameAsString();
                                    if(!declaringType.equals(classDeclaration.getNameAsString())){
                                        edgeName = declaringType+"_"+edgeName;
                                    }
                                    Set<ObjectNode> newSet = new HashSet<ObjectNode>();
                                    newSet.add(nodeCreation(objectCreationExprField));
                                    currFields.put(edgeName, newSet);
                                }
                            }
                            else{
                                String edgeName = variableDeclarator.getNameAsString();
                                if(!declaringType.equals(classDeclaration.getNameAsString())){
                                    edgeName = declaringType+"_"+edgeName;
                                }

                                Set<ObjectNode> newSet = new HashSet<ObjectNode>();
                                currFields.put(edgeName, newSet);
                            }
                        }
                        else{
                            if(initializerOptional.isPresent()){
                                Expression initializer = initializerOptional.get();    
                                if(initializer instanceof ArrayCreationExpr){
                                    // ArrayCreationExpr arrayCreationExpr = (ArrayCreationExpr)initializer;
                                    String edgeName = variableDeclarator.getNameAsString();
                                    if(!declaringType.equals(classDeclaration.getNameAsString())){
                                        edgeName = declaringType+"_"+edgeName;
                                    }
                                    Set<ObjectNode> newSet = new HashSet<ObjectNode>();
                                    newSet.add(arrayCreation((ArrayCreationExpr)initializer));
                                    // newSet.add(nodeCreation(objectCreationExprField));
                                    currFields.put(edgeName, newSet);
                                }
                            }
                            else{
                                String edgeName = variableDeclarator.getNameAsString();
                                if(!declaringType.equals(classDeclaration.getNameAsString())){
                                    edgeName = declaringType+"_"+edgeName;
                                }

                                Set<ObjectNode> newSet = new HashSet<ObjectNode>();
                                currFields.put(edgeName, newSet);
                            }
                        }
                    }
                }
            }
            ObjectNode node = new ObjectNode(classType);
            this.fields.put(node, currFields);
            this.pointsToAnything.put(node, false);
            this.objectEscapeState.put(node, 0);
            return node;
        }

        public void pointsToAnything(MethodCallExpr expr){
            Set<ObjectNode> objectNodesSet = new HashSet<ObjectNode>();
            // Assuming function call arguments are only local variables
            for(Expression arg : expr.getArguments()){
                if(arg instanceof NameExpr && this.localVariableMaps.containsKey(((NameExpr)arg).getName())){
                    objectNodesSet.addAll(this.localVariableMaps.get(((NameExpr)arg).getName()));
                }
            }
            if(expr.getScope().isPresent()){
                if(expr.getScope().get() instanceof NameExpr && this.localVariableMaps.containsKey(((NameExpr)expr.getScope().get()).getName())){
                    objectNodesSet.addAll(this.localVariableMaps.get(((NameExpr)expr.getScope().get()).getName()));
                }   
            }

            List<ObjectNode> queue = new ArrayList<ObjectNode>(objectNodesSet);
            while(!queue.isEmpty()){
                ObjectNode top = queue.remove(0);
                for(String field : this.fields.get(top).keySet()){
                    for(ObjectNode subNode : this.fields.get(top).get(field)){
                        if(!queue.contains(subNode)){
                            queue.add(subNode);
                        }
                        objectNodesSet.add(subNode);
                    }
                }
            }
            
            for(ObjectNode node : objectNodesSet){
                this.pointsToAnything.put(node, true);
                this.objectEscapeState.put(node, 2);
            }
            
        }

        public PointsToGraph copyGraph(){
            PointsToGraph ptg = new PointsToGraph(this.objectEscapeState, this.localVariableMaps, this.fields, this.pointsToAnything, this.pointsToAnythingLocalVar, this.selfObject);
            return ptg;
        }

        public boolean compare(PointsToGraph otherGraph){
            if(!otherGraph.objectEscapeState.keySet().equals(objectEscapeState.keySet())){
                return false;
            }
            if(!otherGraph.localVariableMaps.keySet().equals(localVariableMaps.keySet())){
                return false;
            }

            for(SimpleName localVar : localVariableMaps.keySet()){
                if(!otherGraph.localVariableMaps.get(localVar).equals(localVariableMaps.get(localVar))){
                    return false;
                }
            }

            for(ObjectNode objectNode : pointsToAnything.keySet()){
                if(!pointsToAnything.get(objectNode).equals(otherGraph.pointsToAnything.get(objectNode))){
                    return false;
                }
            }
            
            for(ObjectNode node : fields.keySet()){
                if(!fields.get(node).keySet().equals(otherGraph.fields.get(node).keySet())){
                    return false;
                }
                for(String field : fields.get(node).keySet()){
                    if(!fields.get(node).get(field).equals(otherGraph.fields.get(node).get(field))){
                        return false;
                    }
                }
            }

            if(otherGraph.selfObject != this.selfObject){
                return false;
            }
            return true;
        }
        public void meet(PointsToGraph otherGraph){
            for(ObjectNode node : this.objectEscapeState.keySet()){
                if(otherGraph.objectEscapeState.containsKey(node)){
                    this.objectEscapeState.put(node, Math.max(this.objectEscapeState.get(node), otherGraph.objectEscapeState.get(node)));
                    for(String field : otherGraph.fields.get(node).keySet()){
                        if(this.fields.get(node).containsKey(field)){
                            this.fields.get(node).get(field).addAll(otherGraph.fields.get(node).get(field));
                        }
                        else{
                            this.fields.get(node).put(field, new HashSet<ObjectNode>(otherGraph.fields.get(node).get(field)));
                        }
                    }
                }
            }
            for(ObjectNode node : otherGraph.objectEscapeState.keySet()){
                if(!this.objectEscapeState.containsKey(node)){
                    this.objectEscapeState.put(node, otherGraph.objectEscapeState.get(node));
                    this.fields.put(node, new HashMap<String, Set<ObjectNode>>());
                    for(String field : otherGraph.fields.get(node).keySet()){
                        this.fields.get(node).put(field, new HashSet<ObjectNode>(otherGraph.fields.get(node).get(field)));
                    }
                }
            }
            
            for(SimpleName localVar : this.localVariableMaps.keySet()){
                if(otherGraph.localVariableMaps.containsKey(localVar)){
                    this.localVariableMaps.get(localVar).addAll(otherGraph.localVariableMaps.get(localVar));
                }
            }

            for(SimpleName localVar : otherGraph.localVariableMaps.keySet()){
                if(!this.localVariableMaps.containsKey(localVar)){
                    this.localVariableMaps.put(localVar, new HashSet<ObjectNode>(otherGraph.localVariableMaps.get(localVar)));
                }
            }

            for(ObjectNode node : otherGraph.pointsToAnything.keySet()){
                if(this.pointsToAnything.containsKey(node)){
                    this.pointsToAnything.put(node, this.pointsToAnything.get(node) || otherGraph.pointsToAnything.get(node));
                }
            }
        }

    }
}

