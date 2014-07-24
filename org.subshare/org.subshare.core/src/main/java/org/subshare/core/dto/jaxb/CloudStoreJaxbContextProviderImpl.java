package org.subshare.core.dto.jaxb;

import org.subshare.core.dto.CryptoRepoFileDTO;

import co.codewizards.cloudstore.core.dto.jaxb.AbstractCloudStoreJaxbContextProvider;

public class CloudStoreJaxbContextProviderImpl extends AbstractCloudStoreJaxbContextProvider {

	@Override
	public Class<?>[] getClassesToBeBound() {
		return new Class<?>[]{
				CryptoRepoFileDTO.class
		};
	}

}
