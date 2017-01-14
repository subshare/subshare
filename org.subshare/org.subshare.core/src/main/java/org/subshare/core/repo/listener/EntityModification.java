package org.subshare.core.repo.listener;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.Serializable;

public class EntityModification implements Serializable {
	private static final long serialVersionUID = 1L;

	private final EntityId entityId;

	private final EntityModificationType type;

	public EntityModification(final EntityId entityId, final EntityModificationType type) {
		this.entityId = assertNotNull(entityId, "entityId");
		this.type = assertNotNull(type, "type");
	}

	public EntityId getEntityId() {
		return entityId;
	}

	public EntityModificationType getType() {
		return type;
	}
}
