package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.local.UserRepoKeyPublicKeyHelper;
import org.subshare.local.persistence.UserRepoKeyPublicKey;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.util.IOUtil;
import mockit.Mock;
import mockit.MockUp;

public class BackupRestoreRepoToRepoSyncIT extends AbstractRepoToRepoSyncIT {
	private static final Logger logger = LoggerFactory.getLogger(BackupRestoreRepoToRepoSyncIT.class);

	@BeforeClass
	public static void backupRestoreRepoToRepoSyncIT_beforeClass() {
		System.setProperty(LocalRepoManager.SYSTEM_PROPERTY_CLOSE_DEFERRED_MILLIS, "10000");
	}

	@AfterClass
	public static void backupRestoreRepoToRepoSyncIT_afterClass() {
		System.clearProperty(LocalRepoManager.SYSTEM_PROPERTY_CLOSE_DEFERRED_MILLIS);
	}

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
	public void backupRestoreClientRepo() throws Exception {
		// First set up all repos.
		syncFromLocalToRemoteToLocal();

		// Create a new file in the local source repository. This file is not yet synced,
		// hence it exists only in the source -- neither in the server, nor the destination.
		final File localSrc_child_2 = localSrcRoot.createFile("2");
		createFileWithRandomContent(localSrc_child_2, "zzz");

		final File localDest_child_2 = localDestRoot.createFile("2");
		File localDest_zzz = localDest_child_2.createFile("zzz");

		// Close all LocalRepoManager instances, before creating the backup!
		// We must close this stuff after creating the test file, because the super-class' file creation code
		// checks, whether we're inside an open repository.
		localSrcRepoManagerLocal.close();
		localSrcRepoManagerLocal = null;

		localDestRepoManagerLocal.close();
		localDestRepoManagerLocal = null;


		// Wait for the real managers to be closed. We don't set
		// SYSTEM_PROPERTY_CLOSE_DEFERRED_MILLIS to 0, because it makes this test really *too* slow.
		// Better wait here.
		waitForLocalRepoManagersToBeClosed();


		// Create a backup by simply copying the directory recursively.
		File localDestRootBackup = createFile(getTestRepositoryBaseDir(), localDestRoot.getName() + ".bak");
		logger.info("************************************************************************************");
		logger.info("Creating backup: '{}' => '{}'", localDestRoot, localDestRootBackup);
		IOUtil.copyDirectory(localDestRoot, localDestRootBackup);
		logger.info("Created backup: '{}' => '{}'", localDestRoot, localDestRootBackup);
		logger.info("************************************************************************************");

		// Now, check whether the scenario is correct, before the sync, i.e.
		// the new test-file "/3/zzz" does not exist locally (yet)!
		assertThat(localDest_zzz.existsNoFollow()).isFalse();

		// Synchronize -- this should download the test-file "/3/zzz" from the server to the client.
		syncFromLocalSrcToRemote();
		syncFromRemoteToLocalDest(); // this already checks, whether the test-file "/3/zzz" was correctly synced down.

		assertThat(localDest_zzz.existsNoFollow()).isTrue(); // just to make this explicit (the actual check was already done by the sync-method above).


		// Wait again for the real managers to be closed.
		waitForLocalRepoManagersToBeClosed();


		// Restore the backup, so that "/3/zzz" does not exist locally, again.
		logger.info("************************************************************************************");
		logger.info("Restoring backup: '{}' => '{}'", localDestRootBackup, localDestRoot);
		localDestRoot.deleteRecursively();
		IOUtil.copyDirectory(localDestRootBackup, localDestRoot);
		logger.info("Restored backup: '{}' => '{}'", localDestRootBackup, localDestRoot);
		logger.info("************************************************************************************");

		// Check, whether the test-file "/3/zzz" really does not exist locally.
		assertThat(localDest_zzz.existsNoFollow()).isFalse();

		// Sync again -- the test-file should be downloaded again, after
		// https://github.com/subshare/subshare/issues/53 was implemented.
		syncFromRemoteToLocalDest(); // this already checks, whether the test-file "/3/zzz" was correctly synced down.

		assertThat(localDest_zzz.existsNoFollow()).isTrue(); // just to make this explicit -- again ;-)
	}

