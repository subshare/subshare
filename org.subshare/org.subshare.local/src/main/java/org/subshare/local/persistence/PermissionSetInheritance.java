package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.DateUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static java.util.Objects.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.FetchGroup;
import javax.jdo.annotations.FetchGroups;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;

import org.subshare.core.dto.PermissionSetInheritanceDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signature;
import org.subshare.core.sign.WriteProtected;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Unique(name="PermissionSetInheritance_permissionSetInheritanceId", members="permissionSetInheritanceId")
@Queries({
	@Query(name="getPermissionSetInheritance_permissionSetInheritanceId", value="SELECT UNIQUE WHERE this.permissionSetInheritanceId == :permissionSetInheritanceId"),
	@Query(name="getPermissionSetInheritancesChangedAfter_localRevision", value="SELECT WHERE this.localRevision > :localRevision")
})
@FetchGroups({
	@FetchGroup(name = FetchGroupConst.PERMISSION_SET_INHERITANCE_DTO, members = {
			@Persistent(name = "permissionSet"),
			@Persistent(name = "signature")
	}),
	@FetchGroup(name = FetchGroupConst.SIGNATURE, members = {
			@Persistent(name = "signature")
	})
})
public class PermissionSetInheritance extends Entity implements WriteProtected, AutoTrackLocalRevision {

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(length=22)
	private String permissionSetInheritanceId;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private PermissionSet permissionSet;

	private long localRevision;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private Date validFrom = now();

	private Date revoked;

	private Date validTo;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Embedded(nullIndicatorColumn="signatureCreated")
	private SignatureImpl signature;

	public PermissionSetInheritance() { }

	public PermissionSetInheritance(final Uid permissionSetInheritanceId) {
		this.permissionSetInheritanceId = permissionSetInheritanceId == null ? null : permissionSetInheritanceId.toString();
	}

	public Uid getPermissionSetInheritanceId() {
		if (permissionSetInheritanceId == null)
			permissionSetInheritanceId = new Uid().toString();

		return new Uid(permissionSetInheritanceId);
	}

	public PermissionSet getPermissionSet() {
		return permissionSet;
	}
	public void setPermissionSet(final PermissionSet permissionSet) {
		if (! equal(this.permissionSet, permissionSet))
			this.permissionSet = permissionSet;
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

	public Date getRevoked() {
		return revoked;
	}
	public void setRevoked(final Date revoked) {
		if (! equal(this.revoked, revoked))
			this.revoked = revoked;
	}

	public Date getValidFrom() {
		return validFrom;
	}
	public void setValidFrom(final Date validFrom) {
		if (! equal(this.validFrom, validFrom))
			this.validFrom = validFrom;
	}

	public Date getValidTo() {
		return validTo;
	}
	public void setValidTo(final Date validTo) {
		if (! equal(this.validTo, validTo))
			this.validTo = validTo;
	}

	@Override
	public String getSignedDataType() {
		return PermissionSetInheritanceDto.SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code PermissionSetInheritance} must exactly match the one in {@link PermissionSetInheritanceDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(getPermissionSetInheritanceId()),
//					localRevision
					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(permissionSet.getCryptoRepoFile().getCryptoRepoFileId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(validFrom),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(revoked),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(validTo)
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
		requireNonNull(permissionSet, "permissionSet");
		return requireNonNull(permissionSet.getCryptoRepoFileIdControllingPermissions(),
				"permissionSet.cryptoRepoFileIdControllingPermissions");
	}

	@Override
	public PermissionType getPermissionTypeRequiredForWrite() {
		return PermissionType.grant;
	}
}
