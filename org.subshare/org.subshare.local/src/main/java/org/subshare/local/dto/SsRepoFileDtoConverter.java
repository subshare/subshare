package org.subshare.local.dto;

import org.subshare.core.dto.SsNormalFileDto;
import org.subshare.core.dto.SsRepoFileDto;
import org.subshare.core.sign.Signable;
import org.subshare.local.persistence.SsNormalFile;

import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.dto.RepoFileDtoConverter;
import co.codewizards.cloudstore.local.persistence.NormalFile;
import co.codewizards.cloudstore.local.persistence.RepoFile;

public class SsRepoFileDtoConverter extends RepoFileDtoConverter {

	protected SsRepoFileDtoConverter(final LocalRepoTransaction transaction) {
		super(transaction);
	}

	@Override
	public RepoFileDto toRepoFileDto(final RepoFile repoFile, final int depth) {
		final RepoFileDto repoFileDto = super.toRepoFileDto(repoFile, depth);
		final Signable signableRepoFile = (Signable) repoFile;
		final SsRepoFileDto ssrfDto = (SsRepoFileDto) repoFileDto;

		if (repoFile instanceof NormalFile) {
			SsNormalFile normalFile = (SsNormalFile) repoFile;
			SsNormalFileDto normalFileDto = (SsNormalFileDto) ssrfDto;
			normalFileDto.setLengthWithPadding(normalFile.getLengthWithPadding());
		}

		ssrfDto.setParentName(repoFile.getParent() == null ? null : repoFile.getParent().getName());
		ssrfDto.setSignature(signableRepoFile.getSignature());
		return repoFileDto;
	}
}
