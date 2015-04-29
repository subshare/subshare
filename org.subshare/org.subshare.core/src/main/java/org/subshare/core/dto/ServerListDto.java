package org.subshare.core.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ServerListDto {

	private List<ServerDto> serverDtos;

	public List<ServerDto> getServerDtos() {
		if (serverDtos == null)
			serverDtos = new ArrayList<>();

		return serverDtos;
	}
	public void setServerDtos(List<ServerDto> serverDtos) {
		this.serverDtos = serverDtos;
	}
}
