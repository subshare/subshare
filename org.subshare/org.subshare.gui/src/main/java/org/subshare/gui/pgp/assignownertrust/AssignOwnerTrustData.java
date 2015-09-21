package org.subshare.gui.pgp.assignownertrust;

import java.util.HashSet;
import java.util.Set;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpOwnerTrust;
import org.subshare.core.user.User;

public class AssignOwnerTrustData {

	private final ObjectProperty<PgpOwnerTrust> ownerTrust = new SimpleObjectProperty<PgpOwnerTrust>(this, "ownerTrust");

	private User user;

	private final Set<PgpKey> pgpKeys = new HashSet<>();

	private Boolean assignToAllPgpKeys;

	public PgpOwnerTrust getOwnerTrust() {
		return ownerTrust.get();
	}
	public void setOwnerTrust(PgpOwnerTrust ownerTrust) {
		this.ownerTrust.set(ownerTrust);
	}
	public ObjectProperty<PgpOwnerTrust> ownerTrustProperty() {
		return ownerTrust;
	}

	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}

	public Set<PgpKey> getPgpKeys() {
		return pgpKeys;
	}

	public Boolean getAssignToAllPgpKeys() {
		return assignToAllPgpKeys;
	}
	public void setAssignToAllPgpKeys(Boolean assignToAllPgpKeys) {
		this.assignToAllPgpKeys = assignToAllPgpKeys;
	}
}
