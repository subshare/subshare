package org.subshare.gui.wizard;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * basic wizard page class
 */
public abstract class WizardPage extends VBox {
	private Wizard wizard;
	protected final Button previousButton = new Button("P_revious");
	protected final Button nextButton = new Button("N_ext");
	protected final Button cancelButton = new Button("Cancel");
	protected final Button finishButton = new Button("_Finish");

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

		previousButton.setOnAction(event -> getWizard().navToPreviousPage());
		previousButton.setGraphic(new ImageView(WizardPage.class.getResource("left-24x24.png").toExternalForm()));

		nextButton.setOnAction(event -> getWizard().navToNextPage());
		nextButton.setGraphic(new ImageView(WizardPage.class.getResource("right-24x24.png").toExternalForm()));

		cancelButton.setOnAction(event -> getWizard().cancel());
		cancelButton.setGraphic(new ImageView(WizardPage.class.getResource("cancel-24x24.png").toExternalForm()));

		finishButton.setOnAction(event -> getWizard().finish());
		finishButton.setGraphic(new ImageView(WizardPage.class.getResource("ok-24x24.png").toExternalForm()));

		finishButton.disabledProperty().addListener(observable -> {
			if (finishButton.disabledProperty().get()) {
				finishButton.setDefaultButton(false);
				nextButton.setDefaultButton(true);
			}
			else {
				nextButton.setDefaultButton(false);
				finishButton.setDefaultButton(true);
			}
		});

		addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (! event.isAltDown())
					return;

				switch (event.getCode()) {
					case LEFT:
						if (!previousButton.isDisabled())
							getWizard().navToPreviousPage();

						event.consume();
						break;
					case RIGHT:
						if (!nextButton.isDisabled())
							getWizard().navToNextPage();

						event.consume();
						break;
					default:
						; // nothing
				}
			}
		});
	}

	private HBox createButtonBar() {
		Region spring = new Region();
		HBox.setHgrow(spring, Priority.ALWAYS);
		HBox buttonBar = new HBox(5);
		cancelButton.setCancelButton(true);
		finishButton.setDefaultButton(true);
		buttonBar.getChildren().addAll(spring, previousButton, nextButton, cancelButton, finishButton);
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

		final Parent content = createContent();
		if (content != null)
			getChildren().add(content);

		getChildren().addAll(spring, createButtonBar());
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

	/**
	 * Callback-method telling this page that it is now shown to the user.
	 */
	protected void onShown() {
	}

	protected void onHidden() {
	}

	protected abstract Parent createContent();

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
		previousButton.setDisable(! hasPreviousPage());
		nextButton.setDisable(! (hasNextPage() && completeProperty.get()));
	}
}