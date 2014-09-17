package org.subshare.local;

import static co.codewizards.cloudstore.core.util.Util.*;
import static org.assertj.core.api.Assertions.*;

import org.subshare.core.GrantAccessDeniedException;
import org.subshare.core.WriteAccessDeniedException;
import org.subshare.core.user.UserRepoKey;
import org.junit.Test;

public class CryptreeImplGrantRevokeWritePermissionTest extends AbstractPermissionTest {

	@Test(expected=WriteAccessDeniedException.class)
	public void writeWithoutWritePermission() throws Exception {
		createOrUpdateCryptoRepoFiles(friend1UserRepoKey, "/3");
	}

	@Test
	public void writeWithWritePermissionDirectFile() throws Exception {
		assertWritePermissionDenied(friend1UserRepoKey, "/3/3_2/3_2_1/3_2_1_a");
		grantWritePermission("/3/3_2/3_2_1/3_2_1_a", friend1UserRepoKey.getPublicKey());
		assertWritePermissionGranted(friend1UserRepoKey, "/3/3_2/3_2_1/3_2_1_a");
		assertWritePermissionDenied(friend1UserRepoKey, "/3/3_2/3_2_a");
		assertWritePermissionDenied(friend1UserRepoKey, "/3/3_3");
	}

	@Test
	public void writeWithWritePermissionDirectDir() throws Exception {
		assertWritePermissionDenied(friend1UserRepoKey, "/3/3_2/3_2_1");
		grantWritePermission("/3/3_2/3_2_1", friend1UserRepoKey.getPublicKey());
		assertWritePermissionGranted(friend1UserRepoKey, "/3/3_2/3_2_1");
		assertWritePermissionDenied(friend1UserRepoKey, "/3/3_2/3_2_a");
		assertWritePermissionDenied(friend1UserRepoKey, "/3/3_3");
	}

	@Test
	public void writeWithWritePermissionInParent() throws Exception {
		assertWritePermissionDenied(friend1UserRepoKey, "/3/3_2/3_2_1/3_2_1_a");
		grantWritePermission("/3/3_2/3_2_1", friend1UserRepoKey.getPublicKey());
		assertWritePermissionGranted(friend1UserRepoKey, "/3/3_2/3_2_1/3_2_1_a");
		assertWritePermissionGranted(friend1UserRepoKey, "/3/3_2/3_2_1");
		assertWritePermissionDenied(friend1UserRepoKey, "/3/3_2/3_2_a");
		assertWritePermissionDenied(friend1UserRepoKey, "/3/3_3");
	}

	@Test
	public void writeWithWritePermissionInParentParent() throws Exception {
		assertWritePermissionDenied(friend1UserRepoKey, "/3/3_2/3_2_1/3_2_1_a");
		grantWritePermission("/3/3_2", friend1UserRepoKey.getPublicKey());
		assertWritePermissionGranted(friend1UserRepoKey, "/3/3_2/3_2_1/3_2_1_a");
		assertWritePermissionGranted(friend1UserRepoKey, "/3/3_2/3_2_1");
		assertWritePermissionGranted(friend1UserRepoKey, "/3/3_2/3_2_a");
		assertWritePermissionGranted(friend1UserRepoKey, "/3/3_2");
		assertWritePermissionDenied(friend1UserRepoKey, "/3/3_3");
	}

	@Test
	public void writeWithWritePermissionInParentParentGrantedIndirectly() throws Exception {
		assertWritePermissionDenied(friend1UserRepoKey, "/3/3_2/3_2_1/3_2_1_a");
		grantGrantPermission("/3/3_2", friend2UserRepoKey.getPublicKey());
		grantWritePermission(friend2UserRepoKey, "/3/3_2", friend1UserRepoKey.getPublicKey());
		assertWritePermissionGranted(friend1UserRepoKey, "/3/3_2/3_2_1/3_2_1_a");
		assertWritePermissionGranted(friend1UserRepoKey, "/3/3_2/3_2_1");
		assertWritePermissionGranted(friend1UserRepoKey, "/3/3_2/3_2_a");
		assertWritePermissionGranted(friend1UserRepoKey, "/3/3_2");
		assertWritePermissionDenied(friend1UserRepoKey, "/3/3_3");
	}

	@Test(expected=GrantAccessDeniedException.class)
	public void grantWritePermissionWithoutGrantPermission() throws Exception {
		assertWritePermissionDenied(friend1UserRepoKey, "/3/3_2/3_2_1/3_2_1_a");
		grantWritePermission(friend2UserRepoKey, "/3/3_2", friend1UserRepoKey.getPublicKey());
	}

	private void assertWritePermissionDenied(final UserRepoKey userRepoKey, final String localPath) {
		try {
			createOrUpdateCryptoRepoFiles(userRepoKey, localPath);
			fail("We should not have access to this: " + localPath);
		} catch (final WriteAccessDeniedException x) {
			doNothing(); // we expect exactly this!
		}
	}

	private void assertWritePermissionGranted(final UserRepoKey userRepoKey, final String localPath) {
		try {
			createOrUpdateCryptoRepoFiles(userRepoKey, localPath);
		} catch (final WriteAccessDeniedException x) {
			fail("We should have access to this: " + localPath);
		}
	}

}
