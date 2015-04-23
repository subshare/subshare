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

import org.subshare.core.GrantAccessDeniedException;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.dto.UserIdentityPayloadDto;
import org.subshare.core.dto.jaxb.UserIdentityPayloadDtoIo;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRepoKey;
import org.subshare.local.persistence.InvitationUserRepoKeyPublicKey;
import org.subshare.local.persistence.Permission;
import org.subshare.local.persistence.PermissionDao;
import org.subshare.local.persistence.RepositoryOwner;
import org.subshare.local.persistence.UserIdentity;
import org.subshare.local.persistence.UserIdentityDao;
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
		boolean hasGrantPermission;
		try {
			getUserRepoKeyWithGrantPermissionOrFail();
			hasGrantPermission = true;
		} catch (GrantAccessDeniedException x) {
			hasGrantPermission = false;
		}

		final UserRepoKeyPublicKeyDao urkpkDao = context.transaction.getDao(UserRepoKeyPublicKeyDao.class);
		for (UserRepoKeyPublicKey userRepoKeyPublicKey : urkpkDao.getObjects()) {
			if (! hasGrantPermission) {
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
		for (final Permission permission : pDao.getNonRevokedPermissions(PermissionType.seeUserIdentity))
			forUserRepoKeyPublicKeys.add(permission.getUserRepoKeyPublicKey());

		// During the invitation hand-shake of a new user, the new user's repository does not have a repository-owner, yet.
		// Thus, the creation of the corresponding UserIdentity must be postponed.
		final RepositoryOwner repositoryOwner = context.getRepositoryOwner();
		if (repositoryOwner != null)
			forUserRepoKeyPublicKeys.add(repositoryOwner.getUserRepoKeyPublicKey());

		for (final UserRepoKeyPublicKey forUserRepoKeyPublicKey : forUserRepoKeyPublicKeys)
			getUserIdentityOrCreate(userRepoKeyPublicKey, forUserRepoKeyPublicKey);
	}

	public UserIdentity getUserIdentityOrCreate(final UserRepoKeyPublicKey ofUserRepoKeyPublicKey, UserRepoKeyPublicKey forUserRepoKeyPublicKey) {
		final UserIdentityDao uiDao = context.transaction.getDao(UserIdentityDao.class);
		final Collection<UserIdentity> userIdentities = uiDao.getUserIdentities(ofUserRepoKeyPublicKey, forUserRepoKeyPublicKey);
		if (!userIdentities.isEmpty())
			return userIdentities.iterator().next();

		final UserRepoKey userRepoKey = getContext().userRepoKeyRing.getUserRepoKey(ofUserRepoKeyPublicKey.getUserRepoKeyId());
		final UserRepoKey signingUserRepoKey = userRepoKey != null ? userRepoKey : getUserRepoKeyWithGrantPermissionOrFail();

		final UserIdentity userIdentity = new UserIdentity();
		userIdentity.setOfUserRepoKeyPublicKey(ofUserRepoKeyPublicKey);
		userIdentity.setForUserRepoKeyPublicKey(forUserRepoKeyPublicKey);
		userIdentity.setEncryptedUserIdentityPayloadDtoData(createEncryptedUserIdentityPayloadDtoData(ofUserRepoKeyPublicKey, forUserRepoKeyPublicKey));
		context.getSignableSigner(signingUserRepoKey).sign(userIdentity);

		return uiDao.makePersistent(userIdentity);
	}

	private UserRepoKey getUserRepoKeyWithGrantPermissionOrFail() {
		final PermissionDao dao = context.transaction.getDao(PermissionDao.class);
		for (final UserRepoKey userRepoKey : context.userRepoKeyRing.getPermanentUserRepoKeys(context.serverRepositoryId)) {
			final boolean owner = isOwner(userRepoKey.getUserRepoKeyId());
			if (owner)
				return userRepoKey;

			final Collection<Permission> permissions = dao.getValidPermissions(PermissionType.grant, userRepoKey.getUserRepoKeyId(), new Date());
			if (!permissions.isEmpty())
				return userRepoKey;
		}
		throw new GrantAccessDeniedException("No UserRepoKey found having grant permissions!");
	}

	private boolean isOwner(final Uid userRepoKeyId) {
		assertNotNull("userRepoKeyId", userRepoKeyId);
		return userRepoKeyId.equals(context.getRepositoryOwnerOrFail().getUserRepoKeyPublicKey().getUserRepoKeyId());
	}

	private byte[] createEncryptedUserIdentityPayloadDtoData(final UserRepoKeyPublicKey ofUserRepoKeyPublicKey, final UserRepoKeyPublicKey forUserRepoKeyPublicKey) {
		final byte[] userIdentityPayloadDtoData = createUserIdentityPayloadDtoData(ofUserRepoKeyPublicKey);
		final byte[] encrypted = encrypt(userIdentityPayloadDtoData, forUserRepoKeyPublicKey.getPublicKey().getPublicKey());
		return encrypted;
	}

	private byte[] createUserIdentityPayloadDtoData(final UserRepoKeyPublicKey ofUserRepoKeyPublicKey) {
		UserIdentityPayloadDto dto = getUserIdentityPayloadDto(ofUserRepoKeyPublicKey);

		if (dto == null)
			dto = createUserIdentityPayloadDto(ofUserRepoKeyPublicKey);

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		new UserIdentityPayloadDtoIo().serialize(dto, out);
		return out.toByteArray();
	}

	public UserIdentityPayloadDto getUserIdentityPayloadDto(final UserRepoKeyPublicKey ofUserRepoKeyPublicKey) {
		assertNotNull("ofUserRepoKeyPublicKey", ofUserRepoKeyPublicKey);
		final UserRepoKeyPublicKeyDao userRepoKeyPublicKeyDao = context.transaction.getDao(UserRepoKeyPublicKeyDao.class);
		final UserIdentityDao userIdentityDao = context.transaction.getDao(UserIdentityDao.class);

		for (final UserRepoKey userRepoKey : context.userRepoKeyRing.getUserRepoKeys(context.serverRepositoryId)) {
			final UserRepoKeyPublicKey forUserRepoKeyPublicKey = userRepoKeyPublicKeyDao.getUserRepoKeyPublicKey(userRepoKey.getUserRepoKeyId());
			if (forUserRepoKeyPublicKey == null)
				continue;

			final Collection<UserIdentity> userIdentities = userIdentityDao.getUserIdentities(ofUserRepoKeyPublicKey, forUserRepoKeyPublicKey);
			if (userIdentities.isEmpty())
				continue;

			final UserIdentity userIdentity = userIdentities.iterator().next();
			final byte[] decrypted = decrypt(userIdentity.getEncryptedUserIdentityPayloadDtoData(), userRepoKey.getKeyPair().getPrivate());
			final UserIdentityPayloadDtoIo userIdentityPayloadDtoIo = new UserIdentityPayloadDtoIo();
			final UserIdentityPayloadDto userIdentityPayloadDto = userIdentityPayloadDtoIo.deserialize(new ByteArrayInputStream(decrypted));
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
		return result;
	}
}
