package org.subshare.core.repo;

import static co.codewizards.cloudstore.core.util.Util.*;
import static java.util.Objects.*;

import java.util.Date;
import java.util.UUID;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.bean.AbstractBean;

public class ServerRepoImpl extends AbstractBean<ServerRepo.Property> implements ServerRepo {

	private final UUID repositoryId;
	private String name;
	private Uid serverId;
	private Uid userId;
	private Date changed = new Date();

	public ServerRepoImpl(final UUID repositoryId) {
		this.repositoryId = requireNonNull(repositoryId, "repositoryId)");
	}

	@Override
	public UUID getRepositoryId() {
		return repositoryId;
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
	public synchronized Uid getServerId() {
		return serverId;
	}

	@Override
	public void setServerId(final Uid serverId) {
		synchronized (this) {
			if (this.serverId != null && ! equal(this.serverId, serverId)) // TODO do we ever need to support changing this?
				throw new IllegalStateException("Cannot modify serverId!");
		}
		setPropertyValue(PropertyEnum.serverId, serverId);
		updateChanged();
	}

	@Override
	public synchronized Uid getUserId() {
		return userId;
	}
	@Override
	public void setUserId(Uid userId) {
		synchronized (this) {
			if (this.userId != null && ! equal(this.userId, userId)) // TODO do we ever need to support changing this?
				throw new IllegalStateException("Cannot modify userId!");
		}
		setPropertyValue(PropertyEnum.userId, userId);
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
		setChanged(new Date());
	}

	@Override
	public ServerRepo clone() {
		return (ServerRepoImpl) super.clone();
	}
}
