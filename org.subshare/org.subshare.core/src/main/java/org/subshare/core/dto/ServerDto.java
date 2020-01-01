package org.subshare.core.dto;

import static co.codewizards.cloudstore.core.util.DateUtil.*;

import java.util.Date;

import co.codewizards.cloudstore.core.Uid;

public class ServerDto {

	private Uid serverId;

	private String name;

	private String url;

	private Date changed;

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

	public Date getChanged() {
		return changed;
	}
	public void setChanged(Date changed) {
		this.changed = copyDate(changed);
	}
}
