package org.subshare.core.dto.split;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.DebugUtil.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.dto.CollisionDto;
import org.subshare.core.dto.CryptoChangeSetDto;
import org.subshare.core.dto.CryptoConfigPropSetDto;
import org.subshare.core.dto.CryptoKeyDto;
import org.subshare.core.dto.CryptoKeyRole;
import org.subshare.core.dto.CryptoLinkDto;
import org.subshare.core.dto.CryptoRepoFileDto;
import org.subshare.core.dto.CurrentHistoCryptoRepoFileDto;
import org.subshare.core.dto.DeletedCollisionDto;
import org.subshare.core.dto.HistoCryptoRepoFileDto;
import org.subshare.core.dto.HistoFrameDto;
import org.subshare.core.dto.PermissionDto;
import org.subshare.core.dto.PermissionSetDto;
import org.subshare.core.dto.PermissionSetInheritanceDto;
import org.subshare.core.dto.UserIdentityDto;
import org.subshare.core.dto.UserIdentityLinkDto;
import org.subshare.core.dto.UserRepoKeyPublicKeyDto;
import org.subshare.core.dto.UserRepoKeyPublicKeyReplacementRequestDeletionDto;
import org.subshare.core.dto.UserRepoKeyPublicKeyReplacementRequestDto;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.config.ConfigImpl;

public class CryptoChangeSetDtoSplitter {
	private static final Logger logger = LoggerFactory.getLogger(CryptoChangeSetDtoSplitter.class);

	public static final String CONFIG_KEY_MAX_CRYPTO_CHANGE_SET_DTO_SIZE = "maxCryptoChangeSetDtoSize";

	/**
	 * Soft limit of how many entity-DTOs should be at maximum in a {@link CryptoChangeSetDto}.
	 * <p>
	 * {@link CryptoChangeSetDto#size()} is limited <i>closely</i> to this value, but not exactly.
	 * The size may (and often does) exceed the limit, because its dependencies are also added,
	 * if an element is added.
	 * <p>
	 * For example, if the current size is 999 and the limit is 1000, of course,
	 * yet one more {@link CryptoRepoFileDto} is going to be added. But this {@link CryptoRepoFileDto} may
	 * require a parent and a {@link CryptoKeyDto} -- thus leading to 3 elements instead of just 1 to be
	 * added, so that the total size would be 1002 despite the limit of 1000.
	 * <p>
	 * Additionally, there are a few objects that need to be added together into the very first part of
	 * a split multi-part-{@code CryptoChangeSetDto}.
	 * <p>
	 * <b>Important:</b> All tests must be run from time to time with this being set to 1.
	 * Only, if the tests succeed despite the low limit (i.e. maximum separation), the code is OK.
	 */
	public static final int DEFAULT_MAX_CRYPTO_CHANGE_SET_DTO_SIZE = 1000;

	private final int maxCryptoChangeSetDtoSize;

	private boolean destroyInput;

	private final CryptoChangeSetDto inCryptoChangeSetDto;

	private List<CryptoChangeSetDto> outCryptoChangeSetDtos;
	private CryptoChangeSetDto outCryptoChangeSetDto;

	private Set<Uid> cryptoRepoFileIdsProcessed;
	private Set<Uid> cryptoKeyIdsProcessed;
	private Set<Uid> cryptoLinkIdsProcessed;
	private Set<Uid> currentHistoCryptoRepoFileIdsProcessed;

	private CryptoRepoFileDto rootCryptoRepoFileDto;
	private Map<Uid, CurrentHistoCryptoRepoFileDto> cryptoRepoFileId2CurrentHistoCryptoRepoFileDto;

	private Map<Uid, CryptoRepoFileDto> cryptoRepoFileId2CryptoRepoFileDto;
	private Map<Uid, CryptoKeyDto> cryptoKeyId2CryptoKeyDto;
	private Map<Uid, List<CryptoLinkDto>> toCryptoKeyId2CryptoLinkDtos;

	private Set<Uid> histoFrameIdsProcessed;
	private Set<Uid> histoCryptoRepoFileIdsProcessed;

	private Map<Uid, HistoFrameDto> histoFrameId2HistoFrameDto;
	private Map<Uid, HistoCryptoRepoFileDto> histoCryptoRepoFileId2HistoCryptoRepoFileDto;

	protected CryptoChangeSetDtoSplitter(final CryptoChangeSetDto inCryptoChangeSetDto) {
		this.inCryptoChangeSetDto = assertNotNull(inCryptoChangeSetDto, "inCryptoChangeSetDto");
		this.maxCryptoChangeSetDtoSize = ConfigImpl.getInstance()
				.getPropertyAsInt(CONFIG_KEY_MAX_CRYPTO_CHANGE_SET_DTO_SIZE, DEFAULT_MAX_CRYPTO_CHANGE_SET_DTO_SIZE);
	}

	public static CryptoChangeSetDtoSplitter createInstance(final CryptoChangeSetDto inCryptoChangeSetDto) {
		return new CryptoChangeSetDtoSplitter(inCryptoChangeSetDto);
	}

