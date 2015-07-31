package org.subshare.gui.localrepo.directory;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

import org.subshare.core.dto.PermissionType;
import org.subshare.core.repo.LocalRepo;
import org.subshare.core.repo.local.SsLocalRepoMetaData;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.gui.filetree.DirectoryFileTreeItem;
import org.subshare.gui.filetree.FileFileTreeItem;
import org.subshare.gui.filetree.FileTreeItem;
import org.subshare.gui.filetree.FileTreePane;
import org.subshare.gui.invitation.issue.IssueInvitationData;
import org.subshare.gui.invitation.issue.IssueInvitationWizard;
import org.subshare.gui.ls.LocalRepoManagerFactoryLs;
import org.subshare.gui.ls.RepoSyncDaemonLs;
import org.subshare.gui.ls.UserRegistryLs;
import org.subshare.gui.statusdialog.StatusDialog;
import org.subshare.gui.util.PlatformUtil;
import org.subshare.gui.wizard.WizardDialog;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncDaemon;

public class LocalRepoDirectoryPane extends VBox {

	private final LocalRepo localRepo;
	private final File file;
	private final String localPath;

	@FXML
	private TextField pathTextField;
	@FXML
	private FileTreePane fileTreePane;

	@FXML
	private CheckBox permissionsInheritedCheckBox;

	@FXML
	private TableView<UserListItem> userTableView;

	@FXML
	private Label permissionTypeLabel;

	@FXML
	private ComboBox<PermissionTypeItem> permissionTypeComboBox;

	@FXML
	private CheckBox readUserIdentityPermissionCheckBox;

	private boolean ignorePermissionTypeSelectionChange;

	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private volatile Runnable updatePermissionsInheritedDataModelRunnable;
	private volatile Runnable grantOrRevokeNormalPermissionRunnable;
	private volatile Runnable grantOrRevokeReadUserIdentityPermissionRunnable;

	private final IntegerProperty backgroundWorkCounter = new SimpleIntegerProperty(this, "backgroundWorkCounter");

	private SecureRandom random;
	private StatusDialog statusDialog;

	public LocalRepoDirectoryPane(final LocalRepo localRepo, final File file) {
		this.localRepo = assertNotNull("localRepo", localRepo);
		this.file = assertNotNull("file", file);
		this.localPath = localRepo.getLocalPath(file);
		loadDynamicComponentFxml(LocalRepoDirectoryPane.class, this);

		final String path = file.getAbsolutePath();
		pathTextField.setText(path);

		fileTreePane.setUseCase(String.format("localRepo:%s:%s", localRepo.getRepositoryId(), path)); //$NON-NLS-1$
		fileTreePane.setRootFileTreeItem(new RootDirectoryFileTreeItem(fileTreePane, file));

		userTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		userTableView.getSelectionModel().getSelectedItems().addListener((InvalidationListener) observable -> onUserTableViewSelectionChange());

		// TODO instead of disabling - which looks very ugly -, we should show a modal dialog explaining that the change is currently applied.
//		this.disableProperty().bind(backgroundWorkCounter.greaterThan(0));
		backgroundWorkCounter.addListener((InvalidationListener) observable -> showOrHideStatusDialog());

		initPermissionTypeComboBox();
		hookListeners();
		updateUi();
		onUserTableViewSelectionChange();
	}

	private void showOrHideStatusDialog() {
		if (backgroundWorkCounter.get() > 0)
			showStatusDialog();
		else
			hideStatusDialog();
	}

	private void showStatusDialog() {
		if (statusDialog == null) {
			statusDialog = new StatusDialog(getScene().getWindow(), Modality.APPLICATION_MODAL, null, null);
			statusDialog.show();
		}
	}

	private void hideStatusDialog() {
		if (statusDialog != null) {
			statusDialog.hide();
			statusDialog = null;
		}
	}

