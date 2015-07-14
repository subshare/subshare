package org.subshare.gui.maintree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import javafx.scene.Parent;

import org.subshare.core.repo.LocalRepo;
import org.subshare.gui.localrepo.LocalRepoPane;

public class LocalRepoMainTreeItem extends MainTreeItem<LocalRepo> {

	public LocalRepoMainTreeItem(final LocalRepo localRepo) {
		super(assertNotNull("localRepo", localRepo));

		getChildren().add(new LocalRepoDirectoryMainTreeItem(localRepo.getLocalRoot()));
	}

	public LocalRepo getLocalRepo() {
		return getValueObject();
	}

	@Override
	protected String getValueString() {
		final LocalRepo localRepo = getLocalRepo();

		final String name = localRepo.getName();
		if (! isEmpty(name))
			return name;

		return localRepo.getLocalRoot().getName();
	}

	@Override
	protected Parent createMainDetailContent() {
		return new LocalRepoPane(getLocalRepo());
	}
}
