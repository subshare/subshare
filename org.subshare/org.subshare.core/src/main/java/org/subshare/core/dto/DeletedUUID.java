package org.subshare.core.dto;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Date;
import java.util.UUID;

/**
 * Reference to a deleted object, holding its ID and the deletion timestamp.
 * <p>
 * Instances of this class are used during sync-merges to find out, whether a new object
 * was added at one location and must be replicated or an object was deleted and must be deleted in
 * the 2nd location, too. The {@link #getDeleted() deleted} timestamp is solely kept to be able to
 * clean up and evict old entries after a while - maybe in the future (currently - 2015-04-30 - not yet implemented).
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class DeletedUUID {

	private UUID uuid;

	private Date deleted;

	public DeletedUUID() { }

	public DeletedUUID(UUID uuid) {
		this.uuid = assertNotNull(uuid, "uuid");
		this.deleted = new Date();
	}

	public UUID getUuid() {
		return uuid;
	}
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public Date getDeleted() {
		return deleted;
	}
	public void setDeleted(Date deleted) {
		this.deleted = deleted;
	}

}
