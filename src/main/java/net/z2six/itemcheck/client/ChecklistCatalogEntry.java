package net.z2six.itemcheck.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.z2six.itemcheck.ItemCheckCatalog;

public record ChecklistCatalogEntry(
        String entryId,
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
                .flatMap(item -> createEntries(item).stream())
                .sorted(Comparator.comparing(ChecklistCatalogEntry::primarySortTag, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ChecklistCatalogEntry::displayName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ChecklistCatalogEntry::entryId))
                .toList();
    }

    public List<Component> buildTooltip() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(this.stack.getHoverName().copy().withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.literal(this.entryId).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("itemcheck.tooltip.stack_size", this.maxStackSize).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("itemcheck.tooltip.primary_sort_tag", this.primarySortTag).withStyle(ChatFormatting.GOLD));
        appendTagSection(tooltip, Component.translatable("itemcheck.tooltip.item_tags"), this.itemTags, ChatFormatting.GREEN);
        appendTagSection(tooltip, Component.translatable("itemcheck.tooltip.block_tags"), this.blockTags, ChatFormatting.AQUA);
        return tooltip;
    }

    public String metadataLine() {
        return this.groupLabel + " | " + this.entryId + " | x" + this.maxStackSize;
    }

    public boolean matchesSearch(String query) {
        return query.isBlank() || this.searchableText.contains(query);
    }

    private static List<ChecklistCatalogEntry> createEntries(Item item) {
        if (item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION || item == Items.TIPPED_ARROW) {
            return BuiltInRegistries.POTION.entrySet().stream()
                    .map(entry -> fromStack(item, PotionContents.createItemStack(item, BuiltInRegistries.POTION.wrapAsHolder(entry.getValue())), entry.getKey().location().toString()))
                    .toList();
        }
        if (item == Items.ENCHANTED_BOOK && Minecraft.getInstance().level != null) {
            return Minecraft.getInstance().level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements()
                    .flatMap(enchantment -> createEnchantedBookEntries(enchantment).stream())
                    .toList();
        }
        return List.of(fromStack(item, new ItemStack(item), null));
    }

    private static List<ChecklistCatalogEntry> createEnchantedBookEntries(net.minecraft.core.Holder.Reference<Enchantment> enchantment) {
        List<ChecklistCatalogEntry> entries = new ArrayList<>();
        String enchantmentId = enchantment.key().location().toString();
        for (int level = enchantment.value().getMinLevel(); level <= enchantment.value().getMaxLevel(); level++) {
            entries.add(fromStack(Items.ENCHANTED_BOOK, EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, level)), enchantmentId + "/" + level));
        }
        return entries;
    }

    private static ChecklistCatalogEntry fromStack(Item item, ItemStack stack, String variantId) {
        List<String> itemTags = ItemCheckCatalog.getItemTags(item);
        List<String> blockTags = ItemCheckCatalog.getBlockTags(item);
        String primarySortTag = ItemCheckCatalog.choosePrimarySortTag(itemTags, blockTags);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        String itemIdString = itemId.toString();
        String entryId = variantId == null ? itemIdString : itemIdString + "#" + variantId;
        String displayName = stack.getHoverName().getString();
        String groupLabel = ItemCheckCatalog.formatGroupLabel(primarySortTag);
        return new ChecklistCatalogEntry(
                entryId,
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
                buildSearchableText(displayName, entryId, itemIdString, groupLabel, primarySortTag, itemTags, blockTags)
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
            String entryId,
            String itemId,
            String groupLabel,
            String primarySortTag,
            List<String> itemTags,
            List<String> blockTags
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append(displayName).append('\n')
                .append(entryId).append('\n')
                .append(itemId).append('\n')
                .append(groupLabel).append('\n')
                .append(primarySortTag).append('\n');
        itemTags.forEach(tag -> builder.append(tag).append('\n'));
        blockTags.forEach(tag -> builder.append(tag).append('\n'));
        return builder.toString().toLowerCase();
    }
}
