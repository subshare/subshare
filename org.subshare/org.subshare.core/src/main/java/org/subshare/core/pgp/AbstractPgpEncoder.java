package org.subshare.core.pgp;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.HashSet;
import java.util.Set;

import co.codewizards.cloudstore.core.io.IInputStream;
import co.codewizards.cloudstore.core.io.IOutputStream;


public abstract class AbstractPgpEncoder implements PgpEncoder {

	private IInputStream inputStream;
	private IOutputStream outputStream;
	private IOutputStream signOutputStream;

	private final Set<PgpKey> encryptPgpKeys = new HashSet<PgpKey>(0);
	private PgpKey signPgpKey;
	private String fileName = "";

	private boolean withIntegrityCheck;
	private SymmetricEncryptionAlgorithm symmetricEncryptionAlgorithm = SymmetricEncryptionAlgorithm.TWOFISH;
	private CompressionAlgorithm compressionAlgorithm = CompressionAlgorithm.ZIP;
	private HashAlgorithm hashAlgorithm = HashAlgorithm.SHA256;

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
	public IOutputStream getSignOutputStream() {
		return signOutputStream;
	}
	@Override
	public void setSignOutputStream(IOutputStream signOutputStream) {
		this.signOutputStream = signOutputStream;
	}

	@Override
	public boolean isWithIntegrityCheck() {
		return withIntegrityCheck;
	}
	@Override
	public void setWithIntegrityCheck(final boolean withIntegrityCheck) {
		this.withIntegrityCheck = withIntegrityCheck;
	}

	@Override
	public SymmetricEncryptionAlgorithm getSymmetricEncryptionAlgorithm() {
		return symmetricEncryptionAlgorithm;
	}
	@Override
	public void setSymmetricEncryptionAlgorithm(final SymmetricEncryptionAlgorithm symmetricEncryptionAlgorithm) {
		this.symmetricEncryptionAlgorithm = assertNotNull("symmetricEncryptionAlgorithm", symmetricEncryptionAlgorithm);
	}

	@Override
	public CompressionAlgorithm getCompressionAlgorithm() {
		return compressionAlgorithm;
	}
	@Override
	public void setCompressionAlgorithm(final CompressionAlgorithm compressionAlgorithm) {
		this.compressionAlgorithm = assertNotNull("compressionAlgorithm", compressionAlgorithm);
	}

	@Override
	public HashAlgorithm getHashAlgorithm() {
		return hashAlgorithm;
	}
	@Override
	public void setHashAlgorithm(final HashAlgorithm hashAlgorithm) {
		this.hashAlgorithm = assertNotNull("hashAlgorithm", hashAlgorithm);
	}

	@Override
	public Set<PgpKey> getEncryptPgpKeys() {
		return encryptPgpKeys;
	}

	@Override
	public PgpKey getSignPgpKey() {
		return signPgpKey;
	}
	@Override
	public void setSignPgpKey(final PgpKey signPgpKey) {
		if (signPgpKey != null && !signPgpKey.isSecretKeyAvailable())
			throw new IllegalArgumentException("signPgpKey.privateKeyAvailable == false :: A private key is required for signing!");

		this.signPgpKey = signPgpKey;
	}

	public String getFileName() {
		return fileName;
	}
	public void setFileName(final String fileName) {
		this.fileName = assertNotNull("fileName", fileName);
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
}
