package org.subshare.core.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ServerRepoRegistryDto {

	private List<ServerRepoDto> serverRepoDtos;

	private List<DeletedUid> deletedServerRepoIds;

	public List<ServerRepoDto> getServerRepoDtos() {
		if (serverRepoDtos == null)
			serverRepoDtos = new ArrayList<>();

		return serverRepoDtos;
	}
	public void setServerRepoDtos(List<ServerRepoDto> serverRepoDtos) {
		this.serverRepoDtos = serverRepoDtos;
	}

	public List<DeletedUid> getDeletedServerRepoIds() {
		if (deletedServerRepoIds == null)
			deletedServerRepoIds = new ArrayList<>();

		return deletedServerRepoIds;
	}
	public void setDeletedServerIds(List<DeletedUid> deletedServerRepoIds) {
		this.deletedServerRepoIds = deletedServerRepoIds;
	}
}
