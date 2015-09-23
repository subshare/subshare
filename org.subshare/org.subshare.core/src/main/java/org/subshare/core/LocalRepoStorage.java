package org.subshare.core;

import java.util.Collection;
import java.util.UUID;

import co.codewizards.cloudstore.core.dto.FileChunkDto;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

/**
 * API to store data into the local repository (and retrieve it).
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface LocalRepoStorage {

	LocalRepoStorageFactory getLocalRepoStorageFactory();
	void setLocalRepoStorageFactory(LocalRepoStorageFactory localRepoStorageFactory);

	LocalRepoTransaction getTransaction();
	void setTransaction(LocalRepoTransaction transaction);

	UUID getRemoteRepositoryId();
	void setRemoteRepositoryId(UUID serverRepositoryId);

	String getRemotePathPrefix();
	void setRemotePathPrefix(String remotePathPrefix);

	void makeMetaOnly();

	boolean isMetaOnly();

	Collection<? extends FileChunkDto> getTempFileChunkDtos(String path);
	void putTempFileChunkDto(String path, long offset);
	void clearTempFileChunkDtos(String path);
	RepoFileDto getRepoFileDto(String path);

}
