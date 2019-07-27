package org.subshare.core.dto;

import static java.util.Objects.*;

import java.util.Date;

import co.codewizards.cloudstore.core.Uid;

/**
 * Reference to a deleted object, holding its ID and the deletion timestamp.
 * <p>
 * Instances of this class are used during sync-merges to find out, whether a new object
 * was added at one location and must be replicated or an object was deleted and must be deleted in
 * the 2nd location, too. The {@link #getDeleted() deleted} timestamp is solely kept to be able to
 * clean up and evict old entries after a while - maybe in the future (currently - 2015-04-30 - not yet implemented).
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class DeletedUid {

	private Uid uid;

	private Date deleted;

	public DeletedUid() { }

	public DeletedUid(Uid uid) {
		this.uid = requireNonNull(uid, "uid");
		this.deleted = new Date();
	}

	public Uid getUid() {
		return uid;
	}
	public void setUid(Uid uid) {
		this.uid = uid;
	}

	public Date getDeleted() {
		return deleted;
	}
	public void setDeleted(Date deleted) {
		this.deleted = deleted;
	}

}
