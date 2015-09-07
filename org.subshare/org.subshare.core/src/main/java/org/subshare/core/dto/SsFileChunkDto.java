package org.subshare.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.dto.FileChunkDto;

@XmlRootElement
public class SsFileChunkDto extends FileChunkDto {

	private int paddingLength = -1;

	public int getPaddingLength() {
		return paddingLength;
	}

	public void setPaddingLength(int paddingLength) {
		this.paddingLength = paddingLength;
	}
}
