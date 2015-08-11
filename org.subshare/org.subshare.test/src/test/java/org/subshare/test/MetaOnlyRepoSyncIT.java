package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;
import mockit.Mock;
import mockit.MockUp;

import org.subshare.core.repo.local.SsLocalRepoMetaData;
import org.subshare.local.UserRepoKeyPublicKeyHelper;
import org.subshare.local.persistence.UserRepoKeyPublicKey;
import org.junit.Test;

import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;

public class MetaOnlyRepoSyncIT extends AbstractRepoToRepoSyncIT {

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

	@Test
	public void syncFromLocalToRemoteToMetaOnly() throws Exception {
		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();
		determineRemotePathPrefix2Encrypted();
		createLocalDestinationRepo();
		makeMetaOnly(localDestRoot);

		// check whether there are no RepoFiles in the DB, yet
		assertThat(getRepoFileDto(localDestRoot, "1 {11 11ä11} 1")).isNull();
		assertThat(getRepoFileDto(localDestRoot, "1 {11 11ä11} 1/a")).isNull();
		assertThat(getRepoFileDto(localDestRoot, "2")).isNull();

		syncFromRemoteToLocalDest(false);

		// check whether the directory is empty (except for the meta-directory)
		File[] files = localDestRoot.listFiles();
		assertThat(files).hasSize(1);
		assertThat(files[0].getName()).isEqualTo(LocalRepoManager.META_DIR_NAME);

		// check whether the RepoFiles now exist in the DB.
		assertThat(getRepoFileDto(localDestRoot, "")).isNotNull();
		assertThat(getRepoFileDto(localDestRoot, "1 {11 11ä11} 1")).isNotNull();
		assertThat(getRepoFileDto(localDestRoot, "1 {11 11ä11} 1/a")).isNotNull();
		assertThat(getRepoFileDto(localDestRoot, "1 {11 11ä11} 1/b")).isNotNull();
		assertThat(getRepoFileDto(localDestRoot, "1 {11 11ä11} 1/c")).isNotNull();
		assertThat(getRepoFileDto(localDestRoot, "2")).isNotNull();
	}

	@Test
	public void syncFromLocalToRemoteToMetaOnlyAddingFiles() throws Exception {
		syncFromLocalToRemoteToMetaOnly();

		final File child_1 = createFile(localSrcRoot, "1 {11 11ä11} 1");
		createFileWithRandomContent(child_1, "000");

		File child_xxx = createDirectory(localSrcRoot, "xxx");
		createFileWithRandomContent(child_xxx, "001");
		createFileWithRandomContent(child_xxx, "002");

		syncFromLocalSrcToRemote();

		// check whether there are no RepoFiles in the DB, yet
		assertThat(getRepoFileDto(localDestRoot, "1 {11 11ä11} 1/000")).isNull();
		assertThat(getRepoFileDto(localDestRoot, "xxx/001")).isNull();
		assertThat(getRepoFileDto(localDestRoot, "xxx/002")).isNull();

		syncFromRemoteToLocalDest(false);

		// check whether the RepoFiles now exist in the DB.
		assertThat(getRepoFileDto(localDestRoot, "1 {11 11ä11} 1/000")).isNotNull();
		assertThat(getRepoFileDto(localDestRoot, "xxx/001")).isNotNull();
		assertThat(getRepoFileDto(localDestRoot, "xxx/002")).isNotNull();
	}

	@Test
	public void syncFromLocalToRemoteToMetaOnlyRemovingFiles() throws Exception {
		syncFromLocalToRemoteToMetaOnly();

		// delete locally
		createFile(localSrcRoot, "1 {11 11ä11} 1", "a").delete();
		createFile(localSrcRoot, "2").deleteRecursively();

		// up-sync + down-sync to meta-only
		syncFromLocalSrcToRemote();
		syncFromRemoteToLocalDest(false);

		// check whether the RepoFiles are deleted from the DB.
		assertThat(getRepoFileDto(localDestRoot, "1 {11 11ä11} 1/a")).isNull();
		assertThat(getRepoFileDto(localDestRoot, "2")).isNull();
	}

	private void makeMetaOnly(File localRoot) {
		try (final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);) {
			SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();
			localRepoMetaData.makeMetaOnly();
		}
	}

	private RepoFileDto getRepoFileDto(File localRoot, String path) {
		try (final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);) {
			SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();
			RepoFileDto repoFileDto = localRepoMetaData.getRepoFileDto(path, 0);
			return repoFileDto;
		}
	}

}
