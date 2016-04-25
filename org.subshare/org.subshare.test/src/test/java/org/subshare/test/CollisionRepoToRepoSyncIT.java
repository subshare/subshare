package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.dto.CollisionDto;
import org.subshare.core.dto.HistoFrameDto;
import org.subshare.core.dto.PlainHistoCryptoRepoFileDto;
import org.subshare.core.dto.PlainHistoCryptoRepoFileDtoTreeNode;
import org.subshare.core.repo.histo.HistoExporter;
import org.subshare.core.repo.histo.HistoExporterImpl;
import org.subshare.core.repo.local.CollisionFilter;
import org.subshare.core.repo.local.HistoFrameFilter;
import org.subshare.core.repo.local.PlainHistoCryptoRepoFileFilter;
import org.subshare.core.repo.local.SsLocalRepoMetaData;
import org.subshare.core.repo.sync.SsRepoToRepoSync;
import org.subshare.local.UserRepoKeyPublicKeyHelper;
import org.subshare.local.persistence.UserRepoKeyPublicKey;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.io.TimeoutException;
import co.codewizards.cloudstore.core.objectfactory.ObjectFactory;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync;
import co.codewizards.cloudstore.core.util.IOUtil;

@RunWith(JMockit.class)
public class CollisionRepoToRepoSyncIT extends AbstractRepoToRepoSyncIT {
	private static final Logger logger = LoggerFactory.getLogger(CollisionRepoToRepoSyncIT.class);

	@Override
	public void before() throws Exception {
		super.before();

		new MockUp<UserRepoKeyPublicKeyHelper>() {
			@Mock
			void createUserIdentities(UserRepoKeyPublicKey userRepoKeyPublicKey) {
				// Our mock should do nothing, because we don't have a real UserRegistry here.
			}
		};

		new MockUp<ObjectFactory>() {
			@Mock
			<T> T create(Invocation invocation, Class<T> clazz, Class<?>[] parameterTypes, Object ... parameters) {
				if (RepoToRepoSync.class.isAssignableFrom(clazz)) {
					return clazz.cast(new MockSsRepoToRepoSync((File) parameters[0], (URL) parameters[1]));
				}
				return invocation.proceed();
			}
		};
	}

	@Override
	public void after() throws Exception {
		for (RepoToRepoSyncCoordinator coordinator : repoToRepoSyncCoordinators)
			coordinator.close();

		repoToRepoSyncCoordinators.clear();
		super.after();
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
		protected void syncUp(ProgressMonitor monitor) {
			RepoToRepoSyncCoordinator coordinator = repoToRepoSyncCoordinatorThreadLocal.get();
			try {
				if (coordinator != null && ! coordinator.waitWhileSyncUpFrozen())
					return;

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

				super.syncDown(fromRepoLocalSync, monitor);
			} finally {
				if (coordinator != null)
					coordinator.setSyncDownDone(true);
			}
		}
	}

	private class RepoToRepoSyncCoordinator {
		private final Logger logger = LoggerFactory.getLogger(CollisionRepoToRepoSyncIT.RepoToRepoSyncCoordinator.class);

		private boolean syncUpFrozen = true;
		private boolean syncUpDone;

		private boolean syncDownFrozen = true;
		private boolean syncDownDone;

		private Throwable error;

		private boolean closed;

		public RepoToRepoSyncCoordinator() {
			repoToRepoSyncCoordinators.add(this);
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
				if (System.currentTimeMillis() - start > 120000L)
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
				if (System.currentTimeMillis() - start > 120000L)
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
				if (System.currentTimeMillis() - start > 120000L)
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
				if (System.currentTimeMillis() - start > 120000L)
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
	}

