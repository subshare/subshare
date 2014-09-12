package org.subshare.core.io;

import static co.codewizards.cloudstore.core.util.IOUtil.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.util.IOUtil;

public interface InputStreamSource {

	InputStream createInputStream() throws IOException;

	static class Helper {
		public static InputStreamSource createInputStreamSource(final byte[] bytes) {
			return new InputStreamSource() {
				private byte[] byteArray = bytes;

				@Override
				public InputStream createInputStream() throws IOException {
					return new ByteArrayInputStream(byteArray == null ? new byte[0] : byteArray) {
						@Override
						public void close() throws IOException {
							byteArray = null;
							super.close();
						}
					};
				}
			};
		}

		public static InputStreamSource createInputStreamSource(final Uid uid) {
			return createInputStreamSource(uid == null ? null : uid.toBytes());
		}

		public static InputStreamSource createInputStreamSource(final UUID uuid) {
			return createInputStreamSource(uuid == null ? null : new Uid(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits()));
		}

		public static InputStreamSource createInputStreamSource(final boolean b) {
			return createInputStreamSource(b ? new byte[] { 1 } : new byte[] { 0 });
		}

		public static InputStreamSource createInputStreamSource(final Boolean b) {
			if (b == null)
				return createInputStreamSource((byte[]) null);

			return createInputStreamSource(b.booleanValue());
		}

		public static InputStreamSource createInputStreamSource(final byte value) {
			return createInputStreamSource(new byte[] { value });
		}

		public static InputStreamSource createInputStreamSource(final Byte value) {
			if (value == null)
				return createInputStreamSource((byte[]) null);

			return createInputStreamSource(value.byteValue());
		}

		public static InputStreamSource createInputStreamSource(final int value) {
			return createInputStreamSource(intToBytes(value));
		}

		public static InputStreamSource createInputStreamSource(final Integer value) {
			if (value == null)
				return createInputStreamSource((byte[]) null);

			return createInputStreamSource(value.intValue());
		}

		public static InputStreamSource createInputStreamSource(final long value) {
			return createInputStreamSource(longToBytes(value));
		}

		public static InputStreamSource createInputStreamSource(final Long value) {
			if (value == null)
				return createInputStreamSource((byte[]) null);

			return createInputStreamSource(value.longValue());
		}

		public static InputStreamSource createInputStreamSource(final String value) {
			if (value == null)
				return createInputStreamSource((byte[]) null);

			try {
				return createInputStreamSource(value.getBytes(IOUtil.CHARSET_NAME_UTF_8));
			} catch (final UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}

		public static InputStreamSource createInputStreamSource(final InputStream in) {
			return new InputStreamSource() {
				@Override
				public InputStream createInputStream() throws IOException {
					return new FilterInputStream(in) {
						@Override
						public void close() throws IOException {
							// do *NOT* close the given InputStream, because it was not opened here (but outside).
						}
					};
				}
			};
		}

		public static InputStreamSource createInputStreamSource(final File file) {
			return new InputStreamSource() {
				@Override
				public InputStream createInputStream() throws IOException {
					return new FileInputStream(file);
				}
			};
		}
	}

}
