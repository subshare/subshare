package org.subshare.local;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;
import static org.subshare.local.CryptreeNodeUtil.decrypt;
import static org.subshare.local.CryptreeNodeUtil.encrypt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.bouncycastle.crypto.params.KeyParameter;
import org.subshare.core.ReadUserIdentityAccessDeniedException;
import org.subshare.core.crypto.KeyFactory;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.dto.UserIdentityPayloadDto;
import org.subshare.core.dto.UserRepoKeyPublicKeyDto;
import org.subshare.core.dto.jaxb.UserIdentityPayloadDtoIo;
import org.subshare.core.user.User;
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

import co.codewizards.cloudstore.core.dto.Uid;

public class UserRepoKeyPublicKeyHelper {

	private final CryptreeContext context;

	public UserRepoKeyPublicKeyHelper(final CryptreeContext context) {
		this.context = assertNotNull("context", context);
	}

	public CryptreeContext getContext() {
		return context;
	}

	public UserRepoKeyPublicKey getUserRepoKeyPublicKeyOrCreate(final UserRepoKey.PublicKey publicKey) {
		assertNotNull("publicKey", publicKey);
		final UserRepoKeyPublicKeyDao urkpkDao = context.transaction.getDao(UserRepoKeyPublicKeyDao.class);

		UserRepoKeyPublicKey userRepoKeyPublicKey = urkpkDao.getUserRepoKeyPublicKey(publicKey.getUserRepoKeyId());
		if (userRepoKeyPublicKey == null)
			userRepoKeyPublicKey = createUserRepoKeyPublicKey(publicKey);

		return userRepoKeyPublicKey;
	}