	public CryptoChangeSetDtoSplitter split() {
		outCryptoChangeSetDtos = new ArrayList<>();

		cryptoRepoFileIdsProcessed = new HashSet<>();
		cryptoKeyIdsProcessed = new HashSet<>();
		cryptoLinkIdsProcessed = new HashSet<>();

		buildCryptoRepoFileId2CryptoRepoFileDto();
		buildCryptoKeyId2CryptoKeyDto();
		buildToCryptoKeyId2CryptoLinkDtos();

		// *BEGIN* *ESSENTIALS* never being split
		// Because it is very hard to test and thus very error-prone, and because we likely *never*
		// have too many entities of these "essentials", we simply do not split them.
		addOutRepositoryOwnerDto();
		addOutUserRepoKeyPublicKeyDtos();
		addOutUserRepoKeyPublicKeyReplacementRequestDtos();
		addOutPermissionSetDtos();
		addOutPermissionDtos();
		addOutPermissionSetInheritanceDtos();
		addOutUserIdentityDtos();
		addOutUserIdentityLinkDtos();
		addOutRootCryptoRepoFileDto();
		// *END* *ESSENTIALS* never being split

		if (outCryptoChangeSetDtos.size() > 1)
			throw new IllegalStateException("More than one out-CryptoChangeSetDto for the essentials!");

		if (outCryptoChangeSetDto != null && ! outCryptoChangeSetDto.isEmpty())
			outCryptoChangeSetDto = null; // force the essentials to be separated, if there are any.

		rootCryptoRepoFileDto = null; // not used, anymore

		// The CryptoRepoFiles, CryptoKeys and CryptoLinks are *partially* essential. Those that are,
		// are already resolved as dependencies before.
		addOutCryptoRepoFileDtos();
		addOutCryptoLinkDtos();
		addOutCryptoKeyDtos();

		// in the above methods, we processed *all* CryptoRepoFileDtos, CryptoLinkDtos and CryptoRepoKeyDtos => not needed anymore => GC
		cryptoRepoFileIdsProcessed = null;
		cryptoKeyIdsProcessed = null;
		cryptoLinkIdsProcessed = null;

		cryptoRepoFileId2CryptoRepoFileDto = null;
		cryptoKeyId2CryptoKeyDto = null;
		toCryptoKeyId2CryptoLinkDtos = null;

		// now we process the histo-stuff.
		histoFrameIdsProcessed = new HashSet<>();
		histoCryptoRepoFileIdsProcessed = new HashSet<>();
		currentHistoCryptoRepoFileIdsProcessed = new HashSet<>();

		buildHistoFrameId2HistoFrameDto();
		buildHistoCryptoRepoFileId2HistoCryptoRepoFileDto();
		buildCryptoRepoFileId2CurrentHistoCryptoRepoFileDto();

		addOutHistoFrameDtos();
		addOutCurrentHistoCryptoRepoFileDtos(); // should only be used in DOWN-syncs!
		addOutHistoCryptoRepoFileDtos(); // should only be used in DOWN-syncs!

		// histo-stuff done => not needed anymore => GC!
		histoFrameIdsProcessed = null;
		histoCryptoRepoFileIdsProcessed = null;
		currentHistoCryptoRepoFileIdsProcessed = null;

		histoFrameId2HistoFrameDto = null;
		histoCryptoRepoFileId2HistoCryptoRepoFileDto = null;
		cryptoRepoFileId2CurrentHistoCryptoRepoFileDto = null;

		addOutCollisionDtos();
		addOutDeletedCollisionDtos();
		addOutCryptoConfigPropSetDtos();

		// *BEGIN* *ESSENTIALS* which *must* be last
		addOutUserRepoKeyPublicKeyReplacementRequestDeletionDtos();
		// *END* *ESSENTIALS* which *must* be last

		deleteEmptyOutCryptoChangeSetDto();
		updateMultiPartCount();

		if (destroyInput && ! inCryptoChangeSetDto.isEmpty())
			throw new IllegalStateException("inCryptoChangeSetDto is not empty!");

		return this;
	}

	private void addOutRootCryptoRepoFileDto() {
		if (rootCryptoRepoFileDto != null) {
			prepareOutCryptoChangeSetDto(); // *never* start a new one for it -- make sure *essentials* are always in the first one.
			addOutCryptoRepoFileDto(rootCryptoRepoFileDto);
		}
	}

	private void addOutRepositoryOwnerDto() {
		if (inCryptoChangeSetDto.getRepositoryOwnerDto() != null) {
			prepareOutCryptoChangeSetDto(); // *never* start a new one for it -- make sure *essentials* are always in the first one.
			outCryptoChangeSetDto.setRepositoryOwnerDto(inCryptoChangeSetDto.getRepositoryOwnerDto());
		}
		if (destroyInput)
			inCryptoChangeSetDto.setRepositoryOwnerDto(null);
	}

	private void addOutPermissionSetDtos() {
		for (final PermissionSetDto dto : inCryptoChangeSetDto.getPermissionSetDtos()) {
			prepareOutCryptoChangeSetDto(); // *never* start a new one for it -- make sure *essentials* are always in the first one.
			addOutPermissionSetDto(dto);
		}
		if (destroyInput)
			inCryptoChangeSetDto.setPermissionSetDtos(null);
	}

	private void addOutPermissionDtos() {
		for (final PermissionDto dto : inCryptoChangeSetDto.getPermissionDtos()) {
			prepareOutCryptoChangeSetDto(); // *never* start a new one for it -- make sure *essentials* are always in the first one.
			addOutPermissionDto(dto);
		}
		if (destroyInput)
			inCryptoChangeSetDto.setPermissionDtos(null);
	}

