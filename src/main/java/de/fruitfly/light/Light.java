package de.fruitfly.light;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.Color;

public class Light {
	private Vector3f position;
	private Color color;
	protected float distance;
	
	public Light(ReadableVector3f position, Color color, float distance) {
		this.position = new Vector3f(position);
		this.color = color;
		this.distance = distance;
	}

	public Vector3f getPosition() {
		return position;
	}

	public Color getColor() {
		return color;
	}

	public float getDistance() {
		return distance;
	}

	@Override
	public String toString() {
		return "Light [position=" + position + ", color=" + color
				+ ", distance=" + distance + "]";
	}
}
