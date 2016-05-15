package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;
import javax.jdo.annotations.Uniques;
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
@Uniques({
	@Unique(name = "Collision_collisionId", members = "collisionId"),
	@Unique(
			name = "Collision_histoCryptoRepoFile1_histoCryptoRepoFile2_duplicateCryptoRepoFileId",
			members = {"histoCryptoRepoFile1", "histoCryptoRepoFile2", "duplicateCryptoRepoFileId"})
})
@Queries({
	@Query(name = "getCollision_collisionId", value = "SELECT UNIQUE WHERE this.collisionId == :collisionId"),
	@Query(
			name="getCollisionsChangedAfter_localRevision",
			value="SELECT WHERE this.localRevision > :localRevision"),

	@Query(
			name = "getCollisions_histoCryptoRepoFile1_histoCryptoRepoFile2",
			value = "SELECT UNIQUE WHERE"
					+ "  (this.histoCryptoRepoFile1 == :histoCryptoRepoFile1 && this.histoCryptoRepoFile2 == :histoCryptoRepoFile2 && this.duplicateCryptoRepoFileId == null)"
					+ "   || (this.histoCryptoRepoFile1 == :histoCryptoRepoFile2 && this.histoCryptoRepoFile2 == :histoCryptoRepoFile1 && this.duplicateCryptoRepoFileId == null)"),

	@Query(
			name = "getCollisions_histoCryptoRepoFile1_duplicateCryptoRepoFileId",
			value = "SELECT UNIQUE WHERE"
					+ "  this.histoCryptoRepoFile1 == :histoCryptoRepoFile1 && this.histoCryptoRepoFile2 == null && this.duplicateCryptoRepoFileId == :duplicateCryptoRepoFileId")

})
public class Collision extends Entity implements WriteProtected, AutoTrackLocalRevision, StoreCallback {

	@Persistent(nullValue = NullValue.EXCEPTION)
	@Column(length = 22)
	private String collisionId;

	private long localRevision;

	@Persistent(nullValue = NullValue.EXCEPTION)
	private HistoCryptoRepoFile histoCryptoRepoFile1;

//	@Persistent(nullValue = NullValue.EXCEPTION)
	private HistoCryptoRepoFile histoCryptoRepoFile2; // null, if duplicateCryptoRepoFileId present!

	@Column(length = 22)
	private String duplicateCryptoRepoFileId;

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
			collisionId = calculateCollisionId().toString();

