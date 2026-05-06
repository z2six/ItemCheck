package net.z2six.itemcheck;

public enum ChecklistFilterAction {
    INCLUDE,
    EXCLUDE;

    public ChecklistFilterAction next() {
        return this == INCLUDE ? EXCLUDE : INCLUDE;
    }
}
