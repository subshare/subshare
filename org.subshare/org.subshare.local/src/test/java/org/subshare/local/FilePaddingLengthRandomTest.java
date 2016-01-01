package org.subshare.local;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;
import java.util.TreeMap;

import org.junit.After;
import org.junit.Test;
import org.subshare.local.FilePaddingLengthRandom.LengthCategory;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.config.ConfigImpl;

public class FilePaddingLengthRandomTest {

	@After
	public void after() {
		for (LengthCategory lengthCategory : FilePaddingLengthRandom.LengthCategory.values())
			System.getProperties().remove(Config.SYSTEM_PROPERTY_PREFIX + lengthCategory.getConfigPropertyKey());
	}

	@Test
	public void generateOneRandomLength() {
		FilePaddingLengthRandom random = new FilePaddingLengthRandom(ConfigImpl.getInstance());
		long length = random.nextPaddingLength();
		assertThat(length).isNotNegative();
		System.out.println(length);
	}

	@Test
	public void testDistribution() {
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + FilePaddingLengthRandom.LengthCategory._100K.getConfigPropertyKey(), "600");
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + FilePaddingLengthRandom.LengthCategory._1M.getConfigPropertyKey(), "295");
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + FilePaddingLengthRandom.LengthCategory._10M.getConfigPropertyKey(), "94");
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + FilePaddingLengthRandom.LengthCategory._100M.getConfigPropertyKey(), "6");
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + FilePaddingLengthRandom.LengthCategory._1G.getConfigPropertyKey(), "4");
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + FilePaddingLengthRandom.LengthCategory._10G.getConfigPropertyKey(), "0");

		FilePaddingLengthRandom random = new FilePaddingLengthRandom(ConfigImpl.getInstance());
		final int invocationCount = 500000;

		Map<FilePaddingLengthRandom.LengthCategory, Integer> lengthCategory2HitCount = new TreeMap<>();
		for (FilePaddingLengthRandom.LengthCategory lengthCategory : FilePaddingLengthRandom.LengthCategory.values())
			lengthCategory2HitCount.put(lengthCategory, 0);

		for (int i = 0 ; i < invocationCount; ++i) {
			long paddingLength = random.nextPaddingLength();
			LengthCategory lengthCategory = getLengthCategory(paddingLength);
			Integer hitCount = lengthCategory2HitCount.get(lengthCategory);
			if (hitCount == null)
				hitCount = 0;

			lengthCategory2HitCount.put(lengthCategory, hitCount + 1);
		}

		System.out.println("lengthCategory2HitCount: " + lengthCategory2HitCount);

		Map<FilePaddingLengthRandom.LengthCategory, Integer> lengthCategory2HitPermille = new TreeMap<>();
		for (Map.Entry<FilePaddingLengthRandom.LengthCategory, Integer> me : lengthCategory2HitCount.entrySet()) {
			long percentage = Math.round(((double) me.getValue()) * 1000d / invocationCount);
			lengthCategory2HitPermille.put(me.getKey(), (int) percentage);
		}

		System.out.println("lengthCategory2HitPercentage: " + lengthCategory2HitPermille);

		assertThat(lengthCategory2HitPermille.get(FilePaddingLengthRandom.LengthCategory._100K)).isBetween(598, 602);
		assertThat(lengthCategory2HitPermille.get(FilePaddingLengthRandom.LengthCategory._1M)).isBetween(293, 297);
		assertThat(lengthCategory2HitPermille.get(FilePaddingLengthRandom.LengthCategory._10M)).isBetween(93, 95);
		assertThat(lengthCategory2HitPermille.get(FilePaddingLengthRandom.LengthCategory._100M)).isBetween(5, 7);
		assertThat(lengthCategory2HitPermille.get(FilePaddingLengthRandom.LengthCategory._1G)).isBetween(3, 5);
		assertThat(lengthCategory2HitPermille.get(FilePaddingLengthRandom.LengthCategory._10G)).isEqualTo(0);
	}

	protected FilePaddingLengthRandom.LengthCategory getLengthCategory(long paddingLength) {
		for (FilePaddingLengthRandom.LengthCategory lengthCategory : FilePaddingLengthRandom.LengthCategory.values()) {
			if (paddingLength >= lengthCategory.getMinLength() &&
					paddingLength < lengthCategory.getMaxLength())
				return lengthCategory;
		}
		throw new IllegalStateException("WTF?!");
	}
}
