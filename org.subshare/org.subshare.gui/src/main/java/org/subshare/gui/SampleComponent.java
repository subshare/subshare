package org.subshare.gui;

import java.io.IOException;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;

public class SampleComponent extends BorderPane {

	public SampleComponent() {
		final FXMLLoader fxmlLoader = new FXMLLoader(
				SampleComponent.class.getResource("Sample.fxml"),
				ResourceBundle.getBundle("org.subshare.gui.messages"));

		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@FXML
	protected void clicked(final ActionEvent event) {
		System.out.println("clicked! " + event);
	}
}
