package de.fruitfly;

import static org.lwjgl.opengl.GL11.*;
import org.newdawn.slick.opengl.Texture;

public class Skybox {
	private Texture texture;
	private float dimension;

	public Skybox(Texture tex, float zFar) {
		this.texture = tex;
		this.texture.bind();
		glTexParameteri( GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP );
		glTexParameteri( GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP );
		
		this.dimension = (float) (2 * Math.sqrt(3)/3 * zFar);
	}
	
		
	public void render() {
		float dh = dimension/2.0f;
		float e = 0.002f;
		texture.bind();
		glPushMatrix();
		glRotatef(130.0f, 0.0f, 0.0f, 1.0f);
		glColor3f(1.0f, 1.0f, 1.0f);
		// bottom
		glBegin(GL_QUADS);
			glTexCoord2f(0.25f + e, 0.0f + e);
			glVertex3f(-dh, -dh, -dh);
			glTexCoord2f(0.5f - e, 0.0f + e);
			glVertex3f(dh, -dh, -dh);
			glTexCoord2f(0.5f - e, 0.25f - e);
			glVertex3f(dh, dh, -dh);
			glTexCoord2f(0.25f + e, 0.25f - e);
			glVertex3f(-dh, dh, -dh);
		glEnd();
		
		// top
		glBegin(GL_QUADS);
			glTexCoord2f(0.25f + e, 0.75f - e);
			glVertex3f(-dh, -dh, dh);
			glTexCoord2f(0.5f - e, 0.75f - e);
			glVertex3f(dh, -dh, dh);
			glTexCoord2f(0.5f - e, 0.5f + e);
			glVertex3f(dh, dh, dh);
			glTexCoord2f(0.25f + e, 0.5f + e);
			glVertex3f(-dh, dh, dh);
		glEnd();
		
		// left
		glBegin(GL_QUADS);
			glTexCoord2f(0.0f + e, 0.25f + e);
			glVertex3f(-dh, -dh, -dh);
			glTexCoord2f(0.25f - e, 0.25f + e);
			glVertex3f(-dh, dh, -dh);
			glTexCoord2f(0.25f - e, 0.5f - e);
			glVertex3f(-dh, dh, dh);
			glTexCoord2f(0.0f + e, 0.5f - e);
			glVertex3f(-dh, -dh, dh);
		glEnd();
		
		// right
		glBegin(GL_QUADS);
			glTexCoord2f(0.5f + e, 0.25f + e);
			glVertex3f(dh, dh, -dh);
			glTexCoord2f(0.75f - e, 0.25f + e);
			glVertex3f(dh, -dh, -dh);
			glTexCoord2f(0.75f - e, 0.5f - e);
			glVertex3f(dh, -dh, dh);
			glTexCoord2f(0.5f + e, 0.5f - e);
			glVertex3f(dh, dh, dh);
		glEnd();
		
		// front
		glBegin(GL_QUADS);
			glTexCoord2f(0.25f + e, 0.25f + e);
			glVertex3f(-dh, dh, -dh);
			glTexCoord2f(0.5f - e, 0.25f + e);
			glVertex3f(dh, dh, -dh);
			glTexCoord2f(0.5f - e, 0.5f - e);
			glVertex3f(dh, dh, dh);
			glTexCoord2f(0.25f + e, 0.5f - e);
			glVertex3f(-dh, dh, dh);
		glEnd();
		
		// back
		glBegin(GL_QUADS);
			glTexCoord2f(1.0f - e, 0.25f + e);
			glVertex3f(-dh, -dh, -dh);
			glTexCoord2f(0.75f + e, 0.25f + e);
			glVertex3f(dh, -dh, -dh);
			glTexCoord2f(0.75f + e, 0.5f - e);
			glVertex3f(dh, -dh, dh);
			glTexCoord2f(1.0f - e, 0.5f - e);
			glVertex3f(-dh, -dh, dh);
		glEnd();
		
		glPopMatrix();
		
	}
}
