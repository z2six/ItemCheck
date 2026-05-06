package net.z2six.itemcheck;

public enum ChecklistSortMode {
    GROUP,
    ALPHABETICAL;

    public ChecklistSortMode next() {
        return this == GROUP ? ALPHABETICAL : GROUP;
    }
}
