package org.zenoda.secretj.admin;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Console;

/**
 * EncryptCmd is a tool that runs on the command line to encrypt classes in jar packages.
 */
public class EncryptCmd {
    private static final Logger logger = LoggerFactory.getLogger(EncryptCmd.class);

    private EncryptCmd() {
    }

    /**
     * Entrypoint
     *
     * @param args arguments include -h(help), -p(password), -j(jars), -c(classes)
     */
    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("h", "help", false, "print this message");
        options.addOption("p", "password", true, "password");
        options.addOption("j", "jars", true, "jars for encryption");
        options.addOption("c", "classes", true, "classes for encryption");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            logger.error(e.getMessage());
            System.exit(1);
        }
        if (cmd.hasOption("help")) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("encrypt-admin", options);
            System.exit(0);
        }
        if (!cmd.hasOption("jars")) {
            logger.error("jars not specified");
            System.exit(1);
        }


        String jarsOption = cmd.getOptionValue("jars");
        if (!jarsOption.matches("^[^\\s,]+\\.jar(,[^\\s,]+\\.jar)*$")) {
            logger.error("The format of jars is incorrect. Example: /abc/123/x.jar,../abc/y.jar");
            System.exit(1);
        }
        String classesOption = null;
        cmd.getOptionValue("classes");
        if (cmd.hasOption("c")) {
            classesOption = cmd.getOptionValue("classes");
            if (!classesOption.matches("^[^\\s,/]+(,[^\\s,/]+)*$")) {
                logger.error("The format of classes is incorrect. Example: foo.bar.Abc,one.two");
                System.exit(1);
            }
        }
        String[] jars = jarsOption.split(",");
        String[] classes = classesOption == null ? null : classesOption.split(",");
        String password = null;
        if (cmd.hasOption("p")) {
            password = cmd.getOptionValue("password");
        } else {
            Console console = System.console();
            if (console == null) {
                logger.error("password not specified and no console available");
                System.exit(1);
            }
            while (true) {
                password = new String(console.readPassword("Password: "));
                String confirm = new String(console.readPassword("Password confirm: "));
                if (confirm.equals(password)) {
                    break;
                } else {
                    console.printf("Passwords do not match. Try again.");
                }
            }
        }
        EncryptService encryptService = new EncryptService();
        try {
            encryptService.encrypt(jars, classes, password);
        } catch (Exception e) {
            logger.error(e.getMessage());
            System.exit(1);
        }
    }
}
