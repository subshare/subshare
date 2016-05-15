package org.subshare.local.persistence;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

import co.codewizards.cloudstore.core.dto.Uid;

public class CollisionTest {

	@Test
	public void xorUids1() {
		Uid uid_in1 = new Uid();
		Uid uid_in2 = new Uid();
		byte[] bytes = uid_in1.toBytes();
		Collision.xorIntoBytes(bytes, uid_in2);

		Uid uid_out1 = new Uid(bytes);
		assertThat(uid_out1).isNotEqualTo(uid_in1);
		assertThat(uid_out1).isNotEqualTo(uid_in2);

		Collision.xorIntoBytes(bytes, uid_in1);
		Uid uid_out2 = new Uid(bytes);
		assertThat(uid_out2).isNotEqualTo(uid_in1);
		assertThat(uid_out2).isEqualTo(uid_in2);
	}

	@Test
	public void xorUids2() {
		Uid uid_in1 = new Uid();
		Uid uid_in2 = new Uid();
		Uid uid_in3 = new Uid();
		byte[] bytes = uid_in1.toBytes();
		Collision.xorIntoBytes(bytes, uid_in2);
		Collision.xorIntoBytes(bytes, uid_in3);

		Uid uid_out1 = new Uid(bytes);
		assertThat(uid_out1).isNotEqualTo(uid_in1);
		assertThat(uid_out1).isNotEqualTo(uid_in2);
		assertThat(uid_out1).isNotEqualTo(uid_in3);

		Collision.xorIntoBytes(bytes, uid_in1);
		Uid uid_out2 = new Uid(bytes);
		assertThat(uid_out2).isNotEqualTo(uid_in1);
		assertThat(uid_out2).isNotEqualTo(uid_in2);
		assertThat(uid_out2).isNotEqualTo(uid_in3);

		Collision.xorIntoBytes(bytes, uid_in2);
		Uid uid_out3 = new Uid(bytes);
		assertThat(uid_out3).isNotEqualTo(uid_in1);
		assertThat(uid_out3).isNotEqualTo(uid_in2);
		assertThat(uid_out3).isEqualTo(uid_in3);
	}
}
