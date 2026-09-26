package io.josemmo.bukkit.plugin.addon.imgui.editor;

public final class EditorFinishPlan {
    public enum Action {
        DELIVER_RESULT,
        DELIVER_ORIGINAL
    }

    private final EditorState state;
    private final Action action;
    private final DeliveryMode delivery;

    public EditorFinishPlan(EditorState state, Action action, DeliveryMode delivery) {
        this.state = state;
        this.action = action;
        this.delivery = delivery;
    }

    public static EditorFinishPlan confirm(boolean resizeSucceeded, boolean hasSpace) {
        if (!resizeSucceeded) {
            return new EditorFinishPlan(EditorState.RECOVERED, Action.DELIVER_ORIGINAL, DeliveryMode.forSpace(hasSpace));
        }
        return new EditorFinishPlan(EditorState.CONFIRMED, Action.DELIVER_RESULT, DeliveryMode.forSpace(hasSpace));
    }

    public static EditorFinishPlan cancel(boolean hasSpace) {
        return new EditorFinishPlan(EditorState.CANCELLED, Action.DELIVER_ORIGINAL, DeliveryMode.forSpace(hasSpace));
    }

    public static EditorFinishPlan recover(boolean hasSpace) {
        return new EditorFinishPlan(EditorState.RECOVERED, Action.DELIVER_ORIGINAL, DeliveryMode.forSpace(hasSpace));
    }

    public EditorState state() {
        return state;
    }

    public Action action() {
        return action;
    }

    public DeliveryMode delivery() {
        return delivery;
    }
}
