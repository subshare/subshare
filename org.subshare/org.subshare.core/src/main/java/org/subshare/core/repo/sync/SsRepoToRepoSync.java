package org.subshare.core.repo.sync;

import java.net.URL;

import org.subshare.core.dto.SsDeleteModificationDto;
import org.subshare.core.repo.transport.CryptreeRestRepoTransport;

import co.codewizards.cloudstore.core.dto.DeleteModificationDto;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;

public class SsRepoToRepoSync extends RepoToRepoSync {

	protected SsRepoToRepoSync(final File localRoot, final URL remoteRoot) {
		super(localRoot, remoteRoot);
	}

	@Override
	protected void applyDeleteModification(RepoTransport fromRepoTransport, RepoTransport toRepoTransport, DeleteModificationDto deleteModificationDto) {
		if (toRepoTransport instanceof CryptreeRestRepoTransport) {
			final SsDeleteModificationDto dto = (SsDeleteModificationDto) deleteModificationDto;
			((CryptreeRestRepoTransport) toRepoTransport).delete(dto);
		}
		else
			super.applyDeleteModification(fromRepoTransport, toRepoTransport, deleteModificationDto);
	}

}
