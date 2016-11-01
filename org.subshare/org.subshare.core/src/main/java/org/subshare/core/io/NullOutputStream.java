package org.subshare.core.io;

import java.io.IOException;
import java.io.OutputStream;

import co.codewizards.cloudstore.core.io.IOutputStream;

public class NullOutputStream extends OutputStream implements IOutputStream {

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
