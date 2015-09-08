package org.subshare.local;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.security.SecureRandom;

import org.subshare.core.crypto.KeyFactory;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.oio.File;

public class FilePaddingLengthRandom {
	private final Config config;

	private static final int[] defaultLengthProbability = {
		600, // ‰ 100K
		310, // ‰   1M
		 90, // ‰  10M
		  0, // ‰ 100M
		  0, // ‰   1G
		  0  // ‰  10G
	};

	private int[] lengthProbability;
	private int lengthProbabilitySum;

	private static SecureRandom random = KeyFactory.secureRandom;

	public static enum LengthCategory {
		_100K(0L, 100L * 1024L),
		_1M(100L * 1024L, 1024L * 1024L),
		_10M(1024L * 1024L, 10L * 1024L * 1024L),
		_100M(10L * 1024L * 1024L, 100L * 1024L * 1024L),
		_1G(100L * 1024L * 1024L, 1024L * 1024L * 1024L),
		_10G(1024L * 1024L * 1024L, 10L * 1024L * 1024L * 1024L);

		private long minLength; // inclusive
		private long maxLength; // exclusive

		private LengthCategory(long minLength, long maxLength) {
			this.minLength = minLength;
			this.maxLength = maxLength;
		}

		public long getMinLength() {
			return minLength;
		}

		public long getMaxLength() {
			return maxLength;
		}

		public String getCategoryId() {
			if (name().charAt(0) != '_')
				throw new IllegalStateException("name does not start with '_'!");

			return name().substring(1);
		}

		public String getConfigPropertyKey() {
			final String key = String.format("filePaddingLengthProbability[%s]", getCategoryId());
			return key;
		}
	}

	static {
		if (defaultLengthProbability.length != LengthCategory.values().length) {
			throw new IllegalStateException(String.format("defaultLengthProbability.length != LengthCategory.values().length :: %d != %d",
					defaultLengthProbability.length, LengthCategory.values().length));
		}
	}

	public FilePaddingLengthRandom(final File file) {
		this(Config.getInstanceForFile(assertNotNull("file", file)));
	}

	public FilePaddingLengthRandom(final Config config) {
		this.config = assertNotNull("config", config);
		populateLengthProbability();
	}

	private void populateLengthProbability() {
		lengthProbability = new int[LengthCategory.values().length];

		lengthProbabilitySum = 0; // should be 100, but maybe the user uses per-mille or simply screwed it up ;-) we can cope with everything > 0
		for (final LengthCategory lengthCategory : LengthCategory.values()) {
			final String key = lengthCategory.getConfigPropertyKey();
			final int probability = config.getPropertyAsPositiveOrZeroInt(key, defaultLengthProbability[lengthCategory.ordinal()]);
			lengthProbability[lengthCategory.ordinal()] = probability;
			lengthProbabilitySum += probability;
		}
	}

	public long nextPaddingLength() {
		LengthCategory lengthCategory = nextLengthCategory();
		long length = Math.abs(random.nextLong());
		length = length % (lengthCategory.getMaxLength() - lengthCategory.getMinLength());
		length = length + lengthCategory.getMinLength();
		return length;
	}

	private LengthCategory nextLengthCategory() {
		int p = random.nextInt(lengthProbabilitySum);

		for (int i = 0; i < lengthProbability.length; ++i) {
			if (p < lengthProbability[i])
				return LengthCategory.values()[i];

			p -= lengthProbability[i];
		}

		throw new IllegalStateException("WTF?!");
	}
}
