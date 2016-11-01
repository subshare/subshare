package org.subshare.core.pgp;

import java.util.Collections;
import java.util.Set;

import co.codewizards.cloudstore.core.io.IInputStream;
import co.codewizards.cloudstore.core.io.IOutputStream;

public abstract class AbstractPgpDecoder implements PgpDecoder {

	private IInputStream inputStream;
	private IOutputStream outputStream;
	private IInputStream signInputStream;
	private PgpKey decryptPgpKey;
	private PgpKey signPgpKey;
	private Set<PgpKeyId> signPgpKeyIds = Collections.emptySet();
	private PgpSignature pgpSignature;
	private boolean failOnMissingSignPgpKey = true;

	@Override
	public IInputStream getInputStream() {
		return inputStream;
	}
	@Override
	public void setInputStream(final IInputStream inputStream) {
		this.inputStream = inputStream;
	}
	protected IInputStream getInputStreamOrFail() {
		final IInputStream inputStream = getInputStream();
		if (inputStream == null)
			throw new IllegalStateException("inputStream == null");

		return inputStream;
	}

	@Override
	public IOutputStream getOutputStream() {
		return outputStream;
	}
	@Override
	public void setOutputStream(final IOutputStream outputStream) {
		this.outputStream = outputStream;
	}
	protected IOutputStream getOutputStreamOrFail() {
		final IOutputStream outputStream = getOutputStream();
		if (outputStream == null)
			throw new IllegalStateException("outputStream == null");

		return outputStream;
	}

	@Override
	public IInputStream getSignInputStream() {
		return signInputStream;
	}
	@Override
	public void setSignInputStream(IInputStream signInputStream) {
		this.signInputStream = signInputStream;
	}

	protected PgpAuthenticationCallback getPgpAuthenticationCallback() {
		final PgpAuthenticationCallback pgpAuthenticationCallback = PgpRegistry.getInstance().getPgpAuthenticationCallback();
		return pgpAuthenticationCallback;
	}

	protected PgpAuthenticationCallback getPgpAuthenticationCallbackOrFail() {
		final PgpAuthenticationCallback pgpAuthenticationCallback = getPgpAuthenticationCallback();
		if (pgpAuthenticationCallback == null)
			throw new IllegalStateException("There is no PgpAuthenticationCallback assigned!");

		return pgpAuthenticationCallback;
	}

	@Override
	public PgpKey getDecryptPgpKey() {
		return decryptPgpKey;
	}
	protected void setDecryptPgpKey(PgpKey decryptPgpKey) {
		this.decryptPgpKey = decryptPgpKey;
	}

	@Override
	public Set<PgpKeyId> getSignPgpKeyIds() {
		return signPgpKeyIds;
	}
	protected void setSignPgpKeyIds(final Set<PgpKeyId> signPgpKeyIds) {
		this.signPgpKeyIds = signPgpKeyIds == null ? Collections.<PgpKeyId>emptySet() : Collections.unmodifiableSet(signPgpKeyIds);
	}

	public PgpKey getSignPgpKey() {
		return signPgpKey;
	}
	protected void setSignPgpKey(final PgpKey signPgpKey) {
		this.signPgpKey = signPgpKey;
	}

	@Override
	public boolean isFailOnMissingSignPgpKey() {
		return failOnMissingSignPgpKey;
	}
	@Override
	public void setFailOnMissingSignPgpKey(boolean failOnMissingSignPgpKey) {
		this.failOnMissingSignPgpKey = failOnMissingSignPgpKey;
	}

	@Override
	public PgpSignature getPgpSignature() {
		return pgpSignature;
	}
	public void setPgpSignature(PgpSignature pgpSignature) {
		this.pgpSignature = pgpSignature;
	}
}
