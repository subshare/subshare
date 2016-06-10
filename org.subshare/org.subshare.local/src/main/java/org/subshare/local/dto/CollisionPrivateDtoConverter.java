package org.subshare.local.dto;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.dto.CollisionPrivateDto;
import org.subshare.local.persistence.Collision;
import org.subshare.local.persistence.CollisionDao;
import org.subshare.local.persistence.CollisionPrivate;
import org.subshare.local.persistence.CollisionPrivateDao;

import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public class CollisionPrivateDtoConverter {

	private final LocalRepoTransaction transaction;

	public static CollisionPrivateDtoConverter create(final LocalRepoTransaction transaction) {
		return createObject(CollisionPrivateDtoConverter.class, transaction);
	}

	protected CollisionPrivateDtoConverter(final LocalRepoTransaction transaction) {
		this.transaction = assertNotNull("transaction", transaction);
	}

	public CollisionPrivateDto toCollisionPrivateDto(final CollisionPrivate collisionPrivate) {
		assertNotNull("collisionPrivate", collisionPrivate);
		CollisionPrivateDto result = new CollisionPrivateDto();
		result.setCollisionId(collisionPrivate.getCollision().getCollisionId());
		result.setComment(collisionPrivate.getComment());
		result.setResolved(collisionPrivate.getResolved());
		return result;
	}

	public CollisionPrivate putCollisionPrivateDto(final CollisionPrivateDto collisionPrivateDto) {
		assertNotNull("collisionPrivateDto", collisionPrivateDto);
		final CollisionDao cDao = transaction.getDao(CollisionDao.class);
		final Collision collision = cDao.getCollisionOrFail(collisionPrivateDto.getCollisionId());
		return putCollisionPrivateDto(collision, collisionPrivateDto);
	}

	public CollisionPrivate putCollisionPrivateDto(final Collision collision, final CollisionPrivateDto collisionPrivateDto) {
		assertNotNull("collision", collision);
		assertNotNull("collisionPrivateDto", collisionPrivateDto);

		final CollisionPrivateDao cpDao = transaction.getDao(CollisionPrivateDao.class);

		CollisionPrivate result = cpDao.getCollisionPrivate(collision);
		if (result == null) {
			result = new CollisionPrivate();
			result.setCollision(collision);
		}

		result.setComment(collisionPrivateDto.getComment());
		result.setResolved(collisionPrivateDto.getResolved());
		result = cpDao.makePersistent(result);
		return result;
	}
}
