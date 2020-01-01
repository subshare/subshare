package org.subshare.core.dto;

import static co.codewizards.cloudstore.core.util.DateUtil.*;

import java.util.Date;
import java.util.UUID;

import co.codewizards.cloudstore.core.Uid;

public class ServerRepoDto {

	private UUID repositoryId;
	private String name;
	private Uid serverId;
	private Uid userId;
	private Date changed;

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

	public Date getChanged() {
		return changed;
	}
	public void setChanged(Date changed) {
		this.changed = copyDate(changed);
	}
}
