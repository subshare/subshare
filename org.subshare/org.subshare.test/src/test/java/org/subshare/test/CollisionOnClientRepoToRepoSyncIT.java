package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Collection;
import java.util.List;

import mockit.integration.junit4.JMockit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.dto.CollisionDto;
import org.subshare.core.dto.PlainHistoCryptoRepoFileDto;
import org.subshare.core.repo.histo.HistoExporter;
import org.subshare.core.repo.histo.HistoExporterImpl;
import org.subshare.core.repo.local.CollisionFilter;
import org.subshare.core.repo.local.SsLocalRepoMetaData;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.IOUtil;

@RunWith(JMockit.class)
public class CollisionOnClientRepoToRepoSyncIT extends CollisionRepoToRepoSyncIT {
	private static final Logger logger = LoggerFactory.getLogger(CollisionOnClientRepoToRepoSyncIT.class);

	/**
	 * Two clients simultaneously create a file with the same name in the same directory.
	 * <p>
	 * The 1st client syncs completely, first. Then the 2nd client syncs completely. Thus,
	 * the collision happens during the down-sync on the 2nd client.
	 *
	 * @see CollisionOnServerRepoToRepoSyncIT#newFileVsNewFileUploadedCollisionOnServer()
	 * @see CollisionOnServerRepoToRepoSyncIT#newFileVsNewFileUploadingCollisionOnServer()
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

		// Verify that *no* version is in the history, yet. The file is new!
		List<PlainHistoCryptoRepoFileDto> plainHistoCryptoRepoFileDtos = getPlainHistoCryptoRepoFileDtos(localSrcRepoManagerLocal, file1);
		assertThat(plainHistoCryptoRepoFileDtos).hasSize(0);
		plainHistoCryptoRepoFileDtos = getPlainHistoCryptoRepoFileDtos(localDestRepoManagerLocal, file2);
		assertThat(plainHistoCryptoRepoFileDtos).hasSize(0);

		// Verify that there is *no* collision, yet.
		SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localSrcRepoManagerLocal.getLocalRepoMetaData();
		Collection<CollisionDto> collisionDtos = localRepoMetaData.getCollisionDtos(new CollisionFilter());
		assertThat(collisionDtos).isEmpty();

		syncFromLocalSrcToRemote();
		syncFromRemoteToLocalDest(false); // should up-sync its own version
		syncFromLocalSrcToRemote(); // should down-sync the change from dest-repo
		assertDirectoriesAreEqualRecursively(
				(remotePathPrefix2Plain.isEmpty() ? getLocalRootWithPathPrefix() : createFile(getLocalRootWithPathPrefix(), remotePathPrefix2Plain)),
				localDestRoot);

		// Verify that *both* versions are in the history.
		plainHistoCryptoRepoFileDtos = getPlainHistoCryptoRepoFileDtos(localSrcRepoManagerLocal, file1);
		assertThat(plainHistoCryptoRepoFileDtos).hasSize(2);

		// Verify that the older one is the previous version of the newer one (the list is sorted by timestamp).
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

		// Verify that there is exactly one collision.
		collisionDtos = localRepoMetaData.getCollisionDtos(new CollisionFilter());
		assertThat(collisionDtos).hasSize(1);

		// Verify that this collision is correct.
		CollisionDto collisionDto = collisionDtos.iterator().next();
		assertThat(collisionDto.getDuplicateCryptoRepoFileId()).isNull();

		assertThat(collisionDto.getHistoCryptoRepoFileId1())
		.isEqualTo(plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId());

		assertThat(collisionDto.getHistoCryptoRepoFileId2())
		.isEqualTo(plainHistoCryptoRepoFileDtos.get(0).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId());
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

		// Verify that *one* version is in the history.
		List<PlainHistoCryptoRepoFileDto> plainHistoCryptoRepoFileDtos = getPlainHistoCryptoRepoFileDtos(localSrcRepoManagerLocal, file1);
		assertThat(plainHistoCryptoRepoFileDtos).hasSize(1);
		plainHistoCryptoRepoFileDtos = getPlainHistoCryptoRepoFileDtos(localDestRepoManagerLocal, file2);
		assertThat(plainHistoCryptoRepoFileDtos).hasSize(1);

		// Verify that there is *no* collision, yet.
		SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localSrcRepoManagerLocal.getLocalRepoMetaData();
		Collection<CollisionDto> collisionDtos = localRepoMetaData.getCollisionDtos(new CollisionFilter());
		assertThat(collisionDtos).isEmpty();

		syncFromLocalSrcToRemote();
		syncFromRemoteToLocalDest(false); // should up-sync its own version
		syncFromLocalSrcToRemote(); // should down-sync the change from dest-repo
		assertDirectoriesAreEqualRecursively(
				(remotePathPrefix2Plain.isEmpty() ? getLocalRootWithPathPrefix() : createFile(getLocalRootWithPathPrefix(), remotePathPrefix2Plain)),
				localDestRoot);

		// Verify that *both* versions are in the history.
		plainHistoCryptoRepoFileDtos = getPlainHistoCryptoRepoFileDtos(localSrcRepoManagerLocal, file1);
		assertThat(plainHistoCryptoRepoFileDtos).hasSize(3);

		// Verify that the older one is the previous version of the newer one (the list is sorted by timestamp).
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

		assertThat(IOUtil.compareFiles(histoFile2, file1)).isTrue();
		assertThat(IOUtil.compareFiles(histoFile1, histoFile2)).isFalse();

		int lastByteOfHistoFile1 = getLastByte(histoFile1);
		assertThat(lastByteOfHistoFile1).isEqualTo(111);

		collisionDtos = localRepoMetaData.getCollisionDtos(new CollisionFilter());
		assertThat(collisionDtos).hasSize(1);

		// Verify that this collision is correct.
		CollisionDto collisionDto = collisionDtos.iterator().next();
		assertThat(collisionDto.getDuplicateCryptoRepoFileId()).isNull();

		assertThat(collisionDto.getHistoCryptoRepoFileId1())
		.isEqualTo(plainHistoCryptoRepoFileDtos.get(2).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId());

		assertThat(collisionDto.getHistoCryptoRepoFileId2())
		.isEqualTo(plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId());
	}

	/**
	 * The first client deletes the file, uploads to the server, the 2nd client modifies it
	 * and then syncs.
	 */
	@Test
	public void deletedFileVsModifiedFileCollisionOnClient() throws Exception {
		prepareLocalAndDestinationRepo();

		File file1 = createFile(localSrcRoot, "2", "a");
		assertThat(file1.getIoFile()).isFile();
		file1.delete();

		File file2 = createFile(localDestRoot, "2", "a");
		assertThat(file2.getIoFile()).isFile();
		modifyFile_append(file2, 222);

		// Verify that *one* version is in the history.
		List<PlainHistoCryptoRepoFileDto> plainHistoCryptoRepoFileDtos = getPlainHistoCryptoRepoFileDtos(localSrcRepoManagerLocal, file1);
		assertThat(plainHistoCryptoRepoFileDtos).hasSize(1);
		plainHistoCryptoRepoFileDtos = getPlainHistoCryptoRepoFileDtos(localDestRepoManagerLocal, file2);
		assertThat(plainHistoCryptoRepoFileDtos).hasSize(1);

		// Verify that there is no collision, yet.
		SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localSrcRepoManagerLocal.getLocalRepoMetaData();
		Collection<CollisionDto> collisionDtos = localRepoMetaData.getCollisionDtos(new CollisionFilter());
		assertThat(collisionDtos).isEmpty();

		syncFromLocalSrcToRemote();
		syncFromRemoteToLocalDest(false); // should up-sync its own version
		syncFromLocalSrcToRemote(); // should down-sync the change from dest-repo
		assertDirectoriesAreEqualRecursively(
				(remotePathPrefix2Plain.isEmpty() ? getLocalRootWithPathPrefix() : createFile(getLocalRootWithPathPrefix(), remotePathPrefix2Plain)),
				localDestRoot);

		// Verify that *both* versions are in the history - one is the deletion itself (modeled as a HistoCryptoRepoFile).
		plainHistoCryptoRepoFileDtos = getPlainHistoCryptoRepoFileDtos(localSrcRepoManagerLocal, file1);
		assertThat(plainHistoCryptoRepoFileDtos).hasSize(3);

		// Verify that the older one is the previous version of the newer one (the list is sorted by timestamp).
		assertThat(plainHistoCryptoRepoFileDtos.get(0).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId())
		.isEqualTo(plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getPreviousHistoCryptoRepoFileId());

		assertThat(plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId())
		.isEqualTo(plainHistoCryptoRepoFileDtos.get(2).getHistoCryptoRepoFileDto().getPreviousHistoCryptoRepoFileId());

		// Verify deleted status.
		assertThat(plainHistoCryptoRepoFileDtos.get(0).getHistoCryptoRepoFileDto().getDeleted()).isNull();
		assertThat(plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getDeleted()).isNotNull();
		assertThat(plainHistoCryptoRepoFileDtos.get(2).getHistoCryptoRepoFileDto().getDeleted()).isNull();

		// Verify that the 2nd version (with 222 at the end) is the current one.
		int lastByte1 = getLastByte(file1);
		assertThat(lastByte1).isEqualTo(222);

		int lastByte2 = getLastByte(file2);
		assertThat(lastByte2).isEqualTo(222);

		// Export both versions of the file and assert that
		// - the current file is identical to the last one
		// - and the first one ends on 111.
		File tempDir0 = createTempDirectory(getClass().getSimpleName() + '.');
		File tempDir2 = createTempDirectory(getClass().getSimpleName() + '.');

		try (HistoExporter histoExporter = HistoExporterImpl.createHistoExporter(localSrcRoot);) {
			histoExporter.exportFile(
					plainHistoCryptoRepoFileDtos.get(0).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId(), tempDir0);

			histoExporter.exportFile(
					plainHistoCryptoRepoFileDtos.get(2).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId(), tempDir2);
		}
		File histoFile0 = createFile(tempDir0, "a");
		File histoFile2 = createFile(tempDir2, "a");

		assertThat(IOUtil.compareFiles(histoFile2, file1)).isTrue();
		assertThat(IOUtil.compareFiles(histoFile0, histoFile2)).isFalse();

		assertThat(histoFile0.length() + 1).isEqualTo(file1.length());

		collisionDtos = localRepoMetaData.getCollisionDtos(new CollisionFilter());
		assertThat(collisionDtos).hasSize(1);

		// Verify that this collision is correct.
		CollisionDto collisionDto = collisionDtos.iterator().next();
		assertThat(collisionDto.getDuplicateCryptoRepoFileId()).isNull();

		assertThat(collisionDto.getHistoCryptoRepoFileId1())
		.isEqualTo(plainHistoCryptoRepoFileDtos.get(2).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId());

		assertThat(collisionDto.getHistoCryptoRepoFileId2())
		.isEqualTo(plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId());
	}

