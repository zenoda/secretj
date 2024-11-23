package com.mytest;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

/**
 * Unit test for simple App.
 */
public class AppTest {
    public static void main(String[] args) throws Exception {
        ClassPool pool = ClassPool.getDefault();
        pool.appendClassPath("sample/commons-cli-1.9.0.jar");
        CtClass cc = pool.get("org.apache.commons.cli.DefaultParser");
        
        CtMethod[] cms = cc.getDeclaredMethods();
        for (CtMethod cm : cms) {
            CtClass returnType = cm.getReturnType();
            if (returnType.isPrimitive()) {
                switch (returnType.getName()) {
                    case "short":
                    case "char":
                    case "byte":
                    case "int":
                    case "long":
                        cm.setBody("return 0;");
                        break;
                    case "double":
                    case "float":
                        cm.setBody("return 0.0;");
                        break;
                    case "boolean":
                        cm.setBody("return false;");
                        break;
                    case "void":
                        cm.setBody("return;");
                        break;
                }
            } else {
                cm.setBody("return null;");
            }
        }

    }
}
