package org.subshare.core.server;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.net.MalformedURLException;
import java.net.URL;

import org.subshare.core.dto.ServerDto;

public class ServerDtoConverter {

	public ServerDto toServerDto(final Server server) {
		assertNotNull("server", server);
		final ServerDto serverDto = new ServerDto();
		serverDto.setServerId(server.getServerId());
		serverDto.setName(server.getName());
		serverDto.setUrl(server.getUrl() == null ? null : server.getUrl().toExternalForm());

		serverDto.setChanged(server.getChanged());
		return serverDto;
	}

	public Server fromServerDto(final ServerDto serverDto) {
		assertNotNull("serverDto", serverDto);
		final Server server = new ServerImpl(serverDto.getServerId());
		server.setName(serverDto.getName());
		try {
			server.setUrl(serverDto.getUrl() == null ? null : new URL(serverDto.getUrl()));
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e);
		}

		server.setChanged(serverDto.getChanged());
		return server;
	}
}
