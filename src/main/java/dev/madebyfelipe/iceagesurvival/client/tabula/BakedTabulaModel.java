package dev.madebyfelipe.iceagesurvival.client.tabula;

import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Um modelo Tabula pronto para desenhar: a árvore de {@link ModelPart} montada do modelo base e as animações já em
 * quadros. As partes ficam num vetor na ordem do {@link TabulaRig}, para cada quadro ser aplicado direto. Os
 * filhos do {@code PartDefinition} se chamam {@code c<índice>}: os nomes do Tabula se repetem entre irmãos.
 */
public final class BakedTabulaModel {
    private final JurassicRebornAppearance appearance;
    private final TabulaRig rig;
    private final ModelPart root;
    private final ModelPart[] parts;
    private final Map<String, PoseClip> clips;
    private final int[] headChain;
    private final int[] breathing;

    private BakedTabulaModel(JurassicRebornAppearance appearance, TabulaRig rig, ModelPart root, ModelPart[] parts,
                             Map<String, PoseClip> clips) {
        this.appearance = appearance;
        this.rig = rig;
        this.root = root;
        this.parts = parts;
        this.clips = Map.copyOf(clips);
        this.headChain = indices(rig, appearance.headChain());
        this.breathing = indices(rig, appearance.breathing());
    }

    /** Monta as partes. Roda na thread principal (no {@code apply} do recarregamento). */
    public static BakedTabulaModel bake(JurassicRebornAppearance appearance, TabulaModelData base, TabulaRig rig,
                                        Map<String, PoseClip> clips) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition[] definitions = new PartDefinition[rig.size()];
        for (int i = 0; i < rig.size(); i++) {
            TabulaCube cube = rig.cube(i);
            PartDefinition parent = rig.parent(i) < 0 ? mesh.getRoot() : definitions[rig.parent(i)];
            float[] o = cube.offset();
            float[] d = cube.dimensions();
            float[] p = cube.position();
            float[] r = cube.rotation();
            CubeListBuilder box = CubeListBuilder.create()
                    .texOffs(cube.textureU(), cube.textureV())
                    .mirror(cube.mirror())
                    .addBox(o[0], o[1], o[2], d[0], d[1], d[2], new CubeDeformation(cube.inflate()));
            definitions[i] = parent.addOrReplaceChild(partName(i), box, PartPose.offsetAndRotation(p[0], p[1], p[2],
                    (float) Math.toRadians(r[0]), (float) Math.toRadians(r[1]), (float) Math.toRadians(r[2])));
        }
        ModelPart root = LayerDefinition.create(mesh, base.textureWidth(), base.textureHeight()).bakeRoot();
        ModelPart[] parts = new ModelPart[rig.size()];
        for (int i = 0; i < rig.size(); i++) {
            ModelPart parent = rig.parent(i) < 0 ? root : parts[rig.parent(i)];
            parts[i] = parent.getChild(partName(i));
        }
        return new BakedTabulaModel(appearance, rig, root, parts, clips);
    }

    private static String partName(int index) {
        return "c" + index;
    }

    private static int[] indices(TabulaRig rig, List<String> names) {
        return names.stream().mapToInt(rig::indexOf).filter(i -> i >= 0).toArray();
    }

    public JurassicRebornAppearance appearance() {
        return appearance;
    }

    public TabulaRig rig() {
        return rig;
    }

    public ModelPart root() {
        return root;
    }

    public PoseClip clip(String name) {
        return clips.get(name);
    }

    public boolean has(String name) {
        return clips.containsKey(name);
    }

    /** Uma animação qualquer, para a espécie que não tem nem IDLE (nunca vazio: o carregador exige uma). */
    public PoseClip anyClip() {
        return clips.values().iterator().next();
    }

    /** Põe as partes no quadro. */
    public void apply(float[] frame) {
        for (int i = 0; i < parts.length; i++) {
            int at = i * TabulaRig.STRIDE;
            ModelPart part = parts[i];
            part.x = frame[at];
            part.y = frame[at + 1];
            part.z = frame[at + 2];
            part.xRot = frame[at + 3];
            part.yRot = frame[at + 4];
            part.zRot = frame[at + 5];
        }
    }

    /** Soma o olhar às partes do pescoço e da cabeça, repartido igualmente entre elas (radianos). */
    public void addLook(float yaw, float pitch) {
        if (headChain.length == 0) {
            return;
        }
        float yawShare = yaw / headChain.length;
        float pitchShare = pitch / headChain.length;
        for (int index : headChain) {
            parts[index].yRot += yawShare;
            parts[index].xRot += pitchShare;
        }
    }

    /** Respiração: o tronco oscila em X, cada parte um pouco defasada da anterior. */
    public void addBreathing(float angle, float phase) {
        for (int i = 0; i < breathing.length; i++) {
            parts[breathing[i]].xRot += angle * (float) Math.sin(phase - i * 0.6F);
        }
    }
}
