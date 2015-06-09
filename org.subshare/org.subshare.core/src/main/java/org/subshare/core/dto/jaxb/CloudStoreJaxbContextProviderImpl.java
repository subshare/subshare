package org.subshare.core.dto.jaxb;

import org.subshare.core.dto.SsDeleteModificationDto;
import org.subshare.core.dto.SsDirectoryDto;
import org.subshare.core.dto.SsNormalFileDto;
import org.subshare.core.dto.SsRequestRepoConnectionRepositoryDto;
import org.subshare.core.dto.SsSymlinkDto;
import org.subshare.core.dto.CreateRepositoryRequestDto;
import org.subshare.core.dto.CryptoChangeSetDto;
import org.subshare.core.dto.CryptoKeyDeactivationDto;
import org.subshare.core.dto.CryptoKeyDto;
import org.subshare.core.dto.CryptoLinkDto;
import org.subshare.core.dto.CryptoRepoFileDto;
import org.subshare.core.dto.CryptoRepoFileDtoList;
import org.subshare.core.dto.DeletedUUID;
import org.subshare.core.dto.DeletedUid;
import org.subshare.core.dto.InvitationUserRepoKeyPublicKeyDto;
import org.subshare.core.dto.LongDto;
import org.subshare.core.dto.ServerRegistryDto;
import org.subshare.core.dto.ServerRepoDto;
import org.subshare.core.dto.ServerRepoRegistryDto;
import org.subshare.core.dto.SignatureDto;
import org.subshare.core.dto.UserDto;
import org.subshare.core.dto.UserIdentityDto;
import org.subshare.core.dto.UserIdentityLinkDto;
import org.subshare.core.dto.UserIdentityPayloadDto;
import org.subshare.core.dto.UserRegistryDto;
import org.subshare.core.dto.UserRepoInvitationDto;
import org.subshare.core.dto.UserRepoKeyPublicKeyDto;
import org.subshare.core.dto.UserRepoKeyPublicKeyReplacementRequestDeletionDto;
import org.subshare.core.dto.UserRepoKeyPublicKeyReplacementRequestDto;

import co.codewizards.cloudstore.core.dto.jaxb.AbstractCloudStoreJaxbContextProvider;

public class CloudStoreJaxbContextProviderImpl extends AbstractCloudStoreJaxbContextProvider {

	@Override
	public Class<?>[] getClassesToBeBound() {
		return new Class<?>[] {
				SsDeleteModificationDto.class,
				SsDirectoryDto.class,
				SsNormalFileDto.class,
				SsRequestRepoConnectionRepositoryDto.class,
				SsSymlinkDto.class,
				CreateRepositoryRequestDto.class,
				CryptoChangeSetDto.class,
				CryptoKeyDto.class,
				CryptoKeyDeactivationDto.class,
				CryptoLinkDto.class,
				CryptoRepoFileDto.class,
				CryptoRepoFileDtoList.class,
				DeletedUid.class,
				DeletedUUID.class,
				InvitationUserRepoKeyPublicKeyDto.class,
				LongDto.class,
				ServerRegistryDto.class,
				ServerRepoDto.class,
				ServerRepoRegistryDto.class,
				SignatureDto.class,
				UserDto.class,
				UserIdentityDto.class,
				UserIdentityLinkDto.class,
				UserIdentityPayloadDto.class,
				UserRegistryDto.class,
				UserRepoInvitationDto.class,
				UserRepoKeyPublicKeyDto.class,
				UserRepoKeyPublicKeyReplacementRequestDto.class,
				UserRepoKeyPublicKeyReplacementRequestDeletionDto.class
		};
	}

}
