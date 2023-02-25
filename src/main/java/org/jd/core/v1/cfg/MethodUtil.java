package org.jd.core.v1.cfg;

import org.apache.bcel.classfile.Method;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.javasyntax.CompilationUnit;
import org.jd.core.v1.model.javasyntax.declaration.AnnotationDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.BaseTypeDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.BodyDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.EnumDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.InterfaceDeclaration;
import org.jd.core.v1.model.message.DecompileContext;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorOrMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileStaticInitializerDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.processor.ConvertClassFileProcessor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;
import org.jd.core.v1.service.deserializer.classfile.ClassFileDeserializer;

import java.io.IOException;

public final class MethodUtil {

    private MethodUtil() {
    }

    public static Method searchMethod(Loader loader, TypeMaker typeMaker, String internalTypeName, String methodName, String methodDescriptor) throws IOException {
        ClassFileDeserializer deserializer = new ClassFileDeserializer();
        ConvertClassFileProcessor converter = new ConvertClassFileProcessor();
        DecompileContext decompileContext = new DecompileContext();
        decompileContext.setMainInternalTypeName(internalTypeName);
        decompileContext.setLoader(loader);
        decompileContext.setTypeMaker(typeMaker);

        ClassFile classFile = deserializer.loadClassFile(loader, internalTypeName);
        decompileContext.setClassFile(classFile);

        CompilationUnit compilationUnit = converter.process(classFile, typeMaker, decompileContext);

        BaseTypeDeclaration typeDeclarations = compilationUnit.typeDeclarations();
        BodyDeclaration bodyDeclaration = null;

        if (typeDeclarations instanceof EnumDeclaration) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
            EnumDeclaration ed = (EnumDeclaration) typeDeclarations;
            bodyDeclaration = ed.getBodyDeclaration();
        } else if (typeDeclarations instanceof AnnotationDeclaration) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
            AnnotationDeclaration ad = (AnnotationDeclaration) typeDeclarations;
            bodyDeclaration = ad.getBodyDeclaration();
        } else if (typeDeclarations instanceof InterfaceDeclaration) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
            InterfaceDeclaration id = (InterfaceDeclaration) typeDeclarations;
            bodyDeclaration = id.getBodyDeclaration();
        }

        if (bodyDeclaration != null) {
            ClassFileBodyDeclaration cfbd = (ClassFileBodyDeclaration) bodyDeclaration;

            for (ClassFileConstructorOrMethodDeclaration md : cfbd.getMethodDeclarations()) {
                if (md instanceof ClassFileMethodDeclaration) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                    ClassFileMethodDeclaration cfmd = (ClassFileMethodDeclaration) md;
                    if (cfmd.getName().equals(methodName) && ((methodDescriptor == null) || cfmd.getDescriptor().equals(methodDescriptor))) {
                        return cfmd.getMethod();
                    }
                } else if (md instanceof ClassFileConstructorDeclaration) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                    ClassFileConstructorDeclaration cfcd = (ClassFileConstructorDeclaration) md;
                    if (cfcd.getMethod().getName().equals(methodName) && ((methodDescriptor == null) || cfcd.getDescriptor().equals(methodDescriptor))) {
                        return cfcd.getMethod();
                    }
                } else if (md instanceof ClassFileStaticInitializerDeclaration && ((ClassFileStaticInitializerDeclaration) md).getMethod().getName().equals(methodName)) {
                    // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                    return ((ClassFileStaticInitializerDeclaration) md).getMethod();
                }
            }
        }

        return null;
    }
}
