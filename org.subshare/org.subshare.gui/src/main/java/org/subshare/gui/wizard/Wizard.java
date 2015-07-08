package org.subshare.gui.wizard;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Deque;
import java.util.LinkedList;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import org.subshare.gui.error.ErrorHandler;
import org.subshare.gui.util.PlatformUtil;

import co.codewizards.cloudstore.core.progress.NullProgressMonitor;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;

/**
 * basic wizard infrastructure class
 */
public abstract class Wizard extends StackPane {
	protected final ObservableList<WizardPage> pages = FXCollections.observableArrayList();
	protected final Deque<WizardPage> history = new LinkedList<>();
	private WizardPage currentPage;
	private final BooleanProperty canFinishProperty = new SimpleBooleanProperty(this, "canFinish");
	private Parent finishingPage = new DefaultFinishingPage();

	private final ObjectProperty<WizardState> stateProperty = new SimpleObjectProperty<>(this, "state", WizardState.NEW);
	private Throwable error;

    protected Wizard() {
    	this((WizardPage[]) null);
    }

	protected Wizard(WizardPage... wizardPages) {
		pages.addListener((ListChangeListener<WizardPage>) c -> {
			while (c.next()) {
				for (WizardPage wizardPage : c.getAddedSubList()) {
					getChildren().add(0, wizardPage);
					wizardPage.completeProperty().addListener(updateCanFinishInvalidationListener);
					wizardPage.onAdded(this);
				}

				for (WizardPage wizardPage : c.getRemoved()) {
					wizardPage.onRemoved(this);
					wizardPage.completeProperty().removeListener(updateCanFinishInvalidationListener);
					getChildren().remove(wizardPage);
				}

				if (currentPage == null && !pages.isEmpty())
					navTo(pages.get(0), false);
			}

			for (WizardPage wizardPage : pages)
				wizardPage.manageButtons();
		});

		if (wizardPages != null)
			pages.addAll(wizardPages);

		setStyle("-fx-padding: 10; -fx-background-color: cornsilk;");

		stateProperty.addListener((ChangeListener<WizardState>) (observable, oldState, newState) -> {
			if (newState == WizardState.FINISHING)
				showFinishingPage();
			else
				hideFinishingPage();
		});
		getChildren().add(0, finishingPage);
	}

	public void init() {
		PlatformUtil.assertFxApplicationThread();

		if (WizardState.NEW != stateProperty.get())
			throw new IllegalStateException("This wizard is not in state NEW! Current state: " + stateProperty.get());

		stateProperty.set(WizardState.IN_USER_INTERACTION);
	}

	protected void navToNextPage() {
		if (hasNextPage() && currentPage != null) {
			final WizardPage nextPage = currentPage.getNextPage();
			if (nextPage != null)
				navTo(nextPage, true);
		}
	}

	protected void navToPreviousPage() {
		if (hasPreviousPage()) {
			final WizardPage previousPage = history.removeLast();
			navTo(previousPage, false);
		}
	}

	protected boolean hasNextPage() {
		if (currentPage == null)
			return false;

		return currentPage.getNextPage() != null;
	}

	protected WizardPage getCurrentPage() {
		return currentPage;
	}

	protected boolean hasPreviousPage() {
		return !history.isEmpty();
	}

	protected void navTo(int nextPageIdx) {
		if (nextPageIdx < 0 || nextPageIdx >= pages.size()) return;
		final WizardPage nextPage = pages.get(nextPageIdx);
		navTo(nextPage, true);
	}

