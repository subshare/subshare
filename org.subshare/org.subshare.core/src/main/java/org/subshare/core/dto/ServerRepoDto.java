package org.subshare.core.dto;

import java.util.UUID;

import co.codewizards.cloudstore.core.dto.Uid;

public class ServerRepoDto {

	private UUID repositoryId;
	private String name;
	private Uid serverId;
	private Uid userId;

	public UUID getRepositoryId() {
		return repositoryId;
	}
	public void setRepositoryId(UUID repositoryId) {
		this.repositoryId = repositoryId;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public Uid getServerId() {
		return serverId;
	}
	public void setServerId(Uid serverId) {
		this.serverId = serverId;
	}

	public Uid getUserId() {
		return userId;
	}
	public void setUserId(Uid userId) {
		this.userId = userId;
	}
}
