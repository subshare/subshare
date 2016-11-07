package org.subshare.gui.pgp.certify;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpSignatureType;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

public class CertifyPgpKeyData {

	private Pgp pgp;
	private PgpKeyId pgpKeyId;
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

	private BooleanProperty skip = new SimpleBooleanProperty(this, "skip");

	public Pgp getPgp() {
		return pgp;
	}
	public void setPgp(Pgp pgp) {
		this.pgp = pgp;
	}

	public PgpKeyId getPgpKeyId() {
		return pgpKeyId;
	}
	protected void setPgpKeyId(PgpKeyId pgpKeyId) {
		if (this.pgpKeyId != null && ! this.pgpKeyId.equals(pgpKeyId))
			throw new IllegalStateException("pgpKeyId already assigned! Cannot replace!");

		this.pgpKeyId = pgpKeyId;
	}

	public PgpKey getPgpKey() {
		return pgpKey;
	}
	public void setPgpKey(final PgpKey pgpKey) {
		if (pgpKey != null)
			setPgpKeyId(assertNotNull("pgpKey.pgpKeyId", pgpKey.getPgpKeyId()));

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

	public boolean isSkip() {
		return skip.get();
	}
	public void setSkip(boolean skip) {
		this.skip.set(skip);
	}
	public BooleanProperty skipProperty() {
		return skip;
	}
}
