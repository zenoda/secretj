package org.zenoda.secretj.admin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * EncryptPlugin is a Maven plugin used to encrypt class files in jar files
 */
@Mojo(name = "encrypt", defaultPhase = LifecyclePhase.PACKAGE)
public class EncryptPlugin extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;
    @Parameter(property = "password")
    private String password;
    @Parameter(property = "classes.include")
    private String[] classes;
    @Parameter(property = "jars.jar")
    private String[] jars;

    /**
     * Default constructor
     */
    public EncryptPlugin() {
        super();
    }

    @Override
    public void execute() throws MojoFailureException {
        for (int i = 0; i < jars.length; i++) {
            jars[i] = project.getBuild().getDirectory() + File.separator + jars[i];
        }
        EncryptService encryptService = new EncryptService();
        try {
            encryptService.encrypt(jars, classes, password);
        } catch (Exception e) {
            throw new MojoFailureException("Failed to encrypt jars", e);
        }
    }
}
