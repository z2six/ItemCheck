package net.z2six.itemcheck.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.z2six.itemcheck.ChecklistFilterAction;
import net.z2six.itemcheck.ChecklistFilterRule;
import net.z2six.itemcheck.ChecklistFilterTab;
import net.z2six.itemcheck.ChecklistFilterType;
import net.z2six.itemcheck.ChecklistSortMode;
import net.z2six.itemcheck.ChecklistTabViewState;

public final class ItemChecklistScreen extends Screen {
    private static final int ROW_HEIGHT = 32;
    private static final int OUTER_MARGIN = 12;
    private static final int PANEL_GAP = 12;
    private static final int EDITOR_WIDTH = 340;
    private static final int INSTRUCTIONS_Y = 26;
    private static final int PROGRESS_Y = 42;
    private static final int SEARCH_Y = 62;
    private static final int TABS_Y = 94;
    private static final int TAB_HEIGHT = 20;
    private static final int TAB_GAP = 4;
    private static final int LIST_TOP = 174;
    private static final int FIELD_HEIGHT = 20;
    private static final int LABEL_COLOR = 0xFFE2E2E2;
    private static final int SORT_BUTTON_WIDTH = 116;
    private static final int STACKABLE_FILTER_WIDTH = 170;
    private static final int EDITOR_TITLE_Y = SEARCH_Y + 14;
    private static final int EDITOR_SUBTITLE_Y = SEARCH_Y + 30;
    private static final int EDITOR_BUTTONS_Y = SEARCH_Y + 64;
    private static final int EDITOR_REORDER_Y = SEARCH_Y + 92;
    private static final int TAB_NAME_LABEL_Y = SEARCH_Y + 126;
    private static final int TAB_NAME_BOX_Y = SEARCH_Y + 138;
    private static final int FILTERS_LABEL_Y = SEARCH_Y + 172;
    private static final int FILTERS_BUTTONS_Y = SEARCH_Y + 184;
    private static final int NO_DUPLICATES_Y = SEARCH_Y + 212;
    private static final int FILTER_LIST_TOP = SEARCH_Y + 240;
    private static final int FILTER_ROW_HEIGHT = 52;

