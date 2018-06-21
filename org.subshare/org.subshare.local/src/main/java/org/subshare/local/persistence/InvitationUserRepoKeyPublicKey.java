package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.FetchGroup;
import javax.jdo.annotations.FetchGroups;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

import org.subshare.core.dto.InvitationUserRepoKeyPublicKeyDto;
import org.subshare.core.dto.UserRepoKeyDto;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signable;
import org.subshare.core.sign.Signature;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyImpl;
import org.subshare.crypto.CryptoRegistry;

import co.codewizards.cloudstore.core.Uid;

@PersistenceCapable
@Discriminator(strategy=DiscriminatorStrategy.VALUE_MAP, value="InvitationUserRepoKeyPublicKey")
@Inheritance(strategy=InheritanceStrategy.SUPERCLASS_TABLE)
@FetchGroups({
	@FetchGroup(name = FetchGroupConst.USER_REPO_KEY_PUBLIC_KEY_DTO, members = {
			@Persistent(name = "signature")
	})
})
public class InvitationUserRepoKeyPublicKey extends UserRepoKeyPublicKey implements Signable {

	private Date validTo;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Embedded(nullIndicatorColumn="signatureCreated")
	private SignatureImpl signature;

	public InvitationUserRepoKeyPublicKey() {
	}
	public InvitationUserRepoKeyPublicKey(UserRepoKey.PublicKeyWithSignature publicKey) {
		super(publicKey);
		setValidTo(publicKey.getValidTo());
		setSignature(publicKey.getSignature());
	}
	public InvitationUserRepoKeyPublicKey(Uid userRepoKeyId) {
		super(userRepoKeyId);
	}

	public Date getValidTo() {
		return validTo;
	}
	public void setValidTo(Date validTo) {
		if (! equal(this.validTo, validTo))
			this.validTo = validTo;
	}

	@Override
	protected UserRepoKey.PublicKey createPublicKey() {
		try {
			return new UserRepoKeyImpl.PublicKeyImpl(
					getUserRepoKeyId(), getServerRepositoryId(),
					CryptoRegistry.getInstance().decodePublicKey(getPublicKeyData()),
					getValidTo(), true);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getSignedDataType() {
		return UserRepoKeyDto.PUBLIC_KEY_SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code InvitationUserRepoKeyPublicKey} must exactly match the one
	 * in {@code UserRepoKey.PublicKeyWithSignature} and the one in {@link InvitationUserRepoKeyPublicKeyDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(getUserRepoKeyId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(getServerRepositoryId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(getPublicKeyData())
			);
		} catch (final IOException x) {
			throw new RuntimeException(x);
		}
	}

	@Override
	public Signature getSignature() {
		return signature;
	}
	@Override
	public void setSignature(final Signature signature) {
		if (!equal(this.signature, signature))
			this.signature = SignatureImpl.copy(signature);
	}
}
