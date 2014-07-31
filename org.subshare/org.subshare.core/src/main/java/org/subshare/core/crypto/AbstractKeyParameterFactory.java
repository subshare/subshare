package org.subshare.core.crypto;


public abstract class AbstractKeyParameterFactory implements KeyParameterFactory {

	CipherTransformation cipherTransformation;

	@Override
	public CipherTransformation getCipherTransformation() {
		return cipherTransformation;
	}
	@Override
	public void setCipherTransformation(final CipherTransformation cipherTransformation) {
		this.cipherTransformation = cipherTransformation;
	}
}
