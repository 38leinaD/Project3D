package de.fruitfly;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Vector3f;

import de.fruitfly.Game.Binding;
import de.fruitfly.collision.Collision;
import de.fruitfly.collision.PolygonCollision;
import de.fruitfly.map.Face;

public class Entity {
	
	private Vector3f acceleration;
	private Vector3f velocity;
	private Vector3f position, newPosition;
	private float yaw, pitch, roll;
	
	public Entity() {
		this.acceleration = new Vector3f(0.0f, 0.0f, 0.0f);
		this.velocity = new Vector3f(0.0f, 0.0f, 0.0f);
		this.position = new Vector3f(1.0f, 0.0f, zCollisionRadius);
		this.newPosition = new Vector3f(position);
	}
	
	public void tick() {
		Vector3f direction = new Vector3f(0.0f, 0.0f, 0.0f);
		boolean jump = false;
		
		if (Game.controlBinding == Binding.Player) {
			if (Game.input.keyPressed(Input.StrafeLeft) && velocity.z == 0.0f) {
				direction.x -= 1.5f * (float) Math.sin(getYaw());
				direction.y += 1.5f * (float) Math.cos(getYaw());
			}
			else if (Game.input.keyPressed(Input.StrafeRight) && velocity.z == 0.0f) {
				direction.x += 1.5f * (float) Math.sin(getYaw());
				direction.y -= 1.5f * (float) Math.cos(getYaw());
			}
			
			if (Game.input.keyPressed(Input.Forward)) {
				direction.x +=  (float) Math.cos(getYaw());
				direction.y += (float) Math.sin(getYaw());
			}
			else if (Game.input.keyPressed(Input.Backward)) {
				direction.x -=  (float) Math.cos(getYaw());
				direction.y -= (float) Math.sin(getYaw());
			}		
					
			jump = Game.input.keyJustPressed(Input.Jump) && velocity.z == 0.0f;
			
			if (Mouse.isGrabbed()) {
				float dx = Mouse.getDX() / 500.0f;
				float dy = Mouse.getDY() / 500.0f;
				
				setYaw(getYaw() - dx);
				setPitch(getPitch() + dy);
			}
			
		}

		if (direction.x > 0.0f || direction.y > 0.0f || direction.z > 0.0f) direction.normalise();

		//acceleration.x += direction.x;
		//acceleration.y += direction.y;
		acceleration.z = - 0.01f;
		
		velocity.x = direction.x * 0.2f;
		velocity.y = direction.y * 0.2f;
		velocity.z += acceleration.z + (jump ? 0.25f : 0.0f);

		if (velocity.x > 2.0f) velocity.x = 2.0f;
		if (velocity.y > 2.0f) velocity.y = 2.0f;
				
		newPosition.x = position.x + velocity.x;
		newPosition.y = position.y + velocity.y;
		newPosition.z = position.z + velocity.z;
		
		Vector3f manhattenPos = new Vector3f(position);
		float newX = newPosition.x;
		float newY = newPosition.y;
		float newZ = newPosition.z;
				
		manhattenPos.x = newX;
		newPosition.set(Collision.handleCollision(this, Game.bspTree.fullFaceList, position, manhattenPos));

		manhattenPos.set(newPosition);
		manhattenPos.y = newY;
		newPosition.set(Collision.handleCollision(this, Game.bspTree.fullFaceList, newPosition, manhattenPos));
		
		manhattenPos.set(newPosition);
		manhattenPos.z = newZ;
		newPosition.set(Collision.handleCollision(this, Game.bspTree.fullFaceList, newPosition, manhattenPos));

		if (manhattenPos.z != newPosition.z) {
			velocity.z = 0.0f;
		}
		
		position.set(newPosition);
	}
	
	public void renderCollider() {
		Vector3f pos = position;
		glColor3f(0.0f, 1.0f, 0.0f);
		glBegin(GL_LINE_LOOP);
			for (float t=0.0f; t<Math.PI * 2; t+=0.2f) {
				glVertex3f((float)(pos.x + xyCollisionRadius * Math.cos(t)), pos.y, (float)(pos.z + zCollisionRadius * Math.sin(t)));
			}
		glEnd();
		
		glBegin(GL_LINE_LOOP);
			for (float t=0.0f; t<Math.PI * 2; t+=0.2f) {
				glVertex3f(pos.x, (float)(pos.y + xyCollisionRadius * Math.cos(t)), (float)(pos.z + zCollisionRadius * Math.sin(t)));
			}
		glEnd();
		
		glBegin(GL_LINE_LOOP);
			for (float t=0.0f; t<Math.PI * 2; t+=0.2f) {
				glVertex3f((float)(pos.x + xyCollisionRadius * Math.sin(t)), (float)(pos.y + xyCollisionRadius * Math.cos(t)), pos.z);
			}
		glEnd();
		
		Vector3f direction = new Vector3f(1.0f * (float)Math.cos(yaw), 1.0f * (float)Math.sin(yaw), 0.0f);
		glColor3f(1.0f, 0.0f, 0.0f);
		glBegin(GL_LINES);
			glVertex3f(pos.x, pos.y, pos.z);
			glVertex3f(pos.x + direction.x, pos.y + direction.y, pos.z);
		glEnd();
	}
	
	private float xyCollisionRadius = 0.3f, zCollisionRadius = 1.0f;
	private float eyeHeight = 2 * zCollisionRadius * 0.8f;
	
	public float getEyeHeight() {
		return eyeHeight;
	}

	public float getXYCollisionRadius() {
		return xyCollisionRadius;
	}

	public void setXYCollisionRadius(float xyCollisionRadius) {
		this.xyCollisionRadius = xyCollisionRadius;
	}

	public float getZCollisionRadius() {
		return zCollisionRadius;
	}

	public void setZCollisionRadius(float zCollisionRadius) {
		this.zCollisionRadius = zCollisionRadius;
	}

	public Vector3f getPosition() {
		return position;
	}

	public void setPosition(Vector3f position) {
		this.position = position;
	}

	public float getPitch() {
		return pitch;
	}

	public void setPitch(float pitch) {
		this.pitch = pitch;
	}

	public float getRoll() {
		return roll;
	}

	public void setRoll(float roll) {
		this.roll = roll;
	}

	public float getYaw() {
		return yaw;
	}

	public void setYaw(float yaw) {
		this.yaw = yaw;
	}

	public Vector3f getNewPosition() {
		return newPosition;
	}

	public Vector3f getAcceleration() {
		return acceleration;
	}

	public Vector3f getVelocity() {
		return velocity;
	}
}
