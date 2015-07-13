package org.subshare.gui.wizard;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Deque;
import java.util.IdentityHashMap;
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
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import org.subshare.gui.error.ErrorHandler;
import org.subshare.gui.util.PlatformUtil;

import co.codewizards.cloudstore.core.progress.NullProgressMonitor;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;

/**
 * Abstract wizard base class.
 * <p>
 * Concrete wizard implementations are supposed to sub-class, add at least one {@link WizardPage}
 * (either via constructor or via {@link #setFirstPage(WizardPage)}) and implement the abstract methods -
 * most importantly {@link #finish(ProgressMonitor)}.
 */
public abstract class Wizard extends StackPane {
	private final IdentityHashMap<WizardPage, Void> knownPages = new IdentityHashMap<WizardPage, Void>();

	protected final ObjectProperty<WizardPage> firstPageProperty = new SimpleObjectProperty<WizardPage>(this, "firstPage") {
		@Override
		public void set(final WizardPage newValue) {
			final WizardPage oldValue = get();
			super.set(newValue);

			if (currentPage == oldValue)
				currentPage = null;

			if (oldValue != null) {
				oldValue.completeProperty().removeListener(updateCanFinishInvalidationListener);
				getChildren().remove(oldValue);
			}

			if (newValue != null) {
				newValue.completeProperty().addListener(updateCanFinishInvalidationListener);
				newValue.setWizard(Wizard.this);
				newValue.updateButtonsDisable();

				if (currentPage == null)
					navTo(newValue, false);
			}
		}
	};

	protected final Deque<WizardPage> history = new LinkedList<>();
	private WizardPage currentPage;
	private final BooleanProperty canFinishProperty = new SimpleBooleanProperty(this, "canFinish");
	private Parent finishingPage = new DefaultFinishingPage();

	private final ObjectProperty<WizardState> stateProperty = new SimpleObjectProperty<>(this, "state", WizardState.NEW);
	private Throwable error;

    protected Wizard() {
    	this((WizardPage) null);
    }

	protected Wizard(final WizardPage firstPage) {
		setFirstPage(firstPage);

		setStyle("-fx-padding: 10; -fx-background-color: cornsilk;");

		stateProperty.addListener((ChangeListener<WizardState>) (observable, oldState, newState) -> {
			if (newState == WizardState.FINISHING)
				showFinishingPage();
			else
				hideFinishingPage();
		});
		getChildren().add(0, finishingPage);

		// The sizing strategy is really strange: It seems to somehow take the following 2 values into account, but
		// so does it with all the individual pages. The final size is not really predictable - at least I didn't find
		// a precise pattern, yet.
		setPrefWidth(500);
		setPrefHeight(300);
	}

	public void registerWizardPage(WizardPage wizardPage) {
		if (knownPages.containsKey(wizardPage))
			return;

		knownPages.put(wizardPage, null);
		wizardPage.setVisible(false);
		getChildren().add(0, wizardPage);
	}

	public ObjectProperty<WizardPage> firstPageProperty() { return firstPageProperty; }
	public WizardPage getFirstPage() { return firstPageProperty.get(); }
	public void setFirstPage(WizardPage wizardPage) { firstPageProperty.set(wizardPage); }

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

