package org.subshare.core.repo.metaonly;

import java.util.List;

public interface ServerRepoFile {

	ServerRepoFile getParent();

	String getLocalName();

	List<ServerRepoFile> getChildren();

	ServerRepoFileType getType();
}
