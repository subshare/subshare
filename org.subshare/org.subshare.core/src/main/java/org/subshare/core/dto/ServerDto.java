package org.subshare.core.dto;

import co.codewizards.cloudstore.core.dto.Uid;

public class ServerDto {

	private Uid serverId;

	private String name;

	private String url;

	public Uid getServerId() {
		return serverId;
	}
	public void setServerId(Uid serverId) {
		this.serverId = serverId;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}

}
