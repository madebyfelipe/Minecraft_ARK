package dev.madebyfelipe.iceagesurvival.client.titan;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.titan.TitanGeometry;
import dev.madebyfelipe.iceagesurvival.core.titan.TitanTexture;
import java.io.BufferedReader;
import java.io.InputStream;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.loading.json.FormatVersion;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;
import software.bernie.geckolib.util.JsonUtil;

/**
 * O modelo e a pele do Titanovenator, montados no cliente a partir do Tyrannosaurus do Fossils and Archeology:
 * Revival instalado ({@link TitanGeometry}, {@link TitanTexture}). Nada do Revival é copiado ou salvo: a derivação
 * acontece em memória, na primeira vez que o boss é desenhado depois de cada recarga de recursos.
 *
 * <p>Se a derivação falhar (outra versão do Revival, arquivo faltando), registra o erro e desenha o Rex original no
 * lugar, em vez de derrubar o jogo.
 */
public final class TitanovenatorAssets implements ResourceManagerReloadListener {
    public static final TitanovenatorAssets INSTANCE = new TitanovenatorAssets();
    /** O endereço virtual do modelo: o GeoModel do boss o intercepta, nenhum arquivo existe nele. */
    public static final ResourceLocation MODEL_ID = IceAgeSurvival.id("geo/entity/titanovenator.geo.json");
    private static final ResourceLocation REX_GEO =
            ResourceLocation.fromNamespaceAndPath("fossil", "geo/entity/tyrannosaurus.geo.json");
    private static final ResourceLocation REX_TEXTURE = ResourceLocation.fromNamespaceAndPath("fossil",
            "textures/entity/tyrannosaurus/tyrannosaurus_male.png");
    private static final ResourceLocation TEXTURE_ID = IceAgeSurvival.id("dynamic/titanovenator");

    @Nullable
    private BakedGeoModel model;
    @Nullable
    private DynamicTexture texture;
    private boolean modelFailed;
    private boolean textureFailed;

    private TitanovenatorAssets() {
    }

    /** Recarga de recursos: descarta o derivado; a próxima chamada o refaz com os recursos novos. */
    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        model = null;
        modelFailed = false;
        textureFailed = false;
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(TEXTURE_ID);
            texture = null;
        }
    }

    /** O modelo do boss; o do Rex, se a derivação não deu certo. */
    public BakedGeoModel model() {
        if (model == null && !modelFailed) {
            try {
                model = derivedModel(Minecraft.getInstance().getResourceManager());
            } catch (Exception | LinkageError e) {
                modelFailed = true;
                IceAgeSurvival.LOGGER.error("Titanovenator: não consegui derivar o modelo do Rex do Revival; "
                        + "usando o Rex original", e);
            }
        }
        return model != null ? model : GeckoLibCache.getBakedModels().get(REX_GEO);
    }

    /** A pele do boss; a do Rex, se a derivação não deu certo. */
    public ResourceLocation texture() {
        if (texture == null && !textureFailed) {
            try {
                texture = derivedTexture(Minecraft.getInstance().getResourceManager());
                Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, texture);
            } catch (Exception | LinkageError e) {
                texture = null;
                textureFailed = true;
                IceAgeSurvival.LOGGER.error("Titanovenator: não consegui derivar a pele do Rex do Revival; "
                        + "usando a do Rex", e);
            }
        }
        return texture != null ? TEXTURE_ID : REX_TEXTURE;
    }

    private static BakedGeoModel derivedModel(ResourceManager manager) throws Exception {
        JsonObject rex;
        try (BufferedReader reader = manager.getResourceOrThrow(REX_GEO).openAsReader()) {
            rex = JsonParser.parseReader(reader).getAsJsonObject();
        }
        Model parsed = JsonUtil.GEO_GSON.fromJson(TitanGeometry.derive(rex), Model.class);
        if (parsed.formatVersion() != FormatVersion.V_1_12_0) {
            throw new IllegalStateException("formato de geo inesperado: " + parsed.formatVersion());
        }
        return BakedModelFactory.getForNamespace(IceAgeSurvival.MODID).constructGeoModel(GeometryTree.fromModel(parsed));
    }

    private static DynamicTexture derivedTexture(ResourceManager manager) throws Exception {
        try (InputStream stream = manager.getResourceOrThrow(REX_TEXTURE).open();
             NativeImage image = NativeImage.read(stream)) {
            if (image.getWidth() != TitanTexture.WIDTH || image.getHeight() != TitanTexture.HEIGHT) {
                throw new IllegalStateException("textura do Rex com tamanho inesperado: " + image.getWidth() + "x"
                        + image.getHeight());
            }
            int[] pixels = new int[TitanTexture.WIDTH * TitanTexture.HEIGHT];
            for (int y = 0; y < TitanTexture.HEIGHT; y++) {
                for (int x = 0; x < TitanTexture.WIDTH; x++) {
                    pixels[y * TitanTexture.WIDTH + x] = abgrToArgb(image.getPixelRGBA(x, y));
                }
            }
            int[] derived = TitanTexture.derive(pixels);
            NativeImage result = new NativeImage(TitanTexture.WIDTH, TitanTexture.HEIGHT, false);
            for (int y = 0; y < TitanTexture.HEIGHT; y++) {
                for (int x = 0; x < TitanTexture.WIDTH; x++) {
                    result.setPixelRGBA(x, y, abgrToArgb(derived[y * TitanTexture.WIDTH + x]));
                }
            }
            return new DynamicTexture(result);
        }
    }

    /** O NativeImage guarda ABGR; a troca de R e B é a mesma nos dois sentidos. */
    static int abgrToArgb(int value) {
        return (value & 0xFF00FF00) | (value >> 16 & 0xFF) | (value & 0xFF) << 16;
    }
}
