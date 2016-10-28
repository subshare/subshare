package org.subshare.core.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.Uid;

@XmlRootElement
public class ServerRepoRegistryDto {

	private List<ServerRepoDto> serverRepoDtos;

	private List<DeletedUUID> deletedServerRepoIds;

	private Uid version;

	public List<ServerRepoDto> getServerRepoDtos() {
		if (serverRepoDtos == null)
			serverRepoDtos = new ArrayList<>();

		return serverRepoDtos;
	}
	public void setServerRepoDtos(List<ServerRepoDto> serverRepoDtos) {
		this.serverRepoDtos = serverRepoDtos;
	}

	public List<DeletedUUID> getDeletedServerRepoIds() {
		if (deletedServerRepoIds == null)
			deletedServerRepoIds = new ArrayList<>();

		return deletedServerRepoIds;
	}
	public void setDeletedServerIds(List<DeletedUUID> deletedServerRepoIds) {
		this.deletedServerRepoIds = deletedServerRepoIds;
	}

	public Uid getVersion() {
		return version;
	}
	public void setVersion(Uid version) {
		this.version = version;
	}
}
