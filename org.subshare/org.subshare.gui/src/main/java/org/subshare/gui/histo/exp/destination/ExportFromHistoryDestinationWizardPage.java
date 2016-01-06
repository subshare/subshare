package org.subshare.gui.histo.exp.destination;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.scene.Parent;

import org.subshare.gui.histo.exp.ExportFromHistoryData;
import org.subshare.gui.wizard.WizardPage;

public class ExportFromHistoryDestinationWizardPage extends WizardPage {

	private final ExportFromHistoryData exportFromHistoryData;
	private ExportFromHistoryDestinationPane exportFromHistoryDestinationPane;
	private boolean shownAtLeastOnce;

	public ExportFromHistoryDestinationWizardPage(ExportFromHistoryData exportFromHistoryData) {
		super("Export from history");
		this.exportFromHistoryData = assertNotNull("exportFromHistoryData", exportFromHistoryData);
		setMinSize(550, 550);
//		setPrefSize(600, 600);
	}

	@Override
	protected Parent createContent() {
		exportFromHistoryDestinationPane = new ExportFromHistoryDestinationPane(exportFromHistoryData) {
			@Override
			protected void updateComplete() {
				ExportFromHistoryDestinationWizardPage.this.setComplete(shownAtLeastOnce && isComplete());
			}
		};
		return exportFromHistoryDestinationPane;
	}

	@Override
	protected void onShown() {
		super.onShown();
		shownAtLeastOnce = true;
		exportFromHistoryDestinationPane.updateComplete();
	}

	@Override
	public void requestFocus() {
		super.requestFocus();

		if (exportFromHistoryDestinationPane != null)
			exportFromHistoryDestinationPane.requestFocus();
	}
}
