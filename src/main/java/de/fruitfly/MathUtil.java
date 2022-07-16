package de.fruitfly;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public class MathUtil {

	public static Vector4f getPoint4f(Vector3f p) {
		return new Vector4f(p.x, p.y, p.z, 1.0f);
	}
	
	public static Vector3f getPoint3f(Vector4f p) {
		return new Vector3f(p.x/p.w, p.y/p.w, p.z/p.w);
	}
}
