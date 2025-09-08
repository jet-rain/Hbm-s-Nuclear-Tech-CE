package com.hbm.render.model;

import com.hbm.blocks.network.energy.BlockCable;
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

import javax.vecmath.Vector3f;
import java.util.*;

public class BlockCableBakedModel implements IBakedModel {

    private final WavefrontObject model;
    private final TextureAtlasSprite sprite;
    private final boolean forBlock;
    private final float baseScale;
    private final float tx, ty, tz;
    private final float itemYaw;
    private final VertexFormat format;

    private final Map<Integer, List<BakedQuad>> cacheByMask = new HashMap<>();
    private List<BakedQuad> itemQuads;

    private BlockCableBakedModel(WavefrontObject model, TextureAtlasSprite sprite, boolean forBlock, float baseScale, float tx, float ty, float tz, float itemYaw) {
        this.model = model;
        this.sprite = sprite;
        this.forBlock = forBlock;
        this.baseScale = baseScale;
        this.tx = tx;
        this.ty = ty;
        this.tz = tz;
        this.itemYaw = itemYaw;
        this.format = forBlock ? DefaultVertexFormats.BLOCK : DefaultVertexFormats.ITEM;
    }

    public static BlockCableBakedModel forBlock(WavefrontObject model, TextureAtlasSprite sprite) {
        return new BlockCableBakedModel(model, sprite, true, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    public static BlockCableBakedModel forItem(WavefrontObject model, TextureAtlasSprite sprite, float baseScale, float tx, float ty, float tz, float yaw) {
        return new BlockCableBakedModel(model, sprite, false, baseScale, tx, ty, tz, yaw);
    }

    public static BlockCableBakedModel empty(TextureAtlasSprite sprite) {
        return new BlockCableBakedModel(new WavefrontObject(new ResourceLocation("minecraft:empty")), sprite, true, 1.0F, 0, 0, 0, 0);
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
        if (side != null) return Collections.emptyList();

        if (forBlock) {
            boolean pX = false, nX = false, pY = false, nY = false, pZ = false, nZ = false;
            if (state != null) {
                try {
                    if (state.getPropertyKeys().contains(BlockCable.POS_X)) pX = state.getValue(BlockCable.POS_X);
                    if (state.getPropertyKeys().contains(BlockCable.NEG_X)) nX = state.getValue(BlockCable.NEG_X);
                    if (state.getPropertyKeys().contains(BlockCable.POS_Y)) pY = state.getValue(BlockCable.POS_Y);
                    if (state.getPropertyKeys().contains(BlockCable.NEG_Y)) nY = state.getValue(BlockCable.NEG_Y);
                    if (state.getPropertyKeys().contains(BlockCable.POS_Z)) pZ = state.getValue(BlockCable.POS_Z);
                    if (state.getPropertyKeys().contains(BlockCable.NEG_Z)) nZ = state.getValue(BlockCable.NEG_Z);
                } catch (Exception ignored) {}
            }
            int mask = (pX ? 1 : 0)
                    | (nX ? 2 : 0)
                    | (pY ? 4 : 0)
                    | (nY ? 8 : 0)
                    | (pZ ? 16 : 0)
                    | (nZ ? 32 : 0);

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

        if (pX && nX && !pY && !nY && !pZ && !nZ) {
            parts.add("CX");
        } else if (!pX && !nX && pY && nY && !pZ && !nZ) {
            parts.add("CY");
        } else if (!pX && !nX && !pY && !nY && pZ && nZ) {
            parts.add("CZ");
        } else {
            parts.add("Core");
            if (pX) parts.add("posX");
            if (nX) parts.add("negX");
            if (pY) parts.add("posY");
            if (nY) parts.add("negY");
            if (nZ) parts.add("posZ"); // note: mirrors original 1.7.10 code (nZ -> posZ)
            if (pZ) parts.add("negZ"); // mirrors original (pZ -> negZ)
        }

        return bakeSelectedParts(parts, 0.0F, 0.0F, 0.0F, true, true);
    }

    private List<BakedQuad> buildItemQuads() {
        List<String> parts = Arrays.asList("Core", "posX", "negX", "posZ", "negZ");
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

                float brightness = 1.0F;
                if (shadow) {
                    brightness = (fny + 0.7F) * 0.9F - Math.abs(fnx) * 0.1F + Math.abs(fnz) * 0.1F;
                    if (brightness < 0.45F) brightness = 0.45F;
                    if (brightness > 1.0F) brightness = 1.0F;
                }
                int cr = clampColor((int) (brightness * 255.0F));

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
                    float u = sprite.getInterpolatedU(t.u * 16.0D);
                    float w = sprite.getInterpolatedV(t.v * 16.0D);

                    px[j] = x; py[j] = y; pz[j] = z;
                    uu[j] = u; vv[j] = w;
                }

                EnumFacing face = facingFromNormal(fnx, fny, fnz);
                UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(format);
                builder.setQuadOrientation(face);
                builder.setTexture(sprite);
                builder.setApplyDiffuseLighting(true);

                Vector3f normal = new Vector3f(fnx, fny, fnz);
                normal.normalize();

                for (int j = 0; j < 4; j++) {
                    putVertex(builder, px[j], py[j], pz[j], uu[j], vv[j], cr, cr, cr, normal);
                }

                quads.add(builder.build());
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
        return sprite;
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
