/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.classfile.Field;
import org.jd.core.v1.model.classfile.Method;
import org.jd.core.v1.model.classfile.attribute.AttributeExceptions;
import org.jd.core.v1.model.classfile.attribute.AttributeSignature;
import org.jd.core.v1.model.classfile.constant.ConstantClass;
import org.jd.core.v1.model.javasyntax.type.*;
import org.jd.core.v1.service.deserializer.classfile.ClassFileFormatException;
import org.jd.core.v1.service.deserializer.classfile.ClassFileReader;
import org.jd.core.v1.util.DefaultList;

import java.util.HashMap;

import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_UNDEFINED_OBJECT;

/*
 * https://jcp.org/aboutJava/communityprocess/maintenance/jsr924/JVMS-SE5.0-Ch4-ClassFile.pdf
 *
 * https://docs.oracle.com/javase/tutorial/extra/generics/methods.html
 *
 * http://www.angelikalanger.com/GenericsFAQ/JavaGenericsFAQ.html
 */
public class TypeMaker {
    protected HashMap<String, MethodTypes> signatureToMethodTypes = new HashMap<>(1024);
    protected HashMap<String, Type> signatureToType = new HashMap<>(1024);

    public TypeMaker(Loader loader) {
        this.loader = loader;

        signatureToType.put("B", PrimitiveType.TYPE_BYTE);
        signatureToType.put("C", PrimitiveType.TYPE_CHAR);
        signatureToType.put("D", PrimitiveType.TYPE_DOUBLE);
        signatureToType.put("F", PrimitiveType.TYPE_FLOAT);
        signatureToType.put("I", PrimitiveType.TYPE_INT);
        signatureToType.put("J", PrimitiveType.TYPE_LONG);
        signatureToType.put("S", PrimitiveType.TYPE_SHORT);
        signatureToType.put("Z", PrimitiveType.TYPE_BOOLEAN);
        signatureToType.put("java/lang/Class", ObjectType.TYPE_CLASS);
        signatureToType.put("java/lang/Object", ObjectType.TYPE_OBJECT);
        signatureToType.put("java/lang/String", ObjectType.TYPE_STRING);
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

        typeTypes.thisType = makeFromInternalTypeName(internalTypeName);

        AttributeSignature attributeSignature = classFile.getAttribute("Signature");

        if (attributeSignature == null) {
            // Create 'typeSignature' with classFile start
            String superTypeName = classFile.getSuperTypeName();
            String[] interfaceTypeNames = classFile.getInterfaceTypeNames();

            if (! "java/lang/Object".equals(superTypeName)) {
                typeTypes.superType = makeFromInternalTypeName(superTypeName);
            }

            if (interfaceTypeNames != null) {
                int length = interfaceTypeNames.length;

                if (length == 1) {
                    typeTypes.interfaces = makeFromInternalTypeName(interfaceTypeNames[0]);
                } else {
                    Types list = new Types(length);
                    for (String interfaceTypeName : interfaceTypeNames) {
                        list.add(makeFromInternalTypeName(interfaceTypeName));
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
            // Signature does not contain synthetic parameterTypes like outer type name, for example.
            MethodTypes mt1 = parseMethodSignature(attributeSignature.getSignature(), method);
            MethodTypes mt2 = parseMethodSignature(method.getDescriptor(), method);

            if (mt1.parameterTypes.size() == mt2.parameterTypes.size()) {
                return mt1;
            } else if (mt1.parameterTypes.isEmpty() && (mt1.typeParameters == null)) {
                return mt2;
            } else {
                DefaultList<Type> parameters = new DefaultList<>(mt2.parameterTypes);

                parameters.subList(1, 1+mt1.parameterTypes.size()).clear();
                parameters.addAll(1, mt1.parameterTypes);

                MethodTypes mt3 = new MethodTypes();

                mt3.typeParameters = mt1.typeParameters;
                mt3.parameterTypes = parameters;
                mt3.returnedType = mt1.returnedType;
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
        Type type = signatureToType.get(signature);

        if (type == null) {
            SignatureReader reader = new SignatureReader(signature);
            type = parseReferenceTypeSignature(reader);
            signatureToType.put(signature, type);
        }

        return type;
    }

    public DefaultList<Type> parseParameterTypes(String signature) {
        MethodTypes methodTypes = parseMethodSignature(signature, null);
        return (methodTypes==null) ? DefaultList.<Type>emptyList() : methodTypes.parameterTypes;
    }

    public Type parseReturnedType(String signature) {
        MethodTypes methodTypes = parseMethodSignature(signature, null);
        return (methodTypes==null) ? null : methodTypes.returnedType;
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

        MethodTypes methodTypes = signatureToMethodTypes.get(cacheKey);

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
                methodTypes.parameterTypes = DefaultList.emptyList();
            } else {
                Type nextParameter = parseReferenceTypeSignature(reader);
                DefaultList<Type> list = new DefaultList<>();

                list.add(firstParameter);

                while (nextParameter != null) {
                    list.add(nextParameter);
                    nextParameter = parseReferenceTypeSignature(reader);
                }

                methodTypes.parameterTypes = list;
            }

            if (reader.read() != ')')
                throw new SignatureFormatException(signature);

            // Result
            methodTypes.returnedType = parseReferenceTypeSignature(reader);

            // Exceptions
            Type firstException = parseExceptionSignature(reader);

            if (firstException == null) {
                // Signature does not contain exceptions
                if (method != null) {
                    AttributeExceptions attributeExceptions = method.getAttribute("Exceptions");

                    if (attributeExceptions != null) {
                        String[] exceptionTypeNames = attributeExceptions.getExceptionTypeNames();

                        if (exceptionTypeNames.length == 1) {
                            methodTypes.exceptions = makeFromInternalTypeName(exceptionTypeNames[0]);
                        } else {
                            Types list = new Types(exceptionTypeNames.length);

                            for (String exceptionTypeName : exceptionTypeNames) {
                                list.add(makeFromInternalTypeName(exceptionTypeName));
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

            signatureToMethodTypes.put(cacheKey, methodTypes);
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
    protected ObjectType parseClassTypeSignature(SignatureReader reader, int dimension) {
        if (reader.nextEqualsTo('L')) {
            // Skip 'L'. Parse 'PackageSpecifier* SimpleClassTypeSignature'
            int index = ++reader.index;
            char endMarker = reader.searchEndMarker();

            if (endMarker == 0) {
                throw new SignatureFormatException(reader.signature);
            }

            String internalTypeName = reader.substring(index);
            ObjectType ot = makeFromInternalTypeName(internalTypeName);

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

            return (dimension==0) ? ot : (ObjectType)ot.createType(dimension);
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
                return WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT;
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
        public ObjectType superType;
        public BaseType interfaces;
    }

    public static class MethodTypes {
        public BaseTypeParameter typeParameters;
        public DefaultList<Type> parameterTypes;
        public Type returnedType;
        public BaseType exceptions;
    }

    protected static final HashMap<String, ObjectType> INTERNALNAME_TO_OBJECTTYPE = new HashMap<>();
    protected static final HashMap<String, ObjectType> INTERNALNAME_TO_OBJECTPRIMITIVETYPE = new HashMap<>();

    static {
        INTERNALNAME_TO_OBJECTTYPE.put(ObjectType.TYPE_CLASS.getInternalName(),             ObjectType.TYPE_CLASS);
        INTERNALNAME_TO_OBJECTTYPE.put(ObjectType.TYPE_OBJECT.getInternalName(),            ObjectType.TYPE_OBJECT);
        INTERNALNAME_TO_OBJECTTYPE.put(ObjectType.TYPE_STRING.getInternalName(),            ObjectType.TYPE_STRING);
        INTERNALNAME_TO_OBJECTTYPE.put(ObjectType.TYPE_THROWABLE.getInternalName(),         ObjectType.TYPE_THROWABLE);
        INTERNALNAME_TO_OBJECTTYPE.put(ObjectType.TYPE_EXCEPTION.getInternalName(),         ObjectType.TYPE_EXCEPTION);
        INTERNALNAME_TO_OBJECTTYPE.put(ObjectType.TYPE_RUNTIME_EXCEPTION.getInternalName(), ObjectType.TYPE_RUNTIME_EXCEPTION);
        INTERNALNAME_TO_OBJECTTYPE.put(ObjectType.TYPE_SYSTEM.getInternalName(),            ObjectType.TYPE_SYSTEM);
        INTERNALNAME_TO_OBJECTTYPE.put(ObjectType.TYPE_STRING_BUILDER.getInternalName(),    ObjectType.TYPE_STRING_BUILDER);
        INTERNALNAME_TO_OBJECTTYPE.put(ObjectType.TYPE_STRING_BUFFER.getInternalName(),     ObjectType.TYPE_STRING_BUFFER);
        INTERNALNAME_TO_OBJECTTYPE.put(ObjectType.TYPE_THREAD.getInternalName(),            ObjectType.TYPE_THREAD);

        INTERNALNAME_TO_OBJECTPRIMITIVETYPE.put(ObjectType.TYPE_PRIMITIVE_BOOLEAN.getInternalName(), ObjectType.TYPE_PRIMITIVE_BOOLEAN);
        INTERNALNAME_TO_OBJECTPRIMITIVETYPE.put(ObjectType.TYPE_PRIMITIVE_BYTE.getInternalName(),    ObjectType.TYPE_PRIMITIVE_BYTE);
        INTERNALNAME_TO_OBJECTPRIMITIVETYPE.put(ObjectType.TYPE_PRIMITIVE_CHAR.getInternalName(),    ObjectType.TYPE_PRIMITIVE_CHAR);
        INTERNALNAME_TO_OBJECTPRIMITIVETYPE.put(ObjectType.TYPE_PRIMITIVE_DOUBLE.getInternalName(),  ObjectType.TYPE_PRIMITIVE_DOUBLE);
        INTERNALNAME_TO_OBJECTPRIMITIVETYPE.put(ObjectType.TYPE_PRIMITIVE_FLOAT.getInternalName(),   ObjectType.TYPE_PRIMITIVE_FLOAT);
        INTERNALNAME_TO_OBJECTPRIMITIVETYPE.put(ObjectType.TYPE_PRIMITIVE_INT.getInternalName(),     ObjectType.TYPE_PRIMITIVE_INT);
        INTERNALNAME_TO_OBJECTPRIMITIVETYPE.put(ObjectType.TYPE_PRIMITIVE_LONG.getInternalName(),    ObjectType.TYPE_PRIMITIVE_LONG);
        INTERNALNAME_TO_OBJECTPRIMITIVETYPE.put(ObjectType.TYPE_PRIMITIVE_SHORT.getInternalName(),   ObjectType.TYPE_PRIMITIVE_SHORT);
        INTERNALNAME_TO_OBJECTPRIMITIVETYPE.put(ObjectType.TYPE_PRIMITIVE_VOID.getInternalName(),    ObjectType.TYPE_PRIMITIVE_VOID);
    }

    protected HashMap<String, ObjectType> descriptorToObjectType = new HashMap<>(1024);
    protected HashMap<String, ObjectType> internalTypeNameToObjectType = new HashMap<>(1024);
    protected HashMap<String, String[]> hierarchy = new HashMap<>(1024);
    protected Loader loader;

    public ObjectType makeFromDescriptor(String descriptor) {
        ObjectType ot = descriptorToObjectType.get(descriptor);

        if (ot == null) {
            if (descriptor.charAt(0) == '[') {
                int dimension = 1;

                while (descriptor.charAt(dimension) == '[') {
                    dimension++;
                }

                ot = (ObjectType)makeFromDescriptorWithoutBracket(descriptor.substring(dimension)).createType(dimension);
            } else {
                ot = makeFromDescriptorWithoutBracket(descriptor);
            }

            descriptorToObjectType.put(descriptor, ot);
        }

        return ot;
    }

    private ObjectType makeFromDescriptorWithoutBracket(String descriptor) {
        ObjectType ot = INTERNALNAME_TO_OBJECTPRIMITIVETYPE.get(descriptor);

        if (ot == null) {
            ot = makeFromInternalTypeName(descriptor.substring(1, descriptor.length()-1));
        }

        return ot;
    }

    public ObjectType makeFromInternalTypeName(String internalTypeName) {
        assert (internalTypeName != null) && !internalTypeName.endsWith(";") : "ObjectTypeMaker.makeFromInternalTypeName(internalTypeName) : invalid internalTypeName";

        ObjectType ot = INTERNALNAME_TO_OBJECTTYPE.get(internalTypeName);

        if (ot == null) {
            ot = internalTypeNameToObjectType.get(internalTypeName);
        }

        if (ot == null) {
            // Search class file with loader, first
            ot = loadFromLoader(internalTypeName);

            if (ot == null) {
                // File not found with the loader -> Try to load class with system class loader
                ot = loadFromClassLoader(internalTypeName);
            }

            if (ot == null) {
                // File not found with the system class loader -> Create type just from 'internalTypeName'
                ot = create(internalTypeName);
            }
        }

        return ot;
    }

    public ObjectType makeFromDescriptorOrInternalTypeName(String descriptorOrInternalTypeName) {
        return (descriptorOrInternalTypeName.charAt(0) == '[') ? makeFromDescriptor(descriptorOrInternalTypeName) : makeFromInternalTypeName(descriptorOrInternalTypeName);
    }

    private ObjectType loadFromLoader(String internalTypeName) {
        try {
            ObjectType ot = internalTypeNameToObjectType.get(internalTypeName);

            if ((ot == null) && loader.canLoad(internalTypeName)) {
                String outerTypeName = getOuterTypeName(internalTypeName);

                if (outerTypeName == null) {
                    int lastSlash = internalTypeName.lastIndexOf('/');
                    String qualifiedName = internalTypeName.replace('/', '.');
                    String name = qualifiedName.substring(lastSlash + 1);

                    ot = new ObjectType(internalTypeName, qualifiedName, name);
                } else {
                    ObjectType outerOT = loadFromLoader(outerTypeName);
                    int index;

                    assert outerOT != null;

                    if (internalTypeName.length() > outerTypeName.length() + 1) {
                        index = outerTypeName.length();
                    } else {
                        index = internalTypeName.lastIndexOf('$');
                    }

                    String innerName = internalTypeName.substring(index + 1);

                    if (Character.isDigit(innerName.charAt(0))) {
                        ot = new InnerObjectType(internalTypeName, null, extractLocalClassName(innerName), outerOT);
                    } else {
                        String qualifiedName = outerOT.getQualifiedName() + '.' + innerName;
                        ot = new InnerObjectType(internalTypeName, qualifiedName, innerName, outerOT);
                    }
                }

                internalTypeNameToObjectType.put(internalTypeName, ot);
            }

            return ot;
        } catch (Exception ignore) {
            // Class file not found by the system class loader, or invalid class file
            return null;
        }
    }

    private ObjectType loadFromClassLoader(String internalTypeName) {
        ObjectType ot = internalTypeNameToObjectType.get(internalTypeName);

        if (ot == null) {
            try {
                Class clazz = getClass().getClassLoader().loadClass(internalTypeName.replace('/', '.'));
                String qualifiedName = clazz.getCanonicalName();
                String name = clazz.getSimpleName();

                if (clazz.isMemberClass()) {
                    String outerInternalTypeName;

                    if (name.isEmpty()) {
                        // Anonymous type
                        outerInternalTypeName = internalTypeName.substring(0, internalTypeName.lastIndexOf('$'));
                    } else {
                        // Inner type
                        outerInternalTypeName = internalTypeName.substring(0, internalTypeName.length()-name.length()-1);
                    }

                    ObjectType outerSot = loadFromClassLoader(outerInternalTypeName);

                    ot = new InnerObjectType(internalTypeName, qualifiedName, name, outerSot);
                } else {
                    ot = new ObjectType(internalTypeName, qualifiedName, name);
                }
            } catch (ClassNotFoundException ignore) {
            }

            internalTypeNameToObjectType.put(internalTypeName, ot);
        }

        return ot;
    }

    private ObjectType create(String internalTypeName) {
        int lastSlash = internalTypeName.lastIndexOf('/');
        int lastDollar = internalTypeName.lastIndexOf('$');
        ObjectType ot;

        if (lastSlash < lastDollar) {
            String outerTypeName = internalTypeName.substring(0, lastDollar);
            ObjectType outerSot = create(outerTypeName);
            String innerName = internalTypeName.substring(outerTypeName.length() + 1);

            if (innerName.isEmpty()) {
                String qualifiedName = internalTypeName.replace('/', '.');
                String name = qualifiedName.substring(lastSlash + 1);
                ot = new ObjectType(internalTypeName, qualifiedName, name);
            } else if (Character.isDigit(innerName.charAt(0))) {
                ot = new InnerObjectType(internalTypeName, null, extractLocalClassName(innerName), outerSot);
            } else {
                String qualifiedName = outerSot.getQualifiedName() + '.' + innerName;
                ot = new InnerObjectType(internalTypeName, qualifiedName, innerName, outerSot);
            }
        } else {
            String qualifiedName = internalTypeName.replace('/', '.');
            String name = qualifiedName.substring(lastSlash + 1);
            ot = new ObjectType(internalTypeName, qualifiedName, name);
        }

        internalTypeNameToObjectType.put(internalTypeName, ot);

        return ot;
    }

    public boolean isAssignable(ObjectType parent, ObjectType child) {
        if (parent == TYPE_UNDEFINED_OBJECT) {
            return true;
        } else if (parent.getDimension() > 0) {
            return (parent.getDimension() == child.getDimension()) && parent.getInternalName().equals(child.getInternalName());
        } else {
            String parentInternalName = parent.getInternalName();
            String childInternalName = child.getInternalName();

            if (parentInternalName.equals(childInternalName) || parentInternalName.equals("java/lang/Object"))
                return true;

            return recursiveIsAssignable(parentInternalName, childInternalName);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean recursiveIsAssignable(String parentInternalName, String childInternalName) {
        if (childInternalName.equals("java/lang/Object"))
            return false;

        String[] superClassAndInterfaceNames = hierarchy.get(childInternalName);

        if (superClassAndInterfaceNames == null) {
            try {
                if (loader.canLoad(childInternalName)) {
                    loadFromLoader(childInternalName);
                    superClassAndInterfaceNames = hierarchy.get(childInternalName);
                } else {
                    Class childClazz = getClass().getClassLoader().loadClass(childInternalName.replace('/', '.'));
                    Class parentClazz = getClass().getClassLoader().loadClass(parentInternalName.replace('/', '.'));
                    return parentClazz.isAssignableFrom(childClazz);
                }
            } catch (Exception ignore) {
                return false;
            }
        }

        if (superClassAndInterfaceNames != null) {
            for (String name : superClassAndInterfaceNames) {
                if (parentInternalName.equals(name))
                    return true;
            }

            for (String name : superClassAndInterfaceNames) {
                if (recursiveIsAssignable(parentInternalName, name))
                    return true;
            }
        }

        return false;
    }

    private String getOuterTypeName(String internalTypeName) throws Exception {
        return loadTypeAndApplyFunction(internalTypeName, null, searchOuterTypeName);
    }

    private Object[] constants = new Object[1000];

    @FunctionalInterface
    private interface Function {
        String apply(ClassFileReader reader, String typeName, String innerTypeName) throws Exception;
    }

    private Function checkInnerTypeName = (reader, typeName, innerTypeName) -> {
        int count = reader.readUnsignedShort();

        for(int i=0; i < count; i++) {
            int innerTypeIndex = reader.readUnsignedShort();

            // Skip 'outerTypeIndex', 'innerNameIndex' & innerAccessFlags'
            reader.skip(3 * 2);

            ConstantClass cc = (ConstantClass)constants[innerTypeIndex];
            String innerInternalTypeName = (String)constants[cc.getNameIndex()];

            if (innerTypeName.equals(innerInternalTypeName)) {
                return typeName;
            }
        }

        return null;
    };

    private Function searchOuterTypeName = (reader, typeName, innerTypeName) -> {
        int count = reader.readUnsignedShort();

        for(int i=0; i < count; i++) {
            int innerTypeIndex = reader.readUnsignedShort();
            int outerTypeIndex = reader.readUnsignedShort();

            // Skip 'innerNameIndex' & innerAccessFlags'
            reader.skip(2 * 2);

            ConstantClass cc = (ConstantClass)constants[innerTypeIndex];
            innerTypeName = (String)constants[cc.getNameIndex()];

            if (innerTypeName.equals(typeName)) {
                if (outerTypeIndex == 0) {
                    // Synthetic inner class -> Search outer class
                    int lastDollar = typeName.lastIndexOf('$');

                    if (lastDollar != -1) {
                        String outerTypeName = typeName.substring(0, lastDollar);
                        return loadTypeAndApplyFunction(outerTypeName, typeName, checkInnerTypeName);
                    }

                    return null;
                } else {
                    // Return 'outerTypeName'
                    cc = (ConstantClass)constants[outerTypeIndex];
                    return (String)constants[cc.getNameIndex()];
                }
            }
        }

        return null;
    };

    private String loadTypeAndApplyFunction(String typeName, String innerTypeName, Function function) throws Exception {
        byte[] data = loader.load(typeName);

        if (data == null) {
            return null;
        } else {
            ClassFileReader reader = new ClassFileReader(data);

            int magic = reader.readInt();

            if (magic != ClassFileReader.JAVA_MAGIC_NUMBER)
                throw new ClassFileFormatException("Invalid CLASS file");

            // Skip 'minorVersion', 'majorVersion'
            reader.skip(2 * 2);

            Object[] constants = loadConstants(reader);

            // Skip 'accessFlags' & 'thisClassIndex'
            reader.skip(2 * 2);

            // Load super class name
            int superClassIndex = reader.readUnsignedShort();
            String superClassName;

            if (superClassIndex == 0) {
                superClassName = null;
            } else {
                ConstantClass cc = (ConstantClass)constants[superClassIndex];
                superClassName = (String)constants[cc.getNameIndex()];
            }

            // Load interface blackListNames
            int count = reader.readUnsignedShort();
            String[] superClassAndInterfaceNames = new String[count + 1];

            superClassAndInterfaceNames[0] = superClassName;

            for (int i = 1; i <= count; i++) {
                int interfaceIndex = reader.readUnsignedShort();
                ConstantClass cc = (ConstantClass)constants[interfaceIndex];
                superClassAndInterfaceNames[i] = (String)constants[cc.getNameIndex()];
            }

            hierarchy.put(typeName, superClassAndInterfaceNames);

            // Skip fields
            count = reader.readUnsignedShort();
            for (int i = 0; i < count; i++) {
                // skip 'accessFlags', 'nameIndex', 'signatureIndex'
                reader.skip(3 * 2);
                skipAttributes(reader);
            }

            // Skip methods
            count = reader.readUnsignedShort();
            for (int i = 0; i < count; i++) {
                // skip 'accessFlags', 'nameIndex', 'signatureIndex'
                reader.skip(3 * 2);
                skipAttributes(reader);
            }

            // Load attributes
            count = reader.readUnsignedShort();
            for (int i = 0; i < count; i++) {
                int attributeNameIndex = reader.readUnsignedShort();
                int attributeLength = reader.readInt();

                String name = (String)constants[attributeNameIndex];

                if ("InnerClasses".equals(name)) {
                    return function.apply(reader, typeName, innerTypeName);
                } else {
                    reader.skip(attributeLength);
                }
            }

            return null;
        }
    }

    private Object[] loadConstants(ClassFileReader reader) throws Exception {
        int count = reader.readUnsignedShort();

        if (count == 0)
            return null;

        if (constants.length < count) {
            constants = new Object[count];
        }

        for (int i=1; i<count; i++) {
            int tag = reader.readByte();

            switch (tag) {
                case 1:
                    constants[i] = reader.readUTF8();
                    break;
                case 7:
                    constants[i] = new ConstantClass(reader.readUnsignedShort());
                    break;
                case 8: case 16: case 19: case 20:
                    reader.skip(2);
                    break;
                case 15:
                    reader.skip(3);
                    break;
                case 3: case 4: case 9: case 10: case 11: case 12: case 17: case 18:
                    reader.skip(4);
                    break;
                case 5: case 6:
                    reader.skip(8);
                    i++;
                    break;
                default:
                    throw new ClassFileFormatException("Invalid constant pool entry");
            }
        }

        return constants;
    }

    private static void skipAttributes(ClassFileReader reader) {
        int count = reader.readUnsignedShort();

        for (int i = 0; i < count; i++) {
            // skip 'attributeNameIndex'
            reader.skip(2);

            int attributeLength = reader.readInt();

            reader.skip(attributeLength);
        }
    }
}
