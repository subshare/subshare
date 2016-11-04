package org.subshare.gui.pgp.certify;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpSignatureType;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class CertifyPgpKeyData {

	private PgpKey pgpKey;

	private ObjectProperty<PgpSignatureType> certificationLevel = new SimpleObjectProperty<PgpSignatureType>(this, "certificationLevel") {
		@Override
		public void set(PgpSignatureType newValue) {
			super.set(assertNotNull("newValue", newValue));
		}
	};
	{
		certificationLevel.set(PgpSignatureType.DEFAULT_CERTIFICATION);
	}

	private final ObjectProperty<PgpKey> signPgpKey = new SimpleObjectProperty<>(this, "signPgpKey");

	public PgpKey getPgpKey() {
		return pgpKey;
	}

	public void setPgpKey(PgpKey pgpKey) {
		if (this.pgpKey != null && this.pgpKey != pgpKey)
			throw new IllegalStateException("pgpKey already assigned! Cannot replace!");

		this.pgpKey = pgpKey;
	}

	public PgpSignatureType getCertificationLevel() {
		return certificationLevel.get();
	}
	public void setCertificationLevel(PgpSignatureType certificationLevel) {
		this.certificationLevel.set(certificationLevel);
	}
	public ObjectProperty<PgpSignatureType> certificationLevelProperty() {
		return certificationLevel;
	}

	public PgpKey getSignPgpKey() {
		return signPgpKey.get();
	}
	public void setSignPgpKey(PgpKey signPgpKey) {
		this.signPgpKey.set(signPgpKey);
	}
	public ObjectProperty<PgpKey> signPgpKeyProperty() {
		return signPgpKey;
	}
}
