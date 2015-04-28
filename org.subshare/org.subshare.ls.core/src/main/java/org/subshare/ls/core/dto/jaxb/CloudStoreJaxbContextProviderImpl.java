package org.subshare.ls.core.dto.jaxb;

import org.subshare.ls.core.dto.Test2Dto;

import co.codewizards.cloudstore.core.dto.jaxb.AbstractCloudStoreJaxbContextProvider;

public class CloudStoreJaxbContextProviderImpl extends AbstractCloudStoreJaxbContextProvider {

	@Override
	public Class<?>[] getClassesToBeBound() {
		return new Class<?>[] {
				Test2Dto.class
		};
	}

}