	protected void navTo(final WizardPage wizardPage, boolean addToHistory) {
		assertNotNull("wizardPage", wizardPage);
		PlatformUtil.assertFxApplicationThread();

		if (currentPage != null && addToHistory)
			history.addLast(currentPage);

		currentPage = wizardPage;
		for (Node child : getChildren())
			child.setVisible(false);

		wizardPage.setVisible(true);
		getChildren().remove(wizardPage); // remove from z-order wherever it is (it might be missing!).
		getChildren().add(wizardPage); // re-add as last, which means top-most z-order.

		wizardPage.manageButtons();
		updateCanFinish();

		if (getParent() != null)
			getParent().requestFocus();

		this.requestFocus();
		wizardPage.requestFocus();

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				if (getParent() != null)
					getParent().requestFocus();

				Wizard.this.requestFocus();
				wizardPage.requestFocus();
			}
		});
	}

	protected void navTo(String id) {
		if (id == null) {
			return;
		}

		pages.stream()
		.filter(page -> id.equals(page.getId()))
		.findFirst()
		.ifPresent(page -> navTo(pages.indexOf(page)));
	}

	/**
	 * Finish this wizard.
	 * <p>
	 * Do not override this method! Override the following callback-methods instead:
	 * <ul>
	 * <li>{@link #preFinish()}
	 * <li>{@link #finish(ProgressMonitor)}
	 * <li>{@link #failed(Throwable)}
	 * <li>{@link #finished()}
	 * </ul>
	 */
	public void finish() {
		PlatformUtil.assertFxApplicationThread();

		final Thread finishThread = new Thread(getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(this)) + ".finishThread") {
			@Override
			public void run() {
				try {
					finish(new NullProgressMonitor()); // reserving the progress-monitor stuff for later ;-)

					Platform.runLater(() -> {
						stateProperty.set(WizardState.FINISHED);
						finished();
					});
				} catch (final Throwable x) {
					Platform.runLater(() -> {
						error = x;
						stateProperty.set(WizardState.FAILED);
						failed(x);
					});
				}
			}
		};

		error = null;
		stateProperty.set(WizardState.FINISHING);
		preFinish();
		finishThread.start();
	}

	/**
	 * Callback-method notifying the concrete wizard that the wizard is currently finishing.
	 * <p>
	 * This method is called on the JavaFX UI thread, before {@link #finish(ProgressMonitor)} is invoked.
	 */
	protected void preFinish() {
	}

	/**
	 * Perform the actual work of this wizard.
	 * <p>
	 * This method is called on a worker-thread, i.e. <b>not</b> the JavaFX UI thread!
	 * @param monitor a progress-monitor for giving feedback. Never <code>null</code>.
	 */
	protected abstract void finish(ProgressMonitor monitor) throws Exception;

	protected void finished() {
	}

	protected void failed(final Throwable error) {
		ErrorHandler.handleError(error);
	}

	/**
	 * Cancel this wizard.
	 * <p>
	 * Do not override this method! You can instead register a listener on the {@link #stateProperty() state}.
	 */
	public void cancel() {
		PlatformUtil.assertFxApplicationThread();
		stateProperty.set(WizardState.CANCELLED);
	}

	/**
	 * Gets the error occurred when finishing this wizard.
	 * <p>
	 * If this wizard's {@link #stateProperty() state} is not {@link WizardState#FAILED FAILED}, this method
	 * returns <code>null</code>.
	 * @return the error which occurred when finishing this wizard or <code>null</code>.
	 */
	public Throwable getError() {
		return error;
	}

	public ReadOnlyObjectProperty<WizardState> stateProperty() {
		return stateProperty;
	}

	public WizardState getState() {
		return stateProperty.get();
	}

	public abstract String getTitle();

	public ReadOnlyBooleanProperty canFinishProperty() {
		return canFinishProperty;
	}

	private final InvalidationListener updateCanFinishInvalidationListener = observable -> updateCanFinish();

	protected void updateCanFinish() {
		boolean canFinish = true;

		for (WizardPage wizardPage : history) {
			if (! wizardPage.completeProperty().getValue())
				canFinish = false;
		}

		WizardPage wizardPage = currentPage;
		while (wizardPage != null) {
			if (! wizardPage.completeProperty().getValue())
				canFinish = false;

			wizardPage = wizardPage.getNextPage();
		}

		canFinishProperty.set(canFinish);
	}

	public ObservableList<WizardPage> getPages() {
		return pages;
	}

	public WizardPage getNextPageFromPages(final WizardPage wizardPage) {
		assertNotNull("wizardPage", wizardPage);
		int index = pages.indexOf(wizardPage);
		if (index < 0)
			return null;

		++index;
		if (index >= pages.size())
			return null;

		return pages.get(index);
	}

	public <P extends WizardPage> P getPageOrFail(final Class<P> type) {
		P page = getPage(type);
		if (page == null)
			throw new IllegalArgumentException("There is no page with this type: " + type.getName());

		return page;
	}

	public <P extends WizardPage> P getPage(final Class<P> type) {
		assertNotNull("type", type);

		for (WizardPage wizardPage : pages) {
			if (type.isInstance(wizardPage))
				return type.cast(wizardPage);
		}
		return null;
	}

	public WizardPage getPageOrFail(final String id) {
		WizardPage page = getPage(id);
		if (page == null)
			throw new IllegalArgumentException("There is no page with this id: " + id);

		return page;
	}

	public WizardPage getPage(final String id) {
		assertNotNull("id", id);

		for (WizardPage wizardPage : pages) {
			if (id.equals(wizardPage.getId()))
				return wizardPage;
		}
		return null;
	}

	public Parent getFinishingPage() {
		return finishingPage;
	}
	public void setFinishingPage(final Parent finishingPage) {
		assertNotNull("finishingPage", finishingPage);
		PlatformUtil.assertFxApplicationThread();

		if (! (finishingPage instanceof FinishingPage))
			throw new IllegalArgumentException("finishingPage is not an instance of FinishingPage!");

		if (stateProperty.get() == WizardState.FINISHING)
			hideFinishingPage(); // hide the old one.

		getChildren().remove(this.finishingPage);
		this.finishingPage = finishingPage;
		getChildren().add(0, finishingPage);

		if (stateProperty.get() == WizardState.FINISHING)
			showFinishingPage(); // immediately show the new one.
	}

	private void showFinishingPage() {
		getChildren().remove(finishingPage);
		for (Node child : getChildren())
			child.setVisible(false);

		finishingPage.setVisible(true);
		getChildren().add(finishingPage);
		finishingPage.requestFocus();
	}
	private void hideFinishingPage() {
		finishingPage.setVisible(false);
		getChildren().remove(finishingPage);
		getChildren().add(0, finishingPage);

		if (currentPage != null)
			currentPage.setVisible(true);
	}
}