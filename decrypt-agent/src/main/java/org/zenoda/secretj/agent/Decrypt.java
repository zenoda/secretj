package org.zenoda.secretj.agent;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import org.apache.commons.io.IOUtils;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.ProtectionDomain;
import java.util.Scanner;
import java.util.logging.Logger;

public class Decrypt {
    private static final Logger logger = Logger.getLogger(Decrypt.class.getName());
    private static byte[] passwordBytes = null;
    private static final String ALGORITHM = "AES";

    public static void premain(String agentArgs, Instrumentation inst) {
        Console console = System.console();
        String password = new String(console.readPassword("Password: "));
        passwordBytes = sha256(password);
        inst.addTransformer(new DecryptClassFileTransformer(), true);
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

    private static class DecryptClassFileTransformer implements ClassFileTransformer {
        private final static String FLAG_FIELD_NAME = "__ZENODAENC__";

        @Override
        public byte[] transform(ClassLoader loader,
                                String className,
                                Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain,
                                byte[] classFileBuffer) throws IllegalClassFormatException {
            byte[] result = classFileBuffer;
            if (isEncryptedClass(classFileBuffer)) {
                String resName = className + ".class.enc";
                InputStream encryptedClassFileInputStream = loader.getResourceAsStream(resName);
                try {
                    result = decrypt(encryptedClassFileInputStream);
                } catch (Exception e) {
                    logger.severe("Decrypt error:" + e.getMessage());
                }
            }
            return result;
        }


        private boolean isEncryptedClass(byte[] classFileBuffer) throws IllegalClassFormatException {
            try {
                ClassPool classPool = ClassPool.getDefault();
                CtClass cc = classPool.makeClass(new ByteArrayInputStream(classFileBuffer));
                try {
                    cc.getDeclaredField(FLAG_FIELD_NAME);
                    return true;
                } catch (NotFoundException ne) {
                    return false;
                }
            } catch (IOException e) {
                throw new IllegalClassFormatException(e.getMessage());
            }
        }

        private byte[] decrypt(InputStream input) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
            byte[] classBytes = IOUtils.toByteArray(input);
            SecretKey secretKey = new SecretKeySpec(passwordBytes, ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return cipher.doFinal(classBytes);
        }
    }
}
