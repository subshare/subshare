package org.subshare.local;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.sign.SignableSigner;
import org.subshare.core.sign.SignableVerifier;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.local.persistence.RepositoryOwner;
import org.subshare.local.persistence.RepositoryOwnerDao;
import org.subshare.local.persistence.UserRepoKeyPublicKey;
import org.subshare.local.persistence.UserRepoKeyPublicKeyDao;
import org.subshare.local.persistence.UserRepoKeyPublicKeyLookupImpl;

import co.codewizards.cloudstore.core.dto.jaxb.RepoFileDtoIo;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public class CryptreeContext {

	public final UserRepoKeyRing userRepoKeyRing; // never null
	public final UserRepoKey userRepoKey; // never null
	public final LocalRepoTransaction transaction; // never null
	public final RepoFileDtoIo repoFileDtoIo; // never null
	public final SignableSigner signableSigner; // never null
	public final SignableVerifier signableVerifier; // never null

	private RepositoryOwner repositoryOwner; // lazily initialised

	public CryptreeContext(final UserRepoKey userRepoKey, final LocalRepoTransaction transaction) {
		this.userRepoKey = assertNotNull("userRepoKey", userRepoKey);
		this.userRepoKeyRing = assertNotNull("userRepoKey.userRepoKeyRing", userRepoKey.getUserRepoKeyRing());
		this.transaction = assertNotNull("transaction", transaction);
		this.repoFileDtoIo = new RepoFileDtoIo();
		this.signableSigner = new SignableSigner(userRepoKey);
		this.signableVerifier = new SignableVerifier(new UserRepoKeyPublicKeyLookupImpl(transaction));
	}

	public RepositoryOwner getRepositoryOwner() {
		if (repositoryOwner == null) {
			final RepositoryOwnerDao roDao = transaction.getDao(RepositoryOwnerDao.class);
			repositoryOwner = roDao.getRepositoryOwner(userRepoKey.getServerRepositoryId());

			// If there is no RepositoryOwner, we lazily assume ownership now ;-)
			if (repositoryOwner == null) {
				final UserRepoKeyPublicKeyDao urkpkDao = transaction.getDao(UserRepoKeyPublicKeyDao.class);
				UserRepoKeyPublicKey userRepoKeyPublicKey = urkpkDao.getUserRepoKeyPublicKey(userRepoKey.getUserRepoKeyId());
				if (userRepoKeyPublicKey == null)
					userRepoKeyPublicKey = urkpkDao.makePersistent(new UserRepoKeyPublicKey(userRepoKey.getPublicKey()));

				repositoryOwner = new RepositoryOwner();
				repositoryOwner.setUserRepoKeyPublicKey(userRepoKeyPublicKey);
				new SignableSigner(userRepoKey).sign(repositoryOwner);
				repositoryOwner = roDao.makePersistent(repositoryOwner);
			}
		}
		return repositoryOwner;
	}
}
