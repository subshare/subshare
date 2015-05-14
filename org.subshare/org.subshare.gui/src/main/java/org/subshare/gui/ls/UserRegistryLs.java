package org.subshare.gui.ls;

import org.subshare.core.user.UserRegistry;
import org.subshare.core.user.UserRegistryImpl;

import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class UserRegistryLs {

	private UserRegistryLs() {
	}

	public static UserRegistry getUserRegistry() {
		return LocalServerClient.getInstance().invokeStatic(UserRegistryImpl.class, "getInstance");
	}
}
