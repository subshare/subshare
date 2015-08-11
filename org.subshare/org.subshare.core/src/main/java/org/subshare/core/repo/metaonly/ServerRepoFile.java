package org.subshare.core.repo.metaonly;

import java.net.URL;
import java.util.List;
import java.util.UUID;

import org.subshare.core.repo.ServerRepo;
import org.subshare.core.server.Server;

import co.codewizards.cloudstore.core.dto.Uid;

public interface ServerRepoFile {
	long getRepoFileId();

	ServerRepoFile getParent();

	Server getServer();

	ServerRepo getServerRepo();

	UUID getLocalRepositoryId();

	String getLocalName();
	String getLocalPath();

	Uid getCryptoRepoFileId();
	String getServerPath();

	List<ServerRepoFile> getChildren();

	ServerRepoFileType getType();

	URL getServerUrl();
}
