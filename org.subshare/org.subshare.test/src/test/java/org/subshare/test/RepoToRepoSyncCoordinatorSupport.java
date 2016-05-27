package org.subshare.test;

import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.repo.sync.SsRepoToRepoSync;

import co.codewizards.cloudstore.core.dto.FileChunkDto;
import co.codewizards.cloudstore.core.dto.NormalFileDto;
import co.codewizards.cloudstore.core.dto.RepoFileDtoTreeNode;
import co.codewizards.cloudstore.core.objectfactory.ObjectFactory;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;
import co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync;
import co.codewizards.cloudstore.core.repo.transport.CollisionException;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;

public class RepoToRepoSyncCoordinatorSupport {

	private static final Logger logger = LoggerFactory.getLogger(RepoToRepoSyncCoordinatorSupport.class);

	private static final Random random = new SecureRandom();

	protected static final ThreadLocal<RepoToRepoSyncCoordinator> repoToRepoSyncCoordinatorThreadLocal = new ThreadLocal<RepoToRepoSyncCoordinator>() {
		@Override
		public RepoToRepoSyncCoordinator get() {
			RepoToRepoSyncCoordinator result = super.get();
			if (result == null || result.isClosed())
				return null;

			return result;
		}
	};
	private final List<RepoToRepoSyncCoordinator> repoToRepoSyncCoordinators = new ArrayList<RepoToRepoSyncCoordinator>();

	public void beforeTest() {
		new MockUp<ObjectFactory>() {
			@Mock
			<T> T createObject(Invocation invocation, Class<T> clazz, Class<?>[] parameterTypes, Object ... parameters) {
				if (RepoToRepoSync.class.isAssignableFrom(clazz)) {
					return clazz.cast(new MockSsRepoToRepoSync((File) parameters[0], (URL) parameters[1]));
				}
				return invocation.proceed();
			}
		};
	}

	public void afterTest() {
		List<RepoToRepoSyncCoordinator> coordinators = new ArrayList<>(repoToRepoSyncCoordinators);
		repoToRepoSyncCoordinators.clear();

		for (RepoToRepoSyncCoordinator coordinator : coordinators)
			coordinator.close();
	}

	public RepoToRepoSyncCoordinator createRepoToRepoSyncCoordinator() {
		RepoToRepoSyncCoordinator coordinator = new RepoToRepoSyncCoordinator();
		repoToRepoSyncCoordinators.add(coordinator);
		return coordinator;
	}

	/**
	 * Mocking the {@link SsRepoToRepoSync} does not work - for whatever reason - hence, this
	 * class is a "manual" mock which is introduced into Subshare using a mocked {@link ObjectFactory}.
	 */
	private static class MockSsRepoToRepoSync extends SsRepoToRepoSync {

		protected MockSsRepoToRepoSync(File localRoot, URL remoteRoot) {
			super(localRoot, remoteRoot);
		}

		@Override
		protected void beginPutFile(RepoTransport fromRepoTransport, RepoTransport toRepoTransport,
				RepoFileDtoTreeNode repoFileDtoTreeNode, String path, NormalFileDto fromNormalFileDto) throws CollisionException {
			RepoToRepoSyncCoordinator coordinator = repoToRepoSyncCoordinatorThreadLocal.get();
			if (coordinator != null) {
				if (localRepoTransport == fromRepoTransport)
					coordinator.resetUploadedFileChunkCount();
				else
					coordinator.resetDownloadedFileChunkCount();
			}

			super.beginPutFile(fromRepoTransport, toRepoTransport, repoFileDtoTreeNode, path, fromNormalFileDto);
		}

		@Override
		protected void putFileData(RepoTransport fromRepoTransport, RepoTransport toRepoTransport,
				RepoFileDtoTreeNode repoFileDtoTreeNode, String path, FileChunkDto fileChunkDto, byte[] fileData) {
			RepoToRepoSyncCoordinator coordinator = repoToRepoSyncCoordinatorThreadLocal.get();

			super.putFileData(fromRepoTransport, toRepoTransport, repoFileDtoTreeNode, path, fileChunkDto, fileData);

			if (coordinator != null) {
				if (localRepoTransport == fromRepoTransport)
					coordinator.incUploadedFileChunkCount();
				else
					coordinator.incDownloadedFileChunkCount();
			}
		}

		@Override
		protected void syncUp(ProgressMonitor monitor) {
			RepoToRepoSyncCoordinator coordinator = repoToRepoSyncCoordinatorThreadLocal.get();
			try {
				if (coordinator != null && ! coordinator.waitWhileSyncUpFrozen())
					return;

				sleep(random.nextInt(3000));

				super.syncUp(monitor);
			} finally {
				if (coordinator != null)
					coordinator.setSyncUpDone(true);
			}
		}

		@Override
		protected void syncDown(boolean fromRepoLocalSync, ProgressMonitor monitor) {
			RepoToRepoSyncCoordinator coordinator = repoToRepoSyncCoordinatorThreadLocal.get();
			try {
				if (coordinator != null && ! coordinator.waitWhileSyncDownFrozen())
					return;

				sleep(random.nextInt(3000));

				super.syncDown(fromRepoLocalSync, monitor);
			} finally {
				if (coordinator != null)
					coordinator.setSyncDownDone(true);
			}
		}
	}

	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			logger.warn("sleep: " + e, e);
		}
	}
}
