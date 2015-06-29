package org.subshare.gui.user;

import static co.codewizards.cloudstore.core.bean.PropertyChangeListenerUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.adapter.JavaBeanStringProperty;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import org.subshare.core.pgp.CreatePgpKeyParam;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.man.PgpPrivateKeyPassphraseStore;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistry;
import org.subshare.gui.ls.PgpLs;
import org.subshare.gui.ls.PgpPrivateKeyPassphraseManagerLs;
import org.subshare.gui.ls.UserRegistryLs;
import org.subshare.gui.pgp.createkey.CreatePgpKeyDialog;
import org.subshare.gui.pgp.createkey.FxPgpUserId;
import org.subshare.gui.pgp.createkey.TimeUnit;
import org.subshare.gui.pgp.creatingkey.CreatingPgpKeyDialog;
import org.subshare.gui.pgp.keytree.PgpKeyPgpKeyTreeItem;
import org.subshare.gui.pgp.keytree.PgpKeyTreeItem;
import org.subshare.gui.pgp.keytree.PgpKeyTreePane;
import org.subshare.gui.pgp.keytree.UserRootPgpKeyTreeItem;
import org.subshare.gui.pgp.selectkey.SelectPgpKeyDialog;
import org.subshare.gui.selectuser.SelectUserDialog;

import co.codewizards.cloudstore.core.oio.File;

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
//	private TreeTableView<PgpKeyTreeItem<?>> pgpKeyTreeTableView;
	private PgpKeyTreePane pgpKeyTreePane;

	@FXML
	private Button createPgpKeyButton;

