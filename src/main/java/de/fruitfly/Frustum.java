package de.fruitfly;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import de.fruitfly.map.Face;
import de.fruitfly.map.Plane;
import de.fruitfly.map.Side;
import de.fruitfly.map.Vertex;

public class Frustum {

	private float fovx, fovy;
	private Plane[] planes;
	private Matrix3f rotX = new Matrix3f();
	private Matrix3f rotZ = new Matrix3f();
	public Plane frontPlane;
	
	public Frustum(float fovx, float fovy) {
		this.fovx = fovx;
		this.fovy = fovy;
		this.planes = new Plane[4];
		for (int i=0; i<4; i++) {
			this.planes[i] = new Plane(new Vector3f(), 0.0f);
		}
	}

	public void update(Vector3f location, float yaw, float pitch) {
		float y = (float) Math.cos(fovy/2.0f);
		float z = (float) Math.sin(fovy/2.0f);
		float x = (float)(Math.sin((fovx )/1.8f)); // TODO: WHY WHY WHY DELILA...
		
		//location = new Vector3f(0.0f, 0.0f, 0.0f);
		
		Matrix3f.setIdentity(rotX);
		Matrix3f.setIdentity(rotZ);
		
		// http://en.wikipedia.org/wiki/Rotation_matrix
		rotZ.m00 = (float) Math.cos(-yaw+Math.PI/2.0f);
		rotZ.m01 = (float) -Math.sin(-yaw+Math.PI/2.0f);
		
		rotZ.m10 = (float) Math.sin(-yaw+Math.PI/2.0f);
		rotZ.m11 = (float) Math.cos(-yaw+Math.PI/2.0f);
				
		rotX.m11 = (float) Math.cos(-pitch);
		rotX.m12 = (float) -Math.sin(-pitch);
		
		rotX.m21 = (float) Math.sin(-pitch);
		rotX.m22 = (float) Math.cos(-pitch);
				
		Vector3f v1 = new Vector3f(-x, y, z);
		Vector3f v2 = new Vector3f(-x, y, -z);
		Vector3f v3 = new Vector3f(x, y, -z);
		Vector3f v4 = new Vector3f(x, y, z);

		v1 = Matrix3f.transform(rotX, v1, null);
		v2 = Matrix3f.transform(rotX, v2, null);
		v3 = Matrix3f.transform(rotX, v3, null);
		v4 = Matrix3f.transform(rotX, v4, null);
		
		v1 = Matrix3f.transform(rotZ, v1, null);
		v2 = Matrix3f.transform(rotZ, v2, null);
		v3 = Matrix3f.transform(rotZ, v3, null);
		v4 = Matrix3f.transform(rotZ, v4, null);
		
		
		Vector3f.add(v1, location, v1);
		Vector3f.add(v2, location, v2);
		Vector3f.add(v3, location, v3);
		Vector3f.add(v4, location, v4);
		
		Vector3f r = new Vector3f();
		this.planes[0].setFromPoints(v4, location, v1); // top
		this.planes[1].setFromPoints(v3, location, v4); // right
		this.planes[2].setFromPoints(v2, location, v3); // bottom
		this.planes[3].setFromPoints(v1, location, v2); // left

	
		Vector3f yAxis = new Vector3f(0.0f, 1.0f, 0.0f);
		yAxis = Matrix3f.transform(rotX, yAxis, null);
		yAxis = Matrix3f.transform(rotZ, yAxis, null);
		
		yAxis.normalise();
		float distance = Vector3f.dot(yAxis, location);
		frontPlane = new Plane(yAxis, distance);
	}
	
	public boolean contains(Vector3f v) {
		for (Plane p : planes) {
			if (p.classifyPoint(v) == Side.Back) return false;
		}
		return true;
	}
	