	private void initPermissionTypeComboBox() {
		permissionTypeComboBox.getItems().add(PermissionTypeItem.NONE);
		for (final PermissionType permissionType : PermissionType.values()) {
			if (PermissionType.readUserIdentity != permissionType) // managed by readUserIdentityPermissionCheckBox
				permissionTypeComboBox.getItems().add(new PermissionTypeItem(permissionType));
		}
	}

	protected void onUserTableViewSelectionChange() {
		ignorePermissionTypeSelectionChange = true;
		try {
			permissionTypeComboBox.getItems().remove(PermissionTypeItem.MIXED);

			final List<UserListItem> userListItems = filterOutOwner(userTableView.getSelectionModel().getSelectedItems());
			permissionTypeLabel.setDisable(userListItems.isEmpty());
			permissionTypeComboBox.setDisable(userListItems.isEmpty());
			readUserIdentityPermissionCheckBox.setDisable(userListItems.isEmpty());

			if (userListItems.isEmpty()) {
				permissionTypeComboBox.getSelectionModel().clearSelection();
				readUserIdentityPermissionCheckBox.setIndeterminate(true);
				return;
			}

			if (userListItems.size() > 1)
				permissionTypeComboBox.getItems().add(PermissionTypeItem.MIXED);

			final Set<Boolean> allUsersReadUserIdentityPermissionTypes = new HashSet<>();
			final Set<Set<PermissionType>> allUsersGrantedPermissionTypes = new HashSet<>();
			for (final UserListItem userListItem : userListItems) {
				Set<PermissionType> grantedPermissionTypes = userListItem.getGrantedPermissionTypes();
				allUsersReadUserIdentityPermissionTypes.add(grantedPermissionTypes.contains(PermissionType.readUserIdentity));
				grantedPermissionTypes = filterOutReadUserIdentity(grantedPermissionTypes);
				allUsersGrantedPermissionTypes.add(grantedPermissionTypes);
			}

			if (allUsersReadUserIdentityPermissionTypes.isEmpty())
				throw new IllegalStateException("WTF?!");

			if (allUsersGrantedPermissionTypes.isEmpty())
				throw new IllegalStateException("WTF?!");

			if (allUsersReadUserIdentityPermissionTypes.size() == 1) {
				readUserIdentityPermissionCheckBox.setIndeterminate(false);
				readUserIdentityPermissionCheckBox.setSelected(allUsersReadUserIdentityPermissionTypes.iterator().next());
			}
			else
				readUserIdentityPermissionCheckBox.setIndeterminate(true);

			if (allUsersGrantedPermissionTypes.size() == 1) {
				permissionTypeComboBox.getSelectionModel().clearSelection();
				final PermissionTypeItem permissionTypeItem = getPermissionTypeItem(allUsersGrantedPermissionTypes.iterator().next());
				permissionTypeComboBox.getSelectionModel().select(permissionTypeItem);
			}
			else {
				permissionTypeComboBox.getSelectionModel().clearSelection();
				permissionTypeComboBox.getSelectionModel().select(PermissionTypeItem.MIXED);
			}
		} finally {
			ignorePermissionTypeSelectionChange = false;
		}
	}

	private Set<PermissionType> filterOutReadUserIdentity(final Set<PermissionType> permissionTypes) {
		assertNotNull("permissionTypes", permissionTypes);
		final EnumSet<PermissionType> result = permissionTypes.isEmpty() ? EnumSet.noneOf(PermissionType.class) : EnumSet.copyOf(permissionTypes);
		result.remove(PermissionType.readUserIdentity);
		return result;
	}

	private PermissionTypeItem getPermissionTypeItem(final Set<PermissionType> permissionTypes) {
		if (permissionTypes.isEmpty())
			return PermissionTypeItem.NONE;

		if (permissionTypes.contains(PermissionType.grant))
			return getPermissionTypeItem(PermissionType.grant);

		if (permissionTypes.contains(PermissionType.write))
			return getPermissionTypeItem(PermissionType.write);

		if (permissionTypes.contains(PermissionType.read))
			return getPermissionTypeItem(PermissionType.read);

		throw new IllegalStateException("permissionTypes neither contains the expected content, nor is it empty: " + permissionTypes);
	}

