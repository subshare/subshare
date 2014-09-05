package org.subshare.local;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.sign.SignableSigner;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyRing;

import co.codewizards.cloudstore.core.dto.jaxb.RepoFileDtoIo;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public class CryptreeContext {

	public final UserRepoKeyRing userRepoKeyRing; // never null
	public final UserRepoKey userRepoKey; // never null
	public final LocalRepoTransaction transaction; // never null
	public final RepoFileDtoIo repoFileDtoIo; // never null
	public final SignableSigner signableSigner; // never null

	public CryptreeContext(final UserRepoKey userRepoKey, final LocalRepoTransaction transaction) {
		this.userRepoKey = assertNotNull("userRepoKey", userRepoKey);
		this.userRepoKeyRing = assertNotNull("userRepoKey.userRepoKeyRing", userRepoKey.getUserRepoKeyRing());
		this.transaction = assertNotNull("transaction", transaction);
		this.repoFileDtoIo = new RepoFileDtoIo();
		this.signableSigner = new SignableSigner(userRepoKey);
	}

}
