package org.subshare.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.dto.FileChunkDto;

@XmlRootElement
public class SsFileChunkDto extends FileChunkDto {

	private int lengthWithPadding = -1;

	public int getLengthWithPadding() {
		return lengthWithPadding;
	}

	public void setLengthWithPadding(int lengthWithPadding) {
		this.lengthWithPadding = lengthWithPadding;
	}
}
