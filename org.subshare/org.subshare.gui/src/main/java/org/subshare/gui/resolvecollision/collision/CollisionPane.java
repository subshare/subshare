package org.subshare.gui.resolvecollision.collision;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.util.Date;

import org.subshare.core.dto.CollisionDto;
import org.subshare.core.repo.LocalRepo;
import org.subshare.gui.ls.LocalRepoManagerFactoryLs;
import org.subshare.gui.resolvecollision.CollisionDtoWithPlainHistoCryptoRepoFileDto;

import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.GridPane;

public class CollisionPane extends GridPane {

	private final CollisionData collisionData;

	@FXML
	private CheckBox resolvedCheckBox;

//	@FXML
//	private TextArea commentTextArea;

	public CollisionPane(final CollisionData collisionData) {
		loadDynamicComponentFxml(CollisionPane.class, this);
		this.collisionData = assertNotNull("collisionData", collisionData);

		final CollisionDtoWithPlainHistoCryptoRepoFileDto collisionDtoWithPlainHistoCryptoRepoFileDto = collisionData.getCollisionDtoWithPlainHistoCryptoRepoFileDto();
		final CollisionDto collisionDto = collisionDtoWithPlainHistoCryptoRepoFileDto.getCollisionDto();

		resolvedCheckBox.setSelected(collisionDto.getResolved() != null);
		resolvedCheckBox.selectedProperty().addListener((InvalidationListener) observable -> updateResolved());

//		commentTextArea.setText(collisionDto.getComment());
//		resolvedCheckBox.textProperty().addListener((InvalidationListener) observable -> updateComment());
	}

	private LocalRepoManager createLocalRepoManager() {
		final LocalRepo localRepo = assertNotNull("collisionData.localRepo", collisionData.getLocalRepo());
		return LocalRepoManagerFactoryLs.getLocalRepoManagerFactory().createLocalRepoManagerForExistingRepository(localRepo.getLocalRoot());
	}

	private void updateResolved() {
		final CollisionDtoWithPlainHistoCryptoRepoFileDto collisionDtoWithPlainHistoCryptoRepoFileDto = collisionData.getCollisionDtoWithPlainHistoCryptoRepoFileDto();
		final CollisionDto collisionDto = collisionDtoWithPlainHistoCryptoRepoFileDto.getCollisionDto();

		final boolean newResolved = resolvedCheckBox.selectedProperty().get();
		if (newResolved)
			collisionDto.setResolved(new Date());
		else
			collisionDto.setResolved(null);
	}

//	private void updateComment() {
//		final String newComment = commentTextArea.getText();
//		collisionData.getCollisionDto().setComment(newComment);
//	}

	@Override
	public void requestFocus() {
		super.requestFocus();
		resolvedCheckBox.requestFocus();
	}
}
