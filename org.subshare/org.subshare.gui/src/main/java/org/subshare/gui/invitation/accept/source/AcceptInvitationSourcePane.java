package org.subshare.gui.invitation.accept.source;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static org.subshare.core.file.FileConst.*;
import static org.subshare.gui.util.FxmlUtil.*;
import static org.subshare.gui.util.PlatformUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.subshare.core.file.DataFileFilter;
import org.subshare.core.file.EncryptedDataFile;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpDecoder;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpKeyValidity;
import org.subshare.core.user.UserRepoInvitationToken;
import org.subshare.gui.IconSize;
import org.subshare.gui.filetree.FileTreePane;
import org.subshare.gui.invitation.accept.AcceptInvitationData;
import org.subshare.gui.ls.PgpLs;
import org.subshare.gui.severity.SeverityImageRegistry;
import org.subshare.gui.wizard.WizardPageContentGridPane;

import co.codewizards.cloudstore.core.Severity;
import co.codewizards.cloudstore.core.io.ByteArrayInputStream;
import co.codewizards.cloudstore.core.io.ByteArrayOutputStream;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.ls.client.LocalServerClient;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;

public class AcceptInvitationSourcePane extends WizardPageContentGridPane {
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

	private static List<Class<? extends ProblemSolver>> problemSolverClasses = Arrays.asList(
			ImportSigningKeyProblemSolver.class
	);

	@FXML
	private FileTreePane fileTreePane;

	@FXML
	private HBox statusMessageBox;
	private final RowConstraints statusMessageBoxRowConstraints = new RowConstraints(0, 0, 0);

	@FXML
	private Label statusMessageLabel;

	@FXML
	private Button solveProblemButton;

	private ProblemSolver problemSolver;

	private final ObjectProperty<CheckInvitationFileResult> checkInvitationFileResult = new SimpleObjectProperty<CheckInvitationFileResult>(this, "checkInvitationFileResult") {
		@Override
		public void set(CheckInvitationFileResult newValue) {
			assertFxApplicationThread();
			super.set(newValue);
			problemSolver = determineProblemSolver();
			updateStatusMessageBox();
		}
	};

//	private final ObjectProperty<Severity> errorSeverityProperty = new SimpleObjectProperty<Severity>(this, "errorSeverity") {
//		@Override
//		public void set(Severity newValue) {
//			super.set(newValue);
//
//		}
//	};
//	private final StringProperty errorMessageProperty = new SimpleStringProperty(this, "errorMessage") {
//		@Override
//		public void set(String newValue) {
//			super.set(newValue);
//			updateStatusMessageBox();
//		}
//	};
//	private final StringProperty errorLongTextProperty = new SimpleStringProperty(this, "errorLongText") {
//		@Override
//		public void set(String newValue) {
//			super.set(newValue);
//			updateStatusMessageBox();
//		}
//	};

	public AcceptInvitationSourcePane(final AcceptInvitationData acceptInvitationData) {
		this.acceptInvitationData = assertNotNull(acceptInvitationData, "acceptInvitationData");
		loadDynamicComponentFxml(AcceptInvitationSourcePane.class, this);
		fileTreePane.fileFilterProperty().set(new DataFileFilter().setAcceptContentType(EncryptedDataFile.CONTENT_TYPE_VALUE));
		fileTreePane.getSelectedFiles().addListener((InvalidationListener) observable -> onSelectedFilesChanged());

		getRowConstraints().add(new RowConstraints());
		getRowConstraints().add(new RowConstraints());
		getRowConstraints().add(statusMessageBoxRowConstraints);

		onSelectedFilesChanged();
		updateStatusMessageBox();
	}

