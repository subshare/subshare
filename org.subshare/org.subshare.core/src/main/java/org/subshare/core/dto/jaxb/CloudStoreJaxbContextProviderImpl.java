package org.subshare.core.dto.jaxb;

import org.subshare.core.dto.SsDirectoryDto;
import org.subshare.core.dto.SsNormalFileDto;
import org.subshare.core.dto.SsSymlinkDto;
import org.subshare.core.dto.CryptoChangeSetDto;
import org.subshare.core.dto.CryptoKeyDeactivationDto;
import org.subshare.core.dto.CryptoKeyDto;
import org.subshare.core.dto.CryptoLinkDto;
import org.subshare.core.dto.CryptoRepoFileDto;
import org.subshare.core.dto.CryptoRepoFileDtoList;
import org.subshare.core.dto.SignatureDto;
import org.subshare.core.dto.UserDto;
import org.subshare.core.dto.UserListDto;
import org.subshare.core.dto.UserRepoInvitationDto;
import org.subshare.core.dto.UserRepoKeyPublicKeyDto;
import org.subshare.core.dto.UserRepoKeyPublicKeyReplacementRequestDeletionDto;
import org.subshare.core.dto.UserRepoKeyPublicKeyReplacementRequestDto;

import co.codewizards.cloudstore.core.dto.jaxb.AbstractCloudStoreJaxbContextProvider;

public class CloudStoreJaxbContextProviderImpl extends AbstractCloudStoreJaxbContextProvider {

	@Override
	public Class<?>[] getClassesToBeBound() {
		return new Class<?>[] {
				SsDirectoryDto.class,
				SsNormalFileDto.class,
				SsSymlinkDto.class,
				CryptoChangeSetDto.class,
				CryptoKeyDto.class,
				CryptoKeyDeactivationDto.class,
				CryptoLinkDto.class,
				CryptoRepoFileDto.class,
				CryptoRepoFileDtoList.class,
				SignatureDto.class,
				UserDto.class,
				UserListDto.class,
				UserRepoInvitationDto.class,
				UserRepoKeyPublicKeyDto.class,
				UserRepoKeyPublicKeyReplacementRequestDto.class,
				UserRepoKeyPublicKeyReplacementRequestDeletionDto.class
		};
	}

}
