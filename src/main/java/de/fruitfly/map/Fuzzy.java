package de.fruitfly.map;

import org.lwjgl.util.vector.Vector3f;

public class Fuzzy {
	
	private static float EPSILON = 0.01f;
	
	public static boolean equal(float a, float b) {
		if (Math.abs(a - b) < EPSILON) return true;
		return false;
	}

	public static boolean equal(Vector3f a, Vector3f b) {
		if (!equal(a.x, b.x)) return false;
		if (!equal(a.y, b.y)) return false;
		if (!equal(a.z, b.z)) return false;
		return true;
	}
	
	public static boolean less(float a, float b) {
		return a < b + EPSILON;
	}
}
