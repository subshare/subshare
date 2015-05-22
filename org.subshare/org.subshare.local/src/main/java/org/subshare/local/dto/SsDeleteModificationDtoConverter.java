package org.subshare.local.dto;

import org.subshare.core.dto.SsDeleteModificationDto;
import org.subshare.local.persistence.SsDeleteModification;

import co.codewizards.cloudstore.core.dto.DeleteModificationDto;
import co.codewizards.cloudstore.local.dto.DeleteModificationDtoConverter;
import co.codewizards.cloudstore.local.persistence.DeleteModification;

public class SsDeleteModificationDtoConverter extends DeleteModificationDtoConverter {

	protected SsDeleteModificationDtoConverter() { }

	@Override
	public DeleteModificationDto toDeleteModificationDto(DeleteModification deleteModification) {
		final SsDeleteModification modification = (SsDeleteModification) deleteModification;
		final SsDeleteModificationDto dto = (SsDeleteModificationDto) super.toDeleteModificationDto(deleteModification);
		dto.setServerPath(modification.getServerPath());
		dto.setCryptoRepoFileIdControllingPermissions(modification.getCryptoRepoFileIdControllingPermissions());
		dto.setSignature(modification.getSignature());
		return dto;
	}
}
