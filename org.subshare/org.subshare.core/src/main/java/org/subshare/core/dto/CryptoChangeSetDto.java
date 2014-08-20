package org.subshare.core.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CryptoChangeSetDto {

	private List<CryptoRepoFileDto> cryptoRepoFileDtos;

	private List<CryptoKeyDto> cryptoKeyDtos;

	private List<CryptoLinkDto> cryptoLinkDtos;

	private List<UserRepoKeyPublicKeyDto> userRepoKeyPublicKeyDtos;

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

	public List<UserRepoKeyPublicKeyDto> getUserRepoKeyPublicKeyDtos() {
		if (userRepoKeyPublicKeyDtos == null)
			userRepoKeyPublicKeyDtos = new ArrayList<UserRepoKeyPublicKeyDto>();

		return userRepoKeyPublicKeyDtos;
	}
	public void setUserRepoKeyPublicKeyDtos(final List<UserRepoKeyPublicKeyDto> userRepoKeyPublicKeyDtos) {
		this.userRepoKeyPublicKeyDtos = userRepoKeyPublicKeyDtos;
	}

	@Override
	public String toString() {
		return "CryptoChangeSetDto[cryptoRepoFileDtos=" + cryptoRepoFileDtos
				+ ", cryptoKeyDtos=" + cryptoKeyDtos + ", cryptoLinkDtos="
				+ cryptoLinkDtos + ", userRepoKeyPublicKeyDtos=" + userRepoKeyPublicKeyDtos + "]";
	}

	public boolean isEmpty() {
		return isEmpty(cryptoRepoFileDtos)
				&& isEmpty(cryptoKeyDtos)
				&& isEmpty(cryptoLinkDtos)
				&& isEmpty(userRepoKeyPublicKeyDtos);
	}

	private static boolean isEmpty(final Collection<?> c) {
		return c == null || c.isEmpty();
	}
}
