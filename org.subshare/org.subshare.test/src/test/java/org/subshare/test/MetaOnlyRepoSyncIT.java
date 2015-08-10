package org.subshare.test;

import static org.assertj.core.api.Assertions.*;
import mockit.Mock;
import mockit.MockUp;

import org.subshare.core.repo.local.SsLocalRepoMetaData;
import org.subshare.local.UserRepoKeyPublicKeyHelper;
import org.subshare.local.persistence.UserRepoKeyPublicKey;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;

public class MetaOnlyRepoSyncIT extends AbstractRepoToRepoSyncIT {

	private static final Logger logger = LoggerFactory.getLogger(RepoToRepoSyncIT.class);

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
		syncFromRemoteToLocalDest(false);

		// check whether the directory is empty (except for the meta-directory)
		File[] files = localDestRoot.listFiles();
		assertThat(files).hasSize(1);
		assertThat(files[0].getName()).isEqualTo(LocalRepoManager.META_DIR_NAME);

		// TODO check whether the meta-data is down-synced correctly!
	}

	private void makeMetaOnly(File localRoot) {
		try (final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);) {
			SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();
			localRepoMetaData.makeMetaOnly();
		}
	}

}
