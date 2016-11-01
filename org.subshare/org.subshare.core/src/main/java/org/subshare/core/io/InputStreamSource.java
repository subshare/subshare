package org.subshare.core.io;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.util.IOUtil.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.UUID;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.io.ByteArrayInputStream;
import co.codewizards.cloudstore.core.io.IInputStream;
import co.codewizards.cloudstore.core.util.IOUtil;

/**
 * Source of an {@link InputStream} used in a {@link MultiInputStream}.
 * <p>
 * An {@link InputStreamSource} is used only once, i.e. the {@link #createInputStream()} is invoked once - or
 * the {@link #discard()} method is invoked instead.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface InputStreamSource {

	/**
	 * Creates a new {@code InputStream}.
	 * <p>
	 * The {@code InputStream} is only created, if needed. After being read completely - or when the {@link MultiInputStream}
	 * is closed - this {@code InputStream} is closed.
	 * @return a new {@code InputStream}. Never <code>null</code>.
	 * @throws IOException in case opening the {@code InputStream} fails.
	 */
	InputStream createInputStream() throws IOException;

	/**
	 * Discards this {@code InputStreamSource} without using it.
	 * <p>
	 * This method is invoked instead of {@link #createInputStream()}, if this source is not used, i.e. no {@code InputStream}
	 * is ever created from it.
	 * <p>
	 * This method allows for cleaning up resources that were allocated when this {@link InputStreamSource} was created. Even
	 * though, resource allocation should happen only when {@linkplain #createInputStream() the InputStream is created}, there
	 * are situations, which require earlier allocation of resources.
	 *
	 * @throws IOException in case discarding fails.
	 */
	void discard() throws IOException;

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

				@Override
				public void discard() {
					// nothing to do
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

		public static InputStreamSource createInputStreamSource(final Date value) {
			return createInputStreamSource(value == null ? null : value.getTime());
		}

		/**
		 * Creates an {@link InputStreamSource} wrapping the given, existing {@code InputStream} instead of
		 * creating a new {@code InputStream}.
		 * <p>
		 * <b>Important:</b> The given {@code InputStream} is closed!
		 * @param in the {@code InputStream} to be wrapped by the {@code InputStreamSource}.
		 * @return a new {@code InputStreamSource} wrapping the given {@code InputStream} {@code in}.
		 */
		public static InputStreamSource createInputStreamSource(final InputStream in) {
			if (in == null)
				return createInputStreamSource((byte[]) null);

			return new InputStreamSource() {
				@Override
				public InputStream createInputStream() throws IOException {
					return in;
				}

				@Override
				public void discard() throws IOException {
					in.close();
				}
			};
		}

		public static InputStreamSource createInputStreamSource(final IInputStream in) {
			return createInputStreamSource(castStream(in));
		}

		public static InputStreamSource createInputStreamSource(final ByteArrayInputStream in) {
			return createInputStreamSource((InputStream) in);
		}

		public static InputStreamSource createInputStreamSource(final File file) {
			if (file == null)
				return createInputStreamSource((byte[]) null);

			return new InputStreamSource() {
				@Override
				public InputStream createInputStream() throws IOException {
					return new FileInputStream(file);
				}

				@Override
				public void discard() {
					// nothing to do
				}
			};
		}
	}

}
