package org.subshare.core.user;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
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
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.dto.DeletedUid;
import org.subshare.core.dto.UserDto;
import org.subshare.core.dto.UserRegistryDto;
import org.subshare.core.dto.UserRepoKeyDto;
import org.subshare.core.dto.UserRepoKeyPublicKeyDto;
import org.subshare.core.dto.jaxb.UserRegistryDtoIo;
import org.subshare.core.fbor.FileBasedObjectRegistry;
import org.subshare.core.io.NullOutputStream;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpDecoder;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.pgp.PgpUserId;
import org.subshare.core.pgp.sync.PgpSync;
import org.subshare.core.pgp.sync.PgpSyncDaemonImpl;
import org.subshare.core.user.ImportUsersFromPgpKeysResult.ImportedUser;
import org.subshare.core.user.UserRepoKey.PublicKeyWithSignature;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.oio.File;

public class UserRegistryImpl extends FileBasedObjectRegistry implements UserRegistry {

	private static final Logger logger = LoggerFactory.getLogger(UserRegistryImpl.class);

	private static final String PAYLOAD_ENTRY_NAME = UserRegistryDto.class.getSimpleName() + ".xml";

	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	private final Map<Uid, User> userId2User = new LinkedHashMap<>();

	private final List<DeletedUid> deletedUserIds = new ArrayList<>();
	private final List<DeletedUid> deletedUserRepoKeyIds = new ArrayList<>();

	private List<User> cache_users;
	private Map<String, Set<User>> cache_email2Users;
	private Map<Uid, User> cache_userRepoKeyId2User;

	private final File userRegistryFile;
	private Uid version;

	private static final class Holder {
		public static final UserRegistryImpl instance = new UserRegistryImpl();
	}

	public static UserRegistry getInstance() {
		return Holder.instance;
	}

	protected UserRegistryImpl() {
		userRegistryFile = createFile(ConfigDir.getInstance().getFile(), USER_REGISTRY_FILE_NAME);
		read();
	}

	@Override
	protected String getContentType() {
		return "application/vnd.subshare.user-registry";
	}

	@Override
	protected File getFile() {
		return userRegistryFile;
	}

	@Override
	protected void read() {
		super.read();
		readPgpUsers();
		writeIfNeeded();
	}

	@Override
	protected void preRead() {
		version = null;
	}

	@Override
	protected void postRead() {
		if (version == null) {
//			version = new Uid(); // done by markDirty()
			markDirty();
		}
	}

	@Override
	protected void readPayloadEntry(ZipInputStream zin, ZipEntry zipEntry) throws IOException {
		if (!PAYLOAD_ENTRY_NAME.equals(zipEntry.getName())) {
			logger.warn("readPayloadEntry: Ignoring unexpected zip-entry: {}", zipEntry.getName());
			return;
		}
		final UserDtoConverter userDtoConverter = new UserDtoConverter();
		final UserRegistryDtoIo userRegistryDtoIo = new UserRegistryDtoIo();
		final UserRegistryDto userRegistryDto = userRegistryDtoIo.deserialize(zin);

		for (User user : getUsers())
			removeUser(user);

		for (final UserDto userDto : userRegistryDto.getUserDtos()) {
			final User user = userDtoConverter.fromUserDto(userDto);
			addUser(user);
		}

		deletedUserIds.clear();
		deletedUserIds.addAll(userRegistryDto.getDeletedUserIds());

		deletedUserRepoKeyIds.clear();
		deletedUserRepoKeyIds.addAll(userRegistryDto.getDeletedUserRepoKeyIds());

		version = userRegistryDto.getVersion();
	}

	public Uid getVersion() {
		return assertNotNull("version", version);
	}


	protected synchronized void readPgpUsers() {
		Collection<PgpKey> masterKeys = PgpRegistry.getInstance().getPgpOrFail().getMasterKeys();
		importUsersFromPgpKeys(masterKeys);
	}

