package org.subshare.gui.util;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javafx.application.Platform;
import co.codewizards.cloudstore.core.concurrent.ExecutionException;

public final class PlatformUtil {

	private PlatformUtil() {
	}

	public static void runAndWait(Runnable runnable) throws ExecutionException {
		if (Platform.isFxApplicationThread()) {
			runnable.run();
			return;
		}

		FutureTask<?> future = new FutureTask<Object>(runnable, null);
		Platform.runLater(future);
		try {
			future.get();
		} catch (java.util.concurrent.ExecutionException | InterruptedException e) {
			throw new ExecutionException(e);
		}
	}

	public static <T> T runAndWait(Callable<T> callable) throws ExecutionException {
		if (Platform.isFxApplicationThread()) {
			try {
				return callable.call();
			} catch (Exception e) {
				throw new ExecutionException(e);
			}
		}

		FutureTask<T> future = new FutureTask<T>(callable);
		Platform.runLater(future);
		try {
			return future.get();
		} catch (java.util.concurrent.ExecutionException | InterruptedException e) {
			throw new ExecutionException(e);
		}
	}
}
