package org.subshare.core.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CryptoChangeSetDto {

	private long revision = -1;

	private List<CryptoRepoFileDto> cryptoRepoFileDtos;

	private List<HistoFrameDto> histoFrameDtos;

	private List<HistoCryptoRepoFileDto> histoCryptoRepoFileDtos; // should only be used in DOWN-syncs!

	private List<CurrentHistoCryptoRepoFileDto> currentHistoCryptoRepoFileDtos; // should only be used in DOWN-syncs!

	private List<CryptoKeyDto> cryptoKeyDtos;

	private List<CryptoLinkDto> cryptoLinkDtos;

	private List<UserRepoKeyPublicKeyDto> userRepoKeyPublicKeyDtos;

	private RepositoryOwnerDto repositoryOwnerDto;

	private List<PermissionSetDto> permissionSetDtos;

	private List<PermissionDto> permissionDtos;

	private List<PermissionSetInheritanceDto> permissionSetInheritanceDtos;

	private List<UserRepoKeyPublicKeyReplacementRequestDto> userRepoKeyPublicKeyReplacementRequestDtos;

	private List<UserRepoKeyPublicKeyReplacementRequestDeletionDto> userRepoKeyPublicKeyReplacementRequestDeletionDtos;

	private List<UserIdentityDto> userIdentityDtos;

	private List<UserIdentityLinkDto> userIdentityLinkDtos;

	private List<CollisionDto> collisionDtos;

	private List<CryptoConfigPropSetDto> cryptoConfigPropSetDtos;

	private List<DeletedCollisionDto> deletedCollisionDtos;

	public CryptoChangeSetDto() { }

	public long getRevision() {
		return revision;
	}
	public void setRevision(long revision) {
		this.revision = revision;
	}

	public List<CryptoRepoFileDto> getCryptoRepoFileDtos() {
		if (cryptoRepoFileDtos == null)
			cryptoRepoFileDtos = new ArrayList<>();

		return cryptoRepoFileDtos;
	}
	public void setCryptoRepoFileDtos(final List<CryptoRepoFileDto> cryptoRepoFileDtos) {
		this.cryptoRepoFileDtos = cryptoRepoFileDtos;
	}

	public List<HistoFrameDto> getHistoFrameDtos() {
		if (histoFrameDtos == null)
			histoFrameDtos = new ArrayList<>();

		return histoFrameDtos;
	}
	public void setHistoFrameDtos(List<HistoFrameDto> histoFrameDtos) {
		this.histoFrameDtos = histoFrameDtos;
	}

	public List<HistoCryptoRepoFileDto> getHistoCryptoRepoFileDtos() {
		if (histoCryptoRepoFileDtos == null)
			histoCryptoRepoFileDtos = new ArrayList<>();

		return histoCryptoRepoFileDtos;
	}
	public void setHistoCryptoRepoFileDtos(List<HistoCryptoRepoFileDto> histoCryptoRepoFileDtos) {
		this.histoCryptoRepoFileDtos = histoCryptoRepoFileDtos;
	}

	public List<CurrentHistoCryptoRepoFileDto> getCurrentHistoCryptoRepoFileDtos() {
		if (currentHistoCryptoRepoFileDtos == null)
			currentHistoCryptoRepoFileDtos = new ArrayList<>();

		return currentHistoCryptoRepoFileDtos;
	}
	public void setCurrentHistoCryptoRepoFileDtos(List<CurrentHistoCryptoRepoFileDto> currentHistoCryptoRepoFileDtos) {
		this.currentHistoCryptoRepoFileDtos = currentHistoCryptoRepoFileDtos;
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

	public List<UserRepoKeyPublicKeyReplacementRequestDeletionDto> getUserRepoKeyPublicKeyReplacementRequestDeletionDtos() {
		if (userRepoKeyPublicKeyReplacementRequestDeletionDtos == null)
			userRepoKeyPublicKeyReplacementRequestDeletionDtos = new ArrayList<>();

		return userRepoKeyPublicKeyReplacementRequestDeletionDtos;
	}
	public void setUserRepoKeyPublicKeyReplacementRequestDeletionDtos(List<UserRepoKeyPublicKeyReplacementRequestDeletionDto> userRepoKeyPublicKeyReplacementRequestDeletionDtos) {
		this.userRepoKeyPublicKeyReplacementRequestDeletionDtos = userRepoKeyPublicKeyReplacementRequestDeletionDtos;
	}

	public List<UserIdentityDto> getUserIdentityDtos() {
		if (userIdentityDtos == null)
			userIdentityDtos = new ArrayList<>();

		return userIdentityDtos;
	}
	public void setUserIdentityDtos(List<UserIdentityDto> userIdentityDtos) {
		this.userIdentityDtos = userIdentityDtos;
	}

	public List<UserIdentityLinkDto> getUserIdentityLinkDtos() {
		if (userIdentityLinkDtos == null)
			userIdentityLinkDtos = new ArrayList<>();

		return userIdentityLinkDtos;
	}
	public void setUserIdentityLinkDtos(final List<UserIdentityLinkDto> userIdentityLinkDtos) {
		this.userIdentityLinkDtos = userIdentityLinkDtos;
	}

	public List<CollisionDto> getCollisionDtos() {
		if (collisionDtos == null)
			collisionDtos = new ArrayList<>();

		return collisionDtos;
	}
	public void setCollisionDtos(List<CollisionDto> collisionDtos) {
		this.collisionDtos = collisionDtos;
	}

	public List<CryptoConfigPropSetDto> getCryptoConfigPropSetDtos() {
		if (cryptoConfigPropSetDtos == null)
			cryptoConfigPropSetDtos = new ArrayList<>();

		return cryptoConfigPropSetDtos;
	}
	public void setCryptoConfigPropSetDtos(List<CryptoConfigPropSetDto> cryptoConfigPropSetDtos) {
		this.cryptoConfigPropSetDtos = cryptoConfigPropSetDtos;
	}

	public List<DeletedCollisionDto> getDeletedCollisionDtos() {
		if (deletedCollisionDtos == null)
			deletedCollisionDtos = new ArrayList<>();

		return deletedCollisionDtos;
	}
	public void setDeletedCollisionDtos(List<DeletedCollisionDto> deletedCollisionDtos) {
		this.deletedCollisionDtos = deletedCollisionDtos;
	}

	@Override
	public String toString() {
		return "CryptoChangeSetDto[revision="+ revision +", cryptoRepoFileDtos=" + cryptoRepoFileDtos
				+ ", histoCryptoRepoFileDtos=" + histoCryptoRepoFileDtos
				+ ", cryptoKeyDtos=" + cryptoKeyDtos + ", cryptoLinkDtos=" + cryptoLinkDtos
				+ ", repositoryOwnerDto=" + repositoryOwnerDto
				+ ", permissionSetDtos=" + permissionSetDtos
				+ ", permissionDtos=" + permissionDtos
				+ ", permissionSetInheritanceDtos=" + permissionSetInheritanceDtos
				+ ", userRepoKeyPublicKeyDtos=" + userRepoKeyPublicKeyDtos
				+ ", userRepoKeyPublicKeyReplacementRequestDtos=" + userRepoKeyPublicKeyReplacementRequestDtos
				+ ", userIdentityDtos=" + userIdentityDtos
				+ ", userIdentityLinkDtos=" + userIdentityLinkDtos
				+ ", histoFrameDtos=" + histoFrameDtos
				+ ", histoCryptoRepoFileDtos=" + histoCryptoRepoFileDtos
				+ ", currentHistoCryptoRepoFileDtos=" + currentHistoCryptoRepoFileDtos
				+ ", collisionDtos=" + collisionDtos
				+ ", cryptoConfigPropSetDtos=" + cryptoConfigPropSetDtos
				+ ", deletedCollisionDtos=" + deletedCollisionDtos
				+ ", empty=" + isEmpty() + "]";
	}

	public boolean isEmpty() {
		return isEmpty(cryptoRepoFileDtos)
				&& isEmpty(histoCryptoRepoFileDtos)
				&& isEmpty(cryptoKeyDtos)
				&& isEmpty(cryptoLinkDtos)
				&& isEmpty(userRepoKeyPublicKeyDtos)
				&& repositoryOwnerDto == null
				&& isEmpty(permissionSetDtos)
				&& isEmpty(permissionDtos)
				&& isEmpty(permissionSetInheritanceDtos)
				&& isEmpty(userRepoKeyPublicKeyReplacementRequestDtos)
				&& isEmpty(userRepoKeyPublicKeyReplacementRequestDeletionDtos)
				&& isEmpty(userIdentityDtos)
				&& isEmpty(userIdentityLinkDtos)
				&& isEmpty(histoFrameDtos)
				&& isEmpty(histoCryptoRepoFileDtos)
				&& isEmpty(currentHistoCryptoRepoFileDtos)
				&& isEmpty(collisionDtos)
				&& isEmpty(cryptoConfigPropSetDtos)
				&& isEmpty(deletedCollisionDtos);
	}

	private static boolean isEmpty(final Collection<?> c) {
		return c == null || c.isEmpty();
	}
}