	private UserRepoKeyPublicKey createUserRepoKeyPublicKey(final UserRepoKey.PublicKey publicKey) {
		assertNotNull("publicKey", publicKey);
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
				final UserRepoKey userRepoKey = getContext().userRepoKeyRing.getUserRepoKey(userRepoKeyPublicKey.getUserRepoKeyId());
				if (userRepoKey == null)
					continue;
			}
			createUserIdentities(userRepoKeyPublicKey);
		}
	}

	private void createUserIdentities(final UserRepoKeyPublicKey userRepoKeyPublicKey) {
		assertNotNull("userRepoKeyPublicKey", userRepoKeyPublicKey);

		final PermissionDao pDao = context.transaction.getDao(PermissionDao.class);

		final Set<UserRepoKeyPublicKey> forUserRepoKeyPublicKeys = new HashSet<>();
		for (final Permission permission : pDao.getNonRevokedPermissions(PermissionType.readUserIdentity))
			forUserRepoKeyPublicKeys.add(permission.getUserRepoKeyPublicKey());

		// During the invitation hand-shake of a new user, the new user's repository does not have a repository-owner, yet.
		// Thus, the creation of the corresponding UserIdentityLink must be postponed.
		final RepositoryOwner repositoryOwner = context.getRepositoryOwner();
		if (repositoryOwner != null)
			forUserRepoKeyPublicKeys.add(repositoryOwner.getUserRepoKeyPublicKey());

		for (final UserRepoKeyPublicKey forUserRepoKeyPublicKey : forUserRepoKeyPublicKeys)
			getUserIdentityLinkOrCreate(userRepoKeyPublicKey, forUserRepoKeyPublicKey);
	}

	public UserIdentityLink getUserIdentityLinkOrCreate(final UserRepoKeyPublicKey ofUserRepoKeyPublicKey, UserRepoKeyPublicKey forUserRepoKeyPublicKey) {
		assertNotNull("ofUserRepoKeyPublicKey", ofUserRepoKeyPublicKey);
		assertNotNull("forUserRepoKeyPublicKey", forUserRepoKeyPublicKey);
		final UserIdentityLinkDao uilDao = context.transaction.getDao(UserIdentityLinkDao.class);

		final Collection<UserIdentityLink> userIdentityLinks = uilDao.getUserIdentityLinks(ofUserRepoKeyPublicKey, forUserRepoKeyPublicKey);
		if (!userIdentityLinks.isEmpty())
			return userIdentityLinks.iterator().next();

		final PlainUserIdentity plainUserIdentity = getPlainUserIdentityOrCreate(ofUserRepoKeyPublicKey);
		final byte[] encryptedUserIdentityKeyData = encrypt(plainUserIdentity.getSharedSecret().getKey(), forUserRepoKeyPublicKey.getPublicKey().getPublicKey());

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
		assertNotNull("ofUserRepoKeyPublicKey", ofUserRepoKeyPublicKey);
		final UserIdentityDao uiDao = context.transaction.getDao(UserIdentityDao.class);

		final Collection<UserIdentity> userIdentities = uiDao.getUserIdentitiesOf(ofUserRepoKeyPublicKey);
		if (userIdentities.isEmpty()) {
			UserIdentity userIdentity = new UserIdentity();
			userIdentity.setOfUserRepoKeyPublicKey(ofUserRepoKeyPublicKey);
			final KeyParameter sharedSecret = KeyFactory.getInstance().createSymmetricKey();
			userIdentity.setEncryptedUserIdentityPayloadDtoData(createEncryptedUserIdentityPayloadDtoData(ofUserRepoKeyPublicKey, sharedSecret));

			final UserRepoKey userRepoKey = getContext().userRepoKeyRing.getUserRepoKey(ofUserRepoKeyPublicKey.getUserRepoKeyId());
			final UserRepoKey signingUserRepoKey = userRepoKey != null ? userRepoKey : getUserRepoKeyWithReadUserIdentityPermissionOrFail();

			context.getSignableSigner(signingUserRepoKey).sign(userIdentity);
			return new PlainUserIdentity(uiDao.makePersistent(userIdentity), sharedSecret);
		}

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

	private UserRepoKey getUserRepoKeyWithReadUserIdentityPermissionOrFail() {
		final PermissionDao dao = context.transaction.getDao(PermissionDao.class);
		for (final UserRepoKey userRepoKey : context.userRepoKeyRing.getPermanentUserRepoKeys(context.serverRepositoryId)) {
			final boolean owner = isOwner(userRepoKey.getUserRepoKeyId());
			if (owner)
				return userRepoKey;

			final Collection<Permission> permissions = dao.getValidPermissions(PermissionType.readUserIdentity, userRepoKey.getUserRepoKeyId(), new Date());
			if (!permissions.isEmpty())
				return userRepoKey;
		}
		throw new ReadUserIdentityAccessDeniedException("No UserRepoKey found having 'readUserIdentity' permission!");
	}

	private boolean isOwner(final Uid userRepoKeyId) {
		assertNotNull("userRepoKeyId", userRepoKeyId);
		return userRepoKeyId.equals(context.getRepositoryOwnerOrFail().getUserRepoKeyPublicKey().getUserRepoKeyId());
	}

	private byte[] createEncryptedUserIdentityPayloadDtoData(final UserRepoKeyPublicKey ofUserRepoKeyPublicKey, final KeyParameter sharedSecret) {
		final byte[] userIdentityPayloadDtoData = createUserIdentityPayloadDtoData(ofUserRepoKeyPublicKey);
//		final byte[] encrypted = encrypt(userIdentityPayloadDtoData, forUserRepoKeyPublicKey.getPublicKey().getPublicKey());
		final byte[] encrypted = encrypt(userIdentityPayloadDtoData, sharedSecret);
		return encrypted;
	}

	private byte[] createUserIdentityPayloadDtoData(final UserRepoKeyPublicKey ofUserRepoKeyPublicKey) {
		UserIdentityPayloadDto dto = getUserIdentityPayloadDto(ofUserRepoKeyPublicKey);

		if (dto == null)
			dto = createUserIdentityPayloadDto(ofUserRepoKeyPublicKey);

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		new UserIdentityPayloadDtoIo().serializeWithGz(dto, out);
		return out.toByteArray();
	}

	public UserIdentityPayloadDto getUserIdentityPayloadDto(final UserRepoKeyPublicKey ofUserRepoKeyPublicKey) {
		assertNotNull("ofUserRepoKeyPublicKey", ofUserRepoKeyPublicKey);
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

			final UserIdentityPayloadDtoIo userIdentityPayloadDtoIo = new UserIdentityPayloadDtoIo();
			final UserIdentityPayloadDto userIdentityPayloadDto = userIdentityPayloadDtoIo.deserializeWithGz(new ByteArrayInputStream(userIdentityPayloadDtoData));
			return userIdentityPayloadDto;
		}

		return null;
	}

	protected UserIdentityPayloadDto createUserIdentityPayloadDto(final UserRepoKeyPublicKey ofUserRepoKeyPublicKey) {
		final User user = context.getUserRegistry().getUserOrFail(ofUserRepoKeyPublicKey.getUserRepoKeyId());
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
}
