package org.subshare.core.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ServerRegistryDto {

	private List<ServerDto> serverDtos;

	private List<DeletedUid> deletedServerIds;

	public List<ServerDto> getServerDtos() {
		if (serverDtos == null)
			serverDtos = new ArrayList<>();

		return serverDtos;
	}
	public void setServerDtos(List<ServerDto> serverDtos) {
		this.serverDtos = serverDtos;
	}

	public List<DeletedUid> getDeletedServerIds() {
		if (deletedServerIds == null)
			deletedServerIds = new ArrayList<>();

		return deletedServerIds;
	}
	public void setDeletedServerIds(List<DeletedUid> deletedServerIds) {
		this.deletedServerIds = deletedServerIds;
	}
}
