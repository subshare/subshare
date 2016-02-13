package org.subshare.core.repo.sync;

import static co.codewizards.cloudstore.core.util.HashUtil.*;
import static org.subshare.core.repo.sync.PaddingUtil.*;

import java.net.URL;

import org.subshare.core.LocalRepoStorage;
import org.subshare.core.LocalRepoStorageFactoryRegistry;
import org.subshare.core.dto.SsDeleteModificationDto;
import org.subshare.core.dto.SsFileChunkDto;
import org.subshare.core.dto.SsNormalFileDto;
import org.subshare.core.repo.transport.CryptreeClientFileRepoTransport;
import org.subshare.core.repo.transport.CryptreeRepoTransport;
import org.subshare.core.repo.transport.CryptreeRestRepoTransport;

import co.codewizards.cloudstore.core.dto.ChangeSetDto;
import co.codewizards.cloudstore.core.dto.DeleteModificationDto;
import co.codewizards.cloudstore.core.dto.DirectoryDto;
import co.codewizards.cloudstore.core.dto.FileChunkDto;
import co.codewizards.cloudstore.core.dto.ModificationDto;
import co.codewizards.cloudstore.core.dto.NormalFileDto;
import co.codewizards.cloudstore.core.dto.RepoFileDtoTreeNode;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync;
import co.codewizards.cloudstore.core.repo.transport.CollisionException;
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
				final LocalRepoStorage lrs = LocalRepoStorageFactoryRegistry.getInstance().getLocalRepoStorageFactoryOrFail().getLocalRepoStorageOrCreate(transaction);
//				final Cryptree cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(transaction, remoteRepositoryId);
				metaOnly = lrs.isMetaOnly();
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
					throws CollisionException {
		if (toRepoTransport instanceof CryptreeRepoTransport)
			((CryptreeRepoTransport) toRepoTransport).beginPutFile(path, (SsNormalFileDto) fromNormalFileDto);
		else
			super.beginPutFile(fromRepoTransport, toRepoTransport, repoFileDtoTreeNode, path, fromNormalFileDto);
	}

	@Override
	protected byte[] getFileData(RepoTransport fromRepoTransport, RepoTransport toRepoTransport,
			RepoFileDtoTreeNode repoFileDtoTreeNode, String path, FileChunkDto fileChunkDto) {
		final SsFileChunkDto fcDto = (SsFileChunkDto) fileChunkDto;

		byte[] fileData = fromRepoTransport.getFileData(path, fileChunkDto.getOffset(), fileChunkDto.getLength());
		if (fileData == null)
			return null; // file was deleted

		final byte[] fileDataNoPadding;
		if (toRepoTransport instanceof CryptreeRestRepoTransport) {
			fileDataNoPadding = fileData;
			fileData = addPadding(fileData, assertNotNegative(fcDto.getLengthWithPadding()) - fcDto.getLength());
		}
		else if (fromRepoTransport instanceof CryptreeRestRepoTransport) {
			fileData = removePadding(fileData);
			fileDataNoPadding = fileData;
		}
		else
			throw new IllegalStateException("WTF?!");

		if (fileDataNoPadding.length != fileChunkDto.getLength())
			return null; // file was modified

		if (fcDto.getLength() > 0) { // a pure padding chunk has a length of 0 and a random SHA1 - thus no check here!
			if (!sha1(fileDataNoPadding).equals(fileChunkDto.getSha1()))
				return null; // file was modified
		}

		return fileData;
	}

	protected static int assertNotNegative(final int value) {
		if (value < 0)
			throw new IllegalArgumentException("value < 0");

		return value;
	}

	@Override
	protected void putFileData(RepoTransport fromRepoTransport, RepoTransport toRepoTransport,
			RepoFileDtoTreeNode repoFileDtoTreeNode, String path, FileChunkDto fileChunkDto, byte[] fileData) {

		super.putFileData(fromRepoTransport, toRepoTransport, repoFileDtoTreeNode, path, fileChunkDto, fileData);
	}

	@Override
	protected void endPutFile(final RepoTransport fromRepoTransport, final RepoTransport toRepoTransport,
			final RepoFileDtoTreeNode repoFileDtoTreeNode, final String path, final NormalFileDto fromNormalFileDto) {
		if (toRepoTransport instanceof CryptreeRestRepoTransport)
			((CryptreeRestRepoTransport) toRepoTransport).endPutFile(path, fromNormalFileDto);
		else if (toRepoTransport instanceof CryptreeClientFileRepoTransport)
			((CryptreeClientFileRepoTransport) toRepoTransport).endPutFile(path, (SsNormalFileDto) fromNormalFileDto);
		else
			super.endPutFile(fromRepoTransport, toRepoTransport, repoFileDtoTreeNode, path, fromNormalFileDto);
	}

	@Override
	protected void makeDirectory(RepoTransport fromRepoTransport, RepoTransport toRepoTransport,
			RepoFileDtoTreeNode repoFileDtoTreeNode, String path, DirectoryDto directoryDto) {
		super.makeDirectory(fromRepoTransport, toRepoTransport, repoFileDtoTreeNode, path, directoryDto);
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
