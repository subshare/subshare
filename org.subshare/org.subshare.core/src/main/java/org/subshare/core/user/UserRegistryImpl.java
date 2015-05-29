package org.subshare.core.user;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
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

import org.subshare.core.dto.DeletedUid;
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

public class UserRegistryImpl implements UserRegistry {

	private final Map<PgpKeyId, User> pgpKeyId2User = new HashMap<>();
	private final Map<Uid, User> userId2User = new LinkedHashMap<>();

	private final List<DeletedUid> deletedUserIds = new ArrayList<>();
	private final List<DeletedUid> deletedUserRepoKeyIds = new ArrayList<>();

	private List<User> cache_users;
	private Map<String, Set<User>> cache_email2Users;
	private Map<Uid, User> cache_userRepoKeyId2User;
//	private Map<Uid, User> cache_userId2User;

	private final File userRegistryFile;
	private boolean dirty;

	private Uid version;

	private static final class Holder {
		public static final UserRegistryImpl instance = new UserRegistryImpl();
	}

	public static UserRegistry getInstance() {
		return Holder.instance;
	}

	protected UserRegistryImpl() {
		userRegistryFile = createFile(ConfigDir.getInstance().getFile(), USER_REGISTRY_FILE_NAME);
		readUserRegistryFile();
		readPgpUsers();
		writeIfNeeded();
	}

	protected File getUserRegistryFile() {
		return userRegistryFile;
	}

	protected void readUserRegistryFile() {
		Uid version = null;
		final UserDtoConverter userDtoConverter = new UserDtoConverter();
		try (LockFile lockFile = acquireLockFile();) {
			lockFile.getLock().lock();
			try {
				deletedUserIds.clear();
				deletedUserRepoKeyIds.clear();
				if (userRegistryFile.exists()) {
					final UserRegistryDtoIo userRegistryDtoIo = new UserRegistryDtoIo();
					final UserRegistryDto userRegistryDto = userRegistryDtoIo.deserializeWithGz(userRegistryFile);
					for (final UserDto userDto : userRegistryDto.getUserDtos()) {
						final User user = userDtoConverter.fromUserDto(userDto);
						addUser(user);
					}
					version = userRegistryDto.getVersion();
					deletedUserIds.addAll(userRegistryDto.getDeletedUserIds());
					deletedUserRepoKeyIds.addAll(userRegistryDto.getDeletedUserRepoKeyIds());
				}
			} finally {
				lockFile.getLock().unlock();
			}
		}
		dirty = false;
		this.version = version != null ? version : new Uid();
	}

	public Uid getVersion() {
		return assertNotNull("version", version);
	}

