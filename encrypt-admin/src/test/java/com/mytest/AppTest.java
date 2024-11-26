package com.mytest;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

/**
 * Unit test for simple App.
 */
public class AppTest {
    public static void main(String[] args) throws Exception {
      String p="^[^,]+(,[^,]+)*$";
      String t="ab/cd,";
      System.out.println(t.matches(p));
    }
}
