package org.subshare.gui.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collection;
import java.util.EventObject;
import java.util.LinkedHashSet;
import java.util.Set;

import org.subshare.core.user.User;

public class EditUserEvent extends EventObject {
	private static final long serialVersionUID = 1L;

	private final Set<? extends User> users;

	public EditUserEvent(final EditUserManager source, final Collection<? extends User> users) {
		super(source);
		this.users = new LinkedHashSet<>(assertNotNull(users, "users"));
	}

	public Set<? extends User> getUsers() {
		return users;
	}

	@Override
	public EditUserManager getSource() {
		return (EditUserManager) super.getSource();
	}
}