	private void addOutPermissionSetInheritanceDtos() {
		for (final PermissionSetInheritanceDto dto : inCryptoChangeSetDto.getPermissionSetInheritanceDtos()) {
			prepareOutCryptoChangeSetDto(); // *never* start a new one for it -- make sure *essentials* are always in the first one.
			addOutPermissionSetInheritanceDto(dto);
		}
		if (destroyInput)
			inCryptoChangeSetDto.setPermissionSetInheritanceDtos(null);
	}

	private void addOutUserRepoKeyPublicKeyReplacementRequestDtos() {
		for (final UserRepoKeyPublicKeyReplacementRequestDto dto : inCryptoChangeSetDto.getUserRepoKeyPublicKeyReplacementRequestDtos()) {
			prepareOutCryptoChangeSetDto(); // *never* start a new one for it -- make sure *essentials* are always in the first one.
			addOutUserRepoKeyPublicKeyReplacementRequestDto(dto);
		}
		if (destroyInput)
			inCryptoChangeSetDto.setUserRepoKeyPublicKeyReplacementRequestDtos(null);
	}

	private void addOutUserIdentityDtos() {
		for (final UserIdentityDto dto : inCryptoChangeSetDto.getUserIdentityDtos()) {
			prepareOutCryptoChangeSetDto(); // *never* start a new one for it -- make sure *essentials* are always in the first one.
			addOutUserIdentityDto(dto);
		}
		if (destroyInput)
			inCryptoChangeSetDto.setUserIdentityDtos(null);
	}

	private void addOutUserIdentityLinkDtos() {
		for (final UserIdentityLinkDto dto : inCryptoChangeSetDto.getUserIdentityLinkDtos()) {
			prepareOutCryptoChangeSetDto(); // *never* start a new one for it -- make sure *essentials* are always in the first one.
			addOutUserIdentityLinkDto(dto);
		}
		if (destroyInput)
			inCryptoChangeSetDto.setUserIdentityLinkDtos(null);
	}

	private void addOutUserRepoKeyPublicKeyDtos() {
		for (final UserRepoKeyPublicKeyDto dto : inCryptoChangeSetDto.getUserRepoKeyPublicKeyDtos()) {
			prepareOutCryptoChangeSetDto(); // *never* start a new one for it -- make sure all of them are together with the RepositoryOwner.
			outCryptoChangeSetDto.getUserRepoKeyPublicKeyDtos().add(dto);
		}
		if (destroyInput)
			inCryptoChangeSetDto.setUserRepoKeyPublicKeyDtos(null);
	}

	private void addOutCryptoRepoFileDtos() {
		for (final CryptoRepoFileDto dto : inCryptoChangeSetDto.getCryptoRepoFileDtos()) {
			nextOrCurrentOutCryptoChangeSetDto(); // must *not* call this again during resolving of dependencies!
			addOutCryptoRepoFileDto(dto);
		}
		if (destroyInput)
			inCryptoChangeSetDto.setCryptoRepoFileDtos(null);
	}

	private void addOutCryptoLinkDtos() {
		for (final CryptoLinkDto dto : inCryptoChangeSetDto.getCryptoLinkDtos()) {
			nextOrCurrentOutCryptoChangeSetDto(); // must *not* call this again during resolving of dependencies!
			addOutCryptoLinkDto(dto);
		}
		if (destroyInput)
			inCryptoChangeSetDto.setCryptoLinkDtos(null);
	}

	private void addOutCryptoKeyDtos() {
		for (final CryptoKeyDto dto : inCryptoChangeSetDto.getCryptoKeyDtos()) {
			nextOrCurrentOutCryptoChangeSetDto(); // must *not* call this again during resolving of dependencies!
			addOutCryptoKeyDto(dto);
		}
		if (destroyInput)
			inCryptoChangeSetDto.setCryptoKeyDtos(null);
	}

	private void addOutHistoCryptoRepoFileDtos() {
		for (final HistoCryptoRepoFileDto dto : inCryptoChangeSetDto.getHistoCryptoRepoFileDtos()) {
			nextOrCurrentOutCryptoChangeSetDto(); // must *not* call this again during resolving of dependencies!
			addOutHistoCryptoRepoFileDto(dto);
		}
		if (destroyInput)
			inCryptoChangeSetDto.setHistoCryptoRepoFileDtos(null);
	}

	private void addOutCurrentHistoCryptoRepoFileDtos() {
		for (final CurrentHistoCryptoRepoFileDto dto : inCryptoChangeSetDto.getCurrentHistoCryptoRepoFileDtos()) {
			nextOrCurrentOutCryptoChangeSetDto(); // must *not* call this again during resolving of dependencies!
			addOutCurrentHistoCryptoRepoFileDto(dto);
		}
		if (destroyInput)
			inCryptoChangeSetDto.setCurrentHistoCryptoRepoFileDtos(null);
	}

	private void addOutHistoFrameDtos() {
		for (final HistoFrameDto dto : inCryptoChangeSetDto.getHistoFrameDtos()) {
			nextOrCurrentOutCryptoChangeSetDto(); // must *not* call this again during resolving of dependencies!
			addOutHistoFrameDto(dto);
		}
		if (destroyInput)
			inCryptoChangeSetDto.setHistoFrameDtos(null);
	}

	private void addOutCollisionDtos() {
		for (final CollisionDto dto : inCryptoChangeSetDto.getCollisionDtos()) {
			nextOrCurrentOutCryptoChangeSetDto(); // must *not* call this again during resolving of dependencies!
			addOutCollisionDto(dto);
		}
		if (destroyInput)
			inCryptoChangeSetDto.setCollisionDtos(null);
	}

