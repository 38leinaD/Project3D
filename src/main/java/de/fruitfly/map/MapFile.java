package de.fruitfly.map;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.Color;

public class MapFile {
	
	private int ptr;
	private String[] tokens;
	
	private List<Entity> entities;
	private Entity currentEntity;
	private Brush currentBrush;
	
	private class Stats {
		int numEntities = 0;
		int numBrushes = 0;
		int numPlanes = 0;
	};
	Stats stats = new Stats();
	
	public MapFile(InputStream is) {
		try {
			StringWriter writer = new StringWriter();
			IOUtils.copy(is, writer);
			String[] lines = writer.toString().split("[\\n\\r]");
			StringBuffer sb = new StringBuffer();
			for (int i=0; i<lines.length; i++) {
				if (lines[i].startsWith("//") || lines[i].trim().equals("")) continue;
				sb.append(lines[i]).append("\n");
			}
			tokens = sb.toString().split("[\\s\\n\\r]+");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		ptr = 0;
		
		entities = new LinkedList<Entity>();
	
		this.parse();
	}
	
	private void parse() {
		while (this.parseEntity()) {};
		
		System.out.println("=== Finished parsing ===");
		System.out.println(" #Entities: " + stats.numEntities);
		System.out.println(" #Brushes: " + stats.numBrushes);
		System.out.println(" #Planes: " + stats.numPlanes);
		
		currentBrush = null;
		currentEntity = null;
	}

	private boolean parseEntity() {
		String token = this.nextToken();
		if (token == null) return false;
		if (!token.equals("{")) {
			System.out.println("** Entity does not start with '{'");
			return false;
		}

		Map<String, String> props = this.parseProps();
		System.out.println(props);
		
		String className = props.get("classname");
		
		if (className.endsWith("worldspawn")) {
			this.currentEntity = new MapFile.BrushEntity();
		}
		else if (className.equals("light")) {
			this.currentEntity = new MapFile.LightEntity(
				createVector3f(props.get("origin")),
				Float.parseFloat(props.get("light")),
				createColor(props.get("_color"))
			);
		}
		else if (className.equals("func_player_start")) {
			this.currentEntity = new PlayerSpawnEntity(createVector3f(props.get("origin")));
		}
		else if (className.equals("func_flame")) {
			this.currentEntity = new FlameEntity(createVector3f(props.get("origin")));
		}
		else {
			
		}
		
		while (true) {
			token = this.nextToken();
			if (token.equals("}")) {
				break;
			}
			else {
				this.revertToken();
				if (!this.parseBrush()) break;
			}
		};

		this.entities.add(this.currentEntity);		
		this.stats.numEntities++;
		return true;
	}
	
	private Map<String, String> parseProps() {
		Map<String, String> props = new HashMap<String, String>();
		
		while (true) {
			String t1 = this.nextToken();
			if (t1 == null) {
				break;
			}
			if (t1.equals("{") || t1.equals("}")) {
				this.revertToken();
				break;
			}
			
			String t2 = this.nextToken();
			if (t2 == null) {
				break;
			}
			if (t2.equals("{") || t2.equals("}")) {
				this.revertToken();
				break;
			}
			
			props.put(t1.replace("\"", ""), t2.replace("\"", ""));
		}
		
		
		return props;
	}

	private boolean parseBrush() {
		String token = this.nextToken();
		if (!token.equals("{")) {
			System.out.println("** Brush does not start with '{''");
			return false;
		}

		this.currentBrush = new MapFile.Brush();
		if (!(this.currentEntity instanceof MapFile.BrushEntity)) {
			throw new RuntimeException("Parsing a non-brush entity.");
		}
		
		((MapFile.BrushEntity)this.currentEntity).brushes.add(this.currentBrush);

		while (true) {
			token = this.nextToken();
			if (token.equals("}")) {
				break;
			}
			else {
				this.revertToken();
				this.parsePlane();
			}
		};
		this.stats.numBrushes++;
		return true;
	}

	private boolean parsePlane() {
		String token = this.nextToken();
		if (!token.equals("(")) {
			System.out.println("** Plane does not start with '('");
			return false;
		}
		this.stats.numPlanes++;

		Vector3f p1 = new Vector3f();
		Vector3f p2 = new Vector3f();
		Vector3f p3 = new Vector3f();

		p1.set(Float.parseFloat(this.nextToken()), Float.parseFloat(this.nextToken()), Float.parseFloat(this.nextToken()));

		token = this.nextToken();
		if (!token.equals(")")) {
			System.out.println("** Expected ')'");
			return false;
		}

		token = this.nextToken();
		if (!token.equals("(")) {
			System.out.println("** Expected '('");
			return false;
		}

		p2.set(Float.parseFloat(this.nextToken()), Float.parseFloat(this.nextToken()), Float.parseFloat(this.nextToken()));
		
		token = this.nextToken();
		if (!token.equals(")")) {
			System.out.println("** Expected ')'");
			return false;
		}

		token = this.nextToken();
		if (!token.equals("(")) {
			System.out.println("** Expected '('");
			return false;
		}

		p3.set(Float.parseFloat(this.nextToken()), Float.parseFloat(this.nextToken()), Float.parseFloat(this.nextToken()));

		token = this.nextToken();
		if (!token.equals(")")) {
			System.out.println("** Expected ')'");
			return false;
		}

		this.nextToken();
		this.nextToken();
		this.nextToken();
		this.nextToken();
		this.nextToken();
		this.nextToken();

		this.nextToken();
		this.nextToken();
		this.nextToken();
		
		String brushType = this.nextToken();
		if (brushType.equals("d")) {
			currentBrush.setType(BrushType.DETAIL);
		}
		
		float r = Float.parseFloat(this.nextToken());
		float g = Float.parseFloat(this.nextToken()); 
		float b = Float.parseFloat(this.nextToken());
		
		currentBrush.setColor(new Color(r, g, b));
		
		Vector3f v1 = new Vector3f();
		Vector3f v2 = new Vector3f();
		Vector3f normal = new Vector3f();

		// todo: check for duplicates planes in brushes
		// todo: check for normal of length < epsilon
		// todo: texture-stuff
		
		Vector3f.sub(p1, p2, v1);
		Vector3f.sub(p3, p2, v2);

		Vector3f.cross(v1, v2, normal);
		normal.normalise();
		float dist = Vector3f.dot(p3, normal);

		Plane plane = new Plane(normal, dist);
		
		this.currentBrush.planes.add(plane);

		this.stats.numPlanes++;

		return true;
	}

	private void revertToken() {
		this.ptr--;
	}
	
	private String nextToken() {
		if (this.ptr >= this.tokens.length) return null;
		String t = this.tokens[this.ptr++];
		if (t.startsWith("\"") && !t.endsWith("\"")) {
			StringBuffer sb = new StringBuffer(t);
			while (true) {
				 t = this.tokens[this.ptr++];
				 sb.append(" " + t);
				 if (t.endsWith("\"")) {
					 return sb.toString();
				 }
			}
		}
		else {
			return t;
		}
	}
	
	public List<Entity> getEntities() {
		return entities;
	}

	public enum Side {
		Front, On, Back, Intersect
	}

	private static Vector3f createVector3f(String str) {
		String[] tokens =  str.split(" ");
		return new Vector3f(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]));
	}
	
	private static Color createColor(String str) {
		String[] tokens =  str.split(" ");
		return new Color(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]));
	}

	enum BrushType {
		STRUCTURAL,
		DETAIL
	}
	
	public class Brush {
		
		private List<Plane> planes;
		private BrushType type;
		private Color color;
		
		public Brush() {
			this.planes = new LinkedList<Plane>();
			this.type = BrushType.STRUCTURAL;
			this.color = Color.white;
		}
		
		public void addPlane(Plane p) {
			this.planes.add(p);
		}

		public List<Plane> getPlanes() {
			return planes;
		}

		public BrushType getType() {
			return type;
		}

		public void setType(BrushType type) {
			this.type = type;
		}

		public Color getColor() {
			return color;
		}

		public void setColor(Color color) {
			this.color = color;
		}
	}
	
	public abstract class Entity {
		
	}
	
	public class BrushEntity extends Entity {
		private List<Brush> brushes;
		
		public BrushEntity() {
			this.brushes = new LinkedList<Brush>();
		}
		
		public void addBrush(Brush b) {
			this.brushes.add(b);
		}

		public List<Brush> getBrushes() {
			return brushes;
		}
	}
	
	public class LightEntity extends Entity {
		private Vector3f position;
		private float distance;
		private Color color;
		
		public LightEntity(Vector3f position, float distance, Color color) {
			this.position = position;
			this.distance = distance;
			this.color = color;
		}

		public Vector3f getPosition() {
			return position;
		}

		public float getDistance() {
			return distance;
		}

		public Color getColor() {
			return color;
		}
	}
	
	public class PlayerSpawnEntity extends Entity {
		private Vector3f position;
		
		public PlayerSpawnEntity(Vector3f position) {
			this.position = position;
		}

		public Vector3f getPosition() {
			return position;
		}
	}
	
	public class FlameEntity extends Entity {
		private Vector3f position;
		
		public FlameEntity(Vector3f position) {
			this.position = position;
		}

		public Vector3f getPosition() {
			return position;
		}
	}
}