	private PermissionTypeItem getPermissionTypeItem(final PermissionType permissionType) {
		for (PermissionTypeItem permissionTypeItem : permissionTypeComboBox.getItems()) {
			if (permissionTypeItem.getPermissionType() == permissionType)
				return permissionTypeItem;
		}
		return null;
	}

	private List<UserListItem> filterOutOwner(final List<UserListItem> userListItems) {
		assertNotNull("userListItems", userListItems);
		final List<UserListItem> result = new ArrayList<>(userListItems.size());
		for (final UserListItem userListItem : userListItems) {
			if (! userListItem.isOwner())
				result.add(userListItem);
		}
		return result;
	}

	protected void onPermissionTypeComboBoxSelectionChange() {
		PlatformUtil.assertFxApplicationThread();

		if (ignorePermissionTypeSelectionChange)
			return;

		final PermissionTypeItem permissionTypeItem = permissionTypeComboBox.getSelectionModel().getSelectedItem();
		if (PermissionTypeItem.MIXED == permissionTypeItem)
			return;

		final List<User> users = getSelectedUsers();

		incBackgroundWorkCounter();
		final Runnable runnable = new Runnable() { // must *not* be converted to lambda! using 'this'!
			@Override
			public void run() {
				try {
					if (grantOrRevokeNormalPermissionRunnable != this)
						return;

					try (final LocalRepoManager localRepoManager = createLocalRepoManager(localRepo);) {
						final SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();
						applySelectedPermissionTypeItem(localRepoManager, localRepoMetaData, users, permissionTypeItem);
					}
					Platform.runLater(() -> updateUi()); // TODO replace by listeners surveilling meta-data-changes!
				} finally {
					decBackgroundWorkCounter();
				}
			}
		};
		grantOrRevokeNormalPermissionRunnable = runnable;
		executorService.execute(runnable);
	}

	protected List<User> getSelectedUsers() {
		PlatformUtil.assertFxApplicationThread();
		final List<User> result = new ArrayList<>();
		for (final UserListItem userListItem : userTableView.getSelectionModel().getSelectedItems())
			result.add(userListItem.getUser());

		return result;
	}

	protected void onReadUserIdentityPermissionCheckBoxChange() {
		PlatformUtil.assertFxApplicationThread();

		if (ignorePermissionTypeSelectionChange)
			return;

		if (readUserIdentityPermissionCheckBox.isIndeterminate())
			return;

		final List<User> users = getSelectedUsers();

		final boolean readUserIdentityPermissionIsGranted = readUserIdentityPermissionCheckBox.isSelected();

		incBackgroundWorkCounter();
		final Runnable runnable = new Runnable() { // must *not* be converted to lambda! using this!
			@Override
			public void run() {
				try {
					if (grantOrRevokeReadUserIdentityPermissionRunnable != this)
						return;

					try (final LocalRepoManager localRepoManager = createLocalRepoManager(localRepo);) {
						final SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();
						applyReadUserIdentityPermission(localRepoManager, localRepoMetaData, users, readUserIdentityPermissionIsGranted);
					}
					Platform.runLater(() -> updateUi()); // TODO replace by listeners surveilling meta-data-changes!
				} finally {
					decBackgroundWorkCounter();
				}
			}
		};
		grantOrRevokeReadUserIdentityPermissionRunnable = runnable;
		executorService.execute(runnable);
	}

