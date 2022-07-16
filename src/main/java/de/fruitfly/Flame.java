package de.fruitfly;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.GL_POINT_SPRITE;

import java.util.LinkedList;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.Color;

import de.fruitfly.light.FlickerLight;

public class Flame {
	
	public class Particle implements Comparable<Particle>{
		public Vector3f p;
		public Vector3f v;
		public Vector3f a;
		public Color c;
		public int ticker;
		public int lifetime;
		public float distance;
		
		public Particle() {
			this.p = new Vector3f();
			this.v = new Vector3f();
			this.a = new Vector3f();
			this.c = new Color(0.0f, 0.0f, 0.0f);
		}

		@Override
		public int compareTo(Particle o) {
			if (this.distance < o.distance) return 1;
			else if (this.distance > o.distance) return -1;
			else return 0;
		}
	}
	
	private Vector3f position;
	private List<Particle> particles = new LinkedList<Particle>();
	public Flame(Vector3f pos) {
		this.position = new Vector3f(pos);
		
		for (int i=0; i<30; i++) {
			Particle p = new Particle();
			p.p.set(position);
			p.p.z += 10.0f;
			this.particles.add(p);
		}
		
		Game.dynamicLights.add(new FlickerLight(position, new Color(0.6f, 0.3f, 0.0f), 8.0f));
	}

	public void tick() {
		for (Particle p : particles) {
			p.p.x += p.v.x;
			p.p.y += p.v.y;
			p.p.z += p.v.z;
			
			p.v.set(p.v.x * p.a.x * 0.01f -0.03f* (p.p.x - position.x), p.v.y * p.a.y * 0.01f -0.03f* (p.p.y - position.y), p.v.z + p.a.z * 0.01f);

			
			if (p.p.z > position.z + 2.0f) {
				p.c.r = 0.6f;
				p.c.g = 0.3f;
				p.c.b = 0.0f;
				p.c.a = 1 - Math.abs(p.p.z - position.z);
				p.p.set(position);
				p.p.z += (float) (Math.random());
				p.a.set(0.0f, 0.0f, 0.2f);
				p.v.set((float)((Math.random()-0.5) * 0.5f), (float)((Math.random()-0.5) * 0.5f), (float)((Math.random() - 0.5) * 0.01f));
			}
		}
	}
	
	public void render() {
		//Game.parTex.bind();
		glEnable(GL_BLEND);
		
		glDisable(GL_TEXTURE_2D);
		glBlendFunc(GL_ONE, GL_SRC_ALPHA);
		//glTexEnvi(GL_POINT_SPRITE, GL_COORD_REPLACE, GL_TRUE);
		//glColor3f(1.0f, 1.0f, 1.0f);
		
		//Collections.sort(usedParticles);
		glEnable(GL_POINT_SPRITE);

		Vector3f distVec = Vector3f.sub(Game.viewCam.getPosition(), position, null);
		float len = distVec.length();
		for (Particle p : particles) {
			//glPointSize(100/p.distance);
			glPointSize(20 / len * 4 / Game.RT_SCALE);
			glBegin(GL_POINTS);

			glColor3f(p.c.r, p.c.g, p.c.b);
			glVertex3f(p.p.x, p.p.y, p.p.z);
			glEnd();
		}

		glDisable(GL_TEXTURE_2D);
	}
}
