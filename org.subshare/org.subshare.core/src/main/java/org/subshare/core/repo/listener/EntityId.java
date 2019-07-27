package org.subshare.core.repo.listener;

import static co.codewizards.cloudstore.core.util.Util.*;
import static java.util.Objects.*;

import java.io.Serializable;

public class EntityId implements Serializable {
	private static final long serialVersionUID = 1L;

	private final String className;

	private final long id;

	public EntityId(final Class<?> clazz, final long id) {
		this(requireNonNull(clazz, "clazz").getName(), id);
	}

	public EntityId(final String className, final long id) {
		this.className = requireNonNull(className, "className");
		this.id = id;
	}

	public String getClassName() {
		return className;
	}

	public long getId() {
		return id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((className == null) ? 0 : className.hashCode());
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;

		if (obj == null)
			return false;

		if (getClass() != obj.getClass())
			return false;

		final EntityId other = (EntityId) obj;
		return equal(this.id, other.id)
				&& equal(this.className, other.className);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + '[' + className + ", " + id + ']';
	}
}
