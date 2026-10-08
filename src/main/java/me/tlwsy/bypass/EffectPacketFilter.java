package me.tlwsy.bypass;

import net.minecraft.core.Holder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.world.effect.MobEffect;

import java.util.ArrayList;
import java.util.List;

public final class EffectPacketFilter {
    private EffectPacketFilter() {
    }

    /** Returns null when the packet contains only effects unavailable to vanilla clients. */
    public static Packet<?> filter(Packet<?> packet, boolean enabled) {
        if (!enabled) return packet;

        if (packet instanceof ClientboundBundlePacket bundle) {
            List<Packet<? super ClientGamePacketListener>> retained = new ArrayList<>();
            boolean changed = false;

            // Protocol bundles cannot contain other bundles. Preserve every unrelated packet.
            for (Packet<? super ClientGamePacketListener> child : bundle.subPackets()) {
                if (isUnsupportedEffect(child)) {
                    changed = true;
                } else {
                    retained.add(child);
                }
            }

            if (!changed) return packet;
            return retained.isEmpty() ? null : new ClientboundBundlePacket(retained);
        }

        return isUnsupportedEffect(packet) ? null : packet;
    }

    private static boolean isUnsupportedEffect(Packet<?> packet) {
        if (packet instanceof ClientboundUpdateMobEffectPacket update) {
            return !isVanillaEffect(update.getEffect());
        }
        if (packet instanceof ClientboundRemoveMobEffectPacket remove) {
            return !isVanillaEffect(remove.effect());
        }
        return false;
    }

    private static boolean isVanillaEffect(Holder<MobEffect> effect) {
        return effect.unwrapKey()
                .map(key -> key.identifier().getNamespace().equals("minecraft"))
                .orElse(false);
    }
}
