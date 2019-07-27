package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static java.util.Objects.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.dto.CollisionDto;
import org.subshare.core.dto.PlainHistoCryptoRepoFileDto;
import org.subshare.core.repo.histo.ExportFileParam;
import org.subshare.core.repo.histo.HistoExporter;
import org.subshare.core.repo.histo.HistoExporterImpl;
import org.subshare.core.repo.local.CollisionFilter;
import org.subshare.core.repo.local.SsLocalRepoMetaData;

import co.codewizards.cloudstore.core.io.TimeoutException;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.IOUtil;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class CollisionOnServerRepoToRepoSyncIT extends CollisionRepoToRepoSyncIT {
	private static final Logger logger = LoggerFactory.getLogger(CollisionOnServerRepoToRepoSyncIT.class);

	private RepoToRepoSyncCoordinatorSupport repoToRepoSyncCoordinatorSupport = new RepoToRepoSyncCoordinatorSupport();

	@Override
	public void before() throws Exception {
		super.before();
		repoToRepoSyncCoordinatorSupport.beforeTest();
	}

	@Override
	public void after() throws Exception {
		repoToRepoSyncCoordinatorSupport.afterTest();
		super.after();
	}

	/**
	 * Two clients simultaneously create a file with the same name in the same directory.
	 * <p>
	 * In contrast to {@link CollisionOnClientRepoToRepoSyncIT#newFileVsNewFileCollisionOnClient()}, both clients first sync-down,
	 * see no change on the server and then sync-up really causing two different
	 * {@code CryptoRepoFile} objects for the same file.
	 * <p>
	 * The collision thus happens and is detected during a following down-sync at a later time.
	 */
	@Test
	public void newFileVsNewFileUploadedCollisionOnServer() throws Exception {
		System.out.println("************************************************************");
		System.out.println("PREPARE >>>");
		prepareLocalAndDestinationRepo();

		File file1 = createFile(localSrcRoot, "2", "new-file");
		createFileWithRandomContent(file1);
		modifyFile_append(file1, 111);

		File file2 = createFile(localDestRoot, "2", "new-file");
		createFileWithRandomContent(file2);
		modifyFile_append(file2, 222);

		SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localSrcRepoManagerLocal.getLocalRepoMetaData();
		Collection<CollisionDto> collisionDtos = localRepoMetaData.getCollisionDtos(new CollisionFilter());
		assertThat(collisionDtos).isEmpty();

		System.out.println("<<< PREPARE");
		System.out.println("************************************************************");

		RepoToRepoSyncCoordinator syncFromLocalSrcToRemoteCoordinator = repoToRepoSyncCoordinatorSupport.createRepoToRepoSyncCoordinator();
		SyncFromLocalSrcToRemoteThread syncFromLocalSrcToRemoteThread = new SyncFromLocalSrcToRemoteThread(syncFromLocalSrcToRemoteCoordinator);
		syncFromLocalSrcToRemoteThread.start();

		// should collide and up-sync its own version in parallel
		RepoToRepoSyncCoordinator syncFromRemoteToLocalDestCoordinator = repoToRepoSyncCoordinatorSupport.createRepoToRepoSyncCoordinator();
		SyncFromRemoteToLocalDestThread syncFromRemoteToLocalDestThread = new SyncFromRemoteToLocalDestThread(syncFromRemoteToLocalDestCoordinator);
		syncFromRemoteToLocalDestThread.start();

		System.out.println("************************************************************");
//		Thread.sleep(30000);
		System.out.println("************************************************************");
		System.out.println("DOWN >>>");

		// Both threads are waiting *before* down-syncing now. We allow them both to continue:
		syncFromLocalSrcToRemoteCoordinator.setSyncDownFrozen(false);
		syncFromRemoteToLocalDestCoordinator.setSyncDownFrozen(false);

		// Then we wait until they're done down-syncing.
		syncFromLocalSrcToRemoteCoordinator.waitForSyncDownDone();
		syncFromRemoteToLocalDestCoordinator.waitForSyncDownDone();

		System.out.println("<<< DOWN");
		System.out.println("*****************");
//		Thread.sleep(60000);
		System.out.println("*****************");
		System.out.println("UP >>>");

		// Now they're waiting *before* up-syncing. We continue the up-sync, thus, now.
		syncFromLocalSrcToRemoteCoordinator.setSyncUpFrozen(false);
		syncFromRemoteToLocalDestCoordinator.setSyncUpFrozen(false);

		// Again, we wait here until they're done (this time, up-syncing).
		syncFromLocalSrcToRemoteCoordinator.waitForSyncUpDone();
		syncFromRemoteToLocalDestCoordinator.waitForSyncUpDone();

		System.out.println("<<< UP");
		System.out.println("*****************");
//		Thread.sleep(60000);
		System.out.println("*****************");
		System.out.println("DOWN (again) >>>");

		// Again, they're waiting for down-sync's green light => go!
		syncFromLocalSrcToRemoteCoordinator.setSyncDownFrozen(false);
		syncFromRemoteToLocalDestCoordinator.setSyncDownFrozen(false);

		// ...wait until they're done.
		syncFromLocalSrcToRemoteCoordinator.waitForSyncDownDone();
		syncFromRemoteToLocalDestCoordinator.waitForSyncDownDone();

		System.out.println("<<< DOWN (again)");
		System.out.println("************************************************************");
//		Thread.sleep(60000);
		System.out.println("************************************************************");

		// TODO check whether Collisions are created on clients, but not yet up-synced

		System.out.println("*****************");
//		Thread.sleep(60000);
		System.out.println("*****************");
		System.out.println("UP (again) >>>");

		// Now they're waiting *before* up-syncing. We continue the up-sync, thus, now.
		syncFromLocalSrcToRemoteCoordinator.setSyncUpFrozen(false);
		syncFromRemoteToLocalDestCoordinator.setSyncUpFrozen(false);

		// Again, we wait here until they're done (this time, up-syncing).
		syncFromLocalSrcToRemoteCoordinator.waitForSyncUpDone();
		syncFromRemoteToLocalDestCoordinator.waitForSyncUpDone();

		System.out.println("<<< UP (again)");
		System.out.println("*****************");

		// TODO check whether Collisions are up-synced.

		// make sure the threads finished.
		syncFromLocalSrcToRemoteThread.join(RepoToRepoSyncCoordinator.WAIT_FOR_THREAD_TIMEOUT_MS);
		syncFromRemoteToLocalDestThread.join(RepoToRepoSyncCoordinator.WAIT_FOR_THREAD_TIMEOUT_MS);

		// sync *again* in order to make sure the collided file is uploaded now.
		syncFromLocalSrcToRemote();
		syncFromRemoteToLocalDest(false);
		syncFromLocalSrcToRemote(); // again, in case the collision was solved on the dest-side

//		syncFromLocalSrcToRemote(); // should down-sync the change from dest-repo ... not needed! they're guaranteed to work in parallel using the ...Coordinator!
		assertDirectoriesAreEqualRecursively(
				(remotePathPrefix2Plain.isEmpty() ? getLocalRootWithPathPrefix() : createFile(getLocalRootWithPathPrefix(), remotePathPrefix2Plain)),
				localDestRoot);

		// Verify that *both* versions are in the history.
		List<PlainHistoCryptoRepoFileDto> plainHistoCryptoRepoFileDtos = getPlainHistoCryptoRepoFileDtos(localSrcRepoManagerLocal, file1);
		assertThat(plainHistoCryptoRepoFileDtos).hasSize(2);

//		// Both new versions should have the same previous version, because the collision happened on the server.
//		Set<Uid> previousHistoCryptoRepoFileIds = new HashSet<>();
//		for (PlainHistoCryptoRepoFileDto phcrfDto : plainHistoCryptoRepoFileDtos)
//			previousHistoCryptoRepoFileIds.add(phcrfDto.getHistoCryptoRepoFileDto().getPreviousHistoCryptoRepoFileId());
//
//		assertThat(previousHistoCryptoRepoFileIds).hasSize(1);

		// Verify that the older one is the previous version of the newer one (the list is sorted by timestamp).
		// Even though the collision happens on the server, the handling process ensures that they are consecutive
		// (rather than forked siblings of the same previous version).
		assertThat(plainHistoCryptoRepoFileDtos.get(0).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId())
		.isEqualTo(plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getPreviousHistoCryptoRepoFileId());

		// Verify that the 2nd version (with 222 at the end) is the current one.
		int lastByte1 = getLastByte(file1);
		int lastByte2 = getLastByte(file2);

		int expectedLastByteOfPreviousVersion;

		assertThat(lastByte1).isEqualTo(lastByte2);

		if (lastByte1 == 111)
			expectedLastByteOfPreviousVersion = 222;
		else if (lastByte1 == 222)
			expectedLastByteOfPreviousVersion = 111;
		else
			throw new IllegalStateException("lastByte is neither 111 nor 222, but: " + lastByte1);

		// Export both versions of the file and assert that
		// - the current file is identical to the last one
		// - and the first one ends on the other value (222, if current is 111; or 111, if current is 222).
		File tempDir0 = createTempDirectory(getClass().getSimpleName() + '.');
		File tempDir1 = createTempDirectory(getClass().getSimpleName() + '.');

		try (HistoExporter histoExporter = HistoExporterImpl.createHistoExporter(localSrcRoot);) {
			histoExporter.exportFile(new ExportFileParam(
					plainHistoCryptoRepoFileDtos.get(0).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId(), tempDir0));

			histoExporter.exportFile(new ExportFileParam(
					plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId(), tempDir1));
		}
		File histoFile0 = createFile(tempDir0, "new-file");
		File histoFile1 = createFile(tempDir1, "new-file");

		assertThat(IOUtil.compareFiles(histoFile1, file1)).isTrue();
		assertThat(IOUtil.compareFiles(histoFile0, histoFile1)).isFalse();

		int lastByteOfHistoFile0 = getLastByte(histoFile0);

		System.out.println("lastByteOfHistoFile0 = " + lastByteOfHistoFile0);
		System.out.println("lastByteOfHistoFile1 = " + lastByte1);

		assertThat(lastByteOfHistoFile0).isEqualTo(expectedLastByteOfPreviousVersion);

		collisionDtos = localRepoMetaData.getCollisionDtos(new CollisionFilter());
		assertThat(collisionDtos).hasSize(2);

		// exactly one of these two should have a duplicateCryptoRepoFileId assigned.
		getCollisionDtoWithDuplicateCryptoRepoFileIdOrFail(collisionDtos);
	}

	@Test
	public void newFileVsNewFileUploadingCollisionOnServer() throws Exception {
		System.out.println("************************************************************");
		System.out.println("PREPARE >>>");
		prepareLocalAndDestinationRepo();

		final boolean scenarioB = random.nextBoolean();
		System.out.println("scenarioB = " + scenarioB);

		File file1 = createFile(localSrcRoot, "2", "new-file");
		createFileWithRandomContent(file1);
		if (scenarioB)
			modifyFile_append(file1, 111);
		else
			modifyFile_append(file1, 111, 100 * 1024 * 1024); // append large amount of bytes (each byte value 111)

		File file2 = createFile(localDestRoot, "2", "new-file");
		createFileWithRandomContent(file2);
		if (! scenarioB)
			modifyFile_append(file2, 222);
		else
			modifyFile_append(file2, 222, 100 * 1024 * 1024); // append large amount of bytes (each byte value 222)

		SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localSrcRepoManagerLocal.getLocalRepoMetaData();
		Collection<CollisionDto> collisionDtos = localRepoMetaData.getCollisionDtos(new CollisionFilter());
		assertThat(collisionDtos).isEmpty();

		System.out.println("<<< PREPARE");
		System.out.println("************************************************************");

		RepoToRepoSyncCoordinator syncFromLocalSrcToRemoteCoordinator = repoToRepoSyncCoordinatorSupport.createRepoToRepoSyncCoordinator();
		SyncFromLocalSrcToRemoteThread syncFromLocalSrcToRemoteThread = new SyncFromLocalSrcToRemoteThread(syncFromLocalSrcToRemoteCoordinator);
		syncFromLocalSrcToRemoteThread.start();

		// should collide and up-sync its own version in parallel
		RepoToRepoSyncCoordinator syncFromRemoteToLocalDestCoordinator = repoToRepoSyncCoordinatorSupport.createRepoToRepoSyncCoordinator();
		SyncFromRemoteToLocalDestThread syncFromRemoteToLocalDestThread = new SyncFromRemoteToLocalDestThread(syncFromRemoteToLocalDestCoordinator);
		syncFromRemoteToLocalDestThread.start();

		System.out.println("************************************************************");
//		Thread.sleep(30000);
		System.out.println("************************************************************");
		System.out.println("DOWN >>>");

		// Both threads are waiting *before* down-syncing now. We allow them both to continue:
		syncFromLocalSrcToRemoteCoordinator.setSyncDownFrozen(false);
		syncFromRemoteToLocalDestCoordinator.setSyncDownFrozen(false);

		syncFromLocalSrcToRemoteCoordinator.setSyncDownFreezeEnabled(false);
		syncFromRemoteToLocalDestCoordinator.setSyncDownFreezeEnabled(false);

		// Then we wait until they're done down-syncing.
		syncFromLocalSrcToRemoteCoordinator.waitForSyncDownDone();
		syncFromRemoteToLocalDestCoordinator.waitForSyncDownDone();

		System.out.println("<<< DOWN");
		System.out.println("*****************");
//		Thread.sleep(60000);
		System.out.println("*****************");
		System.out.println("CONTINUE SYNC >>>");

		// Now they're waiting *before* up-syncing. We continue the up-sync, thus, now.
		syncFromLocalSrcToRemoteCoordinator.setSyncUpFrozen(false);
		syncFromRemoteToLocalDestCoordinator.setSyncUpFrozen(false);

		syncFromLocalSrcToRemoteCoordinator.setSyncUpFreezeEnabled(false);
		syncFromRemoteToLocalDestCoordinator.setSyncUpFreezeEnabled(false);

		// Here, we do *not* wait until they're done, but only until the first block was uploaded.
		syncFromLocalSrcToRemoteCoordinator.waitForUploadedFileChunkCountGreaterOrEqual(1);
		syncFromRemoteToLocalDestCoordinator.waitForUploadedFileChunkCountGreaterOrEqual(1);

		// wait for the threads to finish.
		syncFromLocalSrcToRemoteThread.join(RepoToRepoSyncCoordinator.WAIT_FOR_THREAD_TIMEOUT_MS);
		syncFromRemoteToLocalDestThread.join(RepoToRepoSyncCoordinator.WAIT_FOR_THREAD_TIMEOUT_MS);

		if (syncFromLocalSrcToRemoteThread.isAlive() || syncFromRemoteToLocalDestThread.isAlive())
			throw new TimeoutException("Threads not finished in time! syncFromLocalSrcToRemoteThread.alive=" + syncFromLocalSrcToRemoteThread.isAlive() + " syncFromRemoteToLocalDestThread.alive=" + syncFromRemoteToLocalDestThread.isAlive());

		syncFromLocalSrcToRemoteCoordinator.throwErrorIfNeeded();
		syncFromRemoteToLocalDestCoordinator.throwErrorIfNeeded();

		System.out.println("************************************************************");
		System.out.println("************************************************************");
		System.out.println("SYNC AGAIN >>>");

		// sync *again* in order to make sure the collided file is uploaded now.
		syncFromLocalSrcToRemote();
		syncFromRemoteToLocalDest(false);
		syncFromLocalSrcToRemote(); // again, in case the collision was solved on the dest-side

		System.out.println("<<< SYNC AGAIN");
		System.out.println("************************************************************");
		System.out.println("************************************************************");

//		syncFromLocalSrcToRemote(); // should down-sync the change from dest-repo ... not needed! they're guaranteed to work in parallel using the ...Coordinator!
		assertDirectoriesAreEqualRecursively(
				(remotePathPrefix2Plain.isEmpty() ? getLocalRootWithPathPrefix() : createFile(getLocalRootWithPathPrefix(), remotePathPrefix2Plain)),
				localDestRoot);

		// Verify that *both* versions are in the history.
		List<PlainHistoCryptoRepoFileDto> plainHistoCryptoRepoFileDtos = getPlainHistoCryptoRepoFileDtos(localSrcRepoManagerLocal, file1);
		assertThat(plainHistoCryptoRepoFileDtos).hasSize(2);

//		// Both new versions should have the same previous version, because the collision happened on the server.
//		Set<Uid> previousHistoCryptoRepoFileIds = new HashSet<>();
//		for (PlainHistoCryptoRepoFileDto phcrfDto : plainHistoCryptoRepoFileDtos)
//			previousHistoCryptoRepoFileIds.add(phcrfDto.getHistoCryptoRepoFileDto().getPreviousHistoCryptoRepoFileId());
//
//		assertThat(previousHistoCryptoRepoFileIds).hasSize(1);

		// Verify that the older one is the previous version of the newer one (the list is sorted by timestamp).
		// Even though the collision happens on the server, the handling process ensures that they are consecutive
		// (rather than forked siblings of the same previous version).
		assertThat(plainHistoCryptoRepoFileDtos.get(0).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId())
		.isEqualTo(plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getPreviousHistoCryptoRepoFileId());

		// Verify that the 2nd version (with 222 at the end) is the current one.
		int lastByte1 = getLastByte(file1);
		int lastByte2 = getLastByte(file2);

		int expectedLastByteOfPreviousVersion;

		assertThat(lastByte1).isEqualTo(lastByte2);

		if (lastByte1 == 111)
			expectedLastByteOfPreviousVersion = 222;
		else if (lastByte1 == 222)
			expectedLastByteOfPreviousVersion = 111;
		else
			throw new IllegalStateException("lastByte is neither 111 nor 222, but: " + lastByte1);

		// Export both versions of the file and assert that
		// - the current file is identical to the last one
		// - and the first one ends on the other value (222, if current is 111; or 111, if current is 222).
		File tempDir0 = createTempDirectory(getClass().getSimpleName() + '.');
		File tempDir1 = createTempDirectory(getClass().getSimpleName() + '.');

		try (HistoExporter histoExporter = HistoExporterImpl.createHistoExporter(localSrcRoot);) {
			histoExporter.exportFile(new ExportFileParam(
					plainHistoCryptoRepoFileDtos.get(0).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId(), tempDir0));

			histoExporter.exportFile(new ExportFileParam(
					plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId(), tempDir1));
		}
		File histoFile0 = createFile(tempDir0, "new-file");
		File histoFile1 = createFile(tempDir1, "new-file");

		assertThat(IOUtil.compareFiles(histoFile1, file1)).isTrue();
		assertThat(IOUtil.compareFiles(histoFile0, histoFile1)).isFalse();

		int lastByteOfHistoFile0 = getLastByte(histoFile0);

		System.out.println("lastByteOfHistoFile0 = " + lastByteOfHistoFile0);
		System.out.println("lastByteOfHistoFile1 = " + lastByte1);

		assertThat(lastByteOfHistoFile0).isEqualTo(expectedLastByteOfPreviousVersion);

		collisionDtos = localRepoMetaData.getCollisionDtos(new CollisionFilter());
		assertThat(collisionDtos).hasSize(2);

		// exactly one of these two should have a duplicateCryptoRepoFileId assigned.
		getCollisionDtoWithDuplicateCryptoRepoFileIdOrFail(collisionDtos);
	}

	private class SyncFromLocalSrcToRemoteThread extends Thread {
		private final Logger logger = LoggerFactory.getLogger(SyncFromLocalSrcToRemoteThread.class);

		private final RepoToRepoSyncCoordinator coordinator;

		public SyncFromLocalSrcToRemoteThread(RepoToRepoSyncCoordinator coordinator) {
			this.coordinator = requireNonNull(coordinator, "coordinator");
		}

		@Override
		public void run() {
			coordinator.bindToCurrentThread();
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
			this.coordinator = requireNonNull(coordinator, "coordinator");
		}

		@Override
		public void run() {
			coordinator.bindToCurrentThread();
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
}
