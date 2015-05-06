package org.subshare.core.user;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.subshare.core.dto.UserDto;
import org.subshare.core.dto.UserRegistryDto;
import org.subshare.core.dto.jaxb.UserRegistryDtoIo;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpRegistry;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.oio.File;

public class UserRegistry {

	public static final String USER_LIST_FILE_NAME = "userList.xml.gz";
	public static final String USER_LIST_LOCK = USER_LIST_FILE_NAME + ".lock";

	private final Map<PgpKeyId, User> pgpKeyId2User = new HashMap<>();
	private final Map<Uid, User> userId2User = new LinkedHashMap<>();

	private List<User> cache_users;
	private Map<String, Set<User>> cache_email2Users;
	private Map<Uid, User> cache_userRepoKeyId2User;

	private final File userListFile;
	private boolean dirty;

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
		writeIfNeeded();
	}

	protected void readUserListFile() {
		final UserDtoConverter userDtoConverter = new UserDtoConverter();
		try (LockFile lockFile = acquireLockFile();) {
			lockFile.getLock().lock();
			try {
				if (userListFile.exists()) {
					final UserRegistryDtoIo userRegistryDtoIo = new UserRegistryDtoIo();
					final UserRegistryDto userRegistryDto = userRegistryDtoIo.deserializeWithGz(userListFile);
					for (final UserDto userDto : userRegistryDto.getUserDtos()) {
						final User user = userDtoConverter.fromUserDto(userDto);
						addUser(user);
					}
				}
			} finally {
				lockFile.getLock().unlock();
			}
		}
		dirty = false;
	}

	protected synchronized void readPgpUsers() {
		for (final PgpKey pgpKey : PgpRegistry.getInstance().getPgpOrFail().getMasterKeys()) {
			User user = pgpKeyId2User.get(pgpKey.getPgpKeyId());
			if (user == null) {
				boolean newUser = true;
				user = new User();
				user.setUserId(new Uid());
				for (final String userId : pgpKey.getUserIds())
					populateUserFromPgpUserId(user, userId);

				// Try to deduplicate by e-mail address.
				for (final String email : user.getEmails()) {
					final Collection<User> usersByEmail = getUsersByEmail(email);
					if (! usersByEmail.isEmpty()) {
						newUser = false;
						user = usersByEmail.iterator().next();

						for (final String userId : pgpKey.getUserIds())
							populateUserFromPgpUserId(user, userId);
					}
				}

				if (newUser)
					addUser(user);
			}

			if (! user.getPgpKeyIds().contains(pgpKey.getPgpKeyId()))
				user.getPgpKeyIds().add(pgpKey.getPgpKeyId());

			pgpKeyId2User.put(pgpKey.getPgpKeyId(), user);
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

			if (isEmpty(user.getFirstName()) && !firstAndLastName[0].isEmpty())
				user.setFirstName(firstAndLastName[0]);

			if (isEmpty(user.getLastName()) && !firstAndLastName[1].isEmpty())
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
		if (cache_users == null)
			cache_users = Collections.unmodifiableList(new ArrayList<User>(userId2User.values()));

		return cache_users;
	}

	private void cleanCache() {
		cache_users = null;
		cache_email2Users = null;
		cache_userRepoKeyId2User = null;
	}

	public synchronized Collection<User> getUsersByEmail(final String email) {
		assertNotNull("email", email);
		if (cache_email2Users == null) {
			final Map<String, Set<User>> cache_email2Users = new HashMap<String, Set<User>>();
			for (final User user : getUsers()) {
				for (String eml : user.getEmails()) {
					eml = eml.toLowerCase(Locale.UK);
					Set<User> users = cache_email2Users.get(eml);
					if (users == null) {
						users = new HashSet<User>();
						cache_email2Users.put(eml, users);
					}
					users.add(user);
				}
			}

			for (final Map.Entry<String, Set<User>> me : cache_email2Users.entrySet())
				me.setValue(Collections.unmodifiableSet(me.getValue()));

			this.cache_email2Users = Collections.unmodifiableMap(cache_email2Users);
		}

		final Set<User> users = cache_email2Users.get(email.toLowerCase(Locale.UK));
		return users != null ? users : Collections.unmodifiableList(new LinkedList<User>());
	}

	public synchronized void addUser(final User user) {
		assertNotNull("user", user);
		assertNotNull("user.userId", user.getUserId());
		userId2User.put(user.getUserId(), user);
		// TODO we either need to hook listeners into user and get notified about all changes to update this registry!
		// OR we need to provide a public write/save/store (or similarly named) method.

		for (final PgpKeyId pgpKeyId : user.getPgpKeyIds())
			pgpKeyId2User.put(pgpKeyId, user); // TODO what about collisions? remove pgpKeyId from the other user?!

		user.addPropertyChangeListener(userPropertyChangeListener);

		cleanCache();
		dirty = true;
	}

	public User getUserOrFail(final Uid userRepoKeyId) {
		final User user = getUser(userRepoKeyId);
		if (user == null)
			throw new IllegalArgumentException("No User found for userRepoKeyId=" + userRepoKeyId);

		return user;
	}

	public synchronized User getUser(final Uid userRepoKeyId) {
		assertNotNull("userRepoKeyId", userRepoKeyId);
		if (cache_userRepoKeyId2User == null) {
			final Map<Uid, User> m = new HashMap<>();

			for (final User user : getUsers()) {
				final UserRepoKeyRing userRepoKeyRing = user.getUserRepoKeyRing();
				if (userRepoKeyRing != null) {
					for (final UserRepoKey userRepoKey : userRepoKeyRing.getUserRepoKeys())
						if (m.put(userRepoKey.getUserRepoKeyId(), user) != null)
							throw new IllegalStateException("Duplicate userRepoKeyId!!! WTF?! " + userRepoKey.getUserRepoKeyId());
				}
				else {
					for (final UserRepoKey.PublicKey publicKey : user.getUserRepoKeyPublicKeys()) {
						if (m.put(publicKey.getUserRepoKeyId(), user) != null)
							throw new IllegalStateException("Duplicate userRepoKeyId!!! WTF?! " + publicKey.getUserRepoKeyId());
					}
				}
			}

			cache_userRepoKeyId2User = m;
		}
		return cache_userRepoKeyId2User.get(userRepoKeyId);
	}

	private final PropertyChangeListener userPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			dirty = true;
			cleanCache();
		}
	};

	private LockFile acquireLockFile() {
		final File dir = ConfigDir.getInstance().getFile();
		return LockFileFactory.getInstance().acquire(createFile(dir, USER_LIST_LOCK), 30000);
	}

	public synchronized void writeIfNeeded() {
		if (dirty)
			write();
	}

	public synchronized void write() {
		final UserRegistryDtoIo userRegistryDtoIo = new UserRegistryDtoIo();
		final UserRegistryDto userRegistryDto = createUserListDto();

		try (LockFile lockFile = acquireLockFile();) {
			lockFile.getLock().lock();
			try {
				final File newUserListFile = createFile(userListFile.getParentFile(), userListFile.getName() + ".new");
				userRegistryDtoIo.serializeWithGz(userRegistryDto, newUserListFile);
				userListFile.delete();
				newUserListFile.renameTo(userListFile);
			} finally {
				lockFile.getLock().unlock();
			}
		}
		dirty = false;
	}

	private synchronized UserRegistryDto createUserListDto() {
		final UserDtoConverter userDtoConverter = new UserDtoConverter();
		final UserRegistryDto userRegistryDto = new UserRegistryDto();
		for (final User user : userId2User.values()) {
			final UserDto userDto = userDtoConverter.toUserDto(user);
			userRegistryDto.getUserDtos().add(userDto);
		}
		return userRegistryDto;
	}
}
