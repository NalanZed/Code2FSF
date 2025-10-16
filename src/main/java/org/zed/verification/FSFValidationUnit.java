package org.zed.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Data
public class FSFValidationUnit {
    public final static ObjectMapper MAPPER = new ObjectMapper();
    List<String> allTs = new ArrayList<>();
    HashMap<String, String> vars = new HashMap<>();
    public FSFValidationUnit() {

    }
    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private HashMap<String,String> initVarsFormSSMP(String ssmp) {
        JavaParser parser = new JavaParser();
        CompilationUnit cu = parser.parse(ssmp).getResult().get();
        HashMap<String, String> varMap = new HashMap<>();
        cu.findFirst(MethodDeclaration.class).ifPresent(md -> {
            md.getParameters().forEach(param -> {
                String paramName = param.getNameAsString();
                String paramType = param.getType().asString();
                varMap.put(paramName, paramType);
            });
        });
        return varMap;
    }

    private List<String> getAllTsFromFSF(List<String[]> fsf) {
        List<String> allTs = new ArrayList<>();
        for (String[] pair : fsf) {
            String T = pair[0];
            if(T.isEmpty()) continue;
            allTs.add(T);
        }
        return allTs;
    }

    public FSFValidationUnit(String ssmp,List<String[]> fsf){
        this.vars.putAll(initVarsFormSSMP(ssmp));
        this.allTs.addAll(getAllTsFromFSF(fsf));
    }

    public static void main(String[] args) {
        // Example usage
        String ssmp = "public class Example { public void method(int a, String b) {} }";
        List<String[]> fsf = new ArrayList<>();
        fsf.add(new String[]{"T1", "D1"});
        fsf.add(new String[]{"T2", "D2"});

        FSFValidationUnit unit = new FSFValidationUnit(ssmp, fsf);
        System.out.println(unit.toJson());
    }

}
