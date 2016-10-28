package org.subshare.gui.localrepo.userrepokeylist;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;

import java.util.UUID;

import org.subshare.core.dto.DebugUserRepoKeyDto;
import org.subshare.core.dto.DebugUserRepoKeyDto.KeyRingType;
import org.subshare.core.dto.UserIdentityPayloadDto;

import co.codewizards.cloudstore.core.Uid;

public class UserRepoKeyListItem {

	private final DebugUserRepoKeyDto debugUserRepoKeyDto;
	private final String name;

	public UserRepoKeyListItem(final DebugUserRepoKeyDto debugUserRepoKeyDto) {
		this.debugUserRepoKeyDto = assertNotNull("debugUserRepoKeyDto", debugUserRepoKeyDto);
		this.name = _getName(debugUserRepoKeyDto.getUserIdentityPayloadDto());
	}

	private static String _getName(UserIdentityPayloadDto dto) {
		if (dto == null)
			return null;

		StringBuilder sb = new StringBuilder();
		if (! isEmpty(dto.getFirstName()))
			sb.append(dto.getFirstName());

		if (sb.length() > 0 && ! isEmpty(dto.getLastName()))
			sb.append(' ');

		if (! isEmpty(dto.getLastName()))
			sb.append(dto.getLastName());

		return sb.toString();
	}

	public DebugUserRepoKeyDto getDebugUserRepoKeyDto() {
		return debugUserRepoKeyDto;
	}

	public Uid getUserRepoKeyId() {
		return debugUserRepoKeyDto.getUserRepoKeyId();
	}

	public UUID getServerRepositoryId() {
		return debugUserRepoKeyDto.getServerRepositoryId();
	}

	public boolean isOwner() {
		return debugUserRepoKeyDto.isOwner();
	}

	public boolean isInDatabase() {
		return debugUserRepoKeyDto.isInDatabase();
	}

	public String getKeyRingType() {
		KeyRingType keyRingType = debugUserRepoKeyDto.getKeyRingType();
		return keyRingType == null ? null : keyRingType.toString();
	}

	public int getUserIdentityCount() {
		return debugUserRepoKeyDto.getUserIdentityCount();
	}

	public boolean isInvitation() {
		return debugUserRepoKeyDto.isInvitation();
	}

	public String getName() {
		return name;
	}
}
