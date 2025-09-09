package com.hbm.render.model;

import com.hbm.blocks.network.FluidDuctStandard;
import com.hbm.render.amlfrom1710.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.vector.Vector3f;

import java.util.*;

@SideOnly(Side.CLIENT)
public class DuctNeoBakedModel implements IBakedModel {

    private final WavefrontObject model;
    private final TextureAtlasSprite baseSprite;
    private final TextureAtlasSprite overlaySprite;
    private final boolean forBlock;
    private final float baseScale;
    private final float tx, ty, tz;
    private final float itemYaw;
    private final VertexFormat format;

    private final Map<Integer, List<BakedQuad>> cacheByMask = new HashMap<>();
    private List<BakedQuad> itemQuads;

    private DuctNeoBakedModel(WavefrontObject model, TextureAtlasSprite baseSprite, TextureAtlasSprite overlaySprite, boolean forBlock, float baseScale, float tx, float ty, float tz, float itemYaw) {
        this.model = model;
        this.baseSprite = baseSprite;
        this.overlaySprite = overlaySprite;
        this.forBlock = forBlock;
        this.baseScale = baseScale;
        this.tx = tx;
        this.ty = ty;
        this.tz = tz;
        this.itemYaw = itemYaw;
        this.format = forBlock ? DefaultVertexFormats.BLOCK : DefaultVertexFormats.ITEM;
    }

