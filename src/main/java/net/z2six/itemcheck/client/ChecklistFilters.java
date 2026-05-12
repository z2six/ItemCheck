package net.z2six.itemcheck.client;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import net.z2six.itemcheck.ChecklistFilterAction;
import net.z2six.itemcheck.ChecklistFilterRule;
import net.z2six.itemcheck.ChecklistFilterTab;
import net.z2six.itemcheck.ChecklistFilterType;

public final class ChecklistFilters {
    private ChecklistFilters() {
    }

    public static boolean matchesTab(ChecklistCatalogEntry entry, ChecklistFilterTab tab) {
        if (!tab.explicitEntryIds().isEmpty()) {
            return tab.explicitEntryIds().contains(entry.entryId());
        }

        List<ChecklistFilterRule> includeRules = tab.filters().stream()
                .filter(rule -> rule.action() == ChecklistFilterAction.INCLUDE && !rule.expression().isBlank())
                .toList();
        if (includeRules.isEmpty()) {
            return false;
        }

        boolean included = includeRules.stream().anyMatch(rule -> matchesRule(entry, rule));
        if (!included) {
            return false;
        }

        return tab.filters().stream()
                .filter(rule -> rule.action() == ChecklistFilterAction.EXCLUDE && !rule.expression().isBlank())
                .noneMatch(rule -> matchesRule(entry, rule));
    }

    public static boolean survivesDuplicateFilter(ChecklistCatalogEntry entry, List<ChecklistFilterTab> tabs, int currentTabIndex) {
        if (currentTabIndex < 0 || currentTabIndex >= tabs.size()) {
            return true;
        }

        ChecklistFilterTab currentTab = tabs.get(currentTabIndex);
        if (!currentTab.noDuplicates()) {
            return true;
        }

        for (int index = 0; index < currentTabIndex; index++) {
            if (matchesTab(entry, tabs.get(index))) {
                return false;
            }
        }

        return true;
    }

    private static boolean matchesRule(ChecklistCatalogEntry entry, ChecklistFilterRule rule) {
        return switch (rule.type()) {
            case ITEM_NAME -> matchesValue(entry.displayNameLower(), rule.expression());
            case ITEM_ID -> matchesValue(entry.itemIdLower(), rule.expression());
            case ITEM_TAG -> matchesAny(entry.itemTags(), rule.expression());
            case BLOCK_TAG -> matchesAny(entry.blockTags(), rule.expression());
            case GROUP -> matchesValue(entry.groupLabelLower(), rule.expression());
        };
    }

    private static boolean matchesValue(String value, String expression) {
        List<Pattern> patterns = parsePatterns(expression);
        return patterns.isEmpty() || patterns.stream().anyMatch(pattern -> pattern.matcher(value).matches());
    }

    private static boolean matchesAny(List<String> values, String expression) {
        List<Pattern> patterns = parsePatterns(expression);
        return patterns.isEmpty() || values.stream()
                .map(String::toLowerCase)
                .anyMatch(value -> patterns.stream().anyMatch(pattern -> pattern.matcher(value).matches()));
    }

    private static List<Pattern> parsePatterns(String expression) {
        return Arrays.stream(expression.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(String::toLowerCase)
                .map(ChecklistFilters::compileWildcardPattern)
                .toList();
    }

    private static Pattern compileWildcardPattern(String token) {
        StringBuilder regex = new StringBuilder("^");
        for (int index = 0; index < token.length(); index++) {
            char character = token.charAt(index);
            if (character == '*') {
                regex.append(".*");
            } else {
                regex.append(Pattern.quote(String.valueOf(character)));
            }
        }
        regex.append('$');
        return Pattern.compile(regex.toString());
    }
}
