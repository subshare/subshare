package org.subshare.local;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.jdo.FetchPlan;
import javax.jdo.PersistenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactory;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.crypto.KeyFactory;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.core.user.UserRepoKeyRingLookup;
import org.subshare.core.user.UserRepoKeyRingLookupContext;
import org.subshare.local.persistence.LocalRepositoryType;
import org.subshare.local.persistence.ScheduledReupload;
import org.subshare.local.persistence.ScheduledReuploadDao;
import org.subshare.local.persistence.SsFileChunk;
import org.subshare.local.persistence.SsLocalRepository;
import org.subshare.local.persistence.SsNormalFile;
import org.subshare.local.persistence.SsRemoteRepository;

import co.codewizards.cloudstore.core.dto.FileChunkDto;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.util.HashUtil;
import co.codewizards.cloudstore.local.ContextWithPersistenceManager;
import co.codewizards.cloudstore.local.LocalRepoSync;
import co.codewizards.cloudstore.local.persistence.FileChunk;
import co.codewizards.cloudstore.local.persistence.LocalRepositoryDao;
import co.codewizards.cloudstore.local.persistence.NormalFile;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDao;
import co.codewizards.cloudstore.local.persistence.RepoFile;

public class SsLocalRepoSync extends LocalRepoSync {

	private static final Logger logger = LoggerFactory.getLogger(SsLocalRepoSync.class);

//	private boolean repoFileContextWasApplied;
	private SsLocalRepository localRepository;
	private UserRepoKeyRing userRepoKeyRing;
	private CryptreeFactory cryptreeFactory;

	private boolean processScheduledReuploadsDone;

	protected SsLocalRepoSync(final LocalRepoTransaction transaction) {
		super(transaction);
	}

	@Override
	public void sync(ProgressMonitor monitor) {
		if (isMetaOnly())
			return;

		processScheduledReuploads();

		super.sync(monitor);
	}

	private void processScheduledReuploads() {
		if (processScheduledReuploadsDone)
			return;


		final ScheduledReuploadDao srDao = transaction.getDao(ScheduledReuploadDao.class);
		Collection<ScheduledReupload> scheduledReuploads = srDao.getObjects();

		logger.debug("processScheduledReuploads: scheduledReuploads.size={} ", scheduledReuploads.size());

		final long localRevision = transaction.getLocalRevision();

		for (final ScheduledReupload scheduledReupload : scheduledReuploads) {
			if (logger.isDebugEnabled())
				logger.debug("processScheduledReuploads: scheduledReupload.repoFile.path='{}' localRevision={}", scheduledReupload.getRepoFile().getPath(), localRevision);

			scheduledReupload.getRepoFile().setLocalRevision(localRevision);
			srDao.deletePersistent(scheduledReupload);
		}
		transaction.flush();

		processScheduledReuploadsDone = true;
	}

	private SsLocalRepository getLocalRepository() {
		if (localRepository == null)
			localRepository = (SsLocalRepository) transaction.getDao(LocalRepositoryDao.class).getLocalRepositoryOrFail();

		return localRepository;
	}

	private boolean isMetaOnly() {
		return getLocalRepository().getLocalRepositoryType() == LocalRepositoryType.CLIENT_META_ONLY;
	}

	@Override
	protected RepoFile sync(final RepoFile parentRepoFile, final File file, final ProgressMonitor monitor, boolean resursiveChildren) {
		if (isMetaOnly()) {
			final RepoFile repoFile = repoFileDao.getRepoFile(localRoot, file);
			return repoFile; // might be null!
		}

		processScheduledReuploads();

		if (resursiveChildren) {
			if (getLocalRepository().getLocalRepositoryType() == LocalRepositoryType.SERVER)
				resursiveChildren = false;
		}

//		repoFileContextWasApplied = false;
		final RepoFile repoFile = super.sync(parentRepoFile, file, monitor, resursiveChildren);
//		if (!repoFileContextWasApplied && repoFile != null)
//			applyRepoFileContextIfExists(repoFile);

		return repoFile;
	}

