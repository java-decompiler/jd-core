/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.classfile.Field;
import org.jd.core.v1.model.classfile.Method;
import org.jd.core.v1.model.classfile.attribute.AttributeExceptions;
import org.jd.core.v1.model.classfile.attribute.AttributeSignature;
import org.jd.core.v1.model.javasyntax.type.*;
import org.jd.core.v1.util.DefaultList;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/*
 * https://jcp.org/aboutJava/communityprocess/maintenance/jsr924/JVMS-SE5.0-Ch4-ClassFile.pdf
 *
 * https://docs.oracle.com/javase/tutorial/extra/generics/methods.html
 *
 * http://www.angelikalanger.com/GenericsFAQ/JavaGenericsFAQ.html
 */
public class SignatureParser {
    protected static final UnknownTypeArgument UNKNOWN_TYPE_ARGUMENT = new UnknownTypeArgument();

    protected HashMap<String, MethodTypes> methodTypesCache = new HashMap<>(1024);
    protected HashMap<String, Type> typeCache = new HashMap<>(1024);
    protected ObjectTypeMaker objectTypeMaker;

    public SignatureParser(ObjectTypeMaker objectTypeMaker) {
        this.objectTypeMaker = objectTypeMaker;

        typeCache.put("B", PrimitiveType.TYPE_BYTE);
        typeCache.put("C", PrimitiveType.TYPE_CHAR);
        typeCache.put("D", PrimitiveType.TYPE_DOUBLE);
        typeCache.put("F", PrimitiveType.TYPE_FLOAT);
        typeCache.put("I", PrimitiveType.TYPE_INT);
        typeCache.put("J", PrimitiveType.TYPE_LONG);
        typeCache.put("S", PrimitiveType.TYPE_SHORT);
        typeCache.put("Z", PrimitiveType.TYPE_BOOLEAN);
        typeCache.put("java/lang/Class", ObjectType.TYPE_CLASS);
        typeCache.put("java/lang/Object", ObjectType.TYPE_OBJECT);
        typeCache.put("java/lang/String", ObjectType.TYPE_STRING);
    }

    /**
     * Rules:
     *  ClassSignature: TypeParameters? SuperclassSignature SuperInterfaceSignature*
     *  SuperclassSignature: ClassTypeSignature
     *  SuperInterfaceSignature: ClassTypeSignature
     */
    @SuppressWarnings("unchecked")
    public TypeTypes parseClassFileSignature(ClassFile classFile) {
        TypeTypes typeTypes = new TypeTypes();
        String internalTypeName = classFile.getInternalTypeName();

        typeTypes.thisType = objectTypeMaker.makeFromInternalTypeName(internalTypeName);

        AttributeSignature attributeSignature = classFile.getAttribute("Signature");

        if (attributeSignature == null) {
            // Create 'typeSignature' with classFile start
            String superTypeName = classFile.getSuperTypeName();
            String[] interfaceTypeNames = classFile.getInterfaceTypeNames();

            if (! "java/lang/Object".equals(superTypeName)) {
                typeTypes.superType = objectTypeMaker.makeFromInternalTypeName(superTypeName);
            }

            if (interfaceTypeNames != null) {
                int length = interfaceTypeNames.length;

                if (length == 1) {
                    typeTypes.interfaces = objectTypeMaker.makeFromInternalTypeName(interfaceTypeNames[0]);
                } else {
                    Types list = new Types(length);
                    for (String interfaceTypeName : interfaceTypeNames) {
                        list.add(objectTypeMaker.makeFromInternalTypeName(interfaceTypeName));
                    }
                    typeTypes.interfaces = list;
                }
            }
        } else {
            // Parse 'signature' attribute
            SignatureReader reader = new SignatureReader(attributeSignature.getSignature());

            typeTypes.typeParameters = parseTypeParameters(reader);
            typeTypes.superType = parseClassTypeSignature(reader, 0);

            Type firstInterface = parseClassTypeSignature(reader, 0);

            if (firstInterface != null) {
                Type nextInterface = parseClassTypeSignature(reader, 0);

                if (nextInterface == null) {
                    typeTypes.interfaces = firstInterface;
                } else {
                    Types list = new Types(classFile.getInterfaceTypeNames().length);

                    list.add(firstInterface);

                    do {
                        list.add(nextInterface);
                        nextInterface = parseClassTypeSignature(reader, 0);
                    } while (nextInterface != null);

                    typeTypes.interfaces = list;
                }
            }
        }

        return typeTypes;
    }

