package org.subshare.local;

import static co.codewizards.cloudstore.core.util.DateUtil.*;
import static java.util.Objects.*;
import static org.subshare.local.CryptreeNodeUtil.*;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.bouncycastle.crypto.params.KeyParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.ReadUserIdentityAccessDeniedException;
import org.subshare.core.crypto.KeyFactory;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.dto.UserIdentityPayloadDto;
import org.subshare.core.dto.UserRepoKeyPublicKeyDto;
import org.subshare.core.dto.jaxb.UserIdentityPayloadDtoIo;
import org.subshare.core.pgp.MissingSigningPgpKeyException;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistry;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyPublicKeyDtoWithSignatureConverter;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.local.persistence.InvitationUserRepoKeyPublicKey;
import org.subshare.local.persistence.Permission;
import org.subshare.local.persistence.PermissionDao;
import org.subshare.local.persistence.RepositoryOwner;
import org.subshare.local.persistence.UserIdentity;
import org.subshare.local.persistence.UserIdentityDao;
import org.subshare.local.persistence.UserIdentityLink;
import org.subshare.local.persistence.UserIdentityLinkDao;
import org.subshare.local.persistence.UserRepoKeyPublicKey;
import org.subshare.local.persistence.UserRepoKeyPublicKeyDao;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public class UserRepoKeyPublicKeyHelper {
	private static final Logger logger = LoggerFactory.getLogger(UserRepoKeyPublicKeyHelper.class);

	private final CryptreeContext context;

	public UserRepoKeyPublicKeyHelper(final CryptreeContext context) {
		this.context = requireNonNull(context, "context");
	}

	public CryptreeContext getContext() {
		return context;
	}

	public UserRepoKeyPublicKey getUserRepoKeyPublicKeyOrCreate(final UserRepoKey.PublicKey publicKey) {
		requireNonNull(publicKey, "publicKey");
		final UserRepoKeyPublicKeyDao urkpkDao = context.transaction.getDao(UserRepoKeyPublicKeyDao.class);

		UserRepoKeyPublicKey userRepoKeyPublicKey = urkpkDao.getUserRepoKeyPublicKey(publicKey.getUserRepoKeyId());
		if (userRepoKeyPublicKey == null)
			userRepoKeyPublicKey = createUserRepoKeyPublicKey(publicKey);

		return userRepoKeyPublicKey;
	}

	private UserRepoKeyPublicKey createUserRepoKeyPublicKey(final UserRepoKey.PublicKey publicKey) {
		requireNonNull(publicKey, "publicKey");
		final UserRepoKeyPublicKeyDao urkpkDao = context.transaction.getDao(UserRepoKeyPublicKeyDao.class);

		final UserRepoKeyPublicKey userRepoKeyPublicKey;
		if (publicKey.isInvitation()) {
			final UserRepoKey.PublicKeyWithSignature publicKeyWithSignature = (UserRepoKey.PublicKeyWithSignature) publicKey;
			userRepoKeyPublicKey = urkpkDao.makePersistent(new InvitationUserRepoKeyPublicKey(publicKeyWithSignature));
		}
		else
			userRepoKeyPublicKey = urkpkDao.makePersistent(new UserRepoKeyPublicKey(publicKey));

		createUserIdentities(userRepoKeyPublicKey);
		return userRepoKeyPublicKey;
	}

	public void createMissingUserIdentities() {
		boolean hasPermission;
		try {
			getUserRepoKeyWithReadUserIdentityPermissionOrFail();
			hasPermission = true;
		} catch (ReadUserIdentityAccessDeniedException x) {
			hasPermission = false;
		}

		final UserRepoKeyPublicKeyDao urkpkDao = context.transaction.getDao(UserRepoKeyPublicKeyDao.class);
		for (UserRepoKeyPublicKey userRepoKeyPublicKey : urkpkDao.getObjects()) {
			if (! hasPermission) {
				// Even though the permission to read user-identities is missing, I create my own user-identities!
				final UserRepoKey userRepoKey = getContext().userRepoKeyRing.getUserRepoKey(userRepoKeyPublicKey.getUserRepoKeyId());
				if (userRepoKey == null)
					continue;
			}
			createUserIdentities(userRepoKeyPublicKey);
		}
	}

	protected void createUserIdentities(final UserRepoKeyPublicKey userRepoKeyPublicKey) { // must be protected, because it's the fastest fix for: https://github.com/jmockit/jmockit1/issues/605
		requireNonNull(userRepoKeyPublicKey, "userRepoKeyPublicKey");

		final Set<UserRepoKeyPublicKey> forUserRepoKeyPublicKeys = getForUserRepoKeyPublicKeysForUserIdentityLinkCreation(
				userRepoKeyPublicKey);

		for (final UserRepoKeyPublicKey forUserRepoKeyPublicKey : forUserRepoKeyPublicKeys)
			getUserIdentityLinkOrCreate(userRepoKeyPublicKey, forUserRepoKeyPublicKey);
	}

	private Set<UserRepoKeyPublicKey> getForUserRepoKeyPublicKeysForUserIdentityLinkCreation(UserRepoKeyPublicKey ofUserRepoKeyPublicKey) {
		final PermissionDao pDao = context.transaction.getDao(PermissionDao.class);

		final Set<UserRepoKeyPublicKey> forUserRepoKeyPublicKeys = new HashSet<>();
		for (final Permission permission : pDao.getNonRevokedPermissions(PermissionType.readUserIdentity))
			forUserRepoKeyPublicKeys.add(permission.getUserRepoKeyPublicKey());

		// During the invitation hand-shake of a new user, the new user's repository does not have a repository-owner, yet.
		// Thus, the creation of the corresponding UserIdentityLink must be postponed.
		final RepositoryOwner repositoryOwner = context.getRepositoryOwner();
		if (repositoryOwner != null)
			forUserRepoKeyPublicKeys.add(repositoryOwner.getUserRepoKeyPublicKey());

		// Every UserRepoKeyPublicKey should be able to read its *own* UserIdentity.
		forUserRepoKeyPublicKeys.add(ofUserRepoKeyPublicKey);

		// The order is essential! We must start with a for-key that we can read!
		// Thus transferring things into a LinkedHashSet in the right order!
		final Set<UserRepoKeyPublicKey> result = new LinkedHashSet<>(forUserRepoKeyPublicKeys.size());

		for (UserRepoKeyPublicKey forUserRepoKeyPublicKey : forUserRepoKeyPublicKeys) {
			if (context.userRepoKeyRing.getUserRepoKey(forUserRepoKeyPublicKey.getUserRepoKeyId()) != null)
				result.add(forUserRepoKeyPublicKey);
		}
		result.addAll(forUserRepoKeyPublicKeys);

		return result;
	}

	public UserIdentityLink getUserIdentityLinkOrCreate(final UserRepoKeyPublicKey ofUserRepoKeyPublicKey, final UserRepoKeyPublicKey forUserRepoKeyPublicKey) {
		requireNonNull(ofUserRepoKeyPublicKey, "ofUserRepoKeyPublicKey");
		requireNonNull(forUserRepoKeyPublicKey, "forUserRepoKeyPublicKey");
		final UserIdentityLinkDao uilDao = context.transaction.getDao(UserIdentityLinkDao.class);

		Collection<UserIdentityLink> userIdentityLinks = uilDao.getUserIdentityLinks(ofUserRepoKeyPublicKey, forUserRepoKeyPublicKey);
		if (!userIdentityLinks.isEmpty())
			return userIdentityLinks.iterator().next();

		final PlainUserIdentity plainUserIdentity = getPlainUserIdentityOrCreate(ofUserRepoKeyPublicKey);

		if (ofUserRepoKeyPublicKey.equals(forUserRepoKeyPublicKey)) {
			// We must query again, because it was probably just created, if the for- and the of-key are the same.
			userIdentityLinks = uilDao.getUserIdentityLinks(ofUserRepoKeyPublicKey, forUserRepoKeyPublicKey);
			if (!userIdentityLinks.isEmpty())
				return userIdentityLinks.iterator().next();
		}
		return createUserIdentityLink(plainUserIdentity, forUserRepoKeyPublicKey);
	}

	private UserIdentityLink createUserIdentityLink(final PlainUserIdentity plainUserIdentity, final UserRepoKeyPublicKey forUserRepoKeyPublicKey) {
		requireNonNull(plainUserIdentity, "plainUserIdentity");
		requireNonNull(forUserRepoKeyPublicKey, "forUserRepoKeyPublicKey");
		final UserIdentityLinkDao uilDao = context.transaction.getDao(UserIdentityLinkDao.class);

		final byte[] encryptedUserIdentityKeyData = encrypt(plainUserIdentity.getSharedSecret().getKey(), forUserRepoKeyPublicKey.getPublicKey().getPublicKey());

		final UserRepoKeyPublicKey ofUserRepoKeyPublicKey = plainUserIdentity.getUserIdentity().getOfUserRepoKeyPublicKey();
		final UserRepoKey userRepoKey = getContext().userRepoKeyRing.getUserRepoKey(ofUserRepoKeyPublicKey.getUserRepoKeyId());
		final UserRepoKey signingUserRepoKey = userRepoKey != null ? userRepoKey : getUserRepoKeyWithReadUserIdentityPermissionOrFail();

		final UserIdentityLink userIdentityLink = new UserIdentityLink();
		userIdentityLink.setUserIdentity(plainUserIdentity.getUserIdentity());
		userIdentityLink.setForUserRepoKeyPublicKey(forUserRepoKeyPublicKey);
		userIdentityLink.setEncryptedUserIdentityKeyData(encryptedUserIdentityKeyData);
		context.getSignableSigner(signingUserRepoKey).sign(userIdentityLink);

		return uilDao.makePersistent(userIdentityLink);
	}

	private PlainUserIdentity getPlainUserIdentityOrCreate(final UserRepoKeyPublicKey ofUserRepoKeyPublicKey) {
		requireNonNull(ofUserRepoKeyPublicKey, "ofUserRepoKeyPublicKey");
		final UserIdentityDao uiDao = context.transaction.getDao(UserIdentityDao.class);

		final Collection<UserIdentity> userIdentities = uiDao.getUserIdentitiesOf(ofUserRepoKeyPublicKey);
		if (userIdentities.isEmpty())
			return createPlainUserIdentity(ofUserRepoKeyPublicKey);

		final UserIdentityLinkDao uilDao = context.transaction.getDao(UserIdentityLinkDao.class);
		for (final UserIdentity userIdentity : userIdentities) { // should normally only be one single entry (if it's not empty)
			final Collection<UserIdentityLink> userIdentityLinks = uilDao.getUserIdentityLinksOf(userIdentity);
			for (final UserIdentityLink userIdentityLink : userIdentityLinks) {
				final UserRepoKey userRepoKey = getContext().userRepoKeyRing.getUserRepoKey(userIdentityLink.getForUserRepoKeyPublicKey().getUserRepoKeyId());
				if (userRepoKey != null) {
					byte[] userIdentityKeyData = decrypt(userIdentityLink.getEncryptedUserIdentityKeyData(), userRepoKey.getKeyPair().getPrivate());
					final KeyParameter sharedSecret = new KeyParameter(userIdentityKeyData);
					return new PlainUserIdentity(userIdentity, sharedSecret);
				}
			}
		}

		// This should IMHO *never* happen.
		throw new ReadUserIdentityAccessDeniedException("No UserRepoKey found being able to decrypt the user-identities found: " + userIdentities);
	}

	private PlainUserIdentity createPlainUserIdentity(final UserRepoKeyPublicKey ofUserRepoKeyPublicKey) {
		requireNonNull(ofUserRepoKeyPublicKey, "ofUserRepoKeyPublicKey");
		final byte[] userIdentityPayloadDtoData = createUserIdentityPayloadDtoData(ofUserRepoKeyPublicKey);
		return createPlainUserIdentity(ofUserRepoKeyPublicKey, userIdentityPayloadDtoData);
	}

	private PlainUserIdentity createPlainUserIdentity(final UserRepoKeyPublicKey ofUserRepoKeyPublicKey, byte[] userIdentityPayloadDtoData) {
		final UserIdentityDao uiDao = context.transaction.getDao(UserIdentityDao.class);

		UserIdentity userIdentity = new UserIdentity();
		userIdentity.setOfUserRepoKeyPublicKey(ofUserRepoKeyPublicKey);
		final KeyParameter sharedSecret = KeyFactory.getInstance().createSymmetricKey();
		userIdentity.setEncryptedUserIdentityPayloadDtoData(encrypt(userIdentityPayloadDtoData, sharedSecret));

		final UserRepoKey userRepoKey = getContext().userRepoKeyRing.getUserRepoKey(ofUserRepoKeyPublicKey.getUserRepoKeyId());
		final UserRepoKey signingUserRepoKey = userRepoKey != null ? userRepoKey : getUserRepoKeyWithReadUserIdentityPermissionOrFail();

		context.getSignableSigner(signingUserRepoKey).sign(userIdentity);

		// We *must* immediately create a UserIdentityLink for ourselves! Otherwise, we're not able to read it, anymore.
		PlainUserIdentity plainUserIdentity = new PlainUserIdentity(uiDao.makePersistent(userIdentity), sharedSecret);
		createUserIdentityLink(plainUserIdentity, ofUserRepoKeyPublicKey);
		return plainUserIdentity;
	}

	private UserRepoKey getUserRepoKeyWithReadUserIdentityPermissionOrFail() {
		final PermissionDao dao = context.transaction.getDao(PermissionDao.class);
		for (final UserRepoKey userRepoKey : context.userRepoKeyRing.getPermanentUserRepoKeys(context.serverRepositoryId)) {
			final boolean owner = isOwner(userRepoKey.getUserRepoKeyId());
			if (owner)
				return userRepoKey;

			final Collection<Permission> permissions = dao.getValidPermissions(PermissionType.readUserIdentity, userRepoKey.getUserRepoKeyId(), now());
			if (!permissions.isEmpty())
				return userRepoKey;
		}
		throw new ReadUserIdentityAccessDeniedException("No UserRepoKey found having 'readUserIdentity' permission!");
	}

	private boolean isOwner(final Uid userRepoKeyId) {
		requireNonNull(userRepoKeyId, "userRepoKeyId");
		return userRepoKeyId.equals(context.getRepositoryOwnerOrFail().getUserRepoKeyPublicKey().getUserRepoKeyId());
	}

	private byte[] createUserIdentityPayloadDtoData(final UserRepoKeyPublicKey ofUserRepoKeyPublicKey) {
		UserIdentityPayloadDto dto = getUserIdentityPayloadDto(ofUserRepoKeyPublicKey);

		if (dto == null)
			dto = createUserIdentityPayloadDto(ofUserRepoKeyPublicKey);

		return new UserIdentityPayloadDtoIo().serializeWithGz(dto);
	}

	public UserIdentityPayloadDto getUserIdentityPayloadDto(final UserRepoKeyPublicKey ofUserRepoKeyPublicKey) {
		final byte[] userIdentityPayloadDtoData = getUserIdentityPayloadDtoData(ofUserRepoKeyPublicKey);

		if (userIdentityPayloadDtoData == null)
			return null;

		final UserIdentityPayloadDtoIo userIdentityPayloadDtoIo = new UserIdentityPayloadDtoIo();
		final UserIdentityPayloadDto userIdentityPayloadDto = userIdentityPayloadDtoIo.deserializeWithGz(userIdentityPayloadDtoData);
		return userIdentityPayloadDto;
	}

	private byte[] getUserIdentityPayloadDtoData(final UserRepoKeyPublicKey ofUserRepoKeyPublicKey) {
		requireNonNull(ofUserRepoKeyPublicKey, "ofUserRepoKeyPublicKey");
		final UserRepoKeyPublicKeyDao userRepoKeyPublicKeyDao = context.transaction.getDao(UserRepoKeyPublicKeyDao.class);
		final UserIdentityLinkDao userIdentityLinkDao = context.transaction.getDao(UserIdentityLinkDao.class);

		for (final UserRepoKey userRepoKey : context.userRepoKeyRing.getUserRepoKeys(context.serverRepositoryId)) {
			final UserRepoKeyPublicKey forUserRepoKeyPublicKey = userRepoKeyPublicKeyDao.getUserRepoKeyPublicKey(userRepoKey.getUserRepoKeyId());
			if (forUserRepoKeyPublicKey == null)
				continue;

			final Collection<UserIdentityLink> userIdentityLinks = userIdentityLinkDao.getUserIdentityLinks(ofUserRepoKeyPublicKey, forUserRepoKeyPublicKey);
			if (userIdentityLinks.isEmpty())
				continue;

			final UserIdentityLink userIdentityLink = userIdentityLinks.iterator().next();

			final byte[] userIdentityKeyData = decrypt(userIdentityLink.getEncryptedUserIdentityKeyData(), userRepoKey.getKeyPair().getPrivate());
			final KeyParameter userIdentityKey = new KeyParameter(userIdentityKeyData);

			final byte[] userIdentityPayloadDtoData = decrypt(userIdentityLink.getUserIdentity().getEncryptedUserIdentityPayloadDtoData(), userIdentityKey);
			return userIdentityPayloadDtoData;
		}

		return null;
	}

	private UserIdentityPayloadDto createUserIdentityPayloadDto(final UserRepoKeyPublicKey ofUserRepoKeyPublicKey) {
		final User user = context.getUserRegistry().getUserByUserRepoKeyIdOrFail(ofUserRepoKeyPublicKey.getUserRepoKeyId());
		final UserIdentityPayloadDto result = new UserIdentityPayloadDto();
		result.setFirstName(user.getFirstName());
		result.setLastName(user.getLastName());
		result.getEmails().addAll(user.getEmails());
		result.getPgpKeyIds().addAll(user.getPgpKeyIds());

		final UserRepoKeyRing userRepoKeyRing = user.getUserRepoKeyRing();
		UserRepoKey.PublicKeyWithSignature publicKey = null;
		if (userRepoKeyRing != null) {
			final UserRepoKey userRepoKey = userRepoKeyRing.getUserRepoKeyOrFail(ofUserRepoKeyPublicKey.getUserRepoKeyId());
			publicKey = userRepoKey.getPublicKey();
		}
		else {
			for (UserRepoKey.PublicKeyWithSignature pk : user.getUserRepoKeyPublicKeys()) {
				if (ofUserRepoKeyPublicKey.getUserRepoKeyId().equals(pk.getUserRepoKeyId())) {
					publicKey = pk;
					break;
				}
			}
		}

		if (publicKey == null)
			throw new IllegalStateException("publicKey == null");

		final UserRepoKeyPublicKeyDtoWithSignatureConverter converter = new UserRepoKeyPublicKeyDtoWithSignatureConverter();
		final UserRepoKeyPublicKeyDto userRepoKeyPublicKeyDto = converter.toUserRepoKeyPublicKeyDto(publicKey);
		result.setUserRepoKeyPublicKeyDto(userRepoKeyPublicKeyDto);

		return result;
	}

	public void removeUserIdentityLinksAfterRevokingReadUserIdentityPermission() {
		final UserRepoKeyPublicKeyDao urkpkDao = getContext().transaction.getDao(UserRepoKeyPublicKeyDao.class);
		final UserIdentityDao uiDao = getContext().transaction.getDao(UserIdentityDao.class);

		final Map<UserRepoKeyPublicKey, byte[]> ofUserRepoKeyPublicKey2UserIdentityPayloadDtoData = new HashMap<>();
		for (final UserRepoKeyPublicKey userRepoKeyPublicKey : urkpkDao.getObjects()) {
			final byte[] userIdentityPayloadDtoData = getUserIdentityPayloadDtoData(userRepoKeyPublicKey);
			if (userIdentityPayloadDtoData == null)
				throw new ReadUserIdentityAccessDeniedException("Could not obtain the decrypted userIdentityPayloadDtoData of " + userRepoKeyPublicKey);

			ofUserRepoKeyPublicKey2UserIdentityPayloadDtoData.put(userRepoKeyPublicKey, userIdentityPayloadDtoData);
			deleteUserIdentityLinksOf(userRepoKeyPublicKey);
		}

		getContext().transaction.flush();

		final long uiCount = uiDao.getObjectsCount();
		if (uiCount != 0)
			throw new IllegalStateException(String.format("WTF?! There are still %s UserIdentity instances in the DB!", uiCount));

		for (final Map.Entry<UserRepoKeyPublicKey, byte[]> me : ofUserRepoKeyPublicKey2UserIdentityPayloadDtoData.entrySet()) {
			final UserRepoKeyPublicKey ofUserRepoKeyPublicKey = me.getKey();
			final byte[] userIdentityPayloadDtoData = me.getValue();

			final Set<UserRepoKeyPublicKey> forUserRepoKeyPublicKeys = getForUserRepoKeyPublicKeysForUserIdentityLinkCreation(
					ofUserRepoKeyPublicKey);

			final PlainUserIdentity plainUserIdentity = createPlainUserIdentity(ofUserRepoKeyPublicKey, userIdentityPayloadDtoData);

			for (final UserRepoKeyPublicKey forUserRepoKeyPublicKey : forUserRepoKeyPublicKeys)
				createUserIdentityLink(plainUserIdentity, forUserRepoKeyPublicKey);
		}
	}

	private void deleteUserIdentityLinksOf(final UserRepoKeyPublicKey ofUserRepoKeyPublicKey) {
		requireNonNull(ofUserRepoKeyPublicKey, "ofUserRepoKeyPublicKey");

		final UserIdentityLinkDao uilDao = getContext().transaction.getDao(UserIdentityLinkDao.class);
		final Collection<UserIdentityLink> userIdentityLinks = uilDao.getUserIdentityLinksOf(ofUserRepoKeyPublicKey);

		// TODO create deletion markers for sync!
		uilDao.deletePersistentAll(userIdentityLinks);
	}

	public void updateUserRepoKeyRingFromUserIdentities() {
		final LocalRepoTransaction transaction = getContext().transaction;
		final UserRepoKeyPublicKeyDao urkpkDao = transaction.getDao(UserRepoKeyPublicKeyDao.class);
		final UserRegistry userRegistry = getContext().getUserRegistry();
		for (final UserRepoKeyPublicKey userRepoKeyPublicKey : urkpkDao.getObjects()) {
			final Uid userRepoKeyId = userRepoKeyPublicKey.getUserRepoKeyId();
			User user = userRegistry.getUserByUserRepoKeyId(userRepoKeyId);
			if (user != null) {
				logger.debug("updateUserRepoKeyRingFromUserIdentities: Skipping {} already associated with {}",
						userRepoKeyPublicKey, user);
				continue;
			}

			final UserIdentityPayloadDto userIdentityPayloadDto = getUserIdentityPayloadDto(userRepoKeyPublicKey);
			if (userIdentityPayloadDto == null) {
				logger.debug("updateUserRepoKeyRingFromUserIdentities: Skipping {} because of missing permissions!",
						userRepoKeyPublicKey);
				continue; // missing permissions!
			}

			Collection<User> users = userRegistry.getUsersByPgpKeyIds(new HashSet<>(userIdentityPayloadDto.getPgpKeyIds()));
			if (users.isEmpty()) {
				user = userRegistry.createUser();
				user.getPgpKeyIds().addAll(userIdentityPayloadDto.getPgpKeyIds());
				user.setFirstName(userIdentityPayloadDto.getFirstName());
				user.setLastName(userIdentityPayloadDto.getLastName());
				user.getEmails().addAll(userIdentityPayloadDto.getEmails());
				userRegistry.addUser(user);
				logger.debug("updateUserRepoKeyRingFromUserIdentities: Created {} for {}",
						user, userRepoKeyPublicKey);
			}
			else {
				user = users.iterator().next();

				for (final PgpKeyId pgpKeyId : userIdentityPayloadDto.getPgpKeyIds()) {
					if (! user.getPgpKeyIds().contains(pgpKeyId))
						user.getPgpKeyIds().add(pgpKeyId);
				}

				logger.debug("updateUserRepoKeyRingFromUserIdentities: Found {} via PGP-key[s] for {}",
						user, userRepoKeyPublicKey);
			}

			final UserRepoKeyPublicKeyDtoWithSignatureConverter converter = new UserRepoKeyPublicKeyDtoWithSignatureConverter();
			try {
				final UserRepoKey.PublicKeyWithSignature pk = converter.fromUserRepoKeyPublicKeyDto(userIdentityPayloadDto.getUserRepoKeyPublicKeyDto());
				user.getUserRepoKeyPublicKeys().add(pk);
			} catch (MissingSigningPgpKeyException x) {
				logger.warn("updateUserRepoKeyRingFromUserIdentities: " + x);

				// The missing keys should already be added above, but we better check again.
				for (final PgpKeyId pgpKeyId : x.getMissingPgpKeyIds()) {
					if (! user.getPgpKeyIds().contains(pgpKeyId))
						user.getPgpKeyIds().add(pgpKeyId);
				}
			}
		}
	}
}
