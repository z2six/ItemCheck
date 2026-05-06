package net.z2six.itemcheck.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.z2six.itemcheck.ChecklistFilterTab;
import net.z2six.itemcheck.ChecklistTabViewState;
import net.z2six.itemcheck.ItemCheckClientBridge;
import net.z2six.itemcheck.Itemcheck;

public record SyncChecklistStatePayload(List<String> checkedItems, ChecklistTabViewState allTabViewState, List<ChecklistFilterTab> filterTabs) implements CustomPacketPayload {
    private static final int MAX_ENTRY_ID_LENGTH = 256;
    public static final Type<SyncChecklistStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Itemcheck.MODID, "sync_checklist_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncChecklistStatePayload> STREAM_CODEC = CustomPacketPayload.codec(SyncChecklistStatePayload::write, SyncChecklistStatePayload::new);

    public SyncChecklistStatePayload {
        checkedItems = List.copyOf(checkedItems);
        allTabViewState = allTabViewState == null ? ChecklistTabViewState.defaultState() : allTabViewState;
        filterTabs = List.copyOf(filterTabs);
    }

    public SyncChecklistStatePayload(RegistryFriendlyByteBuf buffer) {
        this(readItemIds(buffer), ChecklistTabViewState.read(buffer), readTabs(buffer));
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(this.checkedItems.size());
        for (String entryId : this.checkedItems) {
            buffer.writeUtf(entryId, MAX_ENTRY_ID_LENGTH);
        }

        this.allTabViewState.write(buffer);
        buffer.writeVarInt(this.filterTabs.size());
        for (ChecklistFilterTab filterTab : this.filterTabs) {
            filterTab.write(buffer);
        }
    }

    @Override
    public Type<SyncChecklistStatePayload> type() {
        return TYPE;
    }

    public static void handle(SyncChecklistStatePayload payload, IPayloadContext context) {
        ItemCheckClientBridge.applyFullSync(payload.checkedItems(), payload.allTabViewState(), payload.filterTabs());
    }

    private static List<String> readItemIds(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<String> ids = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            ids.add(buffer.readUtf(MAX_ENTRY_ID_LENGTH));
        }
        return ids;
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
