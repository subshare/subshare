package org.subshare.core.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.dto.Uid;

@XmlRootElement
public class PgpKeyStateRegistryDto {
	private List<PgpKeyStateDto> pgpKeyStateDtos;
	private Uid version;

	public List<PgpKeyStateDto> getPgpKeyStateDtos() {
		if (pgpKeyStateDtos == null)
			pgpKeyStateDtos = new ArrayList<>();

		return pgpKeyStateDtos;
	}

	public void setPgpKeyStateDtos(List<PgpKeyStateDto> pgpKeyStateDtos) {
		this.pgpKeyStateDtos = pgpKeyStateDtos;
	}

	public Uid getVersion() {
		return version;
	}
	public void setVersion(Uid version) {
		this.version = version;
	}
}
