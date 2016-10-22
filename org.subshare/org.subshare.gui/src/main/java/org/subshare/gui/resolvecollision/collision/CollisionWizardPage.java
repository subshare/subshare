package org.subshare.gui.resolvecollision.collision;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.gui.wizard.WizardPage;

import javafx.scene.Parent;

public class CollisionWizardPage extends WizardPage {

	private final CollisionData collisionData;

	public CollisionWizardPage(final CollisionData collisionData) {
		super("Collision");
		this.collisionData = assertNotNull("collisionData", collisionData);
	}

	@Override
	protected Parent createContent() {
		return new CollisionPane(collisionData);
	}
}