    public MethodTypes parseConstructorSignature(Method method) {
        AttributeSignature attributeSignature = method.getAttribute("Signature");

        if (attributeSignature == null) {
            return parseMethodSignature(method.getDescriptor(), method);
        } else {
            // Signature does not contain synthetic parameters like outer type name, for example.
            MethodTypes mt1 = parseMethodSignature(attributeSignature.getSignature(), method);
            MethodTypes mt2 = parseMethodSignature(method.getDescriptor(), method);

            if (mt1.parameters.size() == mt2.parameters.size()) {
                return mt1;
            } else if (mt1.parameters.isEmpty() && (mt1.typeParameters == null)) {
                return mt2;
            } else {
                DefaultList<Type> parameters = new DefaultList<>(mt2.parameters);

                parameters.subList(1, 1+mt1.parameters.size()).clear();
                parameters.addAll(1, mt1.parameters);

                MethodTypes mt3 = new MethodTypes();

                mt3.typeParameters = mt1.typeParameters;
                mt3.parameters = parameters;
                mt3.returned = mt1.returned;
                mt3.exceptions = mt1.exceptions;

                return mt3;
            }
        }
    }

    public MethodTypes parseMethodSignature(Method method) {
        AttributeSignature attributeSignature = method.getAttribute("Signature");
        String signature = (attributeSignature == null) ? method.getDescriptor() : attributeSignature.getSignature();
        return parseMethodSignature(signature, method);
    }

    public Type parseFieldSignature(Field field) {
        AttributeSignature attributeSignature = field.getAttribute("Signature");
        String signature = (attributeSignature == null) ? field.getDescriptor() : attributeSignature.getSignature();
        return parseTypeSignature(signature);
    }

    public Type parseTypeSignature(String signature) {
        Type type = typeCache.get(signature);

        if (type == null) {
            SignatureReader reader = new SignatureReader(signature);
            type = parseReferenceTypeSignature(reader);
            typeCache.put(signature, type);
        }

        return type;
    }

    public List<Type> parseParameterTypes(String signature) {
        MethodTypes methodTypes = parseMethodSignature(signature, null);
        return (methodTypes==null) ? Collections.<Type>emptyList() : methodTypes.parameters;
    }

    public Type parseReturnedType(String signature) {
        MethodTypes methodTypes = parseMethodSignature(signature, null);
        return (methodTypes==null) ? null : methodTypes.returned;
    }

    public static int countDimension(String descriptor) {
        int count = 0;

        for (int i=0, len=descriptor.length(); (i<len) && (descriptor.charAt(i)=='['); i++) {
            count++;
        }

        return count;
    }