	@Override
	protected RepoFile _createRepoFile(final RepoFile parentRepoFile, final File file, final ProgressMonitor monitor) {
		final RepoFile repoFile = super._createRepoFile(parentRepoFile, file, monitor);
//		applyRepoFileContextIfExists(repoFile);
		return repoFile;
	}

	@Override
	protected void sha(final NormalFile normalFile, final File file, final ProgressMonitor monitor) {
		assertNotNull("normalFile", normalFile);
		assertNotNull("file", file);
		assertNotNull("monitor", monitor);

		SsNormalFile nf = (SsNormalFile) normalFile;

		final PersistenceManager pm = ((ContextWithPersistenceManager) transaction).getPersistenceManager();
		pm.getFetchPlan().setGroups(FetchPlan.DEFAULT);

		// normalFile.fileChunks is cleared in super-method! Hence we copy them first.
		final Map<Long, SsFileChunk> original_offset2FileChunk = new HashMap<>(normalFile.getFileChunks().size());
		for (FileChunk fileChunk : normalFile.getFileChunks()) {
			final SsFileChunk fc = pm.detachCopy((SsFileChunk) fileChunk);
			original_offset2FileChunk.put(fc.getOffset(), fc);
		}

		super.sha(normalFile, file, monitor);

		// must assign padding after super.sha(...), because super.sha(...) may override nf.length!
		if (nf.getLengthWithPadding() < nf.getLength())
			assignLengthWithPadding(nf);

		SsFileChunk lastNewFileChunk = null;

		final long fileNoPaddingLength = nf.getLength();
		final long fileWithPaddingLength = nf.getLengthWithPadding();
		final long filePaddingLength = fileWithPaddingLength - fileNoPaddingLength;
		if (filePaddingLength > 0) {
			for (FileChunk fileChunk : nf.getFileChunks()) {
				if (lastNewFileChunk == null || lastNewFileChunk.getOffset() < fileChunk.getOffset())
					lastNewFileChunk = (SsFileChunk) fileChunk;
			}

			assertNotNull("lastNewFileChunk", lastNewFileChunk); // every file has at least one chunk! even if it is empty!

			long offset = lastNewFileChunk.getOffset();

			lastNewFileChunk.makeWritable();
			int chunkWithPaddingLength = (int) Math.min(FileChunkDto.MAX_LENGTH, fileWithPaddingLength - offset);
			lastNewFileChunk.setLengthWithPadding(chunkWithPaddingLength);
			lastNewFileChunk.makeReadOnly();

			offset = lastNewFileChunk.getOffset() + lastNewFileChunk.getLengthWithPadding();
			while (offset < fileWithPaddingLength) {
				chunkWithPaddingLength = (int) Math.min(FileChunkDto.MAX_LENGTH, fileWithPaddingLength - offset);

				SsFileChunk fileChunk = original_offset2FileChunk.get(offset);
				if (fileChunk == null)
					fileChunk = createPaddingFileChunk(nf, offset, chunkWithPaddingLength);
				else {
					fileChunk.makeWritable();
					fileChunk.setNormalFile(nf);
					if (fileChunk.getLengthWithPadding() != chunkWithPaddingLength || fileChunk.getLength() != 0) {
						fileChunk.setSha1(createRandomSha1());
						fileChunk.setLengthWithPadding(chunkWithPaddingLength);
						fileChunk.setLength(0); // padding NOT INCLUDED!!!
					}
					fileChunk.makeReadOnly();
				}
				nf.getFileChunks().add(fileChunk);

				lastNewFileChunk = fileChunk;
				offset = lastNewFileChunk.getOffset() + lastNewFileChunk.getLengthWithPadding();
			}
		}
	}

	@Override
	protected void onFinalizeFileChunk(FileChunk fileChunk) {
		super.onFinalizeFileChunk(fileChunk);
		final SsFileChunk fc = (SsFileChunk) fileChunk;
		fc.setLengthWithPadding(fc.getLength());
	}

