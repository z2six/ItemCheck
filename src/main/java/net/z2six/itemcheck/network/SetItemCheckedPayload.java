package net.z2six.itemcheck.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.z2six.itemcheck.ItemCheckCatalog;
import net.z2six.itemcheck.ItemCheckNetworking;
import net.z2six.itemcheck.ItemCheckSavedData;
import net.z2six.itemcheck.Itemcheck;

public record SetItemCheckedPayload(ResourceLocation itemId, boolean checked) implements CustomPacketPayload {
    public static final Type<SetItemCheckedPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Itemcheck.MODID, "set_item_checked"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetItemCheckedPayload> STREAM_CODEC = CustomPacketPayload.codec(SetItemCheckedPayload::write, SetItemCheckedPayload::new);

    public SetItemCheckedPayload(RegistryFriendlyByteBuf buffer) {
        this(buffer.readResourceLocation(), buffer.readBoolean());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeResourceLocation(this.itemId);
        buffer.writeBoolean(this.checked);
    }

    @Override
    public Type<SetItemCheckedPayload> type() {
        return TYPE;
    }

    public static void handle(SetItemCheckedPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !ItemCheckCatalog.isTrackable(payload.itemId())) {
            return;
        }

        ItemCheckSavedData.get(player.serverLevel()).setChecked(payload.itemId(), payload.checked());
        ItemCheckNetworking.broadcastUpdate(payload.itemId(), payload.checked());
    }
}
