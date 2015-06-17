package org.subshare.gui.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.adapter.JavaBeanStringProperty;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;

import org.subshare.core.user.User;
import org.subshare.gui.user.pgpkeytree.PgpKeyTreeItem;
import org.subshare.gui.user.pgpkeytree.RootPgpKeyTreeItem;

public class UserPane extends GridPane {

	private final EditUserManager editUserManager;
	private final User user;

	private final JavaBeanStringProperty firstNameProperty;
	private final JavaBeanStringProperty lastNameProperty;

	private final ObservableList<EmailWrapper> emailWrappers;

	@FXML
	private TextField firstNameTextField;

	@FXML
	private TextField lastNameTextField;

	@FXML
	private TableView<EmailWrapper> emailsTableView;

	@FXML
	private TableColumn<EmailWrapper, String> emailTableColumn;

	@FXML
	private TreeTableView<PgpKeyTreeItem<?>> pgpKeyTreeTableView;

	private boolean ignoreUpdateEmailsOrWrappers;

	private final PropertyChangeListener userEmailsPropertyChangeListener = event -> Platform.runLater(() -> updateEmailWrappers());

	private final InvalidationListener emailWrapperInvalidationListener = observable -> updateEmails();

	public UserPane(final EditUserManager editUserManager, final User user) {
		this.editUserManager = assertNotNull("editUserManager", editUserManager);
		this.user = assertNotNull("user", user);
		loadDynamicComponentFxml(UserPane.class, this);

		try {
			firstNameProperty = JavaBeanStringPropertyBuilder.create()
				    .bean(user)
				    .name(User.PropertyEnum.firstName.name())
				    .build();
			firstNameTextField.textProperty().bindBidirectional(firstNameProperty);

			lastNameProperty = JavaBeanStringPropertyBuilder.create()
				    .bean(user)
				    .name(User.PropertyEnum.lastName.name())
				    .build();
			lastNameTextField.textProperty().bindBidirectional(lastNameProperty);

			this.user.addPropertyChangeListener(User.PropertyEnum.emails, userEmailsPropertyChangeListener);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}

		emailWrappers = FXCollections.observableList(createEmailWrapperList());
		emailWrappers.addListener((ListChangeListener<EmailWrapper>) c -> updateEmails());

		emailsTableView.setItems(emailWrappers);
		emailTableColumn.setCellFactory(cast(TextFieldTableCell.forTableColumn()));

		emailTableColumn.prefWidthProperty().bind(emailsTableView.widthProperty().subtract(10)); // TODO we should find out the scroll-bar-width and subtract this!

		final RootPgpKeyTreeItem root = new RootPgpKeyTreeItem(pgpKeyTreeTableView, user);
		pgpKeyTreeTableView.setShowRoot(false);
		pgpKeyTreeTableView.setRoot(root);
	}

	private List<EmailWrapper> createEmailWrapperList() {
		final List<EmailWrapper> result = new ArrayList<>(user.getEmails().size());
		for (String email : user.getEmails()) {
			final EmailWrapper emailWrapper = createEmailWrapper(email);
			result.add(emailWrapper);
		}
		result.add(createEmailWrapper(null));
		return result;
	}

	private EmailWrapper createEmailWrapper(String email) {
		EmailWrapper emailWrapper = new EmailWrapper(email);
		emailWrapper.valueProperty().addListener(emailWrapperInvalidationListener);
		return emailWrapper;
	}

	private void updateEmailWrappers() {
		// user.emails => emailWrappers
		if (ignoreUpdateEmailsOrWrappers)
			return;

		ignoreUpdateEmailsOrWrappers = true;
		try {
			final Iterator<String> eIterator = user.getEmails().iterator();
			final ListIterator<EmailWrapper> wIterator = emailWrappers.listIterator();

			while (eIterator.hasNext()) {
				final String email = eIterator.next();
				EmailWrapper wrapper = wIterator.hasNext() ? wIterator.next() : null;
				if (wrapper == null) {
					wrapper = createEmailWrapper(email);
					wIterator.add(wrapper);
				}
				else if (! equal(email, wrapper.getValue()))
					wrapper.setValue(email);
			}

			if (wIterator.hasNext()) {
				while (wIterator.hasNext()) {
					wIterator.next();

					if (wIterator.hasNext())
						wIterator.remove();
				}
			}
			else
				emailWrappers.add(createEmailWrapper(null));
		} finally {
			ignoreUpdateEmailsOrWrappers = false;
		}
	}

	private void updateEmails() {
		// emailWrappers => user.emails
		if (ignoreUpdateEmailsOrWrappers)
			return;

		ignoreUpdateEmailsOrWrappers = true;
		try {
			final List<String> emails = user.getEmails(); // CopyOnWriteArrayList => read-only iterator!
			final ListIterator<EmailWrapper> wIterator = emailWrappers.listIterator();

			// TODO this implementation is not thread-safe! It is *highly* unlikely that this ever causes trouble, but
			// we might want to change the list implementation or sth. similar. Or maybe simply clear() and addAll(...)?
			int emailsIndex = -1;
			while (wIterator.hasNext()) {
				final EmailWrapper wrapper = wIterator.next();
				if (isEmpty(wrapper.getValue()))
					continue;

				++emailsIndex;
				if (emailsIndex >= emails.size())
					emails.add(wrapper.getValue());
				else {
					final String email = emails.get(emailsIndex);
					if (! equal(email, wrapper.getValue()))
						emails.set(emailsIndex, wrapper.getValue());
				}
			}

			int removeCount = emails.size() - emailsIndex - 1;
			for (int i = 0; i < removeCount; ++i)
				emails.remove(emails.size() - 1);

			if (emailWrappers.isEmpty() || ! isEmpty(emailWrappers.get(emailWrappers.size() - 1).getValue()))
				emailWrappers.add(createEmailWrapper(null));

			while (emailWrappers.size() >= 2
					&& isEmpty(emailWrappers.get(emailWrappers.size() - 1).getValue())
					&& isEmpty(emailWrappers.get(emailWrappers.size() - 2).getValue()))
				emailWrappers.remove(emailWrappers.size() - 1);

		} finally {
			ignoreUpdateEmailsOrWrappers = false;
		}
	}

	@FXML
	private void closeButtonClicked(final ActionEvent event) {
		editUserManager.endEditing(Collections.singleton(user));
	}

	@Override
	protected void finalize() throws Throwable {
		user.removePropertyChangeListener(User.PropertyEnum.emails, userEmailsPropertyChangeListener);
		super.finalize();
	}
}
