package de.fruitfly;

import static org.lwjgl.opengl.GL11.*;

import java.util.List;

import de.fruitfly.map.Const;

public class Frustum2D {
	/*
	public Frustum2D(float x, float y, float w, float h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
	}
	*/
	public float x;
	public float y;
	public float w;
	public float h;
	public float zRef;
	
	public Frustum2D(float xmin, float ymin, float xmax, float ymax) {
		this.x = xmin;
		this.y = ymin;
		this.w = xmax - xmin;
		this.h = ymax - ymin;
	}
	
	private Frustum2D() {
		
	}
	
	public Frustum2D clipAgainst(Frustum2D f) {
		Frustum2D r = new Frustum2D();
		r.zRef = this.zRef;
		r.x = Math.max(this.x, f.x);
		r.y = Math.max(this.y, f.y);
		float xx = Math.min(this.x + this.w, f.x + f.w);
		float yy = Math.min(this.y + this.h, f.y + f.h);
		r.w = xx - r.x;
		r.h = yy - r.y;
		
		if (r.w <= Const.EPSILON || r.h <= Const.EPSILON) return null;
		
		return r;
	}
	
	public boolean intersects(Frustum2D f) {
		return clipAgainst(f) != null;
	}
	
	public static Frustum2D join(List<Frustum2D> frustums) {
		float xmin = Float.POSITIVE_INFINITY;
		float ymin = Float.POSITIVE_INFINITY;
		float xmax = Float.NEGATIVE_INFINITY;
		float ymax = Float.NEGATIVE_INFINITY;
		
		for (Frustum2D f : frustums) {
			if (f.x < xmin) xmin = f.x;
			if (f.x + f.w > xmax) xmax = f.x + f.w;
			if (f.y < ymin) ymin = f.y;
			if (f.y + f.h > ymax) ymax = f.y + f.h;
		}
		
		return new Frustum2D(xmin, ymin, xmax, ymax);
	}
	
	public void render() {
		glBegin(GL_LINE_LOOP);
			glVertex2f(x, y);
			glVertex2f(x+w, y);
			glVertex2f(x+w, y+h);
			glVertex2f(x, y+h);
		glEnd();
	}
}
