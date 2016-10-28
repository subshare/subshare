package org.subshare.gui.pgp.assignownertrust;

import java.util.HashSet;
import java.util.Set;

import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpOwnerTrust;
import org.subshare.core.user.User;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class AssignOwnerTrustData {

	private final ObjectProperty<PgpOwnerTrust> ownerTrust = new SimpleObjectProperty<>(this, "ownerTrust");

	private User user;

	private final Set<PgpKey> pgpKeys = new HashSet<>();

	private ObjectProperty<Boolean> assignToAllPgpKeys = new SimpleObjectProperty<>(this, "assignToAllPgpKeys");

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

	public ObjectProperty<Boolean> assignToAllPgpKeysProperty() {
		return assignToAllPgpKeys;
	}
	public Boolean getAssignToAllPgpKeys() {
		return assignToAllPgpKeys.get();
	}
	public void setAssignToAllPgpKeys(Boolean assignToAllPgpKeys) {
		this.assignToAllPgpKeys.set(assignToAllPgpKeys);
	}
}