	private SsFileChunk createPaddingFileChunk(final SsNormalFile normalFile, final long offset, final int paddingLength) {
		assertNotNull("normalFile", normalFile);
		SsFileChunk fileChunk = (SsFileChunk) createObject(FileChunk.class);
		fileChunk.setNormalFile(normalFile);
		fileChunk.setOffset(offset);
		fileChunk.setSha1(createRandomSha1());
		fileChunk.setLengthWithPadding(paddingLength);
		fileChunk.setLength(0); // padding NOT INCLUDED (anymore)!!!
		return fileChunk;
	}

	private String createRandomSha1() {
		final byte[] randomBytes = new byte[16];
		KeyFactory.secureRandom.nextBytes(randomBytes);
		final String sha1 = HashUtil.sha1(randomBytes);
		return sha1;
	}

	private void assignLengthWithPadding(final SsNormalFile normalFile) {
		final File file = normalFile.getFile(localRoot);
		final FilePaddingLengthRandom filePaddingLengthRandom = new FilePaddingLengthRandom(file);
		final long paddingLength = filePaddingLengthRandom.nextPaddingLength();

		if (paddingLength < 0)
			throw new IllegalStateException("paddingLength < 0");

		normalFile.setLengthWithPadding(normalFile.getLength() + paddingLength);
	}

	@Override
	public void updateRepoFile(final RepoFile repoFile, final File file, final ProgressMonitor monitor) {
		super.updateRepoFile(repoFile, file, monitor);
//		applyRepoFileContextIfExists(repoFile);
	}

//	protected void applyRepoFileContextIfExists(final RepoFile repoFile) {
//		assertNotNull("repoFile", repoFile);
//		repoFileContextWasApplied = true;
//		final RepoFileContext repoFileContext = RepoFileContext.getContext();
//		if (repoFileContext != null) {
//			final SsRepoFile ccRepoFile = (SsRepoFile) repoFile;
//			final RepoFile parentRepoFile = repoFile.getParent();
//			final String parentName = parentRepoFile == null ? null : parentRepoFile.getName();
//
//			if (!equal(parentName, repoFileContext.getSsRepoFileDto().getParentName()))
//				throw new IllegalStateException(String.format("parentName != ssRepoFileDto.parentName :: '%s' != '%s'",
//						parentName, repoFileContext.getSsRepoFileDto().getParentName()));
//
//			ccRepoFile.setSignature(repoFileContext.getSsRepoFileDto().getSignature());
//
//			final HistoCryptoRepoFileDto histoCryptoRepoFileDto = repoFileContext.getCryptoRepoFileOnServerDto();
//			if (histoCryptoRepoFileDto != null)
//				HistoCryptoRepoFileDtoConverter.create(transaction).putCryptoRepoFileOnServer(histoCryptoRepoFileDto);
//		}
//	}

	@Override
	protected void createCopyModificationsIfPossible(NormalFile newNormalFile) {
		// We don't support any Modification in Subshare - at least for now.
		// The remote-repo-dependent 'Modification' (and all its subclasses) should be replaced by a
		// global (not remote-repo-dependent) 'Mod', anyway! See the new TO-DO and the @deprecated-comment in
		// 'Modification' for more details!
	}

	@Override
	protected void createDeleteModifications(RepoFile repoFile) {
		// We don't support any Modification in Subshare - at least for now.
		// The remote-repo-dependent 'Modification' (and all its subclasses) should be replaced by a
		// global (not remote-repo-dependent) 'Mod', anyway! See the new TO-DO and the @deprecated-comment in
		// 'Modification' for more details!
		//
		// Note: The delete-tracking is done via CryptoRepoFile.deleted and HistoCryptoRepoFile.deleted!
//		//
//		// We *temporarily* create a DeleteModification and convert it to CryptoRepoFile.deleted *later*.
//		// This is necessary, because the CryptoRepoFile might be overwritten during down-sync, before
//		// our delete-info can be processed.
//		super.createDeleteModifications(repoFile);
	}

	@Override
	protected void deleteRepoFileWithAllChildrenRecursively(RepoFile repoFile) {
		final Cryptree cryptree = getCryptree(transaction);
		final String localPath = repoFile.getPath();
		cryptree.preDelete(localPath);
		super.deleteRepoFileWithAllChildrenRecursively(repoFile);
	}

