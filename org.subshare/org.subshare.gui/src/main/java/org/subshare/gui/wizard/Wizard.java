package org.subshare.gui.wizard;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Deque;
import java.util.LinkedList;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.layout.StackPane;

/**
 * basic wizard infrastructure class
 */
public abstract class Wizard extends StackPane {
	protected final ObservableList<WizardPage> pages = FXCollections.observableArrayList();
	protected final Deque<WizardPage> history = new LinkedList<>();
	private WizardPage currentPage;
	private final BooleanProperty canFinishProperty = new SimpleBooleanProperty(this, "canFinish");

	public final ObjectProperty<EventHandler<WizardActionEvent>> onFinishProperty() { return onFinish; }
    public final void setOnFinish(EventHandler<WizardActionEvent> value) { onFinishProperty().set(value); }
    public final EventHandler<WizardActionEvent> getOnFinish() { return onFinishProperty().get(); }
    private ObjectProperty<EventHandler<WizardActionEvent>> onFinish = new ObjectPropertyBase<EventHandler<WizardActionEvent>>() {
        @Override protected void invalidated() {
            setEventHandler(WizardActionEvent.FINISH, get());
        }

        @Override
        public Object getBean() {
            return Wizard.this;
        }

        @Override
        public String getName() {
            return "onFinish";
        }
    };

    public final ObjectProperty<EventHandler<WizardActionEvent>> onCancelProperty() { return onCancel; }
    public final void setOnCancel(EventHandler<WizardActionEvent> value) { onCancelProperty().set(value); }
    public final EventHandler<WizardActionEvent> getOnCancel() { return onCancelProperty().get(); }
    private ObjectProperty<EventHandler<WizardActionEvent>> onCancel = new ObjectPropertyBase<EventHandler<WizardActionEvent>>() {
        @Override protected void invalidated() {
            setEventHandler(WizardActionEvent.CANCEL, get());
        }

        @Override
        public Object getBean() {
            return Wizard.this;
        }

        @Override
        public String getName() {
            return "onCancel";
        }
    };

    protected Wizard() {
    	this((WizardPage[]) null);
    }

	protected Wizard(WizardPage... nodes) {
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

		if (nodes != null)
			pages.addAll(nodes);

		setStyle("-fx-padding: 10; -fx-background-color: cornsilk;");
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

		if (currentPage != null && addToHistory)
			history.addLast(currentPage);

		currentPage = wizardPage;
		getChildren().remove(wizardPage); // remove from z-order wherever it is (it might be missing!).
		getChildren().add(wizardPage); // re-add as last, which means top-most z-order.

		currentPage.manageButtons();
		updateCanFinish();
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


	public void finish() {
		doFinish();

		final EventHandler<WizardActionEvent> handler = getOnFinish();
		if (handler != null)
			handler.handle(new WizardActionEvent(this, this, WizardActionEvent.FINISH));
	}

	protected abstract void doFinish();

	public void cancel() {
		doCancel();

		final EventHandler<WizardActionEvent> handler = getOnCancel();
		if (handler != null)
			handler.handle(new WizardActionEvent(this, this, WizardActionEvent.CANCEL));
	}

	protected void doCancel() {
		// override, if needed!
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
}