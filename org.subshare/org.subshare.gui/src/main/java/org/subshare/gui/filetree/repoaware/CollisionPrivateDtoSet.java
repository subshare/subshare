package org.subshare.gui.filetree.repoaware;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.subshare.core.dto.CollisionPrivateDto;

import co.codewizards.cloudstore.core.Uid;

public class CollisionPrivateDtoSet {

	private final Collection<CollisionPrivateDto> allCollisionPrivateDtos;
	private final Collection<CollisionPrivateDto> directCollisionPrivateDtos;
	private Collection<CollisionPrivateDto> childCollisionPrivateDtos;

	public CollisionPrivateDtoSet(final Collection<CollisionPrivateDto> allCollisionPrivateDtos, final Collection<CollisionPrivateDto> directCollisionPrivateDtos) {
		this.allCollisionPrivateDtos = assertNotNull("allCollisionPrivateDtos", allCollisionPrivateDtos);
		this.directCollisionPrivateDtos = assertNotNull("directCollisionPrivateDtos", directCollisionPrivateDtos);
	}

	public Collection<CollisionPrivateDto> getAllCollisionPrivateDtos() {
		return allCollisionPrivateDtos;
	}

	public Collection<CollisionPrivateDto> getDirectCollisionPrivateDtos() {
		return directCollisionPrivateDtos;
	}

	public Collection<CollisionPrivateDto> getChildCollisionPrivateDtos() {
		if (childCollisionPrivateDtos == null) {
			final Map<Uid, CollisionPrivateDto> collisionId2CollisionPrivateDto = new HashMap<>();
			for (final CollisionPrivateDto collisionPrivateDto : allCollisionPrivateDtos)
				collisionId2CollisionPrivateDto.put(assertNotNull("collisionPrivateDto.collisionId", collisionPrivateDto.getCollisionId()), collisionPrivateDto);

			for (final CollisionPrivateDto collisionPrivateDto : directCollisionPrivateDtos)
				collisionId2CollisionPrivateDto.remove(assertNotNull("collisionPrivateDto.collisionId", collisionPrivateDto.getCollisionId()));

			childCollisionPrivateDtos = Collections.unmodifiableList(new ArrayList<>(collisionId2CollisionPrivateDto.values()));
		}
		return childCollisionPrivateDtos;
	}
}
