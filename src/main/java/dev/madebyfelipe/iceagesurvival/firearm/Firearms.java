package dev.madebyfelipe.iceagesurvival.firearm;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.firearms.AmmoFamily;
import dev.madebyfelipe.iceagesurvival.core.firearms.Firearm;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * As armas de fogo no estilo do Dino Crisis 2 (D58): pistola, escopeta e submetralhadora (Bancada de Armaria),
 * metralhadora pesada, canhão sólido e rifle antitanque (só no saque dos postos e da base), as quatro munições, os sons
 * e a esfera do canhão. O dano segue o DC2 por espécie ({@code core/firearms/Ballistics}). Registros próprios, fora
 * dos {@code Mod*}.
 */
public final class Firearms {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, IceAgeSurvival.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, IceAgeSurvival.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, IceAgeSurvival.MODID);

    public static final Map<AmmoFamily, RegistryObject<Item>> AMMO = new EnumMap<>(AmmoFamily.class);
    public static final Map<Firearm, RegistryObject<SoundEvent>> SHOT_SOUNDS = new EnumMap<>(Firearm.class);
    /** A recarga de cada arma, no ritmo da animação dela ({@code tools/gen_firearm_sounds.py}). */
    public static final Map<Firearm, RegistryObject<SoundEvent>> RELOAD_SOUNDS = new EnumMap<>(Firearm.class);
    public static final Map<Firearm, RegistryObject<FirearmItem>> GUNS = new EnumMap<>(Firearm.class);

    static {
        for (AmmoFamily family : AmmoFamily.values()) {
            AMMO.put(family, ITEMS.register(family.itemId(), () -> new Item(new Item.Properties())));
        }
        for (Firearm gun : Firearm.values()) {
            SHOT_SOUNDS.put(gun, sound("firearm.shot." + gun.id()));
            RELOAD_SOUNDS.put(gun, sound("firearm.reload." + gun.id()));
        }
        for (Firearm gun : Firearm.values()) {
            boolean heavy = gun == Firearm.HEAVY_MACHINE_GUN || gun == Firearm.SOLID_CANNON
                    || gun == Firearm.ANTI_TANK_RIFLE;
            GUNS.put(gun, ITEMS.register(gun.id(), () -> new FirearmItem(new Item.Properties()
                    .rarity(heavy ? Rarity.RARE : Rarity.UNCOMMON), gun)));
        }
    }

    /** Clique seco: puxou o gatilho sem munição para recarregar. */
    public static final RegistryObject<SoundEvent> EMPTY = sound("firearm.empty");
    /** A esfera do canhão sólido estourando no alvo: a vibração que destrói as células. */
    public static final RegistryObject<SoundEvent> SOLID_IMPACT = sound("firearm.solid_impact");
    /** O bicho entrou em fúria. */
    public static final RegistryObject<SoundEvent> RAGE = sound("firearm.rage");

    public static final RegistryObject<EntityType<SolidCannonShot>> SOLID_CANNON_SHOT = ENTITY_TYPES.register(
            "solid_cannon_shot", () -> EntityType.Builder.<SolidCannonShot>of(SolidCannonShot::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(1).build("solid_cannon_shot"));

    private Firearms() {
    }

    private static RegistryObject<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(IceAgeSurvival.id(name)));
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        SOUNDS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(Firearms::addToCreativeTabs);
        MinecraftForge.EVENT_BUS.addListener(FirearmHits::onLevelTick);
        MinecraftForge.EVENT_BUS.addListener(FirearmHits::onServerStopped);
    }

    public static FirearmItem gun(Firearm gun) {
        return GUNS.get(gun).get();
    }

    public static Item ammo(AmmoFamily family) {
        return AMMO.get(family).get();
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            for (Firearm gun : Firearm.values()) {
                event.accept(GUNS.get(gun));
            }
            for (AmmoFamily family : AmmoFamily.values()) {
                event.accept(AMMO.get(family));
            }
        }
    }
}
