package org.subshare.gui.histo;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import javafx.scene.control.TreeItem;

import org.subshare.core.dto.HistoCryptoRepoFileDto;
import org.subshare.core.dto.PlainHistoCryptoRepoFileDto;

import co.codewizards.cloudstore.core.dto.NormalFileDto;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.Uid;

public class HistoCryptoRepoFileTreeItem extends TreeItem<HistoCryptoRepoFileTreeItem> {

	public static class Root extends HistoCryptoRepoFileTreeItem {
	}

	public static enum Action {
		ADD,
		MODIFY,
		DELETE
	}

	private final PlainHistoCryptoRepoFileDto plainHistoCryptoRepoFileDto;

	public HistoCryptoRepoFileTreeItem(final PlainHistoCryptoRepoFileDto plainHistoCryptoRepoFileDto) {
		this.plainHistoCryptoRepoFileDto = assertNotNull("plainHistoCryptoRepoFileDto", plainHistoCryptoRepoFileDto);
		assertNotNull("plainHistoCryptoRepoFileDto.cryptoRepoFileId", plainHistoCryptoRepoFileDto.getCryptoRepoFileId());
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

	public Action getAction() {
		final HistoCryptoRepoFileDto histoCryptoRepoFileDto = plainHistoCryptoRepoFileDto.getHistoCryptoRepoFileDto();

		if (histoCryptoRepoFileDto == null)
			return null;

		if (histoCryptoRepoFileDto.getDeleted() != null)
			return Action.DELETE;

		if (histoCryptoRepoFileDto.getPreviousHistoCryptoRepoFileId() != null)
			return Action.MODIFY;

		return Action.ADD;
	}

	public String getName() {
		final RepoFileDto repoFileDto = plainHistoCryptoRepoFileDto.getRepoFileDto();
		return repoFileDto == null
				? ("<" + getCryptoRepoFileId() + ">")
						: (isEmpty(repoFileDto.getName()) ? "/" : repoFileDto.getName());
	}

	public Long getLength() {
		final RepoFileDto repoFileDto = plainHistoCryptoRepoFileDto.getRepoFileDto();
		if (repoFileDto instanceof NormalFileDto) {
			final NormalFileDto normalFileDto = (NormalFileDto) repoFileDto;
			return normalFileDto.getLength();
		}
		return null;
	}
}