    private static final Comparator<ChecklistCatalogEntry> GROUP_SORT = Comparator
            .comparing(ChecklistCatalogEntry::primarySortTag, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(ChecklistCatalogEntry::displayName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(entry -> entry.itemId().toString());
    private static final Comparator<ChecklistCatalogEntry> ALPHABETICAL_SORT = Comparator
            .comparing(ChecklistCatalogEntry::displayName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(entry -> entry.itemId().toString());

    private final List<ChecklistCatalogEntry> catalog = ChecklistCatalogEntry.createCatalog();
    private final List<Button> tabButtons = new ArrayList<>();

    private ChecklistList checklist;
    private EditBox searchBox;
    private EditBox tabNameBox;
    private Checkbox stackableOnlyCheckbox;
    private Checkbox noDuplicatesCheckbox;
    private Button saveTabButton;
    private Button deleteTabButton;
    private Button moveLeftButton;
    private Button moveRightButton;
    private Button sortModeButton;
    private Button addIncludeFilterButton;
    private Button addExcludeFilterButton;
    private FilterEditorList filterEditorList;
    private int selectedCustomTabIndex = -1;
    private boolean editorOpen;
    private List<ChecklistFilterTab> renderedTabs = List.of();
    private ChecklistTabViewState renderedAllTabViewState = ChecklistTabViewState.defaultState();
    private List<ChecklistCatalogEntry> orderedEntries = List.of();
    private List<ChecklistCatalogEntry> visibleEntries = List.of();

    public ItemChecklistScreen() {
        super(Component.translatable("itemcheck.screen.title"));
    }

    @Override
    protected void init() {
        ChecklistClientState.requestSync();
        this.renderedTabs = List.copyOf(ChecklistClientState.getFilterTabs());
        this.renderedAllTabViewState = ChecklistClientState.getAllTabViewState();

        int listWidth = this.getListWidth();
        this.searchBox = this.addRenderableWidget(this.createEditBox(
                OUTER_MARGIN,
                SEARCH_Y,
                listWidth - SORT_BUTTON_WIDTH - STACKABLE_FILTER_WIDTH - TAB_GAP * 2,
                Component.translatable("itemcheck.search.hint"),
                value -> this.refreshVisibleEntries()
        ));
        this.sortModeButton = this.addRenderableWidget(Button.builder(this.getSortButtonLabel(), button -> this.toggleSortMode())
                .bounds(this.getListRight() - SORT_BUTTON_WIDTH, SEARCH_Y, SORT_BUTTON_WIDTH, FIELD_HEIGHT)
                .build());
        this.createStackableOnlyCheckbox(this.getSelectedViewState().hideNonStackable());

        this.checklist = this.addRenderableWidget(new ChecklistList(this.minecraft, listWidth, this.height, LIST_TOP));
        this.initEditorWidgets();
        this.updateLayout();
        this.rebuildTabButtons();
        this.loadEditorFromSelection();
        this.refreshVisibleEntries();
    }

    @Override
    public void tick() {
        super.tick();
        List<ChecklistFilterTab> syncedTabs = ChecklistClientState.getFilterTabs();
        ChecklistTabViewState syncedAllTabViewState = ChecklistClientState.getAllTabViewState();
        if (!this.renderedTabs.equals(syncedTabs) || !this.renderedAllTabViewState.equals(syncedAllTabViewState)) {
            this.renderedTabs = List.copyOf(syncedTabs);
            this.renderedAllTabViewState = syncedAllTabViewState;
            if (this.selectedCustomTabIndex >= this.renderedTabs.size()) {
                this.selectedCustomTabIndex = this.renderedTabs.isEmpty() ? -1 : this.renderedTabs.size() - 1;
            }
            if (this.selectedCustomTabIndex < 0) {
                this.editorOpen = false;
            }
            this.updateLayout();
            this.rebuildTabButtons();
            this.loadEditorFromSelection();
            this.refreshVisibleEntries();
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(guiGraphics);
        guiGraphics.fill(0, 0, this.width, LIST_TOP - 10, 0xD0121212);
        guiGraphics.fill(0, LIST_TOP - 10, this.width, LIST_TOP - 9, 0xFF3A3A3A);

        if (this.editorOpen) {
            int editorX = this.getEditorX();
            guiGraphics.fill(editorX, SEARCH_Y, editorX + EDITOR_WIDTH, this.height - OUTER_MARGIN, 0x8A181818);
            guiGraphics.fill(editorX, SEARCH_Y, editorX + EDITOR_WIDTH, SEARCH_Y + 1, 0xFF4E4E4E);
            guiGraphics.fill(editorX, this.height - OUTER_MARGIN - 1, editorX + EDITOR_WIDTH, this.height - OUTER_MARGIN, 0xFF4E4E4E);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("itemcheck.screen.instructions"), OUTER_MARGIN, INSTRUCTIONS_Y, 0xD0D0D0, false);

        String searchQuery = this.getSearchQuery();
        Component progress = ChecklistClientState.isSynced()
                ? Component.translatable("itemcheck.screen.progress", ChecklistClientState.getCheckedCount(), this.catalog.size())
                : Component.translatable("itemcheck.screen.progress_syncing", ChecklistClientState.getCheckedCount(), this.catalog.size());
        guiGraphics.drawString(this.font, progress, OUTER_MARGIN, PROGRESS_Y, 0x9EF79E, false);
        if (!searchQuery.isBlank()) {
            guiGraphics.drawString(this.font, Component.translatable("itemcheck.search.active", searchQuery), OUTER_MARGIN + 220, PROGRESS_Y, 0xC6C6C6, false);
        }

        if (this.editorOpen) {
            this.renderEditorLabels(guiGraphics);
        }

        ChecklistList.ChecklistEntry hoveredEntry = this.checklist.getHoveredEntry();
        if (hoveredEntry != null) {
            guiGraphics.renderComponentTooltip(this.font, hoveredEntry.entry().buildTooltip(), mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Button tabButton : this.tabButtons) {
            if (tabButton.visible && tabButton.active && tabButton.isMouseOver(mouseX, mouseY)) {
                this.checklist.clearDragState();
                this.setFocused(null);
                return tabButton.mouseClicked(mouseX, mouseY, button);
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }

    private void initEditorWidgets() {
        int editorInnerX = this.getEditorX() + 16;
        int editorInnerWidth = EDITOR_WIDTH - 32;

        this.saveTabButton = this.addRenderableWidget(Button.builder(Component.translatable("itemcheck.tab.save"), button -> this.saveSelectedTab())
                .bounds(editorInnerX, EDITOR_BUTTONS_Y, 92, FIELD_HEIGHT)
                .build());
        this.deleteTabButton = this.addRenderableWidget(Button.builder(Component.translatable("itemcheck.tab.delete"), button -> this.deleteSelectedTab())
                .bounds(editorInnerX + 98, EDITOR_BUTTONS_Y, 92, FIELD_HEIGHT)
                .build());

        this.moveLeftButton = this.addRenderableWidget(Button.builder(Component.translatable("itemcheck.tab.move_left"), button -> this.moveSelectedTab(-1))
                .bounds(editorInnerX, EDITOR_REORDER_Y, 140, FIELD_HEIGHT)
                .build());
        this.moveRightButton = this.addRenderableWidget(Button.builder(Component.translatable("itemcheck.tab.move_right"), button -> this.moveSelectedTab(1))
                .bounds(editorInnerX + 148, EDITOR_REORDER_Y, 140, FIELD_HEIGHT)
                .build());

        this.tabNameBox = this.addRenderableWidget(this.createEditBox(editorInnerX, TAB_NAME_BOX_Y, editorInnerWidth, Component.translatable("itemcheck.editor.tab_name_hint"), value -> {
        }));
        this.addIncludeFilterButton = this.addRenderableWidget(Button.builder(Component.translatable("itemcheck.editor.add_include"), button -> this.filterEditorList.addRule(ChecklistFilterAction.INCLUDE))
                .bounds(editorInnerX, FILTERS_BUTTONS_Y, 138, FIELD_HEIGHT)
                .build());
        this.addExcludeFilterButton = this.addRenderableWidget(Button.builder(Component.translatable("itemcheck.editor.add_exclude"), button -> this.filterEditorList.addRule(ChecklistFilterAction.EXCLUDE))
                .bounds(editorInnerX + 146, FILTERS_BUTTONS_Y, 138, FIELD_HEIGHT)
                .build());
        this.noDuplicatesCheckbox = this.createNoDuplicatesCheckbox(editorInnerX, editorInnerWidth, false);
        this.filterEditorList = this.addRenderableWidget(new FilterEditorList(this.minecraft, editorInnerX, FILTER_LIST_TOP, editorInnerWidth, this.height - OUTER_MARGIN - FILTER_LIST_TOP));
    }

    private void renderEditorLabels(GuiGraphics guiGraphics) {
        int editorX = this.getEditorX() + 16;
        guiGraphics.drawString(this.font, Component.translatable("itemcheck.editor.title"), editorX, EDITOR_TITLE_Y, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, Component.translatable("itemcheck.editor.subtitle_dynamic"), editorX, EDITOR_SUBTITLE_Y, 0xC8C8C8, false);
        guiGraphics.drawString(this.font, Component.translatable("itemcheck.editor.tab_name"), editorX, TAB_NAME_LABEL_Y, LABEL_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("itemcheck.editor.filters"), editorX, FILTERS_LABEL_Y, LABEL_COLOR, false);
    }

    private EditBox createEditBox(int x, int y, int width, Component hint, java.util.function.Consumer<String> responder) {
        String hintText = hint.getString();
        EditBox editBox = new EditBox(this.font, x, y, width, FIELD_HEIGHT, Component.empty());
        editBox.setMaxLength(120);
        editBox.setSuggestion(hintText);
        editBox.setResponder(value -> {
            editBox.setSuggestion(value.isBlank() ? hintText : null);
            responder.accept(value);
        });
        return editBox;
    }

    private Checkbox createNoDuplicatesCheckbox(int x, int width, boolean selected) {
        if (this.noDuplicatesCheckbox != null) {
            this.removeWidget(this.noDuplicatesCheckbox);
        }

        this.noDuplicatesCheckbox = this.addRenderableWidget(Checkbox.builder(Component.translatable("itemcheck.editor.no_duplicates"), this.font)
                .pos(x, NO_DUPLICATES_Y)
                .maxWidth(width)
                .selected(selected)
                .build());
        return this.noDuplicatesCheckbox;
    }

    private Checkbox createStackableOnlyCheckbox(boolean selected) {
        if (this.stackableOnlyCheckbox != null) {
            this.removeWidget(this.stackableOnlyCheckbox);
        }

        this.stackableOnlyCheckbox = this.addRenderableWidget(Checkbox.builder(Component.translatable("itemcheck.filter.hide_non_stackable"), this.font)
                .pos(OUTER_MARGIN, SEARCH_Y)
                .maxWidth(STACKABLE_FILTER_WIDTH)
                .selected(selected)
                .onValueChange((checkbox, checked) -> this.setHideNonStackable(checked))
                .build());
        return this.stackableOnlyCheckbox;
    }

    private int getListWidth() {
        return this.getListRight() - OUTER_MARGIN;
    }

    private int getListRight() {
        return this.editorOpen ? this.getEditorX() - PANEL_GAP : this.width - OUTER_MARGIN;
    }

    private int getEditorX() {
        return this.width - EDITOR_WIDTH - OUTER_MARGIN;
    }

    private String getSearchQuery() {
        return this.searchBox == null ? "" : this.searchBox.getValue().trim().toLowerCase();
    }

    private Component getSortButtonLabel() {
        return this.getSelectedSortMode() == ChecklistSortMode.ALPHABETICAL
                ? Component.translatable("itemcheck.sort.a_to_z")
                : Component.translatable("itemcheck.sort.grouped");
    }

    private void toggleSortMode() {
        ChecklistTabViewState currentState = this.getSelectedViewState();
        this.persistSelectedViewState(new ChecklistTabViewState(currentState.sortMode().next(), List.of(), currentState.hideNonStackable()));
    }

    private void setHideNonStackable(boolean hideNonStackable) {
        ChecklistTabViewState currentState = this.getSelectedViewState();
        if (currentState.hideNonStackable() == hideNonStackable) {
            return;
        }

        this.persistSelectedViewState(new ChecklistTabViewState(currentState.sortMode(), currentState.manualOrder(), hideNonStackable));
    }

    private static Component getFilterActionLabel(ChecklistFilterAction action) {
        return Component.translatable(action == ChecklistFilterAction.INCLUDE ? "itemcheck.filter_action.include" : "itemcheck.filter_action.exclude");
    }

    private static Component getFilterTypeLabel(ChecklistFilterType type) {
        return switch (type) {
            case ITEM_NAME -> Component.translatable("itemcheck.filter_type.item_name");
            case ITEM_ID -> Component.translatable("itemcheck.filter_type.item_id");
            case ITEM_TAG -> Component.translatable("itemcheck.filter_type.item_tag");
            case BLOCK_TAG -> Component.translatable("itemcheck.filter_type.block_tag");
            case GROUP -> Component.translatable("itemcheck.filter_type.group");
        };
    }

    private static Component getFilterTypeHint(ChecklistFilterType type) {
        return switch (type) {
            case ITEM_NAME -> Component.translatable("itemcheck.editor.name_filter_hint");
            case ITEM_ID -> Component.translatable("itemcheck.editor.item_id_filter_hint");
            case ITEM_TAG -> Component.translatable("itemcheck.editor.item_tag_filter_hint");
            case BLOCK_TAG -> Component.translatable("itemcheck.editor.block_tag_filter_hint");
            case GROUP -> Component.translatable("itemcheck.editor.group_filter_hint");
        };
    }

    private void rebuildTabButtons() {
        for (Button tabButton : this.tabButtons) {
            this.removeWidget(tabButton);
        }
        this.tabButtons.clear();

        int left = OUTER_MARGIN;
        int right = this.getListRight();
        int x = left;
        int y = TABS_Y;

        List<Component> labels = new ArrayList<>();
        labels.add(Component.translatable("itemcheck.tab.all"));
        this.renderedTabs.forEach(tab -> labels.add(Component.literal(tab.name())));
        labels.add(Component.translatable("itemcheck.tab.new"));

        for (int index = 0; index < labels.size(); index++) {
            Component label = labels.get(index);
            boolean isNewButton = index == labels.size() - 1;
            boolean selected = !isNewButton && index - 1 == this.selectedCustomTabIndex;
            Component displayLabel = selected ? Component.literal("[" + label.getString() + "]") : label;
            int buttonWidth = Math.max(56, Math.min(110, this.font.width(displayLabel) + 20));
            if (x + buttonWidth > right) {
                x = left;
                y += TAB_HEIGHT + TAB_GAP;
            }

            final int customIndex = index - 1;
            Button button = this.addRenderableWidget(Button.builder(displayLabel, pressed -> {
                        if (isNewButton) {
                            this.createNewTab();
                        } else {
                            this.selectTab(customIndex);
                        }
                    })
                    .bounds(x, y, buttonWidth, TAB_HEIGHT)
                    .build(builder -> new Button(builder) {
                        @Override
                        public boolean mouseClicked(double mouseX, double mouseY, int button) {
                            if (button == 1 && this.visible && this.active && this.isMouseOver(mouseX, mouseY) && customIndex >= 0) {
                                ItemChecklistScreen.this.openEditorForTab(customIndex);
                                return true;
                            }

                            return super.mouseClicked(mouseX, mouseY, button);
                        }
                    }));
            this.tabButtons.add(button);
            x += buttonWidth + TAB_GAP;
        }
    }

    private void selectTab(int customIndex) {
        this.checklist.clearDragState();
        this.setFocused(null);
        this.selectedCustomTabIndex = customIndex;
        if (customIndex < 0) {
            this.editorOpen = false;
            this.updateLayout();
        }
        this.rebuildTabButtons();
        this.loadEditorFromSelection();
        this.refreshVisibleEntries();
    }

    private void openEditorForTab(int customIndex) {
        this.checklist.clearDragState();
        this.setFocused(null);
        this.selectedCustomTabIndex = customIndex;
        this.editorOpen = customIndex >= 0;
        this.updateLayout();
        this.rebuildTabButtons();
        this.loadEditorFromSelection();
        this.refreshVisibleEntries();
    }

    private void loadEditorFromSelection() {
        if (this.selectedCustomTabIndex >= 0 && this.selectedCustomTabIndex < this.renderedTabs.size()) {
            ChecklistFilterTab tab = this.renderedTabs.get(this.selectedCustomTabIndex);
            this.tabNameBox.setValue(tab.name());
            this.createNoDuplicatesCheckbox(this.getEditorX() + 16, EDITOR_WIDTH - 32, tab.noDuplicates());
            this.filterEditorList.setRules(tab.filters());
            this.setEditorEnabled(true);
        } else {
            this.tabNameBox.setValue("");
            this.createNoDuplicatesCheckbox(this.getEditorX() + 16, EDITOR_WIDTH - 32, false);
            this.filterEditorList.setRules(List.of());
            this.setEditorEnabled(false);
        }

        this.updateEditorButtons();
        this.updateLayout();
    }

    private void setEditorEnabled(boolean enabled) {
        this.tabNameBox.setEditable(enabled);
        this.tabNameBox.active = enabled;
        this.noDuplicatesCheckbox.active = enabled;
        this.addIncludeFilterButton.active = enabled;
        this.addExcludeFilterButton.active = enabled;
        this.filterEditorList.setEditorActive(enabled);
    }

    private void updateEditorButtons() {
        boolean hasSelection = this.selectedCustomTabIndex >= 0 && this.selectedCustomTabIndex < this.renderedTabs.size();
        this.saveTabButton.active = hasSelection;
        this.deleteTabButton.active = hasSelection;
        this.moveLeftButton.active = hasSelection && this.selectedCustomTabIndex > 0;
        this.moveRightButton.active = hasSelection && this.selectedCustomTabIndex < this.renderedTabs.size() - 1;
    }

    private void refreshVisibleEntries() {
        String searchQuery = this.getSearchQuery();
        List<ChecklistFilterTab> tabs = ChecklistClientState.getFilterTabs();
        ChecklistTabViewState selectedViewState = this.getSelectedViewState(tabs, ChecklistClientState.getAllTabViewState());
        this.orderedEntries = this.applyOrdering(this.catalog.stream()
                .filter(entry -> !selectedViewState.hideNonStackable() || entry.maxStackSize() > 1)
                .filter(entry -> this.matchesCurrentTab(entry, tabs))
                .toList(), selectedViewState);
        this.visibleEntries = this.orderedEntries.stream()
                .filter(entry -> entry.matchesSearch(searchQuery))
                .toList();
        this.sortModeButton.setMessage(this.getSortButtonLabel());
        if (this.stackableOnlyCheckbox.selected() != selectedViewState.hideNonStackable()) {
            this.createStackableOnlyCheckbox(selectedViewState.hideNonStackable());
        }
        this.checklist.setCatalogEntries(this.visibleEntries);
    }

    private boolean matchesCurrentTab(ChecklistCatalogEntry entry, List<ChecklistFilterTab> tabs) {
        if (this.selectedCustomTabIndex < 0 || this.selectedCustomTabIndex >= tabs.size()) {
            return true;
        }

        ChecklistFilterTab tab = tabs.get(this.selectedCustomTabIndex);
        return ChecklistFilters.matchesTab(entry, tab)
                && ChecklistFilters.survivesDuplicateFilter(entry, tabs, this.selectedCustomTabIndex);
    }

    private void createNewTab() {
        List<ChecklistFilterTab> tabs = new ArrayList<>(ChecklistClientState.getFilterTabs());
        tabs.add(ChecklistFilterTab.blank());
        this.editorOpen = true;
        this.persistTabs(tabs, tabs.size() - 1);
    }

    private void saveSelectedTab() {
        if (this.selectedCustomTabIndex < 0 || this.selectedCustomTabIndex >= this.renderedTabs.size()) {
            return;
        }

        List<ChecklistFilterTab> tabs = new ArrayList<>(ChecklistClientState.getFilterTabs());
        tabs.set(this.selectedCustomTabIndex, this.readTabFromEditor());
        this.editorOpen = false;
        this.persistTabs(tabs, this.selectedCustomTabIndex);
    }

    private void deleteSelectedTab() {
        if (this.selectedCustomTabIndex < 0 || this.selectedCustomTabIndex >= this.renderedTabs.size()) {
            return;
        }

        List<ChecklistFilterTab> tabs = new ArrayList<>(ChecklistClientState.getFilterTabs());
        tabs.remove(this.selectedCustomTabIndex);
        int newIndex = tabs.isEmpty() ? -1 : Math.min(this.selectedCustomTabIndex, tabs.size() - 1);
        this.editorOpen = newIndex >= 0 && this.editorOpen;
        this.persistTabs(tabs, newIndex);
    }

    private void moveSelectedTab(int delta) {
        if (this.selectedCustomTabIndex < 0 || this.selectedCustomTabIndex >= this.renderedTabs.size()) {
            return;
        }

        int targetIndex = this.selectedCustomTabIndex + delta;
        if (targetIndex < 0 || targetIndex >= this.renderedTabs.size()) {
            return;
        }

        List<ChecklistFilterTab> tabs = new ArrayList<>(ChecklistClientState.getFilterTabs());
        ChecklistFilterTab movedTab = tabs.remove(this.selectedCustomTabIndex);
        tabs.add(targetIndex, movedTab);
        this.persistTabs(tabs, targetIndex);
    }

    private ChecklistFilterTab readTabFromEditor() {
        ChecklistTabViewState viewState = this.selectedCustomTabIndex >= 0 && this.selectedCustomTabIndex < this.renderedTabs.size()
                ? this.renderedTabs.get(this.selectedCustomTabIndex).viewState()
                : ChecklistTabViewState.defaultState();
        return new ChecklistFilterTab(
                this.tabNameBox.getValue(),
                this.filterEditorList.readRules(),
                this.noDuplicatesCheckbox.selected(),
                viewState
        );
    }

    private void persistTabs(List<ChecklistFilterTab> tabs, int newSelectedCustomTabIndex) {
        this.persistState(ChecklistClientState.getAllTabViewState(), tabs, newSelectedCustomTabIndex);
    }

    private void persistSelectedViewState(ChecklistTabViewState updatedViewState) {
        ChecklistTabViewState allTabViewState = ChecklistClientState.getAllTabViewState();
        List<ChecklistFilterTab> tabs = new ArrayList<>(ChecklistClientState.getFilterTabs());
        if (this.selectedCustomTabIndex >= 0 && this.selectedCustomTabIndex < tabs.size()) {
            ChecklistFilterTab currentTab = tabs.get(this.selectedCustomTabIndex);
            tabs.set(this.selectedCustomTabIndex, new ChecklistFilterTab(currentTab.name(), currentTab.filters(), currentTab.noDuplicates(), updatedViewState));
        } else {
            allTabViewState = updatedViewState;
        }
        this.persistState(allTabViewState, tabs, this.selectedCustomTabIndex);
    }

    private void persistState(ChecklistTabViewState allTabViewState, List<ChecklistFilterTab> tabs, int newSelectedCustomTabIndex) {
        ChecklistClientState.saveFilterTabs(allTabViewState, tabs);
        this.renderedTabs = List.copyOf(tabs);
        this.renderedAllTabViewState = allTabViewState;
        this.selectedCustomTabIndex = newSelectedCustomTabIndex;
        if (this.selectedCustomTabIndex < 0) {
            this.editorOpen = false;
        }
        this.updateLayout();
        this.rebuildTabButtons();
        this.loadEditorFromSelection();
        this.refreshVisibleEntries();
    }

    private ChecklistSortMode getSelectedSortMode() {
        return this.getSelectedViewState().sortMode();
    }

    private ChecklistTabViewState getSelectedViewState() {
        return this.getSelectedViewState(this.renderedTabs, this.renderedAllTabViewState);
    }

    private ChecklistTabViewState getSelectedViewState(List<ChecklistFilterTab> tabs, ChecklistTabViewState allTabViewState) {
        if (this.selectedCustomTabIndex < 0 || this.selectedCustomTabIndex >= tabs.size()) {
            return allTabViewState;
        }
        return tabs.get(this.selectedCustomTabIndex).viewState();
    }

    private List<ChecklistCatalogEntry> applyOrdering(List<ChecklistCatalogEntry> entries, ChecklistTabViewState viewState) {
        Comparator<ChecklistCatalogEntry> baseComparator = viewState.sortMode() == ChecklistSortMode.ALPHABETICAL ? ALPHABETICAL_SORT : GROUP_SORT;
        List<ChecklistCatalogEntry> baseOrderedEntries = entries.stream()
                .sorted(baseComparator)
                .toList();
        if (viewState.manualOrder().isEmpty()) {
            return baseOrderedEntries;
        }

        Map<ResourceLocation, Integer> manualPositions = new HashMap<>();
        List<ResourceLocation> manualOrder = viewState.manualOrder();
        for (int index = 0; index < manualOrder.size(); index++) {
            manualPositions.putIfAbsent(manualOrder.get(index), index);
        }

        return baseOrderedEntries.stream()
                .sorted(Comparator.comparingInt((ChecklistCatalogEntry entry) -> manualPositions.getOrDefault(entry.itemId(), Integer.MAX_VALUE))
                        .thenComparing(baseComparator))
                .toList();
    }

    private void applyManualReorder(ResourceLocation draggedItemId, int targetVisibleIndex) {
        if (this.visibleEntries.size() < 2 || this.orderedEntries.isEmpty()) {
            return;
        }

        List<ResourceLocation> visibleIds = this.visibleEntries.stream()
                .map(ChecklistCatalogEntry::itemId)
                .toList();
        int fromVisibleIndex = visibleIds.indexOf(draggedItemId);
        if (fromVisibleIndex < 0) {
            return;
        }

        int clampedTargetIndex = Math.max(0, Math.min(targetVisibleIndex, visibleIds.size()));
        if (clampedTargetIndex > fromVisibleIndex) {
            clampedTargetIndex--;
        }
        if (clampedTargetIndex == fromVisibleIndex) {
            return;
        }

        List<ResourceLocation> reorderedVisibleIds = new ArrayList<>(visibleIds);
        reorderedVisibleIds.remove(fromVisibleIndex);
        reorderedVisibleIds.add(clampedTargetIndex, draggedItemId);

        Set<ResourceLocation> visibleSet = new HashSet<>(visibleIds);
        Iterator<ResourceLocation> reorderedVisibleIterator = reorderedVisibleIds.iterator();
        List<ResourceLocation> reorderedFullOrder = new ArrayList<>(this.orderedEntries.size());
        for (ChecklistCatalogEntry entry : this.orderedEntries) {
            if (visibleSet.contains(entry.itemId())) {
                reorderedFullOrder.add(reorderedVisibleIterator.next());
            } else {
                reorderedFullOrder.add(entry.itemId());
            }
        }

        ChecklistTabViewState currentState = this.getSelectedViewState(List.copyOf(ChecklistClientState.getFilterTabs()), ChecklistClientState.getAllTabViewState());
        this.persistSelectedViewState(new ChecklistTabViewState(currentState.sortMode(), reorderedFullOrder, currentState.hideNonStackable()));
    }

    private void updateLayout() {
        int listWidth = this.getListWidth();
        int listRight = this.getListRight();
        int stackableFilterX = listRight - SORT_BUTTON_WIDTH - STACKABLE_FILTER_WIDTH - TAB_GAP;
        this.searchBox.setRectangle(Math.max(80, stackableFilterX - OUTER_MARGIN - TAB_GAP), FIELD_HEIGHT, OUTER_MARGIN, SEARCH_Y);
        this.stackableOnlyCheckbox.setPosition(stackableFilterX, SEARCH_Y);
        this.sortModeButton.setRectangle(SORT_BUTTON_WIDTH, FIELD_HEIGHT, listRight - SORT_BUTTON_WIDTH, SEARCH_Y);
        this.checklist.setRectangle(listWidth, this.height - LIST_TOP - OUTER_MARGIN, OUTER_MARGIN, LIST_TOP);
        this.layoutEditorWidgets();
    }

    private void layoutEditorWidgets() {
        int editorInnerX = this.getEditorX() + 16;
        int editorInnerWidth = EDITOR_WIDTH - 32;
        boolean visible = this.editorOpen;

        this.saveTabButton.setRectangle(92, FIELD_HEIGHT, editorInnerX, EDITOR_BUTTONS_Y);
        this.saveTabButton.visible = visible;
        this.deleteTabButton.setRectangle(92, FIELD_HEIGHT, editorInnerX + 98, EDITOR_BUTTONS_Y);
        this.deleteTabButton.visible = visible;
        this.moveLeftButton.setRectangle(140, FIELD_HEIGHT, editorInnerX, EDITOR_REORDER_Y);
        this.moveLeftButton.visible = visible;
        this.moveRightButton.setRectangle(140, FIELD_HEIGHT, editorInnerX + 148, EDITOR_REORDER_Y);
        this.moveRightButton.visible = visible;
        this.tabNameBox.setRectangle(editorInnerWidth, FIELD_HEIGHT, editorInnerX, TAB_NAME_BOX_Y);
        this.tabNameBox.visible = visible;
        this.addIncludeFilterButton.setRectangle(138, FIELD_HEIGHT, editorInnerX, FILTERS_BUTTONS_Y);
        this.addIncludeFilterButton.visible = visible;
        this.addExcludeFilterButton.setRectangle(138, FIELD_HEIGHT, editorInnerX + 146, FILTERS_BUTTONS_Y);
        this.addExcludeFilterButton.visible = visible;
        this.noDuplicatesCheckbox.setPosition(editorInnerX, NO_DUPLICATES_Y);
        this.noDuplicatesCheckbox.visible = visible;
        this.filterEditorList.setRectangle(editorInnerWidth, this.height - OUTER_MARGIN - FILTER_LIST_TOP, editorInnerX, FILTER_LIST_TOP);
        this.filterEditorList.visible = visible;
        this.filterEditorList.active = visible;
    }

    private final class ChecklistList extends ObjectSelectionList<ChecklistList.ChecklistEntry> {
        private final Minecraft minecraft;
        private ChecklistEntry pressedEntry;
        private ChecklistEntry draggedEntry;
        private int dragInsertIndex = -1;
        private double pressedMouseX;
        private double pressedMouseY;
        private double dragMouseX;
        private double dragMouseY;

        private ChecklistList(Minecraft minecraft, int listWidth, int screenHeight, int top) {
            super(minecraft, listWidth, screenHeight - top - OUTER_MARGIN, top, ROW_HEIGHT);
            this.minecraft = minecraft;
            this.setPosition(OUTER_MARGIN, top);
        }

        @Override
        public int getRowWidth() {
            return this.width - 14;
        }

        @Override
        protected boolean isValidMouseClick(int button) {
            return button == 0 || button == 1;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRight() - 6;
        }

        private void setCatalogEntries(List<ChecklistCatalogEntry> entries) {
            this.clearDragState();
            this.replaceEntries(entries.stream().map(ChecklistEntry::new).toList());
            this.setScrollAmount(0.0);
        }

        private ChecklistEntry getHoveredEntry() {
            return this.getHovered();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            ChecklistEntry entry = this.getEntryAtPosition(mouseX, mouseY);
            if (entry != null) {
                if (button == 1) {
                    ChecklistClientState.toggle(entry.entry().itemId());
                    return true;
                }
                if (button == 0) {
                    this.setSelected(entry);
                    this.setFocused(entry);
                    this.pressedEntry = entry;
                    this.draggedEntry = null;
                    this.dragInsertIndex = -1;
                    this.pressedMouseX = mouseX;
                    this.pressedMouseY = mouseY;
                    this.dragMouseX = mouseX;
                    this.dragMouseY = mouseY;
                    return true;
                }
            }

            this.clearDragState();
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (button != 0 || this.pressedEntry == null) {
                return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            }

            this.dragMouseX = mouseX;
            this.dragMouseY = mouseY;
            if (this.draggedEntry == null
                    && Math.abs(mouseX - this.pressedMouseX) < 3.0
                    && Math.abs(mouseY - this.pressedMouseY) < 3.0) {
                return true;
            }

            this.draggedEntry = this.pressedEntry;
            this.autoScrollWhileDragging(mouseY);
            this.dragInsertIndex = this.calculateDropIndex(mouseY);
            return true;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (button == 0 && this.pressedEntry != null) {
                ResourceLocation draggedItemId = this.draggedEntry == null ? null : this.draggedEntry.entry().itemId();
                int targetIndex = this.dragInsertIndex;
                this.clearDragState();
                if (draggedItemId != null) {
                    ItemChecklistScreen.this.applyManualReorder(draggedItemId, targetIndex);
                }
                return true;
            }

            return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            if (this.draggedEntry != null) {
                this.renderDropIndicator(guiGraphics);
                this.renderDragPreview(guiGraphics);
            }
        }

        private void clearDragState() {
            this.pressedEntry = null;
            this.draggedEntry = null;
            this.dragInsertIndex = -1;
        }

        private void autoScrollWhileDragging(double mouseY) {
            int threshold = 18;
            if (mouseY < this.getY() + threshold) {
                this.setScrollAmount(Math.max(0.0, this.getScrollAmount() - 8.0));
            } else if (mouseY > this.getBottom() - threshold) {
                this.setScrollAmount(this.getScrollAmount() + 8.0);
            }
        }

        private int calculateDropIndex(double mouseY) {
            for (int index = 0; index < this.getItemCount(); index++) {
                int midpoint = this.getRowTop(index) + ROW_HEIGHT / 2;
                if (mouseY < midpoint) {
                    return index;
                }
            }
            return this.getItemCount();
        }

        private void renderDropIndicator(GuiGraphics guiGraphics) {
            if (this.dragInsertIndex < 0) {
                return;
            }

            int indicatorY;
            if (this.getItemCount() == 0) {
                indicatorY = this.getY() + 2;
            } else if (this.dragInsertIndex <= 0) {
                indicatorY = this.getRowTop(0);
            } else if (this.dragInsertIndex >= this.getItemCount()) {
                indicatorY = this.getRowBottom(this.getItemCount() - 1);
            } else {
                indicatorY = this.getRowTop(this.dragInsertIndex);
            }

            int left = this.getRowLeft();
            int right = this.getRowRight();
            guiGraphics.fill(left, indicatorY - 1, right, indicatorY + 1, 0xFF7DD3FC);
        }

        private void renderDragPreview(GuiGraphics guiGraphics) {
            int rowLeft = this.getRowLeft();
            int rowWidth = this.getRowWidth();
            int previewTop = Math.max(this.getY(), Math.min((int) this.dragMouseY - ROW_HEIGHT / 2, this.getBottom() - ROW_HEIGHT));
            this.renderRowBackground(guiGraphics, this.draggedEntry.entry(), previewTop, rowLeft, rowWidth, true, true);
            this.renderRowContents(guiGraphics, this.draggedEntry.entry(), previewTop, rowLeft, rowWidth);
        }

        private void renderRowBackground(GuiGraphics guiGraphics, ChecklistCatalogEntry entry, int top, int left, int width, boolean hovered, boolean dragging) {
            boolean checked = ChecklistClientState.isChecked(entry.itemId());
            int backgroundColor;
            int accentColor;
            if (dragging) {
                backgroundColor = checked ? 0xCC3E6E9E : 0xAA29445E;
                accentColor = 0xFF9BD8FF;
            } else {
                backgroundColor = checked ? (hovered ? 0xCC4D8F4D : 0xB53A743A) : (hovered ? 0xAA303030 : 0x881C1C1C);
                accentColor = checked ? 0xFFB8FFB8 : 0xFF6C6C6C;
            }

            guiGraphics.fill(left, top, left + width, top + ROW_HEIGHT - 1, backgroundColor);
            guiGraphics.fill(left, top, left + 3, top + ROW_HEIGHT - 1, accentColor);
        }

        private void renderRowContents(GuiGraphics guiGraphics, ChecklistCatalogEntry entry, int top, int left, int width) {
            boolean checked = ChecklistClientState.isChecked(entry.itemId());
            int nameColor = checked ? 0xF2FFF2 : 0xFFFFFF;
            int metaColor = checked ? 0xD6F7D6 : 0xA8A8A8;
            int itemX = left + 8;
            int itemY = top + 8;
            int textX = left + 30;
            String stackLabel = "x" + entry.maxStackSize();
            int stackWidth = this.minecraft.font.width(stackLabel);
            int maxNameWidth = Math.max(40, width - 52 - stackWidth);
            String displayName = checked ? "\u2713 " + entry.displayName() : entry.displayName();
            String visibleName = this.minecraft.font.plainSubstrByWidth(displayName, maxNameWidth);
            String visibleMeta = this.minecraft.font.plainSubstrByWidth(entry.metadataLine(), width - 40);

            guiGraphics.renderItem(entry.stack(), itemX, itemY);
            guiGraphics.drawString(this.minecraft.font, visibleName, textX, top + 6, nameColor, false);
            guiGraphics.drawString(this.minecraft.font, visibleMeta, textX, top + 18, metaColor, false);
            guiGraphics.drawString(this.minecraft.font, stackLabel, left + width - stackWidth - 10, top + 10, nameColor, false);
        }

        private final class ChecklistEntry extends ObjectSelectionList.Entry<ChecklistEntry> {
            private final ChecklistCatalogEntry entry;

            private ChecklistEntry(ChecklistCatalogEntry entry) {
                this.entry = entry;
            }

            private ChecklistCatalogEntry entry() {
                return this.entry;
            }

            @Override
            public void renderBack(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
                ChecklistList.this.renderRowBackground(guiGraphics, this.entry, top, left, width, hovered, this == ChecklistList.this.draggedEntry);
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
                ChecklistList.this.renderRowContents(guiGraphics, this.entry, top, left, width);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return button == 0 || button == 1;
            }

            @Override
            public Component getNarration() {
                return ChecklistClientState.isChecked(this.entry.itemId())
                        ? Component.translatable("itemcheck.screen.narration.checked", this.entry.displayName())
                        : Component.translatable("itemcheck.screen.narration.unchecked", this.entry.displayName());
            }
        }
    }

    private final class FilterEditorList extends ContainerObjectSelectionList<FilterEditorList.FilterRuleEntry> {
        private boolean editorActive;

        private FilterEditorList(Minecraft minecraft, int x, int y, int width, int height) {
            super(minecraft, width, height, y, FILTER_ROW_HEIGHT);
            this.setPosition(x, y);
        }

        @Override
        public int getRowWidth() {
            return this.width - 14;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRight() - 6;
        }

        @Override
        protected void renderListBackground(GuiGraphics guiGraphics) {
            guiGraphics.fill(this.getX(), this.getY(), this.getRight(), this.getBottom(), 0x4A101010);
        }

        private void setEditorActive(boolean active) {
            this.editorActive = active;
            this.active = active;
            this.children().forEach(entry -> entry.setActive(active));
        }

        private void setRules(List<ChecklistFilterRule> rules) {
            this.replaceEntries(rules.stream().map(FilterRuleEntry::new).toList());
            this.children().forEach(entry -> entry.setActive(this.editorActive));
            this.setScrollAmount(0.0);
        }

        private List<ChecklistFilterRule> readRules() {
            return this.children().stream()
                    .map(FilterRuleEntry::toRule)
                    .filter(rule -> !rule.expression().isBlank())
                    .toList();
        }

        private void addRule(ChecklistFilterAction action) {
            if (!this.editorActive) {
                return;
            }

            FilterRuleEntry entry = new FilterRuleEntry(new ChecklistFilterRule(action, ChecklistFilterType.ITEM_NAME, ""));
            entry.setActive(true);
            this.addEntry(entry);
            this.ensureVisible(entry);
        }

        private void removeRule(FilterRuleEntry entry) {
            this.removeEntry(entry);
        }

        private final class FilterRuleEntry extends ContainerObjectSelectionList.Entry<FilterRuleEntry> {
            private static final int ACTION_WIDTH = 72;
            private static final int TYPE_WIDTH = 88;
            private static final int REMOVE_WIDTH = 22;
            private static final int GAP = 4;
            private static final int ROW_PADDING = 4;

            private final Button actionButton;
            private final Button typeButton;
            private final EditBox expressionBox;
            private final Button removeButton;
            private final List<GuiEventListener> children;
            private final List<NarratableEntry> narratables;
            private ChecklistFilterAction action;
            private ChecklistFilterType type;

            private FilterRuleEntry(ChecklistFilterRule rule) {
                this.action = rule.action();
                this.type = rule.type();
                this.actionButton = Button.builder(getFilterActionLabel(this.action), button -> this.cycleAction())
                        .bounds(0, 0, ACTION_WIDTH, FIELD_HEIGHT)
                        .build();
                this.typeButton = Button.builder(getFilterTypeLabel(this.type), button -> this.cycleType())
                        .bounds(0, 0, TYPE_WIDTH, FIELD_HEIGHT)
                        .build();
                this.expressionBox = createEditBox(0, 0, 100, getFilterTypeHint(this.type), value -> {
                });
                this.expressionBox.setValue(rule.expression());
                this.removeButton = Button.builder(Component.literal("x"), button -> FilterEditorList.this.removeRule(this))
                        .bounds(0, 0, REMOVE_WIDTH, FIELD_HEIGHT)
                        .build();
                this.children = List.of(this.actionButton, this.typeButton, this.expressionBox, this.removeButton);
                this.narratables = List.of(this.actionButton, this.typeButton, this.expressionBox, this.removeButton);
                this.updateSuggestion();
            }

            private void setActive(boolean active) {
                this.actionButton.active = active;
                this.typeButton.active = active;
                this.expressionBox.active = active;
                this.expressionBox.setEditable(active);
                this.removeButton.active = active;
            }

            private ChecklistFilterRule toRule() {
                return new ChecklistFilterRule(this.action, this.type, this.expressionBox.getValue().toLowerCase());
            }

            private void updateSuggestion() {
                String hint = getFilterTypeHint(this.type).getString();
                this.expressionBox.setSuggestion(this.expressionBox.getValue().isBlank() ? hint : null);
            }

            private void cycleAction() {
                this.action = this.action.next();
                this.actionButton.setMessage(getFilterActionLabel(this.action));
            }

            private void cycleType() {
                this.type = this.type.next();
                this.typeButton.setMessage(getFilterTypeLabel(this.type));
                this.updateSuggestion();
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return this.children;
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return this.narratables;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
                int backgroundColor = hovered ? 0x66383838 : 0x44242424;
                guiGraphics.fill(left, top, left + width, top + height - 1, backgroundColor);

                int buttonY = top + ROW_PADDING;
                int x = left + ROW_PADDING;

                this.actionButton.setX(x);
                this.actionButton.setY(buttonY);
                x += ACTION_WIDTH + GAP;

                this.typeButton.setX(x);
                this.typeButton.setY(buttonY);

                this.removeButton.setX(left + width - ROW_PADDING - REMOVE_WIDTH);
                this.removeButton.setY(buttonY);

                this.expressionBox.setX(left + ROW_PADDING);
                this.expressionBox.setY(top + ROW_PADDING + FIELD_HEIGHT + GAP);
                this.expressionBox.setWidth(Math.max(40, width - ROW_PADDING * 2));

                this.actionButton.render(guiGraphics, mouseX, mouseY, partialTick);
                this.typeButton.render(guiGraphics, mouseX, mouseY, partialTick);
                this.expressionBox.render(guiGraphics, mouseX, mouseY, partialTick);
                this.removeButton.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
    }
}
