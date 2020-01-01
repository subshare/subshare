package org.subshare.gui;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.DateUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static java.util.Objects.*;
import static org.subshare.gui.util.ResourceBundleUtil.*;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.man.PgpPrivateKeyPassphraseStore;
import org.subshare.gui.backup.exp.ExportBackupWizard;
import org.subshare.gui.error.ErrorHandler;
import org.subshare.gui.ls.LocalServerInitLs;
import org.subshare.gui.ls.PgpLs;
import org.subshare.gui.ls.PgpPrivateKeyPassphraseManagerLs;
import org.subshare.gui.pgp.privatekeypassphrase.PgpPrivateKeyPassphrasePromptDialog;
import org.subshare.gui.splash.SplashPane;
import org.subshare.gui.util.PlatformUtil;
import org.subshare.gui.welcome.ServerWizard;
import org.subshare.gui.welcome.Welcome;
import org.subshare.gui.welcome.pgp.privatekeypassphrase.IntroWizard;
import org.subshare.gui.wizard.WizardDialog;
import org.subshare.gui.wizard.WizardState;
import org.subshare.ls.server.SsLocalServer;
import org.subshare.ls.server.cproc.SsLocalServerProcessLauncher;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import co.codewizards.cloudstore.core.appid.AppIdRegistry;
import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.updater.CloudStoreUpdaterCore;
import co.codewizards.cloudstore.core.util.DebugUtil;
import co.codewizards.cloudstore.core.util.DerbyUtil;
import co.codewizards.cloudstore.core.util.MainArgsUtil;
import co.codewizards.cloudstore.core.version.Version;
import co.codewizards.cloudstore.ls.client.LocalServerClient;
import co.codewizards.cloudstore.ls.server.LocalServer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class SubShareGui extends Application {

	private static final Logger logger = LoggerFactory.getLogger(SubShareGui.class);

	private volatile SsLocalServer localServer;
	private Stage primaryStage;
	private Stage splashStage;
	private SplashPane splashPane;
	private volatile File localServerStopFile;
	private volatile ExitCode exitCode = ExitCode.SUCCESS;
	private static final List<Image> icons;
	private volatile boolean updaterDirAlreadyCreated;
	static {
		icons = Collections.unmodifiableList(Arrays.asList(
				loadImage("subshare_16x16.png"),
				loadImage("subshare_32x32.png"),
				loadImage("subshare_48x48.png"),
				loadImage("subshare_256x256.png")
				));
	}

	private final Timer localServerStopFileTimer = new Timer("localServerStopFileTimer", true);
	private TimerTask localServerStopFileTimerTask;

	private static final Image loadImage(String fileName) {
		final URL url = SubShareGui.class.getResource(fileName);
		if (url == null)
			throw new IllegalArgumentException(String.format("Resource '%s' not found!", fileName));

		return new Image(url.toExternalForm());
	}

	@Override
	public void init() throws Exception {
		super.init();
	}

	@Override
	public void start(final Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;
		primaryStage.getIcons().addAll(icons);
		primaryStage.setTitle("subshare");

		if (isAfterUpdateHook()) {
			createUpdaterCore().getUpdaterDir().deleteRecursively();

			showUpdateDoneDialog();
			System.exit(1);
		}

		// Show splash...
		showSplash();

		// ...and do initialisation in background.
		startInitThread();
	}

	private boolean isAfterUpdateHook() {
		for (final String parameter : getParameters().getRaw()) {
			if ("afterUpdateHook".equals(parameter))
				return true;
		}
		return false;
	}

	private void showSplash() { // TODO we should *additionally* implement a Preloader, later. JavaFX has a special Preloader class.
		splashPane = new SplashPane();
		final Scene scene = new Scene(splashPane, 400, 300);
		scene.setFill(null); // needed to make it transparent ;-)
//		scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
		splashStage = new Stage(StageStyle.TRANSPARENT);
		splashStage.getIcons().addAll(icons);
		splashStage.setScene(scene);
		splashStage.setTitle("subshare");
		splashStage.show();
	}

	private void startInitThread() {
		new Thread() {
			{
				setName("Initialisation");
			}

			@Override
			public void run() {
				try {
					Thread.setDefaultUncaughtExceptionHandler(ErrorHandler.getUncaughtExceptionHandler());
					initLogging();

					final CloudStoreUpdaterCore updaterCore = createUpdaterCore();
					if (updaterCore.createUpdaterDirIfUpdateNeeded()) {
						updaterDirAlreadyCreated = true;
						PlatformUtil.runAndWait(() -> showUpdateStartingDialog(updaterCore));
						stopLater();
						return;
					}

					// We create the LocalServer before constructing the UI to make sure, the UI can access everything.
					// First we try to launch it in a separate JVM.
					new SsLocalServerProcessLauncher().start();

					// Then, as fallback, we try to launch it inside this JVM.
					localServer = new SsLocalServer();
					if (! localServer.start())
						localServer = null;

					LocalServerInitLs.initPrepare();

					// Automatically close the client, if the separate server process ends (if it actually is a separate process).
					if (LocalServerClient.getInstance().isLocalServerInSeparateProcess())
						createLocalServerStopFileTimerTask();

					final Set<PgpKeyId> pgpKeyIdsHavingPrivateKeyBeforeRestore = getIdsOfMasterKeysWithPrivateKey();
					tryPgpKeysNoPassphrase();
					PlatformUtil.runAndWait(() -> promptPgpKeyPassphrases(getWindow()));
					if (exitCode != ExitCode.SUCCESS) {
						stopLater();
						return;
					}

					final Welcome welcome = new Welcome(getWindow());
					if (! welcome.welcome()) {
						exitCode = ExitCode.WELCOME_WIZARD_ABORTED;
						stopLater();
						return;
					}

					// If private keys were restored in the Welcome process, we must redo PGP passphrase handling.
					if (!getIdsOfMasterKeysWithPrivateKey().equals(pgpKeyIdsHavingPrivateKeyBeforeRestore)) {
						tryPgpKeysNoPassphrase();
						PlatformUtil.runAndWait(() -> promptPgpKeyPassphrases(getWindow()));
					}

					PlatformUtil.runAndWait(() -> backupIfNeeded());

					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							try {
								final Parent root = FXMLLoader.load(
										SubShareGui.class.getResource("MainPane.fxml"),
										getMessages(SubShareGui.class));

								final Scene scene = new Scene(root, 800, 600);
								scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
								splashStage.hide();
								splashPane = null;

								primaryStage.setScene(scene);
								primaryStage.show();
								splashPane = null;

								LocalServerInitLs.initFinish();
							} catch (Exception x) {
								ErrorHandler.handleError(x);
								exitCode = ExitCode.EXCEPTION_CAUGHT;
								stopLater();
							}
						}
					});
				} catch (Exception x) {
					ErrorHandler.handleError(x);
					exitCode = ExitCode.EXCEPTION_CAUGHT;
					stopLater();
				}
			}
		}.start();
	}

	protected Window getWindow() {
		Stage stage = primaryStage;
		Scene scene = stage == null ? null : stage.getScene();

		if (scene == null) {
			stage = splashStage;
			scene = stage == null ? null : stage.getScene();
		}
		return scene == null ? null : scene.getWindow();
	}

	private Set<PgpKeyId> getIdsOfMasterKeysWithPrivateKey() {
		final Collection<PgpKey> masterKeysWithPrivateKey = PgpLs.getPgpOrFail().getMasterKeysWithSecretKey();
		final Set<PgpKeyId> pgpKeyIds = new HashSet<>(masterKeysWithPrivateKey.size());
		for (PgpKey pgpKey : masterKeysWithPrivateKey)
			pgpKeyIds.add(pgpKey.getPgpKeyId());

		return pgpKeyIds;
	}

	protected void stopLater() {
		Platform.runLater(() -> {
			try {
				stop();
			} catch (Exception x) {
				logger.error("stopLater: " + x, x);
			}
		});
	}

	protected void backupIfNeeded() {
		final ExportBackupWizard wizard = new ExportBackupWizard();
		if (wizard.isNeeded())
			new WizardDialog(getWindow(), wizard).showAndWait();
	}

	@Override
	public void stop() throws Exception {
		PlatformUtil.assertFxApplicationThread();

		if (! updaterDirAlreadyCreated) {
			if (exitCode == ExitCode.SUCCESS)
				backupIfNeeded();

			final CloudStoreUpdaterCore updaterCore = createUpdaterCore();
			if (updaterCore.createUpdaterDirIfUpdateNeeded())
				showUpdateStartingDialog(updaterCore);
		}

		PlatformUtil.notifyExiting();

		final LocalServer _localServer = localServer;
		localServer = null;

		super.stop();

		new Thread() {
			{
				setName(SubShareGui.class.getSimpleName() + ".StopThread");
//				setDaemon(true); // commented <= must *not* be a daemon thread!
			}

			@Override
			public void run() {
				LocalServerClient.getInstance().close();
				if (_localServer != null)
					_localServer.stop();

				try {
					Thread.sleep(1000L);
				} catch (InterruptedException e) { doNothing(); }

				System.exit(exitCode.getNumericCode());
			}

		}.start();
	}

	private CloudStoreUpdaterCore createUpdaterCore() {
		return new CloudStoreUpdaterCore();
	}

	public static void main(String[] args) {
		args = MainArgsUtil.extractAndApplySystemPropertiesReturnOthers(args);
		launch(args);
	}

	private void tryPgpKeysNoPassphrase() throws InterruptedException {
		final Pgp pgp = PgpLs.getPgpOrFail();
		final PgpPrivateKeyPassphraseStore pgpPrivateKeyPassphraseStore = PgpPrivateKeyPassphraseManagerLs.getPgpPrivateKeyPassphraseStore();
		final Date now = now();

		for (final PgpKey pgpKey : pgp.getMasterKeysWithSecretKey()) {
			if (! pgpKey.isValid(now))
				continue;

			final PgpKeyId pgpKeyId = pgpKey.getPgpKeyId();
			if (pgpPrivateKeyPassphraseStore.hasPassphrase(pgpKeyId))
				continue;

			// We try an empty password to prevent a dialog from popping up, if the PGP key is not passphrase-protected.

			// To prevent log pollution as well as speeding this up (LocalServer-RPC does retries in case of *all* exceptions),
			// I first invoke testPassphrase(...) (even though this would not be necessary).
			if (pgp.testPassphrase(pgpKey, new char[0])) {
				try {
					pgpPrivateKeyPassphraseStore.putPassphrase(pgpKeyId, new char[0]);
					// successful => next PGP key
				} catch (Exception x) {
					doNothing();
				}
			}
		}
	}

	private void promptPgpKeyPassphrases(Window owner) {
		final boolean serverWizardIsNeeded = new ServerWizard(true, true).isNeeded();
		boolean introAlreadyShown = false;

		final Pgp pgp = PgpLs.getPgpOrFail();
		final PgpPrivateKeyPassphraseStore pgpPrivateKeyPassphraseStore = PgpPrivateKeyPassphraseManagerLs.getPgpPrivateKeyPassphraseStore();
		final Date now = now();

		for (final PgpKey pgpKey : pgp.getMasterKeysWithSecretKey()) {
			if (pgpKey.isRevoked() || !pgpKey.isValid(now))
				continue;

			final PgpKeyId pgpKeyId = pgpKey.getPgpKeyId();
			if (pgpPrivateKeyPassphraseStore.hasPassphrase(pgpKeyId))
				continue;

			if (serverWizardIsNeeded && ! introAlreadyShown) {
				// Show the user a nice introductory explaining why we're going to ask for the PGP key passphrase.
				introAlreadyShown = true;
				final IntroWizard introWizard = new IntroWizard();
				new WizardDialog(owner, introWizard).showAndWait();
				if (introWizard.getState() == WizardState.CANCELLED) {
					exitCode = ExitCode.INTRO_WIZARD_ABORTED;
					return;
				}
			}

			boolean retry = false;
			String errorMessage = null;
			do {
				final PgpPrivateKeyPassphrasePromptDialog dialog = new PgpPrivateKeyPassphrasePromptDialog(owner, pgpKey, errorMessage);
				dialog.showAndWait();

				retry = false;
				errorMessage = null;

				final char[] passphrase = dialog.getPassphrase();
				if (passphrase != null) {
					try {
						pgpPrivateKeyPassphraseStore.putPassphrase(pgpKeyId, passphrase);
					} catch (SecurityException x) {
						logger.error("promptPgpKeyPassphrases: " + x, x);
						retry = true;
						errorMessage = "Sorry, the passphrase you entered is wrong! Please try again.";
					} catch (Exception x) {
						ErrorHandler.handleError(x);
					}
				}
			} while (retry);
		}
	}

	private static void initLogging() throws IOException, JoranException {
		final File logDir = ConfigDir.getInstance().getLogDir();
		DerbyUtil.setLogFile(createFile(logDir, "derby.log"));

		final String logbackXmlName = "logback.client.xml";
		final File logbackXmlFile = createFile(ConfigDir.getInstance().getFile(), logbackXmlName);
		if (!logbackXmlFile.exists()) {
			AppIdRegistry.getInstance().copyResourceResolvingAppId(
					SubShareGui.class, logbackXmlName, logbackXmlFile);
		}

		final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		try {
		  final JoranConfigurator configurator = new JoranConfigurator();
		  configurator.setContext(context);
		  // Call context.reset() to clear any previous configuration, e.g. default
		  // configuration. For multi-step configuration, omit calling context.reset().
		  context.reset();
		  configurator.doConfigure(logbackXmlFile.getIoFile());
		} catch (final JoranException je) {
			// StatusPrinter will handle this
			doNothing();
		}
		StatusPrinter.printInCaseOfErrorsOrWarnings(context);
		DebugUtil.logSystemProperties();
	}

	private void showUpdateStartingDialog(final CloudStoreUpdaterCore updaterCore) {
		requireNonNull(updaterCore, "updaterCore");
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setHeaderText("Update to a new Subshare version!");

		final String text = String.format("You are currently using Subshare version %s.\n\nThe new version %s is available and is going to be installed, now.\n\nThe update might take a few minutes - please be patient!",
				updaterCore.getLocalVersion(), updaterCore.getRemoteVersion());

//		alert.setContentText(text);
		// The above does not adjust the dialog size :-( Using a Text node instead works better.

		final Text contentText = new Text(text);
		final HBox contentTextContainer = new HBox();
		contentTextContainer.getChildren().add(contentText);

		GridPane.setMargin(contentText, new Insets(8));
		alert.getDialogPane().setContent(contentTextContainer);

		alert.showAndWait();
	}

	private void showUpdateDoneDialog() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setHeaderText("Update to a new Subshare version done!");

		final Version localVersion = new CloudStoreUpdaterCore().getLocalVersion();

		final String text = String.format("Subshare was updated to version %s.",
				localVersion);

		final Text contentText = new Text(text);
		final HBox contentTextContainer = new HBox();
		contentTextContainer.getChildren().add(contentText);

		GridPane.setMargin(contentText, new Insets(8));
		alert.getDialogPane().setContent(contentTextContainer);

		alert.showAndWait();
	}

	protected File getLocalServerStopFile() {
		if (localServerStopFile == null)
			localServerStopFile = createFile(ConfigDir.getInstance().getFile(), "localServerRunning.deleteToStop");

		return localServerStopFile;
	}

	private void createLocalServerStopFileTimerTask() {
		final File localServerStopFile = getLocalServerStopFile();
		if (! localServerStopFile.exists())
			logger.warn("localServerStopFileTimerTask.run: file '{}' does not exist during GUI client startup!", localServerStopFile);

		synchronized (localServerStopFileTimer) {
			cancelLocalServerStopFileTimerTask();

			localServerStopFileTimerTask = new TimerTask() {
				@Override
				public void run() {
					if (localServerStopFile.exists()) {
						logger.debug("localServerStopFileTimerTask.run: file '{}' exists => nothing to do.", localServerStopFile);
						return;
					}
					logger.info("localServerStopFileTimerTask.run: file '{}' does not exist => stopping server!", localServerStopFile);

					exitCode = ExitCode.LOCAL_SERVER_STOPPED;
					stopLater();
				}
			};

			final long period = 5000L;
			localServerStopFileTimer.schedule(localServerStopFileTimerTask, period, period);
		}
	}

	private void cancelLocalServerStopFileTimerTask() {
		synchronized (localServerStopFileTimer) {
			if (localServerStopFileTimerTask != null) {
				localServerStopFileTimerTask.cancel();
				localServerStopFileTimerTask = null;
			}
		}
	}
}
