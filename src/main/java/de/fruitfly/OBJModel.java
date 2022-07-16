package de.fruitfly;

import static org.lwjgl.opengl.GL11.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;

// http://en.wikipedia.org/wiki/Wavefront_.obj_file
public class OBJModel {
	
	private class Material {
		private Texture diffuseMap;
	}
	
	private class Group {
		private String name;
		private Material material;
		private List<Face> faces;
		
		public Group() {
			this.faces = new LinkedList<Face>();
		}
	}
	
	private class Vertex {
		private Vector3f position;
		private Vector2f texCoord;
		
		public Vertex(Vector3f pos, Vector2f tex) {
			this.position = pos;
			this.texCoord = tex;
		}
	}
	
	private class Face {
		private List<Vertex> vertices;
		
		public Face() {
			vertices = new LinkedList<Vertex>();
		}
	}
	
	private Map<String, Material> materials = new HashMap<String, OBJModel.Material>();
	private List<Group> groups = new LinkedList<Group>();
	
	public OBJModel(File f) {
		BufferedReader in;
		try {
			in = new BufferedReader(new FileReader(f));
			parseOBJHeader(in, f.getParentFile());
			parseOBJ(in);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private void parseOBJHeader(BufferedReader in, File refPath) throws IOException {
		
		while (true) {
			in.mark(0);
			String line = in.readLine();
			if (line == null) break;
			
			if (ignoreLine(line)) {
				continue;
			}
			else if (line.startsWith("mtllib")) {
				String mtlFilename = line.split(" ")[1];
				File mtlFile = new File(refPath, mtlFilename);
				parseMTL(new BufferedReader(new FileReader(mtlFile)), refPath);
				break;
			}
			else {
				in.reset();
				break;
			}
		}
	}

	private void parseOBJ(BufferedReader in) throws IOException {
		Group group = null;
		List<Vector3f> vtxPositions = new LinkedList<Vector3f>();;
		List<Vector2f> vtxTexCoords = new LinkedList<Vector2f>();;

		boolean processedFace = false;
		while (true) {
			String line = in.readLine();
			if (line == null) break;
			
			if (ignoreLine(line)) {
				continue;
			}
		
			if (line.startsWith("g")) {
				group = new Group();
				group.name = line.split(" ")[1];
				groups.add(group);
				

			}
			else if (line.startsWith("vt")) {
				String[] tokens = line.split(" ");
				Vector2f v = new Vector2f(Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]));
				vtxTexCoords.add(v);			
				
				if (processedFace) {
					processedFace = false;
					
					vtxPositions = new LinkedList<Vector3f>();
					vtxTexCoords = new LinkedList<Vector2f>();
				}
			}
			else if (line.startsWith("v")) {
				String[] tokens = line.split(" ");
				Vector3f v = new Vector3f(Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]), Float.parseFloat(tokens[3]));
				vtxPositions.add(v);
				
				if (processedFace) {
					processedFace = false;
					
					vtxPositions = new LinkedList<Vector3f>();
					vtxTexCoords = new LinkedList<Vector2f>();
				}
			}
			else if (line.startsWith("f")) {
				Face f = new Face();
				String[] tokens = line.split(" ");
				for (int i=1; i<tokens.length; i++) {
					String[] tokens2 = tokens[i].split("/");
					Vector3f pos = vtxPositions.get(Integer.parseInt(tokens2[0]) - 1); // indices are one-based
					Vector2f tex = vtxTexCoords.get(Integer.parseInt(tokens2[1]) - 1);
					Vertex vtx = new Vertex(pos, tex);
					f.vertices.add(vtx);
				}
				
				group.faces.add(f);
				
				processedFace = true;
			}
			else if (line.startsWith("usemtl")) {
				group.material = materials.get(line.split(" ")[1]);
			}
			else if (line.startsWith("s")) {
				
			}
		}
	}
	
	private void parseMTL(BufferedReader in, File refPath) throws IOException {
		Material mat = null;
		
		while (true) {
			String line = in.readLine();
			if (line == null) break;
			
			if (ignoreLine(line)) {
				continue;
			}
			
			if (line.startsWith("newmtl")) {
				String mtlName = line.split(" ")[1];
				mat = new Material();
				materials.put(mtlName, mat);
			}
			else if (line.startsWith("map_Kd")) {
				String texName = line.split(" ")[1];
				File texFile = new File(refPath, texName);
				Texture tex = TextureLoader.getTexture(texFile.getName().split("\\.")[1], new FileInputStream(texFile), true);
				mat.diffuseMap = tex;
			}
		}
	}
	
	private boolean ignoreLine(String line) {
		return line.startsWith("#") || line.trim().equals("");
	}
	
	public void render() {
		for (Group g : groups) {
			
			g.material.diffuseMap.bind();
			
			for (Face f : g.faces) {
				glBegin(GL_TRIANGLE_FAN);
				for (Vertex v : f.vertices) {
					glTexCoord2f(v.texCoord.x, v.texCoord.y);
					glVertex3f(v.position.x, v.position.y, v.position.z);					
				}
				glEnd();
			}
		}
	}
}
