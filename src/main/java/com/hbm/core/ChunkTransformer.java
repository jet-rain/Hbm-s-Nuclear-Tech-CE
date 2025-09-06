package com.hbm.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import static com.hbm.core.HbmCorePlugin.coreLogger;
import static com.hbm.core.HbmCorePlugin.fail;
import static org.objectweb.asm.Opcodes.*;

public class ChunkTransformer implements IClassTransformer {

    private static final ObfSafeName getBlockState = new ObfSafeName("getBlockState", "func_186032_a");
    private static final ObfSafeName setBlockState = new ObfSafeName("setBlockState", "func_177436_a");

    private static InsnList createGetHook() {
        InsnList insn = new InsnList();
        LabelNode Lskip = new LabelNode();
        LabelNode Lcontinue = new LabelNode();
        insn.add(new FieldInsnNode(GETSTATIC, "com/hbm/events/ChunkEventRegistry", "hasActiveListener", "Z"));
        insn.add(new JumpInsnNode(IFEQ, Lskip));
        insn.add(new VarInsnNode(ALOAD, 0));
        insn.add(new VarInsnNode(ILOAD, 1));
        insn.add(new VarInsnNode(ILOAD, 2));
        insn.add(new VarInsnNode(ILOAD, 3));
        insn.add(new MethodInsnNode(INVOKESTATIC, "com/hbm/core/ChunkHook", "onGetBlockState",
                "(Lnet/minecraft/world/chunk/Chunk;III)Lnet/minecraft/block/state/IBlockState;", false));
        insn.add(new InsnNode(DUP));
        insn.add(new JumpInsnNode(IFNULL, Lcontinue));
        insn.add(new InsnNode(ARETURN));

        insn.add(Lcontinue);
        insn.add(new InsnNode(POP));
        insn.add(Lskip);
        return insn;
    }

    private static InsnList createSetHook() {
        InsnList insn = new InsnList();
        LabelNode Lskip = new LabelNode();
        LabelNode Lcontinue = new LabelNode();
        LabelNode LreturnNull = new LabelNode();
        insn.add(new FieldInsnNode(GETSTATIC, "com/hbm/events/ChunkEventRegistry", "hasActiveListener", "Z"));
        insn.add(new JumpInsnNode(IFEQ, Lskip));
        insn.add(new VarInsnNode(ALOAD, 0));
        insn.add(new VarInsnNode(ALOAD, 1));
        insn.add(new VarInsnNode(ALOAD, 2));
        insn.add(new MethodInsnNode(INVOKESTATIC, "com/hbm/core/ChunkHook", "onSetBlockState",
                "(Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)" +
                "Lnet/minecraft/block/state/IBlockState;", false));
        insn.add(new InsnNode(DUP));
        insn.add(new JumpInsnNode(IFNULL, Lcontinue));
        insn.add(new InsnNode(DUP));
        insn.add(new FieldInsnNode(GETSTATIC, "com/hbm/events/interfaces/IChunkEventListener", "RETURN_NULL",
                "Lnet/minecraft/block/state/IBlockState;"));
        insn.add(new JumpInsnNode(IF_ACMPEQ, LreturnNull));
        insn.add(new InsnNode(ARETURN));

        insn.add(LreturnNull);
        insn.add(new InsnNode(POP));
        insn.add(new InsnNode(ACONST_NULL));
        insn.add(new InsnNode(ARETURN));

        insn.add(Lcontinue);
        insn.add(new InsnNode(POP));
        insn.add(Lskip);
        return insn;
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!"net.minecraft.world.chunk.Chunk".equals(transformedName)) return basicClass;

        coreLogger.info("Patching class {} / {}", transformedName, name);
        try {
            ClassNode cn = new ClassNode();
            new ClassReader(basicClass).accept(cn, 0);
            int patched = 0;
            for (MethodNode mn : cn.methods) {
                if (getBlockState.matches(mn.name) && "(III)Lnet/minecraft/block/state/IBlockState;".equals(mn.desc)) {
                    coreLogger.info("Patching method {}{}", mn.name, mn.desc);
                    mn.instructions.insert(createGetHook());
                    patched++;
                } else if (setBlockState.matches(mn.name) &&
                           "(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)Lnet/minecraft/block/state/IBlockState;".equals(
                                   mn.desc)) {
                    coreLogger.info("Patching method {}{}", mn.name, mn.desc);
                    mn.instructions.insert(createSetHook());
                    patched++;
                }
            }
            if (patched < 2) throw new IllegalStateException("Patched " + patched + " out of 2 methods");
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            cn.accept(cw);
            return cw.toByteArray();
        } catch (Throwable t) {
            fail("net.minecraft.world.chunk.Chunk", t);
            return basicClass;
        }
    }
}
