package net.z2six.itemcheck.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.z2six.itemcheck.ItemCheckNetworking;
import net.z2six.itemcheck.Itemcheck;

public record RequestChecklistStatePayload() implements CustomPacketPayload {
    public static final Type<RequestChecklistStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Itemcheck.MODID, "request_checklist_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestChecklistStatePayload> STREAM_CODEC = CustomPacketPayload.codec((payload, buffer) -> {
    }, buffer -> new RequestChecklistStatePayload());

    @Override
    public Type<RequestChecklistStatePayload> type() {
        return TYPE;
    }

    public static void handle(RequestChecklistStatePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            ItemCheckNetworking.sendFullSync(player);
        }
    }
}
