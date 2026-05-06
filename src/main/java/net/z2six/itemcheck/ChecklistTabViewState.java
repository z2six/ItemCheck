package net.z2six.itemcheck;

import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;

public record ChecklistTabViewState(ChecklistSortMode sortMode, List<String> manualOrder, boolean hideNonStackable) {
    private static final int MAX_MANUAL_ORDER = 4096;
    private static final String SORT_MODE_KEY = "sort_mode";
    private static final String MANUAL_ORDER_KEY = "manual_order";
    private static final String HIDE_NON_STACKABLE_KEY = "hide_non_stackable";

    public ChecklistTabViewState {
        sortMode = sortMode == null ? ChecklistSortMode.GROUP : sortMode;
        manualOrder = sanitizeManualOrder(manualOrder);
    }

    public ChecklistTabViewState(ChecklistSortMode sortMode, List<String> manualOrder) {
        this(sortMode, manualOrder, true);
    }

    public static ChecklistTabViewState defaultState() {
        return new ChecklistTabViewState(ChecklistSortMode.GROUP, List.of(), true);
    }

    public static ChecklistTabViewState read(RegistryFriendlyByteBuf buffer) {
        ChecklistSortMode[] values = ChecklistSortMode.values();
        int sortModeIndex = buffer.readVarInt();
        ChecklistSortMode sortMode = sortModeIndex >= 0 && sortModeIndex < values.length ? values[sortModeIndex] : ChecklistSortMode.GROUP;
        int count = buffer.readVarInt();
        List<String> manualOrder = new java.util.ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            manualOrder.add(buffer.readUtf(256));
        }
        boolean hideNonStackable = buffer.readBoolean();
        return new ChecklistTabViewState(sortMode, manualOrder, hideNonStackable);
    }

    public static ChecklistTabViewState fromTag(CompoundTag tag) {
        ChecklistSortMode sortMode = readSortMode(tag.getString(SORT_MODE_KEY));
        ListTag orderTag = tag.getList(MANUAL_ORDER_KEY, Tag.TAG_STRING);
        List<String> manualOrder = orderTag.stream()
                .map(Tag::getAsString)
                .toList();
        boolean hideNonStackable = !tag.contains(HIDE_NON_STACKABLE_KEY, Tag.TAG_BYTE) || tag.getBoolean(HIDE_NON_STACKABLE_KEY);
        return new ChecklistTabViewState(sortMode, manualOrder, hideNonStackable);
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(this.sortMode.ordinal());
        buffer.writeVarInt(this.manualOrder.size());
        for (String entryId : this.manualOrder) {
            buffer.writeUtf(entryId, 256);
        }
        buffer.writeBoolean(this.hideNonStackable);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString(SORT_MODE_KEY, this.sortMode.name());
        ListTag orderTag = new ListTag();
        this.manualOrder.stream()
                .map(StringTag::valueOf)
                .forEach(orderTag::add);
        tag.put(MANUAL_ORDER_KEY, orderTag);
        tag.putBoolean(HIDE_NON_STACKABLE_KEY, this.hideNonStackable);
        return tag;
    }

    private static List<String> sanitizeManualOrder(List<String> manualOrder) {
        if (manualOrder == null || manualOrder.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> orderedIds = new LinkedHashSet<>();
        for (String entryId : manualOrder) {
            if (ItemCheckCatalog.isTrackableEntryKey(entryId)) {
                orderedIds.add(entryId);
                if (orderedIds.size() >= MAX_MANUAL_ORDER) {
                    break;
                }
            }
        }
        return List.copyOf(orderedIds);
    }

    private static ChecklistSortMode readSortMode(String value) {
        try {
            return ChecklistSortMode.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return ChecklistSortMode.GROUP;
        }
    }
}