	@Override
	public synchronized ImportUsersFromPgpKeysResult importUsersFromPgpKeys(final Collection<PgpKey> pgpKeys) {
		final List<User> newUsers = new ArrayList<>();
		final Set<Uid> modifiedUserIds = new HashSet<>();

		final Map<PgpKeyId, PgpKey> pgpKeyId2PgpKey = new HashMap<>();
		final Map<String, List<User>> email2NewUsers = new HashMap<>();
		final Map<PgpKeyId, List<User>> pgpKeyId2Users = createPgpKeyId2Users();
		for (PgpKey pgpKey : pgpKeys) {
			pgpKey = pgpKey.getMasterKey();

			pgpKeyId2PgpKey.put(pgpKey.getPgpKeyId(), pgpKey);

			List<User> usersByPgpKeyId = pgpKeyId2Users.get(pgpKey.getPgpKeyId());
			if (usersByPgpKeyId == null) {
				usersByPgpKeyId = new ArrayList<User>(1);
				pgpKeyId2Users.put(pgpKey.getPgpKeyId(), usersByPgpKeyId);
			}

			User user = usersByPgpKeyId.isEmpty() ? null : usersByPgpKeyId.get(0);
			if (user == null) {
				boolean newUser = true;
				user = createUser();
				for (final String userId : pgpKey.getUserIds())
					populateUserFromPgpUserId(user, userId);

				// Try to deduplicate by e-mail address.
				for (final String email : user.getEmails()) {
					final Collection<User> usersByEmail = getUsersByEmail(email);
					if (! usersByEmail.isEmpty()) {
						newUser = false;
						user = usersByEmail.iterator().next();

						for (final String userId : pgpKey.getUserIds()) {
							if (populateUserFromPgpUserId(user, userId) && !newUsers.contains(user))
								modifiedUserIds.add(user.getUserId());
						}
					}
				}

				if (newUser) {
					for (final String email : user.getEmails()) {
						List<User> l = email2NewUsers.get(email);
						if (l == null) {
							l = new ArrayList<>();
							email2NewUsers.put(email, l);
						}
						if (l.isEmpty())
							l.add(user);
						else {
							newUser = false;
							user = l.get(0);

							for (final String userId : pgpKey.getUserIds()) {
								if (populateUserFromPgpUserId(user, userId) && !newUsers.contains(user))
									modifiedUserIds.add(user.getUserId());
							}
						}
					}
				}

				if (newUser)
					newUsers.add(user);
			}

			if (! user.getPgpKeyIds().contains(pgpKey.getPgpKeyId()))
				user.getPgpKeyIds().add(pgpKey.getPgpKeyId());

			usersByPgpKeyId.add(user);
		}

		final Set<Uid> newUserIds = new HashSet<Uid>();
		for (User user : newUsers) {
			// The order of PgpKeyIds does not say anything - the numbers are random! I only sort
			// them for the sake of deterministic behaviour.
			final PgpKeyId pgpKeyId = new TreeSet<>(user.getPgpKeyIds()).iterator().next();
			final PgpKey pgpKey = pgpKeyId2PgpKey.get(pgpKeyId);
			assertNotNull("pgpKey", pgpKey);
			final Uid userId = new Uid(getLast16(pgpKey.getFingerprint()));
			user.setUserId(userId);
			addUser(user);
			newUserIds.add(user.getUserId());
		}

		final ImportUsersFromPgpKeysResult result = new ImportUsersFromPgpKeysResult();
		for (PgpKey pgpKey : pgpKeys) {
			final PgpKeyId pgpKeyId = pgpKey.getPgpKeyId();
			final List<User> users = pgpKeyId2Users.get(pgpKeyId);
			assertNotNull("users", users);
			for (final User user : users) {
				final boolean _new = newUserIds.contains(user.getUserId());
				final boolean modified = modifiedUserIds.contains(user.getUserId());

				if (_new && modified)
					throw new IllegalStateException("_new && modified :: userId=" + user.getUserId());

				final ImportedUser importedUser = new ImportedUser(user, _new, modified);

				List<ImportedUser> list = result.getPgpKeyId2ImportedUsers().get(pgpKeyId);
				if (list == null) {
					list = new ArrayList<>();
					result.getPgpKeyId2ImportedUsers().put(pgpKeyId, list);
				}
				list.add(importedUser);
			}
		}
		return result;
	}

