package dev.madebyfelipe.iceagesurvival.outpost;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * O posto: o template {@code structures/military_outpost.nbt} (gerado por {@code tools/gen_military_outpost.py} a
 * partir da torre de vigia do Felipe). A torre fica no meio de um pátio cercado de muro de pedra, com o Portão de Pedra
 * Grande na frente; no térreo, o Terminal Militar e o baú com o saque, de frente para a porta. A camada 0 do template
 * é o chão: o posto é posto com ela na altura do terreno.
 *
 * <p>Em coordenadas do template, a porta da torre e o portão ficam no lado norte (z = 0); a rotação da peça gira tudo.
 */
public class OutpostPiece extends TemplateStructurePiece {
    public static final ResourceLocation TEMPLATE = IceAgeSurvival.id("military_outpost");
    /** O lado do quadrado do template (muro a muro). */
    public static final int SIZE = 17;
    /** O terminal e o vão do portão (fileira de baixo, coluna do meio), em coordenadas do template. */
    static final BlockPos TERMINAL = new BlockPos(9, 1, 10);
    static final BlockPos GATE = new BlockPos(7, 1, 0);

    public OutpostPiece(StructureTemplateManager templates, BlockPos origin, Rotation rotation) {
        super(Outposts.OUTPOST_PIECE.get(), 0, templates, TEMPLATE, TEMPLATE.toString(), settings(rotation), origin);
    }

    public OutpostPiece(StructureTemplateManager templates, CompoundTag tag) {
        super(Outposts.OUTPOST_PIECE.get(), tag, templates,
                location -> settings(Rotation.valueOf(tag.getString("Rot"))));
    }

    private static StructurePlaceSettings settings(Rotation rotation) {
        return new StructurePlaceSettings().setRotation(rotation).setRotationPivot(new BlockPos(SIZE / 2, 0, SIZE / 2))
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
        return templatePosition.offset(net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
                .calculateRelativePosition(placeSettings, local));
    }

    public BlockPos terminalPos() {
        return worldPos(TERMINAL);
    }

    /** A parte de baixo, no meio, do portão grande. */
    public BlockPos gatePos() {
        return worldPos(GATE);
    }
}
