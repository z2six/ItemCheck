package net.z2six.itemcheck.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.z2six.itemcheck.ChecklistFilterTab;
import net.z2six.itemcheck.ChecklistTabViewState;
import net.z2six.itemcheck.ItemCheckNetworking;
import net.z2six.itemcheck.ItemCheckSavedData;
import net.z2six.itemcheck.Itemcheck;

public record SaveFilterTabsPayload(ChecklistTabViewState allTabViewState, List<ChecklistFilterTab> filterTabs) implements CustomPacketPayload {
    public static final Type<SaveFilterTabsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Itemcheck.MODID, "save_filter_tabs"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SaveFilterTabsPayload> STREAM_CODEC = CustomPacketPayload.codec(SaveFilterTabsPayload::write, SaveFilterTabsPayload::new);

    public SaveFilterTabsPayload {
        allTabViewState = allTabViewState == null ? ChecklistTabViewState.defaultState() : allTabViewState;
        filterTabs = List.copyOf(filterTabs);
    }

    public SaveFilterTabsPayload(RegistryFriendlyByteBuf buffer) {
        this(ChecklistTabViewState.read(buffer), readTabs(buffer));
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        this.allTabViewState.write(buffer);
        buffer.writeVarInt(this.filterTabs.size());
        for (ChecklistFilterTab filterTab : this.filterTabs) {
            filterTab.write(buffer);
        }
    }

    @Override
    public Type<SaveFilterTabsPayload> type() {
        return TYPE;
    }

    public static void handle(SaveFilterTabsPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        ItemCheckSavedData savedData = ItemCheckSavedData.get(player.serverLevel());
        savedData.setAllTabViewState(payload.allTabViewState());
        savedData.setFilterTabs(payload.filterTabs());
        if (player.getServer() != null) {
            ItemCheckNetworking.sendFullSyncToAll(player.getServer());
        }
    }

    private static List<ChecklistFilterTab> readTabs(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<ChecklistFilterTab> tabs = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            tabs.add(ChecklistFilterTab.read(buffer));
        }
        return tabs;
    }
}
