package net.z2six.itemcheck;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class ItemCheckSavedData extends SavedData {
    private static final String DATA_NAME = Itemcheck.MODID + "_checked_items";
    private static final String CHECKED_ITEMS_KEY = "checked_items";
    private static final String FILTER_TABS_KEY = "filter_tabs";
    private static final String ALL_TAB_VIEW_STATE_KEY = "all_tab_view_state";
    private static final int MAX_FILTER_TABS = 64;

    private final Set<ResourceLocation> checkedItems = new HashSet<>();
    private List<ChecklistFilterTab> filterTabs = List.of();
    private ChecklistTabViewState allTabViewState = ChecklistTabViewState.defaultState();

    public static ItemCheckSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ItemCheckSavedData::new, ItemCheckSavedData::load), DATA_NAME);
    }

    private static ItemCheckSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        ItemCheckSavedData data = new ItemCheckSavedData();
        ListTag checkedList = tag.getList(CHECKED_ITEMS_KEY, Tag.TAG_STRING);

        for (int index = 0; index < checkedList.size(); index++) {
            ResourceLocation itemId = ResourceLocation.tryParse(checkedList.getString(index));
            if (itemId != null && ItemCheckCatalog.isTrackable(itemId)) {
                data.checkedItems.add(itemId);
            }
        }

        ListTag tabsList = tag.getList(FILTER_TABS_KEY, Tag.TAG_COMPOUND);
        data.filterTabs = tabsList.stream()
                .limit(MAX_FILTER_TABS)
                .map(CompoundTag.class::cast)
                .map(ChecklistFilterTab::fromTag)
                .toList();
        if (tag.contains(ALL_TAB_VIEW_STATE_KEY, Tag.TAG_COMPOUND)) {
            data.allTabViewState = ChecklistTabViewState.fromTag(tag.getCompound(ALL_TAB_VIEW_STATE_KEY));
        }

        return data;
    }

    public Set<ResourceLocation> getCheckedItems() {
        return Set.copyOf(this.checkedItems);
    }

    public List<ChecklistFilterTab> getFilterTabs() {
        return List.copyOf(this.filterTabs);
    }

    public ChecklistTabViewState getAllTabViewState() {
        return this.allTabViewState;
    }

    public void setChecked(ResourceLocation itemId, boolean checked) {
        boolean changed = checked ? this.checkedItems.add(itemId) : this.checkedItems.remove(itemId);
        if (changed) {
            this.setDirty();
        }
    }

    public void setFilterTabs(List<ChecklistFilterTab> tabs) {
        List<ChecklistFilterTab> sanitizedTabs = tabs.stream()
                .limit(MAX_FILTER_TABS)
                .map(tab -> new ChecklistFilterTab(tab.name(), tab.filters(), tab.noDuplicates(), tab.viewState()))
                .toList();

        if (!this.filterTabs.equals(sanitizedTabs)) {
            this.filterTabs = sanitizedTabs;
            this.setDirty();
        }
    }

    public void setAllTabViewState(ChecklistTabViewState viewState) {
        ChecklistTabViewState sanitizedState = viewState == null
                ? ChecklistTabViewState.defaultState()
                : new ChecklistTabViewState(viewState.sortMode(), viewState.manualOrder(), viewState.hideNonStackable());
        if (!this.allTabViewState.equals(sanitizedState)) {
            this.allTabViewState = sanitizedState;
            this.setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag checkedList = new ListTag();
        this.checkedItems.stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .forEach(itemId -> checkedList.add(StringTag.valueOf(itemId.toString())));
        tag.put(CHECKED_ITEMS_KEY, checkedList);

        ListTag tabsList = new ListTag();
        this.filterTabs.stream()
                .map(ChecklistFilterTab::toTag)
                .forEach(tabsList::add);
        tag.put(FILTER_TABS_KEY, tabsList);
        tag.put(ALL_TAB_VIEW_STATE_KEY, this.allTabViewState.toTag());
        return tag;
    }
}
