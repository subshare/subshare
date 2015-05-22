package org.subshare.local.transport;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.UUID;

import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.dto.SsDeleteModificationDto;
import org.subshare.core.repo.transport.CryptreeFileRepoTransport;

import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.transport.FileRepoTransport;

public class CryptreeFileRepoTransportImpl extends FileRepoTransport implements CryptreeFileRepoTransport {

	@Override
	public void delete(final SsDeleteModificationDto deleteModificationDto) {
		assertNotNull("deleteModificationDto", deleteModificationDto);
		final UUID clientRepositoryId = assertNotNull("clientRepositoryId", getClientRepositoryId());

		final LocalRepoManager localRepoManager = getLocalRepoManager();
		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final Cryptree cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(transaction, clientRepositoryId);
			cryptree.assertSignatureOk(deleteModificationDto);

//			path = repoTransport.unprefixPath(path); // TODO do I need to unprefix this?!
//			repoTransport.makeDirectory(path, lastModified == null ? null : lastModified.toDate());

			if (true)
				throw new UnsupportedOperationException("Fuck, this doesn't work! I need a different way!");

			transaction.commit();
		}
	}

}
