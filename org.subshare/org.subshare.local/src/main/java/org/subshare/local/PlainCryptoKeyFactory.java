package org.subshare.local;

import static co.codewizards.cloudstore.core.util.Util.*;
import static org.subshare.local.CryptreeNodeUtil.*;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.KeyParameter;
import org.subshare.core.crypto.KeyFactory;
import org.subshare.core.dto.CryptoKeyPart;
import org.subshare.core.dto.CryptoKeyRole;
import org.subshare.core.dto.CryptoKeyType;
import org.subshare.local.persistence.CryptoKey;

abstract class PlainCryptoKeyFactory {

	private CryptreeNode cryptreeNode;

	private CryptoKeyPart cryptoKeyPart;

	public CryptreeNode getCryptreeNode() {
		return cryptreeNode;
	}
	public void setCryptreeNode(final CryptreeNode cryptreeNode) {
		this.cryptreeNode = cryptreeNode;
	}
	public CryptreeNode getCryptreeNodeOrFail() {
		final CryptreeNode cryptreeNode = getCryptreeNode();
		assertNotNull("cryptreeNode", cryptreeNode);
		return cryptreeNode;
	}

	public CryptoKeyPart getCryptoKeyPart() {
		return cryptoKeyPart;
	}
	public void setCryptoKeyPart(final CryptoKeyPart cryptoKeyPart) {
		this.cryptoKeyPart = cryptoKeyPart;
	}
	public CryptoKeyPart getCryptoKeyPartOrFail() {
		final CryptoKeyPart cryptoKeyPart = getCryptoKeyPart();
		assertNotNull("cryptoKeyPart", cryptoKeyPart);
		return cryptoKeyPart;
	}

	public abstract PlainCryptoKey createPlainCryptoKey();

	protected CryptoKey createCryptoKey(final CryptoKeyRole cryptoKeyRole, final CryptoKeyType cryptoKeyType) {
		assertNotNull("cryptoKeyRole", cryptoKeyRole);
		assertNotNull("cryptoKeyType", cryptoKeyType);
		final CryptoKey cryptoKey = new CryptoKey();
		cryptoKey.setCryptoRepoFile(getCryptreeNodeOrFail().getCryptoRepoFile());
		cryptoKey.setCryptoKeyRole(cryptoKeyRole);
		cryptoKey.setCryptoKeyType(cryptoKeyType);
		return cryptoKey;
	}

	protected PlainCryptoKey createSymmetricPlainCryptoKey(final CryptoKeyRole cryptoKeyRole) {
		assertNotNull("cryptoKeyRole", cryptoKeyRole);

		if (CryptoKeyPart.sharedSecret != getCryptoKeyPartOrFail())
			throw new IllegalStateException("CryptoKeyPart.sharedSecret != getCryptoKeyPartOrFail()");

		final KeyParameter keyParameter = KeyFactory.getInstance().createSymmetricKey();
		final CryptoKey cryptoKey = createCryptoKey(cryptoKeyRole, CryptoKeyType.symmetric);
		final PlainCryptoKey plainCryptoKey = new PlainCryptoKey(cryptoKey, CryptoKeyPart.sharedSecret, keyParameter);
		return plainCryptoKey;
	}

	static class ClearanceKeyPlainCryptoKeyFactory extends PlainCryptoKeyFactory {

