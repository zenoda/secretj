# 1.  Introduction
Secretj is a tool software used to encrypt compiled Java programs.

# 2.  Features
- Command line operation
- Run as Maven plugin
- Support jar packages and nested jar packages
- Support spring boot fat jar
- Enter password at startup

# 3.  Build
## 3.1.  Pull source code
```shell
  git clone https://github.com/zenoda/secretj.git
```
## 3.2. Build and install to local repository
```shell
    cd secretj
    mvn -U -e -DskipTests clean install
```
# 4.  Usage
## 4.1.  Command line
### 4.1.1.  Encryption
```shell
    java -jar encrypt-admin/target/encrypt-admin-1.0.0.jar -j sample/target/sample-1.0.0.jar -c org.zenoda.secretj.sample.HelloController -p 12345678
```
- The parameter -j (or --jars) specifies the path of the jar package file to be encrypted, which can be a relative path or an absolute path. Multiple jar packages are separated by commas (,).
- The parameter -c (or --classes) specifies the class file to be encrypted, supports fuzzy matching, and separates multiple classes with commas (,).
- The parameter -p (or --password) specifies the password used for encryption and decryption. The strength of the password is not yet controlled and can be controlled by oneself.
- After the command is executed successfully, an encrypted jar file will be generated in the directory where the original jar file is located, with the - encrypted identifier added to the file name.

### 4.1.2. Decryption at startup
```shell
    java -javaagent:decrypt-agent/target/decrypt-agent-1.0.0.jar -jar sample/target/sample-1.0.0-encrypted.jar
```
- decrypt-agent is used to decrypt classes at runtime. The compiled decrypt agent and its dependencies (all jar files under decrypt-agent/target) can be placed in any file directory, and the runtime uses the full or relative path after the Javaagent parameter.
- Please enter the correct password according to the prompts at startup, and the program will run normally. If the password is incorrect, an error will be reported when loading the encrypted class. **Attention:** Due to different encryption classes, entering the wrong password may not necessarily cause the program to fail to start. Therefore, please verify whether the functions related to the encrypted classes are working properly.

## 4.2. Used as a Maven plugin
- Configuration in pom.xml
```xml
<!-- .... -->
<plugin>
    <groupId>org.zenoda.secretj</groupId>
    <artifactId>encrypt-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <classes>
            <include>org.zenoda.secretj</include>
        </classes>
        <jars>
            <jar>sample-1.0.0.jar</jar>
        </jars>
        <password>${password}</password>
    </configuration>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>encrypt</goal>
            </goals>
        </execution>
    </executions>
</plugin>
<!-- .... -->
```
- **Note:** If other packaging plugins (such as spring-boot-maven-plugin) are configured at the same time, please place this plugin after the other packaging plugins.
- **Note:** Do not write the password into the pom.xml to avoid leakage. Please enter the password through the maven runtime environment variable (such as 'mvn -Dpassword=123456').