    public static DuctNeoBakedModel forBlock(WavefrontObject model, TextureAtlasSprite baseSprite, TextureAtlasSprite overlaySprite) {
        return new DuctNeoBakedModel(model, baseSprite, overlaySprite, true, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    public static DuctNeoBakedModel forItem(WavefrontObject model, TextureAtlasSprite baseSprite, TextureAtlasSprite overlaySprite, float baseScale, float tx, float ty, float tz, float yaw) {
        return new DuctNeoBakedModel(model, baseSprite, overlaySprite, false, baseScale, tx, ty, tz, yaw);
    }

    public static DuctNeoBakedModel empty(TextureAtlasSprite sprite) {
        return new DuctNeoBakedModel(new WavefrontObject(new ResourceLocation("minecraft:empty")), sprite, sprite, true, 1.0F, 0, 0, 0, 0);
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
        if (side != null) return Collections.emptyList();

        if (forBlock) {
            boolean pX = false, nX = false, pY = false, nY = false, pZ = false, nZ = false;
            if (state != null) {
                try {
                    if (state.getPropertyKeys().contains(FluidDuctStandard.POS_X)) pX = state.getValue(FluidDuctStandard.POS_X);
                    if (state.getPropertyKeys().contains(FluidDuctStandard.NEG_X)) nX = state.getValue(FluidDuctStandard.NEG_X);
                    if (state.getPropertyKeys().contains(FluidDuctStandard.POS_Y)) pY = state.getValue(FluidDuctStandard.POS_Y);
                    if (state.getPropertyKeys().contains(FluidDuctStandard.NEG_Y)) nY = state.getValue(FluidDuctStandard.NEG_Y);
                    if (state.getPropertyKeys().contains(FluidDuctStandard.POS_Z)) pZ = state.getValue(FluidDuctStandard.POS_Z);
                    if (state.getPropertyKeys().contains(FluidDuctStandard.NEG_Z)) nZ = state.getValue(FluidDuctStandard.NEG_Z);
                } catch (Exception ignored) {}
            }

            int mask = (pX ? 32 : 0)
                    | (nX ? 16 : 0)
                    | (pY ? 8 : 0)
                    | (nY ? 4 : 0)
                    | (pZ ? 2 : 0)
                    | (nZ ? 1 : 0);

            List<BakedQuad> quads = cacheByMask.get(mask);
            if (quads != null) return quads;

            quads = buildWorldQuads(pX, nX, pY, nY, pZ, nZ);
            cacheByMask.put(mask, quads);
            return quads;
        } else {
            if (itemQuads == null) {
                itemQuads = buildItemQuads();
            }
            return itemQuads;
        }
    }

    private List<BakedQuad> buildWorldQuads(boolean pX, boolean nX, boolean pY, boolean nY, boolean pZ, boolean nZ) {
        List<String> parts = new ArrayList<>();

        int mask = (pX ? 32 : 0)
                | (nX ? 16 : 0)
                | (pY ? 8 : 0)
                | (nY ? 4 : 0)
                | (pZ ? 2 : 0)
                | (nZ ? 1 : 0);

        if (mask == 0) {
            parts.add("pX");
            parts.add("nX");
            parts.add("pY");
            parts.add("nY");
            parts.add("pZ");
            parts.add("nZ");
        } else if (mask == 0b100000 || mask == 0b010000) {
            parts.add("pX");
            parts.add("nX");
        } else if (mask == 0b001000 || mask == 0b000100) {
            parts.add("pY");
            parts.add("nY");
        } else if (mask == 0b000010 || mask == 0b000001) {
            parts.add("pZ");
            parts.add("nZ");
        } else {
            if (pX) parts.add("pX");
            if (nX) parts.add("nX");
            if (pY) parts.add("pY");
            if (nY) parts.add("nY");
            if (pZ) parts.add("nZ"); // mirrors original (pZ -> nZ)
            if (nZ) parts.add("pZ"); // mirrors original (nZ -> pZ)

            if (!pX && !pY && !pZ) parts.add("ppn");
            if (!pX && !pY && !nZ) parts.add("ppp");
            if (!nX && !pY && !pZ) parts.add("npn");
            if (!nX && !pY && !nZ) parts.add("npp");
            if (!pX && !nY && !pZ) parts.add("pnn");
            if (!pX && !nY && !nZ) parts.add("pnp");
            if (!nX && !nY && !pZ) parts.add("nnn");
            if (!nX && !nY && !nZ) parts.add("nnp");
        }

        return bakeSelectedParts(parts, 0.0F, 0.0F, 0.0F, true, true);
    }

    private List<BakedQuad> buildItemQuads() {
        List<String> parts = Arrays.asList("pX", "nX", "pZ", "nZ");
        return bakeSelectedParts(parts, 0.0F, 0.0F, itemYaw, false, false);
    }

    private List<BakedQuad> bakeSelectedParts(List<String> parts, float roll, float pitch, float yaw, boolean shadow, boolean centerToBlock) {
        List<BakedQuad> quads = new ArrayList<>();

        for (GroupObject go : model.groupObjects) {
            if (!parts.contains(go.name)) continue;

            for (Face f : go.faces) {
                Vertex n = f.faceNormal;

                double[] n1 = rotateX(n.x, n.y, n.z, roll);
                double[] n2 = rotateZ(n1[0], n1[1], n1[2], pitch);
                double[] n3 = rotateY(n2[0], n2[1], n2[2], yaw);

                float fnx = (float) n3[0];
                float fny = (float) n3[1];
                float fnz = (float) n3[2];

                int cr = 255;

                int vCount = f.vertices.length;
                if (vCount < 3) continue;

                int[] idxs = vCount >= 4 ? new int[]{0, 1, 2, 3} : new int[]{0, 1, 2, 2};

                float[] px = new float[4];
                float[] py = new float[4];
                float[] pz = new float[4];
                float[] uu = new float[4];
                float[] vv = new float[4];

                for (int j = 0; j < 4; j++) {
                    int i = idxs[j];
                    Vertex v = f.vertices[i];

                    double[] p1 = rotateX(v.x, v.y, v.z, roll);
                    double[] p2 = rotateZ(p1[0], p1[1], p1[2], pitch);
                    double[] p3 = rotateY(p2[0], p2[1], p2[2], yaw);

                    float x = (float) p3[0];
                    float y = (float) p3[1];
                    float z = (float) p3[2];

                    if (centerToBlock) {
                        x += 0.5F;
                        y += 0.5F;
                        z += 0.5F;
                    }

                    x = x * baseScale + tx;
                    y = y * baseScale + ty;
                    z = z * baseScale + tz;

                    TextureCoordinate t = f.textureCoordinates[i];
                    float u = baseSprite.getInterpolatedU(t.u * 16.0D);
                    float w = baseSprite.getInterpolatedV(t.v * 16.0D);

                    px[j] = x; py[j] = y; pz[j] = z;
                    uu[j] = u; vv[j] = w;
                }

                EnumFacing face = facingFromNormal(fnx, fny, fnz);
                Vector3f normal = new Vector3f(fnx, fny, fnz);
                normal.normalise();

                // Base pass (no tint)
                {
                    UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(format);
                    builder.setQuadOrientation(face);
                    builder.setTexture(baseSprite);
                    builder.setApplyDiffuseLighting(true);
                    for (int j = 0; j < 4; j++) {
                        putVertex(builder, px[j], py[j], pz[j], uu[j], vv[j], cr, cr, cr, normal);
                    }
                    quads.add(builder.build());
                }
                // Overlay pass (tinted via block color handler, tintIndex = 1)
                {
                    UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(format);
                    builder.setQuadOrientation(face);
                    builder.setTexture(overlaySprite);
                    builder.setApplyDiffuseLighting(true);
                    builder.setQuadTint(1);
                    float[] ou = new float[4], ov = new float[4];
                    for (int j = 0; j < 4; j++) {
                        TextureCoordinate t = f.textureCoordinates[idxs[j]];
                        ou[j] = overlaySprite.getInterpolatedU(t.u * 16.0D);
                        ov[j] = overlaySprite.getInterpolatedV(t.v * 16.0D);
                    }
                    for (int j = 0; j < 4; j++) {
                        putVertex(builder, px[j], py[j], pz[j], ou[j], ov[j], cr, cr, cr, normal);
                    }
                    quads.add(builder.build());
                }
            }
        }

        return quads;
    }

    private void putVertex(UnpackedBakedQuad.Builder builder, float x, float y, float z, float u, float v, int cr, int cg, int cb, Vector3f normal) {
        for (int e = 0; e < format.getElementCount(); e++) {
            VertexFormatElement element = format.getElement(e);
            switch (element.getUsage()) {
                case POSITION -> builder.put(e, x, y, z);
                case COLOR -> builder.put(e, cr / 255.0F, cg / 255.0F, cb / 255.0F, 1.0F);
                case UV -> {
                    if (element.getIndex() == 0) {
                        builder.put(e, u, v);
                    } else {
                        builder.put(e, 0.0F, 0.0F);
                    }
                }
                case NORMAL -> builder.put(e, normal.x, normal.y, normal.z);
                case PADDING -> builder.put(e, 0.0F);
                default -> builder.put(e);
            }
        }
    }

    private static int clampColor(int c) {
        if (c < 0) return 0;
        return Math.min(c, 255);
    }

    private static EnumFacing facingFromNormal(float nx, float ny, float nz) {
        return EnumFacing.getFacingFromVector(nx, ny, nz);
    }

    private static double[] rotateX(double x, double y, double z, float angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double ny = y * cos - z * sin;
        double nz = y * sin + z * cos;
        return new double[]{x, ny, nz};
    }

    private static double[] rotateY(double x, double y, double z, float angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double nx = x * cos + z * sin;
        double nz = -x * sin + z * cos;
        return new double[]{nx, y, nz};
    }

    private static double[] rotateZ(double x, double y, double z, float angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double nx = x * cos - y * sin;
        double ny = x * sin + y * cos;
        return new double[]{nx, ny, z};
    }

    @Override
    public boolean isAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return baseSprite;
    }

    private static final ItemCameraTransforms CUSTOM_TRANSFORMS = createCustomTransforms();
    private static ItemCameraTransforms createCustomTransforms() {
        ItemTransformVec3f gui = new ItemTransformVec3f(
                new org.lwjgl.util.vector.Vector3f(30, -45, 0),
                new org.lwjgl.util.vector.Vector3f(0, 0, 0),
                new org.lwjgl.util.vector.Vector3f(0.8f, 0.8f, 0.8f)
        );

        ItemTransformVec3f thirdPerson = new ItemTransformVec3f(
                new org.lwjgl.util.vector.Vector3f(75, 45, 0),
                new org.lwjgl.util.vector.Vector3f(0, 0.25f, 0),
                new org.lwjgl.util.vector.Vector3f(0.5f, 0.5f, 0.5f)
        );

        ItemTransformVec3f firstPerson = new ItemTransformVec3f(
                new org.lwjgl.util.vector.Vector3f(0, 45, 0),
                new org.lwjgl.util.vector.Vector3f(0, 0.25f, 0),
                new org.lwjgl.util.vector.Vector3f(0.5f, 0.5f, 0.5f)
        );

        ItemTransformVec3f ground = new ItemTransformVec3f(
                new org.lwjgl.util.vector.Vector3f(0, 0, 0),
                new org.lwjgl.util.vector.Vector3f(0, 2f / 16, 0),
                new org.lwjgl.util.vector.Vector3f(0.5f, 0.5f, 0.5f)
        );

        ItemTransformVec3f head = new ItemTransformVec3f(
                new org.lwjgl.util.vector.Vector3f(0, 0, 0),
                new org.lwjgl.util.vector.Vector3f(0, 13f / 16, 7f / 16),
                new org.lwjgl.util.vector.Vector3f(1, 1, 1)
        );

        ItemTransformVec3f fixed = new ItemTransformVec3f(
                new org.lwjgl.util.vector.Vector3f(0, 180, 0),
                new org.lwjgl.util.vector.Vector3f(0, 0, 0),
                new org.lwjgl.util.vector.Vector3f(0.75f, 0.75f, 0.75f)
        );

        return new ItemCameraTransforms(thirdPerson, thirdPerson, firstPerson, firstPerson, head, gui, ground, fixed);
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return CUSTOM_TRANSFORMS;
    }

    @Override
    public ItemOverrideList getOverrides() {
        return ItemOverrideList.NONE;
    }
}