		@Override
		public PlainCryptoKey createPlainCryptoKey() {
			final CryptreeNode cryptreeNode = getCryptreeNodeOrFail();
			final CryptoKeyPart cryptoKeyPart = getCryptoKeyPartOrFail();

			final AsymmetricCipherKeyPair keyPair = KeyFactory.getInstance().createAsymmetricKeyPair();
			final CryptoKey cryptoKey = createCryptoKey(CryptoKeyRole.clearanceKey, CryptoKeyType.asymmetric);

			final PlainCryptoKey clearanceKeyPlainCryptoKey_public = new PlainCryptoKey(cryptoKey, CryptoKeyPart.publicKey, keyPair.getPublic());
			final PlainCryptoKey clearanceKeyPlainCryptoKey_private = new PlainCryptoKey(cryptoKey, CryptoKeyPart.privateKey, keyPair.getPrivate());

			createCryptoLink(clearanceKeyPlainCryptoKey_public);
			createCryptoLink(cryptreeNode.getUserRepoKeyPublicKey(), clearanceKeyPlainCryptoKey_private);

			// TODO maybe we should give the clearance key other users, too?! not only the current user?!

			final PlainCryptoKey subdirKeyPlainCryptoKey = cryptreeNode.getActivePlainCryptoKey(CryptoKeyRole.subdirKey, CryptoKeyPart.sharedSecret);
			if (subdirKeyPlainCryptoKey == null) { // during initialisation, this is possible
				// This is also possible during revocation of read-access-rights. We thus must allow it.
//				if (cryptreeNode.getParent() != null) // but only, if there is no parent (see SubdirKeyPlainCryptoKeyFactory below)
//					throw new IllegalStateException("subdirKeyPlainCryptoKey == null, but cryptreeNode.parent != null");
			}
			else
				createCryptoLink(clearanceKeyPlainCryptoKey_public, subdirKeyPlainCryptoKey);

			switch(cryptoKeyPart) {
				case publicKey:
					return clearanceKeyPlainCryptoKey_public;
				case privateKey:
					return clearanceKeyPlainCryptoKey_private;
				default:
					throw new IllegalStateException("Property cryptoKeyPart has an unexpected value: " + cryptoKeyPart);
			}
		}
	}

	static class SubdirKeyPlainCryptoKeyFactory extends PlainCryptoKeyFactory {
		@Override
		public PlainCryptoKey createPlainCryptoKey() {
			final CryptreeNode cryptreeNode = getCryptreeNodeOrFail();
			if (! cryptreeNode.isDirectory())
				throw new IllegalStateException("Cannot create a subdirKey, because this CryptreeNode is *not* a directory!");

			final PlainCryptoKey plainCryptoKey = createSymmetricPlainCryptoKey(CryptoKeyRole.subdirKey);

			boolean saved = false;
			saved |= createCryptoLinkFromParentSubdirKey(plainCryptoKey);
			saved |= createCryptoLinkFromClearanceKey(plainCryptoKey);

			if (! saved)
				throw new IllegalStateException("Cannot create subdirKey because nobody has a clearance key leading directly or indirectly to it!");

			for (final CryptreeNode child : cryptreeNode.getChildren()) {
				createCryptoLinkToChildSubdirKey(plainCryptoKey, child);
				createCryptoLinkToChildDataKey(plainCryptoKey, child);
			}

			return plainCryptoKey;
		}

		private void createCryptoLinkToChildSubdirKey(final PlainCryptoKey fromPlainCryptoKey, final CryptreeNode toChild) {
			assertNotNull("fromPlainCryptoKey", fromPlainCryptoKey);
			assertNotNull("toChild", toChild);

			if (! toChild.isDirectory())
				return;

			// The key on the *to*-side of the CryptoLink *must* exist! But to easily avoid endless recursions
			// (without further code), we don't create the key when following the link forward.
			final PlainCryptoKey childSubdirKeyPlainCryptoKey = toChild.getActivePlainCryptoKey(CryptoKeyRole.subdirKey, CryptoKeyPart.sharedSecret);
			if (childSubdirKeyPlainCryptoKey != null)
				createCryptoLink(fromPlainCryptoKey, childSubdirKeyPlainCryptoKey);
		}

		private void createCryptoLinkToChildDataKey(final PlainCryptoKey fromPlainCryptoKey, final CryptreeNode toChild) {
			assertNotNull("fromPlainCryptoKey", fromPlainCryptoKey);
			assertNotNull("toChild", toChild);

			if (toChild.isDirectory())
				return;

			// The key on the *to*-side of the CryptoLink *must* exist! But to easily avoid endless recursions
			// (without further code), we don't create the key when following the link forward.
			final PlainCryptoKey childDataKeyPlainCryptoKey = toChild.getActivePlainCryptoKey(CryptoKeyRole.dataKey, CryptoKeyPart.sharedSecret);
			if (childDataKeyPlainCryptoKey != null)
				createCryptoLink(fromPlainCryptoKey, childDataKeyPlainCryptoKey);
		}