		return new Uid(collisionId);
	}

	public HistoCryptoRepoFile getHistoCryptoRepoFile1() {
		return histoCryptoRepoFile1;
	}
	public void setHistoCryptoRepoFile1(HistoCryptoRepoFile histoCryptoRepoFile1) {
		if (! equal(this.histoCryptoRepoFile1, histoCryptoRepoFile1))
			this.histoCryptoRepoFile1 = histoCryptoRepoFile1;
	}
	public HistoCryptoRepoFile getHistoCryptoRepoFile2() {
		return histoCryptoRepoFile2;
	}
	public void setHistoCryptoRepoFile2(HistoCryptoRepoFile histoCryptoRepoFile2) {
		if (! equal(this.histoCryptoRepoFile2, histoCryptoRepoFile2))
			this.histoCryptoRepoFile2 = histoCryptoRepoFile2;
	}

	public Uid getDuplicateCryptoRepoFileId() {
		return duplicateCryptoRepoFileId == null ? null : new Uid(duplicateCryptoRepoFileId);
	}
	public void setDuplicateCryptoRepoFileId(final Uid duplicateCryptoRepoFileId) {
		if (! equal(this.getDuplicateCryptoRepoFileId(), duplicateCryptoRepoFileId))
			this.duplicateCryptoRepoFileId = duplicateCryptoRepoFileId == null ? null : duplicateCryptoRepoFileId.toString();
	}

	public Date getResolved() {
		return resolved;
	}
	public void setResolved(Date resolved) {
		if (! equal(this.resolved, resolved))
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
					InputStreamSource.Helper.createInputStreamSource(getCollisionId()),
//					localRevision
					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(histoCryptoRepoFile1 == null ? null : histoCryptoRepoFile1.getHistoCryptoRepoFileId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(histoCryptoRepoFile2 == null ? null : histoCryptoRepoFile2.getHistoCryptoRepoFileId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(getDuplicateCryptoRepoFileId()),

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
		if (! equal(this.signature, signature))
			this.signature = SignatureImpl.copy(signature);
	}

	@Override
	public void jdoPreStore() {
		final Uid collisionId = getCollisionId();
		final CryptoRepoFile cryptoRepoFile1 = assertNotNull("histoCryptoRepoFile1", histoCryptoRepoFile1).getCryptoRepoFile();
		final CryptoRepoFile cryptoRepoFile2 = histoCryptoRepoFile2 == null ? null : histoCryptoRepoFile2.getCryptoRepoFile();
		final CryptoRepoFile parent1 = assertNotNull("cryptoRepoFile1", cryptoRepoFile1).getParent();
		final CryptoRepoFile parent2 = cryptoRepoFile2 == null ? null : cryptoRepoFile2.getParent();

		if (histoCryptoRepoFile2 == null && duplicateCryptoRepoFileId == null)
			throw new IllegalStateException("Both histoCryptoRepoFile2 and duplicateCryptoRepoFileId are null! One of them must be non-null!");

		if (histoCryptoRepoFile2 != null && duplicateCryptoRepoFileId != null)
			throw new IllegalStateException("Both histoCryptoRepoFile2 and duplicateCryptoRepoFileId are non-null! One of them must be null!");

		if (histoCryptoRepoFile2 != null && cryptoRepoFile2 == null)
			throw new IllegalStateException("histoCryptoRepoFile2 is not null, but cryptoRepoFile2 is null!");

		if (duplicateCryptoRepoFileId == null && ! equal(parent1, parent2))
			throw new IllegalStateException(String.format(
					"histoCryptoRepoFile1.cryptoRepoFile.parent != histoCryptoRepoFile2.cryptoRepoFile.parent :: histoCryptoRepoFile1=%s histoCryptoRepoFile2=%s parent1=%s parent2=%s",
					histoCryptoRepoFile1, histoCryptoRepoFile2, parent1, parent2));

		if (! collisionId.equals(calculateCollisionId()))
			throw new IllegalStateException("collisionId != calculateCollisionId()");

		final PersistenceManager pm = JDOHelper.getPersistenceManager(this);
		final CollisionDao collisionDao = new CollisionDao().persistenceManager(pm);
		Collision c = collisionDao.getCollision(histoCryptoRepoFile1, histoCryptoRepoFile2, getDuplicateCryptoRepoFileId());
		if (c != null && c != this)
			throw new IllegalStateException(String.format("There is already another Collision between these two HistoCryptoRepoFiles/duplicateCryptoRepoFileId: %s, %s, %s",
					histoCryptoRepoFile1, histoCryptoRepoFile2, duplicateCryptoRepoFileId));
	}

	private Uid calculateCollisionId() {
		byte[] bytes = assertNotNull("histoCryptoRepoFile1", histoCryptoRepoFile1).getHistoCryptoRepoFileId().toBytes();
		xorIntoBytes(bytes, histoCryptoRepoFile2 == null ? null : histoCryptoRepoFile2.getHistoCryptoRepoFileId());
		xorIntoBytes(bytes, getDuplicateCryptoRepoFileId());
		return new Uid(bytes);
	}

	protected static void xorIntoBytes(final byte[] bytes, Uid uid) {
		if (uid != null) {
			final byte[] bytes2 = uid.toBytes();
			if (bytes.length != bytes2.length)
				throw new IllegalArgumentException("bytes.length != bytes2.length");

			for (int i = 0; i < bytes.length; i++) {
				bytes[i] = (byte) ((bytes[i] & 0xff) ^ (bytes2[i] & 0xff));
			}
		}
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
		final CryptoRepoFile cryptoRepoFile2 = histoCryptoRepoFile2 == null ? cryptoRepoFile1 : histoCryptoRepoFile2.getCryptoRepoFile();
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
