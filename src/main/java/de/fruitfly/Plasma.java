package de.fruitfly;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.GL_POINT_SPRITE;

import java.util.LinkedList;
import java.util.List;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.Color;

import de.fruitfly.Flame.Particle;
import de.fruitfly.light.Light;

public class Plasma {

	public class Particle {
		public Vector3f p;
		public Vector3f v;
		public Vector3f a;
		public Color c;
		public int ticker;
		public int lifetime;
		
		public Particle() {
			this.p = new Vector3f();
			this.v = new Vector3f();
			this.a = new Vector3f();
			this.c = new Color(0.0f, 0.0f, 0.0f);
		}

	}
	
	private Vector3f position;
	private Vector3f direction;
	private Color color;
	private Light light;
	private List<Particle> particles = new LinkedList<Particle>();
	
	public Plasma(ReadableVector3f position, ReadableVector3f direction, Color color) {
		this.position = new Vector3f(position);
		this.direction = new Vector3f(direction);
		this.color = new Color(color);
		this.light = new Light(position, color, 5.0f);
		
		for (int i=0; i<30; i++) {
			Particle p = new Particle();
			p.p.set(position);
			p.v.set(direction);
			p.v.scale(0.5f);
			p.c.r = 1.0f;
			p.c.g = 0.0f;
			p.c.b = 0.0f;
			this.particles.add(p);
		}
		
		Game.dynamicLights.add(light);
	}
	
	public void render() {
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
	
	public void tick() {
		if (	position.x < -100 || position.x > 100 ||
				position.y < -100 || position.y > 100 ||
				position.z < -100 || position.z > 100) {
			
			Game.dynamicLights.remove(light);
			Game.plasmas.remove(this);
			System.out.println("REMOVED PLASMA");
			return;
		}

		position.x += direction.x * 0.5f;
		position.y += direction.y * 0.5f;
		position.z += direction.z * 0.5f;
		
		light.getPosition().set(position);
		
		for (Particle p : particles) {
			p.p.x += p.v.x * 0.5f;
			p.p.y += p.v.y * 0.5f;
			p.p.z += p.v.z * 0.5f;

			if (Math.sqrt((p.p.x - position.x) * (p.p.x - position.x) + (p.p.x - position.x) * (p.p.x - position.x) + (p.p.x - position.x) * (p.p.x - position.x)) > 5.5f) {
				p.p.set(position);
				p.v.x = ((float)(Math.random()) - 0.5f) * 0.2f;
				p.v.y = ((float)(Math.random()) - 0.5f) * 0.2f;
				p.v.z = ((float)(Math.random()) - 0.5f) * 0.2f;
				
				Vector3f.add(p.v, direction, p.v);
				p.v.scale(0.7f);
			}
		}
	}
}
