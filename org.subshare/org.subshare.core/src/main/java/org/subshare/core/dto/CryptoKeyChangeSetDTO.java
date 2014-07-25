package org.subshare.core.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CryptoKeyChangeSetDTO {

	private List<CryptoKeyDTO> cryptoKeyDTOs;

	private List<CryptoLinkDTO> cryptoLinkDTOs;

	public List<CryptoKeyDTO> getCryptoKeyDTOs() {
		if (cryptoKeyDTOs == null)
			cryptoKeyDTOs = new ArrayList<CryptoKeyDTO>();

		return cryptoKeyDTOs;
	}
	public void setCryptoKeyDTOs(final List<CryptoKeyDTO> cryptoKeyDTOs) {
		this.cryptoKeyDTOs = cryptoKeyDTOs;
	}

	public List<CryptoLinkDTO> getCryptoLinkDTOs() {
		if (cryptoLinkDTOs == null)
			cryptoLinkDTOs = new ArrayList<CryptoLinkDTO>();

		return cryptoLinkDTOs;
	}
	public void setCryptoLinkDTOs(final List<CryptoLinkDTO> cryptoLinkDTOs) {
		this.cryptoLinkDTOs = cryptoLinkDTOs;
	}

}
