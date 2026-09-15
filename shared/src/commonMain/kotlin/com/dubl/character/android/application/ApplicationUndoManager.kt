package com.dubl.character.android.application

/** One-step semantic undo owned by the shared application layer. */
internal class ApplicationUndoManager {
    private var pending: (() -> Unit)? = null

    val canUndo: Boolean get() = pending != null

    fun record(action: () -> Unit) {
        pending = action
    }

    fun undoLast(): Boolean {
        val action = pending ?: return false
        pending = null
        action()
        return true
    }
}
