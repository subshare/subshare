package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.UUID;

import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.PersistenceCapable;

import co.codewizards.cloudstore.local.persistence.RemoteRepository;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.SUPERCLASS_TABLE)
@Discriminator(strategy=DiscriminatorStrategy.VALUE_MAP, value="SsRemoteRepository")
public class SsRemoteRepository extends RemoteRepository {

	public SsRemoteRepository() {
	}
	public SsRemoteRepository(UUID repositoryId) {
		super(repositoryId);
	}

	private String remotePathPrefix;

	public String getRemotePathPrefix() {
		return remotePathPrefix;
	}
	public void setRemotePathPrefix(String remotePathPrefix) {
		if (! equal(this.remotePathPrefix, remotePathPrefix))
			this.remotePathPrefix = remotePathPrefix;
	}
}
