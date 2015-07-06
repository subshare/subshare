package org.subshare.gui.wizard;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * basic wizard page class
 */
public abstract class WizardPage extends VBox {
	private Wizard wizard;
	private Button priorButton = new Button("_Previous");
	private Button nextButton = new Button("N_ext");
	private Button cancelButton = new Button("Cancel");
	private Button finishButton = new Button("_Finish");

	private final BooleanProperty completeProperty = new SimpleBooleanProperty(this, "complete", true) {
		@Override
		protected void invalidated() {
			manageButtons();
		}
	};

	protected WizardPage(String title) {
		Label label = new Label(title);
		label.setStyle("-fx-font-weight: bold; -fx-padding: 0 0 5 0;");
		setId(getClass().getSimpleName());
		setSpacing(5);
		setStyle("-fx-padding:10; -fx-background-color: honeydew; -fx-border-color: derive(honeydew, -30%); -fx-border-width: 3;");

		priorButton.setOnAction(event -> getWizard().navToPreviousPage());
		nextButton.setOnAction(event -> getWizard().navToNextPage());
		cancelButton.setOnAction(event -> getWizard().cancel());
		finishButton.setOnAction(event -> getWizard().finish());
	}

	private HBox getButtons() {
		Region spring = new Region();
		HBox.setHgrow(spring, Priority.ALWAYS);
		HBox buttonBar = new HBox(5);
		cancelButton.setCancelButton(true);
		finishButton.setDefaultButton(true);
		buttonBar.getChildren().addAll(spring, priorButton, nextButton, cancelButton, finishButton);
		return buttonBar;
	}

	/**
	 * Callback-method telling this page that it was added to a wizard.
	 */
	protected void onAdded(Wizard wizard) {
		if (this.wizard != null)
			throw new IllegalStateException("this.wizard != null :: Added a wizard-page to two wizards simultaneously!");

		this.wizard = assertNotNull("wizard", wizard);

		Region spring = new Region();
		VBox.setVgrow(spring, Priority.ALWAYS);

		final Parent content = getContent();
		if (content != null)
			getChildren().add(content);

		getChildren().addAll(spring, getButtons());
		finishButton.disableProperty().bind(wizard.canFinishProperty().not());
	}

	/**
	 * Callback-method telling this page that it was removed from a wizard. This is usually never invoked, because
	 * usually wizards are assembled, used and forgotten.
	 */
	protected void onRemoved(Wizard wizard) {
		if (this.wizard != assertNotNull("wizard", wizard))
			throw new IllegalStateException("this.wizard != wizard :: Removed from a wizard this page is not associated with?! WTF?!");

		this.wizard = null;

		getChildren().clear();
		finishButton.disableProperty().unbind();
	}

	protected abstract Parent getContent();

	protected boolean hasNextPage() {
		if (getWizard() == null)
			return false;

		return getWizard().hasNextPage();
	}

	protected boolean hasPreviousPage() {
		if (getWizard() == null)
			return false;

		return getWizard().hasPreviousPage();
	}

	public WizardPage getNextPage() {
		if (getWizard() == null)
			return null;

		return getWizard().getNextPageFromPages(this);
	}

//	protected WizardPage getPriorPage() {
//		return getWizard().getPreviousPageFromPages();
//	}

	protected void navTo(String id) {
		getWizard().navTo(id);
	}

	protected Wizard getWizard() {
		return wizard;
	}

	public BooleanProperty completeProperty() {
		return completeProperty;
	}

	public void manageButtons() {
		priorButton.setDisable(! hasPreviousPage());
		nextButton.setDisable(! (hasNextPage() && completeProperty.get()));
	}
}