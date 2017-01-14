package org.subshare.local;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.subshare.core.Cryptree;
import org.subshare.core.dto.jaxb.CollisionPrivateDtoIo;
import org.subshare.core.sign.SignableSigner;
import org.subshare.core.sign.SignableVerifier;
import org.subshare.core.user.UserRegistry;
import org.subshare.core.user.UserRegistryImpl;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;
import org.subshare.local.persistence.RepositoryOwner;
import org.subshare.local.persistence.RepositoryOwnerDao;
import org.subshare.local.persistence.UserRepoKeyPublicKeyLookupImpl;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.dto.jaxb.RepoFileDtoIo;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;

public class CryptreeContext {

	public final UserRepoKeyRing userRepoKeyRing; // never null on client; always null on server
//	public final UserRepoKey userRepoKey; // never null
	public final LocalRepoTransaction transaction; // never null
	public final UUID localRepositoryId; // never null
	public final UUID remoteRepositoryId; // never null
	public final UUID serverRepositoryId; // never null
	public final String remotePathPrefix; // never null on client; always null on server
	public final boolean isOnServer;
	public final RepoFileDtoIo repoFileDtoIo; // never null
	public final CollisionPrivateDtoIo collisionPrivateDtoIo; // never null
	public final SignableVerifier signableVerifier; // never null

	private RepositoryOwner repositoryOwner; // lazily initialised

	private final Map<UserRepoKey, SignableSigner> userRepoKey2SignableSigner = new HashMap<>();

	private final Map<String, CryptreeNode> localPath2CryptreeNode = new HashMap<>();
	private final Map<Uid, CryptreeNode> cryptoRepoFileId2CryptreeNode = new HashMap<>();

	private Uid cryptoRepoFileIdForRemotePathPrefix;

	public CryptreeContext(
			final UserRepoKeyRing userRepoKeyRing, final LocalRepoTransaction transaction,
			final UUID localRepositoryId, final UUID remoteRepositoryId, final UUID serverRepositoryId,
			final String remotePathPrefix, final boolean isOnServer) {
		if (!isOnServer) {
			assertNotNull(userRepoKeyRing, "userRepoKeyRing");
			assertNotNull(remotePathPrefix, "remotePathPrefix");
		}

		this.userRepoKeyRing = userRepoKeyRing;

		this.transaction = assertNotNull(transaction, "transaction");
		this.localRepositoryId = assertNotNull(localRepositoryId, "localRepositoryId");
		this.remoteRepositoryId = assertNotNull(remoteRepositoryId, "remoteRepositoryId");
		this.serverRepositoryId = assertNotNull(serverRepositoryId, "serverRepositoryId");
		this.remotePathPrefix = remotePathPrefix;
		this.isOnServer = isOnServer;
		this.repoFileDtoIo = new RepoFileDtoIo();
		this.collisionPrivateDtoIo = new CollisionPrivateDtoIo();
		this.signableVerifier = new SignableVerifier(new UserRepoKeyPublicKeyLookupImpl(transaction));

		if (userRepoKeyRing != null && userRepoKeyRing.getPermanentUserRepoKeys(serverRepositoryId).isEmpty())
			throw new IllegalArgumentException(String.format(
					"userRepoKeyRing.getUserRepoKeys(serverRepositoryId).isEmpty() :: serverRepositoryId=%s",
					serverRepositoryId));
	}

	public UserRegistry getUserRegistry() {
		return UserRegistryImpl.getInstance();
	}

	public CryptreeNode getCryptreeNodeOrCreate(String localPath) {
		assertNotNull(localPath, "localPath");
		if ("/".equals(localPath))
			localPath = "";

		CryptreeNode cryptreeNode = localPath2CryptreeNode.get(localPath);
		if (cryptreeNode == null) {
			cryptreeNode = createCryptreeNodeOrFail(localPath);
			localPath2CryptreeNode.put(localPath, cryptreeNode);
		}
		return cryptreeNode;
	}

