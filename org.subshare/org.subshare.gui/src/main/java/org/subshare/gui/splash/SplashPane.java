package org.subshare.gui.splash;

import static org.subshare.gui.util.FxmlUtil.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

public class SplashPane extends BorderPane {

	@FXML
	private ImageView imageView;

	@FXML
	private Image image;

	public SplashPane() {
		loadDynamicComponentFxml(SplashPane.class, this);

		widthProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(final ObservableValue<? extends Number> observable, final Number oldValue, final Number newValue) {
				imageView.setFitWidth(getWidth());
			}
		});
	}

}
