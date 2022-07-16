package de.fruitfly.collision;

import org.lwjgl.util.vector.Vector3f;

import de.fruitfly.map.Face;

public class PolygonCollision {
	private Face face;
	private Vector3f newPosition;

	public PolygonCollision(Face face, Vector3f newPosition) {
		this.face = face;
		this.newPosition = newPosition;
	}

	public Face getFace() {
		return face;
	}

	public Vector3f getNewPosition() {
		return newPosition;
	}
}
