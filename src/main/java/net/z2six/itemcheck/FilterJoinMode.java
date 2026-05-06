package net.z2six.itemcheck;

public enum FilterJoinMode {
    AND,
    OR;

    public FilterJoinMode next() {
        return this == AND ? OR : AND;
    }
}
