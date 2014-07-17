package org.subshare.crypto;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

import junit.framework.Assert;

import org.subshare.crypto.CryptoRegistry;
import org.subshare.crypto.MACCalculator;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class MACBenchmark
{
	private static final Logger logger = LoggerFactory.getLogger(MACBenchmark.class);

	private static final String MAC_ALGORITHM = "HMAC-SHA1";

	private static final int ITERATION_COUNT = 100000;

	private static SecureRandom random = new SecureRandom();

	@BeforeClass
	public static void beforeClass()
	{
		CryptoRegistry.getInstance();
	}

	@Test
	public void benchmarkCreateManyInitialisedMACCalculators()
	throws Exception
	{
		for (int i = 0; i < ITERATION_COUNT; ++i) {
			CryptoRegistry.getInstance().createMACCalculator(MAC_ALGORITHM, true);
		}
	}

	@Test
	public void benchmarkCreateManyUNinitialisedMACCalculators()
	throws Exception
	{
		for (int i = 0; i < ITERATION_COUNT; ++i) {
			CryptoRegistry.getInstance().createMACCalculator(MAC_ALGORITHM, false);
		}
	}

	@Test
	public void benchmarkCalculateManyMACsWithSameMACCalculator()
	throws Exception
	{
		byte[] data = new byte[10240 + random.nextInt(4096)];
		random.nextBytes(data);

		MACCalculator macCalculator = CryptoRegistry.getInstance().createMACCalculator(MAC_ALGORITHM, true);
		for (int i = 0; i < ITERATION_COUNT; ++i) {
			byte[] mac = new byte[macCalculator.getMacSize()];
			macCalculator.update(data, 0, data.length);
			macCalculator.doFinal(mac, 0);
		}
	}

	@Test
	public void benchmarkCalculateManyMACsWithNewMACCalculator()
	throws Exception
	{
		byte[] data = new byte[10240 + random.nextInt(4096)];
		random.nextBytes(data);

		for (int i = 0; i < ITERATION_COUNT; ++i) {
			MACCalculator macCalculator = CryptoRegistry.getInstance().createMACCalculator(MAC_ALGORITHM, true);
			byte[] mac = new byte[macCalculator.getMacSize()];
			macCalculator.update(data, 0, data.length);
			macCalculator.doFinal(mac, 0);
		}
	}

	@Test
	public void testByteOrder()
	{
		byte[] ba = new byte[10];
		Arrays.fill(ba, (byte)0);
		ByteBuffer buf = ByteBuffer.wrap(ba);

		int someInt = random.nextInt();

		ba[0] = (byte)(someInt >>> 8);
		ba[1] = (byte)someInt;

		short short1 = (short)((ba[0] << 8) + (ba[1] & 0xff));
		int int1 = ((ba[0] << 8) & 0xffff) + (ba[1] & 0xff);

		buf.rewind();
		short short2 = buf.getShort();

		logger.info("short1 = {}", short1);
		logger.info("short2 = {}", short2);
		logger.info("int1 = {}", int1);
		Assert.assertEquals(short2, short1);
	}
}
