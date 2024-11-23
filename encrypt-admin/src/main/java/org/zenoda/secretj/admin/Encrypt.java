package org.zenoda.secretj.admin;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import org.apache.commons.cli.*;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class Encrypt {
    private static final Logger logger = Logger.getLogger(Encrypt.class.getName());
    private static final String ALGORITHM = "AES";

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("h", "help", false, "print this message");
        options.addOption("p", "password", true, "password");
        options.addOption("j", "jars", true, "jars for encryption");
        options.addOption("c", "classes", true, "classes for encryption");
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            logger.severe(e.getMessage());
            System.exit(1);
        }
        if (cmd.hasOption("help")) {
            formatter.printHelp("encrypt-admin", options);
        } else {
            String password = cmd.getOptionValue("password");
            String jars = cmd.getOptionValue("jars");
            String classes = cmd.getOptionValue("classes");
            try {
                encrypt(jars, classes, password);
            } catch (Exception e) {
                logger.severe(e.getMessage());
                System.exit(1);
            }
        }
    }

    private static void encrypt(String jars, String classes, String password) throws Exception {
        SecretKey secretKey = new SecretKeySpec(sha256(password), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        String[] inputJars = jars.split(":");
        String[] classesToEncrypt = classes == null ? null : classes.split(":");
        for (String inputJar : inputJars) {
            String outputJar = inputJar.substring(0, inputJar.lastIndexOf(".")) + "-encrypted.jar";
            JarInputStream jarInputStream = new JarInputStream(new FileInputStream(inputJar));
            Manifest manifest = jarInputStream.getManifest();
            JarOutputStream jarOutputStream = manifest == null ? new JarOutputStream(new FileOutputStream(outputJar)) : new JarOutputStream(new FileOutputStream(outputJar), manifest);
            try (jarInputStream; jarOutputStream) {
                encryptStream(classesToEncrypt, jarInputStream, jarOutputStream, cipher);
            }
        }
    }

    private static void encryptStream(String[] classes, JarInputStream jarInputStream, JarOutputStream jarOutputStream, Cipher cipher) throws Exception {
        for (JarEntry entry = jarInputStream.getNextJarEntry(); entry != null; entry = jarInputStream.getNextJarEntry()) {
            if (entry.getName().endsWith(".class") && isClassToEncrypt(entry.getName(), classes)) {
                logger.info("encrypt class: " + entry.getName());
                byte[] classBytes = jarInputStream.readAllBytes();
                byte[] mockBytes = makeMock(classBytes);
                JarEntry newEntry = createNewEntry(entry.getName(), entry, mockBytes);
                jarOutputStream.putNextEntry(newEntry);
                jarOutputStream.write(mockBytes);
                byte[] encryptedBytes = encryptClassBytes(classBytes, cipher);
                JarEntry newEncEntry = createNewEntry(entry.getName() + ".enc", entry, mockBytes);
                jarOutputStream.putNextEntry(newEncEntry);
                jarOutputStream.write(encryptedBytes);
            } else if (entry.getName().endsWith(".jar")) {
                ByteArrayOutputStream dataBuffStream = new ByteArrayOutputStream();
                JarInputStream newJarInputStream = new JarInputStream(jarInputStream);
                Manifest manifest = newJarInputStream.getManifest();
                try (JarOutputStream newJarOutputStream = manifest == null ? new JarOutputStream(dataBuffStream) : new JarOutputStream(dataBuffStream, manifest)) {
                    encryptStream(classes, newJarInputStream, newJarOutputStream, cipher);
                }
                byte[] dataBuff = dataBuffStream.toByteArray();
                JarEntry newEntry = createNewEntry(entry.getName(), entry, dataBuff);
                jarOutputStream.putNextEntry(newEntry);
                jarOutputStream.write(dataBuff);
            } else {
                if (entry.isDirectory()) {
                    JarEntry newEntry = new JarEntry(entry.getName());
                    jarOutputStream.putNextEntry(newEntry);
                } else {
                    byte[] data = jarInputStream.readAllBytes();
                    JarEntry newEntry = createNewEntry(entry.getName(), entry, data);
                    jarOutputStream.putNextEntry(newEntry);
                    jarOutputStream.write(data);
                }
            }
            jarOutputStream.closeEntry();
        }
    }

    private static JarEntry createNewEntry(String name, JarEntry entry, byte[] data) {
        int method = entry.getMethod();
        JarEntry newEntry = new JarEntry(name);
        newEntry.setMethod(method);
        if (method == JarEntry.STORED) {
            newEntry.setSize(data.length);
            newEntry.setCompressedSize(data.length);
            CRC32 crc32 = new CRC32();
            crc32.update(data);
            newEntry.setCrc(crc32.getValue());
        }
        return newEntry;
    }

    private static byte[] makeMock(byte[] classBytes) throws Exception {
        ClassPool pool = ClassPool.getDefault();
        CtClass cc = pool.makeClass(new ByteArrayInputStream(classBytes));
        if (cc.isInterface()) {
            return classBytes;
        }
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
        //Add a flag field.
        CtField flagField = CtField.make("private static final java.lang.String __ZENODAENC__=\"HELLOWORLD\";", cc);
        cc.addField(flagField);
        return cc.toBytecode();
    }

    private static byte[] sha256(String input) {
        try {
            // 获取SHA-256 MessageDigest实例
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 计算哈希值
            return digest.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isClassToEncrypt(String entryName, String[] classesToEncrypt) {
        if (classesToEncrypt == null) {
            return true;
        }
        for (String className : classesToEncrypt) {
            className = className.replaceAll("\\.", "/");
            if (entryName.contains(className)) {
                return true;
            }
        }
        return false;
    }

    private static byte[] encryptClassBytes(byte[] classBytes, Cipher cipher) throws Exception {
        return cipher.doFinal(classBytes);
    }

    private static void copyInputStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
    }
}
