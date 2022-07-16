package de.fruitfly.map;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.Color;

public class Plane {

	private Vector3f normal;
	private float distance;
	
	public Plane(Vector3f normal, float distance) {
		this.normal = new Vector3f(normal);
		this.distance = distance;
	}
	
	public Plane(Plane p) {
		this.normal = new Vector3f(p.getNormal());
		this.distance = p.getDistance();
	}
	
	public boolean isCoplanarTo(Plane p) {
		float dot = Vector3f.dot(this.normal, p.normal);
		return Math.abs(dot - 1) < Const.EPSILON || Math.abs(dot + 1) < Const.EPSILON;
	}
	
	public boolean inverseOf(Plane p) {
		if (Math.abs(Vector3f.dot(this.normal, p.normal) + 1) > Const.EPSILON) {
			return false;
		}
		if (Math.abs(this.distance + p.distance) > Const.EPSILON) {
			return false;
		}
		return true;
	}
	
	public Side sideOf(Plane p) {
		if (!this.isCoplanarTo(p)) {
			return Side.Spanning;
		}
		if (this.distance - p.distance > Const.EPSILON) {
			return Side.Front;
		}
		else if (this.distance - p.distance < -Const.EPSILON) {
			return Side.Back;
		}
		else {
			return Side.On;
		}
	}
	
	public Side classifyPoint(Vector3f p) {
		float sideValue = Vector3f.dot(this.getNormal(), p);
		if (Fuzzy.equal(sideValue, this.getDistance())) {
			return Side.On;
		}
		else if (Fuzzy.less(sideValue, this.getDistance())) {
			return Side.Back;
		}
		else {
			return Side.Front;
		}
	}
	
	public Side classifyFace(Face f) {
		int numFront = 0;
		int numBack = 0;
		
		for (Vertex vtx : f.getVertices()) {
			Side sp = this.classifyPoint(vtx.getPosition());
			if (sp == Side.Front) {
				numFront++;
			}
			else if (sp == Side.Back) {
				numBack++;
			}
		}
		
		if (numFront > 0 && numBack == 0) {
			return Side.Front;
		}
		else if (numFront == 0 && numBack > 0) {
			return Side.Back;
		}
		else if (numFront == 0 && numBack == 0) {
			return Side.On;
		}
		else {
			return Side.Spanning;
		}
	}
	
	public Vector3f projectOntoPlane(Vector3f v) {
		float w = Vector3f.dot(v, getNormal());
		Vector3f u = new Vector3f(getNormal());
		u.scale(w);
		Vector3f.sub(v, u, u);
		return u;
	}
	
	public boolean equals(Plane p) {
		if (Math.abs(this.distance - p.distance) > Const.EPSILON) {
			return false;
		}

		if (Math.abs(Vector3f.dot(this.normal, p.normal) - 1) > Const.EPSILON) {
			return false;
		}
		return true;
	}
	
	public void setFromPoints(Vector3f v1, Vector3f v2, Vector3f v3) {
		Vector3f vv1 = new Vector3f(v1);
		Vector3f vv2 = new Vector3f(v3);
		Vector3f.sub(vv1, v2, vv1);
		Vector3f.sub(vv2, v2, vv2);
		Vector3f.cross(vv2, vv1, normal);
		normal.normalise();
		
		distance = Vector3f.dot(v1, normal);
	}

	public Vector3f getNormal() {
		return normal;
	}

	public void setNormal(Vector3f normal) {
		this.normal = normal;
	}

	public float getDistance() {
		return distance;
	}

	public void setDistance(float distance) {
		this.distance = distance;
	}

	@Override
	public String toString() {
		return "Plane [normal=" + normal + ", distance=" + distance + "]";
	}
}