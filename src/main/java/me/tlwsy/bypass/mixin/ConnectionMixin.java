package me.tlwsy.bypass.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import me.tlwsy.bypass.BypassedConnection;
import me.tlwsy.bypass.BypassFabricCheck;
import me.tlwsy.bypass.EffectPacketFilter;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Connection.class)
public abstract class ConnectionMixin implements BypassedConnection {
    @Shadow
    private Channel channel;

    @Unique
    private volatile boolean bypassFabricCheck$registrySyncBypassed;

    @Override
    public void bypassFabricCheck$markRegistrySyncBypassed() {
        bypassFabricCheck$registrySyncBypassed = true;
    }

    @WrapMethod(method = "doSendPacket")
    private void bypassFabricCheck$filterEffects(Packet<?> packet, ChannelFutureListener listener,
                                                boolean flush, Operation<Void> original) {
        // Admission and optional filtering are independent. Changing /bypass does not
        // change filter preferences, and menu changes apply to existing connections.
        if (bypassFabricCheck$registrySyncBypassed) {
            packet = EffectPacketFilter.filter(packet, BypassFabricCheck.CONFIG.get().filterModdedEffects());
        }

        if (packet != null) {
            original.call(packet, listener, flush);
        } else {
            if (flush) channel.flush();
            if (listener != null) channel.newSucceededFuture().addListener(listener);
        }
    }
}
