package org.subshare.gui.wizard;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.layout.GridPane;

public abstract class WizardPageContentGridPane extends GridPane implements CompletableContent {

	protected final BooleanProperty complete = new SimpleBooleanProperty(this, "complete");

	protected abstract boolean isComplete();

	protected final void updateComplete() {
		complete.set(isComplete());
	}

	@Override
	public final ReadOnlyBooleanProperty completeProperty() {
		return complete;
	}
}