	private void addOutDeletedCollisionDtos() {
		for (final DeletedCollisionDto dto : inCryptoChangeSetDto.getDeletedCollisionDtos()) {
			nextOrCurrentOutCryptoChangeSetDto(); // must *not* call this again during resolving of dependencies!
			addOutDeletedCollisionDto(dto);
		}
		if (destroyInput)
			inCryptoChangeSetDto.setDeletedCollisionDtos(null);
	}

	private void addOutCryptoConfigPropSetDtos() {
		for (final CryptoConfigPropSetDto dto : inCryptoChangeSetDto.getCryptoConfigPropSetDtos()) {
			nextOrCurrentOutCryptoChangeSetDto(); // must *not* call this again during resolving of dependencies!
			addOutCryptoConfigPropSetDto(dto);
		}
		if (destroyInput)
			inCryptoChangeSetDto.setCryptoConfigPropSetDtos(null);
	}

	private void addOutUserRepoKeyPublicKeyReplacementRequestDeletionDtos() {
		for (final UserRepoKeyPublicKeyReplacementRequestDeletionDto dto : inCryptoChangeSetDto.getUserRepoKeyPublicKeyReplacementRequestDeletionDtos()) {
			nextOrCurrentOutCryptoChangeSetDto(); // must *not* call this again during resolving of dependencies!
			addOutUserRepoKeyPublicKeyReplacementRequestDeletionDto(dto);
		}
		if (destroyInput)
			inCryptoChangeSetDto.setUserRepoKeyPublicKeyReplacementRequestDeletionDtos(null);
	}

	private void addOutPermissionSetDto(final PermissionSetDto dto) {
		assertNotNull(dto, "dto");
		final Uid cryptoRepoFileId = assertNotNull(dto.getCryptoRepoFileId(), "permissionSetDto.cryptoRepoFileId");

		// *first* add dependencies as needed

		// dependency: cryptoRepoFileId => CryptoRepoFileDto
		final CryptoRepoFileDto cryptoRepoFileDto = cryptoRepoFileId2CryptoRepoFileDto.get(cryptoRepoFileId);
		if (cryptoRepoFileDto != null)
			addOutCryptoRepoFileDto(cryptoRepoFileDto);

		// *then* add actual DTO
		outCryptoChangeSetDto.getPermissionSetDtos().add(dto);
	}

	private void addOutPermissionDto(final PermissionDto dto) {
		assertNotNull(dto, "dto");
		final Uid cryptoRepoFileId = assertNotNull(dto.getCryptoRepoFileId(), "permissionDto.cryptoRepoFileId");

		// *first* add dependencies as needed

		// dependency: cryptoRepoFileId => CryptoRepoFileDto
		final CryptoRepoFileDto cryptoRepoFileDto = cryptoRepoFileId2CryptoRepoFileDto.get(cryptoRepoFileId);
		if (cryptoRepoFileDto != null)
			addOutCryptoRepoFileDto(cryptoRepoFileDto);

		// *then* add actual DTO
		outCryptoChangeSetDto.getPermissionDtos().add(dto);
	}

	private void addOutPermissionSetInheritanceDto(final PermissionSetInheritanceDto dto) {
		assertNotNull(dto, "dto");
		final Uid cryptoRepoFileId = assertNotNull(dto.getCryptoRepoFileId(), "permissionSetInheritanceDto.cryptoRepoFileId");

		// *first* add dependencies as needed

		// dependency: cryptoRepoFileId => CryptoRepoFileDto
		final CryptoRepoFileDto cryptoRepoFileDto = cryptoRepoFileId2CryptoRepoFileDto.get(cryptoRepoFileId);
		if (cryptoRepoFileDto != null)
			addOutCryptoRepoFileDto(cryptoRepoFileDto);

		// *then* add actual DTO
		outCryptoChangeSetDto.getPermissionSetInheritanceDtos().add(dto);
	}

	private void addOutUserRepoKeyPublicKeyReplacementRequestDto(final UserRepoKeyPublicKeyReplacementRequestDto dto) {
		assertNotNull(dto, "dto");
		outCryptoChangeSetDto.getUserRepoKeyPublicKeyReplacementRequestDtos().add(dto);
	}

	private void addOutUserIdentityDto(UserIdentityDto dto) {
		assertNotNull(dto, "dto");
		outCryptoChangeSetDto.getUserIdentityDtos().add(dto);
	}

	private void addOutUserIdentityLinkDto(UserIdentityLinkDto dto) {
		assertNotNull(dto, "dto");
		outCryptoChangeSetDto.getUserIdentityLinkDtos().add(dto);
	}

