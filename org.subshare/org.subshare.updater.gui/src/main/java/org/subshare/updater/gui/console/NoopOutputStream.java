package org.subshare.updater.gui.console;

import java.io.IOException;
import java.io.OutputStream;

public class NoopOutputStream extends OutputStream {

	@Override
	public void write(int b) throws IOException {
		// nothing to do!
	}

}
