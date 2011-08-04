/**
 * Programmer: Jacob Scott
 * Program Name: Rand
 * Description:
 * Date: Mar 29, 2011
 */
package me.jascotty2.util;

import java.util.Date;
import java.util.Random;

/**
 * @author jacob
 */
public class Rand {

	static Random rand = new Random();
	public static char filenameChars[] = {};
	protected static boolean isRand = false;

	public static String randFname() {
		return randFname(10, 25);
	}

	public static String randFname(int length) {
		return randFname(length, length);
	}

	public static String randFname(int minlength, int maxlength) {
		if (filenameChars.length == 0) {
			// populate with alphanumerical chars
			filenameChars = new char[62];
			int n = 48;
			for (int i = 0; i < 62; ++i) {
				filenameChars[i] = (char) n++;
				if (n == 58) {
					n = 65;
				} else if (n == 91) {
					n = 97;
				}
			}
		}
		String ret = "";
		for (int i = RandomInt(minlength, maxlength); i > 0; --i) {
			ret += filenameChars[RandomInt(0, filenameChars.length - 1)];
		}
		return ret;
	}

	public static int RandomInt(int min, int max) {
		if (min == max) {
			return min;
		}
		if (max < min) {
			return RandomInt(max, min);
		}
		if (!isRand) {
			rand.setSeed((new Date()).getTime());
			isRand = true;
		}
		return min + rand.nextInt(max - min + 1);
	}

	public static double RandomDouble() {
		if (!isRand) {
			rand.setSeed((new Date()).getTime());
			isRand = true;
		}
		return rand.nextDouble();
	}

	public static double RandomDouble(double min, double max) {
		if (!isRand) {
			rand.setSeed((new Date()).getTime());
			isRand = true;
		}
		return min + rand.nextDouble() * (max - min);
	}

	public static boolean RandomBoolean() {
		if (!isRand) {
			rand.setSeed((new Date()).getTime());
			isRand = true;
		}
		return rand.nextBoolean();
	}

	public static boolean RandomBoolean(double chance) {
		if (chance >= 1) {
			return true;
		}
		if (chance <= 0) {
			return false;
		}
		if (!isRand) {
			rand.setSeed((new Date()).getTime());
			isRand = true;
		}
		return rand.nextDouble() <= chance;
	}
} // end class Rand

