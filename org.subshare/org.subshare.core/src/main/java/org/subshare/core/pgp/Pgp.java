package org.subshare.core.pgp;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

public interface Pgp {

	int getPriority();

	boolean isSupported();

	Collection<PgpKey> getMasterKeys();

	PgpKey getPgpKey(long pgpKeyId);

	PgpEncoder createEncoder(InputStream in, OutputStream out);

	PgpDecoder createDecoder(InputStream in, OutputStream out);

}
