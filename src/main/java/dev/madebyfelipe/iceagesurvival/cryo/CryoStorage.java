package dev.madebyfelipe.iceagesurvival.cryo;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.network.CryoListPayload;
import dev.madebyfelipe.iceagesurvival.network.ModPayloads;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;

/**
 * Criogenia (MC-19/MC-21): as criaturas que cada jogador congelou pelo menu da tecla O, sem limite. Fica nos dados
 * do Overworld, por jogador, e não se perde ao morrer. Cada entrada é a criatura inteira ({@link
 * PrehistoricCreature#freezeData()}: identidade, genoma, pontos, sela, inventário), com um id próprio pelo qual o menu
 * a pede de volta. Congelada, o tempo não passa para ela. O servidor confere tudo a cada pedido: guardar tira a
 * criatura do mundo, soltar só apaga a entrada se ela entrou no mundo, e um pedido repetido não acha mais o que já
 * mudou.
 */
public final class CryoStorage extends SavedData {
    private static final String NAME = "iceagesurvival_cryo";

    /** Uma criatura congelada: id da entrada, espécie, dados congelados, nível e nome (para a lista do menu). */
    public record Entry(UUID id, ResourceLocation type, CompoundTag data, int level, String name) {
        public Entry {
            data = data.copy();
        }

        /** O UUID da própria criatura, se os dados o têm. */
        @Nullable
        public UUID creature() {
            return data.hasUUID("UUID") ? data.getUUID("UUID") : null;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", id);
            tag.putString("Type", type.toString());
            tag.put("Creature", data.copy());
            tag.putInt("Level", level);
            tag.putString("Name", name);
            return tag;
        }

        @Nullable
        private static Entry load(CompoundTag tag) {
            ResourceLocation type = ResourceLocation.tryParse(tag.getString("Type"));
            if (!tag.hasUUID("Id") || type == null || !tag.contains("Creature", Tag.TAG_COMPOUND)) {
                return null;
            }
            return new Entry(tag.getUUID("Id"), type, tag.getCompound("Creature"), tag.getInt("Level"),
                    tag.getString("Name"));
        }
    }

    private final Map<UUID, List<Entry>> players = new HashMap<>();

    @Nullable
    public static CryoStorage get(@Nullable MinecraftServer server) {
        ServerLevel overworld = server == null ? null : server.overworld();
        return overworld == null ? null
                : overworld.getDataStorage().computeIfAbsent(CryoStorage::load, CryoStorage::new, NAME);
    }

    @Nullable
    private static CryoStorage of(Player player) {
        return player.level().isClientSide ? null : get(player.level().getServer());
    }

    /** As criaturas congeladas do jogador, na ordem em que entraram. */
    public static List<Entry> entries(Player player) {
        CryoStorage data = of(player);
        return data == null ? List.of() : data.list(player.getUUID());
    }

    /** As entradas de um jogador (cópia). */
    public List<Entry> list(UUID owner) {
        return List.copyOf(players.getOrDefault(owner, List.of()));
    }

    /** Congela a criatura do jogador e a tira do mundo. Só no servidor. */
    public static boolean freeze(Player player, PrehistoricCreature creature) {
        CryoStorage data = of(player);
        if (data == null || !creature.canBeFrozenBy(player)) {
            return false;
        }
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(creature.getType());
        String name = creature.getName().getString();
        Entry entry = new Entry(UUID.randomUUID(), type, creature.freezeData(), creature.creatureLevel(), name);
        // A criatura viva é a verdade: alguma entrada antiga com o mesmo UUID (cópia de criativo) sai.
        data.forget(player.getUUID(), entry.creature());
        data.players.computeIfAbsent(player.getUUID(), id -> new ArrayList<>()).add(entry);
        data.setDirty();
        creature.level().playSound(null, creature.blockPosition(), SoundEvents.GLASS_PLACE, SoundSource.NEUTRAL,
                1.0F, 0.6F);
        creature.discard();
        player.displayClientMessage(Component.translatable("iceagesurvival.cryo.frozen", name), true);
        sync(player);
        return true;
    }

