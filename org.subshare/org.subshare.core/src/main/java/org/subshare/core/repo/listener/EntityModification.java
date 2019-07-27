package org.subshare.core.repo.listener;

import static java.util.Objects.*;

import java.io.Serializable;

public class EntityModification implements Serializable {
	private static final long serialVersionUID = 1L;

	private final EntityId entityId;

	private final EntityModificationType type;

	public EntityModification(final EntityId entityId, final EntityModificationType type) {
		this.entityId = requireNonNull(entityId, "entityId");
		this.type = requireNonNull(type, "type");
	}

	public EntityId getEntityId() {
		return entityId;
	}

	public EntityModificationType getType() {
		return type;
	}
}