    /**
     * Rules:
     *  MethodTypeSignature: TypeParameters? '(' ReferenceTypeSignature* ')' ReturnType ThrowsSignature*
     *  ReturnType: TypeSignature | VoidDescriptor
     *  ThrowsSignature: '^' ClassTypeSignature | '^' TypeVariableSignature
     */
    @SuppressWarnings("unchecked")
    protected MethodTypes parseMethodSignature(String signature, Method method) {
        String cacheKey = signature;
        boolean containsThrowsSignature = (signature.indexOf('^') != -1);

        if (!containsThrowsSignature && (method != null)) {
            AttributeExceptions attributeExceptions = method.getAttribute("Exceptions");

            if (attributeExceptions != null) {
                StringBuilder sb = new StringBuilder(signature);

                for (String exceptionTypeName : attributeExceptions.getExceptionTypeNames()) {
                    sb.append("^L").append(exceptionTypeName).append(';');
                }

                cacheKey = sb.toString();
            }
        }

        MethodTypes methodTypes = methodTypesCache.get(cacheKey);

        if (methodTypes == null) {
            SignatureReader reader = new SignatureReader(signature);

            // Type parameterTypes
            methodTypes = new MethodTypes();
            methodTypes.typeParameters = parseTypeParameters(reader);

            // Parameters
            if (reader.read() != '(')
                throw new SignatureFormatException(signature);

            Type firstParameter = parseReferenceTypeSignature(reader);

            if (firstParameter == null) {
                methodTypes.parameters = Collections.emptyList();
            } else {
                Type nextParameter = parseReferenceTypeSignature(reader);

                if (nextParameter == null) {
                    methodTypes.parameters = Collections.singletonList(firstParameter);
                } else {
                    DefaultList<Type> list = new DefaultList<>();

                    list.add(firstParameter);

                    do {
                        list.add(nextParameter);
                        nextParameter = parseReferenceTypeSignature(reader);
                    } while (nextParameter != null);

                    methodTypes.parameters = list;
                }
            }

            if (reader.read() != ')')
                throw new SignatureFormatException(signature);

            // Result
            methodTypes.returned = parseReferenceTypeSignature(reader);

            // Exceptions
            Type firstException = parseExceptionSignature(reader);

            if (firstException == null) {
                // Signature does not contain exceptions
                if (method != null) {
                    AttributeExceptions attributeExceptions = method.getAttribute("Exceptions");

                    if (attributeExceptions != null) {
                        String[] exceptionTypeNames = attributeExceptions.getExceptionTypeNames();

                        if (exceptionTypeNames.length == 1) {
                            methodTypes.exceptions = objectTypeMaker.makeFromInternalTypeName(exceptionTypeNames[0]);
                        } else {
                            Types list = new Types(exceptionTypeNames.length);

                            for (String exceptionTypeName : exceptionTypeNames) {
                                list.add(objectTypeMaker.makeFromInternalTypeName(exceptionTypeName));
                            }

                            methodTypes.exceptions = list;
                        }
                    }
                }
            } else {
                Type nextException = parseExceptionSignature(reader);

                if (nextException == null) {
                    methodTypes.exceptions = firstException;
                } else {
                    Types list = new Types();

                    list.add(firstException);

                    do {
                        list.add(nextException);
                        nextException = parseExceptionSignature(reader);
                    } while (nextException != null);

                    methodTypes.exceptions = list;
                }
            }

            methodTypesCache.put(cacheKey, methodTypes);
        }

        return methodTypes;
    }

    /**
     * Rules:
     *  TypeParameters: '<' TypeParameter+ '>'
     */
    @SuppressWarnings("unchecked")
    protected BaseTypeParameter parseTypeParameters(SignatureReader reader) {
        if (reader.nextEqualsTo('<')) {
            // Skip '<'
            reader.index++;

            TypeParameter firstTypeParameter = parseTypeParameter(reader);

            if (firstTypeParameter == null)
                throw new SignatureFormatException(reader.signature);

            TypeParameter nextTypeParameter = parseTypeParameter(reader);
            BaseTypeParameter typeParameters;

            if (nextTypeParameter == null) {
                typeParameters = firstTypeParameter;
            } else {
                TypeParameters list = new TypeParameters();
                list.add(firstTypeParameter);

                do {
                    list.add(nextTypeParameter);
                    nextTypeParameter = parseTypeParameter(reader);
                } while (nextTypeParameter != null);

                typeParameters = list;
            }

            if (reader.read() != '>')
                throw new SignatureFormatException(reader.signature);

            return typeParameters;
        } else {
            return null;
        }
    }