	private void addOutCryptoRepoFileDto(final CryptoRepoFileDto dto) {
		assertNotNull(dto, "dto");
		final Uid cryptoRepoFileId = assertNotNull(dto.getCryptoRepoFileId(), "cryptoRepoFileDto.cryptoRepoFileId");
		if (! cryptoRepoFileIdsProcessed.add(cryptoRepoFileId))
			return;

		// *first* add dependencies as needed

		// dependency: cryptoKeyId => CryptoKeyDto
		final Uid cryptoKeyId = assertNotNull(dto.getCryptoKeyId(), "cryptoRepoFileDto.cryptoKeyId"); // though temporarily null during inserts, it must never be null at the end of a tx! CryptoRepoFileDtoConverter already enforces it => do the same here.
		final CryptoKeyDto cryptoKeyDto = cryptoKeyId2CryptoKeyDto.get(cryptoKeyId);
		if (cryptoKeyDto != null)
			addOutCryptoKeyDto(cryptoKeyDto);

		// dependency: parentCryptoRepoFileId => CryptoRepoFileDto (recursive!)
		final Uid parentCryptoRepoFileId = dto.getParentCryptoRepoFileId();
		if (parentCryptoRepoFileId != null) { // is null for root.
			final CryptoRepoFileDto parentDto = cryptoRepoFileId2CryptoRepoFileDto.get(parentCryptoRepoFileId);
			// parentDto may be null, because it may already be synced before, in a separate change-set (maybe years before)
			if (parentDto != null)
				addOutCryptoRepoFileDto(parentDto);
		}

		// *then* add actual DTO
		outCryptoChangeSetDto.getCryptoRepoFileDtos().add(dto);
	}

	private void addOutCryptoKeyDto(final CryptoKeyDto dto) {
		assertNotNull(dto, "dto");
		final Uid cryptoKeyId = assertNotNull(dto.getCryptoKeyId(), "cryptoKeyDto.cryptoKeyId");
		if (! cryptoKeyIdsProcessed.add(cryptoKeyId))
			return;

		// *first* add dependencies as needed

		// dependency: cryptoRepoFileId => CryptoRepoFileDto
		final Uid cryptoRepoFileId = assertNotNull(dto.getCryptoRepoFileId(), "cryptoKeyDto.cryptoRepoFileId");
		final CryptoRepoFileDto cryptoRepoFileDto = cryptoRepoFileId2CryptoRepoFileDto.get(cryptoRepoFileId);
		if (cryptoRepoFileDto != null)
			addOutCryptoRepoFileDto(cryptoRepoFileDto);

		// dependency: those CryptoLinkDtos that are needed to decrypt the current key
		final List<CryptoLinkDto> cryptoLinkDtos = toCryptoKeyId2CryptoLinkDtos.get(cryptoKeyId);
		if (cryptoLinkDtos != null) {
			for (final CryptoLinkDto cryptoLinkDto : cryptoLinkDtos) {
				if (isFromChildCryptoRepoFile(cryptoRepoFileId, cryptoLinkDto))
					continue; // skip those from the sub-folders and files.

				addOutCryptoLinkDto(cryptoLinkDto);
			}
		}

		// *then* add actual DTO
		outCryptoChangeSetDto.getCryptoKeyDtos().add(dto);
	}

	private boolean isFromChildCryptoRepoFile(final Uid cryptoRepoFileId, final CryptoLinkDto cryptoLinkDto) {
		assertNotNull(cryptoRepoFileId, "cryptoRepoFileId");
		assertNotNull(cryptoLinkDto, "cryptoLinkDto");

		final Uid fromCryptoKeyId = cryptoLinkDto.getFromCryptoKeyId();
		if (fromCryptoKeyId == null)
			return false; // public key or encrypted with user-repo-key.

		final CryptoKeyDto fromCryptoKeyDto = cryptoKeyId2CryptoKeyDto.get(fromCryptoKeyId);
		if (fromCryptoKeyDto == null)
			return false; // we don't know, but at least it's not in the current change-set, i.e. not further bloating dependencies.

		final Uid fromCryptoRepoFileId = assertNotNull(fromCryptoKeyDto.getCryptoRepoFileId(), "fromCryptoKeyDto.cryptoRepoFileId");
//		final CryptoRepoFileDto fromCryptoRepoFileDto = cryptoRepoFileId2CryptoRepoFileDto.get(fromCryptoRepoFileId);
////		if (fromCryptoRepoFileDto == null)
////			return false; // we can't determine.
////
////		if (cryptoRepoFileId.equals(fromCryptoRepoFileDto.getParentCryptoRepoFileId()))
////			return true;
//
//		// sanity-check for backlinkKey-based logic (better, because it does not require the fromCryptoRepoFileDto)
//		if (fromCryptoRepoFileDto != null && cryptoRepoFileId.equals(fromCryptoRepoFileDto.getParentCryptoRepoFileId())) {
//			if (CryptoKeyRole.backlinkKey != assertNotNull(fromCryptoKeyDto.getCryptoKeyRole(), "fromCryptoKeyDto.cryptoKeyRole"))
//				throw new IllegalStateException("WTF?!");
//		}

		// checking via backlinkKey-based logic (better, because it does not require the fromCryptoRepoFileDto)
		final CryptoKeyRole fromCryptoKeyRole = assertNotNull(fromCryptoKeyDto.getCryptoKeyRole(), "fromCryptoKeyDto.cryptoKeyRole");
		if (CryptoKeyRole.backlinkKey == fromCryptoKeyRole && ! cryptoRepoFileId.equals(fromCryptoRepoFileId))
			return true;

		return false;
	}

