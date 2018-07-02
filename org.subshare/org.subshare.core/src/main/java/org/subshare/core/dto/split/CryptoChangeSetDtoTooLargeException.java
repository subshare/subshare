package org.subshare.core.dto.split;

import co.codewizards.cloudstore.core.exception.ApplicationException;

@ApplicationException
public class CryptoChangeSetDtoTooLargeException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public CryptoChangeSetDtoTooLargeException(int cryptoChangeSetDtoFinalFileCount) {
		super(Integer.toString(cryptoChangeSetDtoFinalFileCount));
	}

	public CryptoChangeSetDtoTooLargeException(String message) {
		super(message);
	}

	public CryptoChangeSetDtoTooLargeException(Throwable cause) {
		super(cause);
	}

	public CryptoChangeSetDtoTooLargeException(String message, Throwable cause) {
		super(message, cause);
	}

	public CryptoChangeSetDtoTooLargeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
