package org.subshare.local;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.context.RepoFileContext;
import org.subshare.local.persistence.SsDeleteModification;
import org.subshare.local.persistence.SsLocalRepository;
import org.subshare.local.persistence.SsRepoFile;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;
import org.subshare.local.persistence.LocalRepositoryType;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.LocalRepoSync;
import co.codewizards.cloudstore.local.persistence.DeleteModification;
import co.codewizards.cloudstore.local.persistence.LocalRepository;
import co.codewizards.cloudstore.local.persistence.LocalRepositoryDao;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RepoFile;

public class SsLocalRepoSync extends LocalRepoSync {

	private boolean repoFileContextWasApplied;

	protected SsLocalRepoSync(final LocalRepoTransaction transaction) {
		super(transaction);
	}

	@Override
	protected RepoFile sync(final RepoFile parentRepoFile, final File file, final ProgressMonitor monitor, boolean resursiveChildren) {
		if (resursiveChildren) {
			final LocalRepository lr = transaction.getDao(LocalRepositoryDao.class).getLocalRepositoryOrFail();
			final SsLocalRepository localRepository = (SsLocalRepository) lr;
			if (localRepository.getLocalRepositoryType() == LocalRepositoryType.SERVER)
				resursiveChildren = false;
		}

		repoFileContextWasApplied = false;
		final RepoFile repoFile = super.sync(parentRepoFile, file, monitor, resursiveChildren);
		if (!repoFileContextWasApplied && repoFile != null)
			applyRepoFileContextIfExists(repoFile);

		return repoFile;
	}

	@Override
	protected RepoFile _createRepoFile(final RepoFile parentRepoFile, final File file, final ProgressMonitor monitor) {
		final RepoFile repoFile = super._createRepoFile(parentRepoFile, file, monitor);
		applyRepoFileContextIfExists(repoFile);
		return repoFile;
	}

	@Override
	public void updateRepoFile(final RepoFile repoFile, final File file, final ProgressMonitor monitor) {
		super.updateRepoFile(repoFile, file, monitor);
		applyRepoFileContextIfExists(repoFile);
	}

	protected void applyRepoFileContextIfExists(final RepoFile repoFile) {
		assertNotNull("repoFile", repoFile);
		repoFileContextWasApplied = true;
		final RepoFileContext repoFileContext = RepoFileContext.getContext();
		if (repoFileContext != null) {
			final SsRepoFile ccRepoFile = (SsRepoFile) repoFile;
			final RepoFile parentRepoFile = repoFile.getParent();
			final String parentName = parentRepoFile == null ? null : parentRepoFile.getName();

			if (!equal(parentName, repoFileContext.getSsRepoFileDto().getParentName()))
				throw new IllegalStateException(String.format("parentName != ssRepoFileDto.parentName :: '%s' != '%s'",
						parentName, repoFileContext.getSsRepoFileDto().getParentName()));

			ccRepoFile.setSignature(repoFileContext.getSsRepoFileDto().getSignature());
		}
	}

	@Override
	protected void populateDeleteModification(final DeleteModification modification, final RepoFile repoFile, final RemoteRepository remoteRepository) {
		super.populateDeleteModification(modification, repoFile, remoteRepository);
		SsDeleteModification ccDeleteModification = (SsDeleteModification) modification;

		final CryptoRepoFile cryptoRepoFile = transaction.getDao(CryptoRepoFileDao.class).getCryptoRepoFile(repoFile);
		final CryptoRepoFile parentCryptoRepoFile = cryptoRepoFile.getParent();
		if (parentCryptoRepoFile == null)
			throw new IllegalStateException("Seems the deleted file is the root?! Cannot delete the repository's root!");

		ccDeleteModification.setCryptoRepoFileIdControllingPermissions(parentCryptoRepoFile.getCryptoRepoFileId());

		final boolean removeCryptreeFromTx = transaction.getContextObject(CryptreeImpl.class) == null;

		final Cryptree cryptree =
				CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(
						transaction, remoteRepository.getRepositoryId());

		// We must remove the Cryptree from the transaction, because this Cryptree thinks, it was on the server-side.
		// It does this, because we do not provide a UserRepoKeyRing (which usually never happens on the client-side).
		// This wrong assumption causes the VerifySignableAndWriteProtectedEntityListener to fail.
		if (removeCryptreeFromTx)
			transaction.removeContextObject(cryptree);

		final String serverPath = cryptree.getServerPath(ccDeleteModification.getPath());
		ccDeleteModification.setServerPath(serverPath);

		// We cannot sign now, because we do not have a UserRepoKey available - we'd need to initialise the Cryptree differently.
		// We therefore sign later.
//		cryptree.sign(ccDeleteModification);
	}
}
