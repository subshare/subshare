package org.subshare.core.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CryptoChangeSetDto {

	private List<CryptoRepoFileDto> cryptoRepoFileDtos;

	private List<CryptoKeyDto> cryptoKeyDtos;

	private List<CryptoLinkDto> cryptoLinkDtos;

	public List<CryptoRepoFileDto> getCryptoRepoFileDtos() {
		if (cryptoRepoFileDtos == null)
			cryptoRepoFileDtos = new ArrayList<CryptoRepoFileDto>();

		return cryptoRepoFileDtos;
	}
	public void setCryptoRepoFileDtos(final List<CryptoRepoFileDto> cryptoRepoFileDtos) {
		this.cryptoRepoFileDtos = cryptoRepoFileDtos;
	}

	public List<CryptoKeyDto> getCryptoKeyDtos() {
		if (cryptoKeyDtos == null)
			cryptoKeyDtos = new ArrayList<CryptoKeyDto>();

		return cryptoKeyDtos;
	}
	public void setCryptoKeyDtos(final List<CryptoKeyDto> cryptoKeyDtos) {
		this.cryptoKeyDtos = cryptoKeyDtos;
	}

	public List<CryptoLinkDto> getCryptoLinkDtos() {
		if (cryptoLinkDtos == null)
			cryptoLinkDtos = new ArrayList<CryptoLinkDto>();

		return cryptoLinkDtos;
	}
	public void setCryptoLinkDtos(final List<CryptoLinkDto> cryptoLinkDtos) {
		this.cryptoLinkDtos = cryptoLinkDtos;
	}

	@Override
	public String toString() {
		return "CryptoChangeSetDto [cryptoRepoFileDtos=" + cryptoRepoFileDtos
				+ ", cryptoKeyDtos=" + cryptoKeyDtos + ", cryptoLinkDtos="
				+ cryptoLinkDtos + "]";
	}
}
