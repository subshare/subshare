package org.subshare.gui;

import java.util.ResourceBundle;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class SubShareGui extends Application {

	@Override
	public void start(final Stage primaryStage) throws Exception {
		try {
			final GridPane root = FXMLLoader.load(
					SubShareGui.class.getResource("Example1Pane.fxml"),
					ResourceBundle.getBundle("org.subshare.gui.messages"));

			final Scene scene = new Scene(root,400,400);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.setTitle("Hello world!");
			primaryStage.show();
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(final String[] args) {
		launch(args);
	}
}
