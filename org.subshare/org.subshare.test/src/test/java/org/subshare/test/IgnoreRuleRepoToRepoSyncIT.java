package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.subshare.local.UserRepoKeyPublicKeyHelper;
import org.subshare.local.persistence.UserRepoKeyPublicKey;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.PropertiesUtil;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class IgnoreRuleRepoToRepoSyncIT extends AbstractRepoToRepoSyncIT {

	@Override
	public void before() throws Exception {
		super.before();

		new MockUp<UserRepoKeyPublicKeyHelper>() {
			@Mock
			void createUserIdentities(UserRepoKeyPublicKey userRepoKeyPublicKey) {
				// Our mock should do nothing, because we don't have a real UserRegistry here.
			}
		};
	}

	@Test
	public void ignoreRulesExistBeforeAffectedFiles() throws Exception {
		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();

		// Create ignore rules.
		Properties properties = new Properties();
		properties.put("ignore[file1].namePattern", "file1");
		properties.put("ignore[dir1].namePattern", "dir1");
		PropertiesUtil.store(createFile(localSrcRoot, ".subshare.properties"), properties, null);

		File src_dir2 = createFile(localSrcRoot, "2");
		assertThat(src_dir2.getIoFile()).isDirectory();

		File src_file1 = createFileWithRandomContent(src_dir2, "file1");
		File src_dir1 = createDirectory(localSrcRoot, "dir1");
		File src_dir1_aa1 = createFileWithRandomContent(src_dir1, "aa1");
		File src_dir1_bb1 = createFileWithRandomContent(src_dir1, "bb1");

		File src_dir2_dir1 = createDirectory(src_dir2, "dir1");
		File src_dir2_dir1_aa2 = createFileWithRandomContent(src_dir2_dir1, "aa2");
		File src_dir2_dir1_bb2 = createFileWithRandomContent(src_dir2_dir1, "bb2");

		assertThat(src_file1.getIoFile()).isFile();
		assertThat(src_dir1.getIoFile()).isDirectory();
		assertThat(src_dir1_aa1.getIoFile()).isFile();
		assertThat(src_dir1_bb1.getIoFile()).isFile();
		assertThat(src_dir2_dir1.getIoFile()).isDirectory();
		assertThat(src_dir2_dir1_aa2.getIoFile()).isFile();
		assertThat(src_dir2_dir1_bb2.getIoFile()).isFile();

		syncFromLocalSrcToRemote();
		determineRemotePathPrefix2Encrypted();
		createLocalDestinationRepo();
		syncFromRemoteToLocalDest(false);

		File dst_dir2 = createFile(localDestRoot, "2");
		assertThat(dst_dir2.getIoFile()).isDirectory();

		File dst_file1 = createFile(dst_dir2, "file1");
		File dst_dir1 = createFile(localDestRoot, "dir1");
		File dst_dir1_aa1 = createFile(dst_dir1, "aa1");
		File dst_dir1_bb1 = createFile(dst_dir1, "bb1");

		File dst_dir2_dir1 = createFile(dst_dir2, "dir1");
		File dst_dir2_dir1_aa2 = createFile(dst_dir2_dir1, "aa2");
		File dst_dir2_dir1_bb2 = createFile(dst_dir2_dir1, "bb2");

		assertThat(dst_file1.getIoFile()).doesNotExist();
		assertThat(dst_dir1.getIoFile()).doesNotExist();
		assertThat(dst_dir1_aa1.getIoFile()).doesNotExist();
		assertThat(dst_dir1_bb1.getIoFile()).doesNotExist();
		assertThat(dst_dir2_dir1.getIoFile()).doesNotExist();
		assertThat(dst_dir2_dir1_aa2.getIoFile()).doesNotExist();
		assertThat(dst_dir2_dir1_bb2.getIoFile()).doesNotExist();
	}

	@Test
	public void ignoreRulesBecomeDisabled() throws Exception {
		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();

		// Create ignore rules.
		Properties properties = new Properties();
		properties.put("ignore[file1].namePattern", "file1");
		properties.put("ignore[dir1].namePattern", "dir1");
		PropertiesUtil.store(createFile(localSrcRoot, ".subshare.properties"), properties, null);

		File src_dir2 = createFile(localSrcRoot, "2");
		assertThat(src_dir2.getIoFile()).isDirectory();

		File src_file1 = createFileWithRandomContent(src_dir2, "file1");
		File src_dir1 = createDirectory(localSrcRoot, "dir1");
		File src_dir1_aa1 = createFileWithRandomContent(src_dir1, "aa1");
		File src_dir1_bb1 = createFileWithRandomContent(src_dir1, "bb1");

		File src_dir2_dir1 = createDirectory(src_dir2, "dir1");
		File src_dir2_dir1_aa2 = createFileWithRandomContent(src_dir2_dir1, "aa2");
		File src_dir2_dir1_bb2 = createFileWithRandomContent(src_dir2_dir1, "bb2");

		assertThat(src_file1.getIoFile()).isFile();
		assertThat(src_dir1.getIoFile()).isDirectory();
		assertThat(src_dir1_aa1.getIoFile()).isFile();
		assertThat(src_dir1_bb1.getIoFile()).isFile();
		assertThat(src_dir2_dir1.getIoFile()).isDirectory();
		assertThat(src_dir2_dir1_aa2.getIoFile()).isFile();
		assertThat(src_dir2_dir1_bb2.getIoFile()).isFile();

		syncFromLocalSrcToRemote();
		determineRemotePathPrefix2Encrypted();
		createLocalDestinationRepo();
		syncFromRemoteToLocalDest(false);

		File dst_dir2 = createFile(localDestRoot, "2");
		assertThat(dst_dir2.getIoFile()).isDirectory();

		File dst_file1 = createFile(localDestRoot, "file1");
		File dst_dir1 = createFile(localDestRoot, "dir1");
		File dst_dir1_aa1 = createFile(dst_dir1, "aa1");
		File dst_dir1_bb1 = createFile(dst_dir1, "bb1");

		File dst_dir2_dir1 = createFile(dst_dir2, "dir1");
		File dst_dir2_dir1_aa2 = createFile(dst_dir2_dir1, "aa2");
		File dst_dir2_dir1_bb2 = createFile(dst_dir2_dir1, "bb2");

		assertThat(dst_file1.getIoFile()).doesNotExist();
		assertThat(dst_dir1.getIoFile()).doesNotExist();
		assertThat(dst_dir1_aa1.getIoFile()).doesNotExist();
		assertThat(dst_dir1_bb1.getIoFile()).doesNotExist();
		assertThat(dst_dir2_dir1.getIoFile()).doesNotExist();
		assertThat(dst_dir2_dir1_aa2.getIoFile()).doesNotExist();
		assertThat(dst_dir2_dir1_bb2.getIoFile()).doesNotExist();

		syncFromLocalSrcToRemote(); // again - just to make sure there's no deletion synced back.

		properties = new Properties();
		properties.put("ignore[file1].enabled", "false");
		properties.put("ignore[dir1].enabled", "false");
		PropertiesUtil.store(createFile(src_dir2, ".subshare.properties"), properties, null);

		syncFromLocalSrcToRemote();
		syncFromRemoteToLocalDest(false);

		assertThat(src_file1.getIoFile()).isFile();
		assertThat(src_dir1.getIoFile()).isDirectory();
		assertThat(src_dir1_aa1.getIoFile()).isFile();
		assertThat(src_dir1_bb1.getIoFile()).isFile();
		assertThat(src_dir2_dir1.getIoFile()).isDirectory();
		assertThat(src_dir2_dir1_aa2.getIoFile()).isFile();
		assertThat(src_dir2_dir1_bb2.getIoFile()).isFile();

		assertThat(dst_file1.getIoFile()).doesNotExist();
		assertThat(dst_dir1.getIoFile()).doesNotExist();
		assertThat(dst_dir1_aa1.getIoFile()).doesNotExist();
		assertThat(dst_dir1_bb1.getIoFile()).doesNotExist();
		assertThat(dst_dir2_dir1.getIoFile()).isDirectory();
		assertThat(dst_dir2_dir1_aa2.getIoFile()).isFile();
		assertThat(dst_dir2_dir1_bb2.getIoFile()).isFile();
	}

}
