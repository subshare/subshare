package org.subshare.gui.filetree.repoaware;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.subshare.core.dto.CollisionDto;

import co.codewizards.cloudstore.core.dto.Uid;

public class CollisionDtoSet {

	private final Collection<CollisionDto> allCollisionDtos;
	private final Collection<CollisionDto> directCollisionDtos;
	private Collection<CollisionDto> childCollisionDtos;

	public CollisionDtoSet(final Collection<CollisionDto> allCollisionDtos, final Collection<CollisionDto> directCollisionDtos) {
		this.allCollisionDtos = assertNotNull("allCollisionDtos", allCollisionDtos);
		this.directCollisionDtos = assertNotNull("directCollisionDtos", directCollisionDtos);
	}

	public Collection<CollisionDto> getAllCollisionDtos() {
		return allCollisionDtos;
	}

	public Collection<CollisionDto> getDirectCollisionDtos() {
		return directCollisionDtos;
	}

	public Collection<CollisionDto> getChildCollisionDtos() {
		if (childCollisionDtos == null) {
			final Map<Uid, CollisionDto> collisionId2CollisionDto = new HashMap<>();
			for (final CollisionDto collisionDto : allCollisionDtos)
				collisionId2CollisionDto.put(assertNotNull("collisionDto.collisionId", collisionDto.getCollisionId()), collisionDto);

			for (final CollisionDto collisionDto : directCollisionDtos)
				collisionId2CollisionDto.remove(assertNotNull("collisionDto.collisionId", collisionDto.getCollisionId()));

			childCollisionDtos = Collections.unmodifiableList(new ArrayList<>(collisionId2CollisionDto.values()));
		}
		return childCollisionDtos;
	}
}