	protected void navTo(final WizardPage wizardPage, boolean addToHistory) {
		assertNotNull("wizardPage", wizardPage);
		PlatformUtil.assertFxApplicationThread();

		if (currentPage != null)
			currentPage.onHidden();

		if (currentPage != null && addToHistory)
			history.addLast(currentPage);

		currentPage = wizardPage;
		for (Node child : getChildren())
			child.setVisible(false);

		wizardPage.setVisible(true);
		getChildren().remove(wizardPage); // remove from z-order wherever it is (it might be missing!).
		getChildren().add(wizardPage); // re-add as last, which means top-most z-order.

		wizardPage.updateButtonsDisable();
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
				wizardPage.onShown();
			}
		});
	}

	/**
	 * Finish this wizard.
	 * <p>
	 * Do not override this method! Override the following callback-methods instead:
	 * <ol>
	 * <li>{@link #finishing()}
	 * <li>{@link #finish(ProgressMonitor)}
	 * <li>{@link #failed(Throwable)}
	 * <li>{@link #finished()}
	 * </ol>
	 */
	public void finish() {
		PlatformUtil.assertFxApplicationThread();

		final Thread finishThread = new Thread(getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(this)) + ".finishThread") {
			@Override
			public void run() {
				try {
					finish(new NullProgressMonitor()); // reserving the progress-monitor stuff for later ;-)

					Platform.runLater(() -> {
						preFinished();
						stateProperty.set(WizardState.FINISHED);
						finished();
					});
				} catch (final Throwable x) {
					Platform.runLater(() -> {
						error = x;
						preFailed(x);
						stateProperty.set(WizardState.FAILED);
						failed(x);
					});
				}
			}
		};

		error = null;
		stateProperty.set(WizardState.FINISHING);
		finishing();
		finishThread.start();
	}

	/**
	 * Callback-method notifying the concrete wizard-implementation about this wizard currently finishing.
	 * <p>
	 * This method is called on the JavaFX UI thread, before {@link #finish(ProgressMonitor)} is invoked.
	 * <p>
	 * This method is invoked immediately after the {@link #getState() state} was set to {@link WizardState#FINISHING}.
	 * @see #finish(ProgressMonitor)
	 * @see #finished()
	 * @see #failed(Throwable)
	 * @see WizardState#FINISHING
	 */
	protected void finishing() {
	}

	/**
	 * Perform the actual work of this wizard.
	 * <p>
	 * This method is called on a worker-thread, i.e. <b>not</b> the JavaFX UI thread!
	 * @param monitor a progress-monitor for giving feedback. Never <code>null</code>.
	 */
	protected abstract void finish(ProgressMonitor monitor) throws Exception;

	/**
	 * Callback-method notifying the concrete wizard-implementation about this wizard having successfully finished.
	 * <p>
	 * This method is called on the JavaFX UI thread, after {@link #finish(ProgressMonitor)} was invoked - and
	 * returned without throwing an exception.
	 * <p>
	 * This method is invoked immediately <i>before</i> the {@link #getState() state} is set to {@link WizardState#FINISHED}.
	 * @see #finish(ProgressMonitor)
	 * @see #finished()
	 * @see WizardState#FINISHED
	 */
	protected void preFinished() {
	}

	/**
	 * Callback-method notifying the concrete wizard-implementation about this wizard having failed.
	 * <p>
	 * This method is called on the JavaFX UI thread, after {@link #finish(ProgressMonitor)} was invoked - and
	 * threw an exception.
	 * <p>
	 * This method is invoked immediately <i>before</i> the {@link #getState() state} is set to {@link WizardState#FAILED}.
	 * @param error the exception thrown by {@link #finish(ProgressMonitor)}. Never <code>null</code>.
	 * @see #finish(ProgressMonitor)
	 * @see #finished()
	 * @see WizardState#FAILED
	 */
	protected void preFailed(final Throwable error) {
		ErrorHandler.handleError(error);
	}

	/**
	 * Callback-method notifying the concrete wizard-implementation about this wizard having successfully finished.
	 * <p>
	 * This method is called on the JavaFX UI thread, after {@link #finish(ProgressMonitor)} was invoked - and
	 * returned without throwing an exception.
	 * <p>
	 * This method is invoked immediately after the {@link #getState() state} was set to {@link WizardState#FINISHED}.
	 * @see #finish(ProgressMonitor)
	 * @see #failed(Throwable)
	 * @see WizardState#FINISHED
	 */
	protected void finished() {
	}

	/**
	 * Callback-method notifying the concrete wizard-implementation about this wizard having failed.
	 * <p>
	 * This method is called on the JavaFX UI thread, after {@link #finish(ProgressMonitor)} was invoked - and
	 * threw an exception.
	 * <p>
	 * This method is invoked immediately after the {@link #getState() state} was set to {@link WizardState#FAILED}.
	 * @param error the exception thrown by {@link #finish(ProgressMonitor)}. Never <code>null</code>.
	 * @see #finish(ProgressMonitor)
	 * @see #finished()
	 * @see WizardState#FAILED
	 */
	protected void failed(final Throwable error) {
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
	 * Gets the error that occurred when finishing this wizard (thrown by {@link #finish(ProgressMonitor)}).
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

	/**
	 * Gets the state of this wizard.
	 * <p>
	 * In order to react on changes of this state, you may hook a listener onto the corresponding
	 * {@link #stateProperty() stateProperty}. Implementors of concrete wizards can also override the callback-methods:
	 * <ol>
	 * <li>{@link #finishing()}
	 * <li>{@link #finish(ProgressMonitor)}
	 * <li>{@link #failed(Throwable)}
	 * <li>{@link #finished()}
	 * </ol>
	 * @return the state of this wizard. Never <code>null</code>.
	 */
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

//	public ObservableList<WizardPage> getPages() {
//		return pages;
//	}
//
//	public WizardPage getNextPageFromPages(final WizardPage wizardPage) {
//		assertNotNull("wizardPage", wizardPage);
//		int index = pages.indexOf(wizardPage);
//		if (index < 0)
//			return null;
//
//		++index;
//		if (index >= pages.size())
//			return null;
//
//		return pages.get(index);
//	}
//
//	public <P extends WizardPage> P getPageOrFail(final Class<P> type) {
//		P page = getPage(type);
//		if (page == null)
//			throw new IllegalArgumentException("There is no page with this type: " + type.getName());
//
//		return page;
//	}
//
//	public <P extends WizardPage> P getPage(final Class<P> type) {
//		assertNotNull("type", type);
//
//		for (WizardPage wizardPage : pages) {
//			if (type.isInstance(wizardPage))
//				return type.cast(wizardPage);
//		}
//		return null;
//	}
//
//	public WizardPage getPageOrFail(final String id) {
//		WizardPage page = getPage(id);
//		if (page == null)
//			throw new IllegalArgumentException("There is no page with this id: " + id);
//
//		return page;
//	}
//
//	public WizardPage getPage(final String id) {
//		assertNotNull("id", id);
//
//		for (WizardPage wizardPage : pages) {
//			if (id.equals(wizardPage.getId()))
//				return wizardPage;
//		}
//		return null;
//	}

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