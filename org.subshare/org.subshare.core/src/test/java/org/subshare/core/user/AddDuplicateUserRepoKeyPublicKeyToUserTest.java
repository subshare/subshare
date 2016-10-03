package org.subshare.core.user;

import static org.assertj.core.api.Assertions.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.junit.Test;
import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.dto.Uid;

public class AddDuplicateUserRepoKeyPublicKeyToUserTest {

	private static class TestPk implements UserRepoKey.PublicKeyWithSignature {
		private final Uid userRepoKeyId;
		private final UUID serverRepositoryId;

		public TestPk(Uid userRepoKeyId, UUID serverRepositoryId) {
			this.userRepoKeyId = userRepoKeyId;
			this.serverRepositoryId = serverRepositoryId;
		}

		@Override
		public Uid getUserRepoKeyId() {
			return userRepoKeyId;
		}

		@Override
		public UUID getServerRepositoryId() {
			return serverRepositoryId;
		}

		@Override
		public AsymmetricKeyParameter getPublicKey() {
			return null;
		}

		@Override
		public Date getValidTo() {
			return null;
		}

		@Override
		public boolean isInvitation() {
			return false;
		}

		@Override
		public String getSignedDataType() {
			return null;
		}

		@Override
		public int getSignedDataVersion() {
			return 0;
		}

		@Override
		public InputStream getSignedData(int signedDataVersion) {
			return null;
		}

		@Override
		public Signature getSignature() {
			return null;
		}

		@Override
		public void setSignature(Signature signature) {
		}

		@Override
		public byte[] getSignedPublicKeyData() {
			return null;
		}

		@Override
		public byte[] getPublicKeyData() {
			return null;
		}

		@Override
		public String toString() {
			return String.format("%s[%s]", getClass().getSimpleName(), userRepoKeyId);
		}
	}

	@Test
	public void addDifferentUserRepoKeyPublicKeysToUser() {
		UserImpl user = new UserImpl();
		UUID serverRepositoryId1 = UUID.randomUUID();
		UUID serverRepositoryId2 = UUID.randomUUID();
		TestPk pk0 = new TestPk(new Uid(), serverRepositoryId1);
		TestPk pk1 = new TestPk(new Uid(), serverRepositoryId1);
		TestPk pk2 = new TestPk(new Uid(), serverRepositoryId2);
		user.getUserRepoKeyPublicKeys().add(pk0);
		user.getUserRepoKeyPublicKeys().add(pk1);
		user.getUserRepoKeyPublicKeys().add(pk2);
		assertThat(user.getUserRepoKeyPublicKeys()).isEqualTo(new HashSet<>(Arrays.asList(pk0, pk1, pk2)));
	}

	@Test
	public void addAllDifferentUserRepoKeyPublicKeysToUser() {
		UserImpl user = new UserImpl();
		List<UserRepoKey.PublicKeyWithSignature> list = new ArrayList<>();
		UUID serverRepositoryId1 = UUID.randomUUID();
		UUID serverRepositoryId2 = UUID.randomUUID();
		TestPk pk0 = new TestPk(new Uid(), serverRepositoryId1);
		TestPk pk1 = new TestPk(new Uid(), serverRepositoryId1);
		TestPk pk2 = new TestPk(new Uid(), serverRepositoryId2);
		list.add(pk0);
		list.add(pk1);
		list.add(pk2);
		user.getUserRepoKeyPublicKeys().addAll(Collections.unmodifiableList(list));
		assertThat(user.getUserRepoKeyPublicKeys()).isEqualTo(new HashSet<>(Arrays.asList(pk0, pk1, pk2)));
	}

	@Test
	public void addSameUserRepoKeyPublicKeyTwiceToUser() {
		UserImpl user = new UserImpl();
		UUID serverRepositoryId1 = UUID.randomUUID();
		UUID serverRepositoryId2 = UUID.randomUUID();
		TestPk pkX = new TestPk(new Uid(), UUID.randomUUID());
		TestPk pk0 = new TestPk(new Uid(), serverRepositoryId1);
		TestPk pk1 = new TestPk(new Uid(), serverRepositoryId1);
		TestPk pk2 = new TestPk(new Uid(), serverRepositoryId2);
		user.getUserRepoKeyPublicKeys().add(pk0);
		user.getUserRepoKeyPublicKeys().add(pkX);
		user.getUserRepoKeyPublicKeys().add(pkX);
		user.getUserRepoKeyPublicKeys().add(pk1);
		user.getUserRepoKeyPublicKeys().add(pkX);
		user.getUserRepoKeyPublicKeys().add(pk2);
		assertThat(user.getUserRepoKeyPublicKeys()).isEqualTo(new HashSet<>(Arrays.asList(pk0, pk1, pkX, pk2)));
	}

	@Test
	public void addAllSameUserRepoKeyPublicKeyInCollectionToUser() {
		UserImpl user = new UserImpl();
		List<UserRepoKey.PublicKeyWithSignature> list = new ArrayList<>();
		UUID serverRepositoryId1 = UUID.randomUUID();
		UUID serverRepositoryId2 = UUID.randomUUID();
		TestPk pkX = new TestPk(new Uid(), UUID.randomUUID());
		TestPk pk0 = new TestPk(new Uid(), serverRepositoryId1);
		TestPk pk1 = new TestPk(new Uid(), serverRepositoryId1);
		TestPk pk2 = new TestPk(new Uid(), serverRepositoryId2);
		list.add(pk0);
		list.add(pkX);
		list.add(pkX);
		list.add(pk1);
		list.add(pkX);
		list.add(pk2);

		user.getUserRepoKeyPublicKeys().addAll(Collections.unmodifiableList(list));
		assertThat(user.getUserRepoKeyPublicKeys()).isEqualTo(new HashSet<>(Arrays.asList(pk0, pk1, pkX, pk2)));
	}
}
