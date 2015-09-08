package org.subshare.local.dto;

import org.subshare.core.dto.SsFileChunkDto;
import org.subshare.local.persistence.SsFileChunk;

import co.codewizards.cloudstore.core.dto.FileChunkDto;
import co.codewizards.cloudstore.local.dto.FileChunkDtoConverter;
import co.codewizards.cloudstore.local.persistence.FileChunk;

public class SsFileChunkDtoConverter extends FileChunkDtoConverter {

	protected SsFileChunkDtoConverter() {
	}

	@Override
	public FileChunkDto toFileChunkDto(FileChunk _fileChunk) {
		final SsFileChunkDto fileChunkDto = (SsFileChunkDto) super.toFileChunkDto(_fileChunk);
		final SsFileChunk fileChunk = (SsFileChunk) _fileChunk;
		fileChunkDto.setLengthWithPadding(fileChunk.getLengthWithPadding());
		return fileChunkDto;
	}
}