	private void addOutCryptoLinkDto(final CryptoLinkDto dto) {
		assertNotNull(dto, "dto");
		final Uid cryptoLinkId = assertNotNull(dto.getCryptoLinkId(), "cryptoLinkDto.cryptoLinkId");
		if (! cryptoLinkIdsProcessed.add(cryptoLinkId))
			return;

		// *first* add dependencies as needed

		// dependency: fromUserRepoKeyId => UserRepoKeyPublicKeyDto
		if (dto.getFromUserRepoKeyId() != null) {
			// nothing to do, because this dependency is always already added first to the outgoing DTO!
		}

		// dependency: fromCryptoKeyId => CryptoKeyDto
		final Uid fromCryptoKeyId = dto.getFromCryptoKeyId();
		if (fromCryptoKeyId != null) {
			final CryptoKeyDto fromCryptoKeyDto = cryptoKeyId2CryptoKeyDto.get(fromCryptoKeyId);
			if (fromCryptoKeyDto != null)
				addOutCryptoKeyDto(fromCryptoKeyDto);
		}

		// dependency: toCryptoKeyId => CryptoKeyDto
		final Uid toCryptoKeyId = assertNotNull(dto.getToCryptoKeyId(), "cryptoLinkDto.toCryptoKeyId");
		final CryptoKeyDto toCryptoKeyDto = cryptoKeyId2CryptoKeyDto.get(toCryptoKeyId);
		if (toCryptoKeyDto != null) // the to...-side should *always* be populated, already. but we write robust code!
			addOutCryptoKeyDto(toCryptoKeyDto);

		// *then* add actual DTO
		outCryptoChangeSetDto.getCryptoLinkDtos().add(dto);
	}

	private void addOutHistoCryptoRepoFileDto(final HistoCryptoRepoFileDto dto) {
		assertNotNull(dto, "dto");
		final Uid histoCryptoRepoFileId = assertNotNull(dto.getHistoCryptoRepoFileId(), "histoCryptoRepoFileDto.histoCryptoRepoFileId");
		if (! histoCryptoRepoFileIdsProcessed.add(histoCryptoRepoFileId))
			return;

		final Uid cryptoRepoFileId = assertNotNull(dto.getCryptoRepoFileId(), "histoCryptoRepoFileDto.cryptoRepoFileId");

		// *first* add dependencies as needed

// *** all CryptoRepoFileDto and CryptoKeyDto instances are already processed when we come here! ***
//		// dependency: cryptoRepoFileId => CryptoRepoFileDto
//		final CryptoRepoFileDto cryptoRepoFileDto = cryptoRepoFileId2CryptoRepoFileDto.get(cryptoRepoFileId);
//		if (cryptoRepoFileDto != null)
//			addOutCryptoRepoFileDto(cryptoRepoFileDto);
//
//		// dependency: cryptoKeyId => CryptoKeyDto
//		final Uid cryptoKeyId = assertNotNull(dto.getCryptoKeyId(), "histoCryptoRepoFileDto.cryptoKeyId");
//		final CryptoKeyDto cryptoKeyDto = cryptoKeyId2CryptoKeyDto.get(cryptoKeyId);
//		if (cryptoKeyDto != null)
//			addOutCryptoKeyDto(cryptoKeyDto);

		// dependency: histoFrameId => HistoFrameDto
		final Uid histoFrameId = assertNotNull(dto.getHistoFrameId(), "histoCryptoRepoFileDto.histoFrameId");
		final HistoFrameDto histoFrameDto = histoFrameId2HistoFrameDto.get(histoFrameId);
		if (histoFrameDto != null)
			addOutHistoFrameDto(histoFrameDto);

		// dependency: CurrentHistoCryptoRepoFileDto -- see: https://github.com/subshare/subshare/issues/68
		final CurrentHistoCryptoRepoFileDto currentHistoCryptoRepoFileDto = cryptoRepoFileId2CurrentHistoCryptoRepoFileDto.get(cryptoRepoFileId);
		if (currentHistoCryptoRepoFileDto != null)
			addOutCurrentHistoCryptoRepoFileDto(currentHistoCryptoRepoFileDto);

		// dependency: previousHistoCryptoRepoFileId => HistoCryptoRepoFileDto
		final Uid previousHistoCryptoRepoFileId = dto.getPreviousHistoCryptoRepoFileId();
		if (previousHistoCryptoRepoFileId != null) {
			final HistoCryptoRepoFileDto previousDto = histoCryptoRepoFileId2HistoCryptoRepoFileDto.get(previousHistoCryptoRepoFileId);
			if (previousDto != null)
				addOutHistoCryptoRepoFileDto(previousDto);
		}

		// other dependencies were added completely before ;-)

		// *then* add actual DTO
		outCryptoChangeSetDto.getHistoCryptoRepoFileDtos().add(dto);
	}

	private void addOutCurrentHistoCryptoRepoFileDto(final CurrentHistoCryptoRepoFileDto dto) {
		assertNotNull(dto, "dto");
		final Uid cryptoRepoFileId = assertNotNull(dto.getCryptoRepoFileId(), "currentHistoCryptoRepoFileDto.cryptoRepoFileId");
		if (! currentHistoCryptoRepoFileIdsProcessed.add(cryptoRepoFileId))
			return;

		// *first* add dependencies as needed

// *** all CryptoRepoFileDto instances are already processed when we come here! ***
//		// dependency: cryptoRepoFileId => CryptoRepoFileDto
//		final CryptoRepoFileDto cryptoRepoFileDto = cryptoRepoFileId2CryptoRepoFileDto.get(cryptoRepoFileId);
//		if (cryptoRepoFileDto != null)
//			addOutCryptoRepoFileDto(cryptoRepoFileDto);

		// dependency: histoCryptoRepoFileId => HistoCryptoRepoFileDto
		// note: dto.histoCryptoRepoFileId may be null, but it never is in the crypto-change-set we're splitting here!
		final Uid histoCryptoRepoFileId = assertNotNull(dto.getHistoCryptoRepoFileId(), "currentHistoCryptoRepoFileDto.histoCryptoRepoFileId");
		final HistoCryptoRepoFileDto histoCryptoRepoFileDto = histoCryptoRepoFileId2HistoCryptoRepoFileDto.get(histoCryptoRepoFileId);
		if (histoCryptoRepoFileDto != null)
			addOutHistoCryptoRepoFileDto(histoCryptoRepoFileDto);

		// *then* add actual DTO
		outCryptoChangeSetDto.getCurrentHistoCryptoRepoFileDtos().add(dto);
	}