	protected void applySelectedPermissionTypeItem(final LocalRepoManager localRepoManager, final SsLocalRepoMetaData localRepoMetaData, final List<User> users, final PermissionTypeItem permissionTypeItem) {
		if (PermissionTypeItem.NONE == permissionTypeItem) {
			revokePermission(localRepoManager, localRepoMetaData, PermissionType.read, users);
			return;
		}

		final PermissionType permissionType = assertNotNull("permissionTypeItem.permissionType", permissionTypeItem.getPermissionType());
		final Set<PermissionType> includedPermissionTypes = filterOutReadUserIdentity(permissionType.getIncludedPermissionTypes());

		final UUID serverRepositoryId = getServerRepositoryId(localRepoManager);
		boolean needSync = false;

		for (final User user : users) {
			final Set<Uid> userRepoKeyIds = getUserRepoKeyIds(user, serverRepositoryId);
			final Set<PermissionType> permissionTypesToRevoke = EnumSet.noneOf(PermissionType.class);

			boolean needGrant = true;
			for (final Uid userRepoKeyId : userRepoKeyIds) {
				final Set<PermissionType> grantedPermissionTypes = filterOutReadUserIdentity(
						localRepoMetaData.getGrantedPermissionTypes(localPath, userRepoKeyId));

				if (grantedPermissionTypes.contains(permissionType))
					needGrant = false;

				for (final PermissionType pt : grantedPermissionTypes) {
					if (! includedPermissionTypes.contains(pt))
						permissionTypesToRevoke.add(pt);
				}
			}

			if (needGrant) {
				needSync = true;
				for (UserRepoKey.PublicKey publicKey : getUserRepoKeyPublicKeysForGrant(localRepoManager, user))
					localRepoMetaData.grantPermission(localPath, permissionType, publicKey);
			}

			for (PermissionType permissionTypeToRevoke : permissionTypesToRevoke) {
				needSync = true;
				localRepoMetaData.revokePermission(localPath, permissionTypeToRevoke, userRepoKeyIds);
			}
		}

		if (needSync)
			startSync();
	}

	protected void applyReadUserIdentityPermission(final LocalRepoManager localRepoManager, final SsLocalRepoMetaData localRepoMetaData, final List<User> users, final boolean readUserIdentityPermissionIsGranted) {
		if (readUserIdentityPermissionIsGranted)
			grantReadUserIdentityPermission(localRepoManager, localRepoMetaData, users);
		else
			revokePermission(localRepoManager, localRepoMetaData, PermissionType.readUserIdentity, users);
	}

	protected void grantReadUserIdentityPermission(final LocalRepoManager localRepoManager, final SsLocalRepoMetaData localRepoMetaData, final List<User> users) {
		final UUID serverRepositoryId = getServerRepositoryId(localRepoManager);

		iterateUsers: for (final User user : users) {
			for (final Uid userRepoKeyId : getUserRepoKeyIds(user, serverRepositoryId)) {
				final Set<PermissionType> grantedPermissionTypes = localRepoMetaData.getGrantedPermissionTypes(localPath, userRepoKeyId);
				if (grantedPermissionTypes.contains(PermissionType.readUserIdentity))
					continue iterateUsers;
			}

			for (UserRepoKey.PublicKey publicKey : getUserRepoKeyPublicKeysForGrant(localRepoManager, user))
				localRepoMetaData.grantPermission(localPath, PermissionType.readUserIdentity, publicKey);
		}

		startSync();
	}