	private static final ThreadLocal<RepoToRepoSyncCoordinator> repoToRepoSyncCoordinatorThreadLocal = new ThreadLocal<RepoToRepoSyncCoordinator>() {
		@Override
		public RepoToRepoSyncCoordinator get() {
			RepoToRepoSyncCoordinator result = super.get();
			if (result == null || result.isClosed())
				return null;

			return result;
		}
	};
	private final List<RepoToRepoSyncCoordinator> repoToRepoSyncCoordinators = new ArrayList<RepoToRepoSyncCoordinator>();

	/**
	 * Two clients simultaneously create a file with the same name in the same directory.
	 * <p>
	 * The 1st client syncs completely, first. Then the 2nd client syncs completely. Thus,
	 * the collision happens during the down-sync on the 2nd client.
	 *
	 * @see #newVsNewFileCollisionOnServer()
	 */
	@Test
	public void newFileVsNewFileCollisionOnClient() throws Exception {
		prepareLocalAndDestinationRepo();

		File file1 = createFile(localSrcRoot, "2", "new-file");
		createFileWithRandomContent(file1);
		modifyFile_append(file1, 111);

		File file2 = createFile(localDestRoot, "2", "new-file");
		createFileWithRandomContent(file2);
		modifyFile_append(file2, 222);

		SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManagerLocal.getLocalRepoMetaData();
		Collection<CollisionDto> collisionDtos = localRepoMetaData.getCollisionDtos(new CollisionFilter());
		assertThat(collisionDtos).isEmpty();

		syncFromLocalSrcToRemote();
		syncFromRemoteToLocalDest(false); // should up-sync its own version
		syncFromLocalSrcToRemote(); // should down-sync the change from dest-repo
		assertDirectoriesAreEqualRecursively(
				(remotePathPrefix2Plain.isEmpty() ? getLocalRootWithPathPrefix() : createFile(getLocalRootWithPathPrefix(), remotePathPrefix2Plain)),
				localDestRoot);

		// Verify that *both* versions are in the history.
		List<PlainHistoCryptoRepoFileDto> plainHistoCryptoRepoFileDtos = getPlainHistoCryptoRepoFileDtos(localRepoManagerLocal, file1);
		assertThat(plainHistoCryptoRepoFileDtos).hasSize(2);

		// Verify that the older one is the previous version of the newer one (the list is sorted by timestamp).
		// Since the collision happens on the client, they are consecutive (rather than forked siblings of the same previous version).
		assertThat(plainHistoCryptoRepoFileDtos.get(0).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId())
		.isEqualTo(plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getPreviousHistoCryptoRepoFileId());

		// Verify that the 2nd version (with 222 at the end) is the current one.
		int lastByte1 = getLastByte(file1);
		assertThat(lastByte1).isEqualTo(222);

		int lastByte2 = getLastByte(file2);
		assertThat(lastByte2).isEqualTo(222);

		// Export both versions of the file and assert that
		// - the current file is identical to the last one
		// - and the first one ends on 111.
		File tempDir0 = createTempDirectory(getClass().getSimpleName() + '.');
		File tempDir1 = createTempDirectory(getClass().getSimpleName() + '.');

		try (HistoExporter histoExporter = HistoExporterImpl.createHistoExporter(localSrcRoot);) {
			histoExporter.exportFile(
					plainHistoCryptoRepoFileDtos.get(0).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId(), tempDir0);

			histoExporter.exportFile(
					plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId(), tempDir1);
		}
		File histoFile0 = createFile(tempDir0, "new-file");
		File histoFile1 = createFile(tempDir1, "new-file");

		assertThat(IOUtil.compareFiles(histoFile1, file1)).isTrue();
		assertThat(IOUtil.compareFiles(histoFile0, histoFile1)).isFalse();

		int lastByteOfHistoFile0 = getLastByte(histoFile0);
		assertThat(lastByteOfHistoFile0).isEqualTo(111);

		collisionDtos = localRepoMetaData.getCollisionDtos(new CollisionFilter());
		assertThat(collisionDtos).hasSize(1);
	}

