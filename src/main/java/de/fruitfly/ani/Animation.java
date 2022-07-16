package de.fruitfly.ani;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL11.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;

import de.fruitfly.Camera;
import de.fruitfly.Game;

public class Animation {
	private List<Vector3f> pos = new LinkedList<Vector3f>();
	private List<Vector3f> rot = new LinkedList<Vector3f>();
	private Camera cam;
	private int ticker = 0;
	public Animation(InputStream is, Camera cam) {
		this.cam = cam;
		try {
			List<String> lines = IOUtils.readLines(is);
			parse(lines);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void parse(List<String> lines) {
		for (String line : lines) {		
			String[] t = line.split(" ");
			pos.add(new Vector3f(Float.parseFloat(t[0]), Float.parseFloat(t[1]), Float.parseFloat(t[2])));
			rot.add(new Vector3f(Float.parseFloat(t[3]), Float.parseFloat(t[4]), Float.parseFloat(t[5])));
		}
	}
	
	public void tick() {
		cam.getPosition().set(pos.get(ticker));
		Vector3f r = rot.get(ticker);
		cam.setYaw((float)(r.x+Math.PI));
		cam.setRoll(r.y);
		cam.setPitch(r.z);
		ticker = (ticker + 1) % pos.size();
	}
	
	public void render() {
		Game.parTex.bind();
		glEnable(GL_BLEND);
		glPointSize(100.0f);
		glColor3f(1.0f, 0.0f, 0.0f);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glEnable(GL_POINT_SPRITE);
		glTexEnvi(GL_POINT_SPRITE, GL_COORD_REPLACE, GL_TRUE);
		//glColor3f(1.0f, 1.0f, 1.0f);
		glBegin(GL_POINTS);
		for (Vector3f p : pos) {
			glVertex3f(p.x, p.y, p.z);
		}
		glEnd();

		glDisable(GL_TEXTURE_2D);

	}
}