	/**
	 * The first client modifies the file, uploads to the server, the 2nd client deletes it
	 * and then syncs.
	 */
	@Test
	public void modifiedFileVsDeletedFileCollisionOnClient() throws Exception {
		prepareLocalAndDestinationRepo();

		File file1 = createFile(localSrcRoot, "2", "a");
		assertThat(file1.getIoFile()).isFile();
		modifyFile_append(file1, 111);

		File file2 = createFile(localDestRoot, "2", "a");
		assertThat(file2.getIoFile()).isFile();
		file2.delete();
		assertThat(file2.getIoFile()).doesNotExist();

		// Verify that *one* version is in the history.
		List<PlainHistoCryptoRepoFileDto> plainHistoCryptoRepoFileDtos = getPlainHistoCryptoRepoFileDtos(localSrcRepoManagerLocal, file1);
		assertThat(plainHistoCryptoRepoFileDtos).hasSize(1);
		plainHistoCryptoRepoFileDtos = getPlainHistoCryptoRepoFileDtos(localDestRepoManagerLocal, file2);
		assertThat(plainHistoCryptoRepoFileDtos).hasSize(1);

		// Verify that there is no collision, yet.
		SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localSrcRepoManagerLocal.getLocalRepoMetaData();
		Collection<CollisionDto> collisionDtos = localRepoMetaData.getCollisionDtos(new CollisionFilter());
		assertThat(collisionDtos).isEmpty();

		syncFromLocalSrcToRemote();
		syncFromRemoteToLocalDest(false); // should up-sync its own version
		syncFromLocalSrcToRemote(); // should down-sync the change from dest-repo
		assertDirectoriesAreEqualRecursively(
				(remotePathPrefix2Plain.isEmpty() ? getLocalRootWithPathPrefix() : createFile(getLocalRootWithPathPrefix(), remotePathPrefix2Plain)),
				localDestRoot);

		// Verify that *both* versions are in the history - one is the deletion itself (modeled as a HistoCryptoRepoFile).
		plainHistoCryptoRepoFileDtos = getPlainHistoCryptoRepoFileDtos(localSrcRepoManagerLocal, file1);
		assertThat(plainHistoCryptoRepoFileDtos).hasSize(3);

		// Verify that the older one is the previous version of the newer one (the list is sorted by timestamp).
		// Since the collision happens on the client, they are consecutive (rather than forked siblings of the same previous version).
		assertThat(plainHistoCryptoRepoFileDtos.get(0).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId())
		.isEqualTo(plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getPreviousHistoCryptoRepoFileId());

		assertThat(plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId())
		.isEqualTo(plainHistoCryptoRepoFileDtos.get(2).getHistoCryptoRepoFileDto().getPreviousHistoCryptoRepoFileId());

		// Verify deleted status.
		assertThat(plainHistoCryptoRepoFileDtos.get(0).getHistoCryptoRepoFileDto().getDeleted()).isNull();
		assertThat(plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getDeleted()).isNull();
		assertThat(plainHistoCryptoRepoFileDtos.get(2).getHistoCryptoRepoFileDto().getDeleted()).isNotNull();

		// Verify that the deletion is the current state - in both working copies.
		assertThat(file1.getIoFile()).doesNotExist();
		assertThat(file2.getIoFile()).doesNotExist();

		// Export both versions of the file and assert that
		// - the current file is identical to the last one
		// - and the first one ends on 111.
		File tempDir0 = createTempDirectory(getClass().getSimpleName() + '.');
		File tempDir2 = createTempDirectory(getClass().getSimpleName() + '.');


		try (HistoExporter histoExporter = HistoExporterImpl.createHistoExporter(localSrcRoot);) {
			histoExporter.exportFile(
					plainHistoCryptoRepoFileDtos.get(0).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId(), tempDir0);

			histoExporter.exportFile(
					plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId(), tempDir2);
		}
		File histoFile0 = createFile(tempDir0, "a");
		File histoFile1 = createFile(tempDir2, "a");

		assertThat(IOUtil.compareFiles(histoFile0, histoFile1)).isFalse();

		assertThat(histoFile0.length() + 1).isEqualTo(histoFile1.length());

		collisionDtos = localRepoMetaData.getCollisionDtos(new CollisionFilter());
		assertThat(collisionDtos).hasSize(1);

		// Verify that this collision is correct.
		CollisionDto collisionDto = collisionDtos.iterator().next();
		assertThat(collisionDto.getDuplicateCryptoRepoFileId()).isNull();

		assertThat(collisionDto.getHistoCryptoRepoFileId1())
		.isEqualTo(plainHistoCryptoRepoFileDtos.get(2).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId());

		assertThat(collisionDto.getHistoCryptoRepoFileId2())
		.isEqualTo(plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId());
	}

