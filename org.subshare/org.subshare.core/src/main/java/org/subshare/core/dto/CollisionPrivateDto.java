package org.subshare.core.dto;

import java.io.Serializable;
import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.Uid;

@SuppressWarnings("serial") // used for LocalServer-communication, only - and they (LocalServer-server & -client) always use the very same JARs.
@XmlRootElement
public class CollisionPrivateDto implements Serializable  {

	private Uid collisionId;

	private Date resolved;

	private String comment;

	public Uid getCollisionId() {
		return collisionId;
	}
	public void setCollisionId(Uid collisionId) {
		this.collisionId = collisionId;
	}

	public Date getResolved() {
		return resolved;
	}

	public void setResolved(Date resolved) {
		this.resolved = resolved;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}
}
