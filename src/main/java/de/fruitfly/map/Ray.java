package de.fruitfly.map;

import org.lwjgl.util.vector.Vector3f;

public class Ray {
	private Vector3f origin;
	private Vector3f direction;
	public boolean positiveInfinity = false;
	public Ray(Vector3f origin, Vector3f direction) {
		this.origin = origin;
		this.direction = direction;
	}
	
	public static Ray fromPoints(Vector3f p1, Vector3f p2) {
		Vector3f direction = new Vector3f(p2);
		Vector3f.sub(p2, p1, direction);
		return new Ray(p1, direction);
	}

	public Float intersects(Face f) {
		// http://mrl.nyu.edu/~perlin/courses/fall2004ugrad/zorin.pdf
		
		// Parallel?
		if (Fuzzy.equal(Vector3f.dot(f.getPlane().getNormal(), this.direction), 0.0f)) return null;
		Vector3f p0 = f.getVertices().get(0).getPosition();
		Vector3f pq = new Vector3f(p0);
		Vector3f.sub(pq, this.origin, pq);
		float t_int = Vector3f.dot(pq, f.getPlane().getNormal()) / Vector3f.dot(direction, f.getPlane().getNormal());
		
		if (t_int < 0) return null;
		
		Vector3f x_int = new Vector3f(direction);
		x_int.scale(t_int);
		Vector3f.add(x_int, origin, x_int);
		
		for (int i=0; i<f.getVertices().size(); i++) {
			Vector3f p1 = f.getVertices().get(i).getPosition();
			Vector3f p2 = f.getVertices().get((i+1) % f.getVertices().size()).getPosition();
			Vector3f v = new Vector3f();
			Vector3f.sub(p2, p1, v);
			Vector3f q = new Vector3f();
			Vector3f.cross(f.getPlane().getNormal(), v, q);
			
			Vector3f r = new Vector3f();
			Vector3f.sub(x_int, p1, r);
			
			// fuzzy?
			if (Vector3f.dot(r, q) < -Const.EPSILON) return null; 
		}
		
		Vector3f r = new Vector3f();
		Vector3f.sub(x_int, origin, r);
		return r.length();
	}

	public Vector3f getOrigin() {
		return origin;
	}

	public void setOrigin(Vector3f origin) {
		this.origin = origin;
	}

	public Vector3f getDirection() {
		return direction;
	}

	public void setDirection(Vector3f direction) {
		this.direction = direction;
	}

	public void setInfinite() {
		positiveInfinity = true;
	}
}
