package org.subshare.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class LongDto {
	public LongDto() {
	}

	public LongDto(long value) {
		this.value = value;
	}

	private long value;

	public long getValue() {
		return value;
	}

	public void setValue(long value) {
		this.value = value;
	}

}
