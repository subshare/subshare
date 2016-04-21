package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.Indices;
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

import org.subshare.core.dto.HistoFrameDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signature;
import org.subshare.core.sign.WriteProtected;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;

/**
 * A {@code HistoFrame} groups all changes made in a client repository in a certain time frame.
 * <p>
 * Whenever the client synchronises its repository upwards to the server, a new
 * {@code HistoFrame} is created. A {@code HistoFrame} is therefore a time frame starting directly
 * after the up-sync N-1 and ending with the up-sync N.
 * <p>
 * All {@code HistoFrame}s together form the entire history of a (server) repository. Old
 * versions of individual files, directories or the entire repository can be obtained by referencing
 * a certain (old) {@code HistoFrame}.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Uniques({
	@Unique(name="UK_HistoFrame_histoFrameId", members="histoFrameId"),
	@Unique(name="UK_HistoFrame_fromRepositoryId_sealed", members={"fromRepositoryId", "sealed"})
})
@Indices({
	@Index(name="HistoFrame_histoFrameId", members="histoFrameId"),
	@Index(name="HistoFrame_fromRepositoryId_sealed", members={"fromRepositoryId", "sealed"})
})
@Queries({
	@Query(name="getHistoFrame_histoFrameId", value="SELECT UNIQUE WHERE this.histoFrameId == :histoFrameId"),
	@Query(
			name="getHistoFramesChangedAfter_localRevision",
			value="SELECT WHERE this.localRevision > :localRevision"),

	@Query(
			name="getHistoFrame_fromRepositoryId_sealed",
			value="SELECT UNIQUE WHERE this.fromRepositoryId == :fromRepositoryId && this.sealed == :sealed")
})
public class HistoFrame extends Entity implements WriteProtected, AutoTrackLocalRevision, StoreCallback {

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(length=22)
	private String histoFrameId;

//	private HistoFrame previousHistoFrame;

	private long localRevision;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private String fromRepositoryId;

	private Date sealed;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Embedded(nullIndicatorColumn="signatureCreated")
	private SignatureImpl signature;

	public HistoFrame() {
	}

	public HistoFrame(final Uid histoFrameId) {
		this.histoFrameId = histoFrameId == null ? null : histoFrameId.toString();
	}

	public Uid getHistoFrameId() {
		if (histoFrameId == null)
			histoFrameId = new Uid().toString();

		return new Uid(histoFrameId);
	}

//	public HistoFrame getPreviousHistoFrame() {
//		return previousHistoFrame;
//	}
//	public void setPreviousHistoFrame(HistoFrame previousHistoFrame) {
//		this.previousHistoFrame = previousHistoFrame;
//	}

	public UUID getFromRepositoryId() {
		return fromRepositoryId == null ? null : UUID.fromString(fromRepositoryId);
	}
	public void setFromRepositoryId(UUID fromRepositoryId) {
		if (! equal(this.getFromRepositoryId(), fromRepositoryId))
			this.fromRepositoryId = fromRepositoryId == null ? null : fromRepositoryId.toString();
	}

	@Override
	public void jdoPreStore() {
		getHistoFrameId();
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
	public String getSignedDataType() {
		return HistoFrameDto.SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	public Date getSealed() {
		return sealed;
	}
	public void setSealed(Date sealed) {
		if (! equal(this.sealed, sealed))
			this.sealed = sealed;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code HistoFrame} must exactly match the one in {@link HistoFrameDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(getHistoFrameId()),

//					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
//					InputStreamSource.Helper.createInputStreamSource(
//							previousHistoFrame == null ? null : previousHistoFrame.getHistoFrameId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(getFromRepositoryId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(sealed)
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
	public Uid getCryptoRepoFileIdControllingPermissions() {
		return null;
	}

	@Override
	public PermissionType getPermissionTypeRequiredForWrite() {
		return PermissionType.write;
	}
}
