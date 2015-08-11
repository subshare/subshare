package org.subshare.core.repo.sync;

import java.net.URL;

import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.dto.SsDeleteModificationDto;
import org.subshare.core.repo.transport.CryptreeRestRepoTransport;

import co.codewizards.cloudstore.core.dto.ChangeSetDto;
import co.codewizards.cloudstore.core.dto.DeleteModificationDto;
import co.codewizards.cloudstore.core.dto.ModificationDto;
import co.codewizards.cloudstore.core.dto.NormalFileDto;
import co.codewizards.cloudstore.core.dto.RepoFileDtoTreeNode;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync;
import co.codewizards.cloudstore.core.repo.transport.DeleteModificationCollisionException;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;

public class SsRepoToRepoSync extends RepoToRepoSync {

	private Boolean metaOnly;

	protected SsRepoToRepoSync(final File localRoot, final URL remoteRoot) {
		super(localRoot, remoteRoot);
	}

	@Override
	protected void syncUp(ProgressMonitor monitor) {
		if (isMetaOnly())
			return; // Currently, meta-only implies read-only, too. Hence we don't need to up-sync as it can never change locally.

		super.syncUp(monitor);
	}

	private boolean isMetaOnly() {
		if (metaOnly == null) {
			try (final LocalRepoTransaction transaction = localRepoManager.beginReadTransaction();) {
				final Cryptree cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(transaction, remoteRepositoryId);
				metaOnly = cryptree.isMetaOnly();
			}
		}
		return metaOnly;
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

	@Override
	protected void beginPutFile(final RepoTransport fromRepoTransport, final RepoTransport toRepoTransport,
			final RepoFileDtoTreeNode repoFileDtoTreeNode, final String path, final NormalFileDto fromNormalFileDto)
					throws DeleteModificationCollisionException {
		if (toRepoTransport instanceof CryptreeRestRepoTransport)
			((CryptreeRestRepoTransport) toRepoTransport).beginPutFile(path, fromNormalFileDto);
		else
			super.beginPutFile(fromRepoTransport, toRepoTransport, repoFileDtoTreeNode, path, fromNormalFileDto);
	}

	@Override
	protected void endPutFile(final RepoTransport fromRepoTransport, final RepoTransport toRepoTransport,
			final RepoFileDtoTreeNode repoFileDtoTreeNode, final String path, final NormalFileDto fromNormalFileDto) {
		if (toRepoTransport instanceof CryptreeRestRepoTransport)
			((CryptreeRestRepoTransport) toRepoTransport).endPutFile(path, fromNormalFileDto);
		else
			super.endPutFile(fromRepoTransport, toRepoTransport, repoFileDtoTreeNode, path, fromNormalFileDto);
	}

	@Override
	protected void sync(final RepoTransport fromRepoTransport, final RepoTransport toRepoTransport,
			final ChangeSetDto changeSetDto, final ProgressMonitor monitor) {
		if (isMetaOnly()) {
			if (fromRepoTransport instanceof CryptreeRestRepoTransport) { // we are syncing DOWN
				for (ModificationDto modificationDto : changeSetDto.getModificationDtos()) {
					if (modificationDto instanceof DeleteModificationDto)
						applyDeleteModification(fromRepoTransport, toRepoTransport, (DeleteModificationDto) modificationDto);
				}
			}
			return; // We do *not* sync actual files!
		}

		super.sync(fromRepoTransport, toRepoTransport, changeSetDto, monitor);
	}

	@Override
	protected void sync(final RepoTransport fromRepoTransport, final RepoTransport toRepoTransport,
			final RepoFileDtoTreeNode repoFileDtoTree,
			final Class<?>[] repoFileDtoClassesIncl, final Class<?>[] repoFileDtoClassesExcl, final boolean filesInProgressOnly,
			final ProgressMonitor monitor) {
		if (isMetaOnly())
			return; // We do *not* sync actual files!

		super.sync(fromRepoTransport, toRepoTransport, repoFileDtoTree, repoFileDtoClassesIncl, repoFileDtoClassesExcl,
				filesInProgressOnly, monitor);
	}
}
