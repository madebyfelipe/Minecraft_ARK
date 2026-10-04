package dev.madebyfelipe.iceagesurvival.outpost;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Um posto: um template de {@code structures/} (gerados por {@code tools/gen_military_outpost.py} a partir das builds
 * do Felipe), girado em volta do centro. A camada 0 do template é o chão: o posto é posto com ela na altura do terreno.
 *
 * <ul>
 *   <li>{@link #TOWER}: a torre de vigia num pátio cercado de muro de pedra, com o Portão de Pedra Grande na frente;
 *   no térreo, o Terminal Militar e o baú com o saque, de frente para a porta (lado norte, z = 0, no template);</li>
 *   <li>{@link #COMPLEX}: o complexo de prédios, com baús de saque e o terminal no térreo.</li>
 * </ul>
 */
public class OutpostPiece extends TemplateStructurePiece {
    public static final ResourceLocation TOWER = IceAgeSurvival.id("military_outpost");
    public static final ResourceLocation COMPLEX = IceAgeSurvival.id("military_outpost_complex");
    /** O lado do quadrado da torre (muro a muro). */
    public static final int TOWER_SIZE = 17;
    /** Na torre, o terminal e o vão do portão (fileira de baixo, coluna do meio), em coordenadas do template. */
    static final BlockPos TOWER_TERMINAL = new BlockPos(9, 1, 10);
    static final BlockPos TOWER_GATE = new BlockPos(7, 1, 0);

    public OutpostPiece(StructureTemplateManager templates, ResourceLocation template, BlockPos origin,
            Rotation rotation) {
        super(Outposts.OUTPOST_PIECE.get(), 0, templates, template, template.toString(),
                settings(templates, template, rotation), origin);
    }

    public OutpostPiece(StructureTemplateManager templates, CompoundTag tag) {
        super(Outposts.OUTPOST_PIECE.get(), tag, templates,
                location -> settings(templates, location, Rotation.valueOf(tag.getString("Rot"))));
    }

    private static StructurePlaceSettings settings(StructureTemplateManager templates, ResourceLocation template,
            Rotation rotation) {
        Vec3i size = templates.getOrCreate(template).getSize();
        return new StructurePlaceSettings().setRotation(rotation)
                .setRotationPivot(new BlockPos(size.getX() / 2, 0, size.getZ() / 2))
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Rot", placeSettings.getRotation().name());
    }

    @Override
    protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level, RandomSource random,
            BoundingBox box) {
    }

    /** Onde fica, no mundo, um ponto dado em coordenadas do template. */
    public BlockPos worldPos(BlockPos local) {
        return templatePosition.offset(StructureTemplate.calculateRelativePosition(placeSettings, local));
    }

    public StructurePlaceSettings placeSettings() {
        return placeSettings;
    }
}
