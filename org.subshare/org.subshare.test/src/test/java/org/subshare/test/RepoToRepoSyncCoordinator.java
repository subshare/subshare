package org.subshare.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.io.TimeoutException;

public class RepoToRepoSyncCoordinator {
	public static final long WAIT_FOR_THREAD_TIMEOUT_MS = 120000L; // you might want to temporarily increase this for debugging!
//	public static final long WAIT_FOR_THREAD_TIMEOUT_MS = 900000L;

	private final Logger logger = LoggerFactory.getLogger(RepoToRepoSyncCoordinator.class);

	private boolean syncUpFrozen = true;
	private boolean syncUpDone;

	private boolean syncDownFrozen = true;
	private boolean syncDownDone;

	private Throwable error;

	private boolean closed;

	protected RepoToRepoSyncCoordinator() {
	}

	protected synchronized boolean isSyncUpFrozen() {
		return syncUpFrozen;
	}
	protected synchronized void setSyncUpFrozen(boolean value) {
		logger.info("setSyncUpFrozen: value={}", value);
		this.syncUpFrozen = value;
		notifyAll();
	}

	protected synchronized boolean isSyncDownFrozen() {
		return syncDownFrozen;
	}
	protected synchronized void setSyncDownFrozen(boolean value) {
		logger.info("setSyncDownFrozen: value={}", value);
		this.syncDownFrozen = value;
		notifyAll();
	}

	protected synchronized boolean isSyncUpDone() {
		return syncUpDone;
	}
	protected synchronized void setSyncUpDone(boolean value) {
		logger.info("setSyncUpDone: value={}", value);
		this.syncUpDone = value;
		notifyAll();
	}

	protected synchronized boolean isSyncDownDone() {
		return syncDownDone;
	}
	protected synchronized void setSyncDownDone(boolean value) {
		logger.info("setSyncDownDone: value={}", value);
		this.syncDownDone = value;
		notifyAll();
	}

	public synchronized boolean waitWhileSyncUpFrozen() {
		final long start = System.currentTimeMillis();
		while (isSyncUpFrozen()) {
			logger.info("waitWhileSyncUpFrozen: Waiting...");
			try {
				wait(30000);
			} catch (InterruptedException e) {
				logger.error("waitWhileSyncUpFrozen: " + e, e);
				return false;
			}
			throwErrorIfNeeded();
			if (System.currentTimeMillis() - start > WAIT_FOR_THREAD_TIMEOUT_MS)
				throw new TimeoutException();
		}
		setSyncUpFrozen(true);
		logger.info("waitWhileSyncUpFrozen: Continuing!");
		return true;
	}

	public synchronized boolean waitWhileSyncDownFrozen() {
		final long start = System.currentTimeMillis();
		while (isSyncDownFrozen()) {
			logger.info("waitWhileSyncDownFrozen: Waiting...");
			try {
				wait(30000);
			} catch (InterruptedException e) {
				logger.error("waitWhileSyncDownFrozen: " + e, e);
				return false;
			}
			throwErrorIfNeeded();
			if (System.currentTimeMillis() - start > WAIT_FOR_THREAD_TIMEOUT_MS)
				throw new TimeoutException();
		}
		setSyncDownFrozen(true);
		logger.info("waitWhileSyncDownFrozen: Continuing!");
		return true;
	}

	public synchronized boolean waitForSyncUpDone() {
		final long start = System.currentTimeMillis();
		while (! isSyncUpDone()) {
			logger.info("waitForSyncUpDone: Waiting...");
			try {
				wait(30000);
			} catch (InterruptedException e) {
				logger.error("waitForSyncUpDone: " + e, e);
				return false;
			}
			throwErrorIfNeeded();
			if (System.currentTimeMillis() - start > WAIT_FOR_THREAD_TIMEOUT_MS)
				throw new TimeoutException();
		}
		setSyncUpDone(false);
		logger.info("waitForSyncUpDone: Continuing!");
		return true;
	}

	public synchronized boolean waitForSyncDownDone() {
		final long start = System.currentTimeMillis();
		while (! isSyncDownDone()) {
			logger.info("waitForSyncDownDone: Waiting...");
			try {
				wait(30000);
			} catch (InterruptedException e) {
				logger.error("waitForSyncDownDone: " + e, e);
				return false;
			}
			throwErrorIfNeeded();
			if (System.currentTimeMillis() - start > WAIT_FOR_THREAD_TIMEOUT_MS)
				throw new TimeoutException();
		}
		setSyncDownDone(false);
		logger.info("waitForSyncDownDone: Continuing!");
		return true;
	}

	public synchronized Throwable getError() {
		return error;
	}
	public synchronized void setError(Throwable error) {
		this.error = error;
		notifyAll();
	}

	public void throwErrorIfNeeded() {
		Throwable error = getError();
		if (error != null) {
			if (error instanceof Error)
				throw (Error) error;

			throw new RuntimeException(error);
		}
	}

	public synchronized boolean isClosed() {
		return closed;
	}

	public synchronized void close() {
		closed = true;

		if (error == null)
			error = new RuntimeException("CLOSED!");

		notifyAll();
	}

	public void bindToCurrentThread() {
		RepoToRepoSyncCoordinatorSupport.repoToRepoSyncCoordinatorThreadLocal.set(this);
	}
}
