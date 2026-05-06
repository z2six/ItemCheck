package net.z2six.itemcheck;

public enum ChecklistFilterType {
    ITEM_NAME,
    ITEM_ID,
    ITEM_TAG,
    BLOCK_TAG,
    GROUP;

    public ChecklistFilterType next() {
        ChecklistFilterType[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
