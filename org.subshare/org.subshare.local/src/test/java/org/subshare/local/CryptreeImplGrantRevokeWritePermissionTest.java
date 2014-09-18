package org.subshare.local;

import static co.codewizards.cloudstore.core.util.Util.*;
import static org.assertj.core.api.Assertions.*;

import org.subshare.core.GrantAccessDeniedException;
import org.subshare.core.WriteAccessDeniedException;
import org.subshare.core.user.UserRepoKeyRing;
import org.junit.Test;

public class CryptreeImplGrantRevokeWritePermissionTest extends AbstractPermissionTest {

	@Override
	public void before() throws Exception {
		super.before();
		grantReadPermission("/", friend1UserRepoKey.getPublicKey());
	}

	@Test(expected=WriteAccessDeniedException.class)
	public void writeWithoutWritePermission() throws Exception {
		createOrUpdateCryptoRepoFiles(friend1UserRepoKeyRing, "/3");
	}

	@Test
	public void writeWithWritePermissionDirectFile() throws Exception {
		assertWritePermissionDenied(friend1UserRepoKeyRing, "/3/3_2/3_2_1/3_2_1_a");
		grantWritePermission("/3/3_2/3_2_1/3_2_1_a", friend1UserRepoKey.getPublicKey());
		assertWritePermissionGranted(friend1UserRepoKeyRing, "/3/3_2/3_2_1/3_2_1_a");
		assertWritePermissionDenied(friend1UserRepoKeyRing, "/3/3_2/3_2_a");
		assertWritePermissionDenied(friend1UserRepoKeyRing, "/3/3_3");
	}

	@Test
	public void writeWithWritePermissionDirectDir() throws Exception {
		assertWritePermissionDenied(friend1UserRepoKeyRing, "/3/3_2/3_2_1");
		grantWritePermission("/3/3_2/3_2_1", friend1UserRepoKey.getPublicKey());
		assertWritePermissionGranted(friend1UserRepoKeyRing, "/3/3_2/3_2_1");
		assertWritePermissionDenied(friend1UserRepoKeyRing, "/3/3_2/3_2_a");
		assertWritePermissionDenied(friend1UserRepoKeyRing, "/3/3_3");
	}

	@Test
	public void writeWithWritePermissionInParent() throws Exception {
		assertWritePermissionDenied(friend1UserRepoKeyRing, "/3/3_2/3_2_1/3_2_1_a");
		grantWritePermission("/3/3_2/3_2_1", friend1UserRepoKey.getPublicKey());
		assertWritePermissionGranted(friend1UserRepoKeyRing, "/3/3_2/3_2_1/3_2_1_a");
		assertWritePermissionGranted(friend1UserRepoKeyRing, "/3/3_2/3_2_1");
		assertWritePermissionDenied(friend1UserRepoKeyRing, "/3/3_2/3_2_a");
		assertWritePermissionDenied(friend1UserRepoKeyRing, "/3/3_3");
	}

	@Test
	public void writeWithWritePermissionInParentParent() throws Exception {
		assertWritePermissionDenied(friend1UserRepoKeyRing, "/3/3_2/3_2_1/3_2_1_a");
		grantWritePermission("/3/3_2", friend1UserRepoKey.getPublicKey());
		assertWritePermissionGranted(friend1UserRepoKeyRing, "/3/3_2/3_2_1/3_2_1_a");
		assertWritePermissionGranted(friend1UserRepoKeyRing, "/3/3_2/3_2_1");
		assertWritePermissionGranted(friend1UserRepoKeyRing, "/3/3_2/3_2_a");
		assertWritePermissionGranted(friend1UserRepoKeyRing, "/3/3_2");
		assertWritePermissionDenied(friend1UserRepoKeyRing, "/3/3_3");
	}

	@Test
	public void writeWithWritePermissionInParentParentGrantedIndirectly() throws Exception {
		assertWritePermissionDenied(friend1UserRepoKeyRing, "/3/3_2/3_2_1/3_2_1_a");
		grantGrantPermission("/3/3_2", friend2UserRepoKey.getPublicKey());
		grantWritePermission(friend2UserRepoKeyRing, "/3/3_2", friend1UserRepoKey.getPublicKey());
		assertWritePermissionGranted(friend1UserRepoKeyRing, "/3/3_2/3_2_1/3_2_1_a");
		assertWritePermissionGranted(friend1UserRepoKeyRing, "/3/3_2/3_2_1");
		assertWritePermissionGranted(friend1UserRepoKeyRing, "/3/3_2/3_2_a");
		assertWritePermissionGranted(friend1UserRepoKeyRing, "/3/3_2");
		assertWritePermissionDenied(friend1UserRepoKeyRing, "/3/3_3");
	}

	@Test
	public void grantWritePermissionWithoutGrantPermission() throws Exception {
		assertWritePermissionDenied(friend1UserRepoKeyRing, "/3/3_2/3_2_1/3_2_1_a");
		try {
			grantWritePermission(friend2UserRepoKeyRing, "/3/3_2", friend1UserRepoKey.getPublicKey());
			fail("We should not have grant access to here!");
		} catch (final GrantAccessDeniedException x) {
			doNothing(); // we expect this!
		}
	}

	private void assertWritePermissionDenied(final UserRepoKeyRing userRepoKeyRing, final String localPath) {
		try {
			createOrUpdateCryptoRepoFiles(userRepoKeyRing, localPath);
			fail("We should not have access to this: " + localPath);
		} catch (final WriteAccessDeniedException x) {
			doNothing(); // we expect exactly this!
		}
	}

	private void assertWritePermissionGranted(final UserRepoKeyRing userRepoKeyRing, final String localPath) {
		try {
			createOrUpdateCryptoRepoFiles(userRepoKeyRing, localPath);
		} catch (final WriteAccessDeniedException x) {
			fail("We should have access to this: " + localPath);
		}
	}

}
