package net.z2six.itemcheck.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.z2six.itemcheck.ItemCheckClientBridge;
import net.z2six.itemcheck.Itemcheck;

public record UpdateItemCheckedPayload(String entryId, boolean checked) implements CustomPacketPayload {
    private static final int MAX_ENTRY_ID_LENGTH = 256;
    public static final Type<UpdateItemCheckedPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Itemcheck.MODID, "update_item_checked"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateItemCheckedPayload> STREAM_CODEC = CustomPacketPayload.codec(UpdateItemCheckedPayload::write, UpdateItemCheckedPayload::new);

    public UpdateItemCheckedPayload(RegistryFriendlyByteBuf buffer) {
        this(buffer.readUtf(MAX_ENTRY_ID_LENGTH), buffer.readBoolean());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(this.entryId, MAX_ENTRY_ID_LENGTH);
        buffer.writeBoolean(this.checked);
    }

    @Override
    public Type<UpdateItemCheckedPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateItemCheckedPayload payload, IPayloadContext context) {
        ItemCheckClientBridge.applyDelta(payload.entryId(), payload.checked());
    }
}
