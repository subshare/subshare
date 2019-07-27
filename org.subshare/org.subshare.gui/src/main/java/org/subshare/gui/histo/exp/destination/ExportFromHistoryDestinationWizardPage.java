package org.subshare.gui.histo.exp.destination;

import static java.util.Objects.*;

import org.subshare.gui.histo.exp.ExportFromHistoryData;
import org.subshare.gui.wizard.WizardPage;

import javafx.scene.Parent;

public class ExportFromHistoryDestinationWizardPage extends WizardPage {

	private final ExportFromHistoryData exportFromHistoryData;
	private ExportFromHistoryDestinationPane exportFromHistoryDestinationPane;
	private boolean shownAtLeastOnce;

	public ExportFromHistoryDestinationWizardPage(ExportFromHistoryData exportFromHistoryData) {
		super("Export from history");
		this.exportFromHistoryData = requireNonNull(exportFromHistoryData, "exportFromHistoryData");
		shownRequired.set(true);
		setMinSize(550, 550);
//		setPrefSize(600, 600);
	}

	@Override
	protected Parent createContent() {
		exportFromHistoryDestinationPane = new ExportFromHistoryDestinationPane(exportFromHistoryData);
		return exportFromHistoryDestinationPane;
	}
}
