package net.z2six.itemcheck;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class ItemCheckCatalog {
    private static final Set<String> PREFERRED_GROUPS = Set.of(
            "planks",
            "logs",
            "saplings",
            "leaves",
            "flowers",
            "wool",
            "carpets",
            "concrete",
            "terracotta",
            "glass_blocks",
            "stairs",
            "slabs",
            "fences",
            "fence_gates",
            "doors",
            "trapdoors",
            "buttons",
            "pressure_plates",
            "signs",
            "hanging_signs",
            "boats",
            "banners"
    );
    private static final List<String> DEPRIORITIZED_PREFIXES = List.of(
            "mineable/",
            "needs_",
            "incorrect_for_",
            "infiniburn_",
            "feature_cannot_replace"
    );
    private static final List<String> DEPRIORITIZED_CONTAINS = List.of(
            "replaceable",
            "_spawnable_on",
            "carver",
            "geode",
            "occludes",
            "dragon",
            "beacon",
            "portal",
            "trial_spawner",
            "sculk"
    );

    private ItemCheckCatalog() {
    }

    public static boolean isTrackable(Item item) {
        if (item == Items.AIR) {
            return false;
        }

        return true;
    }

    public static boolean isTrackable(ResourceLocation itemId) {
        return BuiltInRegistries.ITEM.containsKey(itemId) && isTrackable(BuiltInRegistries.ITEM.get(itemId));
    }

    public static boolean isTrackableEntryKey(String entryKey) {
        if (entryKey == null || entryKey.isBlank()) {
            return false;
        }

        String itemId = entryKey;
        int variantSeparator = entryKey.indexOf('#');
        if (variantSeparator >= 0) {
            itemId = entryKey.substring(0, variantSeparator);
        }

        ResourceLocation parsedItemId = ResourceLocation.tryParse(itemId);
        return parsedItemId != null && isTrackable(parsedItemId);
    }

    public static List<String> getItemTags(Item item) {
        return stringifyTags(BuiltInRegistries.ITEM.wrapAsHolder(item).tags());
    }

    public static List<String> getBlockTags(Item item) {
        if (item instanceof BlockItem blockItem) {
            return stringifyTags(BuiltInRegistries.BLOCK.wrapAsHolder(blockItem.getBlock()).tags());
        }

        return List.of();
    }

    public static String choosePrimarySortTag(List<String> itemTags, List<String> blockTags) {
        return findBestTag(itemTags, false).orElseGet(() -> findBestTag(blockTags, true).orElse("misc"));
    }

    public static String formatGroupLabel(String tagId) {
        return tagPath(tagId).replace('_', ' ');
    }

    private static Optional<String> findBestTag(List<String> tags, boolean blockTag) {
        return tags.stream()
                .max(Comparator.comparingInt((String tagId) -> tagScore(tagId, blockTag))
                        .thenComparing(String::length, Comparator.reverseOrder())
                        .thenComparing(Comparator.naturalOrder()));
    }

    private static int tagScore(String tagId, boolean blockTag) {
        String path = tagPath(tagId);
        String namespace = tagNamespace(tagId);
        int score = blockTag ? 200 : 500;

        if ("minecraft".equals(namespace) || "c".equals(namespace)) {
            score += 120;
        }

        if (PREFERRED_GROUPS.contains(path)) {
            score += 400;
        }

        for (String prefix : DEPRIORITIZED_PREFIXES) {
            if (path.startsWith(prefix)) {
                score -= 450;
            }
        }

        for (String fragment : DEPRIORITIZED_CONTAINS) {
            if (path.contains(fragment)) {
                score -= 275;
            }
        }

        score += Math.max(0, 64 - path.length());
        return score;
    }

    private static String tagNamespace(String tagId) {
        int separator = tagId.indexOf(':');
        return separator >= 0 ? tagId.substring(0, separator) : "minecraft";
    }

    private static String tagPath(String tagId) {
        int separator = tagId.indexOf(':');
        return separator >= 0 ? tagId.substring(separator + 1) : tagId;
    }

    private static <T> List<String> stringifyTags(Stream<TagKey<T>> tags) {
        return tags.map(tag -> tag.location().toString())
                .sorted(String::compareToIgnoreCase)
                .toList();
    }
}
