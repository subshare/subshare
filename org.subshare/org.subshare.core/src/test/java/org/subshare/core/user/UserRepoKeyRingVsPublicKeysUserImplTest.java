package org.subshare.core.user;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.Test;

import co.codewizards.cloudstore.core.dto.Uid;

public class UserRepoKeyRingVsPublicKeysUserImplTest {

	@Test
	public void firstCreateUserRepoKeyRingThenAddPublicKeys() {
		UserImpl user = new UserImpl();

		// First create UserRepoKeyRing.
		user.getUserRepoKeyRingOrCreate(); // no need to add anything - existence is sufficient.

		// Then create and add public key outside key-ring.
		UUID serverRepositoryId1 = UUID.randomUUID();
		TestPk pk0 = new TestPk(new Uid(), serverRepositoryId1);
		try {
			user.getUserRepoKeyPublicKeys().add(pk0);
			fail("No exception thrown!");
		} catch (IllegalStateException x) {
			Pattern p = Pattern.compile("UserImpl\\[[^]]*] already has a userRepoKeyRing! Cannot add public keys! Either there is a userRepoKeyRing or there are public keys! There cannot be both! userRepoKeyRing=UserRepoKeyRingImpl\\[\\[\\]\\], userRepoKeyPublicKeys=\\[\\], event\\.changeCollection=\\[TestPk\\[[^]]*\\]\\]");
			if (! p.matcher(x.getMessage()).matches())
				fail(x.toString());
		}
	}

	@Test
	public void firstAddPublicKeysThenCreateUserRepoKeyRing() {
		UserImpl user = new UserImpl();

		// First create and add public key outside key-ring.
		UUID serverRepositoryId1 = UUID.randomUUID();
		TestPk pk0 = new TestPk(new Uid(), serverRepositoryId1);
		user.getUserRepoKeyPublicKeys().add(pk0);

		// Then create UserRepoKeyRing.
		try {
			user.getUserRepoKeyRingOrCreate(); // no need to add anything - existence is sufficient.
			fail("No exception thrown!");
		} catch (IllegalStateException x) {
			Pattern p = Pattern.compile("UserImpl\\[[^]]*] already has public keys! Cannot create a userRepoKeyRing! Either there is a userRepoKeyRing or there are public keys! There cannot be both! userRepoKeyPublicKeys=\\[TestPk\\[[^]]*\\]\\], userRepoKeyRing=null");
			if (! p.matcher(x.getMessage()).matches())
				fail(x.toString());
		}
	}

}
