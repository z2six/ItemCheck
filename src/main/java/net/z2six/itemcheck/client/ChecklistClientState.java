package net.z2six.itemcheck.client;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.z2six.itemcheck.ChecklistFilterTab;
import net.z2six.itemcheck.ChecklistTabViewState;
import net.z2six.itemcheck.ItemCheckClientBridge;
import net.z2six.itemcheck.network.RequestChecklistStatePayload;
import net.z2six.itemcheck.network.SaveFilterTabsPayload;
import net.z2six.itemcheck.network.SetItemCheckedPayload;

public final class ChecklistClientState {
    private static final Set<ResourceLocation> CHECKED_ITEMS = new HashSet<>();
    private static ChecklistTabViewState allTabViewState = ChecklistTabViewState.defaultState();
    private static List<ChecklistFilterTab> filterTabs = List.of();
    private static boolean synced;

    private ChecklistClientState() {
    }

    public static void installBridge() {
        ItemCheckClientBridge.install(ChecklistClientState::replaceState, ChecklistClientState::applyDelta);
    }

    public static void replaceState(Collection<ResourceLocation> checkedItems, ChecklistTabViewState newAllTabViewState, List<ChecklistFilterTab> newFilterTabs) {
        CHECKED_ITEMS.clear();
        CHECKED_ITEMS.addAll(checkedItems);
        allTabViewState = newAllTabViewState == null ? ChecklistTabViewState.defaultState() : newAllTabViewState;
        filterTabs = List.copyOf(newFilterTabs);
        synced = true;
    }

    public static void applyDelta(ResourceLocation itemId, boolean checked) {
        if (checked) {
            CHECKED_ITEMS.add(itemId);
        } else {
            CHECKED_ITEMS.remove(itemId);
        }
        synced = true;
    }

    public static void reset() {
        CHECKED_ITEMS.clear();
        allTabViewState = ChecklistTabViewState.defaultState();
        filterTabs = List.of();
        synced = false;
    }

    public static boolean isChecked(ResourceLocation itemId) {
        return CHECKED_ITEMS.contains(itemId);
    }

    public static int getCheckedCount() {
        return CHECKED_ITEMS.size();
    }

    public static List<ChecklistFilterTab> getFilterTabs() {
        return filterTabs;
    }

    public static ChecklistTabViewState getAllTabViewState() {
        return allTabViewState;
    }

    public static boolean isSynced() {
        return synced;
    }

    public static void requestSync() {
        if (Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(new RequestChecklistStatePayload());
        }
    }

    public static void toggle(ResourceLocation itemId) {
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }

        boolean checked = !isChecked(itemId);
        applyDelta(itemId, checked);
        PacketDistributor.sendToServer(new SetItemCheckedPayload(itemId, checked));
    }

    public static void saveFilterTabs(ChecklistTabViewState newAllTabViewState, List<ChecklistFilterTab> newFilterTabs) {
        allTabViewState = newAllTabViewState == null ? ChecklistTabViewState.defaultState() : newAllTabViewState;
        filterTabs = List.copyOf(newFilterTabs);
        if (Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(new SaveFilterTabsPayload(allTabViewState, filterTabs));
        }
    }
}
