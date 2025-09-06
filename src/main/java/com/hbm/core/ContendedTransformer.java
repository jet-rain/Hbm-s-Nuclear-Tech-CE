package com.hbm.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public final class ContendedTransformer implements IClassTransformer, Opcodes {
    private static final String ANNO_DESC = "Lcom/hbm/interfaces/Contended;";
    private static final String PAD_PREFIX = "__pad$";
    private static final String CL_PRE = "__cl$pre$";
    private static final String CL_POST = "__cl$post$";
    private static final boolean ENABLED = getBoolProp("hbm.contended.enabled", true);
    private static final int PAD_LONGS = getIntProp("hbm.contended.padLongs", 8);
    private static final int PRE_LINES = getIntProp("hbm.contended.prePadLines", 1);
    private static final int POST_LINES = getIntProp("hbm.contended.postPadLines", 1);
    private static final boolean APPLY_STATIC = getBoolProp("hbm.contended.applyToStatic", false);
    private static final boolean DEBUG = getBoolProp("hbm.contended.debug", true);

    private static final String DEFAULT_GROUP = "";
    private static final int ACC_MODULE = 0x8000;
    private static final int ACC_RECORD = 0x10000;

    @Nullable
    private static String getContendedGroup(List<AnnotationNode> anns) {
        if (anns == null) return null;
        for (AnnotationNode an : anns) {
            if (!ANNO_DESC.equals(an.desc)) continue;
            if (an.values == null || an.values.isEmpty()) return DEFAULT_GROUP;
            for (int i = 0; i < an.values.size(); i += 2) {
                Object k = an.values.get(i);
                Object v = an.values.get(i + 1);
                if ("value".equals(k) && v instanceof String) {
                    return (String) v;
                }
            }
            return DEFAULT_GROUP;
        }
        return null;
    }

    private static boolean alreadyPadded(List<FieldNode> fields) {
        for (FieldNode f : fields) {
            String n = f.name;
            if (n == null) continue;
            if (n.startsWith(PAD_PREFIX) || n.startsWith(CL_PRE) || n.startsWith(CL_POST)) return true;
        }
        return false;
    }

    private static void addPadFields(List<FieldNode> out, List<FieldNode> originals, boolean isStatic, int count, String prefix) {
        if (count <= 0) return;
        final int access = ACC_PRIVATE | ACC_TRANSIENT | ACC_SYNTHETIC | (isStatic ? ACC_STATIC : 0);
        for (int i = 0; i < count; i++) {
            String name = uniquePadName(out, originals, prefix + i);
            out.add(new FieldNode(access, name, "J", null, null));
        }
    }

    private static String uniquePadName(List<FieldNode> existing, List<FieldNode> originals, String base) {
        String name = base;
        int n = 0;
        outer:
        while (true) {
            for (FieldNode f : existing) {
                if (name.equals(f.name)) {
                    name = base + "_" + (++n);
                    continue outer;
                }
            }
            for (FieldNode f : originals) {
                if (name.equals(f.name)) {
                    name = base + "_" + (++n);
                    continue outer;
                }
            }
            return name;
        }
    }

    private static String groupKeySafe(String key) {
        StringBuilder sb = new StringBuilder(key.length());
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            sb.append(Character.isLetterOrDigit(c) || c == '$' || c == '_' ? c : '_');
        }
        return sb.toString();
    }

    private static boolean getBoolProp(String key, boolean dflt) {
        try {
            String v = System.getProperty(key);
            return v == null ? dflt : Boolean.parseBoolean(v);
        } catch (Throwable t) {
            return dflt;
        }
    }

    private static int getIntProp(String key, int dflt) {
        try {
            String v = System.getProperty(key);
            return v == null ? dflt : Integer.parseInt(v);
        } catch (Throwable t) {
            return dflt;
        }
    }

    private static <T> T firstNonNull(T a, T b) {
        return a != null ? a : b;
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!ENABLED || basicClass == null) return basicClass;
        if ("com.hbm.interfaces.Contended".equals(transformedName)) return basicClass;

        try {
            ClassNode cn = new ClassNode();
            new ClassReader(basicClass).accept(cn, 0);
            if ((cn.access & (ACC_INTERFACE | ACC_ANNOTATION | ACC_ENUM | ACC_MODULE | ACC_RECORD)) != 0) {
                return basicClass;
            }
            if (alreadyPadded(cn.fields)) return basicClass;
            final String classGroup = firstNonNull(getContendedGroup(cn.visibleAnnotations), getContendedGroup(cn.invisibleAnnotations));
            final boolean classContended = (classGroup != null);

            final List<FieldNode> orig = cn.fields;
            final int n = orig.size();
            final FI[] fis = new FI[n];

            for (int i = 0; i < n; i++) {
                FieldNode fn = orig.get(i);
                boolean isStatic = (fn.access & ACC_STATIC) != 0;

                String group = getContendedGroup(fn.visibleAnnotations);
                if (group == null) group = getContendedGroup(fn.invisibleAnnotations);

                if (group == null && classContended) {
                    group = classGroup;
                }

                if (group != null && isStatic && !APPLY_STATIC) {
                    group = null;
                }

                fis[i] = new FI(i, fn, isStatic, group);
            }

            LinkedHashMap<String, List<FI>> groups = new LinkedHashMap<>();
            for (FI fi : fis) {
                if (fi.group == null) continue;
                String key = (fi.isStatic ? "S:" : "I:") + fi.group;
                List<FI> list = groups.computeIfAbsent(key, k -> new ArrayList<>());
                list.add(fi);
            }

            if (groups.isEmpty()) {
                return basicClass;
            }

            boolean[] emitted = new boolean[n];
            List<FieldNode> rebuilt = new ArrayList<>(n + groups.size() * 8);

            final int prePad = Math.max(0, PRE_LINES) * Math.max(1, PAD_LONGS);
            final int postPad = Math.max(0, POST_LINES) * Math.max(1, PAD_LONGS);

            int serial = 0;

            for (int i = 0; i < n; i++) {
                if (emitted[i]) continue;
                FI fi = fis[i];

                if (fi.group == null) {
                    rebuilt.add(fi.node);
                    emitted[i] = true;
                    continue;
                }

                String key = (fi.isStatic ? "S:" : "I:") + fi.group;
                List<FI> glist = groups.get(key);
                if (glist == null) {
                    rebuilt.add(fi.node);
                    emitted[i] = true;
                    continue;
                }

                if (prePad > 0) {
                    addPadFields(rebuilt, orig, fi.isStatic, prePad, PAD_PREFIX + groupKeySafe(key) + "$pre$" + (serial++) + "$");
                }

                for (FI gfi : glist) {
                    if (!emitted[gfi.index]) {
                        rebuilt.add(gfi.node);
                        emitted[gfi.index] = true;
                    }
                }

                if (postPad > 0) {
                    addPadFields(rebuilt, orig, fi.isStatic, postPad, PAD_PREFIX + groupKeySafe(key) + "$post$" + (serial++) + "$");
                }
            }

            if (DEBUG) {
                HbmCorePlugin.coreLogger.info("[Contended] {} classGroup={} groups={} pads(pre,post)={},{} applyStatic={}", transformedName,
                        classContended ? ("\"" + classGroup + "\"") : "none", groups.keySet(), prePad, postPad, APPLY_STATIC);
            }

            cn.fields = rebuilt;

            ClassWriter cw = new ClassWriter(0);
            cn.accept(cw);
            return cw.toByteArray();
        } catch (Throwable t) {
            if (DEBUG) HbmCorePlugin.coreLogger.info("[Contended] ERROR {}: {}", transformedName, t);
            return basicClass;
        }
    }

    private static final class FI {
        final int index;
        final FieldNode node;
        final boolean isStatic;
        final String group;
        FI(int index, FieldNode node, boolean isStatic, String group) {
            this.index = index;
            this.node = node;
            this.isStatic = isStatic;
            this.group = group;
        }
    }
}