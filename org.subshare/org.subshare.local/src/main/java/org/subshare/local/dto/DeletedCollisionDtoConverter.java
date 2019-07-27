package org.subshare.local.dto;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static java.util.Objects.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.dto.DeletedCollisionDto;
import org.subshare.local.persistence.DeletedCollision;
import org.subshare.local.persistence.DeletedCollisionDao;

import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public class DeletedCollisionDtoConverter {
	private static final Logger logger = LoggerFactory.getLogger(DeletedCollisionDtoConverter.class);

	private final LocalRepoTransaction transaction;

	public static DeletedCollisionDtoConverter create(final LocalRepoTransaction transaction) {
		return createObject(DeletedCollisionDtoConverter.class, transaction);
	}

	protected DeletedCollisionDtoConverter(final LocalRepoTransaction transaction) {
		this.transaction = requireNonNull(transaction, "transaction");
	}

	public DeletedCollisionDto toDeletedCollisionDto(final DeletedCollision deletedCollision) {
		requireNonNull(deletedCollision, "deletedCollision");
		DeletedCollisionDto result = new DeletedCollisionDto();
		result.setCollisionId(deletedCollision.getCollisionId());
		result.setSignature(deletedCollision.getSignature());
		return result;
	}

	public DeletedCollision putDeletedCollisionDto(final DeletedCollisionDto dto) {
		requireNonNull(dto, "dto");

		final DeletedCollisionDao dcDao = transaction.getDao(DeletedCollisionDao.class);
		DeletedCollision result = dcDao.getDeletedCollision(dto.getCollisionId());
		if (result == null) {
			result = new DeletedCollision(dto.getCollisionId());
		}
		result.setSignature(dto.getSignature());
		result = dcDao.makePersistent(result);
		return result;
	}
}