    /**
     * Rules:
     *  TypeParameter: Identifier ClassBound InterfaceBound*
     *  ClassBound: ':' FieldTypeSignature?
     *  InterfaceBound: ':' FieldTypeSignature
     */
    @SuppressWarnings("unchecked")
    protected TypeParameter parseTypeParameter(SignatureReader reader) {
        int fistIndex = reader.index;

        // Search ':'
        if (reader.search(':')) {
            String identifier = reader.substring(fistIndex);

            // Parser bounds
            Type firstBound = null;
            TypeBounds types = null;

            while (reader.nextEqualsTo(':')) {
                // Skip ':'
                reader.index++;

                Type bound = parseReferenceTypeSignature(reader);

                if ((bound != null) && !bound.getDescriptor().equals("Ljava/lang/Object;")) {
                    if (firstBound == null) {
                        firstBound = bound;
                    } else if (types == null) {
                        types = new TypeBounds();
                        types.add(firstBound);
                        types.add(bound);
                    } else {
                        types.add(bound);
                    }
                }
            }

            if (firstBound == null) {
                return new TypeParameter(identifier);
            } else if (types == null) {
                return new TypeParameterWithTypeBounds(identifier, firstBound);
            } else {
                return new TypeParameterWithTypeBounds(identifier, types);
            }
        } else {
            return null;
        }
    }

    /**
     * Rules:
     *  ThrowsSignature: '^' ClassTypeSignature | '^' TypeVariableSignature
     */
    protected Type parseExceptionSignature(SignatureReader reader) {
        if (reader.nextEqualsTo('^')) {
            // Skip '^'
            reader.index++;

            return parseReferenceTypeSignature(reader);
        } else {
            return null;
        }
    }

    /**
     * Rules:
     *  ReferenceTypeSignature: ClassTypeSignature | ArrayTypeSignature | TypeVariableSignature
     *  SimpleClassTypeSignature: Identifier TypeArguments?
     *  ArrayTypeSignature: '[' TypeSignature
     *  TypeSignature: '[' FieldTypeSignature | '[' BaseType
     *  BaseType: 'B' | 'C' | 'D' | 'F' | 'I' | 'J' | 'S' | 'Z'
     *  TypeVariableSignature: 'T' Identifier ';'
     */
    protected Type parseReferenceTypeSignature(SignatureReader reader) {
        if (reader.available()) {
            int dimension = 0;
            char c = reader.read();

            while (c == '[') {
                dimension++;
                c = reader.read();
            }

            switch (c) {
                case 'B':
                    return (dimension == 0) ? PrimitiveType.TYPE_BYTE : PrimitiveType.TYPE_BYTE.createType(dimension);
                case 'C':
                    return (dimension == 0) ? PrimitiveType.TYPE_CHAR : PrimitiveType.TYPE_CHAR.createType(dimension);
                case 'D':
                    return (dimension == 0) ? PrimitiveType.TYPE_DOUBLE : PrimitiveType.TYPE_DOUBLE.createType(dimension);
                case 'F':
                    return (dimension == 0) ? PrimitiveType.TYPE_FLOAT : PrimitiveType.TYPE_FLOAT.createType(dimension);
                case 'I':
                    return (dimension == 0) ? PrimitiveType.TYPE_INT : PrimitiveType.TYPE_INT.createType(dimension);
                case 'J':
                    return (dimension == 0) ? PrimitiveType.TYPE_LONG : PrimitiveType.TYPE_LONG.createType(dimension);
                case 'L':
                    // Unread 'L'
                    reader.index--;
                    return parseClassTypeSignature(reader, dimension);
                case 'S':
                    return (dimension == 0) ? PrimitiveType.TYPE_SHORT : PrimitiveType.TYPE_SHORT.createType(dimension);
                case 'T':
                    int index = reader.index;

                    if (reader.search(';') == false)
                        return null;

                    String identifier = reader.substring(index);

                    // Skip ';'
                    reader.index++;

                    return new GenericType(identifier, dimension);
                case 'V':
                    assert dimension == 0;
                    return PrimitiveType.TYPE_VOID;
                case 'Z':
                    return (dimension == 0) ? PrimitiveType.TYPE_BOOLEAN : PrimitiveType.TYPE_BOOLEAN.createType(dimension);
                default:
                    // Unread 'c'
                    reader.index--;
                    return null;
            }
        } else {
            return null;
        }
    }