	protected Collection<UserRepoKey.PublicKey> getUserRepoKeyPublicKeysForGrant(final LocalRepoManager localRepoManager, User user) {
		final UUID serverRepositoryId = getServerRepositoryId(localRepoManager);

		final List<UserRepoKey.PublicKey> permanentUserRepoKeyPublicKeys = new ArrayList<>();
		final List<UserRepoKey.PublicKey> invitationUserRepoKeyPublicKeys = new ArrayList<>();

		if (user.getUserRepoKeyRing() == null) {
			final List<? extends UserRepoKey.PublicKey> userRepoKeyPublicKeys = user.getUserRepoKeyPublicKeys(serverRepositoryId);
			for (UserRepoKey.PublicKey publicKey : userRepoKeyPublicKeys) {
				if (publicKey.isInvitation())
					invitationUserRepoKeyPublicKeys.add(publicKey);
				else
					permanentUserRepoKeyPublicKeys.add(publicKey);
			}
		}
		else {
			for (UserRepoKey userRepoKey : user.getUserRepoKeyRing().getPermanentUserRepoKeys(serverRepositoryId))
				permanentUserRepoKeyPublicKeys.add(userRepoKey.getPublicKey());

			for (UserRepoKey userRepoKey : user.getUserRepoKeyRing().getInvitationUserRepoKeys(serverRepositoryId))
				invitationUserRepoKeyPublicKeys.add(userRepoKey.getPublicKey());
		}

		if (! permanentUserRepoKeyPublicKeys.isEmpty()) {
			final int index = getRandom().nextInt(permanentUserRepoKeyPublicKeys.size());
			return Collections.singletonList(permanentUserRepoKeyPublicKeys.get(index));
		}

		if (invitationUserRepoKeyPublicKeys.isEmpty())
			throw new IllegalStateException("Neither permanent nor invitation-key found!");

		return invitationUserRepoKeyPublicKeys;
	}

	protected void revokePermission(final LocalRepoManager localRepoManager, final SsLocalRepoMetaData localRepoMetaData, PermissionType permissionType, final List<User> users) {
		final UUID serverRepositoryId = getServerRepositoryId(localRepoManager);
		final Set<Uid> userRepoKeyIds = new HashSet<Uid>();
		for (final User user : users) {
			for (final Uid userRepoKeyId : getUserRepoKeyIds(user, serverRepositoryId))
				userRepoKeyIds.add(userRepoKeyId);
		}
		localRepoMetaData.revokePermission(localPath, permissionType, userRepoKeyIds);
		startSync();
	}

	protected void hookListeners() {
		permissionsInheritedCheckBox.selectedProperty().addListener((InvalidationListener) observable -> updatePermissionsInheritedDataModel());

		permissionTypeComboBox.getSelectionModel().selectedItemProperty().addListener(
				(InvalidationListener) observable -> onPermissionTypeComboBoxSelectionChange());

		InvalidationListener invalidationListener = (InvalidationListener) -> onReadUserIdentityPermissionCheckBoxChange();
		readUserIdentityPermissionCheckBox.indeterminateProperty().addListener(invalidationListener);
		readUserIdentityPermissionCheckBox.selectedProperty().addListener(invalidationListener);
	}

	protected void updateUi() {
		updatePermissionsInheritedCheckBox();
		loadUserListItems();
	}

	protected void updatePermissionsInheritedCheckBox() {
		PlatformUtil.assertFxApplicationThread();
		incBackgroundWorkCounter();
		executorService.execute(() -> {
			try (final LocalRepoManager localRepoManager = createLocalRepoManager(localRepo);) {
				final SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();
				final boolean permissionsInherited = localRepoMetaData.isPermissionsInherited(localPath);
				Platform.runLater(() -> permissionsInheritedCheckBox.setSelected(permissionsInherited));
			} finally {
				decBackgroundWorkCounter();
			}
		});
	}

	protected void loadUserListItems() {
		PlatformUtil.assertFxApplicationThread();
		incBackgroundWorkCounter();
		executorService.execute(() -> {
			try (final LocalRepoManager localRepoManager = createLocalRepoManager(localRepo);) {
				final SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();
				final Map<User, Set<Uid>> user2UserRepoKeyIds = getUsersHavingUserRepoKey(localRepoManager);
				final List<UserListItem> userListItems = new ArrayList<>();
				for (Map.Entry<User, Set<Uid>> me : user2UserRepoKeyIds.entrySet()) {
					final User user = me.getKey();
					final Set<Uid> userRepoKeyIds = me.getValue();
					final UserListItem uli = new UserListItem(user);
					uli.getUserRepoKeyIds().addAll(userRepoKeyIds);

					if (userRepoKeyIds.contains(localRepoMetaData.getOwnerUserRepoKeyId()))
						uli.setOwner(true);
					else {
						for (Uid userRepoKeyId : userRepoKeyIds) {
							Set<PermissionType> pts = localRepoMetaData.getGrantedPermissionTypes(localPath, userRepoKeyId);
							uli.getGrantedPermissionTypes().addAll(pts);

							pts = localRepoMetaData.getEffectivePermissionTypes(localPath, userRepoKeyId);
							uli.getEffectivePermissionTypes().addAll(pts);

							pts = localRepoMetaData.getInheritedPermissionTypes(localPath, userRepoKeyId);
							uli.getInheritedPermissionTypes().addAll(pts);
						}
					}

					userListItems.add(uli);
				}

				Platform.runLater(() -> addOrUpdateUserListItems(userListItems));
			} finally {
				decBackgroundWorkCounter();
			}
		});
	}

