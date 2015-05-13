package org.subshare.core.repo.transport;

import java.util.UUID;

import org.subshare.core.pgp.PgpKey;

import co.codewizards.cloudstore.core.repo.transport.RepoTransport;

public interface CryptreeRepoTransport extends RepoTransport {

	void createRepository(UUID serverRepositoryId, PgpKey pgpKey);

}
