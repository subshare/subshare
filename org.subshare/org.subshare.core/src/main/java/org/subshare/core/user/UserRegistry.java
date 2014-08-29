package org.subshare.core.user;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.subshare.core.dto.UserDto;
import org.subshare.core.dto.UserListDto;
import org.subshare.core.dto.jaxb.UserListDtoIo;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpRegistry;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.oio.File;

public class UserRegistry {

	public static final String USER_LIST_FILE_NAME = "userList.xml.gz";
	public static final String USER_LIST_LOCK = USER_LIST_FILE_NAME + ".lock";

	private final Map<Long, User> pgpKeyId2User = new HashMap<Long, User>();
	private final List<User> users = new ArrayList<User>();
	private List<User> _users;
	private final File userListFile;
	private final UserListDtoIo userListDtoIo = new UserListDtoIo();

	private static final class Holder {
		public static final UserRegistry instance = new UserRegistry();
	}

	public static UserRegistry getInstance() {
		return Holder.instance;
	}

	protected UserRegistry() {
		userListFile = createFile(ConfigDir.getInstance().getFile(), USER_LIST_FILE_NAME);
		readUserListFile();
		readPgpUsers();
	}

	protected void readUserListFile() {
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

	protected void readPgpUsers() {
		for (final PgpKey pgpKey : PgpRegistry.getInstance().getPgpOrFail().getMasterKeys()) {
			User user = pgpKeyId2User.get(pgpKey.getPgpKeyId());
			if (user == null) {
				user = new User();
				pgpKeyId2User.put(pgpKey.getPgpKeyId(), user);
				users.add(user);
				_users = null;
				for (final String userId : pgpKey.getUserIds())
					populateUserFromPgpUserId(user, userId);
			}

			if (! user.getPgpKeyIds().contains(pgpKey.getPgpKeyId()))
				user.getPgpKeyIds().add(pgpKey.getPgpKeyId());
		}
	}

	private void populateUserFromPgpUserId(final User user, String pgpUserId) {
		assertNotNull("user", user);
		pgpUserId = assertNotNull("pgpUserId", pgpUserId).trim();

		final int lastLt = pgpUserId.lastIndexOf('<');
		final int lastGt = pgpUserId.lastIndexOf('>');
		if (lastLt < 0) {
			final int lastSpace = pgpUserId.lastIndexOf(' ');
			if (lastSpace < 0) {
				final String email = lastGt < 0 ? pgpUserId : pgpUserId.substring(0, lastGt);
				user.getEmails().add(email);
			}
			else {
				final String email = lastGt < 0 ? pgpUserId.substring(lastSpace + 1) : pgpUserId.substring(lastSpace + 1, lastGt);
				user.getEmails().add(email);
			}
		}
		else { // this should apply to most or even all
			final String email = lastGt < 0 ? pgpUserId.substring(lastLt + 1) : pgpUserId.substring(lastLt + 1, lastGt);
			final String fullName = pgpUserId.substring(0, lastLt).trim();

			final String[] firstAndLastName = extractFirstAndLastNameFromFullName(fullName);

			user.getEmails().add(email);

			if (!firstAndLastName[0].isEmpty())
				user.setFirstName(firstAndLastName[0]);

			if (!firstAndLastName[1].isEmpty())
				user.setLastName(firstAndLastName[1]);
		}
	}

	private String[] extractFirstAndLastNameFromFullName(String fullName) {
		fullName = assertNotNull("fullName", fullName).trim();

		if (fullName.endsWith(")")) {
			final int lastOpenBracket = fullName.lastIndexOf('(');
			if (lastOpenBracket >= 0) {
				fullName = fullName.substring(0, lastOpenBracket).trim();
			}
		}

		final int lastSpace = fullName.lastIndexOf(' ');
		if (lastSpace < 0)
			return new String[] { "", fullName };
		else {
			final String firstName = fullName.substring(0, lastSpace).trim();
			final String lastName = fullName.substring(lastSpace + 1).trim();
			return new String[] { firstName, lastName };
		}
	}

	public synchronized Collection<User> getUsers() {
		if (_users == null)
			_users = Collections.unmodifiableList(new ArrayList<User>(users));

		return _users;
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
		final UserDtoConverter userDtoConverter = new UserDtoConverter();
		final UserListDto userListDto = new UserListDto();
		for (final User user : users) {
			final UserDto userDto = userDtoConverter.toUserDto(user);
			userListDto.getUserDtos().add(userDto);
		}
		return userListDto;
	}

}
