package org.subshare.core.file;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import org.junit.Test;

public class EncryptedDataFileTest {

	private final Random random = new Random();

	@Test
	public void writeReadSimpleEncryptedDataFile() throws Exception {
		final byte[] data = new byte[100 + random.nextInt(1024 * 1024)];
		random.nextBytes(data);

		EncryptedDataFile edf1 = new EncryptedDataFile();
		edf1.putDefaultData(data);

		byte[] edfData = edf1.write();

		try {
			FileOutputStream fout = new FileOutputStream(File.createTempFile("xxx-", ".zip"));
			fout.write(edfData);
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		EncryptedDataFile edf2 = new EncryptedDataFile(edfData);
		byte[] defaultData2 = edf2.getDefaultData();

		assertThat(defaultData2).isNotNull();
		assertThat(defaultData2).isEqualTo(data);
	}

}
