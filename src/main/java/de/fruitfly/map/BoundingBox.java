package de.fruitfly.map;

import java.util.List;

import org.lwjgl.util.vector.Vector3f;

public class BoundingBox {
	
	private Vector3f min, max;
	
	public BoundingBox() {
		this((List<Vector3f>)null);
	}
	
	public BoundingBox(List<Vector3f> points) {
		this.min = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
		this.max = new Vector3f(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);	

		if (points == null || points.size() == 0) return;

		for (Vector3f p : points) {
			this.expand(p);
		}
	}
	
	public BoundingBox(Vector3f... points) {
		this.min = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
		this.max = new Vector3f(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);	
		
		for (Vector3f p : points) {
			this.expand(p);
		}
	}
	
	public BoundingBox(BoundingBox bbox) {
		this.min = new Vector3f(bbox.min);
		this.max = new Vector3f(bbox.max);
	}

	public boolean contains(Vector3f p) {
		if (p.x > this.max.x) return false;
		if (p.x < this.min.x) return false;
		
		if (p.y > this.max.y) return false;
		if (p.y < this.min.y) return false;
		
		if (p.z > this.max.z) return false;
		if (p.z < this.min.z) return false;
		
		return true;
	}

	public void expand(Vector3f p) {
		if (p.x > this.max.x) this.max.x = p.x;
		if (p.x < this.min.x) this.min.x = p.x;
	
		if (p.y > this.max.y) this.max.y = p.y;
		if (p.y < this.min.y) this.min.y = p.y;
		
		if (p.z > this.max.z) this.max.z = p.z;
		if (p.z < this.min.z) this.min.z = p.z;
	}

	public boolean intersects(BoundingBox bb) {
		if (this.min.x > bb.max.x || bb.min.x > this.max.x ||
			this.min.y > bb.max.y || bb.min.y > this.max.y ||
			this.min.z > bb.max.z || bb.min.z > this.max.z ) {  
			return false;
		}

		return true;
	}
}
