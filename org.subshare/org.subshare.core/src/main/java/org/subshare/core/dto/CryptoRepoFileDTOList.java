package org.subshare.core.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CryptoRepoFileDTOList {

	private List<CryptoRepoFileDTO> elements;

	public List<CryptoRepoFileDTO> getElements() {
		if (elements == null)
			elements = new ArrayList<CryptoRepoFileDTO>();

		return elements;
	}

	public void setElements(final List<CryptoRepoFileDTO> elements) {
		this.elements = elements;
	}

}
