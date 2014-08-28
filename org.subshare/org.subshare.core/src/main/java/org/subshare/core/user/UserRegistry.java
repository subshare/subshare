package org.subshare.core.user;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;

import java.util.ArrayList;
import java.util.List;

import org.subshare.core.dto.UserListDto;
import org.subshare.core.dto.jaxb.UserListDtoIo;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.oio.File;

public class UserRegistry {

	public static final String USER_LIST_FILE_NAME = "userList.xml.gz";
	public static final String USER_LIST_LOCK = USER_LIST_FILE_NAME + ".lock";

	private final List<User> users = new ArrayList<User>();
	private final File userListFile;
	private final UserListDtoIo userListDtoIo = new UserListDtoIo();

	public UserRegistry() {
		userListFile = createFile(ConfigDir.getInstance().getFile(), USER_LIST_FILE_NAME);
		readUserListFile();
	}

	private void readUserListFile() {
		try (LockFile lockFile = acquireLockFile();) {
			lockFile.getLock().lock();
			try {
				if (userListFile.exists()) {
					final UserListDto userListDto = userListDtoIo.deserializeWithGz(userListFile);

				}
			} finally {
				lockFile.getLock().unlock();
			}
		}
	}

	private LockFile acquireLockFile() {
		final File dir = ConfigDir.getInstance().getFile();
		return LockFileFactory.getInstance().acquire(createFile(dir, USER_LIST_LOCK), 30000);
	}

	private void writeUserListFile() {
		final UserListDto userListDto = createUserListDto();

		try (LockFile lockFile = acquireLockFile();) {
			lockFile.getLock().lock();
			try {
				final File newUserListFile = createFile(userListFile.getParentFile(), userListFile.getName() + ".new");
				userListDtoIo.serializeWithGz(userListDto, newUserListFile);
				userListFile.delete();
				newUserListFile.renameTo(userListFile);
			} finally {
				lockFile.getLock().unlock();
			}
		}
	}

	private synchronized UserListDto createUserListDto() {
		final UserListDto userListDto = new UserListDto();
		for (final User user : users) {

		}
		return userListDto;
	}

}
