package net.z2six.itemcheck;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;

public record ChecklistFilterTab(String name, List<ChecklistFilterRule> filters, boolean noDuplicates, ChecklistTabViewState viewState) {
    private static final int MAX_NAME_LENGTH = 120;
    private static final int MAX_FILTERS = 128;
    private static final String NAME_KEY = "name";
    private static final String FILTERS_KEY = "filters";
    private static final String NO_DUPLICATES_KEY = "no_duplicates";
    private static final String VIEW_STATE_KEY = "view_state";

    public ChecklistFilterTab {
        name = sanitizeName(name);
        filters = filters == null ? List.of() : filters.stream().limit(MAX_FILTERS).map(rule -> new ChecklistFilterRule(rule.action(), rule.type(), rule.expression())).toList();
        viewState = viewState == null ? ChecklistTabViewState.defaultState() : new ChecklistTabViewState(viewState.sortMode(), viewState.manualOrder(), viewState.hideNonStackable());
    }

    public static ChecklistFilterTab blank() {
        return new ChecklistFilterTab("New Tab", List.of(), false, ChecklistTabViewState.defaultState());
    }

    public static ChecklistFilterTab read(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<ChecklistFilterRule> filters = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            filters.add(ChecklistFilterRule.read(buffer));
        }

        return new ChecklistFilterTab(
                buffer.readUtf(MAX_NAME_LENGTH),
                filters,
                buffer.readBoolean(),
                ChecklistTabViewState.read(buffer)
        );
    }

    public static ChecklistFilterTab fromTag(CompoundTag tag) {
        if (tag.contains(FILTERS_KEY, Tag.TAG_LIST)) {
            ListTag filtersTag = tag.getList(FILTERS_KEY, Tag.TAG_COMPOUND);
            List<ChecklistFilterRule> filters = filtersTag.stream()
                    .limit(MAX_FILTERS)
                    .map(CompoundTag.class::cast)
                    .map(ChecklistFilterRule::fromTag)
                    .toList();
            ChecklistTabViewState viewState = tag.contains(VIEW_STATE_KEY, Tag.TAG_COMPOUND)
                    ? ChecklistTabViewState.fromTag(tag.getCompound(VIEW_STATE_KEY))
                    : ChecklistTabViewState.defaultState();
            return new ChecklistFilterTab(tag.getString(NAME_KEY), filters, tag.getBoolean(NO_DUPLICATES_KEY), viewState);
        }

        return migrateLegacyTab(tag);
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(this.filters.size());
        for (ChecklistFilterRule filter : this.filters) {
            filter.write(buffer);
        }
        buffer.writeUtf(this.name, MAX_NAME_LENGTH);
        buffer.writeBoolean(this.noDuplicates);
        this.viewState.write(buffer);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString(NAME_KEY, this.name);
        ListTag filtersTag = new ListTag();
        this.filters.stream().map(ChecklistFilterRule::toTag).forEach(filtersTag::add);
        tag.put(FILTERS_KEY, filtersTag);
        tag.putBoolean(NO_DUPLICATES_KEY, this.noDuplicates);
        tag.put(VIEW_STATE_KEY, this.viewState.toTag());
        return tag;
    }

    private static ChecklistFilterTab migrateLegacyTab(CompoundTag tag) {
        List<ChecklistFilterRule> filters = new ArrayList<>();
        addLegacyFilter(filters, ChecklistFilterType.ITEM_NAME, tag.getString("name_contains"));
        addLegacyFilter(filters, ChecklistFilterType.ITEM_ID, tag.getString("item_id_contains"));
        addLegacyFilter(filters, ChecklistFilterType.ITEM_TAG, tag.getString("item_tag_contains"));
        addLegacyFilter(filters, ChecklistFilterType.BLOCK_TAG, tag.getString("block_tag_contains"));
        addLegacyFilter(filters, ChecklistFilterType.GROUP, tag.getString("group_contains"));
        return new ChecklistFilterTab(tag.getString(NAME_KEY), filters, tag.getBoolean(NO_DUPLICATES_KEY), ChecklistTabViewState.defaultState());
    }

    private static void addLegacyFilter(List<ChecklistFilterRule> filters, ChecklistFilterType type, String expression) {
        if (expression != null && !expression.isBlank()) {
            filters.add(new ChecklistFilterRule(ChecklistFilterAction.INCLUDE, type, expression));
        }
    }

    private static String sanitizeName(String value) {
        if (value == null) {
            return "Untitled Tab";
        }

        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return "Untitled Tab";
        }

        return trimmed.length() > MAX_NAME_LENGTH ? trimmed.substring(0, MAX_NAME_LENGTH) : trimmed;
    }
}
