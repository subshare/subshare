package org.subshare.local.dto;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.dto.CollisionDto;
import org.subshare.local.persistence.Collision;
import org.subshare.local.persistence.CollisionDao;
import org.subshare.local.persistence.HistoCryptoRepoFileDao;

import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public class CollisionDtoConverter {

	private final LocalRepoTransaction transaction;

	public static CollisionDtoConverter create(final LocalRepoTransaction transaction) {
		return createObject(CollisionDtoConverter.class, transaction);
	}

	protected CollisionDtoConverter(final LocalRepoTransaction transaction) {
		this.transaction = assertNotNull("transaction", transaction);
	}

	public CollisionDto toCollisionDto(final Collision collision) {
		assertNotNull("collision", collision);
		CollisionDto result = new CollisionDto();
		result.setCollisionId(collision.getCollisionId());
		result.setHistoCryptoRepoFileId1(collision.getHistoCryptoRepoFile1().getHistoCryptoRepoFileId());
		result.setHistoCryptoRepoFileId2(collision.getHistoCryptoRepoFile2().getHistoCryptoRepoFileId());
		result.setSignature(collision.getSignature());
		return result;
	}

	public Collision putCollisionDto(final CollisionDto collisionDto) {
		assertNotNull("collisionDto", collisionDto);

		final CollisionDao dao = transaction.getDao(CollisionDao.class);
		Collision result = dao.getCollision(collisionDto.getCollisionId());
		if (result == null)
			result = new Collision(collisionDto.getCollisionId());

		final HistoCryptoRepoFileDao hcrfDao = transaction.getDao(HistoCryptoRepoFileDao.class);

		result.setHistoCryptoRepoFile1(hcrfDao.getHistoCryptoRepoFileOrFail(collisionDto.getHistoCryptoRepoFileId1()));
		result.setHistoCryptoRepoFile2(hcrfDao.getHistoCryptoRepoFileOrFail(collisionDto.getHistoCryptoRepoFileId2()));
		result.setSignature(collisionDto.getSignature());
		result = dao.makePersistent(result);
		return result;
	}
}
