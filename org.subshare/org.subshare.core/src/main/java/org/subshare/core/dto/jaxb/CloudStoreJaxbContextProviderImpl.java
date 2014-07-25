package org.subshare.core.dto.jaxb;

import org.subshare.core.dto.CryptoKeyChangeSetDTO;
import org.subshare.core.dto.CryptoKeyDTO;
import org.subshare.core.dto.CryptoLinkDTO;
import org.subshare.core.dto.CryptoRepoFileDTO;
import org.subshare.core.dto.CryptoRepoFileDTOList;

import co.codewizards.cloudstore.core.dto.jaxb.AbstractCloudStoreJaxbContextProvider;

public class CloudStoreJaxbContextProviderImpl extends AbstractCloudStoreJaxbContextProvider {

	@Override
	public Class<?>[] getClassesToBeBound() {
		return new Class<?>[] {
				CryptoKeyChangeSetDTO.class,
				CryptoKeyDTO.class,
				CryptoLinkDTO.class,
				CryptoRepoFileDTO.class,
				CryptoRepoFileDTOList.class
		};
	}

}
