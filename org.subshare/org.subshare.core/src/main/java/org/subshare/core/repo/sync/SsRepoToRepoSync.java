package org.subshare.core.repo.sync;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.HashUtil.*;
import static co.codewizards.cloudstore.core.util.IOUtil.*;

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
					throws DeleteModificationCollisionException {
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

	private static final int chunkPayloadLengthBytesLength = 4;

	private byte[] addPadding(final byte[] fileData, final int paddingLength) {
		assertNotNull("fileData", fileData);

		if (paddingLength < 0)
			throw new IllegalArgumentException("paddingLength < 0");

		int index = -1;
		final byte[] result = new byte[1 + chunkPayloadLengthBytesLength + fileData.length + paddingLength];
		result[++index] = 1; // version

		final byte[] lengthBytes = intToBytes(fileData.length);
		if (lengthBytes.length != chunkPayloadLengthBytesLength)
			throw new IllegalStateException("lengthBytes.length != chunkPayloadLengthBytesLength");

		for (int i = 0; i < lengthBytes.length; ++i)
			result[++index] = lengthBytes[i];

		System.arraycopy(fileData, 0, result, ++index, fileData.length); // 0-padding
		return result;
	}

	private byte[] removePadding(final byte[] fileData) {
		assertNotNull("fileData", fileData);
		// We do *not* pass the paddingLength as parameter but instead encode it (or more precisely the payload-length)
		// into the fileData to ensure we *never* encounter an inconsistency between data and meta-data. Such
		// inconsistencies may happen, if a file is modified during transport and in our current solution, this
		// situation is cleanly detected and causes a warning + abortion of the transfer (causing the transfer
		// to be re-tried later).

		int index = -1;
		final byte version = fileData[++index];
		if (version != 1)
			throw new IllegalArgumentException(String.format("version == %d != 1", version));

		final byte[] lengthBytes = new byte[chunkPayloadLengthBytesLength];
		for (int i = 0; i < lengthBytes.length; ++i)
			lengthBytes[i] = fileData[++index];

		final int length = bytesToInt(lengthBytes);

		final byte[] result = new byte[length];
		System.arraycopy(fileData, ++index, result, 0, result.length);
		return result;
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