	/**
	 * Two clients simultaneously modify the same file.
	 */
	@Test
	public void modifiedFileVsModifiedFileCollisionOnClient() throws Exception {
		prepareLocalAndDestinationRepo();

		File file1 = createFile(localSrcRoot, "2", "a");
		assertThat(file1.getIoFile()).isFile();
		modifyFile_append(file1, 111);

		File file2 = createFile(localDestRoot, "2", "a");
		assertThat(file2.getIoFile()).isFile();
		modifyFile_append(file2, 222);

		SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManagerLocal.getLocalRepoMetaData();
		Collection<CollisionDto> collisionDtos = localRepoMetaData.getCollisionDtos(new CollisionFilter());
		assertThat(collisionDtos).isEmpty();

		syncFromLocalSrcToRemote();
		syncFromRemoteToLocalDest(false); // should up-sync its own version
		syncFromLocalSrcToRemote(); // should down-sync the change from dest-repo
		assertDirectoriesAreEqualRecursively(
				(remotePathPrefix2Plain.isEmpty() ? getLocalRootWithPathPrefix() : createFile(getLocalRootWithPathPrefix(), remotePathPrefix2Plain)),
				localDestRoot);

		// Verify that *both* versions are in the history.
		List<PlainHistoCryptoRepoFileDto> plainHistoCryptoRepoFileDtos = getPlainHistoCryptoRepoFileDtos(localRepoManagerLocal, file1);
		assertThat(plainHistoCryptoRepoFileDtos).hasSize(3);

		// Verify that the older one is the previous version of the newer one (the list is sorted by timestamp).
		// Since the collision happens on the client, they are consecutive (rather than forked siblings of the same previous version).
		assertThat(plainHistoCryptoRepoFileDtos.get(0).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId())
		.isEqualTo(plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getPreviousHistoCryptoRepoFileId());

		assertThat(plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId())
		.isEqualTo(plainHistoCryptoRepoFileDtos.get(2).getHistoCryptoRepoFileDto().getPreviousHistoCryptoRepoFileId());

		// Verify that the 2nd version (with 222 at the end) is the current one.
		int lastByte1 = getLastByte(file1);
		assertThat(lastByte1).isEqualTo(222);

		int lastByte2 = getLastByte(file2);
		assertThat(lastByte2).isEqualTo(222);

		// Export both versions of the file and assert that
		// - the current file is identical to the last one
		// - and the first one ends on 111.
		File tempDir1 = createTempDirectory(getClass().getSimpleName() + '.');
		File tempDir2 = createTempDirectory(getClass().getSimpleName() + '.');

		try (HistoExporter histoExporter = HistoExporterImpl.createHistoExporter(localSrcRoot);) {
			histoExporter.exportFile(
					plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId(), tempDir1);

			histoExporter.exportFile(
					plainHistoCryptoRepoFileDtos.get(2).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId(), tempDir2);
		}
		File histoFile1 = createFile(tempDir1, "a");
		File histoFile2 = createFile(tempDir2, "a");

//		IOUtil.copyFile(file1, tempDir2.createFile("a.current"));
		assertThat(IOUtil.compareFiles(histoFile2, file1)).isTrue();
		assertThat(IOUtil.compareFiles(histoFile1, histoFile2)).isFalse();

		int lastByteOfHistoFile1 = getLastByte(histoFile1);
		assertThat(lastByteOfHistoFile1).isEqualTo(111);

		collisionDtos = localRepoMetaData.getCollisionDtos(new CollisionFilter());
		assertThat(collisionDtos).hasSize(1);
	}

