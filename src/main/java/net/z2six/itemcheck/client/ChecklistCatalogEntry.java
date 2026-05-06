package net.z2six.itemcheck.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.z2six.itemcheck.ItemCheckCatalog;

public record ChecklistCatalogEntry(
        ResourceLocation itemId,
        ItemStack stack,
        String displayName,
        String displayNameLower,
        String itemIdString,
        String itemIdLower,
        int maxStackSize,
        String primarySortTag,
        String groupLabel,
        String groupLabelLower,
        List<String> itemTags,
        List<String> blockTags,
        String searchableText
) {
    public ChecklistCatalogEntry {
        itemTags = List.copyOf(itemTags);
        blockTags = List.copyOf(blockTags);
    }

    public static List<ChecklistCatalogEntry> createCatalog() {
        return BuiltInRegistries.ITEM.stream()
                .filter(ItemCheckCatalog::isTrackable)
                .map(ChecklistCatalogEntry::fromItem)
                .sorted(Comparator.comparing(ChecklistCatalogEntry::primarySortTag, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ChecklistCatalogEntry::displayName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(entry -> entry.itemId().toString()))
                .toList();
    }

    public List<Component> buildTooltip() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(this.stack.getHoverName().copy().withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.literal(this.itemId.toString()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("itemcheck.tooltip.stack_size", this.maxStackSize).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("itemcheck.tooltip.primary_sort_tag", this.primarySortTag).withStyle(ChatFormatting.GOLD));
        appendTagSection(tooltip, Component.translatable("itemcheck.tooltip.item_tags"), this.itemTags, ChatFormatting.GREEN);
        appendTagSection(tooltip, Component.translatable("itemcheck.tooltip.block_tags"), this.blockTags, ChatFormatting.AQUA);
        return tooltip;
    }

    public String metadataLine() {
        return this.groupLabel + " | " + this.itemId + " | x" + this.maxStackSize;
    }

    public boolean matchesSearch(String query) {
        return query.isBlank() || this.searchableText.contains(query);
    }

    private static ChecklistCatalogEntry fromItem(Item item) {
        ItemStack stack = new ItemStack(item);
        List<String> itemTags = ItemCheckCatalog.getItemTags(item);
        List<String> blockTags = ItemCheckCatalog.getBlockTags(item);
        String primarySortTag = ItemCheckCatalog.choosePrimarySortTag(itemTags, blockTags);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        String displayName = stack.getHoverName().getString();
        String groupLabel = ItemCheckCatalog.formatGroupLabel(primarySortTag);
        String itemIdString = itemId.toString();
        return new ChecklistCatalogEntry(
                itemId,
                stack,
                displayName,
                displayName.toLowerCase(),
                itemIdString,
                itemIdString.toLowerCase(),
                item.getDefaultMaxStackSize(),
                primarySortTag,
                groupLabel,
                groupLabel.toLowerCase(),
                itemTags,
                blockTags,
                buildSearchableText(displayName, itemIdString, groupLabel, primarySortTag, itemTags, blockTags)
        );
    }

    private static void appendTagSection(List<Component> tooltip, Component title, List<String> tags, ChatFormatting color) {
        tooltip.add(Component.empty());
        tooltip.add(title.copy().withStyle(ChatFormatting.YELLOW));
        if (tags.isEmpty()) {
            tooltip.add(Component.translatable("itemcheck.tooltip.none").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        for (String tag : tags) {
            tooltip.add(Component.literal(tag).withStyle(color));
        }
    }

    private static String buildSearchableText(
            String displayName,
            String itemId,
            String groupLabel,
            String primarySortTag,
            List<String> itemTags,
            List<String> blockTags
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append(displayName).append('\n')
                .append(itemId).append('\n')
                .append(groupLabel).append('\n')
                .append(primarySortTag).append('\n');
        itemTags.forEach(tag -> builder.append(tag).append('\n'));
        blockTags.forEach(tag -> builder.append(tag).append('\n'));
        return builder.toString().toLowerCase();
    }
}
