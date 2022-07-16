package de.fruitfly;

import static org.lwjgl.opengl.GL11.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;

// http://tfc.duke.free.fr/coding/md2-specs-en.html
public class MD2Model {
	private int skinWidth, skinHeight;

	private Vector2f[] texCoords;
	private Tri[] tris;
	private Frame[] frames;
	
	private Texture[] skins;
	
	private class Frame {
		Vector3f scale;
		Vector3f translate;
		String name;
		
		Vertex[] vertices;
	}
	
	private class Vertex {
		Vector3f position = new Vector3f();
		Vector3f normal = new Vector3f();
	}
	
	private class Tri {
		int[] vertexIndices = new int[3];
		int[] texCoordIndices = new int[3];
	}
	
	public MD2Model(File f) {
		try {
			RandomAccessFile raf = new RandomAccessFile(f, "r");		

			int magic = readInt(raf);
			if (magic != 844121161) throw new RuntimeException("No MD2 file.");
			
			int version = readInt(raf);
			skinWidth = readInt(raf);
			skinHeight = readInt(raf);
			
			int frameSize = readInt(raf);
			
			int num_skins = readInt(raf);
			int num_vertices = readInt(raf);
			int num_st = readInt(raf);
			int num_tris = readInt(raf);
			int num_glcmds = readInt(raf);
			int num_frames = readInt(raf);
			
			int offset_skins = readInt(raf);
			int offset_st = readInt(raf);
			int offset_tris = readInt(raf);
			int offset_frames = readInt(raf);
			int offset_glcmds = readInt(raf);
			int offset_end = readInt(raf);
			
			System.out.println("num_skins: " + num_skins);
			System.out.println("num_vertices: " + num_vertices);
			System.out.println("num_st: " + num_st);
			System.out.println("num_tris: " + num_tris);
			System.out.println("num_glcmds: " + num_glcmds);
			System.out.println("num_frames: " + num_frames);
			
			System.out.println("offset_skins: " + offset_skins);
			System.out.println("offset_st: " + offset_st);
			System.out.println("offset_tris: " + offset_tris);
			System.out.println("offset_frames: " + offset_frames);
			System.out.println("offset_glcmds: " + offset_glcmds);
			System.out.println("offset_end: " + offset_end);
			
			// read skins
			skins = new Texture[num_skins];
			raf.seek(offset_skins);
			for (int i=0; i<num_skins; i++) {
				String skn = readString(raf, 64);
				skins[i] = TextureLoader.getTexture("PNG", Game.class.getResourceAsStream("/green.png"));
			}

			// read st
			texCoords = new Vector2f[num_st];
			raf.seek(offset_st);
			for (int i=0; i<num_st; i++) {
				texCoords[i] = new Vector2f();
				texCoords[i].x = readShort(raf) / (float)skinWidth;
				texCoords[i].y = readShort(raf) / (float)skinHeight;
			}
			
			//read triangles
			tris = new Tri[num_tris];
			raf.seek(offset_tris);
			for (int i=0; i<num_tris; i++) {
				tris[i] = new Tri();
				tris[i].vertexIndices[0] = readShort(raf);
				tris[i].vertexIndices[1] = readShort(raf);
				tris[i].vertexIndices[2] = readShort(raf);
				tris[i].texCoordIndices[0] = readShort(raf);
				tris[i].texCoordIndices[1] = readShort(raf);
				tris[i].texCoordIndices[2] = readShort(raf);
			}
			
			// read frames
			raf.seek(offset_frames);
			System.out.println("oofset0: " + raf.getFilePointer());
			frames = new Frame[num_frames];
			for (int i=0; i<num_frames; i++) {
				Frame fr = new Frame();
				frames[i] = fr;
				
				fr.scale = new Vector3f();
				fr.scale.x = readFloat(raf) / 16;
				fr.scale.y = readFloat(raf) / 16;
				fr.scale.z = readFloat(raf) / 16;
				
				fr.translate = new Vector3f();
				fr.translate.x = readFloat(raf) / 16;
				fr.translate.y = readFloat(raf) / 16;
				fr.translate.z = readFloat(raf) / 16;
				
				fr.name = readString(raf, 16);
				System.out.println("Frame " + fr.name);
				
				fr.vertices = new Vertex[num_vertices];
				for (int j=0; j<num_vertices; j++) {
					Vertex v = new Vertex();
					fr.vertices[j] = v;
					
					v.position.x = raf.readUnsignedByte() * fr.scale.x + fr.translate.x;
					v.position.y = raf.readUnsignedByte() * fr.scale.y + fr.translate.y;
					v.position.z = raf.readUnsignedByte() * fr.scale.z + fr.translate.z;
					
					raf.readByte(); // normal
				}
				System.out.println("oofset: " + raf.getFilePointer());
			}
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void render() {
		glPushMatrix();
		glTranslatef(2.0f, -4.0f, 12.5f);
		renderLerp(curFrame, 40 + (curFrame + 1) % 40, tt);
		//renderLerp(curFrame, (curFrame + 1) % 30, tt);
		glPopMatrix();
	}
	
	float tt = 0.0f;
	int curFrame = 0;
	public void tick() {
		tt+=0.2f;
		
		if (tt > 1.0f) {
			curFrame = 40 + (curFrame + 1) % 40;
			tt = 0.0f;
		}
	}
	
	private void renderLerp(int n, int m, float t) {
		if (n < 0 || n >= frames.length) {
			System.out.println("Frame " + n + " out of range [0.."
					+ (frames.length - 1) + "].");
			return;
		}
		glEnable(GL_TEXTURE_2D);
		skins[0].bind();

		Frame curFrame = frames[n];
		Frame nextFrame = frames[m];
		
		Vector3f lerpPos = new Vector3f();
		
		glBegin(GL_TRIANGLES);
		for (int i = 0; i < tris.length; i++) {
			Tri tri = tris[i];
			/* Draw each vertex */
			for (int j = 0; j < 3; j++) {
				Vertex curVtx = curFrame.vertices[tri.vertexIndices[j]];
				Vertex nextVtx = nextFrame.vertices[tri.vertexIndices[j]];

				Vector2f texCoord = texCoords[tri.texCoordIndices[j]];

				glTexCoord2f(texCoord.x, texCoord.y);

				// glNormal3fv (anorms_table[pvert->normalIndex]);

				lerpPos.x = curVtx.position.x + (nextVtx.position.x - curVtx.position.x) * t;
				lerpPos.y = curVtx.position.y + (nextVtx.position.y - curVtx.position.y) * t;
				lerpPos.z = curVtx.position.z + (nextVtx.position.z - curVtx.position.z) * t;
				glVertex3f(lerpPos.x, lerpPos.y, lerpPos.z);
			}
		}
		glEnd();
	}
	
	private static int readInt(RandomAccessFile raf) throws IOException {
		int a = raf.readUnsignedByte();
		int b = raf.readUnsignedByte();
		int c = raf.readUnsignedByte();
		int d = raf.readUnsignedByte();

		return d << 24 | c << 16 | b << 8 | a;
	}
	
	private static short readShort(RandomAccessFile raf) throws IOException {
		int a = raf.readUnsignedByte();
		int b = raf.readUnsignedByte();

		return (short)(b << 8 | a);
	}
	
	private static String readString(RandomAccessFile raf, int len) throws IOException {
		byte[] buf = new byte[len];
		raf.readFully(buf);
		return new String(buf).split("\0")[0];
	}
	
	private static float readFloat(RandomAccessFile raf) throws IOException {
		int a = raf.readUnsignedByte();
		int b = raf.readUnsignedByte();
		int c = raf.readUnsignedByte();
		int d = raf.readUnsignedByte();
		
		return Float.intBitsToFloat(d << 24 | c << 16 | b << 8 | a);
	}
}
