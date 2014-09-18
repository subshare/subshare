package org.subshare.local;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.subshare.core.sign.SignableSigner;
import org.subshare.core.sign.SignableVerifier;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.local.persistence.RepositoryOwner;
import org.subshare.local.persistence.RepositoryOwnerDao;
import org.subshare.local.persistence.UserRepoKeyPublicKeyLookupImpl;

import co.codewizards.cloudstore.core.dto.jaxb.RepoFileDtoIo;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public class CryptreeContext {

	public final UserRepoKeyRing userRepoKeyRing; // never null
//	public final UserRepoKey userRepoKey; // never null
	public final LocalRepoTransaction transaction; // never null
	public final UUID localRepositoryId; // never null
	public final UUID remoteRepositoryId; // never null
	public final UUID serverRepositoryId; // never null
	public final RepoFileDtoIo repoFileDtoIo; // never null
	public final SignableVerifier signableVerifier; // never null

	private RepositoryOwner repositoryOwner; // lazily initialised

	private final Map<UserRepoKey, SignableSigner> userRepoKey2SignableSigner = new HashMap<>();

	public CryptreeContext(final UserRepoKeyRing userRepoKeyRing, final LocalRepoTransaction transaction, final UUID localRepositoryId, final UUID remoteRepositoryId, final UUID serverRepositoryId) {
//		this.userRepoKey = assertNotNull("userRepoKey", userRepoKey);
		this.userRepoKeyRing = assertNotNull("userRepoKeyRing", userRepoKeyRing);
		this.transaction = assertNotNull("transaction", transaction);
		this.localRepositoryId = assertNotNull("localRepositoryId", localRepositoryId);
		this.remoteRepositoryId = assertNotNull("remoteRepositoryId", remoteRepositoryId);
		this.serverRepositoryId = assertNotNull("serverRepositoryId", serverRepositoryId);
		this.repoFileDtoIo = new RepoFileDtoIo();
		this.signableVerifier = new SignableVerifier(new UserRepoKeyPublicKeyLookupImpl(transaction));

		if (userRepoKeyRing.getUserRepoKeys(serverRepositoryId).isEmpty())
			throw new IllegalArgumentException(String.format(
					"userRepoKeyRing.getUserRepoKeys(serverRepositoryId).isEmpty() :: serverRepositoryId=%s",
					serverRepositoryId));
	}

	public RepositoryOwner getRepositoryOwnerOrFail() {
		if (repositoryOwner == null) {
			final RepositoryOwnerDao roDao = transaction.getDao(RepositoryOwnerDao.class);
			repositoryOwner = roDao.getRepositoryOwnerOrFail(serverRepositoryId);
		}
		return repositoryOwner;
	}

	public SignableSigner getSignableSigner(final UserRepoKey userRepoKey) {
		assertNotNull("userRepoKey", userRepoKey);
		SignableSigner signableSigner = userRepoKey2SignableSigner.get(userRepoKey);
		if (signableSigner == null) {
			signableSigner = new SignableSigner(userRepoKey);
			userRepoKey2SignableSigner.put(userRepoKey, signableSigner);
		}
		return signableSigner;
	}
}
