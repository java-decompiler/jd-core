# JD-Core

JD-Core is a JAVA decompiler written in JAVA.

- Java Decompiler projects home page:
[http://java-decompiler.github.io](http://java-decompiler.github.io)
- JD-GUI source code:
[https://github.com/java-decompiler/jd-core](https://github.com/java-decompiler/jd-core)
- Maven repository:
[https://raw.github.com/java-decompiler/mvn-repo/master](https://raw.github.com/java-decompiler/mvn-repo/master)

## Description
JD-Core is a standalone JAVA library containing the JAVA decompiler of
"Java Decompiler project". It support Java 1.1.8 to Java 10.0,
including Lambda expressions, method references and default methods.
JD-Core is the engine of JD-GUI.

## How to build JD-Core ?
```
> git clone https://github.com/java-decompiler/jd-core.git
> cd jd-core
> ./gradlew build
```
generate _"build/libs/jd-core-x.y.z.jar"_

## How to use JD-Core ?

1. Implement the
_[jd.core.loader.Loader](https://github.com/java-decompiler/jd-core/blob/master/src/main/java/org/jd/core/loader/Loader.java)_
interface,
2. Implement the
_[jd.core.printer.Printer](https://github.com/java-decompiler/jd-core/blob/master/src/main/java/org/jd/core/printer/Printer.java)_
interface,
3. And call the method _"decompile(loader, printer, internalTypeName);"_

## Example

1. Implement the _Loader_ interface:
```java
Loader loader = new Loader() {
    @Override
    public byte[] load(String internalName) throws LoaderException {
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
            } catch (IOException e) {
                throw new LoaderException(e);
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
    @Override public void printDeclaration(int flags, String internalTypeName, String name, String descriptor) { sb.append(name); }
    @Override public void printReference(int flags, String internalTypeName, String name, String descriptor, String ownerInternalName) { sb.append(name); }

    @Override public void indent() { this.indentationCount++; }
    @Override public void unindent() { if (this.indentationCount > 0) this.indentationCount--; }

    @Override public void startLine(int lineNumber) { for (int i=0; i<indentationCount; i++) sb.append(TAB); }
    @Override public void endLine() { sb.append(NEWLINE); }
    @Override public void extraLine(int count) { while (count-- > 0) sb.append(NEWLINE); }

    @Override public void startMarker(int type) {}
    @Override public void endMarker(int type) {}
};
```

3. And call the method _"decompile(loader, printer, internalTypeName);"_
```java
ClassFileToJavaSourceDecompiler decompiler = new
ClassFileToJavaSourceDecompiler();

decompiler.decompile(loader, printer, "path/to/YourClass");

String source = printer.toString();
```
