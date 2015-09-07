package org.subshare.local;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;
import java.util.TreeMap;

import org.subshare.local.FilePaddingLengthRandom.LengthCategory;
import org.junit.Test;

import co.codewizards.cloudstore.core.config.Config;

public class FilePaddingLengthRandomTest {

	@Test
	public void generateOneRandomLength() {
		FilePaddingLengthRandom random = new FilePaddingLengthRandom(Config.getInstance());
		long length = random.nextPaddingLength();
		assertThat(length).isNotNegative();
		System.out.println(length);
	}

	@Test
	public void testDistribution() {
		FilePaddingLengthRandom random = new FilePaddingLengthRandom(Config.getInstance());
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

		assertThat(lengthCategory2HitPermille.get(FilePaddingLengthRandom.LengthCategory._100K)).isBetween(399, 401);
		assertThat(lengthCategory2HitPermille.get(FilePaddingLengthRandom.LengthCategory._1M)).isBetween(393, 395);
		assertThat(lengthCategory2HitPermille.get(FilePaddingLengthRandom.LengthCategory._10M)).isBetween(189, 191);
		assertThat(lengthCategory2HitPermille.get(FilePaddingLengthRandom.LengthCategory._100M)).isBetween(9, 11);
		assertThat(lengthCategory2HitPermille.get(FilePaddingLengthRandom.LengthCategory._1G)).isBetween(3, 5);
		assertThat(lengthCategory2HitPermille.get(FilePaddingLengthRandom.LengthCategory._10G)).isBetween(1, 3);
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
