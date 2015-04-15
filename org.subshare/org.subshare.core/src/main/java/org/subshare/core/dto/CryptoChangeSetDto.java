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

	private RepositoryOwnerDto repositoryOwnerDto;

	private List<PermissionSetDto> permissionSetDtos;

	private List<PermissionDto> permissionDtos;

	private List<PermissionSetInheritanceDto> permissionSetInheritanceDtos;

	private List<UserRepoKeyPublicKeyReplacementRequestDto> userRepoKeyPublicKeyReplacementRequestDtos;

	public List<CryptoRepoFileDto> getCryptoRepoFileDtos() {
		if (cryptoRepoFileDtos == null)
			cryptoRepoFileDtos = new ArrayList<>();

		return cryptoRepoFileDtos;
	}
	public void setCryptoRepoFileDtos(final List<CryptoRepoFileDto> cryptoRepoFileDtos) {
		this.cryptoRepoFileDtos = cryptoRepoFileDtos;
	}

	public List<CryptoKeyDto> getCryptoKeyDtos() {
		if (cryptoKeyDtos == null)
			cryptoKeyDtos = new ArrayList<>();

		return cryptoKeyDtos;
	}
	public void setCryptoKeyDtos(final List<CryptoKeyDto> cryptoKeyDtos) {
		this.cryptoKeyDtos = cryptoKeyDtos;
	}

	public List<CryptoLinkDto> getCryptoLinkDtos() {
		if (cryptoLinkDtos == null)
			cryptoLinkDtos = new ArrayList<>();

		return cryptoLinkDtos;
	}
	public void setCryptoLinkDtos(final List<CryptoLinkDto> cryptoLinkDtos) {
		this.cryptoLinkDtos = cryptoLinkDtos;
	}

	public List<UserRepoKeyPublicKeyDto> getUserRepoKeyPublicKeyDtos() {
		if (userRepoKeyPublicKeyDtos == null)
			userRepoKeyPublicKeyDtos = new ArrayList<>();

		return userRepoKeyPublicKeyDtos;
	}
	public void setUserRepoKeyPublicKeyDtos(final List<UserRepoKeyPublicKeyDto> userRepoKeyPublicKeyDtos) {
		this.userRepoKeyPublicKeyDtos = userRepoKeyPublicKeyDtos;
	}

	public RepositoryOwnerDto getRepositoryOwnerDto() {
		return repositoryOwnerDto;
	}
	public void setRepositoryOwnerDto(final RepositoryOwnerDto repositoryOwnerDto) {
		this.repositoryOwnerDto = repositoryOwnerDto;
	}

	public List<PermissionSetDto> getPermissionSetDtos() {
		if (permissionSetDtos == null)
			permissionSetDtos = new ArrayList<>();

		return permissionSetDtos;
	}
	public void setPermissionSetDtos(final List<PermissionSetDto> permissionSetDtos) {
		this.permissionSetDtos = permissionSetDtos;
	}

	public List<PermissionDto> getPermissionDtos() {
		if (permissionDtos == null)
			permissionDtos = new ArrayList<>();

		return permissionDtos;
	}
	public void setPermissionDtos(final List<PermissionDto> permissionDtos) {
		this.permissionDtos = permissionDtos;
	}

	public List<PermissionSetInheritanceDto> getPermissionSetInheritanceDtos() {
		if (permissionSetInheritanceDtos == null)
			permissionSetInheritanceDtos = new ArrayList<>();

		return permissionSetInheritanceDtos;
	}
	public void setPermissionSetInheritanceDtos(final List<PermissionSetInheritanceDto> permissionSetInheritanceDtos) {
		this.permissionSetInheritanceDtos = permissionSetInheritanceDtos;
	}

	public List<UserRepoKeyPublicKeyReplacementRequestDto> getUserRepoKeyPublicKeyReplacementRequestDtos() {
		if (userRepoKeyPublicKeyReplacementRequestDtos == null)
			userRepoKeyPublicKeyReplacementRequestDtos = new ArrayList<>();

		return userRepoKeyPublicKeyReplacementRequestDtos;
	}

	public void setUserRepoKeyPublicKeyReplacementRequestDtos(List<UserRepoKeyPublicKeyReplacementRequestDto> userRepoKeyPublicKeyReplacementRequestDtos) {
		this.userRepoKeyPublicKeyReplacementRequestDtos = userRepoKeyPublicKeyReplacementRequestDtos;
	}

	@Override
	public String toString() {
		return "CryptoChangeSetDto[cryptoRepoFileDtos=" + cryptoRepoFileDtos
				+ ", cryptoKeyDtos=" + cryptoKeyDtos + ", cryptoLinkDtos="
				+ cryptoLinkDtos + ", userRepoKeyPublicKeyDtos=" + userRepoKeyPublicKeyDtos
				+ ", userRepoKeyPublicKeyReplacementRequestDtos=" + userRepoKeyPublicKeyReplacementRequestDtos + "]";
	}

	public boolean isEmpty() {
		return isEmpty(cryptoRepoFileDtos)
				&& isEmpty(cryptoKeyDtos)
				&& isEmpty(cryptoLinkDtos)
				&& isEmpty(userRepoKeyPublicKeyDtos)
				&& repositoryOwnerDto == null
				&& isEmpty(permissionSetDtos)
				&& isEmpty(permissionDtos)
				&& isEmpty(permissionSetInheritanceDtos)
				&& isEmpty(userRepoKeyPublicKeyReplacementRequestDtos);
	}

	private static boolean isEmpty(final Collection<?> c) {
		return c == null || c.isEmpty();
	}
}
