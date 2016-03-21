package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Unique;
import javax.jdo.listener.StoreCallback;

import org.subshare.core.dto.CollisionDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signature;
import org.subshare.core.sign.WriteProtected;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;

@PersistenceCapable
@Inheritance(strategy = InheritanceStrategy.NEW_TABLE)
@Unique(name = "Collision_collisionId", members = "collisionId")
public class Collision extends Entity implements WriteProtected, AutoTrackLocalRevision, StoreCallback {

	@Persistent(nullValue = NullValue.EXCEPTION)
	@Column(length = 22)
	private String collisionId;

	private long localRevision;

	@Persistent(nullValue = NullValue.EXCEPTION)
	private HistoCryptoRepoFile histoCryptoRepoFile1;

	@Persistent(nullValue = NullValue.EXCEPTION)
	private HistoCryptoRepoFile histoCryptoRepoFile2;

	private Date resolved;

	@Persistent(nullValue = NullValue.EXCEPTION)
	@Embedded(nullIndicatorColumn = "signatureCreated")
	private SignatureImpl signature;

	public Collision() {
	}

	public Collision(final Uid collisionId) {
		this.collisionId = collisionId == null ? null : collisionId.toString();
	}

	public Uid getCollisionId() {
		if (collisionId == null)
			collisionId = new Uid().toString();

		return new Uid(collisionId);
	}

	public HistoCryptoRepoFile getHistoCryptoRepoFile1() {
		return histoCryptoRepoFile1;
	}
	public void setHistoCryptoRepoFile1(HistoCryptoRepoFile histoCryptoRepoFile1) {
		this.histoCryptoRepoFile1 = histoCryptoRepoFile1;
	}
	public HistoCryptoRepoFile getHistoCryptoRepoFile2() {
		return histoCryptoRepoFile2;
	}
	public void setHistoCryptoRepoFile2(HistoCryptoRepoFile histoCryptoRepoFile2) {
		this.histoCryptoRepoFile2 = histoCryptoRepoFile2;
	}

	public Date getResolved() {
		return resolved;
	}
	public void setResolved(Date resolved) {
		this.resolved = resolved;
	}

	@Override
	public String getSignedDataType() {
		return CollisionDto.SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code Collision} must exactly match the one in {@code CollisionDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(collisionId),
//					localRevision
					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(histoCryptoRepoFile1 == null ? null : histoCryptoRepoFile1.getHistoCryptoRepoFileId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(histoCryptoRepoFile2 == null ? null : histoCryptoRepoFile2.getHistoCryptoRepoFileId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(resolved)
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

	@Override
	public void jdoPreStore() {
		final CryptoRepoFile cryptoRepoFile1 = assertNotNull("histoCryptoRepoFile1", histoCryptoRepoFile1).getCryptoRepoFile();
		final CryptoRepoFile cryptoRepoFile2 = assertNotNull("histoCryptoRepoFile2", histoCryptoRepoFile2).getCryptoRepoFile();
		final CryptoRepoFile parent1 = assertNotNull("cryptoRepoFile1", cryptoRepoFile1).getParent();
		final CryptoRepoFile parent2 = assertNotNull("cryptoRepoFile2", cryptoRepoFile2).getParent();
		if (! equal(parent1, parent2))
			throw new IllegalStateException(String.format(
					"histoCryptoRepoFile1.cryptoRepoFile.parent != histoCryptoRepoFile2.cryptoRepoFile.parent :: histoCryptoRepoFile1=%s histoCryptoRepoFile2=%s parent1=%s parent2=%s",
					histoCryptoRepoFile1, histoCryptoRepoFile2, parent1, parent2));
	}

	@Override
	public long getLocalRevision() {
		return localRevision;
	}
	@Override
	public void setLocalRevision(final long localRevision) {
		if (! equal(this.localRevision, localRevision))
			this.localRevision = localRevision;
	}

	@Override
	public Uid getCryptoRepoFileIdControllingPermissions() {
		final CryptoRepoFile cryptoRepoFile1 = assertNotNull("histoCryptoRepoFile1", histoCryptoRepoFile1).getCryptoRepoFile();
		final CryptoRepoFile cryptoRepoFile2 = assertNotNull("histoCryptoRepoFile2", histoCryptoRepoFile2).getCryptoRepoFile();
		assertNotNull("cryptoRepoFile1", cryptoRepoFile1);
		assertNotNull("cryptoRepoFile2", cryptoRepoFile2);

		// In most (nearly all) cases, the cryptoRepoFile1 and cryptoRepoFile2 are equal, because
		// collisions happen with the very same file/dir/symlink. There is, however, a very rare
		// situation that two *new* files (or dirs) are enlisted simultaneously and thus lead to
		// two different CryptoRepoFile objects. In this very rare case, we currently fall back
		// to null, i.e. requiring solely write-permission somewhere (not specificly here).
		if (cryptoRepoFile1.equals(cryptoRepoFile2))
			return cryptoRepoFile1.getCryptoRepoFileId();
		else
			return null;
	}

	@Override
	public PermissionType getPermissionTypeRequiredForWrite() {
		return PermissionType.write;
	}
}
