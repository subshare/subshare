package org.subshare.gui.server;

import static java.util.Objects.*;

import org.subshare.core.repo.ServerRepo;

import javafx.beans.property.StringProperty;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;

public class ServerRepoListItem {

	private final ServerRepo serverRepo;
	private final StringProperty nameProperty;

	public ServerRepoListItem(final ServerRepo serverRepo) {
		this.serverRepo = requireNonNull(serverRepo, "serverRepo");
		try {
			nameProperty = JavaBeanStringPropertyBuilder.create()
					.bean(serverRepo)
					.name(ServerRepo.PropertyEnum.name.name())
					.build();
		} catch (final NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public ServerRepo getServerRepo() {
		return serverRepo;
	}

	public StringProperty nameProperty() {
		return nameProperty;
	}

//	public String getName() {
//		return requireNonNull("serverRepo", serverRepo).getName();
//	}
//	public void setName(String name) {
//		requireNonNull("serverRepo", serverRepo).setName(name);
//	}
}