	private void modifyFile_append(File file, int byteToAppend) throws IOException {
		try (OutputStream out = file.createOutputStream(true)) { // append
			out.write(byteToAppend);
		}
	}

//	/**
//	 * The first client deletes the file, uploads to the server, the 2nd client modifies it
//	 * and then syncs.
//	 */
//	@Test
//	public void deletedFileVsModifiedFileCollisionOnClient() throws Exception {
//
//	}
//
//	/**
//	 * The first client modifies the file, uploads to the server, the 2nd client deletes it
//	 * and then syncs.
//	 */
//	@Test
//	public void modifiedFileVsDeletedFileCollisionOnClient() throws Exception {
//
//	}

	/**
	 * Two clients simultaneously create a file with the same name in the same directory.
	 * <p>
	 * In contrast to {@link #newVsNewFileCollisionOnClient()}, both clients first sync-down,
	 * see no change on the server and then sync-up really causing two different
	 * {@code CryptoRepoFile} objects for the same file.
	 * <p>
	 * The collision thus happens and is detected during a following down-sync at a later time.
	 *
	 * @see #newVsNewFileCollisionOnClient()
	 */
	@Test
	public void newFileVsNewFileCollisionOnServer() throws Exception {
		prepareLocalAndDestinationRepo();

		File file1 = createFile(localSrcRoot, "2", "new-file");
		createFileWithRandomContent(file1);
		modifyFile_append(file1, 111);

		File file2 = createFile(localDestRoot, "2", "new-file");
		createFileWithRandomContent(file2);
		modifyFile_append(file2, 222);

		SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManagerLocal.getLocalRepoMetaData();
		Collection<CollisionDto> collisionDtos = localRepoMetaData.getCollisionDtos(new CollisionFilter());
		assertThat(collisionDtos).isEmpty();

		RepoToRepoSyncCoordinator syncFromLocalSrcToRemoteCoordinator = new RepoToRepoSyncCoordinator();
		SyncFromLocalSrcToRemoteThread syncFromLocalSrcToRemoteThread = new SyncFromLocalSrcToRemoteThread(syncFromLocalSrcToRemoteCoordinator);
		syncFromLocalSrcToRemoteThread.start();

		// should collide and up-sync its own version in parallel
		RepoToRepoSyncCoordinator syncFromRemoteToLocalDestCoordinator = new RepoToRepoSyncCoordinator();
		SyncFromRemoteToLocalDestThread syncFromRemoteToLocalDestThread = new SyncFromRemoteToLocalDestThread(syncFromRemoteToLocalDestCoordinator);
		syncFromRemoteToLocalDestThread.start();

		System.out.println("************************************************************");
//		Thread.sleep(30000);
		System.out.println("************************************************************");

		// Both threads are waiting *before* down-syncing now. We allow them both to continue:
		syncFromLocalSrcToRemoteCoordinator.setSyncDownFrozen(false);
		syncFromRemoteToLocalDestCoordinator.setSyncDownFrozen(false);

		// Then we wait until they're done down-syncing.
		syncFromLocalSrcToRemoteCoordinator.waitForSyncDownDone();
		syncFromRemoteToLocalDestCoordinator.waitForSyncDownDone();

		System.out.println("*****************");
//		Thread.sleep(60000);
		System.out.println("*****************");

		// Now they're waiting *before* up-syncing. We continue the up-sync, thus, now.
		syncFromLocalSrcToRemoteCoordinator.setSyncUpFrozen(false);
		syncFromRemoteToLocalDestCoordinator.setSyncUpFrozen(false);

		// Again, we wait here until they're done (this time, up-syncing).
		syncFromLocalSrcToRemoteCoordinator.waitForSyncUpDone();
		syncFromRemoteToLocalDestCoordinator.waitForSyncUpDone();

		System.out.println("*****************");
//		Thread.sleep(60000);
		System.out.println("*****************");

		// Again, they're waiting for down-sync's green light => go!
		syncFromLocalSrcToRemoteCoordinator.setSyncDownFrozen(false);
		syncFromRemoteToLocalDestCoordinator.setSyncDownFrozen(false);

		// ...wait until they're done.
		syncFromLocalSrcToRemoteCoordinator.waitForSyncDownDone();
		syncFromRemoteToLocalDestCoordinator.waitForSyncDownDone();

		System.out.println("************************************************************");
//		Thread.sleep(60000);
		System.out.println("************************************************************");


//		syncFromLocalSrcToRemote(); // should down-sync the change from dest-repo ... not needed! they're guaranteed to work in parallel using the ...Coordinator!
		assertDirectoriesAreEqualRecursively(
				(remotePathPrefix2Plain.isEmpty() ? getLocalRootWithPathPrefix() : createFile(getLocalRootWithPathPrefix(), remotePathPrefix2Plain)),
				localDestRoot);

		// Verify that *both* versions are in the history.
		List<PlainHistoCryptoRepoFileDto> plainHistoCryptoRepoFileDtos = getPlainHistoCryptoRepoFileDtos(localRepoManagerLocal, file1);
		assertThat(plainHistoCryptoRepoFileDtos).hasSize(2);

		// Both new versions should have the same previous version, because the collision happened on the server.
		Set<Uid> previousHistoCryptoRepoFileIds = new HashSet<>();
		for (PlainHistoCryptoRepoFileDto phcrfDto : plainHistoCryptoRepoFileDtos)
			previousHistoCryptoRepoFileIds.add(phcrfDto.getHistoCryptoRepoFileDto().getPreviousHistoCryptoRepoFileId());

		assertThat(previousHistoCryptoRepoFileIds).hasSize(1);

		// Verify that the 2nd version (with 222 at the end) is the current one.
		int lastByte1 = getLastByte(file1);
		assertThat(lastByte1).isEqualTo(222);

		int lastByte2 = getLastByte(file2);
		assertThat(lastByte2).isEqualTo(222);

		// Export both versions of the file and assert that
		// - the current file is identical to the last one
		// - and the first one ends on 111.
		File tempDir0 = createTempDirectory(getClass().getSimpleName() + '.');
		File tempDir1 = createTempDirectory(getClass().getSimpleName() + '.');

		try (HistoExporter histoExporter = HistoExporterImpl.createHistoExporter(localSrcRoot);) {
			histoExporter.exportFile(
					plainHistoCryptoRepoFileDtos.get(0).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId(), tempDir0);

			histoExporter.exportFile(
					plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId(), tempDir1);
		}
		File histoFile0 = createFile(tempDir0, "new-file");
		File histoFile1 = createFile(tempDir1, "new-file");

		assertThat(IOUtil.compareFiles(histoFile1, file1)).isTrue();
		assertThat(IOUtil.compareFiles(histoFile0, histoFile1)).isFalse();

		int lastByteOfHistoFile0 = getLastByte(histoFile0);
		assertThat(lastByteOfHistoFile0).isEqualTo(111);

		collisionDtos = localRepoMetaData.getCollisionDtos(new CollisionFilter());
		assertThat(collisionDtos).hasSize(1); // TODO shouldn't this be 2?! after all, both clients should detect a collision independently and there's no code, yet, removing one of them.
	}

