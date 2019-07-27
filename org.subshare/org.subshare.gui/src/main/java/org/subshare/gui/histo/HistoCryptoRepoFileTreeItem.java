package org.subshare.gui.histo;

import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static java.util.Objects.*;

import java.util.List;

import org.subshare.core.dto.CollisionPrivateDto;
import org.subshare.core.dto.HistoCryptoRepoFileDto;
import org.subshare.core.dto.PlainHistoCryptoRepoFileDto;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.dto.NormalFileDto;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import javafx.scene.control.TreeItem;

public class HistoCryptoRepoFileTreeItem extends TreeItem<HistoCryptoRepoFileTreeItem> {

	private Boolean hasUnresolvedCollision;

	public static class Root extends HistoCryptoRepoFileTreeItem {
	}

	private final PlainHistoCryptoRepoFileDto plainHistoCryptoRepoFileDto;

	public HistoCryptoRepoFileTreeItem(final PlainHistoCryptoRepoFileDto plainHistoCryptoRepoFileDto) {
		this.plainHistoCryptoRepoFileDto = requireNonNull(plainHistoCryptoRepoFileDto, "plainHistoCryptoRepoFileDto");
		requireNonNull(plainHistoCryptoRepoFileDto.getCryptoRepoFileId(), "plainHistoCryptoRepoFileDto.cryptoRepoFileId");
		setValue(this);
	}

	private HistoCryptoRepoFileTreeItem() {
		this.plainHistoCryptoRepoFileDto = null;
		setValue(this);
	}

	public Uid getCryptoRepoFileId() {
		return plainHistoCryptoRepoFileDto.getCryptoRepoFileId();
	}

	public PlainHistoCryptoRepoFileDto getPlainHistoCryptoRepoFileDto() {
		return plainHistoCryptoRepoFileDto;
	}

	public HistoCryptoRepoFileDto getHistoCryptoRepoFileDto() {
		return plainHistoCryptoRepoFileDto.getHistoCryptoRepoFileDto();
	}

	public RepoFileDto getRepoFileDto() {
		return plainHistoCryptoRepoFileDto.getRepoFileDto();
	}

	public HistoCryptoRepoFileTreeItem getChildOrFail(final String name) {
		final HistoCryptoRepoFileTreeItem child = getChild(name);
		if (child == null)
			throw new IllegalArgumentException(String.format("There is no child with name='%s'! children=%s", name, getChildren()));

		return child;
	}

	public HistoCryptoRepoFileTreeItem getChild(final String name) {
		requireNonNull(name, "name");
		for (final TreeItem<HistoCryptoRepoFileTreeItem> c : getChildren()) {
			final HistoCryptoRepoFileTreeItem child = (HistoCryptoRepoFileTreeItem) c;
			final RepoFileDto repoFileDto = child.getPlainHistoCryptoRepoFileDto().getRepoFileDto();
			if (repoFileDto == null)
				continue;

			if (name.equals(repoFileDto.getName()))
					return child;
		}
		return null;
	}

	public boolean hasCollision() {
		return ! plainHistoCryptoRepoFileDto.getCollisionDtos().isEmpty();
	}

	public boolean hasUnresolvedCollision() {
		if (hasUnresolvedCollision == null)
			hasUnresolvedCollision = determineHasUnresolvedCollision();

		return hasUnresolvedCollision;
	}

	private boolean determineHasUnresolvedCollision() {
		final List<CollisionPrivateDto> cpDtos = plainHistoCryptoRepoFileDto.getCollisionPrivateDtos();
		for (CollisionPrivateDto cpDto : cpDtos) {
			if (cpDto.getResolved() == null)
				return true;
		}
		return false;
	}

	public String getName() {
		final RepoFileDto repoFileDto = plainHistoCryptoRepoFileDto.getRepoFileDto();
		return repoFileDto == null
				? ("<" + getCryptoRepoFileId() + ">")
						: (isEmpty(repoFileDto.getName()) ? "/" : repoFileDto.getName());
	}

	public Long getLength() {
		final RepoFileDto repoFileDto = plainHistoCryptoRepoFileDto.getRepoFileDto();
		if (repoFileDto instanceof NormalFileDto
				&& ! PlainHistoCryptoRepoFileDto.Action.DELETE.equals(plainHistoCryptoRepoFileDto.getAction())) {
			final NormalFileDto normalFileDto = (NormalFileDto) repoFileDto;
			return normalFileDto.getLength();
		}
		return null;
	}

	@Override
	public String toString() {
		return String.format("%s[name='%s']", this.getClass().getSimpleName(), getName());
	}
}
