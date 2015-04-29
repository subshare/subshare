package org.subshare.gui.util;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;
import static co.codewizards.cloudstore.core.util.Util.cast;
import static co.codewizards.cloudstore.core.util.Util.doNothing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ReflectionUtil {

	private ReflectionUtil() { }

//	public static Class<?> getCallerClass() {
//		// TODO try to use sun.reflect.Reflection.getCallerClass(), if it's in the classpath.
//
//		final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
//		assertNotNull("Thread.currentThread.stackTrace", stackTrace);
//		final int stackTraceElementIndex = 2;
//
//		if (stackTrace.length < stackTraceElementIndex + 1)
//			throw new IllegalStateException(String.format("Thread.currentThread.stackTrace.length < %s", stackTraceElementIndex + 1));
//
//		final StackTraceElement stackTraceElement = stackTrace[stackTraceElementIndex];
//		final String className = stackTraceElement.getClassName();
//
//	}

	public static <T> T invoke(final Object object, final String methodName, final Object ... args) {
		assertNotNull("object", object);
		assertNotNull("methodName", methodName);

		final Class<?>[] argTypes = args == null ? new Class<?>[0] : new Class<?>[args.length];
		for (int i = 0; i < argTypes.length; i++)
			argTypes[i] = args[i] == null ? null : args[i].getClass();

		// TODO cache methods - don't search for them again and again
		final List<Method> methods = getDeclaredMethods(object.getClass(), methodName);
		final List<Method> compatibleMethods = new ArrayList<Method>(Math.min(5, methods.size()));
		for (final Method method : methods) {
			if (isMethodCompatible(method, argTypes))
				compatibleMethods.add(method);
		}

		if (compatibleMethods.isEmpty()) {
			final Class<?> clazz = object.getClass();
			final String methodNameWithParameterTypes = createMethodNameWithParameterTypes(methodName, argTypes);
			throw new IllegalArgumentException(new NoSuchMethodException(String.format("Neither %s nor one of its super-classes declares the method %s (or an equivalent using super-types of these parameter-types)!", clazz.getName(), methodNameWithParameterTypes)));
		}

		if (compatibleMethods.size() > 1) {
			// TODO maybe find + invoke the most suitable instead of throwing this exception?
			final Class<?> clazz = object.getClass();
			final String methodNameWithParameterTypes = createMethodNameWithParameterTypes(methodName, argTypes);
			throw new IllegalArgumentException(new NoSuchMethodException(String.format("%s and its super-classes declare multiple methods matching %s (or an equivalent using super-types of these parameter-types)!", clazz.getName(), methodNameWithParameterTypes)));
		}

		return invoke(object, compatibleMethods.get(0), args);
	}

	public static <T> T invoke(final Object object, final String methodName, Class<?>[] parameterTypes, final Object ... args) {
		assertNotNull("object", object);
		assertNotNull("methodName", methodName);

		if (parameterTypes == null)
			return invoke(object, methodName, args);

		final Method method = getDeclaredMethodOrFail(object.getClass(), methodName, parameterTypes);
		return invoke(object, method, args);
	}

	private static <T> T invoke(final Object object, Method method, final Object ... args) {
		try {
			method.setAccessible(true);

			final Object result = method.invoke(object, args);
			return cast(result);
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	private static boolean isMethodCompatible(final Method method, final Class<?>[] argTypes) {
		final Class<?>[] parameterTypes = method.getParameterTypes();
		if (argTypes.length != parameterTypes.length)
			return false;

		for (int i = 0; i < parameterTypes.length; i++) {
			if (argTypes[i] != null && !parameterTypes[i].isAssignableFrom(argTypes[i]))
				return false;
		}

		return true;
	}

	public static List<Method> getDeclaredMethods(final Class<?> clazz, final String name) {
		final List<Method> result = new ArrayList<>();

		Class<?> c = clazz;
		while (c != null) {
			final Method[] methods = c.getDeclaredMethods();
			for (final Method method : methods) {
				if (name.equals(method.getName()))
					result.add(method);
			}
			c = c.getSuperclass();
		}

		return result;
	}

	public static Method getDeclaredMethodOrFail(final Class<?> clazz, final String name, final Class<?>[] parameterTypes) {
		final Method method = getDeclaredMethod(clazz, name, parameterTypes);
		if (method == null) {
			final String methodNameWithParameterTypes = createMethodNameWithParameterTypes(name, parameterTypes);
			throw new IllegalArgumentException(new NoSuchMethodException(String.format("Neither %s nor one of its super-classes declares the method %s!", clazz.getName(), methodNameWithParameterTypes)));
		}
		return method;
	}

	private static String createMethodNameWithParameterTypes(final String name, final Class<?>[] parameterTypes) {
		final StringBuilder sb = new StringBuilder();
		for (Class<?> parameterType : parameterTypes) {
			if (sb.length() > 0)
				sb.append(", ");

			sb.append(parameterType.getName());
		}
		return name + '(' + sb.toString() + ')';
	}

	public static Method getDeclaredMethod(final Class<?> clazz, final String name, final Class<?>[] parameterTypes) {
		Class<?> c = clazz;
		while (c != null) {
			try {
				final Method declaredMethod = c.getDeclaredMethod(name, parameterTypes);
				return declaredMethod;
			} catch (NoSuchMethodException x) {
				doNothing(); // expected in many cases ;-)
			}
			c = c.getSuperclass();
		}
		return null;
	}

}