	private void addOutHistoFrameDto(final HistoFrameDto dto) {
		assertNotNull(dto, "dto");
		final Uid histoFrameId = assertNotNull(dto.getHistoFrameId(), "histoFrameDto.histoFrameId");
		if (! histoFrameIdsProcessed.add(histoFrameId))
			return;

		outCryptoChangeSetDto.getHistoFrameDtos().add(dto);
	}

	private void addOutCollisionDto(final CollisionDto dto) {
		assertNotNull(dto, "dto");
		outCryptoChangeSetDto.getCollisionDtos().add(dto);
	}

	private void addOutDeletedCollisionDto(final DeletedCollisionDto dto) {
		assertNotNull(dto, "dto");
		outCryptoChangeSetDto.getDeletedCollisionDtos().add(dto);
	}

	private void addOutCryptoConfigPropSetDto(final CryptoConfigPropSetDto dto) {
		assertNotNull(dto, "dto");
		outCryptoChangeSetDto.getCryptoConfigPropSetDtos().add(dto);
	}

	private void addOutUserRepoKeyPublicKeyReplacementRequestDeletionDto(final UserRepoKeyPublicKeyReplacementRequestDeletionDto dto) {
		assertNotNull(dto, "dto");
		outCryptoChangeSetDto.getUserRepoKeyPublicKeyReplacementRequestDeletionDtos().add(dto);
	}

	private void buildCryptoRepoFileId2CryptoRepoFileDto() {
		cryptoRepoFileId2CryptoRepoFileDto = null;
		final Map<Uid, CryptoRepoFileDto> map = new HashMap<>(inCryptoChangeSetDto.getCryptoRepoFileDtos().size());
		for (final CryptoRepoFileDto dto : inCryptoChangeSetDto.getCryptoRepoFileDtos()) {
			map.put(assertNotNull(dto.getCryptoRepoFileId(), "cryptoRepoFileDto.cryptoRepoFileId"), dto);
			if (dto.getParentCryptoRepoFileId() == null) {
				if (rootCryptoRepoFileDto != null)
					throw new IllegalStateException("Multiple rootCryptoRepoFileDto?!!!");

				rootCryptoRepoFileDto = dto;
			}
		}

		cryptoRepoFileId2CryptoRepoFileDto = map;

		logger.info("buildCryptoRepoFileId2CryptoRepoFileDto: after indexing {} DTOs:", map.size());
		logMemoryStats(logger);
	}

	private void buildCryptoKeyId2CryptoKeyDto() {
		cryptoKeyId2CryptoKeyDto = null;
		final Map<Uid, CryptoKeyDto> map = new HashMap<>(inCryptoChangeSetDto.getCryptoKeyDtos().size());
		for (final CryptoKeyDto dto : inCryptoChangeSetDto.getCryptoKeyDtos())
			map.put(assertNotNull(dto.getCryptoKeyId(), "cryptoKeyDto.cryptoKeyId"), dto);

		cryptoKeyId2CryptoKeyDto = map;

		logger.info("buildCryptoKeyId2CryptoKeyDto: after indexing {} DTOs:", map.size());
		logMemoryStats(logger);
	}

	private void buildToCryptoKeyId2CryptoLinkDtos() {
		toCryptoKeyId2CryptoLinkDtos = null;
		final Map<Uid, List<CryptoLinkDto>> map = new HashMap<>();
		for (CryptoLinkDto dto : inCryptoChangeSetDto.getCryptoLinkDtos()) {
			final Uid toCryptoKeyId = assertNotNull(dto.getToCryptoKeyId(), "cryptoLinkDto.toCryptoKeyId");
			List<CryptoLinkDto> list = map.get(toCryptoKeyId);
			if (list == null) {
				list = new LinkedList<>();
				map.put(toCryptoKeyId, list);
			}
			list.add(dto);
		}
		toCryptoKeyId2CryptoLinkDtos = map;

		logger.info("buildToCryptoKeyId2CryptoLinkDtos: after indexing {} DTOs:", inCryptoChangeSetDto.getCryptoLinkDtos().size());
		logMemoryStats(logger);
	}

	private void buildHistoFrameId2HistoFrameDto() {
		histoFrameId2HistoFrameDto = null;
		final Map<Uid, HistoFrameDto> map = new HashMap<>();
		for (HistoFrameDto dto : inCryptoChangeSetDto.getHistoFrameDtos())
			map.put(assertNotNull(dto.getHistoFrameId(), "histoFrameDto.histoFrameId"), dto);

		histoFrameId2HistoFrameDto = map;

		logger.info("buildHistoFrameId2HistoFrameDto: after indexing {} DTOs:", map.size());
		logMemoryStats(logger);
	}

