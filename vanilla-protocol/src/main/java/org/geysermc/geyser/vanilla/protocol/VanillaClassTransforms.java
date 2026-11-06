/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.MethodModel;
import java.lang.classfile.MethodTransform;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;

import static java.lang.constant.ConstantDescs.CD_Object;
import static java.lang.constant.ConstantDescs.CD_int;

/** Extends native block ID lookups without replacing vanilla packet codecs. */
public final class VanillaClassTransforms {
    private static final ClassDesc HOOK = ClassDesc.of("org.geysermc.geyser.vanilla.protocol.VanillaBlockStates");

    private VanillaClassTransforms() {
    }

    public static byte[] idMapper(byte[] original) {
        var classFile = ClassFile.of();
        var model = classFile.parse(original);
        if (!model.thisClass().asInternalName().equals("net/minecraft/core/IdMapper")) {
            throw new IllegalArgumentException("Expected the pinned client's IdMapper class");
        }
        int[] transformed = {0};
        byte[] result = classFile.transformClass(model, (builder, element) -> {
            if (element instanceof MethodModel method && (matches(method, "byId", "(I)Ljava/lang/Object;")
                || matches(method, "getId", "(Ljava/lang/Object;)I"))) {
                boolean byId = method.methodName().equalsString("byId");
                builder.transformMethod(method, MethodTransform.transformingCode(new CodeTransform() {
                    @Override
                    public void atStart(CodeBuilder code) {
                        var fallback = code.newLabel();
                        code.aload(0);
                        if (byId) {
                            code.iload(1).invokestatic(HOOK, "byId", MethodTypeDesc.of(CD_Object, CD_Object, CD_int))
                                .dup().ifnull(fallback).areturn();
                        } else {
                            code.aload(1).invokestatic(HOOK, "getId", MethodTypeDesc.of(CD_int, CD_Object, CD_Object))
                                .dup().iflt(fallback).ireturn();
                        }
                        code.labelBinding(fallback).pop();
                    }

                    @Override
                    public void accept(CodeBuilder code, CodeElement element) {
                        code.with(element);
                    }
                }));
                transformed[0]++;
            } else {
                builder.with(element);
            }
        });
        if (transformed[0] != 2) {
            throw new IllegalStateException("The pinned client's IdMapper lookup methods changed");
        }
        return result;
    }

    private static boolean matches(MethodModel method, String name, String descriptor) {
        return method.methodName().equalsString(name) && method.methodType().equalsString(descriptor);
    }
}