	protected void addOrUpdateUserListItems(final List<UserListItem> userListItems) {
		assertNotNull("userListItems", userListItems);
		final Map<User, UserListItem> user2UserListItemModel = new IdentityHashMap<>();
		for (final UserListItem userListItem : userListItems)
			user2UserListItemModel.put(userListItem.getUser(), userListItem);

		final List<UserListItem> userListItemsToRemove = new ArrayList<>();
		final Map<User, UserListItem> user2UserListItemView = new IdentityHashMap<>();
		for (final UserListItem userListItem : userTableView.getItems()) {
			user2UserListItemView.put(userListItem.getUser(), userListItem);

			if (! user2UserListItemModel.containsKey(userListItem.getUser()))
				userListItemsToRemove.add(userListItem);
		}

		userTableView.getItems().removeAll(userListItemsToRemove);

		for (final UserListItem userListItemModel : userListItems) {
			UserListItem userListItemView = user2UserListItemView.get(userListItemModel.getUser());
			if (userListItemView == null)
				userTableView.getItems().add(userListItemModel);
			else
				userListItemView.copyFrom(userListItemModel);
		}
		onUserTableViewSelectionChange();
	}

	private Map<User, Set<Uid>> getUsersHavingUserRepoKey(final LocalRepoManager localRepoManager) {
		final UUID serverRepositoryId = getServerRepositoryId(localRepoManager);
		final Map<User, Set<Uid>> result = new LinkedHashMap<>();

		for (final User user : UserRegistryLs.getUserRegistry().getUsers()) {
			Set<Uid> userRepoKeyIds = getUserRepoKeyIds(user, serverRepositoryId);
			if (! userRepoKeyIds.isEmpty())
				result.put(user, userRepoKeyIds);
		}
		return result;
	}

	private Set<Uid> getUserRepoKeyIds(User user, final UUID serverRepositoryId) {
		final UserRepoKeyRing userRepoKeyRing = user.getUserRepoKeyRing();
		final Set<Uid> userRepoKeyIds = new HashSet<>();
		if (userRepoKeyRing != null) {
			final List<UserRepoKey> userRepoKeys = userRepoKeyRing.getUserRepoKeys(serverRepositoryId);
			for (UserRepoKey userRepoKey : userRepoKeys)
				userRepoKeyIds.add(userRepoKey.getUserRepoKeyId());
		}
		else {
			final List<UserRepoKey.PublicKeyWithSignature> userRepoKeyPublicKeys = user.getUserRepoKeyPublicKeys(serverRepositoryId);
			for (UserRepoKey.PublicKeyWithSignature publicKey : userRepoKeyPublicKeys)
				userRepoKeyIds.add(publicKey.getUserRepoKeyId());
		}
		return userRepoKeyIds;
	}

	private UUID getServerRepositoryId(final LocalRepoManager localRepoManager) {
		assertNotNull("localRepoManager", localRepoManager);
		final Map<UUID, URL> remoteRepositoryId2RemoteRootMap = localRepoManager.getRemoteRepositoryId2RemoteRootMap();
		final Iterator<UUID> iterator = remoteRepositoryId2RemoteRootMap.keySet().iterator();

		if (! iterator.hasNext())
			throw new IllegalStateException("remoteRepositoryId2RemoteRootMap is empty!");

		final UUID result = iterator.next();

		if (iterator.hasNext())
			throw new IllegalStateException("remoteRepositoryId2RemoteRootMap contains more than 1 entry!");

		return result;
	}

