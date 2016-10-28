package org.subshare.local.dto;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.dto.CollisionDto;
import org.subshare.local.persistence.Collision;
import org.subshare.local.persistence.CollisionDao;
import org.subshare.local.persistence.CryptoKey;
import org.subshare.local.persistence.CryptoKeyDao;
import org.subshare.local.persistence.HistoCryptoRepoFile;
import org.subshare.local.persistence.HistoCryptoRepoFileDao;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public class CollisionDtoConverter {
	private static final Logger logger = LoggerFactory.getLogger(CollisionDtoConverter.class);

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
		result.setHistoCryptoRepoFileId2(
				collision.getHistoCryptoRepoFile2() == null ? null : collision.getHistoCryptoRepoFile2().getHistoCryptoRepoFileId());
		result.setDuplicateCryptoRepoFileId(collision.getDuplicateCryptoRepoFileId());
		result.setCryptoKeyId(assertNotNull("collision.cryptoKey", collision.getCryptoKey()).getCryptoKeyId());
		result.setCollisionPrivateDtoData(collision.getCollisionPrivateDtoData());
		result.setSignature(collision.getSignature());
		return result;
	}

	public Collision putCollisionDto(final CollisionDto collisionDto) {
		assertNotNull("collisionDto", collisionDto);

		final CollisionDao cDao = transaction.getDao(CollisionDao.class);
		final HistoCryptoRepoFileDao hcrfDao = transaction.getDao(HistoCryptoRepoFileDao.class);

		final HistoCryptoRepoFile histoCryptoRepoFile1 = hcrfDao.getHistoCryptoRepoFileOrFail(collisionDto.getHistoCryptoRepoFileId1());
		final Uid duplicateCryptoRepoFileId = collisionDto.getDuplicateCryptoRepoFileId();

		final CryptoKeyDao cryptoKeyDao = transaction.getDao(CryptoKeyDao.class);

		Collision result = cDao.getCollision(collisionDto.getCollisionId());
		if (result == null) {
//			if (duplicateCryptoRepoFileId != null) {
//				result = cDao.getCollisionWithDuplicateCryptoRepoFileId(histoCryptoRepoFile1, duplicateCryptoRepoFileId);
//				if (result != null) {
//					logger.warn("putCollisionDto: Discarded duplicate Collision for same combination of histoCryptoRepoFile1 + duplicateCryptoRepoFileId! histoCryptoRepoFileId1={} duplicateCryptoRepoFileId={} keptCollisionId={}",
//							collisionDto.getHistoCryptoRepoFileId1(), duplicateCryptoRepoFileId);
//					result.setLocalRevision(transaction.getLocalRevision()); // make sure it's re-synced!
//					return result;
//				}
//			}
			result = new Collision(collisionDto.getCollisionId());
		}

		result.setHistoCryptoRepoFile1(histoCryptoRepoFile1);
		result.setHistoCryptoRepoFile2(
				collisionDto.getHistoCryptoRepoFileId2() == null ? null : hcrfDao.getHistoCryptoRepoFileOrFail(collisionDto.getHistoCryptoRepoFileId2()));
		result.setDuplicateCryptoRepoFileId(duplicateCryptoRepoFileId);

		final CryptoKey cryptoKey = cryptoKeyDao.getCryptoKeyOrFail(collisionDto.getCryptoKeyId());
		result.setCryptoKey(cryptoKey);

		result.setCollisionPrivateDtoData(collisionDto.getCollisionPrivateDtoData());
		result.setSignature(collisionDto.getSignature());
		result = cDao.makePersistent(result);
		return result;
	}
}
