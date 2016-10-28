package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
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

import co.codewizards.cloudstore.core.oio.File;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class SymlinkCollisionOnClientRepoToRepoSyncIT extends CollisionRepoToRepoSyncIT {
	private static final Logger logger = LoggerFactory.getLogger(SymlinkCollisionOnClientRepoToRepoSyncIT.class);

	/**
	 * Two clients simultaneously create a file and a symlink respectively with the same name in the same directory.
	 * <p>
	 * The 1st client syncs completely, first. Then the 2nd client syncs completely. Thus,
	 * the collision happens during the down-sync on the 2nd client.
	 */
	@Test
	public void newFileVsNewSymlinkCollisionOnClient() throws Exception {
		prepareLocalAndDestinationRepo();

		File file1 = createFile(localSrcRoot, "2", "new-file");
		createFileWithRandomContent(file1);
		modifyFile_append(file1, 111);

		File file2 = createFile(localDestRoot, "2", "new-file");
		File fileA = createFile(localDestRoot, "2", "a");
		assertThat(fileA.getIoFile()).exists();
		createRelativeSymlink(file2, fileA);

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
		syncFromRemoteToLocalDest(false); // should up-sync the symlink
		syncFromLocalSrcToRemote(); // should down-sync the symlink from dest-repo
		assertDirectoriesAreEqualRecursively(
				(remotePathPrefix2Plain.isEmpty() ? getLocalRootWithPathPrefix() : createFile(getLocalRootWithPathPrefix(), remotePathPrefix2Plain)),
				localDestRoot);

		// Verify that *both* versions are in the history.
		plainHistoCryptoRepoFileDtos = getPlainHistoCryptoRepoFileDtos(localSrcRepoManagerLocal, file1);
		assertThat(plainHistoCryptoRepoFileDtos).hasSize(2);

		// Verify that the older one is the previous version of the newer one (the list is sorted by timestamp).
		assertThat(plainHistoCryptoRepoFileDtos.get(0).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId())
		.isEqualTo(plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getPreviousHistoCryptoRepoFileId());

		// Verify that the 2nd version (the symlink) is the current one.
		assertThat(file1.isSymbolicLink()).isTrue();
		assertThat(file1.readSymbolicLinkToPathString()).isEqualTo("a");

		assertThat(file2.isSymbolicLink()).isTrue();
		assertThat(file2.readSymbolicLinkToPathString()).isEqualTo("a");

		// Export both versions of the file and assert that
		// - the current file is identical to the last one
		// - and the first one ends on 111.
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

		assertThat(histoFile1.isSymbolicLink()).isTrue();
		assertThat(histoFile1.readSymbolicLinkToPathString()).isEqualTo("a");

		assertThat(histoFile0.isSymbolicLink()).isFalse();

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

//	@Test
//	public void modifiedFileVsNewSymlinkCollisionOnClient() throws Exception {
//		prepareLocalAndDestinationRepo();
//
//		File file1 = createFile(localSrcRoot, "2", "a");
//		assertThat(file1.getIoFile()).isFile();
//		modifyFile_append(file1, 111);
//
//		File file2 = createFile(localDestRoot, "2", "a");
//		assertThat(file2.getIoFile()).isFile();
//		modifyFile_append(file2, 222);
//
//		// Verify that *one* version is in the history.
//		List<PlainHistoCryptoRepoFileDto> plainHistoCryptoRepoFileDtos = getPlainHistoCryptoRepoFileDtos(localSrcRepoManagerLocal, file1);
//		assertThat(plainHistoCryptoRepoFileDtos).hasSize(1);
//		plainHistoCryptoRepoFileDtos = getPlainHistoCryptoRepoFileDtos(localDestRepoManagerLocal, file2);
//		assertThat(plainHistoCryptoRepoFileDtos).hasSize(1);
//
//		// Verify that there is *no* collision, yet.
//		SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localSrcRepoManagerLocal.getLocalRepoMetaData();
//		Collection<CollisionDto> collisionDtos = localRepoMetaData.getCollisionDtos(new CollisionFilter());
//		assertThat(collisionDtos).isEmpty();
//
//		syncFromLocalSrcToRemote();
//		syncFromRemoteToLocalDest(false); // should up-sync its own version
//		syncFromLocalSrcToRemote(); // should down-sync the change from dest-repo
//		assertDirectoriesAreEqualRecursively(
//				(remotePathPrefix2Plain.isEmpty() ? getLocalRootWithPathPrefix() : createFile(getLocalRootWithPathPrefix(), remotePathPrefix2Plain)),
//				localDestRoot);
//
//		// Verify that *both* versions are in the history.
//		plainHistoCryptoRepoFileDtos = getPlainHistoCryptoRepoFileDtos(localSrcRepoManagerLocal, file1);
//		assertThat(plainHistoCryptoRepoFileDtos).hasSize(3);
//
//		// Verify that the older one is the previous version of the newer one (the list is sorted by timestamp).
//		assertThat(plainHistoCryptoRepoFileDtos.get(0).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId())
//		.isEqualTo(plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getPreviousHistoCryptoRepoFileId());
//
//		assertThat(plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId())
//		.isEqualTo(plainHistoCryptoRepoFileDtos.get(2).getHistoCryptoRepoFileDto().getPreviousHistoCryptoRepoFileId());
//
//		// Verify that the 2nd version (with 222 at the end) is the current one.
//		int lastByte1 = getLastByte(file1);
//		assertThat(lastByte1).isEqualTo(222);
//
//		int lastByte2 = getLastByte(file2);
//		assertThat(lastByte2).isEqualTo(222);
//
//		// Export both versions of the file and assert that
//		// - the current file is identical to the last one
//		// - and the first one ends on 111.
//		File tempDir1 = createTempDirectory(getClass().getSimpleName() + '.');
//		File tempDir2 = createTempDirectory(getClass().getSimpleName() + '.');
//
//		try (HistoExporter histoExporter = HistoExporterImpl.createHistoExporter(localSrcRoot);) {
//			histoExporter.exportFile(
//					plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId(), tempDir1);
//
//			histoExporter.exportFile(
//					plainHistoCryptoRepoFileDtos.get(2).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId(), tempDir2);
//		}
//		File histoFile1 = createFile(tempDir1, "a");
//		File histoFile2 = createFile(tempDir2, "a");
//
//		assertThat(IOUtil.compareFiles(histoFile2, file1)).isTrue();
//		assertThat(IOUtil.compareFiles(histoFile1, histoFile2)).isFalse();
//
//		int lastByteOfHistoFile1 = getLastByte(histoFile1);
//		assertThat(lastByteOfHistoFile1).isEqualTo(111);
//
//		collisionDtos = localRepoMetaData.getCollisionDtos(new CollisionFilter());
//		assertThat(collisionDtos).hasSize(1);
//
//		// Verify that this collision is correct.
//		CollisionDto collisionDto = collisionDtos.iterator().next();
//		assertThat(collisionDto.getDuplicateCryptoRepoFileId()).isNull();
//
//		assertThat(collisionDto.getHistoCryptoRepoFileId1())
//		.isEqualTo(plainHistoCryptoRepoFileDtos.get(2).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId());
//
//		assertThat(collisionDto.getHistoCryptoRepoFileId2())
//		.isEqualTo(plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId());
//	}
}