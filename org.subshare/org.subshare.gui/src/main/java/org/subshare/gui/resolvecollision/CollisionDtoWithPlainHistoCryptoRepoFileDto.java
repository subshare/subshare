package org.subshare.gui.resolvecollision;

import org.subshare.core.dto.CollisionDto;
import org.subshare.core.dto.PlainHistoCryptoRepoFileDto;

public class CollisionDtoWithPlainHistoCryptoRepoFileDto {

	private CollisionDto collisionDto;
	private PlainHistoCryptoRepoFileDto plainHistoCryptoRepoFileDto1;
	private PlainHistoCryptoRepoFileDto plainHistoCryptoRepoFileDto2;

	public CollisionDtoWithPlainHistoCryptoRepoFileDto() {
	}

	public CollisionDto getCollisionDto() {
		return collisionDto;
	}
	public void setCollisionDto(CollisionDto collisionDto) {
		this.collisionDto = collisionDto;
	}
	public PlainHistoCryptoRepoFileDto getPlainHistoCryptoRepoFileDto1() {
		return plainHistoCryptoRepoFileDto1;
	}
	public void setPlainHistoCryptoRepoFileDto1(PlainHistoCryptoRepoFileDto plainHistoCryptoRepoFileDto1) {
		this.plainHistoCryptoRepoFileDto1 = plainHistoCryptoRepoFileDto1;
	}
	public PlainHistoCryptoRepoFileDto getPlainHistoCryptoRepoFileDto2() {
		return plainHistoCryptoRepoFileDto2;
	}
	public void setPlainHistoCryptoRepoFileDto2(PlainHistoCryptoRepoFileDto plainHistoCryptoRepoFileDto2) {
		this.plainHistoCryptoRepoFileDto2 = plainHistoCryptoRepoFileDto2;
	}
}
