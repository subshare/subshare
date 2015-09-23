package org.subshare.local.dto;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;

import org.subshare.core.dto.SsFileChunkDto;
import org.subshare.local.persistence.SsFileChunk;
import org.subshare.local.persistence.TempFileChunk;

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

	public FileChunkDto toFileChunkDto(TempFileChunk fileChunk) {
		final FileChunkDto fileChunkDto = createObject(FileChunkDto.class);
		fileChunkDto.setOffset(fileChunk.getOffset());
		fileChunkDto.setLength(fileChunk.getLength());
		fileChunkDto.setSha1(fileChunk.getSha1());
		return fileChunkDto;
	}
}
