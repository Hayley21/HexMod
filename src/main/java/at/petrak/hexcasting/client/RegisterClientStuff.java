package at.petrak.hexcasting.client;

import at.petrak.hexcasting.api.SpellDatum;
import at.petrak.hexcasting.common.blocks.HexBlocks;
import at.petrak.hexcasting.common.items.HexItems;
import at.petrak.hexcasting.common.items.ItemFocus;
import at.petrak.hexcasting.common.items.magic.ItemPackagedSpell;
import at.petrak.hexcasting.common.particles.ConjureParticle;
import at.petrak.hexcasting.common.particles.HexParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.BiConsumer;

public class RegisterClientStuff {
    @SubscribeEvent
    public static void init(FMLClientSetupEvent evt) {
        evt.enqueueWork(() -> {
            ItemProperties.register(HexItems.FOCUS.get(), ItemFocus.DATATYPE_PRED,
                (stack, level, holder, holderID) -> {
                    var tag = stack.getOrCreateTag();
                    var isSealed = tag.getBoolean(ItemFocus.TAG_SEALED);
                    var baseNum = isSealed ? 100f : 0f;
                    if (stack.hasTag()) {
                        var typename = tag.getCompound(ItemFocus.TAG_DATA).getAllKeys().iterator().next();
                        return baseNum + switch (typename) {
                            case SpellDatum.TAG_ENTITY -> 1f;
                            case SpellDatum.TAG_DOUBLE -> 2f;
                            case SpellDatum.TAG_VEC3 -> 3f;
                            case SpellDatum.TAG_WIDGET -> 4f;
                            case SpellDatum.TAG_LIST -> 5f;
                            case SpellDatum.TAG_PATTERN -> 6f;
                            default -> 0f; // uh oh
                        };
                    } else {
                        return baseNum;
                    }
                });
            for (RegistryObject<Item> packager : new RegistryObject[]{
                HexItems.CYPHER,
                HexItems.TRINKET,
                HexItems.ARTIFACT,
            }) {
                ItemProperties.register(packager.get(), ItemPackagedSpell.HAS_PATTERNS_PRED,
                    (stack, level, holder, holderID) ->
                        stack.getOrCreateTag().contains(ItemPackagedSpell.TAG_PATTERNS) ? 1f : 0f
                );
            }

            HexTooltips.init();
        });

        renderLayers(ItemBlockRenderTypes::setRenderLayer);
    }

    public static void renderLayers(BiConsumer<Block, RenderType> consumer) {
        consumer.accept(HexBlocks.CONJURED.get(), RenderType.cutout());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerParticles(ParticleFactoryRegisterEvent event) {
        ParticleEngine particleManager = Minecraft.getInstance().particleEngine;
        particleManager.register(HexParticles.CONJURE_BLOCK_PARTICLE.get(), ConjureParticle.BlockProvider::new);
        particleManager.register(HexParticles.CONJURE_LIGHT_PARTICLE.get(), ConjureParticle.LightProvider::new);
    }
}
