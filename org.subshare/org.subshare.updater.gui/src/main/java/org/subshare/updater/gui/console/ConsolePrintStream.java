package org.subshare.updater.gui.console;

import java.io.PrintStream;
import java.util.Formatter;
import java.util.Locale;

public class ConsolePrintStream extends PrintStream {

	private final ConsolePane consolePane;

	private Formatter formatter;

	public ConsolePrintStream(final ConsolePane consolePane) {
		super(new NoopOutputStream(), true);
		this.consolePane = consolePane;
	}

	@Override
	public void write(int b) {
		if (Character.isValidCodePoint(b))
			consolePane.print(String.valueOf((char) b));
		else
			consolePane.print("#" + Integer.toHexString(b));
	}

	@Override
	public void write(byte[] buf, int off, int len) {
		for (int i = 0; i < len; ++i)
			write(buf[off + i]);
	}

	@Override
	public void print(boolean b) {
		consolePane.print(Boolean.toString(b));
	}

	@Override
	public void print(char c) {
		consolePane.print(String.valueOf(c));
	}

	@Override
	public void print(int i) {
		consolePane.print(String.valueOf(i));
	}

	@Override
	public void print(long l) {
		consolePane.print(String.valueOf(l));
	}

	@Override
	public void print(float f) {
		consolePane.print(String.valueOf(f));
	}

	@Override
	public void print(double d) {
		consolePane.print(String.valueOf(d));
	}

	@Override
	public void print(char[] s) {
		consolePane.print(String.valueOf(s));
	}

	@Override
	public void print(String s) {
		consolePane.print(s);
	}

	@Override
	public void print(Object obj) {
		consolePane.print(String.valueOf(obj));
	}

	@Override
	public void println() {
		consolePane.println("");
	}

	@Override
	public void println(boolean b) {
		consolePane.println(Boolean.toString(b));
	}

	@Override
	public void println(char c) {
		consolePane.println(String.valueOf(c));
	}

	@Override
	public void println(int i) {
		consolePane.println(String.valueOf(i));
	}

	@Override
	public void println(long l) {
		consolePane.println(String.valueOf(l));
	}

	@Override
	public void println(float f) {
		consolePane.println(String.valueOf(f));
	}

	@Override
	public void println(double d) {
		consolePane.println(String.valueOf(d));
	}

	@Override
	public void println(char[] s) {
		consolePane.println(String.valueOf(s));
	}

	@Override
	public void println(String s) {
		consolePane.println(s);
	}

	@Override
	public void println(Object obj) {
		consolePane.println(String.valueOf(obj));
	}

	@Override
	public PrintStream printf(String format, Object... args) {
		return format(format, args);
	}

	@Override
	public PrintStream printf(Locale l, String format, Object... args) {
		return format(l, format, args);
	}

	@Override
	public PrintStream format(String format, Object... args) {
		synchronized (this) {
			getFormatter().format(format, args);
			return this;
		}
	}

	@Override
	public PrintStream format(Locale l, String format, Object... args) {
		synchronized (this) {
			getFormatter().format(l, format, args);
			return this;
		}
	}

	@Override
	public PrintStream append(CharSequence csq) {
		print(csq);
		return this;
	}

	@Override
	public PrintStream append(CharSequence csq, int start, int end) {
		print(csq.subSequence(start, end).toString());
		return this;
	}

	@Override
	public PrintStream append(char c) {
		print(c);
		return this;
	}

	private Formatter getFormatter() {
		if (formatter == null)
			formatter = new Formatter(this);

		return formatter;
	}
}
