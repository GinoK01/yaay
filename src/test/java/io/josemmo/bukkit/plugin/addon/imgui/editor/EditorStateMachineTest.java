package io.josemmo.bukkit.plugin.addon.imgui.editor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EditorStateMachineTest {
    @Test
    public void finishesFromEditingExactlyOnce() {
        EditorState[] targets = new EditorState[] {
            EditorState.CANCELLED,
            EditorState.CONFIRMED,
            EditorState.RECOVERED
        };
        for (int i = 0; i < targets.length; i++) {
            EditorStateMachine machine = new EditorStateMachine();
            assertEquals(EditorState.EDITING, machine.state());
            assertTrue(machine.tryFinish(targets[i]));
            assertEquals(targets[i], machine.state());
            assertFalse(machine.tryFinish(targets[i]));
            assertFalse(machine.tryFinish(EditorState.CONFIRMED));
            assertEquals(targets[i], machine.state());
        }
    }

    @Test
    public void rejectsEditingAndNull() {
        EditorStateMachine machine = new EditorStateMachine();
        assertFalse(machine.tryFinish(EditorState.EDITING));
        assertFalse(machine.tryFinish(null));
        assertEquals(EditorState.EDITING, machine.state());
    }

    @Test
    public void secondSessionFinishDoesNotChangeState() {
        EditorSession session = new EditorSession();
        assertNull(session.finish(EditorState.CANCELLED));
        assertEquals(EditorState.CANCELLED, session.state());
        assertNull(session.finish(EditorState.CONFIRMED));
        assertEquals(EditorState.CANCELLED, session.state());
        assertNull(session.finish(EditorState.RECOVERED));
        assertEquals(EditorState.CANCELLED, session.state());
    }

    @Test
    public void fullInventoryDropsTheResizedItem() {
        EditorFinishPlan plan = EditorFinishPlan.confirm(true, false);
        assertEquals(EditorState.CONFIRMED, plan.state());
        assertEquals(EditorFinishPlan.Action.DELIVER_RESULT, plan.action());
        assertEquals(DeliveryMode.DROP, plan.delivery());
    }

    @Test
    public void createFailureRecoversTheOriginalAndDoesNotConfirm() {
        EditorFinishPlan withSpace = EditorFinishPlan.confirm(false, true);
        assertEquals(EditorState.RECOVERED, withSpace.state());
        assertEquals(EditorFinishPlan.Action.DELIVER_ORIGINAL, withSpace.action());
        assertEquals(DeliveryMode.INVENTORY, withSpace.delivery());

        EditorFinishPlan withoutSpace = EditorFinishPlan.confirm(false, false);
        assertEquals(EditorState.RECOVERED, withoutSpace.state());
        assertEquals(EditorFinishPlan.Action.DELIVER_ORIGINAL, withoutSpace.action());
        assertEquals(DeliveryMode.DROP, withoutSpace.delivery());
        assertFalse(withoutSpace.state() == EditorState.CONFIRMED);
    }

    @Test
    public void cancelThenConfirmIsRejected() {
        EditorStateMachine machine = new EditorStateMachine();
        assertTrue(machine.tryFinish(EditorState.CANCELLED));
        assertFalse(machine.tryFinish(EditorState.CONFIRMED));
        assertEquals(EditorState.CANCELLED, machine.state());

        EditorFinishPlan cancelled = EditorFinishPlan.cancel(true);
        assertEquals(EditorState.CANCELLED, cancelled.state());
        assertEquals(EditorFinishPlan.Action.DELIVER_ORIGINAL, cancelled.action());
        assertEquals(DeliveryMode.INVENTORY, cancelled.delivery());
    }
}
