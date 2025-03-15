# 1. Secretj 简介
Secretj 是一个用于对编译后的java程序进行加密的工具软件。

# 2. 特性
- 命令行运行
- 作为maven插件运行
- 支持jar包及嵌套jar包
- 支持spring-boot fat jar
- 运行时输入密码

# 3. 构建方法
## 3.1. 拉取源码
```shell
  git clone https://github.com/zenoda/secretj.git
```
## 3.2. 构建并安装到本地仓库
```shell
    cd secretj
    mvn -U -e -DskipTests clean install
```
# 4. 使用方法
## 4.1. 命令行使用
### 4.1.1. 加密
```shell
    java -jar encrypt-admin/target/encrypt-admin-1.0.0.jar -j sample/target/sample-1.0.0.jar -c org.zenoda.secretj.sample.HelloController -p 12345678
```
- 参数 -j(或--jars) 指定要进行加密的jar包文件路径，可以是相对路径或绝对路径，多个jar包用逗号（,）分隔。
- 参数 -c(或--classes) 指定要进行加密的class文件，支持模糊匹配，多个class用逗号（,）分隔。
- 参数 -p(或--password) 指定加密和解密使用的密码，密码强度暂未控制，可自行掌握。
- 命令执行成功后，会在原始jar文件所在目录生成加密后的jar文件，文件名中添加-encrypted标识。

### 4.1.2. 运行时解密
```shell
    java -javaagent:decrypt-agent/target/decrypt-agent-1.0.0.jar -jar sample/target/sample-1.0.0-encrypted.jar
```
- decrypt-agent用于运行时对class进行解密，可以将编译后的decrypt-agent及其依赖（decrypt-agent/target下的所有jar包）放置在任意文件目录，运行时在javaagent参数后使用完整或相对路径。
- 运行时请根据提示输入正确密码，程序即可正常运行，如密码错误会在加密的class进行加载时报错。**注意：** 由于具体加密的class不同，密码输入错误并不一定会导致程序无法启动，所以请注意验证与加密class相关的功能是否正常。

## 4.2. 作为maven插件使用
- pom.xml中的配置
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
- **注意：** 如果同时配置其他打包插件（如：spring-boot-maven-plugin）,请将本插件放置在其他打包插件之后。
- **注意：** 不要把密码写入pom.xml文件中，以免泄漏，请通过maven运行时环境变量输入密码（例如：mvn -Dpassword=123456）。
