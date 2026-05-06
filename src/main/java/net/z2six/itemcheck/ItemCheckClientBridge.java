package net.z2six.itemcheck;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.List;

public final class ItemCheckClientBridge {
    private static FullSyncHandler fullSyncHandler = (checkedItems, allTabViewState, filterTabs) -> {
    };
    private static BiConsumer<String, Boolean> deltaHandler = (entryId, checked) -> {
    };

    private ItemCheckClientBridge() {
    }

    public static void install(FullSyncHandler newFullSyncHandler, BiConsumer<String, Boolean> newDeltaHandler) {
        fullSyncHandler = newFullSyncHandler;
        deltaHandler = newDeltaHandler;
    }

    public static void applyFullSync(Collection<String> checkedItems, ChecklistTabViewState allTabViewState, List<ChecklistFilterTab> filterTabs) {
        fullSyncHandler.accept(checkedItems, allTabViewState, filterTabs);
    }

    public static void applyDelta(String entryId, boolean checked) {
        deltaHandler.accept(entryId, checked);
    }

    @FunctionalInterface
    public interface FullSyncHandler {
        void accept(Collection<String> checkedItems, ChecklistTabViewState allTabViewState, List<ChecklistFilterTab> filterTabs);
    }
}
