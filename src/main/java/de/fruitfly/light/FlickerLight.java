package de.fruitfly.light;

import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.Color;

public class FlickerLight extends Light {

	public FlickerLight(Vector3f position, Color color, float distance) {
		super(position, color, distance);
		imDistance = distance;
	}

	private float imDistance;
	private float targetDistance;
	private int ticker;
	public void tick() {
		if ((ticker < 0 && imDistance - 0.1 < targetDistance) || (ticker >= 0 && imDistance + 0.1 > targetDistance)) {
			targetDistance = 0.5f * distance + 0.5f * (float) (Math.random() * distance);
			if (targetDistance < imDistance) ticker = -1;
			else ticker = 1;
		}
		imDistance = 0.6f * imDistance + 0.4f * targetDistance;
	}
	
	public float getDistance() {
		return imDistance;
	}
}
