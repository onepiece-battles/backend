package com.yichi.infra.support.Utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class JavaTypeUtils {
  private JavaTypeUtils() {}

  public static final Class<?> getReifiedGenericType(
      Class<?> reifiedClass, Class<?> baseClass, int typeIndex) {
    JavaTypeUtils.TypeTraver traver = new JavaTypeUtils.TypeTraver(baseClass, typeIndex);
    traverseGraphAndFindReifiedType(
        new JavaTypeUtils.TypeNode(
            (JavaTypeUtils.TypeNode) null, reifiedClass, reifiedClass.getTypeParameters()),
        traver);
    return traver.matchedType;
  }

  private static Class<?> getReifiedTypeFromTypeChain(JavaTypeUtils.TypeNode node, int typeIndex) {
    Type t = node.superGenericTypes[typeIndex];
    if (t instanceof Class) {
      return (Class) t;
    } else {
      while (node.previousNode != null) {
        int index = -1;

        for (int i = 0; i < node.previousNode.selfGenericTypes.length; ++i) {
          if (t.getTypeName().equals(node.previousNode.selfGenericTypes[i].getTypeName())) {
            index = i;
            break;
          }
        }

        if (index == -1) {
          throw new RuntimeException("Can not calculate java generic type");
        }

        t = node.previousNode.superGenericTypes[index];
        if (t instanceof Class) {
          return (Class) t;
        }

        node = node.previousNode;
      }

      return null;
    }
  }

  private static void traverseGraphAndFindReifiedType(
      JavaTypeUtils.TypeNode node, JavaTypeUtils.TypeTraver traver) {
    if (node.nodeClass == traver.targetClass) {
      traver.matchedType = getReifiedTypeFromTypeChain(node, traver.targetTypeIndex);
      if (traver.isFinished()) {
        return;
      }
    }

    Type[] var2 = node.nodeClass.getGenericInterfaces();
    int var3 = var2.length;

    for (int var4 = 0; var4 < var3; ++var4) {
      Type type = var2[var4];
      traverseGraphAndFindReifiedType(type, node, traver);
      if (traver.isFinished()) {
        return;
      }
    }

    Type type = node.nodeClass.getGenericSuperclass();
    if (type != null) {
      traverseGraphAndFindReifiedType(type, node, traver);
      if (traver.isFinished()) {
        return;
      }
    }
  }

  private static void traverseGraphAndFindReifiedType(
      Type parent, JavaTypeUtils.TypeNode node, JavaTypeUtils.TypeTraver traver) {
    if (parent instanceof Class) {
      traverseGraphAndFindReifiedType(new JavaTypeUtils.TypeNode(node, (Class) parent), traver);
      if (traver.isFinished()) {
        return;
      }
    } else if (parent instanceof ParameterizedType) {
      try {
        ParameterizedType pt = (ParameterizedType) parent;
        Class<?> genericClass = Class.forName(pt.getRawType().getTypeName());
        Type[] types = pt.getActualTypeArguments();
        traverseGraphAndFindReifiedType(
            new JavaTypeUtils.TypeNode(node, genericClass, types), traver);
        if (traver.isFinished()) {
          return;
        }
      } catch (ClassNotFoundException var6) {
        throw new RuntimeException(var6);
      }
    }
  }

  private static class TypeNode {
    final JavaTypeUtils.TypeNode previousNode;
    final Class<?> nodeClass;
    final Type[] selfGenericTypes;
    final Type[] superGenericTypes;

    TypeNode(JavaTypeUtils.TypeNode previousNode, Class<?> nodeClass, Type[] superGenericTypes) {
      this.previousNode = previousNode;
      this.nodeClass = nodeClass;
      this.superGenericTypes = superGenericTypes;
      this.selfGenericTypes = nodeClass.getTypeParameters();
    }

    TypeNode(JavaTypeUtils.TypeNode previousNode, Class<?> nodeClass) {
      this(previousNode, nodeClass, new Type[0]);
    }
  }

  private static class TypeTraver {
    Class<?> matchedType;
    final Class<?> targetClass;
    final int targetTypeIndex;

    TypeTraver(Class<?> targetClass, int targetTypeIndex) {
      this.targetClass = targetClass;
      this.targetTypeIndex = targetTypeIndex;
    }

    boolean isFinished() {
      return this.matchedType != null;
    }
  }
}
