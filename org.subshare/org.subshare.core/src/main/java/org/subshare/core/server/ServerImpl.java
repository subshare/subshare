package org.subshare.core.server;

import static co.codewizards.cloudstore.core.util.DateUtil.*;
import static java.util.Objects.*;

import java.net.URL;
import java.util.Date;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.bean.AbstractBean;

public class ServerImpl extends AbstractBean<Server.Property> implements Cloneable, Server {

	public ServerImpl() {
		this(null);
	}

	public ServerImpl(final Uid serverId) {
		this.serverId = serverId == null ? new Uid() : serverId;
	}

	private final Uid serverId;

	private String name;

	private URL url;

	private Date changed = now();

	@Override
	public synchronized Uid getServerId() {
		return serverId;
	}

	@Override
	public synchronized String getName() {
		return name;
	}
	@Override
	public void setName(final String name) {
		setPropertyValue(PropertyEnum.name, name);
		updateChanged();
	}

	@Override
	public synchronized URL getUrl() {
		return url;
	}
	@Override
	public void setUrl(final URL url) {
		setPropertyValue(PropertyEnum.url, url);
		updateChanged();
	}

	@Override
	public synchronized Date getChanged() {
		return changed;
	}
	@Override
	public void setChanged(final Date changed) {
		requireNonNull(changed, "changed");
		setPropertyValue(PropertyEnum.changed, changed);
	}

	protected void updateChanged() {
		setChanged(now());
	}

	@Override
	public Server clone() {
		return (ServerImpl) super.clone();
	}
}
