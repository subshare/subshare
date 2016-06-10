package org.subshare.gui.resolvecollision.collision;

import org.subshare.core.repo.LocalRepo;
import org.subshare.gui.resolvecollision.CollisionDtoWithPlainHistoCryptoRepoFileDto;

public class CollisionData {

	private LocalRepo localRepo;

	private CollisionDtoWithPlainHistoCryptoRepoFileDto collisionDtoWithPlainHistoCryptoRepoFileDto;

	public CollisionData() {
	}

	public LocalRepo getLocalRepo() {
		return localRepo;
	}
	public void setLocalRepo(LocalRepo localRepo) {
		this.localRepo = localRepo;
	}

	public CollisionDtoWithPlainHistoCryptoRepoFileDto getCollisionDtoWithPlainHistoCryptoRepoFileDto() {
		return collisionDtoWithPlainHistoCryptoRepoFileDto;
	}
	public void setCollisionDtoWithPlainHistoCryptoRepoFileDto(
			CollisionDtoWithPlainHistoCryptoRepoFileDto collisionDtoWithPlainHistoCryptoRepoFileDto) {
		this.collisionDtoWithPlainHistoCryptoRepoFileDto = collisionDtoWithPlainHistoCryptoRepoFileDto;
	}
}
