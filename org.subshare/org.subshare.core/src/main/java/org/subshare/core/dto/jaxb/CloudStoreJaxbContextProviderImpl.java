package org.subshare.core.dto.jaxb;

import org.subshare.core.dto.CollisionDto;
import org.subshare.core.dto.CollisionPrivateDto;
import org.subshare.core.dto.CreateRepositoryRequestDto;
import org.subshare.core.dto.CryptoChangeSetDto;
import org.subshare.core.dto.CryptoKeyDeactivationDto;
import org.subshare.core.dto.CryptoKeyDto;
import org.subshare.core.dto.CryptoLinkDto;
import org.subshare.core.dto.CryptoRepoFileDto;
import org.subshare.core.dto.CryptoRepoFileDtoList;
import org.subshare.core.dto.CurrentHistoCryptoRepoFileDto;
import org.subshare.core.dto.DeletedCollisionDto;
import org.subshare.core.dto.DeletedUUID;
import org.subshare.core.dto.DeletedUid;
import org.subshare.core.dto.HistoCryptoRepoFileDto;
import org.subshare.core.dto.HistoFrameDto;
import org.subshare.core.dto.InvitationUserRepoKeyPublicKeyDto;
import org.subshare.core.dto.LongDto;
import org.subshare.core.dto.PgpKeyStateDto;
import org.subshare.core.dto.PgpKeyStateRegistryDto;
import org.subshare.core.dto.PlainHistoCryptoRepoFileDto;
import org.subshare.core.dto.RepoFileDtoWithCurrentHistoCryptoRepoFileDto;
import org.subshare.core.dto.ServerRegistryDto;
import org.subshare.core.dto.ServerRepoDto;
import org.subshare.core.dto.ServerRepoRegistryDto;
import org.subshare.core.dto.SignatureDto;
import org.subshare.core.dto.SsDeleteModificationDto;
import org.subshare.core.dto.SsDirectoryDto;
import org.subshare.core.dto.SsFileChunkDto;
import org.subshare.core.dto.SsNormalFileDto;
import org.subshare.core.dto.SsRequestRepoConnectionRepositoryDto;
import org.subshare.core.dto.SsSymlinkDto;
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
			CollisionDto.class,
			CollisionPrivateDto.class,
			CreateRepositoryRequestDto.class,
			CryptoChangeSetDto.class,
			CryptoKeyDto.class,
			CryptoKeyDeactivationDto.class,
			CryptoLinkDto.class,
			CryptoRepoFileDto.class,
			CryptoRepoFileDtoList.class,
			CurrentHistoCryptoRepoFileDto.class,
			DeletedCollisionDto.class,
			DeletedUid.class,
			DeletedUUID.class,
			HistoCryptoRepoFileDto.class,
			HistoFrameDto.class,
			InvitationUserRepoKeyPublicKeyDto.class,
			LongDto.class,
			PlainHistoCryptoRepoFileDto.class,
			PgpKeyStateDto.class,
			PgpKeyStateRegistryDto.class,
			RepoFileDtoWithCurrentHistoCryptoRepoFileDto.class,
			ServerRegistryDto.class,
			ServerRepoDto.class,
			ServerRepoRegistryDto.class,
			SignatureDto.class,
			SsDeleteModificationDto.class,
			SsDirectoryDto.class,
			SsNormalFileDto.class,
			SsFileChunkDto.class,
			SsRequestRepoConnectionRepositoryDto.class,
			SsSymlinkDto.class,
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
