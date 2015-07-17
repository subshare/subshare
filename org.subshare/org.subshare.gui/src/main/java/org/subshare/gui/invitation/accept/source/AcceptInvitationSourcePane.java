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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.util.Pair;

import org.subshare.core.file.DataFileFilter;
import org.subshare.core.file.EncryptedDataFile;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpDecoder;
import org.subshare.core.user.UserRepoInvitationToken;
import org.subshare.gui.IconSize;
import org.subshare.gui.filetree.FileTreePane;
import org.subshare.gui.invitation.accept.AcceptInvitationData;
import org.subshare.gui.ls.PgpLs;
import org.subshare.gui.severity.SeverityImageRegistry;

import co.codewizards.cloudstore.core.Severity;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.ls.client.LocalServerClient;

public abstract class AcceptInvitationSourcePane extends GridPane {
	private final AcceptInvitationData acceptInvitationData;

	private static final AtomicInteger nextThreadId = new AtomicInteger();

	private final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(final Runnable r) {
			return new Thread(AcceptInvitationSourcePane.class.getSimpleName() + '@' + nextThreadId.getAndIncrement() + ".executorServiceThread") {
				@Override
				public void run() {
					r.run();
				}
			};
		}
	});

	@FXML
	private FileTreePane fileTreePane;

	@FXML
	private HBox errorMessageBox;
	private final RowConstraints errorMessageBoxRowConstraints = new RowConstraints(0, 0, 0);

	@FXML
	private ImageView errorMessageImageView;
	@FXML
	private Label errorMessageLabel;

	private final ObjectProperty<Severity> errorSeverityProperty = new SimpleObjectProperty<Severity>(this, "errorSeverity") {
		@Override
		public void set(Severity newValue) {
			super.set(newValue);
			updateErrorMessageBox();
		}
	};
	private final StringProperty errorMessageProperty = new SimpleStringProperty(this, "errorMessage") {
		@Override
		public void set(String newValue) {
			super.set(newValue);
			updateErrorMessageBox();
		}
	};
	private final StringProperty errorLongTextProperty = new SimpleStringProperty(this, "errorLongText") {
		@Override
		public void set(String newValue) {
			super.set(newValue);
			updateErrorMessageBox();
		}
	};

	public AcceptInvitationSourcePane(final AcceptInvitationData acceptInvitationData) {
		this.acceptInvitationData = assertNotNull("acceptInvitationData", acceptInvitationData);
		loadDynamicComponentFxml(AcceptInvitationSourcePane.class, this);
		fileTreePane.fileFilterProperty().set(new DataFileFilter().setAcceptContentType(EncryptedDataFile.CONTENT_TYPE_VALUE));
		fileTreePane.getSelectedFiles().addListener((InvalidationListener) observable -> onSelectedFilesChanged());

		getRowConstraints().add(new RowConstraints());
		getRowConstraints().add(new RowConstraints());
		getRowConstraints().add(errorMessageBoxRowConstraints);

		onSelectedFilesChanged();
		updateErrorMessageBox();
	}

	private void updateErrorMessageBox() {
		Severity severity = errorSeverityProperty.get();
		String msg = errorMessageProperty.get();
		String lt = errorLongTextProperty.get();

		errorMessageImageView.setImage(SeverityImageRegistry.getInstance().getImage(severity, IconSize._24x24));
		errorMessageLabel.setText(msg);
		errorMessageLabel.setTooltip(isEmpty(lt) ? null : new Tooltip(lt));
		errorMessageBox.setVisible(! isEmpty(msg));
		errorMessageBoxRowConstraints.setMinHeight(isEmpty(msg) ? 0 : USE_COMPUTED_SIZE);
		errorMessageBoxRowConstraints.setMaxHeight(isEmpty(msg) ? 0 : USE_COMPUTED_SIZE);
		errorMessageBoxRowConstraints.setPrefHeight(isEmpty(msg) ? 0 : USE_COMPUTED_SIZE);
	}

	protected boolean isComplete() {
		return acceptInvitationData.getInvitationFile() != null;
	}

	protected void onSelectedFilesChanged() {
		File file = getSelectedFile();

		if (file != null && ! file.isFile())
			file = null;

		checkSelectedFileAsync(file);
	}

	protected File getSelectedFile() {
		final Iterator<File> selectedFilesIterator = fileTreePane.getSelectedFiles().iterator();
		return selectedFilesIterator.hasNext() ? selectedFilesIterator.next() : null;
	}

	protected void checkSelectedFileAsync(final File file) {
		errorSeverityProperty.set(Severity.INFO);
		errorLongTextProperty.set(null);
		acceptInvitationData.setInvitationFile(null);
		updateComplete();

		if (file == null) {
			errorMessageProperty.set(null);
			return;
		}
		errorMessageProperty.set("Checking the selected file...");

		executorService.submit(() -> {
			try {
				if (! file.equals(getSelectedFile()))
					return;

				final Pair<Severity, String[]> checkSelectedFileResult = checkSelectedFile(file);

				Platform.runLater(() -> {
					if (! file.equals(getSelectedFile()))
						return;

					errorSeverityProperty.set(checkSelectedFileResult == null ? Severity.INFO : checkSelectedFileResult.getKey());
					errorMessageProperty.set(checkSelectedFileResult == null ? null : checkSelectedFileResult.getValue()[0]);
					errorLongTextProperty.set(checkSelectedFileResult == null ? null : checkSelectedFileResult.getValue()[1]);
					acceptInvitationData.setInvitationFile(file);
					updateComplete();
				});
			} catch (final Exception x) {
				Platform.runLater(() -> {
					if (! file.equals(getSelectedFile()))
						return;

					errorSeverityProperty.set(Severity.ERROR);
					errorLongTextProperty.set(x.getLocalizedMessage());
					errorMessageProperty.set("Failed to decrypt the selected file!");
				});
			}
		});
	}

	private Pair<Severity, String[]> checkSelectedFile(File file) throws Exception {
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

			if (decoder.getPgpSignature() != null)
				return null;
			else {
				if (decoder.getSignPgpKeyIds().isEmpty())
					new Pair<>(Severity.ERROR, new String[] {"The invitation is not signed!", "The invitation token is not signed at all! A signature is required."});

				// TODO (1) we should use the public-key inside the invitation somehow + (2) we should check, if we have a chain of trust to the signing key!
				// PROBLEM: The signing key might not yet be known (in our key-ring)! Since it may be included in the invitation
				// and even have certifications establishing trust, this might still be valid and even totally fine!
				return new Pair<>(Severity.WARNING, new String[] { "Signature could not be verified!",
						String.format("The invitation token is signed by the PGP keys %s, but none of these PGP keys is in our key ring.\n\nIt is probably included in the invitation, but we cannot check this, now (not implemented, yet, sorry).",
								decoder.getSignPgpKeyIds()) });
			}
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