	protected synchronized void readPgpUsers() {
		for (final PgpKey pgpKey : PgpRegistry.getInstance().getPgpOrFail().getMasterKeys()) {
			User user = pgpKeyId2User.get(pgpKey.getPgpKeyId());
			if (user == null) {
				boolean newUser = true;
				user = createUser();
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

	@Override
	public User createUser() {
		return new UserImpl();
	}

	@Override
	public synchronized Collection<User> getUsers() {
		if (cache_users == null)
			cache_users = Collections.unmodifiableList(new ArrayList<User>(userId2User.values()));

		return cache_users;
	}

	private void cleanCache() {
		cache_users = null;
		cache_email2Users = null;
		cache_userRepoKeyId2User = null;
//		cache_userId2User = null;
	}

	@Override
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

	@Override
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
		markDirty();
	}

	@Override
	public synchronized void removeUser(final User user) {
		_removeUser(user);
		deletedUserIds.add(new DeletedUid(user.getUserId()));
	}

	protected synchronized void _removeUser(final User user) {
		assertNotNull("user", user);
		userId2User.remove(user.getUserId());

		for (final PgpKeyId pgpKeyId : user.getPgpKeyIds())
			pgpKeyId2User.remove(pgpKeyId);

		user.removePropertyChangeListener(userPropertyChangeListener);

		cleanCache();
		markDirty();
	}

	@Override
	public synchronized User getUserByUserIdOrFail(final Uid userId) {
		final User user = getUserByUserId(userId);
		if (user == null)
			throw new IllegalArgumentException("No User found for userId=" + userId);

		return user;
	}

	@Override
	public synchronized User getUserByUserId(final Uid userId) {
		assertNotNull("userId", userId);
//		if (cache_userId2User == null) {
//			final Map<Uid, User> m = new HashMap<>();
//
//			for (final User user : getUsers())
//				m.put(user.getUserId(), user);
//
//			cache_userId2User = m;
//		}
//		return cache_userId2User.get(userId);
		return userId2User.get(userId);
	}

	@Override
	public User getUserByUserRepoKeyIdOrFail(final Uid userRepoKeyId) {
		final User user = getUserByUserRepoKeyId(userRepoKeyId);
		if (user == null)
			throw new IllegalArgumentException("No User found for userRepoKeyId=" + userRepoKeyId);

		return user;
	}

	@Override
	public synchronized User getUserByUserRepoKeyId(final Uid userRepoKeyId) {
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

	@Override
	public Collection<User> getUsersByPgpKeyIds(final Set<PgpKeyId> pgpKeyIds) {
		final List<User> result = new ArrayList<User>();
		iterateUsers: for (final User user : getUsers()) {
			for (final PgpKeyId pgpKeyId : user.getPgpKeyIds()) {
				if (pgpKeyIds.contains(pgpKeyId)) {
					result.add(user);
					continue iterateUsers;
				}
			}
		}
		return result;
	}

	private final PropertyChangeListener userPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			final User user = (User) evt.getSource();
			assertNotNull("user", user);

			markDirty();
			cleanCache();
		}
	};

	protected void markDirty() {
		dirty = true;
		version = new Uid();
	}

	protected LockFile acquireLockFile() {
		final File dir = ConfigDir.getInstance().getFile();
		return LockFileFactory.getInstance().acquire(createFile(dir, USER_REGISTRY_FILE_LOCK), 30000);
	}

	@Override
	public synchronized void writeIfNeeded() {
		if (dirty)
			write();
	}

	@Override
	public synchronized void write() {
		final UserRegistryDtoIo userRegistryDtoIo = new UserRegistryDtoIo();
		final UserRegistryDto userRegistryDto = createUserListDto();

		try (LockFile lockFile = acquireLockFile();) {
			lockFile.getLock().lock();
			try {
				final File newUserListFile = createFile(userRegistryFile.getParentFile(), userRegistryFile.getName() + ".new");
				userRegistryDtoIo.serializeWithGz(userRegistryDto, newUserListFile);
				userRegistryFile.delete();
				newUserListFile.renameTo(userRegistryFile);
			} finally {
				lockFile.getLock().unlock();
			}
		}
		dirty = false;
	}

	public void mergeFrom(final byte[] data) {
		assertNotNull("data", data);
		final UserRegistryDtoIo userRegistryDtoIo = new UserRegistryDtoIo();
		final UserRegistryDto userRegistryDto = userRegistryDtoIo.deserializeWithGz(new ByteArrayInputStream(data));
		mergeFrom(userRegistryDto);
	}

	protected synchronized void mergeFrom(final UserRegistryDto userRegistryDto) {
		assertNotNull("userRegistryDto", userRegistryDto);

		final List<UserDto> newUserDtos = new ArrayList<>(userRegistryDto.getUserDtos().size());
		for (final UserDto userDto : userRegistryDto.getUserDtos()) {
			final Uid userId = assertNotNull("userDto.userId", userDto.getUserId());
			final User user = getUserByUserId(userId);
			if (user == null)
				newUserDtos.add(userDto);
			else
				merge(user, userDto);
		}

		final Set<DeletedUid> newDeletedUserIds = new HashSet<>(userRegistryDto.getDeletedUserIds());
		newDeletedUserIds.removeAll(this.deletedUserIds);
		final Map<DeletedUid, User> newDeletedUsers = new HashMap<>(newDeletedUserIds.size());
		for (final DeletedUid deletedUserId : newDeletedUserIds) {
			final User user = getUserByUserId(deletedUserId.getUid());
			if (user != null)
				newDeletedUsers.put(deletedUserId, user);
		}

		final Set<DeletedUid> newDeletedUserRepoKeyIds = new HashSet<>(userRegistryDto.getDeletedUserRepoKeyIds());
		newDeletedUserRepoKeyIds.removeAll(this.deletedUserRepoKeyIds);
		final Map<DeletedUid, User> newDeletedUserRepoKeyId2User = new HashMap<>(newDeletedUserRepoKeyIds.size());
		for (final DeletedUid deletedUserRepoKeyId : newDeletedUserRepoKeyIds) {
			final User user = getUserByUserRepoKeyId(deletedUserRepoKeyId.getUid());
			if (user != null)
				newDeletedUserRepoKeyId2User.put(deletedUserRepoKeyId, user);
		}

		final UserDtoConverter userDtoConverter = new UserDtoConverter();
		for (final UserDto userDto : newUserDtos) {
			final User user = userDtoConverter.fromUserDto(userDto);
			addUser(user);
		}

		for (final Map.Entry<DeletedUid, User> me : newDeletedUsers.entrySet()) {
			_removeUser(me.getValue());
			deletedUserIds.add(me.getKey());
		}

		for (final Map.Entry<DeletedUid, User> me : newDeletedUserRepoKeyId2User.entrySet()) {
			// TODO implement this!
			throw new UnsupportedOperationException("NYI");
		}

		writeIfNeeded();
	}

	private void merge(final User toUser, final UserDto fromUserDto) {
		assertNotNull("toUser", toUser);
		assertNotNull("fromUserDto", fromUserDto);
		if (toUser.getChanged().before(fromUserDto.getChanged())) {
			toUser.setFirstName(fromUserDto.getFirstName());
			toUser.setLastName(fromUserDto.getLastName());

			if (!toUser.getEmails().equals(fromUserDto.getEmails())) {
				toUser.getEmails().clear();
				toUser.getEmails().addAll(fromUserDto.getEmails());
			}

			if (!toUser.getPgpKeyIds().equals(fromUserDto.getPgpKeyIds())) {
				toUser.getPgpKeyIds().clear();
				toUser.getPgpKeyIds().addAll(fromUserDto.getPgpKeyIds());
			}

			toUser.setChanged(fromUserDto.getChanged());
			if (!toUser.getChanged().equals(fromUserDto.getChanged())) // sanity check - to make sure listeners don't change it again
				throw new IllegalStateException("toUser.changed != fromUserDto.changed");
		}
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
