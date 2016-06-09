package org.subshare.gui.resolvecollision.collision;

import org.subshare.core.repo.LocalRepo;
import org.subshare.gui.resolvecollision.CollisionDtoWithPlainHistoCryptoRepoFileDto;

public class CollisionData {

	private LocalRepo localRepo;

//	private ObjectProperty<Uid> collisionId = new SimpleObjectProperty<>(this, "collisionId");

//	private ObjectProperty<CollisionDto> collisionDto = new SimpleObjectProperty<>(this, "collisionDto");

	private CollisionDtoWithPlainHistoCryptoRepoFileDto collisionDtoWithPlainHistoCryptoRepoFileDto;

	public CollisionData() {
	}

	public LocalRepo getLocalRepo() {
		return localRepo;
	}
	public void setLocalRepo(LocalRepo localRepo) {
		this.localRepo = localRepo;
	}

//	public Uid getCollisionId() {
//		return collisionId.get();
//	}
//	public void setCollisionId(Uid collisionId) {
//		this.collisionId.set(collisionId);
//	}
//	public ObjectProperty<Uid> collisionIdProperty() {
//		return collisionId;
//	}

//	public CollisionDto getCollisionDto() {
//		return collisionDto.get();
//	}
//	public void setCollisionDto(CollisionDto collisionDto) {
//		this.collisionDto.set(collisionDto);
//	}
//	public ObjectProperty<CollisionDto> collisionDtoProperty() {
//		return this.collisionDto;
//	}

	public CollisionDtoWithPlainHistoCryptoRepoFileDto getCollisionDtoWithPlainHistoCryptoRepoFileDto() {
		return collisionDtoWithPlainHistoCryptoRepoFileDto;
	}
	public void setCollisionDtoWithPlainHistoCryptoRepoFileDto(
			CollisionDtoWithPlainHistoCryptoRepoFileDto collisionDtoWithPlainHistoCryptoRepoFileDto) {
		this.collisionDtoWithPlainHistoCryptoRepoFileDto = collisionDtoWithPlainHistoCryptoRepoFileDto;
	}
}