		private boolean createCryptoLinkFromParentSubdirKey(final PlainCryptoKey toPlainCryptoKey) {
			assertNotNull("toPlainCryptoKey", toPlainCryptoKey);
			final CryptreeNode cryptreeNode = getCryptreeNodeOrFail();
			final CryptreeNode parent = cryptreeNode.getParent();
			if (parent != null) {
				final PlainCryptoKey subdirKeyPlainCryptoKey = parent.getActivePlainCryptoKeyOrCreate(CryptoKeyRole.subdirKey, CryptoKeyPart.sharedSecret);
				createCryptoLink(subdirKeyPlainCryptoKey, toPlainCryptoKey);
				return true;
			}
			return false;
		}

		private boolean createCryptoLinkFromClearanceKey(final PlainCryptoKey toPlainCryptoKey) {
			assertNotNull("toPlainCryptoKey", toPlainCryptoKey);
			final CryptreeNode cryptreeNode = getCryptreeNodeOrFail();

			final PlainCryptoKey clearanceKeyPlainCryptoKey_public;
			if (cryptreeNode.getParent() == null) // If it's the root and there is no public key, yet, we become the owner of the repo, now ;-)
				clearanceKeyPlainCryptoKey_public = cryptreeNode.getActivePlainCryptoKeyOrCreate(CryptoKeyRole.clearanceKey, CryptoKeyPart.publicKey);
			else
				clearanceKeyPlainCryptoKey_public = cryptreeNode.getActivePlainCryptoKey(CryptoKeyRole.clearanceKey, CryptoKeyPart.publicKey);

			if (clearanceKeyPlainCryptoKey_public != null) {
				createCryptoLink(clearanceKeyPlainCryptoKey_public, toPlainCryptoKey);
				return true;
			}
			return false;
		}
	}

	static class FileKeyPlainCryptoKeyFactory extends PlainCryptoKeyFactory {
		@Override
		public PlainCryptoKey createPlainCryptoKey() {
			final CryptreeNode cryptreeNode = getCryptreeNodeOrFail();
			if (! cryptreeNode.isDirectory())
				throw new IllegalStateException("Cannot create a fileKey, because this CryptreeNode is *not* a directory!");

			final PlainCryptoKey plainCryptoKey = createSymmetricPlainCryptoKey(CryptoKeyRole.fileKey);

			createCryptoLinkFromSubdirKey(plainCryptoKey);

			for (final CryptreeNode child : getCryptreeNodeOrFail().getChildren())
				createCryptoLinkToChildDataKey(plainCryptoKey, child);

			return plainCryptoKey;
		}

		private void createCryptoLinkFromSubdirKey(final PlainCryptoKey toPlainCryptoKey) {
			assertNotNull("toPlainCryptoKey", toPlainCryptoKey);
			final CryptreeNode cryptreeNode = getCryptreeNodeOrFail();
			final PlainCryptoKey subdirKeyPlainCryptoKey = cryptreeNode.getActivePlainCryptoKeyOrCreate(CryptoKeyRole.subdirKey, CryptoKeyPart.sharedSecret);
			createCryptoLink(subdirKeyPlainCryptoKey, toPlainCryptoKey);
		}

		private void createCryptoLinkToChildDataKey(final PlainCryptoKey fromPlainCryptoKey, final CryptreeNode toChild) {
			assertNotNull("fromPlainCryptoKey", fromPlainCryptoKey);
			assertNotNull("toChild", toChild);

			// The key on the *to*-side of the CryptoLink *must* exist! But to easily avoid endless recursions
			// (without further code), we don't create the key when following the link forward.
			final PlainCryptoKey childDataKeyPlainCryptoKey = toChild.getActivePlainCryptoKey(CryptoKeyRole.dataKey, CryptoKeyPart.sharedSecret);
			if (childDataKeyPlainCryptoKey != null)
				createCryptoLink(fromPlainCryptoKey, childDataKeyPlainCryptoKey);
		}
	}

	static class BacklinkKeyPlainCryptoKeyFactory extends PlainCryptoKeyFactory {
		@Override
		public PlainCryptoKey createPlainCryptoKey() {
			final PlainCryptoKey plainCryptoKey = createSymmetricPlainCryptoKey(CryptoKeyRole.backlinkKey);
			createCryptoLinkFromSubdirKey(plainCryptoKey);

			for (final CryptreeNode child : getCryptreeNodeOrFail().getChildren())
				createCryptoLinkFromChildBacklinkKey(child, plainCryptoKey);

			createCryptoLinkToParentBacklinkKey(plainCryptoKey);

			return plainCryptoKey;
		}