    protected boolean isAReferenceTypeSignature(SignatureReader reader) {
        if (reader.available()) {
            // Skip dimension
            char c = reader.read();

            while (c == '[') {
                c = reader.read();
            }

            switch (c) {
                case 'B': case 'C': case 'D': case 'F': case 'I': case 'J':
                    return true;
                case 'L':
                    // Unread 'L'
                    reader.index--;
                    return isAClassTypeSignature(reader);
                case 'S':
                    return true;
                case 'T':
                    reader.searchEndMarker();
                    return true;
                case 'V': case 'Z':
                    return true;
                default:
                    // Unread 'c'
                    reader.index--;
                    return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Rules:
     *  ClassTypeSignature: 'L' PackageSpecifier* SimpleClassTypeSignature ClassTypeSignatureSuffix* ';'
     *  SimpleClassTypeSignature: Identifier TypeArguments?
     *  ClassTypeSignatureSuffix: '.' SimpleClassTypeSignature
     */
    protected Type parseClassTypeSignature(SignatureReader reader, int dimension) {
        if (reader.nextEqualsTo('L')) {
            // Skip 'L'. Parse 'PackageSpecifier* SimpleClassTypeSignature'
            int index = ++reader.index;
            char endMarker = reader.searchEndMarker();

            if (endMarker == 0) {
                throw new SignatureFormatException(reader.signature);
            }

            String internalTypeName = reader.substring(index);
            ObjectType ot = objectTypeMaker.makeFromInternalTypeName(internalTypeName);

            if (endMarker == '<') {
                // Skip '<'
                reader.index++;
                ot = ot.createType(parseTypeArguments(reader));
                if (reader.read() != '>')
                    throw new SignatureFormatException(reader.signature);
            }

            // Parse 'ClassTypeSignatureSuffix*'
            while (reader.nextEqualsTo('.')) {
                // Skip '.'
                index = ++reader.index;
                endMarker = reader.searchEndMarker();

                if (endMarker == 0) {
                    throw new SignatureFormatException(reader.signature);
                }

                String name = reader.substring(index);
                internalTypeName += '$' + name;
                String qualifiedName;

                if (Character.isDigit(name.charAt(0))) {
                    name = extractLocalClassName(name);
                    qualifiedName = null;
                } else {
                    qualifiedName = ot.getQualifiedName() + '.' + name;
                }

                if (endMarker == '<') {
                    // Skip '<'
                    reader.index++;

                    BaseTypeArgument typeArguments = parseTypeArguments(reader);
                    if (reader.read() != '>') {
                        throw new SignatureFormatException(reader.signature);
                    }

                    ot = new InnerObjectType(internalTypeName, qualifiedName, name, typeArguments, ot);
                } else {
                    ot = new InnerObjectType(internalTypeName, qualifiedName, name, ot);
                }
            }

            // Skip ';'
            reader.index++;

            return (dimension==0) ? ot : ot.createType(dimension);
        } else {
            return null;
        }
    }

    protected boolean isAClassTypeSignature(SignatureReader reader) {
        if (reader.nextEqualsTo('L')) {
            // Skip 'L'
            reader.index++;

            // Parse 'PackageSpecifier* SimpleClassTypeSignature'
            char endMarker = reader.searchEndMarker();

            if (endMarker == 0)
                throw new SignatureFormatException(reader.signature);

            if (endMarker == '<') {
                // Skip '<'
                reader.index++;
                isATypeArguments(reader);
                if (reader.read() != '>')
                    throw new SignatureFormatException(reader.signature);
            }

            // Parse 'ClassTypeSignatureSuffix*'
            while (reader.nextEqualsTo('.')) {
                // Skip '.'
                reader.index++;

                endMarker = reader.searchEndMarker();

                if (endMarker == 0)
                    throw new SignatureFormatException(reader.signature);

                if (endMarker == '<') {
                    // Skip '<'
                    reader.index++;
                    isATypeArguments(reader);
                    if (reader.read() != '>')
                        throw new SignatureFormatException(reader.signature);
                }
            }

            // Skip ';'
            reader.index++;

            return true;
        } else {
            return false;
        }
    }

    /**
     * Rules:
     *  TypeArguments: '<' TypeArgument+ '>'
     */
    protected BaseTypeArgument parseTypeArguments(SignatureReader reader) {
        TypeArgument firstTypeArgument = parseTypeArgument(reader);

        if (firstTypeArgument == null) {
            throw new SignatureFormatException(reader.signature);
        }

        TypeArgument nextTypeArgument = parseTypeArgument(reader);

        if (nextTypeArgument == null) {
            return firstTypeArgument;
        } else {
            ArrayTypeArguments typeArguments = new ArrayTypeArguments();
            typeArguments.add(firstTypeArgument);

            do {
                typeArguments.add(nextTypeArgument);
                nextTypeArgument = parseTypeArgument(reader);
            } while (nextTypeArgument != null);

            return typeArguments;
        }
    }

    protected boolean isATypeArguments(SignatureReader reader) {
        if (isATypeArgument(reader) == false)
            throw new SignatureFormatException(reader.signature);

        while (isATypeArgument(reader));

        return true;
    }

    /**
     * Rules:
     *  TypeArgument: WildcardIndicator? FieldTypeSignature | '*'
     *  WildcardIndicator: '+' | '-'
     */
    protected TypeArgument parseTypeArgument(SignatureReader reader) {
        switch (reader.read()) {
            case '+':
                return new WildcardExtendsTypeArgument(parseReferenceTypeSignature(reader));
            case '-':
                return new WildcardSuperTypeArgument(parseReferenceTypeSignature(reader));
            case '*':
                return UNKNOWN_TYPE_ARGUMENT;
            default:
                // Unread 'c'
                reader.index--;
                return parseReferenceTypeSignature(reader);
        }
    }

    protected boolean isATypeArgument(SignatureReader reader) {
        switch (reader.read()) {
            case '+': case '-':
                return isAReferenceTypeSignature(reader);
            case '*':
                return true;
            default:
                // Unread 'c'
                reader.index--;
                return isAReferenceTypeSignature(reader);
        }
    }

    protected static String extractLocalClassName(String name) {
        if (Character.isDigit(name.charAt(0))) {
            int i = 0, len = name.length();

            while ((i < len) && Character.isDigit(name.charAt(i))) {
                i++;
            }

            return (i == len) ? null : name.substring(i);
        }

        return name;
    }

    private final static class SignatureReader {
        public String signature;
        public char[] array;
        public int length;
        public int index = 0;

        public SignatureReader(String signature) {
            this(signature, 0);
        }

        public SignatureReader(String signature, int index) {
            this.signature = signature;
            this.array = signature.toCharArray();
            this.length = array.length;
            this.index = index;
        }

        public char read() {
            return array[index++];
        }

        public boolean nextEqualsTo(char c) {
            return (index < length) && (array[index] == c);
        }

        public boolean search(char c) {
            int length = array.length;

            for (int i=index; i<length; i++) {
                if (array[i] == c) {
                    index = i;
                    return true;
                }
            }

            return false;
        }

        public char searchEndMarker() {
            int length = array.length;

            while (index < length) {
                char c = array[index];

                if ((c == '<') || (c == ';')) {
                    return c;
                }

                index++;
            }

            return 0;
        }

        public boolean available() {
            return index < length;
        }

        public String substring(int beginIndex) {
            return new String(array, beginIndex, index-beginIndex);
        }

        @Override
        public String toString() {
            return "SignatureReader{index=" + index + ", nextChars=" + (new String(array, index, length-index)) + "}";
        }
    }

    public static class TypeTypes {
        public ObjectType thisType;
        public BaseTypeParameter typeParameters;
        public Type              superType;
        public BaseType interfaces;
    }

    public static class MethodTypes {
        public BaseTypeParameter typeParameters;
        public List<Type>        parameters;
        public Type              returned;
        public BaseType exceptions;
    }
}