	public CryptreeNode getCryptreeNodeOrCreate(final Uid cryptoRepoFileId) {
		assertNotNull(cryptoRepoFileId, "cryptoRepoFileId");
		CryptreeNode cryptreeNode = cryptoRepoFileId2CryptreeNode.get(cryptoRepoFileId);
		if (cryptreeNode == null) {
			cryptreeNode = createCryptreeNodeOrFail(cryptoRepoFileId);
			cryptoRepoFileId2CryptreeNode.put(cryptoRepoFileId, cryptreeNode);
		}
		return cryptreeNode;
	}

	public void registerCryptreeNode(final RepoFile repoFile, final CryptreeNode cryptreeNode) {
		assertNotNull(repoFile, "repoFile");
		assertNotNull(cryptreeNode, "cryptreeNode");
		localPath2CryptreeNode.put(repoFile.getPath(), cryptreeNode);
	}

	public void registerCryptreeNode(final CryptoRepoFile cryptoRepoFile, final CryptreeNode cryptreeNode) {
		assertNotNull(cryptoRepoFile, "cryptoRepoFile");
		assertNotNull(cryptreeNode, "cryptreeNode");
		cryptoRepoFileId2CryptreeNode.put(cryptoRepoFile.getCryptoRepoFileId(), cryptreeNode);
	}

	public CryptreeNode getCryptreeNodeOrCreate(final CryptreeNode parent, final CryptreeNode child, final RepoFile repoFile, final CryptoRepoFile cryptoRepoFile) {
		CryptreeNode cryptreeNode = null;

		String localPath = null;
		if (repoFile != null) {
			localPath = repoFile.getPath();
			cryptreeNode = localPath2CryptreeNode.get(localPath);
		}

		if (cryptreeNode != null)
			return cryptreeNode;

		Uid cryptoRepoFileId = null;
		if (cryptoRepoFile != null) {
			cryptoRepoFileId = cryptoRepoFile.getCryptoRepoFileId();
			cryptreeNode = cryptoRepoFileId2CryptreeNode.get(cryptoRepoFileId);
		}

		if (cryptreeNode != null)
			return cryptreeNode;

		cryptreeNode = new CryptreeNode(parent, child, this, repoFile, cryptoRepoFile);

		if (localPath != null)
			localPath2CryptreeNode.put(localPath, cryptreeNode);

		if (cryptoRepoFileId != null)
			cryptoRepoFileId2CryptreeNode.put(cryptoRepoFileId, cryptreeNode);

		return cryptreeNode;
	}

	public RepositoryOwner getRepositoryOwnerOrFail() {
		if (repositoryOwner == null) {
			final RepositoryOwnerDao roDao = transaction.getDao(RepositoryOwnerDao.class);
			repositoryOwner = roDao.getRepositoryOwnerOrFail(serverRepositoryId);
		}
		return repositoryOwner;
	}

	public RepositoryOwner getRepositoryOwner() {
		if (repositoryOwner == null) {
			final RepositoryOwnerDao roDao = transaction.getDao(RepositoryOwnerDao.class);
			repositoryOwner = roDao.getRepositoryOwner(serverRepositoryId);
		}
		return repositoryOwner;
	}

	public SignableSigner getSignableSigner(final UserRepoKey userRepoKey) {
		assertNotNull(userRepoKey, "userRepoKey");
		SignableSigner signableSigner = userRepoKey2SignableSigner.get(userRepoKey);
		if (signableSigner == null) {
			signableSigner = new SignableSigner(userRepoKey);
			userRepoKey2SignableSigner.put(userRepoKey, signableSigner);
		}
		return signableSigner;
	}

	private CryptreeNode createCryptreeNodeOrFail(final String localPath) {
		final RepoFile repoFile = getRepoFile(localPath);
		if (repoFile != null) {
			final CryptreeNode cryptreeNode = new CryptreeNode(this, repoFile);
			return cryptreeNode;
		}
		final CryptoRepoFile cryptoRepoFile = getCryptoRepoFileOrFail(localPath);
		final CryptreeNode cryptreeNode = new CryptreeNode(this, cryptoRepoFile);
		return cryptreeNode;
	}

