package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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
import javax.jdo.annotations.Uniques;

import org.subshare.core.dto.PermissionSetDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.sign.Signature;
import org.subshare.core.sign.WriteProtected;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Uniques({
	@Unique(name="PermissionSet_cryptoRepoFile", members="cryptoRepoFile"),
})
@Queries({
	@Query(name="getPermissionSet_cryptoRepoFile", value="SELECT UNIQUE WHERE this.cryptoRepoFile == :cryptoRepoFile"),
	@Query(name="getPermissionSetsChangedAfter_localRevision", value="SELECT WHERE this.localRevision > :localRevision")
})
@FetchGroups({
	@FetchGroup(name = FetchGroupConst.PERMISSION_SET_DTO, members = {
			@Persistent(name = "cryptoRepoFile"),
			@Persistent(name = "signature")
	}),
	@FetchGroup(name = FetchGroupConst.PERMISSION_DTO, members = {
			@Persistent(name = "cryptoRepoFile")
	}),
	@FetchGroup(name = FetchGroupConst.PERMISSION_SET_INHERITANCE_DTO, members = {
			@Persistent(name = "cryptoRepoFile")
	}),
	@FetchGroup(name = FetchGroupConst.SIGNATURE, members = {
			@Persistent(name = "signature")
	})
})
public class PermissionSet extends Entity implements WriteProtected, AutoTrackLocalRevision {

	@Persistent(nullValue=NullValue.EXCEPTION)
	private CryptoRepoFile cryptoRepoFile;

	private long localRevision;

	@Persistent(mappedBy="permissionSet")
	private Set<Permission> permissions;

	@Persistent(mappedBy="permissionSet")
	private Set<PermissionSetInheritance> permissionSetInheritances;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Embedded(nullIndicatorColumn="signatureCreated")
	private SignatureImpl signature;

	public PermissionSet() { }

	public CryptoRepoFile getCryptoRepoFile() {
		return cryptoRepoFile;
	}
	public void setCryptoRepoFile(final CryptoRepoFile cryptoRepoFile) {
		if (!equal(this.cryptoRepoFile, cryptoRepoFile))
			this.cryptoRepoFile = cryptoRepoFile;
	}

	public Set<Permission> getPermissions() {
		if (permissions == null)
			permissions = new HashSet<Permission>();

		return permissions;
	}

	public boolean isPermissionsInherited(final Date timestamp) {
		assertNotNull(timestamp, "timestamp");
		for (final PermissionSetInheritance permissionSetInheritance : getPermissionSetInheritances()) {
			if (permissionSetInheritance.getValidFrom().after(timestamp))
				continue;

			if (permissionSetInheritance.getValidTo() == null || permissionSetInheritance.getValidTo().compareTo(timestamp) >= 0)
				return true;
		}
		return false;
	}

	public Set<PermissionSetInheritance> getPermissionSetInheritances() {
		if (permissionSetInheritances == null)
			permissionSetInheritances = new HashSet<PermissionSetInheritance>();

		return permissionSetInheritances;
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
		return PermissionSetDto.SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code PermissionSet} must exactly match the one in {@link PermissionSetDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			return InputStreamSource.Helper.createInputStreamSource(cryptoRepoFile.getCryptoRepoFileId()).createInputStream();
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
		// We *must* use the parent, whenever there is one, because we are otherwise not able to interrupt
		// the inheritance of permissions by another party. If we wouldn't take the parent for PermissionSetInheritance
		// (and for consistency, here in PermissionSet, too), we would interrupt the chain of trust in the moment
		// we interrupt the inheritance. Of course, this could be circumvented using the validTo time or using
		// some other complicated algorithm, but choosing the parent-CryptoRepoFile as the one controlling permissions
		// instead, is the easiest and most elegant solution.
		final CryptoRepoFile cryptoRepoFile = assertNotNull(this.cryptoRepoFile, "this.cryptoRepoFile");
		final CryptoRepoFile parentCryptoRepoFile = cryptoRepoFile.getParent();
		return assertNotNull(parentCryptoRepoFile == null ? cryptoRepoFile.getCryptoRepoFileId() : parentCryptoRepoFile.getCryptoRepoFileId(),
				"cryptoRepoFileIdControllingPermissions");
	}

	@Override
	public PermissionType getPermissionTypeRequiredForWrite() {
		return PermissionType.grant;
	}
}
