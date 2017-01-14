package org.subshare.gui.createrepo.selectserver;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.net.URL;

import org.subshare.core.server.Server;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.adapter.JavaBeanObjectPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;

public class ServerListItem {

	private final Server server;

	private final StringProperty name;
	private final ObjectProperty<URL> url;

	public ServerListItem(final Server server) {
		this.server = assertNotNull(server, "server");
		try {
			name = JavaBeanStringPropertyBuilder.create()
					.bean(server)
					.name(Server.PropertyEnum.name.name()).build();

			url = JavaBeanObjectPropertyBuilder.create()
					.bean(server)
					.name(Server.PropertyEnum.url.name()).build();
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public Server getServer() {
		return server;
	}

	public StringProperty nameProperty() {
		return name;
	}

	public ObjectProperty<URL> urlProperty() {
		return url;
	}
}
