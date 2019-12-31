package org.subshare.gui.util;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.concurrent.ExecutionException;
import javafx.application.Platform;

public final class PlatformUtil {

	private static final Logger logger = LoggerFactory.getLogger(PlatformUtil.class);

	private static final AtomicBoolean exiting = new AtomicBoolean();

	public static void runAndWait(Runnable runnable) throws ExecutionException {
		if (Platform.isFxApplicationThread()) {
			runnable.run();
			return;
		}

		FutureTask<?> future = new FutureTask<Object>(runnable, null);
		Platform.runLater(future);
		try {
			while (true) {
				if (exiting.get())
					throw new ExecutionException("Not waiting, because 'exiting' flag is already true!");

				try {
					future.get(5, TimeUnit.SECONDS);
					return;
				} catch (java.util.concurrent.TimeoutException x) {
					doNothing();
				}
			}
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
			while (true) {
				if (exiting.get())
					throw new ExecutionException("Not waiting, because 'exiting' flag is already true!");

				try {
					return future.get(5, TimeUnit.SECONDS);
				} catch (java.util.concurrent.TimeoutException x) {
					doNothing();
				}
			}
		} catch (java.util.concurrent.ExecutionException | InterruptedException e) {
			throw new ExecutionException(e);
		}
	}

	/**
	 * Notifies {@code PlatformUtil} about the application beginning to exit.
	 * <p>
	 * <b>Workaround:</b> This is an ugly workaround, because there seems to be no clean API available.
	 * {@code com.sun.javafx.application.PlatformImpl.addListener(com.sun.javafx.application.PlatformImpl.FinishListener)}
	 * would be what we need, but unfortunately, this is {@code com.sun.javafx} and not public API :-(
	 */
	public static void notifyExiting() {
		exiting.compareAndSet(false, true);
	}

	private PlatformUtil() {
	}

	public static void assertFxApplicationThread() {
		if (! Platform.isFxApplicationThread())
			throw new IllegalStateException(String.format("Thread mismatch! This thread (%s) is not the Java FX application thread! UI methods must not be invoked on other threads!",
					Thread.currentThread().toString()));
	}
}