	private void updateStatusMessageBox() {
		final CheckInvitationFileResult cifResult = checkInvitationFileResult.get();
		final Severity severity = cifResult == null ? Severity.INFO : cifResult.getSeverity();
		final String msg = cifResult == null ? null : cifResult.getMessage();
		final String lt = cifResult == null ? null : cifResult.getLongText();
		final Image severityImage = SeverityImageRegistry.getInstance().getImage(severity, IconSize._24x24);
		statusMessageLabel.setGraphic(severityImage == null ? null : new ImageView(severityImage));
		statusMessageLabel.setText(msg);
		statusMessageLabel.setTooltip(isEmpty(lt) ? null : new Tooltip(lt));
		statusMessageBox.setVisible(! isEmpty(msg));
		statusMessageBoxRowConstraints.setMinHeight(isEmpty(msg) ? 0 : USE_COMPUTED_SIZE);
		statusMessageBoxRowConstraints.setMaxHeight(isEmpty(msg) ? 0 : USE_COMPUTED_SIZE);
		statusMessageBoxRowConstraints.setPrefHeight(isEmpty(msg) ? 0 : USE_COMPUTED_SIZE);

		if (problemSolver == null)
			statusMessageBox.getChildren().remove(solveProblemButton);
		else
			statusMessageBox.getChildren().add(solveProblemButton);
	}

	protected List<ProblemSolver> createProblemSolvers() {
		final List<ProblemSolver> problemSolvers = new ArrayList<>(problemSolverClasses.size());
		for (final Class<? extends ProblemSolver> problemSolverClass : problemSolverClasses) {
			final ProblemSolver problemSolver;
			try {
				problemSolver = problemSolverClass.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			problemSolver.setInvitationFile(getSelectedFile());
			problemSolver.setCheckInvitationFileResult(getCheckInvitationFileResult());
			problemSolvers.add(problemSolver);
		}

		Collections.sort(problemSolvers, new Comparator<ProblemSolver>() {
			@Override
			public int compare(ProblemSolver o1, ProblemSolver o2) {
				int res = -1 * Integer.compare(o1.getPriority(), o2.getPriority()); // highest priority first!
				if (res != 0)
					return res;

				res = o1.getClass().getName().compareTo(o2.getClass().getName());
				return res;
			}
		});

		return problemSolvers;
	}

	protected ProblemSolver determineProblemSolver() {
		if (getSelectedFile() == null || getCheckInvitationFileResult() == null)
			return null;

		for (ProblemSolver problemSolver : createProblemSolvers()) {
			if (problemSolver.canSolveProblem())
				return problemSolver;
		};
		return null;
	}

	@Override
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
		acceptInvitationData.setInvitationFile(null);
		updateComplete();

		if (file == null) {
			setCheckInvitationFileResult(null);
			return;
		}
		setCheckInvitationFileResult(new CheckInvitationFileResult(CheckInvitationFileResult.Type.OK, Severity.INFO,
				"Checking the selected file...", null));

		executorService.submit(() -> {
			try {
				if (! file.equals(getSelectedFile()))
					return;

				final CheckInvitationFileResult checkInvitationFileResult = checkSelectedFile(file);

				Platform.runLater(() -> {
					if (! file.equals(getSelectedFile()))
						return;

					if (CheckInvitationFileResult.Type.ERROR_GENERAL.compareTo(checkInvitationFileResult.getType()) > 0)
						acceptInvitationData.setInvitationFile(file);

					setCheckInvitationFileResult(checkInvitationFileResult);
					updateComplete();
				});
			} catch (final Exception x) {
				Platform.runLater(() -> {
					if (! file.equals(getSelectedFile()))
						return;

					setCheckInvitationFileResult(
							new CheckInvitationFileResult(CheckInvitationFileResult.Type.ERROR_GENERAL, Severity.ERROR,
									"Failed to decrypt the selected file!",
									x.getLocalizedMessage()));
				});
			}
		});
	}

	public CheckInvitationFileResult getCheckInvitationFileResult() {
		return checkInvitationFileResult.get();
	}

	public void setCheckInvitationFileResult(CheckInvitationFileResult checkInvitationFileResult) {
		this.checkInvitationFileResult.set(checkInvitationFileResult);
	}