	private class SyncFromLocalSrcToRemoteThread extends Thread {
		private final Logger logger = LoggerFactory.getLogger(SyncFromLocalSrcToRemoteThread.class);

		private final RepoToRepoSyncCoordinator coordinator;

		public SyncFromLocalSrcToRemoteThread(RepoToRepoSyncCoordinator coordinator) {
			this.coordinator = assertNotNull("coordinator", coordinator);
		}

		@Override
		public void run() {
			repoToRepoSyncCoordinatorThreadLocal.set(coordinator);
			try {
				syncFromLocalSrcToRemote();
			} catch (Throwable e) {
				logger.error("run: " + e, e);
				coordinator.setError(e);
			}
		}
	}

	private class SyncFromRemoteToLocalDestThread extends Thread {
		private final Logger logger = LoggerFactory.getLogger(SyncFromRemoteToLocalDestThread.class);

		private final RepoToRepoSyncCoordinator coordinator;

		public SyncFromRemoteToLocalDestThread(RepoToRepoSyncCoordinator coordinator) {
			this.coordinator = assertNotNull("coordinator", coordinator);
		}

		@Override
		public void run() {
			repoToRepoSyncCoordinatorThreadLocal.set(coordinator);
			try {
				syncFromRemoteToLocalDest(false);
			} catch (Throwable e) {
				logger.error("run: " + e, e);
				coordinator.setError(e);
			}
		}
	}

//	/**
//	 * Two clients simultaneously modify the same file.
//	 */
//	@Test
//	public void modifiedFileVsModifiedFileCollisionOnServer() throws Exception {
//
//	}
//
//	/**
//	 * The first client deletes the file, uploads to the server, the 2nd client modifies it
//	 * and then syncs.
//	 */
//	@Test
//	public void deletedFileVsModifiedFileCollisionOnServer() throws Exception {
//
//	}
//
//	/**
//	 * The first client modifies the file, uploads to the server, the 2nd client deletes it
//	 * and then syncs.
//	 */
//	@Test
//	public void modifiedFileVsDeletedFileCollisionOnServer() throws Exception {
//
//	}

