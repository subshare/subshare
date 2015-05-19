package org.subshare.gui.concurrent;

import javafx.concurrent.Task;

import org.subshare.gui.error.ErrorHandler;

public abstract class SsTask<T> extends Task<T> {

	@Override
	protected void failed() {
		ErrorHandler.handleError(getException());
	}
}