	private void buildHistoCryptoRepoFileId2HistoCryptoRepoFileDto() {
//		rootHistoCryptoRepoFileDtos = new ArrayList<>();
		histoCryptoRepoFileId2HistoCryptoRepoFileDto = null;
		final Map<Uid, HistoCryptoRepoFileDto> map = new HashMap<>();
//		final Uid rootCryptoRepoFileId = rootCryptoRepoFileDto == null ? null : rootCryptoRepoFileDto.getCryptoRepoFileId();

		for (final HistoCryptoRepoFileDto dto : inCryptoChangeSetDto.getHistoCryptoRepoFileDtos()) {
			map.put(dto.getHistoCryptoRepoFileId(), dto);
//			if (rootCryptoRepoFileId != null && rootCryptoRepoFileId.equals(dto.getCryptoRepoFileId()))
//				rootHistoCryptoRepoFileDtos.add(dto);
		}

		histoCryptoRepoFileId2HistoCryptoRepoFileDto = map;

		logger.info("buildHistoCryptoRepoFileId2HistoCryptoRepoFileDto: after indexing {} DTOs:", map.size());
		logMemoryStats(logger);
	}

	private void buildCryptoRepoFileId2CurrentHistoCryptoRepoFileDto() {
		cryptoRepoFileId2CurrentHistoCryptoRepoFileDto = null;
		final Map<Uid, CurrentHistoCryptoRepoFileDto> map = new HashMap<>();
		for (final CurrentHistoCryptoRepoFileDto dto : inCryptoChangeSetDto.getCurrentHistoCryptoRepoFileDtos()) {
			final Uid cryptoRepoFileId = assertNotNull(dto.getCryptoRepoFileId(), "currentHistoCryptoRepoFileDto.cryptoRepoFileId");
			final CurrentHistoCryptoRepoFileDto old = map.put(cryptoRepoFileId, dto);
			if (old != null)
				throw new IllegalStateException("Multiple CurrentHistoCryptoRepoFileDto for cryptoRepoFileId=" + cryptoRepoFileId);
		}
		cryptoRepoFileId2CurrentHistoCryptoRepoFileDto = map;

		logger.info("buildCryptoRepoFileId2CurrentHistoCryptoRepoFileDto: after indexing {} DTOs:", map.size());
		logMemoryStats(logger);
	}

//	private void buildRootCurrentHistoCryptoRepoFileDtos() {
//		rootCurrentHistoCryptoRepoFileDtos = new ArrayList<>();
//		final Uid rootCryptoRepoFileId = rootCryptoRepoFileDto == null ? null : rootCryptoRepoFileDto.getCryptoRepoFileId();
//
//		for (CurrentHistoCryptoRepoFileDto dto : inCryptoChangeSetDto.getCurrentHistoCryptoRepoFileDtos()) {
//			if (rootCryptoRepoFileId != null && rootCryptoRepoFileId.equals(dto.getCryptoRepoFileId()))
//				rootCurrentHistoCryptoRepoFileDtos.add(dto);
//		}
//	}

	public int getMaxCryptoChangeSetDtoSize() {
		return maxCryptoChangeSetDtoSize;
	}

	protected void nextOrCurrentOutCryptoChangeSetDto() {
		prepareOutCryptoChangeSetDto();
		if (maxCryptoChangeSetDtoSize > 0 && outCryptoChangeSetDto.size() >= maxCryptoChangeSetDtoSize)
			nextOutCryptoChangeSetDto();
	}

	protected void nextOutCryptoChangeSetDto() {
		outCryptoChangeSetDto = null;
		prepareOutCryptoChangeSetDto();
	}

	protected void prepareOutCryptoChangeSetDto() {
		assertNotNull(outCryptoChangeSetDtos, "outCryptoChangeSetDtos");
		if (outCryptoChangeSetDto == null) {
			outCryptoChangeSetDto = new CryptoChangeSetDto();
			outCryptoChangeSetDto.setRevision(inCryptoChangeSetDto.getRevision());
			outCryptoChangeSetDto.setMultiPartIndex(outCryptoChangeSetDtos.size());
			outCryptoChangeSetDtos.add(outCryptoChangeSetDto);
		}
	}

	private void deleteEmptyOutCryptoChangeSetDto() {
		assertNotNull(outCryptoChangeSetDtos, "outCryptoChangeSetDtos");
		while (outCryptoChangeSetDto != null && outCryptoChangeSetDto.isEmpty()) {
			CryptoChangeSetDto removed = outCryptoChangeSetDtos.remove(outCryptoChangeSetDtos.size() - 1);
			if (outCryptoChangeSetDto != removed)
				throw new IllegalStateException("outCryptoChangeSetDto != removed");

			outCryptoChangeSetDto = outCryptoChangeSetDtos.isEmpty()
					? null : outCryptoChangeSetDtos.get(outCryptoChangeSetDtos.size() - 1);
		}
	}

	private void updateMultiPartCount() {
		for (final CryptoChangeSetDto cryptoChangeSetDto : outCryptoChangeSetDtos)
			cryptoChangeSetDto.setMultiPartCount(outCryptoChangeSetDtos.size());
	}

	public List<CryptoChangeSetDto> getOutCryptoChangeSetDtos() {
		return outCryptoChangeSetDtos;
	}

	public boolean isDestroyInput() {
		return destroyInput;
	}
	public CryptoChangeSetDtoSplitter setDestroyInput(boolean destroyInput) {
		this.destroyInput = destroyInput;
		return this;
	}
}
