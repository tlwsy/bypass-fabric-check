package me.tlwsy.bypass;

import me.tlwsy.bypass.config.BypassSettings;
import com.mojang.serialization.Lifecycle;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EffectPacketFilterTest {
    private static Holder<MobEffect> customEffect;
    private static RegistryAccess serverRegistries;
    private static RegistryAccess clientRegistries;

    @BeforeAll
    static void initializeRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        MappedRegistry<MobEffect> registry = new MappedRegistry<>(Registries.MOB_EFFECT, Lifecycle.stable());
        BuiltInRegistries.MOB_EFFECT.listElements().forEach(effect ->
                Registry.registerForHolder(registry, effect.key(), effect.value()));
        while (registry.size() < 46) {
            Registry.registerForHolder(registry, Identifier.fromNamespaceAndPath("testmod", "effect_" + registry.size()),
                    new MobEffect(MobEffectCategory.NEUTRAL, 0) {});
        }
        customEffect = Registry.registerForHolder(registry, Identifier.fromNamespaceAndPath("xaerominimap", "no_cave_maps"),
                new MobEffect(MobEffectCategory.NEUTRAL, 0) {});
        registry.freeze();
        serverRegistries = new RegistryAccess.ImmutableRegistryAccess(List.of(registry));
        clientRegistries = new RegistryAccess.ImmutableRegistryAccess(List.of(BuiltInRegistries.MOB_EFFECT));
    }

    @Test
    void reproducesReportedUnknownIdAndFiltersPacketBeforeEncoding() {
        var packet = update(customEffect, false);
        var bytes = Unpooled.buffer();
        try {
            ClientboundUpdateMobEffectPacket.STREAM_CODEC.encode(new RegistryFriendlyByteBuf(bytes, serverRegistries), packet);
            var error = assertThrows(IllegalArgumentException.class, () ->
                    ClientboundUpdateMobEffectPacket.STREAM_CODEC.decode(new RegistryFriendlyByteBuf(bytes, clientRegistries)));
            assertEquals("No value with id 46", error.getMessage());
            assertNull(EffectPacketFilter.filter(packet, true));
        } finally {
            bytes.release();
        }
    }

    @Test
    void preservedVanillaPacketStillDecodesWithClientRegistries() {
        var packet = update(MobEffects.SPEED, true);
        var bytes = Unpooled.buffer();
        try {
            var filtered = assertInstanceOf(ClientboundUpdateMobEffectPacket.class, EffectPacketFilter.filter(packet, true));
            ClientboundUpdateMobEffectPacket.STREAM_CODEC.encode(new RegistryFriendlyByteBuf(bytes, serverRegistries), filtered);
            var decoded = ClientboundUpdateMobEffectPacket.STREAM_CODEC.decode(new RegistryFriendlyByteBuf(bytes, clientRegistries));
            assertEquals(MobEffects.SPEED, decoded.getEffect());
            assertEquals(200, decoded.getEffectDurationTicks());
        } finally {
            bytes.release();
        }
    }

    @Test
    void dropsCustomEffectUpdatesIncludingLoginResends() {
        assertNull(EffectPacketFilter.filter(update(customEffect, false), true));
        assertNull(EffectPacketFilter.filter(update(customEffect, true), true));
    }

    @Test
    void dropsCustomEffectRemoval() {
        assertNull(EffectPacketFilter.filter(new ClientboundRemoveMobEffectPacket(1, customEffect), true));
    }

    @Test
    void preservesVanillaEffectUpdatesAndRemoval() {
        Packet<?> update = update(MobEffects.SPEED, false);
        Packet<?> remove = new ClientboundRemoveMobEffectPacket(1, MobEffects.SPEED);
        assertSame(update, EffectPacketFilter.filter(update, true));
        assertSame(remove, EffectPacketFilter.filter(remove, true));
    }

    @Test
    void preservesUnrelatedPackets() {
        Packet<?> chat = new ClientboundSystemChatPacket(Component.literal("Effect applied"), false);
        assertSame(chat, EffectPacketFilter.filter(chat, true));
    }

    @Test
    void removesOnlyCustomEffectsFromBundleWithoutMutatingSharedPacket() {
        var speed = update(MobEffects.SPEED, false);
        var chat = new ClientboundSystemChatPacket(Component.literal("Effect applied"), false);
        var custom = update(customEffect, false);
        var remove = new ClientboundRemoveMobEffectPacket(1, customEffect);
        var bundle = new ClientboundBundlePacket(List.of(speed, custom, chat, remove));

        var filtered = assertInstanceOf(ClientboundBundlePacket.class, EffectPacketFilter.filter(bundle, true));
        assertEquals(List.of(speed, chat), packets(filtered));
        // The same source bundle can still be sent intact to a compatible modded client.
        assertEquals(List.of(speed, custom, chat, remove), packets(bundle));
    }

    @Test
    void dropsBundleContainingOnlyUnsupportedEffects() {
        var bundle = new ClientboundBundlePacket(List.of(update(customEffect, true),
                new ClientboundRemoveMobEffectPacket(1, customEffect)));
        assertNull(EffectPacketFilter.filter(bundle, true));
    }

    @Test
    void preservesUnchangedBundle() {
        var bundle = new ClientboundBundlePacket(List.of(update(MobEffects.SPEED, true)));
        assertSame(bundle, EffectPacketFilter.filter(bundle, true));
    }

    @Test
    void rejectsUnregisteredEffect() {
        assertNull(EffectPacketFilter.filter(update(Holder.direct(customEffect.value()), false), true));
    }

    @Test
    void defaultSettingsDoNotFilterAnyEffectPackets() {
        var update = update(customEffect, false);
        var remove = new ClientboundRemoveMobEffectPacket(1, customEffect);
        var bundle = new ClientboundBundlePacket(List.of(update, remove));
        for (Packet<?> packet : List.of(update, remove, bundle)) {
            assertSame(packet, EffectPacketFilter.filter(packet, BypassSettings.DEFAULTS.filterModdedEffects()));
        }
    }

    @Test
    void disablingFilterImmediatelyPassesPacketsAgain() {
        var packet = update(customEffect, false);
        assertNull(EffectPacketFilter.filter(packet, true));
        assertSame(packet, EffectPacketFilter.filter(packet, false));
    }

    private static ClientboundUpdateMobEffectPacket update(Holder<MobEffect> effect, boolean blend) {
        return new ClientboundUpdateMobEffectPacket(1, new MobEffectInstance(effect, 200), blend);
    }

    private static List<Packet<?>> packets(ClientboundBundlePacket bundle) {
        List<Packet<?>> packets = new ArrayList<>();
        bundle.subPackets().forEach(packets::add);
        return packets;
    }
}
