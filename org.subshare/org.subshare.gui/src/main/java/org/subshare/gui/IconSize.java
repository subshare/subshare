package org.subshare.gui;

public enum IconSize {
	_16x16,
	_24x24,
	_32x32,
	_64x64;

	private final int width;
	private final int height;

	private IconSize() {
		String s = name();
		if (!s.startsWith("_"))
			throw new IllegalStateException("name does not start with '_'!");

		s = s.substring(1);

		final int xIndex = s.indexOf('x');
		if (xIndex < 0)
			throw new IllegalStateException("name does not contain 'x'!");

		final String w = s.substring(0, xIndex);
		final String h = s.substring(xIndex + 1);

		this.width = Integer.parseInt(w);
		this.height = Integer.parseInt(h);
	}

	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return height;
	}

//	public static void main(String[] args) {
//		for (IconSize iconSize : IconSize.values()) {
//			System.out.println(iconSize.getWidth());
//			System.out.println(iconSize.getHeight());
//			System.out.println();
//		}
//	}
}