	// TODO add file-type-change-collisions (e.g. from directory to file)

	protected void prepareLocalAndDestinationRepo() throws Exception {
		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();
		determineRemotePathPrefix2Encrypted();
		createLocalDestinationRepo();
		syncFromRemoteToLocalDest();
	}

	private int getLastByte(File file) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(file.getIoFile(), "r")) {
			raf.seek(raf.length() - 1);
			int result = raf.read();
			assertThat(result).isGreaterThanOrEqualTo(0);
			return result;
		}
	}

	private List<PlainHistoCryptoRepoFileDto> getPlainHistoCryptoRepoFileDtos(LocalRepoManager localRepoManager, File file) throws IOException {
		final String path = "/" + localRepoManager.getLocalRoot().relativize(file).replace('\\', '/');
		SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManagerLocal.getLocalRepoMetaData();
		List<PlainHistoCryptoRepoFileDto> result = new ArrayList<>();

		// TODO need to extend the filter with a path! Do this when extending the UI to show a history in every folder-detail-pane.
		// The current implementation is very inefficient - but we have only small test data, anyway ;-)
		Collection<HistoFrameDto> histoFrameDtos = localRepoMetaData.getHistoFrameDtos(new HistoFrameFilter());
		for (HistoFrameDto histoFrameDto : histoFrameDtos) {
			PlainHistoCryptoRepoFileFilter filter = new PlainHistoCryptoRepoFileFilter();
			filter.setHistoFrameId(histoFrameDto.getHistoFrameId());
			filter.setFillParents(true);
			Collection<PlainHistoCryptoRepoFileDto> plainHistoCryptoRepoFileDtos = localRepoMetaData.getPlainHistoCryptoRepoFileDtos(filter);
			PlainHistoCryptoRepoFileDtoTreeNode rootNode = PlainHistoCryptoRepoFileDtoTreeNode.createTree(plainHistoCryptoRepoFileDtos);
			for (PlainHistoCryptoRepoFileDtoTreeNode node : rootNode) {
				if (path.equals(node.getPath()))
					result.add(node.getPlainHistoCryptoRepoFileDto());
			}
		}

		Collections.sort(result, new Comparator<PlainHistoCryptoRepoFileDto>() {
			@Override
			public int compare(PlainHistoCryptoRepoFileDto o1, PlainHistoCryptoRepoFileDto o2) {
				Date signatureCreated1 = o1.getHistoCryptoRepoFileDto().getSignature().getSignatureCreated();
				Date signatureCreated2 = o2.getHistoCryptoRepoFileDto().getSignature().getSignatureCreated();
				return signatureCreated1.compareTo(signatureCreated2);
			}
		});

		return result;
	}
}