//	@FXML
//	private Button importPgpKeyButton;

	@FXML
	private Button exportPgpKeyButton;

	@FXML
	private Button assignPgpKeyToOtherUserButton;

	@FXML
	private Button signPgpKeyButton;

	@FXML
	private Button deletePgpKeyButton;

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

			addWeakPropertyChangeListener(this.user, User.PropertyEnum.emails, userEmailsPropertyChangeListener);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}

		emailWrappers = FXCollections.observableList(createEmailWrapperList());
		emailWrappers.addListener((ListChangeListener<EmailWrapper>) c -> updateEmails());

		emailsTableView.setItems(emailWrappers);
		emailTableColumn.setCellFactory(cast(TextFieldTableCell.forTableColumn()));

		emailTableColumn.prefWidthProperty().bind(emailsTableView.widthProperty().subtract(10)); // TODO we should find out the scroll-bar-width and subtract this!

		final UserRootPgpKeyTreeItem root = new UserRootPgpKeyTreeItem(pgpKeyTreePane.getTreeTableView(), user);
		pgpKeyTreePane.getTreeTableView().setRoot(root);
		pgpKeyTreePane.getTreeTableView().getSelectionModel().getSelectedItems().addListener((InvalidationListener) observable -> updateDisable());
		updateDisable();
	}

	private void updateDisable() {
		final int selectedPgpKeysSize = getSelectedPgpKeys().size();
		exportPgpKeyButton.setDisable(selectedPgpKeysSize == 0);
		deletePgpKeyButton.setDisable(selectedPgpKeysSize == 0);
		assignPgpKeyToOtherUserButton.setDisable(selectedPgpKeysSize == 0);
		signPgpKeyButton.setDisable(selectedPgpKeysSize != 1);
	}

	private Set<PgpKey> getSelectedPgpKeys() {
		final Set<PgpKey> result = new LinkedHashSet<PgpKey>();
		for (final TreeItem<PgpKeyTreeItem<?>> treeItem : pgpKeyTreePane.getTreeTableView().getSelectionModel().getSelectedItems()) {
			final PgpKeyTreeItem<?> pgpKeyTreeItem = treeItem.getValue();
			final PgpKeyPgpKeyTreeItem pgpKeyPgpKeyTreeItem = pgpKeyTreeItem.getThisOrParentPgpKeyTreeItemOfType(PgpKeyPgpKeyTreeItem.class);
			assertNotNull("pgpKeyPgpKeyTreeItem", pgpKeyPgpKeyTreeItem);
			final PgpKey pgpKey = pgpKeyPgpKeyTreeItem.getPgpKey();
			result.add(pgpKey);
		}
		return result;
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

	protected Pgp getPgp() {
		return PgpLs.getPgpOrFail();
	}

	@FXML
	private void createPgpKeyButtonClicked(final ActionEvent event) {
		final Window owner = getScene().getWindow();
		final CreatePgpKeyDialog dialog = new CreatePgpKeyDialog(owner, createCreatePgpKeyParam());
		dialog.showAndWait();
		final CreatePgpKeyParam createPgpKeyParam = dialog.getCreatePgpKeyParam();
		if (createPgpKeyParam == null)
			return;

		final PgpPrivateKeyPassphraseStore pgpPrivateKeyPassphraseStore = PgpPrivateKeyPassphraseManagerLs.getPgpPrivateKeyPassphraseStore();

		final CreatingPgpKeyDialog dialog2 = new CreatingPgpKeyDialog(owner, createPgpKeyParam);
		final Thread createPgpKeyThread = new Thread("createPgpKeyThread") {
			@Override
			public void run() {
				final Pgp pgp = getPgp();
				final PgpKey pgpKey = pgp.createPgpKey(createPgpKeyParam);
				user.getPgpKeyIds().add(pgpKey.getPgpKeyId());

				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						dialog2.close();

//						final PgpKeyPgpKeyTreeItem child = new PgpKeyPgpKeyTreeItem(pgpKey); // now done by listener
//						pgpKeyTreeTableView.getRoot().getChildren().add(child);

						pgpPrivateKeyPassphraseStore.putPassphrase(pgpKey.getPgpKeyId(), createPgpKeyParam.getPassphrase());
					}
				});
			}
		};
		dialog2.show();
		createPgpKeyThread.start();
	}

	private CreatePgpKeyParam createCreatePgpKeyParam() {
		final CreatePgpKeyParam createPgpKeyParam = new CreatePgpKeyParam();
		createPgpKeyParam.setValiditySeconds(TimeUnit.YEAR.getSeconds() * 10);

		final String name = getName();

		Set<String> emails = new HashSet<>();
		for (final EmailWrapper emailWrapper : emailWrappers) {
			final String email = trim(emailWrapper.getValue());
			if (isEmpty(email))
				continue;

			if (emails.add(email))
				createPgpKeyParam.getUserIds().add(new FxPgpUserId(name, email));
		}

		return createPgpKeyParam;
	}

	private String getName() {
		final String firstName = trim(firstNameTextField.getText());
		final String lastName = trim(lastNameTextField.getText());
		final StringBuilder sb = new StringBuilder();

		if (! isEmpty(firstName))
			sb.append(firstName);

		if (! isEmpty(lastName)) {
			if (sb.length() > 0)
				sb.append(' ');

			sb.append(lastName);
		}

		return sb.toString();
	}

