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
import org.subshare.core.dto.CryptoChangeSetDto;
import org.subshare.core.dto.CryptoKeyDto;
import org.subshare.core.dto.CryptoLinkDto;
import org.subshare.core.dto.CryptoRepoFileDto;
import org.subshare.core.dto.PermissionDto;
import org.subshare.core.dto.PermissionSetDto;
import org.subshare.core.dto.PermissionSetInheritanceDto;
import org.subshare.core.dto.UserRepoKeyPublicKeyDto;

import co.codewizards.cloudstore.core.Uid;

public class CryptoChangeSetDtoSplitter {
	private static final Logger logger = LoggerFactory.getLogger(CryptoChangeSetDtoSplitter.class);

	private static final int MAX_ENTITY_DTOS_PER_BUNDLE = 1000;

	private boolean destroyInput = false;

	private final CryptoChangeSetDto inCryptoChangeSetDto;

	private List<CryptoChangeSetDto> outCryptoChangeSetDtos;
	private CryptoChangeSetDto outCryptoChangeSetDto;

	private Set<Uid> cryptoRepoFileIdsProcessed;
	private Set<Uid> cryptoKeyIdsProcessed;
	private Set<Uid> cryptoLinkIdsProcessed;

	private Map<Uid, CryptoRepoFileDto> cryptoRepoFileId2CryptoRepoFileDto;
	private Map<Uid, CryptoKeyDto> cryptoKeyId2CryptoKeyDto;
	private Map<Uid, List<CryptoLinkDto>> toCryptoKeyId2CryptoLinkDtos;

	protected CryptoChangeSetDtoSplitter(final CryptoChangeSetDto inCryptoChangeSetDto) {
		this.inCryptoChangeSetDto = assertNotNull(inCryptoChangeSetDto, "inCryptoChangeSetDto");
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
		addOutPermissionSetDtos();
		addOutPermissionDtos();
		addOutPermissionSetInheritanceDtos();
		addOutUserRepoKeyPublicKeyReplacementRequestDtos();
		addOutUserIdentityDtos();
		addOutUserIdentityLinkDtos();
		// *END* *ESSENTIALS* never being split

		// The CryptoRepoFiles, CryptoKeys and CryptoLinks are *partially* essential. Those that are,
		// are already resolved as dependencies before.
		addOutCryptoRepoFileDtos();
		addOutCryptoLinkDtos();
		addOutCryptoKeyDtos();

		// in the above methods, we processed *all* CryptoRepoFileDtos, CryptoLinkDtos and CryptoRepoKeyDtos => not needed anymore.
		cryptoRepoFileIdsProcessed = null;
		cryptoKeyIdsProcessed = null;
		cryptoLinkIdsProcessed = null;
		cryptoRepoFileId2CryptoRepoFileDto = null;
		cryptoKeyId2CryptoKeyDto = null;
		toCryptoKeyId2CryptoLinkDtos = null;

//		addOut...Dto();

		deleteEmptyOutCryptoChangeSetDto();
		return this;
	}

	private void addOutRepositoryOwnerDto() {
		getOutCryptoChangeSetDto(); // *never* start a new one for it -- make sure *essentials* are always in the first one.
		outCryptoChangeSetDto.setRepositoryOwnerDto(inCryptoChangeSetDto.getRepositoryOwnerDto());
		if (destroyInput)
			inCryptoChangeSetDto.setRepositoryOwnerDto(null);
	}

	private void addOutPermissionSetDtos() {
		for (PermissionSetDto dto : inCryptoChangeSetDto.getPermissionSetDtos()) {
			getOutCryptoChangeSetDto(); // *never* start a new one for it -- make sure *essentials* are always in the first one.
			addOutPermissionSetDto(dto);
		}
		if (destroyInput)
			inCryptoChangeSetDto.setPermissionSetDtos(null);
	}

	private void addOutPermissionDtos() {
		for (PermissionDto dto : inCryptoChangeSetDto.getPermissionDtos()) {
			getOutCryptoChangeSetDto(); // *never* start a new one for it -- make sure *essentials* are always in the first one.
			addOutPermissionDto(dto);
		}
		if (destroyInput)
			inCryptoChangeSetDto.setPermissionDtos(null);
	}

	private void addOutPermissionSetInheritanceDtos() {
		for (PermissionSetInheritanceDto dto : inCryptoChangeSetDto.getPermissionSetInheritanceDtos()) {
			getOutCryptoChangeSetDto(); // *never* start a new one for it -- make sure *essentials* are always in the first one.
			addOutPermissionSetInheritanceDto(dto);
		}
		if (destroyInput)
			inCryptoChangeSetDto.setPermissionSetInheritanceDtos(null);
	}