	private synchronized Map<PgpKeyId, List<User>> createPgpKeyId2Users() {
		final Map<PgpKeyId, List<User>> result = new HashMap<PgpKeyId, List<User>>();
		for (final User user : userId2User.values()) {
			for (final PgpKeyId pgpKeyId : user.getPgpKeyIds()) {
				List<User> users = result.get(pgpKeyId);
				if (users == null) {
					users = new ArrayList<User>(1);
					result.put(pgpKeyId, users);
				}
				users.add(user);
			}
		}
		return result;
	}

	private static byte[] getLast16(byte[] fingerprint) {
		final byte[] result = new byte[16];

		if (fingerprint.length < result.length)
			throw new IllegalArgumentException("fingerprint.length < " + result.length);

		System.arraycopy(fingerprint, fingerprint.length - result.length, result, 0, result.length);
		return result;
	}

	private static boolean populateUserFromPgpUserId(final User user, final String pgpUserIdStr) {
		assertNotNull("user", user);
		boolean modified = false;
		final PgpUserId pgpUserId = new PgpUserId(assertNotNull("pgpUserIdStr", pgpUserIdStr));
		if (! isEmpty(pgpUserId.getEmail()) && ! user.getEmails().contains(pgpUserId.getEmail())) {
			user.getEmails().add(pgpUserId.getEmail());
			modified = true;
		}

		if (isEmpty(user.getFirstName()) && isEmpty(user.getLastName())) {
			modified = true;

			final String fullName = pgpUserId.getName();
			if (! isEmpty(fullName)) {
				final String[] firstAndLastName = extractFirstAndLastNameFromFullName(fullName);

				if (isEmpty(user.getFirstName()) && !firstAndLastName[0].isEmpty())
					user.setFirstName(firstAndLastName[0]);

				if (isEmpty(user.getLastName()) && !firstAndLastName[1].isEmpty())
					user.setLastName(firstAndLastName[1]);
			}
		}
		return modified;
	}

	private static String[] extractFirstAndLastNameFromFullName(String fullName) {
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

//		for (final PgpKeyId pgpKeyId : user.getPgpKeyIds())
//			pgpKeyId2User.put(pgpKeyId, user); // TODO what about collisions? remove pgpKeyId from the other user?!

		user.addPropertyChangeListener(userPropertyChangeListener);

		cleanCache();
		markDirty();

		firePropertyChange(PropertyEnum.users, null, getUsers()); // TODO refactor to use an ObservableList instead?! all analogue to ServerRegistry?!
	}

	@Override
	public synchronized void removeUser(final User user) {
		_removeUser(user);
		deletedUserIds.add(new DeletedUid(user.getUserId()));
	}

