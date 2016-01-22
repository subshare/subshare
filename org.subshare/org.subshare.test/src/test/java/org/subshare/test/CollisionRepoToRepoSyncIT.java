package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import mockit.Mock;
import mockit.MockUp;

import org.junit.Test;
import org.subshare.core.dto.HistoFrameDto;
import org.subshare.core.dto.PlainHistoCryptoRepoFileDto;
import org.subshare.core.dto.PlainHistoCryptoRepoFileDtoTreeNode;
import org.subshare.core.repo.histo.HistoExporterImpl;
import org.subshare.core.repo.local.HistoFrameFilter;
import org.subshare.core.repo.local.PlainHistoCryptoRepoFileFilter;
import org.subshare.core.repo.local.SsLocalRepoMetaData;
import org.subshare.local.UserRepoKeyPublicKeyHelper;
import org.subshare.local.persistence.UserRepoKeyPublicKey;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.util.IOUtil;

public class CollisionRepoToRepoSyncIT extends AbstractRepoToRepoSyncIT {

	@Override
	public void before() throws Exception {
		super.before();

		new MockUp<UserRepoKeyPublicKeyHelper>() {
			@Mock
			private void createUserIdentities(final UserRepoKeyPublicKey userRepoKeyPublicKey) {
				// Our mock should do nothing, because we don't have a real UserRegistry here.
			}
		};
	}

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

		HistoExporterImpl.createHistoExporter(localSrcRoot).exportFile(
				plainHistoCryptoRepoFileDtos.get(0).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId(), tempDir0);
		File histoFile0 = createFile(tempDir0, "new-file");

		HistoExporterImpl.createHistoExporter(localSrcRoot).exportFile(
				plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId(), tempDir1);
		File histoFile1 = createFile(tempDir1, "new-file");

		assertThat(IOUtil.compareFiles(histoFile1, file1)).isTrue();

		int lastByteOfHistoFile0 = getLastByte(histoFile0);
		assertThat(lastByteOfHistoFile0).isEqualTo(111);

		// TODO verify that a collision marker is in the meta-data!
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

		HistoExporterImpl.createHistoExporter(localSrcRoot).exportFile(
				plainHistoCryptoRepoFileDtos.get(1).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId(), tempDir1);
		File histoFile1 = createFile(tempDir1, "a");

		HistoExporterImpl.createHistoExporter(localSrcRoot).exportFile(
				plainHistoCryptoRepoFileDtos.get(2).getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId(), tempDir2);
		File histoFile2 = createFile(tempDir2, "a");

		IOUtil.copyFile(file1, tempDir2.createFile("a.current"));
		assertThat(IOUtil.compareFiles(histoFile2, file1)).isTrue();

		int lastByteOfHistoFile1 = getLastByte(histoFile1);
		assertThat(lastByteOfHistoFile1).isEqualTo(111);

		// TODO verify that a collision marker is in the meta-data!
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
//
//	/**
//	 * Two clients simultaneously create a file with the same name in the same directory.
//	 * <p>
//	 * In contrast to {@link #newVsNewFileCollisionOnClient()}, both clients first sync-down,
//	 * see no change on the server and then sync-up really causing two different
//	 * {@code CryptoRepoFile} objects for the same file.
//	 * <p>
//	 * The collision thus happens and is detected during a following down-sync at a later time.
//	 *
//	 * @see #newVsNewFileCollisionOnClient()
//	 */
//	@Test
//	public void newFileVsNewFileCollisionOnServer() throws Exception {
//
////		// Both new versions should have the same previous version, because the collision happened on the server.
////		Set<Uid> previousHistoCryptoRepoFileIds = new HashSet<>();
////		for (PlainHistoCryptoRepoFileDto phcrfDto : plainHistoCryptoRepoFileDtos)
////			previousHistoCryptoRepoFileIds.add(phcrfDto.getHistoCryptoRepoFileDto().getPreviousHistoCryptoRepoFileId());
////
////		assertThat(previousHistoCryptoRepoFileIds).hasSize(1);
//
//
//	}
//
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
