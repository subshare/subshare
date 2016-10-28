package org.subshare.gui.concurrent;

import org.subshare.gui.error.ErrorHandler;

import javafx.concurrent.Task;

public abstract class SsTask<T> extends Task<T> {

	@Override
	protected void failed() {
		ErrorHandler.handleError(getException());
	}
}
