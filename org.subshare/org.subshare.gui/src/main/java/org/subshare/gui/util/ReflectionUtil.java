//package org.subshare.gui.util;
//
//import static co.codewizards.cloudstore.core.util.AssertUtil.*;
//
//public class ReflectionUtil {
//
//	private ReflectionUtil() { }
//
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
//
//}
