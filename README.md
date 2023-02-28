[![](https://jitpack.io/v/nbauma109/jd-core.svg)](https://jitpack.io/#nbauma109/jd-core)
[![](https://jitci.com/gh/nbauma109/jd-core/svg)](https://jitci.com/gh/nbauma109/jd-core)
[![CodeQL](https://github.com/nbauma109/jd-core/actions/workflows/codeql-analysis.yml/badge.svg?branch=master)](https://github.com/nbauma109/jd-core/actions/workflows/codeql-analysis.yml)
[![Maven Release](https://github.com/nbauma109/jd-core/actions/workflows/maven.yml/badge.svg)](https://github.com/nbauma109/jd-core/actions/workflows/maven.yml)
[![Github Release](https://github.com/nbauma109/jd-core/actions/workflows/release.yml/badge.svg)](https://github.com/nbauma109/jd-core/actions/workflows/release.yml)
[![Coverage Status](https://codecov.io/gh/nbauma109/jd-core/branch/master/graph/badge.svg)](https://app.codecov.io/gh/nbauma109/jd-core)

# JD-Core

JD-Core is a JAVA decompiler written in JAVA.

- Java Decompiler project home page:
[http://java-decompiler.github.io](https://java-decompiler.github.io)
- JD-Core source code:
[https://github.com/java-decompiler/jd-core](https://github.com/java-decompiler/jd-core)
- Jitpack maven repository:
[https://jitpack.io/](https://jitpack.io/)

## Description
JD-Core is a standalone JAVA library containing the JAVA decompiler of
"Java Decompiler project". It support Java 1.1.8 to Java 12.0,
including Lambda expressions, method references and default methods.
JD-Core is the engine of JD-GUI.

# Strategy and goals 

Pursuing strategy described in this research paper : 
[The Strengths and Behavioral Quirks of Java Bytecode Decompilers](https://www.researchgate.net/profile/Cesar_Soto-Valero/publication/334465294_The_Strengths_and_Behavioral_Quirks_of_Java_Bytecode_Decompilers/links/ )
Improved recompilation of decompiled source codes
Decompiling and recompiling without any errors :
- commons-io:commons-io:2.11.0
- commons-codec:commons-codec:1.15
- org.apache.commons:commons-collections4:4.4
- org.apache.commons:commons-lang3:3.12.0
- org.apache.logging.log4j:log4j-core:2.20.0
- junit:junit:4.13.2
- and more ... (check unit tests)

## How to build JD-Core ?
```
> git clone https://github.com/java-decompiler/jd-core.git
> cd jd-core
> ./gradlew build
```
generate _"build/libs/jd-core-x.y.z.jar"_

## How to use JD-Core ?

1. Implement the
_[org.jd.core.loader.Loader](https://github.com/java-decompiler/jd-core/blob/master/src/main/java/org/jd/core/v1/api/loader/Loader.java)_
interface,
2. Implement the
_[org.jd.core.printer.Printer](https://github.com/java-decompiler/jd-core/blob/master/src/main/java/org/jd/core/v1/api/printer/Printer.java)_
interface,
3. And call the method _"decompile(loader, printer, internalTypeName);"_

## Example

1. Implement the _Loader_ interface:
```java
Loader loader = new Loader() {
    @Override
    public byte[] load(String internalName) throws IOException {
        InputStream is = this.getClass().getResourceAsStream("/" + internalName + ".class");

        if (is == null) {
            return null;
        } else {
            try (InputStream in=is; ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int read = in.read(buffer);

                while (read > 0) {
                    out.write(buffer, 0, read);
                    read = in.read(buffer);
                }

                return out.toByteArray();
            }
        }
    }

    @Override
    public boolean canLoad(String internalName) {
        return this.getClass().getResource("/" + internalName + ".class") != null;
    }
};
```

2. Implement the _Printer_ interface
```java
Printer printer = new Printer() {
    protected static final String TAB = "  ";
    protected static final String NEWLINE = "\n";

    protected int indentationCount = 0;
    protected StringBuilder sb = new StringBuilder();

    @Override public String toString() { return sb.toString(); }

    @Override public void start(int maxLineNumber, int majorVersion, int minorVersion) {}
    @Override public void end() {}

    @Override public void printText(String text) { sb.append(text); }
    @Override public void printNumericConstant(String constant) { sb.append(constant); }
    @Override public void printStringConstant(String constant, String ownerInternalName) { sb.append(constant); }
    @Override public void printKeyword(String keyword) { sb.append(keyword); }
    @Override public void printDeclaration(int type, String internalTypeName, String name, String descriptor) { sb.append(name); }
    @Override public void printReference(int type, String internalTypeName, String name, String descriptor, String ownerInternalName) { sb.append(name); }

    @Override public void indent() { this.indentationCount++; }
    @Override public void unindent() { this.indentationCount--; }

    @Override public void startLine(int lineNumber) { for (int i=0; i<indentationCount; i++) sb.append(TAB); }
    @Override public void endLine() { sb.append(NEWLINE); }
    @Override public void extraLine(int count) { while (count-- > 0) sb.append(NEWLINE); }

    @Override public void startMarker(int type) {}
    @Override public void endMarker(int type) {}
};
```

3. And call the method _"decompile(loader, printer, internalTypeName);"_
```java
ClassFileToJavaSourceDecompiler decompiler = new ClassFileToJavaSourceDecompiler();

decompiler.decompile(loader, printer, "path/to/YourClass");

String source = printer.toString();
```

## License
Released under the [GNU GPL v3](LICENSE).