	protected UserRepoKeyRing getUserRepoKeyRing() {
		if (userRepoKeyRing == null) {
			final UserRepoKeyRingLookup lookup = UserRepoKeyRingLookup.Helper.getUserRepoKeyRingLookup();
			final UserRepoKeyRingLookupContext context = new UserRepoKeyRingLookupContext(getClientRepositoryId(), getServerRepositoryId());
			userRepoKeyRing = lookup.getUserRepoKeyRing(context);
			if (userRepoKeyRing == null)
				throw new IllegalStateException(String.format("UserRepoKeyRingLookup.getUserRepoKeyRing(context) returned null! lookup=%s context=%s", lookup, context));

//			return assertNotNull("cryptreeRepoTransportFactory.userRepoKeyRing", getRepoTransportFactory().getUserRepoKeyRing());
		}
		return userRepoKeyRing;
	}

	private UUID getServerRepositoryId() {
		final Collection<RemoteRepository> remoteRepositories = transaction.getDao(RemoteRepositoryDao.class).getObjects();
		if (remoteRepositories.size() != 1)
			throw new IllegalStateException("remoteRepositories.size() != 1");

		return remoteRepositories.iterator().next().getRepositoryId();
	}

	private String getServerPathPrefix() {
		final Collection<RemoteRepository> remoteRepositories = transaction.getDao(RemoteRepositoryDao.class).getObjects();
		if (remoteRepositories.size() != 1)
			throw new IllegalStateException("remoteRepositories.size() != 1");

		return ((SsRemoteRepository)remoteRepositories.iterator().next()).getRemotePathPrefix();
	}

	private UUID getClientRepositoryId() {
		return getLocalRepository().getRepositoryId();
	}

	protected CryptreeFactory getCryptreeFactory() {
		if (cryptreeFactory == null)
			cryptreeFactory = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail();

		return cryptreeFactory;
	}

	protected Cryptree getCryptree(final LocalRepoTransaction transaction) {
		return getCryptreeFactory().getCryptreeOrCreate(transaction, getServerRepositoryId(), getServerPathPrefix(), getUserRepoKeyRing());
	}

//	@Override
//	protected void populateDeleteModification(final DeleteModification modification, final RepoFile repoFile, final RemoteRepository remoteRepository) {
//		super.populateDeleteModification(modification, repoFile, remoteRepository);
//		SsDeleteModification ccDeleteModification = (SsDeleteModification) modification;
//
//		final CryptoRepoFile cryptoRepoFile = transaction.getDao(CryptoRepoFileDao.class).getCryptoRepoFile(repoFile);
//		final CryptoRepoFile parentCryptoRepoFile = cryptoRepoFile.getParent();
//		if (parentCryptoRepoFile == null)
//			throw new IllegalStateException("Seems the deleted file is the root?! Cannot delete the repository's root!");
//
//		ccDeleteModification.setCryptoRepoFileIdControllingPermissions(parentCryptoRepoFile.getCryptoRepoFileId());
//
//		final boolean removeCryptreeFromTx = transaction.getContextObject(CryptreeImpl.class) == null;
//
//		final Cryptree cryptree =
//				CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(
//						transaction, remoteRepository.getRepositoryId());
//
//		// We must remove the Cryptree from the transaction, because this Cryptree thinks, it was on the server-side.
//		// It does this, because we do not provide a UserRepoKeyRing (which usually never happens on the client-side).
//		// This wrong assumption causes the VerifySignableAndWriteProtectedEntityListener to fail.
//		if (removeCryptreeFromTx)
//			transaction.removeContextObject(cryptree);
//
//		final String serverPath = cryptree.getServerPath(ccDeleteModification.getPath());
//		ccDeleteModification.setServerPath(serverPath);
//
//		// We cannot sign now, because we do not have a UserRepoKey available - we'd need to initialise the Cryptree differently.
//		// We therefore sign later.
////		cryptree.sign(ccDeleteModification);
//	}
}