	public void render(Camera cam) {
		if (Game.viewCam != Game.freeCam) return;
		{
		float y = (float) Math.cos(fovy/2.0f);
		float z = (float) Math.sin(fovy/2.0f);
		float x = (float)(Math.sin((fovx )/1.8f));
		
		Vector3f v1 = new Vector3f(-x, y, z);
		Vector3f v2 = new Vector3f(-x, y, -z);
		Vector3f v3 = new Vector3f(x, y, -z);
		Vector3f v4 = new Vector3f(x, y, z);
		
		glPushMatrix();
		glTranslatef(Game.playerCam.getPosition().x, Game.playerCam.getPosition().y, Game.playerCam.getPosition().z);
		glRotatef((float)(Game.playerCam.getYaw()/Math.PI * 180.0f - 90.0f), 0.0f, 0.0f, 1.0f);
		glRotatef((float)(Game.playerCam.getPitch()/Math.PI * 180.0f), 1.0f, 0.0f, 0.0f);
		
		glColor3f(1.0f, 0.0f, 0.0f);

		

		glBegin(GL_LINE_LOOP);
			glVertex3f(v1.x, v1.y, v1.z);
			glVertex3f(v2.x, v2.y, v2.z);
			glVertex3f(v3.x, v3.y, v3.z);
			glVertex3f(v4.x, v4.y, v4.z);
		glEnd();
		
		v1.scale(5.0f);
		v2.scale(5.0f);
		v3.scale(5.0f);
		v4.scale(5.0f);
		glBegin(GL_LINE_LOOP);
			glVertex3f(v1.x, v1.y, v1.z);
			glVertex3f(v2.x, v2.y, v2.z);
			glVertex3f(v3.x, v3.y, v3.z);
			glVertex3f(v4.x, v4.y, v4.z);
		glEnd();
		
		glBegin(GL_LINES);
			glVertex3f(0.0f, 0.0f, 0.0f);
			glVertex3f(v1.x, v1.y, v1.z);
			
			glVertex3f(0.0f, 0.0f, 0.0f);
			glVertex3f(v2.x, v2.y, v2.z);
			
			glVertex3f(0.0f, 0.0f, 0.0f);
			glVertex3f(v3.x, v3.y, v3.z);
			
			glVertex3f(0.0f, 0.0f, 0.0f);
			glVertex3f(v4.x, v4.y, v4.z);
		glEnd();
		
		glPopMatrix();
		}
		{
			float y = (float) Math.cos(fovy/2.0f);
			float z = (float) Math.sin(fovy/2.0f);
			float x = (float)(Math.sin((fovx )/1.8f)); // TODO: WHY WHY WHY DELILA...
			
			//location = new Vector3f(0.0f, 0.0f, 0.0f);
			
			Matrix3f.setIdentity(rotX);
			Matrix3f.setIdentity(rotZ);
			
			// http://en.wikipedia.org/wiki/Rotation_matrix
			rotZ.m00 = (float) Math.cos(-cam.getYaw()+Math.PI/2.0f);
			rotZ.m01 = (float) -Math.sin(-cam.getYaw()+Math.PI/2.0f);
			
			rotZ.m10 = (float) Math.sin(-cam.getYaw()+Math.PI/2.0f);
			rotZ.m11 = (float) Math.cos(-cam.getYaw()+Math.PI/2.0f);
					
			rotX.m11 = (float) Math.cos(-cam.getPitch());
			rotX.m12 = (float) -Math.sin(-cam.getPitch());
			
			rotX.m21 = (float) Math.sin(-cam.getPitch());
			rotX.m22 = (float) Math.cos(-cam.getPitch());
					
			Vector3f v1 = new Vector3f(-x, y, z);
			Vector3f v2 = new Vector3f(-x, y, -z);
			Vector3f v3 = new Vector3f(x, y, -z);
			Vector3f v4 = new Vector3f(x, y, z);

			v1 = Matrix3f.transform(rotX, v1, null);
			v2 = Matrix3f.transform(rotX, v2, null);
			v3 = Matrix3f.transform(rotX, v3, null);
			v4 = Matrix3f.transform(rotX, v4, null);
			
			v1 = Matrix3f.transform(rotZ, v1, null);
			v2 = Matrix3f.transform(rotZ, v2, null);
			v3 = Matrix3f.transform(rotZ, v3, null);
			v4 = Matrix3f.transform(rotZ, v4, null);
			
			Vector3f.add(v1, cam.getPosition(), v1);
			Vector3f.add(v2, cam.getPosition(), v2);
			Vector3f.add(v3, cam.getPosition(), v3);
			Vector3f.add(v4, cam.getPosition(), v4);
			
			glBegin(GL_TRIANGLES);
				glColor3f(1.0f, 0.0f, 0.0f);
				glVertex3f(v4.x, v4.y, v4.z);
				glVertex3f(cam.getPosition().x, cam.getPosition().y, cam.getPosition().z);
				glVertex3f(v1.x, v1.y, v1.z);
				
				glColor3f(0.0f, 1.0f, 0.0f);
				glVertex3f(v3.x, v3.y, v3.z);
				glVertex3f(cam.getPosition().x, cam.getPosition().y, cam.getPosition().z);
				glVertex3f(v4.x, v4.y, v4.z);
				
				glColor3f(0.0f, 0.0f, 1.0f);
				glVertex3f(v2.x, v2.y, v2.z);
				glVertex3f(cam.getPosition().x, cam.getPosition().y, cam.getPosition().z);
				glVertex3f(v3.x, v3.y, v3.z);
				
				glColor3f(1.0f, 0.0f, 1.0f);
				glVertex3f(v1.x, v1.y, v1.z);
				glVertex3f(cam.getPosition().x, cam.getPosition().y, cam.getPosition().z);
				glVertex3f(v2.x, v2.y, v2.z);
			glEnd();
		}
		
	}

	public float getFovx() {
		return fovx;
	}

	public float getFovy() {
		return fovy;
	}
}
