package org.subshare.local;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.local.CryptreeNodeUtil.*;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.KeyParameter;
import org.subshare.core.crypto.KeyFactory;
import org.subshare.core.dto.CryptoKeyPart;
import org.subshare.core.dto.CryptoKeyRole;
import org.subshare.core.dto.CryptoKeyType;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.user.UserRepoKey;
import org.subshare.crypto.CipherOperationMode;
import org.subshare.local.persistence.CryptoKey;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.PermissionDao;
import org.subshare.local.persistence.PermissionSet;
import org.subshare.local.persistence.PermissionSetInheritance;
import org.subshare.local.persistence.UserRepoKeyPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class PlainCryptoKeyFactory {

	private CryptreeNode cryptreeNode;
//	private CryptoKeyPart cryptoKeyPart;
	private CipherOperationMode cipherOperationMode;

	public CryptreeNode getCryptreeNode() {
		return cryptreeNode;
	}
	public void setCryptreeNode(final CryptreeNode cryptreeNode) {
		this.cryptreeNode = cryptreeNode;
	}
	public CryptreeNode getCryptreeNodeOrFail() {
		final CryptreeNode cryptreeNode = getCryptreeNode();
		return assertNotNull("cryptreeNode", cryptreeNode);
	}

//	public CryptoKeyPart getCryptoKeyPart() {
//		return cryptoKeyPart;
//	}
//	public void setCryptoKeyPart(final CryptoKeyPart cryptoKeyPart) {
//		this.cryptoKeyPart = cryptoKeyPart;
//	}
//	public CryptoKeyPart getCryptoKeyPartOrFail() {
//		final CryptoKeyPart cryptoKeyPart = getCryptoKeyPart();
//		return assertNotNull("cryptoKeyPart", cryptoKeyPart);
//	}

	public CipherOperationMode getCipherOperationMode() {
		return cipherOperationMode;
	}
	public void setCipherOperationMode(final CipherOperationMode cipherOperationMode) {
		this.cipherOperationMode = cipherOperationMode;
	}
	public CipherOperationMode getCipherOperationModeOrFail() {
		final CipherOperationMode cipherOperationMode = getCipherOperationMode();
		return assertNotNull("cipherOperationMode", cipherOperationMode);
	}

	public CryptreeContext getContext() {
		return cryptreeNode == null ? null : cryptreeNode.getContext();
	}

	public CryptreeContext getContextOrFail() {
		final CryptreeContext context = getContext();
		return assertNotNull("context", context);
	}

	public abstract PlainCryptoKey createPlainCryptoKey();


	protected CryptoKey createCryptoKey(final CryptoKeyRole cryptoKeyRole, final CryptoKeyType cryptoKeyType) {
		assertNotNull("cryptoKeyRole", cryptoKeyRole);
		assertNotNull("cryptoKeyType", cryptoKeyType);
		final CryptoKey cryptoKey = new CryptoKey();
		cryptoKey.setCryptoRepoFile(getCryptreeNodeOrFail().getCryptoRepoFile());
		cryptoKey.setCryptoKeyRole(cryptoKeyRole);
		cryptoKey.setCryptoKeyType(cryptoKeyType);

		getCryptreeNodeOrFail().sign(cryptoKey);

		return cryptoKey;
	}

	protected PlainCryptoKey createSymmetricPlainCryptoKey(final CryptoKeyRole cryptoKeyRole) {
		assertNotNull("cryptoKeyRole", cryptoKeyRole);

//		if (CryptoKeyPart.sharedSecret != getCryptoKeyPartOrFail())
//			throw new IllegalStateException("CryptoKeyPart.sharedSecret != getCryptoKeyPartOrFail()");

		final KeyParameter keyParameter = KeyFactory.getInstance().createSymmetricKey();
		final CryptoKey cryptoKey = createCryptoKey(cryptoKeyRole, CryptoKeyType.symmetric);
		final PlainCryptoKey plainCryptoKey = new PlainCryptoKey(cryptoKey, CryptoKeyPart.sharedSecret, keyParameter);
		return plainCryptoKey;
	}

	static class ClearanceKeyPlainCryptoKeyFactory extends PlainCryptoKeyFactory {
		private static final Logger logger = LoggerFactory.getLogger(PlainCryptoKeyFactory.ClearanceKeyPlainCryptoKeyFactory.class);

		@Override
		public PlainCryptoKey createPlainCryptoKey() {
			final CryptreeNode cryptreeNode = getCryptreeNodeOrFail();
//			final CryptoKeyPart cryptoKeyPart = getCryptoKeyPartOrFail();
			final CipherOperationMode cipherOperationMode = getCipherOperationModeOrFail();
			final CryptreeContext context = getContextOrFail();

			logger.debug("createPlainCryptoKey: >>> cryptoRepoFile={} repoFile={}",
					cryptreeNode.getCryptoRepoFile(), cryptreeNode.getRepoFile());

			// TODO we must check, if there is already a clearance key to which we have no access.
			// We should not implicitly create a clearance key - we maybe simply don't have access.
			// In this case, we must throw an AccessDeniedException instead!

			final AsymmetricCipherKeyPair keyPair = KeyFactory.getInstance().createAsymmetricKeyPair();
			final CryptoKey cryptoKey = createCryptoKey(CryptoKeyRole.clearanceKey, CryptoKeyType.asymmetric);

			final PlainCryptoKey clearanceKeyPlainCryptoKey_public = new PlainCryptoKey(cryptoKey, CryptoKeyPart.publicKey, keyPair.getPublic());
			final PlainCryptoKey clearanceKeyPlainCryptoKey_private = new PlainCryptoKey(cryptoKey, CryptoKeyPart.privateKey, keyPair.getPrivate());

			createCryptoLink(getCryptreeNodeOrFail(), clearanceKeyPlainCryptoKey_public);

			// We use the same for write and read, because this reduces the ability of an attacker to find out which
			// UserRepoKeys belong to the same person.
			UserRepoKey userRepoKeyForRead = cryptreeNode.getUserRepoKey(false, PermissionType.write);
			if (userRepoKeyForRead == null)
				userRepoKeyForRead = context.userRepoKeyRing.getPermanentUserRepoKeys(context.serverRepositoryId).get(0);

			final UserRepoKeyPublicKey userRepoKeyPublicKey = cryptreeNode.getUserRepoKeyPublicKeyOrCreate(userRepoKeyForRead);
			createCryptoLink(getCryptreeNodeOrFail(), userRepoKeyPublicKey, clearanceKeyPlainCryptoKey_private);

			// TODO maybe we should give the clearance key other users, too?! not only the current user?! NO, this happens outside. At least right now and I think this is good.

			createCryptoLinkToSubdirKey(clearanceKeyPlainCryptoKey_public);

			logger.debug("createPlainCryptoKey: <<< cryptoRepoFile={} repoFile={}",
					cryptreeNode.getCryptoRepoFile(), cryptreeNode.getRepoFile());

			switch(cipherOperationMode) {
				case DECRYPT:
					return clearanceKeyPlainCryptoKey_private;
				case ENCRYPT:
					return clearanceKeyPlainCryptoKey_public;
				default:
					throw new IllegalStateException("Property cipherOperationMode has an unexpected value: " + cipherOperationMode);
			}
		}

		private void createCryptoLinkToSubdirKey(final PlainCryptoKey fromPlainCryptoKey) {
			assertNotNull("fromPlainCryptoKey", fromPlainCryptoKey);
			final CryptreeNode cryptreeNode = getCryptreeNodeOrFail();

			final PlainCryptoKey subdirKeyPlainCryptoKey = cryptreeNode.getActivePlainCryptoKey(CryptoKeyRole.subdirKey, CipherOperationMode.DECRYPT);
			if (subdirKeyPlainCryptoKey == null) { // during initialisation, this is possible
				// This is also possible during revocation of read-access-rights. We thus must allow it.
//				if (cryptreeNode.getParent() != null) // but only, if there is no parent (see SubdirKeyPlainCryptoKeyFactory below)
//					throw new IllegalStateException("subdirKeyPlainCryptoKey == null, but cryptreeNode.parent != null");
			}
			else
				createCryptoLink(getCryptreeNodeOrFail(), fromPlainCryptoKey, subdirKeyPlainCryptoKey);
		}
	}

	static class SubdirKeyPlainCryptoKeyFactory extends PlainCryptoKeyFactory {
		private static final Logger logger = LoggerFactory.getLogger(PlainCryptoKeyFactory.SubdirKeyPlainCryptoKeyFactory.class);

		@Override
		public PlainCryptoKey createPlainCryptoKey() {
			final CryptreeNode cryptreeNode = getCryptreeNodeOrFail();

			logger.debug("createPlainCryptoKey: >>> cryptoRepoFile={} repoFile={}",
					cryptreeNode.getCryptoRepoFile(), cryptreeNode.getRepoFile());

			if (! cryptreeNode.isDirectory())
				throw new IllegalStateException("Cannot create a subdirKey, because this CryptreeNode is *not* a directory!");

			final CryptoKeyType cryptoKeyType;
			final CryptoRepoFile cryptoRepoFile = cryptreeNode.getCryptoRepoFile();
			if (cryptoRepoFile == null)
				cryptoKeyType = CryptoKeyType.symmetric;
			else {
				final PermissionDao permissionDao = getContextOrFail().transaction.getDao(PermissionDao.class);
				if (0 == permissionDao.getPermissionCountOfDirectChildCryptoRepoFiles(cryptoRepoFile, PermissionType.grant))
					cryptoKeyType = CryptoKeyType.symmetric;
				else
					cryptoKeyType = CryptoKeyType.asymmetric;
			}

			final PlainCryptoKey plainCryptoKey_privateOrShared;
			final PlainCryptoKey plainCryptoKey_publicOrShared;
			switch (cryptoKeyType) {
				case symmetric:
					plainCryptoKey_privateOrShared = createSymmetricPlainCryptoKey(CryptoKeyRole.subdirKey);
					plainCryptoKey_publicOrShared = plainCryptoKey_privateOrShared;
					break;
				case asymmetric:
					final AsymmetricCipherKeyPair keyPair = KeyFactory.getInstance().createAsymmetricKeyPair();
					final CryptoKey cryptoKey = createCryptoKey(CryptoKeyRole.subdirKey, cryptoKeyType);

					plainCryptoKey_publicOrShared = new PlainCryptoKey(cryptoKey, CryptoKeyPart.publicKey, keyPair.getPublic());
					plainCryptoKey_privateOrShared = new PlainCryptoKey(cryptoKey, CryptoKeyPart.privateKey, keyPair.getPrivate());

					createCryptoLink(cryptreeNode, plainCryptoKey_publicOrShared);
					break;
				default:
					throw new IllegalStateException("Unexpected cryptoKeyType: " + cryptoKeyType);
			}


			boolean saved = false;
			saved |= createCryptoLinkFromParentSubdirKey(plainCryptoKey_privateOrShared);
			saved |= createCryptoLinkFromClearanceKey(plainCryptoKey_privateOrShared);

			if (! saved)
				throw new IllegalStateException("Cannot create subdirKey because nobody has a clearance key leading directly or indirectly to it!");

			createCryptoLinkToFileKey(plainCryptoKey_publicOrShared);

			final PermissionSet permissionSet = cryptreeNode.getPermissionSet();
			if (permissionSet == null || containsNonRevokedPermissionSetInheritance(permissionSet)) {
				for (final CryptreeNode child : cryptreeNode.getChildren()) {
					createCryptoLinkToChildSubdirKey(plainCryptoKey_publicOrShared, child);
//					createCryptoLinkToChildDataKey(plainCryptoKey_publicOrShared, child);
					createCryptoLinkToChildBacklinkKey(plainCryptoKey_publicOrShared, child);
				}
			}

			logger.debug("createPlainCryptoKey: <<< cryptoRepoFile={} repoFile={}",
					cryptreeNode.getCryptoRepoFile(), cryptreeNode.getRepoFile());

			switch(getCipherOperationModeOrFail()) {
				case DECRYPT:
					return plainCryptoKey_privateOrShared;
				case ENCRYPT:
					return plainCryptoKey_publicOrShared;
				default:
					throw new IllegalStateException("Property cipherOperationMode has an unexpected value: " + getCipherOperationModeOrFail());
			}
		}

		private void createCryptoLinkToChildSubdirKey(final PlainCryptoKey fromPlainCryptoKey, final CryptreeNode toChild) {
			assertNotNull("fromPlainCryptoKey", fromPlainCryptoKey);
			assertNotNull("toChild", toChild);

			if (! toChild.isDirectory())
				return;

			// The key on the *to*-side of the CryptoLink *must* exist! But to easily avoid endless recursions
			// (without further code), we don't create the key when following the link forward.
			final PlainCryptoKey childSubdirKeyPlainCryptoKey = toChild.getActivePlainCryptoKey(CryptoKeyRole.subdirKey, CipherOperationMode.DECRYPT);
			if (childSubdirKeyPlainCryptoKey != null)
				createCryptoLink(toChild, fromPlainCryptoKey, childSubdirKeyPlainCryptoKey);
		}

//		private void createCryptoLinkToChildDataKey(final PlainCryptoKey fromPlainCryptoKey, final CryptreeNode toChild) {
//			assertNotNull("fromPlainCryptoKey", fromPlainCryptoKey);
//			assertNotNull("toChild", toChild);
//
//			if (toChild.isDirectory())
//				return;
//
//			// The key on the *to*-side of the CryptoLink *must* exist! But to easily avoid endless recursions
//			// (without further code), we don't create the key when following the link forward.
//			final PlainCryptoKey childDataKeyPlainCryptoKey = toChild.getActivePlainCryptoKey(CryptoKeyRole.dataKey, CipherOperationMode.DECRYPT);
//			if (childDataKeyPlainCryptoKey != null)
//				createCryptoLink(toChild, fromPlainCryptoKey, childDataKeyPlainCryptoKey);
//		}

		private void createCryptoLinkToFileKey(final PlainCryptoKey fromPlainCryptoKey) {
			assertNotNull("fromPlainCryptoKey", fromPlainCryptoKey);
			final CryptreeNode cryptreeNode = getCryptreeNodeOrFail();

			final PlainCryptoKey fileKeyPlainCryptoKey = cryptreeNode.getActivePlainCryptoKey(CryptoKeyRole.fileKey, CipherOperationMode.DECRYPT);
			if (fileKeyPlainCryptoKey != null)
				createCryptoLink(cryptreeNode, fromPlainCryptoKey, fileKeyPlainCryptoKey);
		}

		private void createCryptoLinkToChildBacklinkKey(final PlainCryptoKey fromPlainCryptoKey, final CryptreeNode toChild) {
			assertNotNull("fromPlainCryptoKey", fromPlainCryptoKey);
			assertNotNull("toChild", toChild);

			if (toChild.isDirectory())
				return;

			final PlainCryptoKey childBacklinkKeyPlainCryptoKey = toChild.getActivePlainCryptoKey(CryptoKeyRole.backlinkKey, CipherOperationMode.DECRYPT);
			if (childBacklinkKeyPlainCryptoKey != null)
				createCryptoLink(toChild, fromPlainCryptoKey, childBacklinkKeyPlainCryptoKey);
		}

		private boolean createCryptoLinkFromParentSubdirKey(final PlainCryptoKey toPlainCryptoKey) {
			assertNotNull("toPlainCryptoKey", toPlainCryptoKey);
			final CryptreeNode cryptreeNode = getCryptreeNodeOrFail();
			final CryptreeNode parent = cryptreeNode.getParent();
			if (parent != null) {
				final PermissionSet parentPermissionSet = cryptreeNode.getPermissionSet();
				if (parentPermissionSet == null || containsNonRevokedPermissionSetInheritance(parentPermissionSet)) {
					final PlainCryptoKey subdirKeyPlainCryptoKey = parent.getActivePlainCryptoKeyOrCreate(CryptoKeyRole.subdirKey, CipherOperationMode.ENCRYPT);
					createCryptoLink(cryptreeNode, subdirKeyPlainCryptoKey, toPlainCryptoKey);
					return true;
				}
			}
			return false;
		}

		private boolean containsNonRevokedPermissionSetInheritance(final PermissionSet permissionSet) {
			assertNotNull("permissionSet", permissionSet);
			for (final PermissionSetInheritance permissionSetInheritance : permissionSet.getPermissionSetInheritances()) {
				if (permissionSetInheritance.getRevoked() == null)
					return true;
			}
			return false;
		}

		private boolean createCryptoLinkFromClearanceKey(final PlainCryptoKey toPlainCryptoKey) {
			assertNotNull("toPlainCryptoKey", toPlainCryptoKey);
			final CryptreeNode cryptreeNode = getCryptreeNodeOrFail();

			final PlainCryptoKey clearanceKeyPlainCryptoKey_public;
			if (cryptreeNode.getParent() == null) // If it's the root and there is no public key, yet, we become the owner of the repo, now ;-)
				clearanceKeyPlainCryptoKey_public = cryptreeNode.getActivePlainCryptoKeyOrCreate(CryptoKeyRole.clearanceKey, CipherOperationMode.ENCRYPT);
			else
				clearanceKeyPlainCryptoKey_public = cryptreeNode.getActivePlainCryptoKey(CryptoKeyRole.clearanceKey, CipherOperationMode.ENCRYPT);

			if (clearanceKeyPlainCryptoKey_public != null) {
				createCryptoLink(cryptreeNode, clearanceKeyPlainCryptoKey_public, toPlainCryptoKey);
				return true;
			}
			return false;
		}
	}

	static class FileKeyPlainCryptoKeyFactory extends PlainCryptoKeyFactory {
		private static final Logger logger = LoggerFactory.getLogger(PlainCryptoKeyFactory.FileKeyPlainCryptoKeyFactory.class);

		@Override
		public PlainCryptoKey createPlainCryptoKey() {
			final CryptreeNode cryptreeNode = getCryptreeNodeOrFail();

			logger.debug("createPlainCryptoKey: >>> cryptoRepoFile={} repoFile={}",
					cryptreeNode.getCryptoRepoFile(), cryptreeNode.getRepoFile());

			if (! cryptreeNode.isDirectory())
				throw new IllegalStateException("Cannot create a fileKey, because this CryptreeNode is *not* a directory!");

			final PlainCryptoKey plainCryptoKey = createSymmetricPlainCryptoKey(CryptoKeyRole.fileKey);

			createCryptoLinkFromSubdirKey(plainCryptoKey);

			for (final CryptreeNode child : cryptreeNode.getChildren())
				createCryptoLinkToChildDataKey(plainCryptoKey, child);

			logger.debug("createPlainCryptoKey: <<< cryptoRepoFile={} repoFile={}",
					cryptreeNode.getCryptoRepoFile(), cryptreeNode.getRepoFile());

			return plainCryptoKey;
		}

		private void createCryptoLinkFromSubdirKey(final PlainCryptoKey toPlainCryptoKey) {
			assertNotNull("toPlainCryptoKey", toPlainCryptoKey);
			final CryptreeNode cryptreeNode = getCryptreeNodeOrFail();
			final PlainCryptoKey subdirKeyPlainCryptoKey = cryptreeNode.getActivePlainCryptoKeyOrCreate(CryptoKeyRole.subdirKey, CipherOperationMode.ENCRYPT);
			createCryptoLink(cryptreeNode, subdirKeyPlainCryptoKey, toPlainCryptoKey);
		}

		private void createCryptoLinkToChildDataKey(final PlainCryptoKey fromPlainCryptoKey, final CryptreeNode toChild) {
			assertNotNull("fromPlainCryptoKey", fromPlainCryptoKey);
			assertNotNull("toChild", toChild);

			// The key on the *to*-side of the CryptoLink *must* exist! But to easily avoid endless recursions
			// (without further code), we don't create the key when following the link forward.
			final PlainCryptoKey childDataKeyPlainCryptoKey = toChild.getActivePlainCryptoKey(CryptoKeyRole.dataKey, CipherOperationMode.DECRYPT);
			if (childDataKeyPlainCryptoKey != null)
				createCryptoLink(toChild, fromPlainCryptoKey, childDataKeyPlainCryptoKey);
		}
	}

	static class BacklinkKeyPlainCryptoKeyFactory extends PlainCryptoKeyFactory {
		private static final Logger logger = LoggerFactory.getLogger(PlainCryptoKeyFactory.BacklinkKeyPlainCryptoKeyFactory.class);

		@Override
		public PlainCryptoKey createPlainCryptoKey() {
			final CryptreeNode cryptreeNode = getCryptreeNodeOrFail();

			logger.debug("createPlainCryptoKey: >>> cryptoRepoFile={} repoFile={}",
					cryptreeNode.getCryptoRepoFile(), cryptreeNode.getRepoFile());

			final PlainCryptoKey plainCryptoKey = createSymmetricPlainCryptoKey(CryptoKeyRole.backlinkKey);
			createCryptoLinkFromSubdirKey(plainCryptoKey);

			for (final CryptreeNode child : cryptreeNode.getChildren())
				createCryptoLinkFromChildBacklinkKey(child, plainCryptoKey);

			createCryptoLinkToParentBacklinkKey(plainCryptoKey);

			logger.debug("createPlainCryptoKey: <<< cryptoRepoFile={} repoFile={}",
					cryptreeNode.getCryptoRepoFile(), cryptreeNode.getRepoFile());

			return plainCryptoKey;
		}

		private void createCryptoLinkToParentBacklinkKey(final PlainCryptoKey fromPlainCryptoKey) {
			assertNotNull("fromPlainCryptoKey", fromPlainCryptoKey);
			final CryptreeNode cryptreeNode = getCryptreeNodeOrFail();
			final CryptreeNode parent = cryptreeNode.getParent();
			if (parent != null) {
				final PlainCryptoKey parentBacklinkKeyPlainCryptoKey = parent.getActivePlainCryptoKey(CryptoKeyRole.backlinkKey, CipherOperationMode.DECRYPT);
				if (parentBacklinkKeyPlainCryptoKey != null)
					createCryptoLink(parent, fromPlainCryptoKey, parentBacklinkKeyPlainCryptoKey);
			}
		}

		private void createCryptoLinkFromSubdirKey(final PlainCryptoKey toPlainCryptoKey) {
			assertNotNull("toPlainCryptoKey", toPlainCryptoKey);
			final CryptreeNode cryptreeNode = getCryptreeNodeOrFail();

			if (! cryptreeNode.isDirectory())
				return;

			final PlainCryptoKey subdirKeyPlainCryptoKey = cryptreeNode.getActivePlainCryptoKeyOrCreate(CryptoKeyRole.subdirKey, CipherOperationMode.ENCRYPT);
			createCryptoLink(cryptreeNode, subdirKeyPlainCryptoKey, toPlainCryptoKey);
		}

		private void createCryptoLinkFromChildBacklinkKey(final CryptreeNode fromChild, final PlainCryptoKey toPlainCryptoKey) {
			assertNotNull("fromChild", fromChild);
			assertNotNull("toPlainCryptoKey", toPlainCryptoKey);
			final PlainCryptoKey childBacklinkKeyPlainCryptoKey = fromChild.getActivePlainCryptoKeyOrCreate(CryptoKeyRole.backlinkKey, CipherOperationMode.ENCRYPT);
			createCryptoLink(getCryptreeNodeOrFail(), childBacklinkKeyPlainCryptoKey, toPlainCryptoKey);
		}
	}

	static class DataKeyPlainCryptoKeyFactory extends PlainCryptoKeyFactory {
		private static final Logger logger = LoggerFactory.getLogger(PlainCryptoKeyFactory.DataKeyPlainCryptoKeyFactory.class);

		@Override
		public PlainCryptoKey createPlainCryptoKey() {
			final CryptreeNode cryptreeNode = getCryptreeNodeOrFail();

			logger.debug("createPlainCryptoKey: >>> cryptoRepoFile={} repoFile={}",
					cryptreeNode.getCryptoRepoFile(), cryptreeNode.getRepoFile());

			final PlainCryptoKey plainCryptoKey = createSymmetricPlainCryptoKey(CryptoKeyRole.dataKey);
			createCryptoLinkFromBacklinkKey(plainCryptoKey);
			createCryptoLinkFromParentFileKey(plainCryptoKey);

			logger.debug("createPlainCryptoKey: <<< cryptoRepoFile={} repoFile={}",
					cryptreeNode.getCryptoRepoFile(), cryptreeNode.getRepoFile());

			return plainCryptoKey;
		}

		private void createCryptoLinkFromBacklinkKey(final PlainCryptoKey toPlainCryptoKey) {
			assertNotNull("toPlainCryptoKey", toPlainCryptoKey);
			final CryptreeNode cryptreeNode = getCryptreeNodeOrFail();

			final PlainCryptoKey backlinkKeyPlainCryptoKey;
			if (cryptreeNode.isDirectory())
				backlinkKeyPlainCryptoKey = cryptreeNode.getActivePlainCryptoKeyOrCreate(CryptoKeyRole.backlinkKey, CipherOperationMode.ENCRYPT);
			else
				backlinkKeyPlainCryptoKey = cryptreeNode.getActivePlainCryptoKey(CryptoKeyRole.backlinkKey, CipherOperationMode.ENCRYPT);

			if (backlinkKeyPlainCryptoKey != null)
				createCryptoLink(cryptreeNode, backlinkKeyPlainCryptoKey, toPlainCryptoKey);
		}

		private void createCryptoLinkFromParentFileKey(final PlainCryptoKey toPlainCryptoKey) {
			assertNotNull("toPlainCryptoKey", toPlainCryptoKey);
			final CryptreeNode cryptreeNode = getCryptreeNodeOrFail();

			if (cryptreeNode.isDirectory())
				return;

			final CryptreeNode parent = cryptreeNode.getParent();
			if (parent == null)
				throw new IllegalStateException("cryptreeNode is *not* a directory, but parent == null !!!");

			final PlainCryptoKey fileKeyPlainCryptoKey = parent.getActivePlainCryptoKeyOrCreate(CryptoKeyRole.fileKey, CipherOperationMode.ENCRYPT);
			createCryptoLink(cryptreeNode, fileKeyPlainCryptoKey, toPlainCryptoKey);
		}
	}

}
