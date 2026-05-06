package net.z2six.itemcheck;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;

public record ChecklistFilterRule(ChecklistFilterAction action, ChecklistFilterType type, String expression) {
    private static final int MAX_FIELD_LENGTH = 120;
    private static final String ACTION_KEY = "action";
    private static final String TYPE_KEY = "type";
    private static final String EXPRESSION_KEY = "expression";

    public ChecklistFilterRule {
        action = action == null ? ChecklistFilterAction.INCLUDE : action;
        type = type == null ? ChecklistFilterType.ITEM_NAME : type;
        expression = sanitizeExpression(expression);
    }

    public static ChecklistFilterRule read(RegistryFriendlyByteBuf buffer) {
        return new ChecklistFilterRule(
                ChecklistFilterAction.values()[buffer.readVarInt()],
                ChecklistFilterType.values()[buffer.readVarInt()],
                buffer.readUtf(MAX_FIELD_LENGTH)
        );
    }

    public static ChecklistFilterRule fromTag(CompoundTag tag) {
        return new ChecklistFilterRule(
                readAction(tag.getString(ACTION_KEY)),
                readType(tag.getString(TYPE_KEY)),
                tag.getString(EXPRESSION_KEY)
        );
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(this.action.ordinal());
        buffer.writeVarInt(this.type.ordinal());
        buffer.writeUtf(this.expression, MAX_FIELD_LENGTH);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString(ACTION_KEY, this.action.name());
        tag.putString(TYPE_KEY, this.type.name());
        tag.putString(EXPRESSION_KEY, this.expression);
        return tag;
    }

    private static ChecklistFilterAction readAction(String value) {
        if ("CANCEL".equals(value)) {
            return ChecklistFilterAction.EXCLUDE;
        }

        try {
            return ChecklistFilterAction.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return ChecklistFilterAction.INCLUDE;
        }
    }

    private static ChecklistFilterType readType(String value) {
        try {
            return ChecklistFilterType.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return ChecklistFilterType.ITEM_NAME;
        }
    }

    private static String sanitizeExpression(String value) {
        if (value == null) {
            return "";
        }

        String trimmed = value.trim();
        return trimmed.length() > MAX_FIELD_LENGTH ? trimmed.substring(0, MAX_FIELD_LENGTH) : trimmed;
    }
}
