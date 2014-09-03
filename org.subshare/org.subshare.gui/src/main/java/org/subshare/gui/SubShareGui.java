package org.subshare.gui;

import static org.subshare.gui.util.ResourceBundleUtil.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SubShareGui extends Application {

	@Override
	public void start(final Stage primaryStage) throws Exception {
		final Parent root = FXMLLoader.load(
				SubShareGui.class.getResource("MainPane.fxml"),
				getMessages(SubShareGui.class));

		final Scene scene = new Scene(root, 800, 600);
		scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
		primaryStage.setScene(scene);
		primaryStage.setTitle("SubShare");
		primaryStage.show();
	}

	public static void main(final String[] args) {
		launch(args);
	}
}
