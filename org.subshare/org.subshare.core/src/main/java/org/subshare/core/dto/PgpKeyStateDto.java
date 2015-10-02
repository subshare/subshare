package org.subshare.core.dto;

import java.util.Date;

import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpOwnerTrust;

public class PgpKeyStateDto {
	private PgpKeyId pgpKeyId;
	private Date created;
	private Date changed;
//	private Date deleted;
//	private Date readded;
	private boolean disabled;
	private PgpOwnerTrust ownerTrust;

	public PgpKeyId getPgpKeyId() {
		return pgpKeyId;
	}
	public void setPgpKeyId(PgpKeyId pgpKeyId) {
		this.pgpKeyId = pgpKeyId;
	}

	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}
	public Date getChanged() {
		return changed;
	}
	public void setChanged(Date changed) {
		this.changed = changed;
	}

	public boolean isDisabled() {
		return disabled;
	}
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public PgpOwnerTrust getOwnerTrust() {
		return ownerTrust;
	}
	public void setOwnerTrust(PgpOwnerTrust ownerTrust) {
		this.ownerTrust = ownerTrust;
	}

//	public Date getDeleted() {
//		return deleted;
//	}
//	public void setDeleted(Date deleted) {
//		this.deleted = deleted;
//	}
//	public Date getReadded() {
//		return readded;
//	}
//	public void setReadded(Date readded) {
//		this.readded = readded;
//	}
}
