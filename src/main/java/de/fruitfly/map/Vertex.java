package de.fruitfly.map;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

public class Vertex {
	private Vector3f position;
	private Vector2f lightUV;
	private Vector2f dyLightUV;
	private Vector2f baseLightUV;

	public Vertex(Vector3f position) {
		this.position = position;
		this.lightUV = new Vector2f();
		this.dyLightUV = new Vector2f();
		this.baseLightUV = new Vector2f();
	}

	public Vertex(Vertex v) {
		this.position = new Vector3f(v.getPosition());
		this.lightUV = new Vector2f(v.getLightUV());
		this.dyLightUV = new Vector2f(v.getDyLightUV());
		this.baseLightUV = new Vector2f();
	}
	
	public Vector3f getPosition() {
		return position;
	}

	public Vector2f getLightUV() {
		return lightUV;
	}
	
	public Vector2f getDyLightUV() {
		return dyLightUV;
	}

	public Vector2f getBaseLightUV() {
		return baseLightUV;
	}
	
	@Override
	public String toString() {
		return "Vertex [position=" + position + ", lightUV=" + lightUV + "]";
	}
}