    /**
     * Guarda dados já congelados (de uma cápsula antiga) para o jogador. Se a mesma criatura já está guardada, não
     * entra de novo. {@code false} só se não há onde guardar (cliente, servidor sem Overworld).
     */
    public static boolean adopt(Player player, ResourceLocation type, CompoundTag frozen, int level, String name) {
        CryoStorage data = of(player);
        if (data == null) {
            return false;
        }
        Entry entry = new Entry(UUID.randomUUID(), type, frozen, level, name);
        List<Entry> list = data.players.computeIfAbsent(player.getUUID(), id -> new ArrayList<>());
        UUID creature = entry.creature();
        if (creature == null || list.stream().noneMatch(other -> creature.equals(other.creature()))) {
            list.add(entry);
            data.setDirty();
        }
        return true;
    }

    /** Solta a criatura da entrada na posição dada; a entrada só sai se ela entrou no mundo. Só no servidor. */
    public static boolean release(ServerLevel level, Player player, UUID entryId, Vec3 pos) {
        CryoStorage data = of(player);
        Entry entry = data == null ? null : data.find(player.getUUID(), entryId);
        if (entry == null) {
            return false;
        }
        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entry.type());
        PrehistoricCreature creature = type.isEmpty() ? null
                : PrehistoricCreature.thaw(level, type.get(), entry.data(), pos);
        if (creature == null) {
            player.displayClientMessage(Component.translatable("iceagesurvival.cryo.no_room"), true);
            return false;
        }
        data.players.get(player.getUUID()).removeIf(other -> other.id().equals(entryId));
        data.setDirty();
        level.playSound(null, creature.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.0F, 1.2F);
        player.displayClientMessage(Component.translatable("iceagesurvival.cryo.released", creature.getName()), true);
        sync(player);
        return true;
    }

    /** Pelo menu: solta à frente do jogador, no chão, a uma distância que deixa o corpo livre dele. */
    public static boolean releaseInFront(Player player, UUID entryId) {
        CryoStorage data = of(player);
        Entry entry = data == null ? null : data.find(player.getUUID(), entryId);
        if (entry == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        double distance = 1.5 + BuiltInRegistries.ENTITY_TYPE.getOptional(entry.type())
                .map(type -> type.getWidth() / 2.0).orElse(1.0);
        Vec3 ahead = player.position().add(forward.scale(distance));
        BlockPos column = BlockPos.containing(ahead);
        // Procura o chão perto da altura dos pés: de um bloco acima até três abaixo.
        for (int dy = 1; dy >= -3; dy--) {
            BlockPos feet = column.offset(0, dy, 0);
            if (!level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty()
                    && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()) {
                return release(level, player, entryId, new Vec3(ahead.x, feet.getY(), ahead.z));
            }
        }
        player.displayClientMessage(Component.translatable("iceagesurvival.cryo.no_room"), true);
        return false;
    }

    /** Manda ao cliente a lista das criaturas congeladas do jogador. */
    public static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer && !(player instanceof FakePlayer)
                && serverPlayer.connection != null) {
            ModPayloads.sendToPlayer(serverPlayer, new CryoListPayload(entries(player).stream()
                    .map(entry -> new CryoListPayload.Row(entry.id(), entry.type(), entry.name(), entry.level()))
                    .toList()));
        }
    }

    @Nullable
    private Entry find(UUID owner, UUID entryId) {
        for (Entry entry : players.getOrDefault(owner, List.of())) {
            if (entry.id().equals(entryId)) {
                return entry;
            }
        }
        return null;
    }

    private void forget(UUID owner, @Nullable UUID creature) {
        List<Entry> list = players.get(owner);
        if (creature != null && list != null && list.removeIf(entry -> creature.equals(entry.creature()))) {
            setDirty();
        }
    }

    public static CryoStorage load(CompoundTag tag) {
        CryoStorage data = new CryoStorage();
        for (Tag element : tag.getList("Players", Tag.TAG_COMPOUND)) {
            CompoundTag player = (CompoundTag) element;
            if (!player.hasUUID("Id")) {
                continue;
            }
            List<Entry> list = new ArrayList<>();
            for (Tag frozen : player.getList("Creatures", Tag.TAG_COMPOUND)) {
                Entry entry = Entry.load((CompoundTag) frozen);
                if (entry != null) {
                    list.add(entry);
                }
            }
            if (!list.isEmpty()) {
                data.players.put(player.getUUID("Id"), list);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        players.forEach((owner, entries) -> {
            if (entries.isEmpty()) {
                return;
            }
            CompoundTag player = new CompoundTag();
            player.putUUID("Id", owner);
            ListTag creatures = new ListTag();
            entries.forEach(entry -> creatures.add(entry.save()));
            player.put("Creatures", creatures);
            list.add(player);
        });
        tag.put("Players", list);
        return tag;
    }
}