	private CryptreeNode createCryptreeNodeOrFail(final Uid cryptoRepoFileId) {
		final CryptoRepoFile cryptoRepoFile = getCryptoRepoFileOrFail(cryptoRepoFileId);
		final CryptreeNode cryptreeNode = new CryptreeNode(this, cryptoRepoFile);
		return cryptreeNode;
	}

	public CryptoRepoFile getCryptoRepoFileOrFail(final Uid cryptoRepoFileId) {
		final CryptoRepoFileDao cryptoRepoFileDao = transaction.getDao(CryptoRepoFileDao.class);
		final CryptoRepoFile cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFileOrFail(cryptoRepoFileId);
		return cryptoRepoFile;
	}

	public CryptoRepoFile getCryptoRepoFileOrFail(final String localPath) {
		final CryptoRepoFile cryptoRepoFile = getCryptoRepoFile(localPath);
		assertNotNull(cryptoRepoFile, "cryptoRepoFile");
		return cryptoRepoFile;
	}

	public CryptoRepoFile getCryptoRepoFile(final String localPath) {
		final CryptoRepoFileDao cryptoRepoFileDao = transaction.getDao(CryptoRepoFileDao.class);
		final CryptoRepoFile cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFile(prefixLocalPath(localPath));
		return cryptoRepoFile;
	}

	private String prefixLocalPath(final String localPath) {
		assertNotNull(localPath, "localPath");

		if (remotePathPrefix.isEmpty())
			return localPath;

		final CryptoRepoFile prefixCryptoRepoFile = getCryptoRepoFileForRemotePathPrefixOrFail();

		if (localPath.isEmpty())
			return prefixCryptoRepoFile.getLocalPathOrFail();
		else {
			if ("/".equals(localPath))
				throw new IllegalStateException("localPath should never be '/', but instead it should be an empty String, if the real root is checked out!");

			if (!localPath.startsWith("/"))
				throw new IllegalStateException(String.format("localPath '%s' is neither empty nor does it start with '/'!", localPath));

			final String prefix = prefixCryptoRepoFile.getLocalPathOrFail();

			if (!prefix.isEmpty() && !prefix.startsWith("/"))
				throw new IllegalStateException(String.format("prefixCryptoRepoFile.localPath '%s' is neither empty nor does it start with '/'!", prefix));

			if (prefix.endsWith("/"))
				throw new IllegalStateException(String.format("prefixCryptoRepoFile.localPath '%s' ends with '/'! It should not!", prefix));

			return prefix + localPath;
		}
	}

	private CryptoRepoFile getCryptoRepoFileForRemotePathPrefixOrFail() {
		final Uid id = getCryptoRepoFileIdForRemotePathPrefixOrFail();
		final CryptoRepoFile prefixCryptoRepoFile = getCryptoRepoFileOrFail(id);
		return prefixCryptoRepoFile;
	}

	/**
	 * @see Cryptree#getCryptoRepoFileIdForRemotePathPrefixOrFail()
	 */
	public Uid getCryptoRepoFileIdForRemotePathPrefixOrFail() {
		if (cryptoRepoFileIdForRemotePathPrefix == null) {
			if (isOnServer)
				throw new IllegalStateException("This method cannot be used on the server!");

			if (remotePathPrefix.isEmpty())
				throw new IllegalStateException("This method cannot be used, if there is no remotePathPrefix!");

			if ("/".equals(remotePathPrefix))
				throw new IllegalStateException("The remotePathPrefix should be an empty string, if the root is checked out!");

			final int lastSlashIndex = remotePathPrefix.lastIndexOf('/');

			final String uidStr = lastSlashIndex < 0 ? remotePathPrefix : remotePathPrefix.substring(lastSlashIndex + 1);
			cryptoRepoFileIdForRemotePathPrefix = new Uid(uidStr);
		}
		return cryptoRepoFileIdForRemotePathPrefix;
	}

	private RepoFile getRepoFile(final String localPath) {
		final LocalRepoManager localRepoManager = transaction.getLocalRepoManager();
		final RepoFileDao repoFileDao = transaction.getDao(RepoFileDao.class);
		final RepoFile repoFile = repoFileDao.getRepoFile(localRepoManager.getLocalRoot(), createFile(localRepoManager.getLocalRoot(), localPath));
		return repoFile;
	}
}