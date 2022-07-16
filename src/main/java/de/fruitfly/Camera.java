package de.fruitfly;

import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import de.fruitfly.Game.Binding;

public class Camera {
	private Vector3f position;
	private float yaw, pitch, roll;
	private Vector3f direction;
	private Frustum frustum;
	private Input input;
	private boolean directControl;
	public Camera(float fovx, float fovy, Input input) {
		this.position = new Vector3f(0.0f, 0.0f, 0.0f);
		this.yaw = 0.0f;
		this.pitch = 0.0f;
		this.roll = 0.0f;
		this.direction = new Vector3f();
		this.input = input;
		this.directControl = false;
		
		this.frustum = new Frustum(fovx, fovy);
	}

	public void attachTo(Entity entity) {
		this.position.set(entity.getPosition());
		this.position.z += entity.getEyeHeight() - entity.getZCollisionRadius();
		this.yaw = entity.getYaw();
		this.pitch = entity.getPitch();
		this.roll = entity.getRoll();
		
		this.update();
	}
	
	public Vector3f getPosition() {
		return position;
	}

	public void setPosition(Vector3f position) {
		this.position = position;
	}

	public float getYaw() {
		return yaw;
	}

	public void setYaw(float yaw) {
		this.yaw = yaw;
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

	private void update() {
		float up = (float) Math.sin(pitch);
		direction.set((float)Math.cos(yaw) , (float)Math.sin(yaw) , up);
		direction.normalise();
		this.frustum.update(position, yaw, pitch);
	}
	
	public Frustum getFrustum() {
		return frustum;
	}
	
	public void setDirectControl(boolean dc) {
		this.directControl = dc;
	}
	
	public void tick() {
		if (!this.directControl) return;
		
		if (input.keyPressed(Input.StrafeLeft)) {
			//cam.setYaw(cam.getYaw() + 0.05f);
			getPosition().x = (getPosition().x - 0.5f * (float) (Math.sin(getYaw())));
			getPosition().y = (getPosition().y + 0.5f * (float) (Math.cos(getYaw())));
		}
		if (input.keyPressed(Input.StrafeRight)) {
			//cam.setYaw(cam.getYaw() - 0.05f);
			
			
			getPosition().x = (getPosition().x + 0.5f * (float) (Math.sin(getYaw())));
			getPosition().y = (getPosition().y - 0.5f * (float) (Math.cos(getYaw())));
		}
		if (input.keyPressed(Input.Forward)) {
			
			
			getPosition().x = (getPosition().x + 0.5f * (float) (Math.cos(getYaw()) * Math.cos(getPitch())));
			getPosition().y = (getPosition().y + 0.5f * (float) (Math.sin(getYaw()) * Math.cos(getPitch())));
			getPosition().z = (getPosition().z + 0.5f * (float) (Math.sin(getPitch())));
		}
		if (input.keyPressed(Input.Backward)) {
			
			getPosition().x = (getPosition().x - 0.5f * (float) (Math.cos(getYaw()) * Math.cos(getPitch())));
			getPosition().y = (getPosition().y - 0.5f * (float) (Math.sin(getYaw()) * Math.cos(getPitch())));
			getPosition().z = (getPosition().z - 0.5f * (float) (Math.sin(getPitch())));
		}		
		
		if (Mouse.isGrabbed()) {
			float dx = Mouse.getDX() / 500.0f;
			float dy = Mouse.getDY() / 500.0f;
			
			setYaw(getYaw() - dx);
			setPitch(getPitch() + dy);
		}

		this.update();
	}

	public ReadableVector3f getDirection() {
		return direction;
	}
}
