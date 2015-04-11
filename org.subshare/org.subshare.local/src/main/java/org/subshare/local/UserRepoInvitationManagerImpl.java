package org.subshare.local;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;
import static co.codewizards.cloudstore.core.util.UrlUtil.appendNonEncodedPath;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

import org.subshare.core.Cryptree;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.pgp.PgpDecoder;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistry;
import org.subshare.core.user.UserRepoInvitation;
import org.subshare.core.user.UserRepoInvitationManager;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyRing;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDao;

public class UserRepoInvitationManagerImpl implements UserRepoInvitationManager {

	private UserRegistry userRegistry;
	private Cryptree cryptree;

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public UserRegistry getUserRegistry() {
		return userRegistry;
	}
	@Override
	public void setUserRegistry(UserRegistry userRegistry) {
		this.userRegistry = userRegistry;
	}

	@Override
	public Cryptree getCryptree() {
		return cryptree;
	}
	@Override
	public void setCryptree(Cryptree cryptree) {
		this.cryptree = cryptree;
	}

	@Override
	public UserRepoInvitation createUserRepoInvitation(final String localPath, final User user, final long validityDurationMillis) {
		assertNotNull("localPath", localPath);
		assertNotNull("user", user);
		final PermissionType permissionType = PermissionType.read; // currently the only permission we allow to grant during invitation. maybe we'll change this later.

		final UserRepoKey grantingUserRepoKey = cryptree.getUserRepoKeyOrFail(localPath, PermissionType.grant);
		final User grantingUser = findUserWithUserRepoKeyRingOrFail(grantingUserRepoKey);

		final UserRepoKey invitationUserRepoKey = grantingUser.createInvitationUserRepoKey(user, cryptree.getRemoteRepositoryId(), validityDurationMillis);
		cryptree.grantPermission(localPath, permissionType, invitationUserRepoKey.getPublicKey());

		final RemoteRepositoryDao remoteRepositoryDao = cryptree.getTransaction().getDao(RemoteRepositoryDao.class);
		final RemoteRepository remoteRepository = remoteRepositoryDao.getRemoteRepositoryOrFail(cryptree.getRemoteRepositoryId());
		final URL remoteRoot = remoteRepository.getRemoteRoot();
		if (remoteRoot == null)
			throw new IllegalStateException("Could not determine the remoteRoot for the remoteRepositoryId " + cryptree.getRemoteRepositoryId());

		final String serverPath = cryptree.getServerPath(localPath);
		final URL completeUrl = appendNonEncodedPath(remoteRoot, serverPath);
		final UserRepoInvitation userRepoInvitation = new UserRepoInvitation(completeUrl, invitationUserRepoKey);
		return userRepoInvitation;
	}

	@Override
	public void importUserRepoInvitation(final UserRepoInvitation userRepoInvitation) {
		assertNotNull("userRepoInvitation", userRepoInvitation);
		final PgpKey decryptPgpKey = determineDecryptPgpKey(userRepoInvitation);
		final User user = findUserWithPgpKeyOrFail(decryptPgpKey);
		user.getUserRepoKeyRingOrCreate().addUserRepoKey(userRepoInvitation.getInvitationUserRepoKey()); // TODO convert into real UserRepoKey!
		userRegistry.write(); // TODO writeIfNeeded() and maybe make write() protected?!
	}

	private PgpKey determineDecryptPgpKey(final UserRepoInvitation userRepoInvitation) {
		final byte[] encryptedSignedPrivateKeyData = userRepoInvitation.getInvitationUserRepoKey().getEncryptedSignedPrivateKeyData();
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final PgpDecoder decoder = PgpRegistry.getInstance().getPgpOrFail().createDecoder(new ByteArrayInputStream(encryptedSignedPrivateKeyData), out);
		try {
			decoder.decode();
			return decoder.getDecryptPgpKey();
		} catch (final IOException e) {
			throw new RuntimeException();
		}
	}

	private User findUserWithUserRepoKeyRingOrFail(UserRepoKey userRepoKey) {
		final Uid userRepoKeyId = userRepoKey.getUserRepoKeyId();
		for (final User user : userRegistry.getUsers()) {
			final UserRepoKeyRing userRepoKeyRing = user.getUserRepoKeyRing();
			if (userRepoKeyRing != null && userRepoKeyRing.getUserRepoKey(userRepoKeyId) != null)
				return user;
		}
		throw new IllegalArgumentException("No User found owning the UserRepoKey with id=" + userRepoKeyId);
	}

	private User findUserWithPgpKeyOrFail(PgpKey pgpKey) {
		final Long pgpKeyId = pgpKey.getPgpKeyId();
		for (final User user : userRegistry.getUsers()) {
			if (user.getPgpKeyIds().contains(pgpKeyId))
				return user;
		}
		throw new IllegalArgumentException("No User associated with the PgpKey with id=" + pgpKeyId);
	}
}