	protected synchronized void _removeUser(final User user) {
		assertNotNull("user", user);
		userId2User.remove(user.getUserId());

//		for (final PgpKeyId pgpKeyId : user.getPgpKeyIds())
//			pgpKeyId2User.remove(pgpKeyId);

		user.removePropertyChangeListener(userPropertyChangeListener);

		cleanCache();
		markDirty();

		firePropertyChange(PropertyEnum.users, null, getUsers()); // TODO refactor to use an ObservableList instead?! all analogue to ServerRegistry?!
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

			firePropertyChange(PropertyEnum.users_user, null, user);
		}
	};

	@Override
	protected void markDirty() {
		super.markDirty();
		version = new Uid();
		deferredWrite();
	}

	@Override
	protected void writePayload(ZipOutputStream zout) throws IOException {
		final UserRegistryDtoIo userRegistryDtoIo = new UserRegistryDtoIo();
		final UserRegistryDto userRegistryDto = createUserRegistryDto();

		zout.putNextEntry(new ZipEntry(PAYLOAD_ENTRY_NAME));
		userRegistryDtoIo.serialize(userRegistryDto, zout);
		zout.closeEntry();
	}

	@Override
	protected void mergeFrom(ZipInputStream zin, ZipEntry zipEntry) {
		if (PAYLOAD_ENTRY_NAME.equals(zipEntry.getName())) {
			final UserRegistryDtoIo userRegistryDtoIo = new UserRegistryDtoIo();
			final UserRegistryDto userRegistryDto = userRegistryDtoIo.deserialize(zin);
			mergeFrom(userRegistryDto);
		}
	}

	protected synchronized void mergeFrom(final UserRegistryDto userRegistryDto) {
		assertNotNull("userRegistryDto", userRegistryDto);

		final Set<PgpKeyId> pgpKeyIds = new HashSet<>();
		for (UserDto userDto : userRegistryDto.getUserDtos()) {
			if (userDto.getUserRepoKeyRingDto() != null) {
				for (UserRepoKeyDto userRepoKeyDto : userDto.getUserRepoKeyRingDto().getUserRepoKeyDtos())
					pgpKeyIds.addAll(getSigningPgpKeyIds(userRepoKeyDto.getSignedPublicKeyData()));
			}
			for (UserRepoKeyPublicKeyDto userRepoKeyPublicKeyDto : userDto.getUserRepoKeyPublicKeyDtos())
				pgpKeyIds.addAll(getSigningPgpKeyIds(userRepoKeyPublicKeyDto.getSignedPublicKeyData()));
		}
		try {
			PgpSync.setDownSyncPgpKeyIds(pgpKeyIds);
			PgpSyncDaemonImpl.getInstance().sync();
		} finally {
			PgpSync.setDownSyncPgpKeyIds(null);
		}

		final Set<Uid> deletedUserIdSet = new HashSet<>(this.deletedUserIds.size());
		for (final DeletedUid deletedUid : this.deletedUserIds)
			deletedUserIdSet.add(deletedUid.getUid());

		final Set<Uid> deletedUserRepoKeyIdSet = new HashSet<>(this.deletedUserRepoKeyIds.size());
		for (final DeletedUid deletedUid : this.deletedUserRepoKeyIds)
			deletedUserRepoKeyIdSet.add(deletedUid.getUid());

		final List<UserDto> newUserDtos = new ArrayList<>(userRegistryDto.getUserDtos().size());
		for (final UserDto userDto : userRegistryDto.getUserDtos()) {
			final Uid userId = assertNotNull("userDto.userId", userDto.getUserId());
			if (deletedUserIdSet.contains(userId))
				continue;

			final User user = getUserByUserId(userId);
			if (user == null)
				newUserDtos.add(userDto);
			else
				merge(user, userDto, deletedUserRepoKeyIdSet);
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

		if (hasMissingPgpKeys())
			PgpSyncDaemonImpl.getInstance().sync();
	}

	private Set<PgpKeyId> getSigningPgpKeyIds(final byte[] signedData) {
		if (signedData == null)
			return Collections.emptySet();

		final Pgp pgp = PgpRegistry.getInstance().getPgpOrFail();
		PgpDecoder decoder = pgp.createDecoder(new ByteArrayInputStream(signedData), new NullOutputStream());
		decoder.setFailOnMissingSignPgpKey(false);
		try {
			decoder.decode();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return decoder.getSignPgpKeyIds();
	}

	private boolean hasMissingPgpKeys() {
		final Pgp pgp = PgpRegistry.getInstance().getPgpOrFail();
		for (final User user : getUsers()) {
			for (final PgpKeyId pgpKeyId : user.getPgpKeyIds()) {
				if (pgp.getPgpKey(pgpKeyId) == null)
					return true;
			}
		}
		return false;
	}

	private void merge(final User toUser, final UserDto fromUserDto, final Set<Uid> deletedUserRepoKeyIdSet) {
		assertNotNull("toUser", toUser);
		assertNotNull("fromUserDto", fromUserDto);
		UserRepoKeyDtoConverter userRepoKeyDtoConverter = null;
		UserRepoKeyPublicKeyDtoWithSignatureConverter userRepoKeyPublicKeyDtoWithSignatureConverter = null;

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

		// We merge the UserRepoKeys outside of the changed-timestamp-controlled area to make sure we never loose any!
		if (fromUserDto.getUserRepoKeyRingDto() != null) {
			for (final UserRepoKeyDto userRepoKeyDto : fromUserDto.getUserRepoKeyRingDto().getUserRepoKeyDtos()) {
				final Uid userRepoKeyId = assertNotNull("userRepoKeyDto.userRepoKeyId", userRepoKeyDto.getUserRepoKeyId());
				if (deletedUserRepoKeyIdSet.contains(userRepoKeyId))
					continue;

				UserRepoKey userRepoKey = toUser.getUserRepoKeyRingOrCreate().getUserRepoKey(userRepoKeyId);
				if (userRepoKey == null) {
					if (userRepoKeyDtoConverter == null)
						userRepoKeyDtoConverter = new UserRepoKeyDtoConverter();

					userRepoKey = userRepoKeyDtoConverter.fromUserRepoKeyDto(userRepoKeyDto);
					toUser.getUserRepoKeyRingOrCreate().addUserRepoKey(assertNotNull("userRepoKey", userRepoKey));
				}
			}
		}
		else {
			for (final UserRepoKeyPublicKeyDto userRepoKeyPublicKeyDto : fromUserDto.getUserRepoKeyPublicKeyDtos()) {
				final Uid userRepoKeyId = assertNotNull("userRepoKeyPublicKeyDto.userRepoKeyId", userRepoKeyPublicKeyDto.getUserRepoKeyId());
				if (deletedUserRepoKeyIdSet.contains(userRepoKeyId))
					continue;

				if (! contains(toUser.getUserRepoKeyPublicKeys(), userRepoKeyId)) {
					if (userRepoKeyPublicKeyDtoWithSignatureConverter == null)
						userRepoKeyPublicKeyDtoWithSignatureConverter = new UserRepoKeyPublicKeyDtoWithSignatureConverter();

					final PublicKeyWithSignature publicKeyWithSignature = userRepoKeyPublicKeyDtoWithSignatureConverter.fromUserRepoKeyPublicKeyDto(userRepoKeyPublicKeyDto);
					toUser.getUserRepoKeyPublicKeys().add(assertNotNull("publicKeyWithSignature", publicKeyWithSignature));
				}
			}
		}
	}

	private static boolean contains(Collection<PublicKeyWithSignature> userRepoKeyPublicKeys, final Uid userRepoKeyId) {
		for (final PublicKeyWithSignature publicKeyWithSignature : userRepoKeyPublicKeys) {
			if (userRepoKeyId.equals(publicKeyWithSignature.getUserRepoKeyId()))
				return true;
		}
		return false;
	}


	private synchronized UserRegistryDto createUserRegistryDto() {
		final UserDtoConverter userDtoConverter = new UserDtoConverter();
		final UserRegistryDto userRegistryDto = new UserRegistryDto();
		for (final User user : userId2User.values()) {
			final UserDto userDto = userDtoConverter.toUserDto(user);
			userRegistryDto.getUserDtos().add(userDto);
		}
		userRegistryDto.getDeletedUserIds().addAll(deletedUserIds);
		userRegistryDto.setVersion(version);
		return userRegistryDto;
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}

	@Override
	public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
	}

	protected void firePropertyChange(Property property, Object oldValue, Object newValue) {
		propertyChangeSupport.firePropertyChange(property.name(), oldValue, newValue);
	}

	@Override
	public Object clone() {
		throw new UnsupportedOperationException();
	}
}
