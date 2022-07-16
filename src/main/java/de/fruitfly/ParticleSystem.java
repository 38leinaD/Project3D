package de.fruitfly;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.GL_COORD_REPLACE;
import static org.lwjgl.opengl.GL20.GL_POINT_SPRITE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.Color;

public class ParticleSystem {
	
	private List<Particle> usedParticles;
	private List<Particle> unusedParticles;
	
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
	
	public ParticleSystem(int size) {
		usedParticles = new ArrayList<ParticleSystem.Particle>(size);
		unusedParticles = new ArrayList<ParticleSystem.Particle>(size);
		
		for (int i=0; i<size; i++) {
			unusedParticles.add(new Particle());
		}
	}
	
	private List<Particle> handlerList = new ArrayList<ParticleSystem.Particle>(100);
	public List<Particle> getParticles(int num) {
		if (num > unusedParticles.size() + usedParticles.size()) System.out.println("*** Requesting " + num + " particles; exceeding size of particle system");
		else if (num > unusedParticles.size()) System.out.println("*** Requsting " + num + " paricles; exceeding current unused particles.");
		
		handlerList.clear();
		while (unusedParticles.size() > 0 && num > 0) {
			handlerList.add(unusedParticles.remove(0));
			num--;
		}
		
		while (usedParticles.size() > 0 && num > 0) {
			handlerList.add(usedParticles.remove(0));
			num--;
		}
		usedParticles.addAll(handlerList);
		
		for (Particle p : handlerList) {
			p.ticker = 0;
		}
		
		return handlerList;
	}
	
	public void tick() {
		Vector3f cp = Game.viewCam.getPosition();
		for (int i=usedParticles.size()-1; i>0; i--) {
			Particle p = usedParticles.get(i);
			p.ticker++;
			if (p.ticker >= p.lifetime) {
				unusedParticles.add(usedParticles.remove(i));
			}
			else {
				p.p.x += p.v.x * 0.2f;
				p.p.y += p.v.y * 0.2f;
				p.p.z += p.v.z * 0.2f;
				
				p.distance = (float)Math.sqrt((p.p.x - cp.x) * (p.p.x - cp.x) + (p.p.y - cp.y) * (p.p.y - cp.y) + (p.p.z - cp.z) * (p.p.z - cp.z));
			}
		}
	}
	
	public void render() {
		Game.parTex.bind();
		glEnable(GL_BLEND);
		
		
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glTexEnvi(GL_POINT_SPRITE, GL_COORD_REPLACE, GL_TRUE);
		//glColor3f(1.0f, 1.0f, 1.0f);
		
		Collections.sort(usedParticles);
		
		for (Particle p : usedParticles) {
			glPointSize(100/p.distance);
			glBegin(GL_POINTS);

			glColor3f(p.c.r, p.c.g, p.c.b);
			glVertex3f(p.p.x, p.p.y, p.p.z);
			glEnd();
		}

		glDisable(GL_TEXTURE_2D);
	}
}