	private void incBackgroundWorkCounter() {
		Platform.runLater(() -> backgroundWorkCounter.set(backgroundWorkCounter.get() + 1));
	}

	private void decBackgroundWorkCounter() {
		Platform.runLater(() -> {
			final int newValue = backgroundWorkCounter.get() - 1;
			if (newValue < 0)
				throw new IllegalStateException("backgroundWorkCounter cannot become negative!");

			backgroundWorkCounter.set(newValue);

			if (newValue == 0) {
				updatePermissionsInheritedDataModelRunnable = null;
				grantOrRevokeNormalPermissionRunnable = null;
				grantOrRevokeReadUserIdentityPermissionRunnable = null;
			}
		});
	}

	private void updatePermissionsInheritedDataModel() {
		PlatformUtil.assertFxApplicationThread();
		final boolean newInherited = permissionsInheritedCheckBox.isSelected();
		incBackgroundWorkCounter();
		final Runnable runnable = new Runnable() { // must *not* be converted to lambda! using this!
			@Override
			public void run() {
				try {
					if (updatePermissionsInheritedDataModelRunnable != this)
						return;

					try (final LocalRepoManager localRepoManager = createLocalRepoManager(localRepo);) {
						final SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();
						localRepoMetaData.setPermissionsInherited(localPath, newInherited);
					}
					Platform.runLater(() -> updateUi()); // TODO replace by listeners surveilling meta-data-changes!
				} finally {
					decBackgroundWorkCounter();
				}
			}
		};
		updatePermissionsInheritedDataModelRunnable = runnable;
		executorService.execute(runnable);
	}

	private LocalRepoManager createLocalRepoManager(final LocalRepo localRepo) {
		return LocalRepoManagerFactoryLs.getLocalRepoManagerFactory().createLocalRepoManagerForExistingRepository(localRepo.getLocalRoot());
	}

	@FXML
	private void syncButtonClicked(final ActionEvent event) {
		startSync();
	}

	private void startSync() {
		final RepoSyncDaemon repoSyncDaemon = RepoSyncDaemonLs.getRepoSyncDaemon();
		repoSyncDaemon.startSync(localRepo.getLocalRoot());
	}

	@FXML
	private void inviteButtonClicked(final ActionEvent event) {
		final IssueInvitationWizard wizard = new IssueInvitationWizard(new IssueInvitationData(localRepo, file));
		final WizardDialog dialog = new WizardDialog(getScene().getWindow(), wizard);
		dialog.show(); // no need to wait ;-)
	}

	@FXML
	private void refreshButtonClicked(final ActionEvent event) {
		fileTreePane.refresh();
		updateUi();
	}

	private static class RootDirectoryFileTreeItem extends DirectoryFileTreeItem {
		private final FileTreePane fileTreePane;

		public RootDirectoryFileTreeItem(FileTreePane fileTreePane, File file) {
			super(file); // file is null-checked by super-constructor.
			this.fileTreePane = assertNotNull("fileTreePane", fileTreePane);
			hookUpdateInvalidationListener(fileTreePane);
		}

		@Override
		protected FileTreePane getFileTreePane() {
			return fileTreePane;
		}

		@Override
		protected List<FileTreeItem<?>> loadChildren() {
			final List<FileTreeItem<?>> children = super.loadChildren();
			children.removeIf(fti
					-> (fti instanceof FileFileTreeItem)
					&& ((FileFileTreeItem) fti).getFile().getName().equals(LocalRepoManager.META_DIR_NAME));
			return children;
		}
	}

	public synchronized SecureRandom getRandom() {
		if (random != null)
			random = new SecureRandom();

		return random;
	}
}
