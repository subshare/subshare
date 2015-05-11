package org.subshare.core.io;

import java.io.IOException;
import java.io.OutputStream;

public class NullOutputStream extends OutputStream {

	public NullOutputStream() {
	}

	@Override
	public void write(int b) throws IOException {
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
	}

	@Override
	public void write(byte[] b) throws IOException {
	}
}
