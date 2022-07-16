package de.fruitfly;

import java.util.LinkedList;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class Input {

	class Key {
		int keyCode;
		boolean isDown;
		boolean justDown;
		
		public Key(int keyCode) {
			this.keyCode = keyCode;
			keys.add(this);
		}
	}
	
	public static Key Forward;
	public static Key Backward;
	public static Key StrafeLeft;
	public static Key StrafeRight;
	public static Key Jump;

	public static Key ToggleControl;
	public static Key ToggleView;
	public static Key ToggleLight;
	public static Key ToggleFaceColor;
	public static Key ToggleTextureFilter;
	public static Key SpawnLight;

	public static Key Action;
	public static Key Dec;
	public static Key Inc;
	public static Key Clr;
	public static Key Do;

	private List<Key> keys = new LinkedList<Key>();
	
	public void init() {
		Forward = new Key(Keyboard.KEY_W);
		Backward = new Key(Keyboard.KEY_S);
		StrafeLeft = new Key(Keyboard.KEY_A);
		StrafeRight = new Key(Keyboard.KEY_D);
		Jump = new Key(Keyboard.KEY_SPACE);
		
		ToggleControl = new Key(Keyboard.KEY_C);
		ToggleView = new Key(Keyboard.KEY_V);
		ToggleLight = new Key(Keyboard.KEY_L);
		ToggleFaceColor = new Key(Keyboard.KEY_F);
		ToggleTextureFilter = new Key(Keyboard.KEY_T);

		SpawnLight = new Key(Keyboard.KEY_P);
		Action = new Key(Keyboard.KEY_RETURN);
		
		Dec = new Key(Keyboard.KEY_NUMPAD4);
		Inc = new Key(Keyboard.KEY_NUMPAD6);
		Do = new Key(Keyboard.KEY_NUMPAD5);
		Clr = new Key(Keyboard.KEY_NUMPAD0);

	}

	public void fetchEvents() {
		if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
			//System.exit(0);
			Mouse.setGrabbed(false);
		}
		
		if (Mouse.isInsideWindow() && Mouse.isButtonDown(0)) {
			Mouse.setGrabbed(true);
		}
		
		for (Key key : keys) {
			key.justDown = false;
		}
		
		while (Keyboard.next()) {
			int keyCode = Keyboard.getEventKey();
			boolean down = Keyboard.getEventKeyState();
			
			Key key = getKeyForKeycode(keyCode);
			if (key != null) {
				key.isDown = down;
				if (down) {
					key.justDown = true;
				}
			}
		}
	}
	
	private Key getKeyForKeycode(int code) {
		for (Key key : keys) {
			if (key.keyCode == code) {
				return key;
			}
		}
		return null;
	}
	
	public boolean keyPressed(Key key) {
		return key.isDown;
	}
	
	public boolean keyJustPressed(Key key) {
		return key.justDown;
	}

}
