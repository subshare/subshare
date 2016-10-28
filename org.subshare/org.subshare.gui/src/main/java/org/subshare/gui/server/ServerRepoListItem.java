package org.subshare.gui.server;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.repo.ServerRepo;

import javafx.beans.property.StringProperty;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;

public class ServerRepoListItem {

	private final ServerRepo serverRepo;
	private final StringProperty nameProperty;

	public ServerRepoListItem(final ServerRepo serverRepo) {
		this.serverRepo = assertNotNull("serverRepo", serverRepo);
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
//		return assertNotNull("serverRepo", serverRepo).getName();
//	}
//	public void setName(String name) {
//		assertNotNull("serverRepo", serverRepo).setName(name);
//	}
}
