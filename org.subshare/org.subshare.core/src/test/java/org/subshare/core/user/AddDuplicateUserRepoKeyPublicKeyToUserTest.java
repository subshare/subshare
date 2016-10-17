package org.subshare.core.user;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import co.codewizards.cloudstore.core.dto.Uid;

public class AddDuplicateUserRepoKeyPublicKeyToUserTest {

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