	@Ignore("Not yet completely implemented!")
	@Test
	public void backupRestoreServerRepo() throws Exception {
		// First set up all repos.
		syncFromLocalToRemoteToLocal();

		// Create a new file in the local source repository. This file is not yet synced,
		// hence it exists only in the source -- neither in the server, nor the destination.
		final File localSrc_child_2 = localSrcRoot.createFile("2");
		createFileWithRandomContent(localSrc_child_2, "zzz");

		final File localDest_child_2 = localDestRoot.createFile("2");
		File localDest_zzz = localDest_child_2.createFile("zzz");

		// Close all LocalRepoManager instances, before creating the backup!
		// We must close this stuff after creating the test file, because the super-class' file creation code
		// checks, whether we're inside an open repository.
		localSrcRepoManagerLocal.close();
		localSrcRepoManagerLocal = null;

		localDestRepoManagerLocal.close();
		localDestRepoManagerLocal = null;


		// Wait for the real managers to be closed. We don't set
		// SYSTEM_PROPERTY_CLOSE_DEFERRED_MILLIS to 0, because it makes this test really *too* slow.
		// Better wait here.
		waitForLocalRepoManagersToBeClosed();


		// Create a backup by simply copying the directory recursively.
		File remoteRootBackup = createFile(getTestRepositoryBaseDir(), remoteRoot.getName() + ".bak");
		logger.info("************************************************************************************");
		logger.info("Creating backup: '{}' => '{}'", remoteRoot, remoteRootBackup);
		IOUtil.copyDirectory(remoteRoot, remoteRootBackup);
		logger.info("Created backup: '{}' => '{}'", remoteRoot, remoteRootBackup);
		logger.info("************************************************************************************");

		// We cannot *easily* look into the encrypted server repo, hence we use the dest-local-repo.
		// Thus, we backup this, too.
		File localDestRootBackup = createFile(getTestRepositoryBaseDir(), localDestRoot.getName() + ".bak");
		logger.info("************************************************************************************");
		logger.info("Creating backup: '{}' => '{}'", localDestRoot, localDestRootBackup);
		IOUtil.copyDirectory(localDestRoot, localDestRootBackup);
		logger.info("Created backup: '{}' => '{}'", localDestRoot, localDestRootBackup);
		logger.info("************************************************************************************");

		// Now, check whether the scenario is correct, before the sync, i.e.
		// the new test-file "/3/zzz" does not exist locally (yet)!
		assertThat(localDest_zzz.existsNoFollow()).isFalse();

		// Synchronize -- this should download the test-file "/3/zzz" from the server to the client.
		syncFromLocalSrcToRemote();
		syncFromRemoteToLocalDest(); // this already checks, whether the test-file "/3/zzz" was correctly synced down.

		assertThat(localDest_zzz.existsNoFollow()).isTrue(); // just to make this explicit (the actual check was already done by the sync-method above).


		// Wait again for the real managers to be closed.
		waitForLocalRepoManagersToBeClosed();


		// Restore the backup, so that "/3/zzz" does not exist locally, again.
		logger.info("************************************************************************************");
		logger.info("Restoring backup: '{}' => '{}'", remoteRootBackup, remoteRoot);
		remoteRoot.deleteRecursively();
		IOUtil.copyDirectory(remoteRootBackup, remoteRoot);
		logger.info("Restored backup: '{}' => '{}'", remoteRootBackup, remoteRoot);
		logger.info("************************************************************************************");

		logger.info("************************************************************************************");
		logger.info("Restoring backup: '{}' => '{}'", localDestRootBackup, localDestRoot);
		localDestRoot.deleteRecursively();
		IOUtil.copyDirectory(localDestRootBackup, localDestRoot);
		logger.info("Restored backup: '{}' => '{}'", localDestRootBackup, localDestRoot);
		logger.info("************************************************************************************");

		// Check, whether the test-file "/3/zzz" really does not exist locally.
		assertThat(localDest_zzz.existsNoFollow()).isFalse();

		// Sync again -- but only from remote to local-dest => the file should still be missing!
		syncFromRemoteToLocalDest(false);

		// Check that it really doesn't exist, yet.
		assertThat(localDest_zzz.existsNoFollow()).isFalse();


		// Sync again -- the test-file should be downloaded again, after
		// https://github.com/subshare/subshare/issues/53 was implemented.
		syncFromLocalSrcToRemote();
		syncFromRemoteToLocalDest(); // this already checks, whether the test-file "/3/zzz" was correctly synced down.

		assertThat(localDest_zzz.existsNoFollow()).isTrue(); // just to make this explicit -- again ;-)
	}

	protected void syncFromLocalToRemoteToLocal() throws Exception {
		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();
		determineRemotePathPrefix2Encrypted();
		createLocalDestinationRepo();
		syncFromRemoteToLocalDest();
	}

	protected void waitForLocalRepoManagersToBeClosed() throws Exception {
		logger.info("************************************************************************************");
		logger.info("Waiting for LocalRepoManager to be really closed.");
		Thread.sleep(20000);
	}
}
