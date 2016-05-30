package org.subshare.gui.histo;

import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import org.subshare.core.repo.LocalRepo;

public interface HistoryPaneContainer {

	/**
	 * Gets the local repository.
	 * @return the local repository. Must not be <code>null</code>.
	 */
	LocalRepo getLocalRepo();

	/**
	 * Gets the local path serving as root for this {@link HistoryPane}.
	 * @return the local path serving as root for this {@link HistoryPane}. May be <code>null</code>
	 * causing the entire repository to be shown.
	 */
	String getLocalPath();

	/**
	 * Gets the {@link TabPane} in which the {@link HistoryPane} is to be shown.
	 * @return the {@link TabPane} containing the {@link HistoryPane} as one of its tabs. Must not be <code>null</code>.
	 */
	TabPane getTabPane();

	/**
	 * Gets the tab containing the {@link HistoryPane}.
	 * @return the tab containing the {@link HistoryPane}. Must not be <code>null</code>.
	 */
	Tab getHistoryTab();

	/**
	 * Gets the button launching the export-wizard.
	 * @return the button launching the export-wizard. Must not be <code>null</code>.
	 */
	Button getExportFromHistoryButton();

}
