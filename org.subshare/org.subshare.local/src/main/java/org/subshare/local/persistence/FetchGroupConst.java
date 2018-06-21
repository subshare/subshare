package org.subshare.local.persistence;

public interface FetchGroupConst extends co.codewizards.cloudstore.local.persistence.FetchGroupConst {

	/**
	 * @deprecated Replace by individual fetch-groups (per part-DTO).
	 */
	@Deprecated
	String CRYPTO_CHANGE_SET_DTO = "CryptoChangeSetDto";

	String CRYPTO_REPO_FILE_DTO = "CryptoRepoFileDto";

	String CRYPTO_LINK_DTO = "CryptoLinkDto";

	String CRYPTO_KEY_DTO = "CryptoKeyDto";

	String CURRENT_HISTO_CRYPTO_REPO_FILE_DTO = "CurrentHistoCryptoRepoFileDto";

	String HISTO_CRYPTO_REPO_FILE_DTO = "HistoCryptoRepoFileDto";

	String HISTO_FRAME_DTO = "HistoFrameDto";

	String CRYPTO_CONFIG_PROP_SET_DTO = "CryptoConfigPropSetDto";

	String COLLISION_DTO = "CollisionDto";

	String PERMISSION_DTO = "PermissionDto";

	String PERMISSION_SET_DTO = "PermissionSetDto";

	String DELETED_COLLISION_DTO = "DeletedCollisionDto";

	String USER_REPO_KEY_PUBLIC_KEY_DTO = "UserRepoKeyPublicKeyDto";

	String REPOSITORY_OWNER_DTO = "RepositoryOwnerDto";

	String PERMISSION_SET_INHERITANCE_DTO = "PermissionSetInheritanceDto";

}