	private CheckInvitationFileResult checkSelectedFile(File file) throws Exception {
		final LocalServerClient lsc = LocalServerClient.getInstance();
		final Pgp pgp = PgpLs.getPgpOrFail();
		try (InputStream in = castStream(file.createInputStream())) {
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

			if (decoder.getPgpSignature() != null) {
				final PgpKeyId pgpKeyId = assertNotNull(decoder.getPgpSignature().getPgpKeyId(), "pgpSignature.pgpKeyId");
				final PgpKey pgpKey = assertNotNull(pgp.getPgpKey(pgpKeyId), "pgp.getPgpKey(" + pgpKeyId + ")");
				final String primaryUserId = pgpKey.getUserIds().isEmpty() ? "<<unknown>>" : pgpKey.getUserIds().get(0);

				final PgpKeyValidity keyValidity = pgp.getKeyValidity(pgpKey);
				switch (keyValidity) {
					case EXPIRED:
						return new CheckInvitationFileResult(CheckInvitationFileResult.Type.SIGNING_KEY_EXPIRED, Severity.ERROR,
								"Signing key expired!",
								String.format("The key '%s' (%s), which was used to sign this invitation, already expired!",
										pgpKeyId.toHumanString(), primaryUserId));

					case REVOKED:
						return new CheckInvitationFileResult(CheckInvitationFileResult.Type.SIGNING_KEY_REVOKED, Severity.ERROR,
								"Signing key revoked!",
								String.format("The key '%s' (%s), which was used to sign this invitation, was revoked!",
										pgpKeyId.toHumanString(), primaryUserId));

					case NOT_TRUSTED:
						return new CheckInvitationFileResult(CheckInvitationFileResult.Type.SIGNING_KEY_NOT_TRUSTED, Severity.ERROR,
								"Signing key not trusted!",
								String.format("The key '%s' (%s), which was used to sign this invitation, is not trusted!",
										pgpKeyId.toHumanString(), primaryUserId));

					case DISABLED:
						return new CheckInvitationFileResult(CheckInvitationFileResult.Type.SIGNING_KEY_DISABLED, Severity.ERROR,
								"Signing key disabled!",
								String.format("The key '%s' (%s), which was used to sign this invitation, is disabled!",
										pgpKeyId.toHumanString(), primaryUserId));

					case MARGINAL:
						return new CheckInvitationFileResult(CheckInvitationFileResult.Type.SIGNING_KEY_MARGINALLY_TRUSTED, Severity.INFO,
								"Signing key only marginally trusted!",
								String.format("The key '%s' (%s), which was used to sign this invitation, is only marginally trusted!",
										pgpKeyId.toHumanString(), primaryUserId));

					case FULL:
					case ULTIMATE:
						return new CheckInvitationFileResult(CheckInvitationFileResult.Type.OK, Severity.INFO);

					default :
						return new CheckInvitationFileResult(CheckInvitationFileResult.Type.SIGNING_KEY_UNKNOWN_VALIDITY, Severity.WARNING,
								String.format("Unknown key-validity: %s", keyValidity), null);
				}
			}
			else {
				if (decoder.getSignPgpKeyIds().isEmpty())
					return new CheckInvitationFileResult(CheckInvitationFileResult.Type.ERROR_SIGNATURE_MISSING, Severity.ERROR,
							"The invitation is not signed!",
							"The invitation token is not signed at all! A signature is required.");

				return new CheckInvitationFileResult(CheckInvitationFileResult.Type.SIGNING_KEY_MISSING, Severity.WARNING,
						"Signature could not be verified!",
						String.format("The invitation token is signed by the PGP keys %s, but none of these PGP keys is in our key ring.\n\nIt is probably included in the invitation, but we cannot check this, now (not implemented, yet, sorry).",
								decoder.getSignPgpKeyIds()));
			}
		}
	}

	private Properties readManifest(final ZipInputStream zin) throws IOException {
		assertNotNull(zin, "zin");

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

	@Override
	public void requestFocus() {
		super.requestFocus();
		fileTreePane.requestFocus();
	}

	@FXML
	private void solveProblemButtonClicked(final ActionEvent event) {
		assertNotNull(problemSolver, "problemSolver");
		problemSolver.setWindow(assertNotNull(getScene().getWindow(), "scene.window"));
		problemSolver.solveProblem();
		onSelectedFilesChanged();
	}
}
