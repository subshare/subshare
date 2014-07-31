package org.subshare.core.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CryptoRepoFileDtoList {

	private List<CryptoRepoFileDto> elements;

	public List<CryptoRepoFileDto> getElements() {
		if (elements == null)
			elements = new ArrayList<CryptoRepoFileDto>();

		return elements;
	}

	public void setElements(final List<CryptoRepoFileDto> elements) {
		this.elements = elements;
	}

}
