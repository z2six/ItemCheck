package net.z2six.itemcheck;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public final class ItemCheckClientBridge {
    private static FullSyncHandler fullSyncHandler = (checkedItems, allTabViewState, filterTabs) -> {
    };
    private static BiConsumer<ResourceLocation, Boolean> deltaHandler = (itemId, checked) -> {
    };

    private ItemCheckClientBridge() {
    }

    public static void install(FullSyncHandler newFullSyncHandler, BiConsumer<ResourceLocation, Boolean> newDeltaHandler) {
        fullSyncHandler = newFullSyncHandler;
        deltaHandler = newDeltaHandler;
    }

    public static void applyFullSync(Collection<ResourceLocation> checkedItems, ChecklistTabViewState allTabViewState, List<ChecklistFilterTab> filterTabs) {
        fullSyncHandler.accept(checkedItems, allTabViewState, filterTabs);
    }

    public static void applyDelta(ResourceLocation itemId, boolean checked) {
        deltaHandler.accept(itemId, checked);
    }

    @FunctionalInterface
    public interface FullSyncHandler {
        void accept(Collection<ResourceLocation> checkedItems, ChecklistTabViewState allTabViewState, List<ChecklistFilterTab> filterTabs);
    }
}
