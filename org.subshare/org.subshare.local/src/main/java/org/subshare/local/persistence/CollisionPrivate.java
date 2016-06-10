package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.Date;

import javax.jdo.annotations.Column;
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

import co.codewizards.cloudstore.local.persistence.Entity;

@PersistenceCapable
@Inheritance(strategy = InheritanceStrategy.NEW_TABLE)
@Uniques({
	@Unique(name = "UK_CollisionPrivate_collision", members = "collision")
})
@Indices({
	@Index(name = "CollisionPrivate_collision", members = "collision")
})
@Queries({
	@Query(name = "getCollisionPrivate_collision", value = "SELECT UNIQUE WHERE this.collision == :collision")
})
public class CollisionPrivate extends Entity implements StoreCallback {

	@Persistent(nullValue=NullValue.EXCEPTION)
	private Collision collision;

	private Date resolved;

	@Column(jdbcType="CLOB")
	private String comment;

//	private byte[] collisionPrivateDtoData;

	public CollisionPrivate() {
	}

	public Collision getCollision() {
		return collision;
	}
	public void setCollision(Collision collision) {
		if (! equal(this.collision, collision))
			this.collision = collision;
	}

//	public CollisionPrivateDto getCollisionPrivateDto() {
//		if (collisionPrivateDtoData == null)
//			return null;
//		else {
//			final CollisionPrivateDtoIo io = new CollisionPrivateDtoIo();
//			CollisionPrivateDto dto = io.deserializeWithGz(new ByteArrayInputStream(collisionPrivateDtoData));
//			return dto;
//		}
//	}
//
//	public void setCollisionPrivateDto(final CollisionPrivateDto dto) {
//		setResolved(dto == null ? null : dto.getResolved());
//
//		if (dto == null)
//			collisionPrivateDtoData = null;
//		else {
//			final CollisionPrivateDtoIo io = new CollisionPrivateDtoIo();
//			final ByteArrayOutputStream out = new ByteArrayOutputStream();
//			io.serializeWithGz(dto, out);
//			collisionPrivateDtoData = out.toByteArray();
//		}
//	}

	public Date getResolved() {
		return resolved;
	}

	public void setResolved(Date resolved) {
		if (! equal(this.resolved, resolved))
			 this.resolved = resolved;
	}

	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		if (! equal(this.comment, comment))
			this.comment = comment;
	}

	@Override
	public void jdoPreStore() {
	}
}
