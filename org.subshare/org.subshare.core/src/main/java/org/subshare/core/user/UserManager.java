package org.subshare.core.user;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import co.codewizards.cloudstore.core.config.ConfigDir;

public class UserManager {

	private final List<User> users = new ArrayList<User>();

	public UserManager() {
		readUsers();
	}

	private void readUsers() {
		try {
			ConfigDir.getInstance().getFile().createFileInputStream();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}


}
