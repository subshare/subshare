package org.subshare.core.io;

import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

public class MultiInputStreamTest {

	private static Random random = new Random();

	@Test
	public void randomCombi() throws Exception {
		final int inputStreamCount = random.nextInt(20);
		final List<InputStreamSource> inputStreamSources = new ArrayList<InputStreamSource>(inputStreamCount);

		final byte[] expected;
		{
			final ByteArrayOutputStream expectedOut = new ByteArrayOutputStream();
			for (int i = 0; i < inputStreamCount; ++i) {
				final byte[] buf = new byte[random.nextInt(1024 * 1024)];
				random.nextBytes(buf);
				expectedOut.write(buf);
				inputStreamSources.add(InputStreamSource.Helper.createInputStreamSource(buf));
			}
			expected = expectedOut.toByteArray();
		}

		final byte[] found;
		try (MultiInputStream in = new MultiInputStream(inputStreamSources);) {
			final ByteArrayOutputStream foundOut = new ByteArrayOutputStream();
			while (true) {
				if (random.nextInt(100) < 20) {
					final int read = in.read();
					if (read < 0)
						break;

					foundOut.write(read);
				}
				else {
					final byte[] buf = new byte[random.nextInt(100 * 1024)];
					final int read = in.read(buf);
					if (read < 0)
						break;

					foundOut.write(buf, 0, read);
				}
			}
			found = foundOut.toByteArray();
		}

		assertThat(found).isEqualTo(expected);
	}

}
