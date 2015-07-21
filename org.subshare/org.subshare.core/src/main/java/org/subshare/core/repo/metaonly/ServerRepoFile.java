package org.subshare.core.repo.metaonly;

import java.net.URL;
import java.util.List;

import org.subshare.core.repo.ServerRepo;
import org.subshare.core.server.Server;

import co.codewizards.cloudstore.core.dto.Uid;

public interface ServerRepoFile {

	ServerRepoFile getParent();

	Server getServer();

	ServerRepo getServerRepo();

	String getLocalName();
	String getLocalPath();

	Uid getCryptoRepoFileId();
	String getServerPath();

	List<ServerRepoFile> getChildren();

	ServerRepoFileType getType();

	URL getServerUrl();
}
