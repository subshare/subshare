package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.lang.reflect.Field;
import java.util.Date;

import javax.jdo.JDOHelper;

import org.subshare.core.dto.SignatureDto;
import org.subshare.core.sign.Signable;
import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.dto.Uid;

/**
 * @deprecated Workaround for <a href="http://www.datanucleus.org/servlet/jira/browse/NUCCORE-1247">NUCCORE-1247</a>.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
@Deprecated
public class SignableEmbeddedWorkaround {

	private SignableEmbeddedWorkaround() { }

	public static Signature getSignature(final Signable signable) {
		final Date signatureCreated = (Date) getFieldValue(signable, "signatureCreated");
		final String signingUserRepoKeyId = (String) getFieldValue(signable, "signingUserRepoKeyId");
		final byte[] signatureData = (byte[]) getFieldValue(signable, "signatureData");

		if (signatureCreated == null || signingUserRepoKeyId == null)
			return null;

		return SignatureImpl.copy(new SignatureDto(signatureCreated, new Uid(signingUserRepoKeyId), signatureData));
	}

	public static void setSignature(final Signable signable, final Signature signature) {
		if (!equal(getSignature(signable), signature)) {
			setFieldValue(signable, "signatureCreated", signature == null ? null : signature.getSignatureCreated());
			final Uid signingUserRepoKeyId = signature == null ? null : signature.getSigningUserRepoKeyId();
			setFieldValue(signable, "signingUserRepoKeyId", signingUserRepoKeyId == null ? null : signingUserRepoKeyId.toString());
			setFieldValue(signable, "signatureData", signature == null ? null : assertNotNull("signature.signatureData", signature.getSignatureData()));
		}
	}

	private static void setFieldValue(final Object object, final String fieldName, final Object value) {
		try {
			final Field field = getDeclaredField(object.getClass(), fieldName);
			field.setAccessible(true);
			field.set(object, value);
			JDOHelper.makeDirty(object, fieldName);
		} catch (final IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private static Object getFieldValue(final Object object, final String fieldName) {
		try {
			final Field field = getDeclaredField(object.getClass(), fieldName);
			field.setAccessible(true);
			final Object value = field.get(object);
			return value;
		} catch (final IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private static Field getDeclaredField(final Class<?> clazz, final String fieldName) {
		Class<?> c = clazz;
		while (c != null && c != Object.class) {
			try {
				final Field field = c.getDeclaredField(fieldName);
				return field;
			} catch (final NoSuchFieldException e) {
				doNothing(); // ignore and continue search in super-classes.
			}
			c = c.getSuperclass();
		}
		throw new RuntimeException(new NoSuchFieldException(String.format(
				"The field '%s' does not exist in class '%s' (nor in any of its super-classes)!",
				fieldName, clazz.getName())));
	}

}
