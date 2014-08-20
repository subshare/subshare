package org.subshare.core.dto.jaxb;

import org.subshare.core.dto.CryptoChangeSetDto;
import org.subshare.core.dto.CryptoKeyDto;
import org.subshare.core.dto.CryptoLinkDto;
import org.subshare.core.dto.CryptoRepoFileDto;
import org.subshare.core.dto.CryptoRepoFileDtoList;
import org.subshare.core.dto.UserRepoKeyPublicKeyDto;

import co.codewizards.cloudstore.core.dto.jaxb.AbstractCloudStoreJaxbContextProvider;

public class CloudStoreJaxbContextProviderImpl extends AbstractCloudStoreJaxbContextProvider {

	@Override
	public Class<?>[] getClassesToBeBound() {
		return new Class<?>[] {
				CryptoChangeSetDto.class,
				CryptoKeyDto.class,
				CryptoLinkDto.class,
				CryptoRepoFileDto.class,
				CryptoRepoFileDtoList.class,
				UserRepoKeyPublicKeyDto.class
		};
	}

}