		private void createCryptoLinkToParentBacklinkKey(final PlainCryptoKey fromPlainCryptoKey) {
			assertNotNull("fromPlainCryptoKey", fromPlainCryptoKey);
			final CryptreeNode cryptreeNode = getCryptreeNodeOrFail();
			final CryptreeNode parent = cryptreeNode.getParent();
			if (parent != null) {
				final PlainCryptoKey parentBacklinkKeyPlainCryptoKey = parent.getActivePlainCryptoKeyOrCreate(CryptoKeyRole.backlinkKey, CryptoKeyPart.sharedSecret);
				createCryptoLink(fromPlainCryptoKey, parentBacklinkKeyPlainCryptoKey);
			}
		}

		private void createCryptoLinkFromSubdirKey(final PlainCryptoKey toPlainCryptoKey) {
			assertNotNull("toPlainCryptoKey", toPlainCryptoKey);
			final CryptreeNode cryptreeNode = getCryptreeNodeOrFail();

			if (! cryptreeNode.isDirectory())
				return;

			final PlainCryptoKey subdirKeyPlainCryptoKey = cryptreeNode.getActivePlainCryptoKeyOrCreate(CryptoKeyRole.subdirKey, CryptoKeyPart.sharedSecret);
			createCryptoLink(subdirKeyPlainCryptoKey, toPlainCryptoKey);
		}

		private void createCryptoLinkFromChildBacklinkKey(final CryptreeNode fromChild, final PlainCryptoKey toPlainCryptoKey) {
			assertNotNull("fromChild", fromChild);
			assertNotNull("toPlainCryptoKey", toPlainCryptoKey);
			final PlainCryptoKey childBacklinkKeyPlainCryptoKey = fromChild.getActivePlainCryptoKey(CryptoKeyRole.backlinkKey, CryptoKeyPart.sharedSecret);
			if (childBacklinkKeyPlainCryptoKey != null)
				createCryptoLink(childBacklinkKeyPlainCryptoKey, toPlainCryptoKey);
		}
	}

	static class DataKeyPlainCryptoKeyFactory extends PlainCryptoKeyFactory {
		@Override
		public PlainCryptoKey createPlainCryptoKey() {
			final PlainCryptoKey plainCryptoKey = createSymmetricPlainCryptoKey(CryptoKeyRole.dataKey);
			createCryptoLinkFromBacklinkKey(plainCryptoKey);
			createCryptoLinkFromFileKey(plainCryptoKey);
			return plainCryptoKey;
		}

		private void createCryptoLinkFromBacklinkKey(final PlainCryptoKey toPlainCryptoKey) {
			assertNotNull("toPlainCryptoKey", toPlainCryptoKey);
			final CryptreeNode cryptreeNode = getCryptreeNodeOrFail();

			final PlainCryptoKey backlinkKeyPlainCryptoKey;
			if (cryptreeNode.isDirectory())
				backlinkKeyPlainCryptoKey = cryptreeNode.getActivePlainCryptoKeyOrCreate(CryptoKeyRole.backlinkKey, CryptoKeyPart.sharedSecret);
			else
				backlinkKeyPlainCryptoKey = cryptreeNode.getActivePlainCryptoKey(CryptoKeyRole.backlinkKey, CryptoKeyPart.sharedSecret);

			if (backlinkKeyPlainCryptoKey != null)
				createCryptoLink(backlinkKeyPlainCryptoKey, toPlainCryptoKey);
		}

		private void createCryptoLinkFromFileKey(final PlainCryptoKey toPlainCryptoKey) {
			assertNotNull("toPlainCryptoKey", toPlainCryptoKey);
			final CryptreeNode cryptreeNode = getCryptreeNodeOrFail();

			if (cryptreeNode.isDirectory())
				return;

			final CryptreeNode parent = cryptreeNode.getParent();
			if (parent == null)
				throw new IllegalStateException("cryptreeNode is *not* a directory, but parent == null !!!");

			final PlainCryptoKey fileKeyPlainCryptoKey = parent.getActivePlainCryptoKeyOrCreate(CryptoKeyRole.fileKey, CryptoKeyPart.sharedSecret);
			createCryptoLink(fileKeyPlainCryptoKey, toPlainCryptoKey);
		}
	}

}