	/**
	 * Two clients simultaneously create a directory with the same name in the same directory.
	 * <p>
	 * The 1st client syncs completely, first. Then the 2nd client syncs completely. Thus,
	 * the collision happens during the down-sync on the 2nd client.
	 */
	@Test
	public void newDirectoryVsNewDirectoryCollisionOnClient() throws Exception {
		prepareLocalAndDestinationRepo();

		File dir1 = createFile(localSrcRoot, "2", "new-dir");
		createDirectory(dir1);
		dir1.setLastModified(10101);

		File dir2 = createFile(localDestRoot, "2", "new-dir");
		createDirectory(dir2);
		dir2.setLastModified(20202);

		// Verify that *no* version is in the history, yet. The file is new!
		List<PlainHistoCryptoRepoFileDto> plainHistoCryptoRepoFileDtos = getPlainHistoCryptoRepoFileDtos(localSrcRepoManagerLocal, dir1);
		assertThat(plainHistoCryptoRepoFileDtos).hasSize(0);
		plainHistoCryptoRepoFileDtos = getPlainHistoCryptoRepoFileDtos(localDestRepoManagerLocal, dir2);
		assertThat(plainHistoCryptoRepoFileDtos).hasSize(0);

		// Verify that there is *no* collision, yet.
		SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localSrcRepoManagerLocal.getLocalRepoMetaData();
		Collection<CollisionDto> collisionDtos = localRepoMetaData.getCollisionDtos(new CollisionFilter());
		assertThat(collisionDtos).isEmpty();

		syncFromLocalSrcToRemote();
		syncFromRemoteToLocalDest(false); // should up-sync its own version
		syncFromLocalSrcToRemote(); // should down-sync the change from dest-repo
		assertDirectoriesAreEqualRecursively(
				(remotePathPrefix2Plain.isEmpty() ? getLocalRootWithPathPrefix() : createFile(getLocalRootWithPathPrefix(), remotePathPrefix2Plain)),
				localDestRoot);

//		// Verify that *both* versions are in the history.
//		plainHistoCryptoRepoFileDtos = getPlainHistoCryptoRepoFileDtos(localSrcRepoManagerLocal, dir1);
//		assertThat(plainHistoCryptoRepoFileDtos).hasSize(2);
//
//		// Verify that the older one is the previous version of the newer one (the list is sorted by timestamp).
//		assertThat(plainHistoCryptoRepoFileDtos.get(0).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId())
//		.isEqualTo(plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getPreviousHistoCryptoRepoFileId());
//
//		long lastModified0 = plainHistoCryptoRepoFileDtos.get(0).getRepoFileDto().getLastModified().getTime();
//		long lastModified1 = plainHistoCryptoRepoFileDtos.get(1).getRepoFileDto().getLastModified().getTime();
//
//		assertThat(lastModified0).isEqualTo(10101);
//		assertThat(lastModified1).isEqualTo(20202);
//
//		// Verify that the 2nd version is the current one.
//		assertThat(dir1.lastModified()).isEqualTo(20202);
//		assertThat(dir2.lastModified()).isEqualTo(dir1.lastModified());
//
//		// Verify that there is exactly one collision.
//		collisionDtos = localRepoMetaData.getCollisionDtos(new CollisionFilter());
//		assertThat(collisionDtos).hasSize(1);
//
//		// Verify that this collision is correct.
//		CollisionDto collisionDto = collisionDtos.iterator().next();
//		assertThat(collisionDto.getDuplicateCryptoRepoFileId()).isNull();
//
//		assertThat(collisionDto.getHistoCryptoRepoFileId1())
//		.isEqualTo(plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId());
//
//		assertThat(collisionDto.getHistoCryptoRepoFileId2())
//		.isEqualTo(plainHistoCryptoRepoFileDtos.get(0).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId());

		// Collisions of directory timestamps are not assumed to be real collisions. We currently ignore them.
		// Hence, this should *not* qualify as a collision - there should be no change - the new version is silently
		// discarded (and the timestamp from the first repo used).

		// Verify that *one* version is in the history.
		plainHistoCryptoRepoFileDtos = getPlainHistoCryptoRepoFileDtos(localSrcRepoManagerLocal, dir1);
		assertThat(plainHistoCryptoRepoFileDtos).hasSize(1);

		long lastModified0 = plainHistoCryptoRepoFileDtos.get(0).getRepoFileDto().getLastModified().getTime();
		assertThat(lastModified0).isEqualTo(10101);

		// Verify that dirs in both working copies have the same timestamp.
		assertThat(dir1.lastModified()).isEqualTo(10101);
		assertThat(dir2.lastModified()).isEqualTo(dir1.lastModified());

		// And check that there's still no collision.
		collisionDtos = localRepoMetaData.getCollisionDtos(new CollisionFilter());
		assertThat(collisionDtos).isEmpty();
	}
}
