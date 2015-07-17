package org.subshare.gui.invitation.accept.source;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static org.subshare.core.file.FileConst.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;

import org.subshare.core.file.DataFileFilter;
import org.subshare.core.file.EncryptedDataFile;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpDecoder;
import org.subshare.core.user.UserRepoInvitationToken;
import org.subshare.gui.filetree.FileTreePane;
import org.subshare.gui.invitation.accept.AcceptInvitationData;
import org.subshare.gui.ls.PgpLs;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.ls.client.LocalServerClient;

public abstract class AcceptInvitationSourcePane extends GridPane {
	private final AcceptInvitationData acceptInvitationData;

	@FXML
	private FileTreePane fileTreePane;

	@FXML
	private HBox errorMessageBox;
	private final RowConstraints errorMessageBoxRowConstraints = new RowConstraints(0, 0, 0);

	@FXML
	private ImageView errorMessageImageView;
	@FXML
	private Label errorMessageLabel;

	private final StringProperty errorMessageProperty = new SimpleStringProperty(this, "errorMessage") {
		@Override
		public void set(String newValue) {
			super.set(newValue);
			updateErrorMessageLabelText();
		}
	};
	private final StringProperty errorLongTextProperty = new SimpleStringProperty(this, "errorLongText") {
		@Override
		public void set(String newValue) {
			super.set(newValue);
			updateErrorMessageLabelText();
		}
	};
	private final StringProperty warningMessageProperty = new SimpleStringProperty(this, "warningMessage") {
		@Override
		public void set(String newValue) {
			super.set(newValue);
			updateErrorMessageLabelText();
		}
	};
	private final StringProperty warningLongTextProperty = new SimpleStringProperty(this, "warningLongText") {
		@Override
		public void set(String newValue) {
			super.set(newValue);
			updateErrorMessageLabelText();
		}
	};

	private void updateErrorMessageLabelText() {
		String msg = errorMessageProperty.get();
		String lt = errorLongTextProperty.get();
		if (isEmpty(msg)) {
			msg = warningMessageProperty.get();
			lt = warningLongTextProperty.get();
		}

		errorMessageLabel.setText(msg);
		errorMessageLabel.setTooltip(isEmpty(lt) ? null : new Tooltip(lt));
		errorMessageBox.setVisible(! isEmpty(msg));
		errorMessageBoxRowConstraints.setMinHeight(isEmpty(msg) ? 0 : USE_COMPUTED_SIZE);
		errorMessageBoxRowConstraints.setMaxHeight(isEmpty(msg) ? 0 : USE_COMPUTED_SIZE);
		errorMessageBoxRowConstraints.setPrefHeight(isEmpty(msg) ? 0 : USE_COMPUTED_SIZE);
	}

	public AcceptInvitationSourcePane(final AcceptInvitationData acceptInvitationData) {
		this.acceptInvitationData = assertNotNull("acceptInvitationData", acceptInvitationData);
		loadDynamicComponentFxml(AcceptInvitationSourcePane.class, this);
		fileTreePane.fileFilterProperty().set(new DataFileFilter().setAcceptContentType(EncryptedDataFile.CONTENT_TYPE_VALUE));
		fileTreePane.getSelectedFiles().addListener((InvalidationListener) observable -> onSelectedFilesChanged());
		getRowConstraints().add(0, new RowConstraints());
		getRowConstraints().add(1, errorMessageBoxRowConstraints);
		onSelectedFilesChanged();
		updateErrorMessageLabelText();
	}

	protected boolean isComplete() {
		return acceptInvitationData.getInvitationFile() != null;
	}

	protected void onSelectedFilesChanged() {
		final Iterator<File> selectedFilesIterator = fileTreePane.getSelectedFiles().iterator();
		File file = selectedFilesIterator.hasNext() ? selectedFilesIterator.next() : null;

		if (file != null && ! file.isFile())
			file = null;

		if (file != null) {
			// TODO we should check, if we have a chain of trust to the signing key!
			// PROBLEM: The signing key might not yet be known! Since it may be included and even have certifications
			// establishing trust, this might still be valid and even totally fine!
			try {
				decrypt(file);
				errorLongTextProperty.set(null);
				errorMessageProperty.set(null);
			} catch (Exception x) {
				file = null;
				errorLongTextProperty.set(x.getLocalizedMessage());
				errorMessageProperty.set("Failed to decrypt the selected file!");
			}
		}

		acceptInvitationData.setInvitationFile(file);
		updateComplete();
	}

	private void decrypt(File file) throws Exception {
		final LocalServerClient lsc = LocalServerClient.getInstance();
		final Pgp pgp = PgpLs.getPgpOrFail();
		try (InputStream in = file.createInputStream();) {
			final EncryptedDataFile encryptedDataFile = new EncryptedDataFile(in);
			byte[] defaultData = encryptedDataFile.getDefaultData();
			if (defaultData == null)
				throw new IllegalArgumentException("No 'defaultData' item found in file!");

			Object defaultDataIn = lsc.invokeConstructor(ByteArrayInputStream.class, defaultData);
			Object decryptedOut = lsc.invokeConstructor(ByteArrayOutputStream.class);
			PgpDecoder decoder = lsc.invoke(pgp, "createDecoder", defaultDataIn, decryptedOut);
			decoder.setFailOnMissingSignPgpKey(false);
			decoder.decode();
			final byte[] decrypted = lsc.invoke(decryptedOut, "toByteArray"); //$NON-NLS-1$
			final ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(decrypted));
			// We expect the very first entry to be the MANIFEST.properties!
			readManifest(zin);
		}
	}

	private Properties readManifest(final ZipInputStream zin) throws IOException {
		assertNotNull("zin", zin);

		final ZipEntry ze = zin.getNextEntry();
		if (ze == null)
			throw new IllegalArgumentException(String.format("userRepoInvitationData is not valid: It lacks the '%s' as very first zip-entry (there is no first ZipEntry)!", MANIFEST_PROPERTIES_FILE_NAME));

		if (!MANIFEST_PROPERTIES_FILE_NAME.equals(ze.getName()))
			throw new IllegalArgumentException(String.format("userRepoInvitationData is not valid: The very first zip-entry is not '%s' (it is '%s' instead)!", MANIFEST_PROPERTIES_FILE_NAME, ze.getName()));

		final Properties properties = new Properties();
		properties.load(zin);

		final String contentType = properties.getProperty(MANIFEST_PROPERTY_CONTENT_TYPE);
		if (!UserRepoInvitationToken.CONTENT_TYPE_USER_REPO_INVITATION.equals(contentType))
			throw new IllegalArgumentException(String.format("userRepoInvitationData is not valid: The manifest indicates the content-type '%s', but '%s' is expected!", contentType, UserRepoInvitationToken.CONTENT_TYPE_USER_REPO_INVITATION));

		return properties;
	}

	protected abstract void updateComplete();

	@Override
	public void requestFocus() {
		super.requestFocus();
		fileTreePane.requestFocus();
	}
}
