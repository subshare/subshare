package org.subshare.core.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserIdentityPayloadDto {

	private List<Long> pgpKeyIds;

	public List<Long> getPgpKeyIds() {
		if (pgpKeyIds == null)
			pgpKeyIds = new ArrayList<>();

		return pgpKeyIds;
	}
	public void setPgpKeyIds(List<Long> pgpKeyIds) {
		this.pgpKeyIds = pgpKeyIds;
	}
}