//	@FXML
//	private void importPgpKeyButtonClicked(final ActionEvent event) {
//		final File file = showOpenFileDialog("Choose file containing PGP key(s) to import");
//		if (file == null)
//			return;
//
//		final Pgp pgp = getPgp();
//		final ImportKeysResult importKeysResult = pgp.importKeys(file);
//		for (ImportedMasterKey importedMasterKey : importKeysResult.getPgpKeyId2ImportedMasterKey().values()) {
//			final PgpKeyId pgpKeyId = importedMasterKey.getPgpKeyId();
//			final PgpKey pgpKey = pgp.getPgpKey(pgpKeyId);
//			assertNotNull("pgp.getPgpKey(" + pgpKeyId + ")", pgpKey);
//
//			for (final String userId : pgpKey.getUserIds()) {
//				final PgpUserId pgpUserId = new PgpUserId(userId);
//				final String email = pgpUserId.getEmail();
//				if (! isEmpty(email) && ! user.getEmails().contains(email))
//					user.getEmails().add(email);
//			}
//
//			if (! user.getPgpKeyIds().contains(pgpKeyId)) {
//				user.getPgpKeyIds().add(pgpKeyId);
//				final PgpKeyPgpKeyTreeItem child = new PgpKeyPgpKeyTreeItem(pgpKey);
//				pgpKeyTreeTableView.getRoot().getChildren().add(child);
//			}
//		}
//	}

	@FXML
	private void exportPgpKeyButtonClicked(final ActionEvent event) {
		final File file = showSaveFileDialog("Choose file to export PGP key(s) into");
		if (file == null)
			return;

		final Set<PgpKey> selectedPgpKeys = getSelectedPgpKeys();

		final boolean[] selectionContainsKeyWithPrivateKey = new boolean[] { false };
		selectedPgpKeys.forEach(pgpKey -> {
			selectionContainsKeyWithPrivateKey[0] |= pgpKey.isPrivateKeyAvailable();
		});

		boolean exportPublicKeysWithPrivateKeys = false;
		if (selectionContainsKeyWithPrivateKey[0]) {
			// TODO ask whether to include private keys!
		}

		if (exportPublicKeysWithPrivateKeys)
			getPgp().exportPublicKeysWithPrivateKeys(selectedPgpKeys, file);
		else
			getPgp().exportPublicKeys(selectedPgpKeys, file);
	}

	private File showSaveFileDialog(final String title) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(title);
		final java.io.File file = fileChooser.showSaveDialog(getScene().getWindow());
		return file == null ? null : createFile(file).getAbsoluteFile();
	}

	private File showOpenFileDialog(final String title) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(title);
		final java.io.File file = fileChooser.showOpenDialog(getScene().getWindow());
		return file == null ? null : createFile(file).getAbsoluteFile();
	}

	private UserRegistry getUserRegistry() {
		return UserRegistryLs.getUserRegistry();
	}

	@FXML
	private void signPgpKeyButtonClicked(final ActionEvent event) {

	}

	@FXML
	private void deletePgpKeyButtonClicked(final ActionEvent event) {

	}

	@FXML
	private void assignPgpKeyToThisUserButtonClicked(final ActionEvent event) {
		final SelectPgpKeyDialog dialog = new SelectPgpKeyDialog(getScene().getWindow(),
				new ArrayList<>(getPgp().getMasterKeys()), null, SelectionMode.MULTIPLE,
				"Please select one or more PGP keys you want to 'pull' here.");
		dialog.showAndWait();
		final List<PgpKey> selectedPgpKeys = dialog.getSelectedPgpKeys();
		if (selectedPgpKeys == null || selectedPgpKeys.isEmpty())
			return;

		final Set<PgpKeyId> selectedPgpKeyIds = new HashSet<>(selectedPgpKeys.size());
		for (final PgpKey pgpKey : selectedPgpKeys)
			selectedPgpKeyIds.add(pgpKey.getPgpKeyId());

		for (User user : getUserRegistry().getUsersByPgpKeyIds(selectedPgpKeyIds))
			user.getPgpKeyIds().removeAll(selectedPgpKeyIds);

		user.getPgpKeyIds().addAll(selectedPgpKeyIds);
	}

	@FXML
	private void assignPgpKeyToOtherUserButtonClicked(final ActionEvent event) {
		final SelectUserDialog dialog = new SelectUserDialog(getScene().getWindow(),
				new ArrayList<>(getUserRegistry().getUsers()), null, SelectionMode.SINGLE,
				"Please select the user to whom you want to 'push' the selected PGP key(s).");
		dialog.showAndWait();
		final List<User> selectedUsers = dialog.getSelectedUsers();
		if (selectedUsers == null || selectedUsers.isEmpty())
			return;

		final User targetUser = selectedUsers.get(0);
		final Set<PgpKey> selectedPgpKeys = getSelectedPgpKeys();
		for (final PgpKey pgpKey : selectedPgpKeys) {
			user.getPgpKeyIds().remove(pgpKey.getPgpKeyId());
			targetUser.getPgpKeyIds().add(pgpKey.getPgpKeyId());
		}
	}

	@FXML
	private void closeButtonClicked(final ActionEvent event) {
		editUserManager.endEditing(Collections.singleton(user));
	}

	@Override
	protected void finalize() throws Throwable {
//		user.removePropertyChangeListener(User.PropertyEnum.emails, userEmailsPropertyChangeListener);
		super.finalize();
	}
}
