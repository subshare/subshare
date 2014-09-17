package org.subshare.core.io;

import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.util.Random;

import org.junit.Test;

public class LimitedInputStreamTest {

	private static Random random = new Random();

	@Test
	public void randomLengthWithUnderlyingSameLength() throws Exception {
		for (int i = 0; i < 500; ++i) {
			final int length = random.nextInt(1024 * 1024);
			int totalBytesRead = 0;
			try (final LimitedInputStream lin = new LimitedInputStream(new ByteArrayInputStream(new byte[length]), length, length);) {
				while (true) {
					if (random.nextInt(100) < 20) {
						final int read = lin.read();
						if (read < 0)
							break;

						++totalBytesRead;
					}
					else {
						final byte[] buf = new byte[random.nextInt(64 * 1024)];
						final int bytesRead = lin.read(buf);
						if (bytesRead < 0)
							break;

						totalBytesRead += bytesRead;
					}
				}
			}
			assertThat(totalBytesRead).isEqualTo(length);
		}
	}

}