	private void addOutUserRepoKeyPublicKeyReplacementRequestDtos() {
		// TODO Auto-generated method stub

	}

	private void addOutUserIdentityDtos() {
		// TODO Auto-generated method stub

	}

	private void addOutUserIdentityLinkDtos() {
		// TODO Auto-generated method stub

	}

	private void addOutUserRepoKeyPublicKeyDtos() {
		for (final UserRepoKeyPublicKeyDto dto : inCryptoChangeSetDto.getUserRepoKeyPublicKeyDtos()) {
			getOutCryptoChangeSetDto(); // *never* start a new one for it -- make sure all of them are together with the RepositoryOwner.
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
		for (CryptoLinkDto dto : inCryptoChangeSetDto.getCryptoLinkDtos()) {
			nextOrCurrentOutCryptoChangeSetDto(); // must *not* call this again during resolving of dependencies!
			addOutCryptoLinkDto(dto);
		}
		if (destroyInput)
			inCryptoChangeSetDto.setCryptoLinkDtos(null);
	}

	private void addOutCryptoKeyDtos() {
		for (CryptoKeyDto dto : inCryptoChangeSetDto.getCryptoKeyDtos()) {
			nextOrCurrentOutCryptoChangeSetDto(); // must *not* call this again during resolving of dependencies!
			addOutCryptoKeyDto(dto);
		}
		if (destroyInput)
			inCryptoChangeSetDto.setCryptoKeyDtos(null);
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
			for (CryptoLinkDto cryptoLinkDto : cryptoLinkDtos) // getDependentInCryptoLinkDtos(cryptoKeyId))
				addOutCryptoLinkDto(cryptoLinkDto);
		}

		// *then* add actual DTO
		outCryptoChangeSetDto.getCryptoKeyDtos().add(dto);
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

//	private List<CryptoLinkDto> getDependentInCryptoLinkDtos(final Uid cryptoKeyId) {
//		final List<CryptoLinkDto> result = new ArrayList<>();
//		// Iterating all of them instead of using an index, because the index would be
//		// *far* *too* *large* (too much memory). Hmmm... maybe try it later...
//		for (final CryptoLinkDto cryptoLinkDto : inCryptoChangeSetDto.getCryptoLinkDtos()) {
//			final Uid toCryptoKeyId = assertNotNull(cryptoLinkDto.getToCryptoKeyId(), "cryptoLinkDto.getToCryptoKeyId");
//			if (cryptoKeyId.equals(toCryptoKeyId))
//				result.add(cryptoLinkDto);
//		}
//		return result;
//	}

	private void buildCryptoRepoFileId2CryptoRepoFileDto() {
		cryptoRepoFileId2CryptoRepoFileDto = null;
		final Map<Uid, CryptoRepoFileDto> map = new HashMap<>(inCryptoChangeSetDto.getCryptoRepoFileDtos().size());
		for (final CryptoRepoFileDto dto : inCryptoChangeSetDto.getCryptoRepoFileDtos())
			map.put(assertNotNull(dto.getCryptoRepoFileId(), "cryptoRepoFileDto.cryptoRepoFileId"), dto);

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

	protected CryptoChangeSetDto nextOrCurrentOutCryptoChangeSetDto() {
		CryptoChangeSetDto outCryptoChangeSetDto = getOutCryptoChangeSetDto();
		if (outCryptoChangeSetDto.size() >= MAX_ENTITY_DTOS_PER_BUNDLE)
			outCryptoChangeSetDto = nextOutCryptoChangeSetDto();

		return outCryptoChangeSetDto;
	}

	protected CryptoChangeSetDto nextOutCryptoChangeSetDto() {
		outCryptoChangeSetDto = null;
		return getOutCryptoChangeSetDto();
	}

	protected CryptoChangeSetDto getOutCryptoChangeSetDto() {
		assertNotNull(outCryptoChangeSetDtos, "outCryptoChangeSetDtos");
		if (outCryptoChangeSetDto == null) {
			outCryptoChangeSetDto = new CryptoChangeSetDto();
			outCryptoChangeSetDto.setRevision(inCryptoChangeSetDto.getRevision());
			outCryptoChangeSetDto.setMultiPartIndex(outCryptoChangeSetDtos.size());
			outCryptoChangeSetDtos.add(outCryptoChangeSetDto);
		}
		return outCryptoChangeSetDto;
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
