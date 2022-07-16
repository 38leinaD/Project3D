package de.fruitfly.light;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureImpl;

import de.fruitfly.Game;
import de.fruitfly.map.Face;

public class Lightmap {

	private int[] pixels;
	private int width, height;
	private Texture tex;
	private IntBuffer buffer;
	
	// it happens (no matter if lummen or bleeding pixels are used) that lummen are partially under walls and thus there are shadows in corners.
	// if res is too low and bleeding pixels are used, you can see seams because interpolating is "stopped" at polygon borders
	
	public Lightmap() {
		root = new Node(new Box(0, 0, 1024, 1024));
		
		IntBuffer idBuffer = BufferUtils.createIntBuffer(1);
		GL11.glGenTextures(idBuffer);
		int texId = idBuffer.get(0);
		
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);

		width = 1024;
		height = 1024;
		buffer = BufferUtils.createIntBuffer(width * height);
		
		boolean hasAlpha = true;
		
		TextureImpl texture = new TextureImpl("lightmap", GL11.GL_TEXTURE_2D, texId);

		texture.setTextureWidth(width);
		texture.setTextureHeight(height);

		IntBuffer temp = BufferUtils.createIntBuffer(16);
		GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE, temp);
		int max = temp.get(0);
		if ((width > max) || (height > max)) {
			throw new RuntimeException("Attempt to allocate a texture to big for the current hardware");
		}
		 
		texture.setWidth(width);
		texture.setHeight(height);
		texture.setAlpha(hasAlpha);

		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
				
//		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
//		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		
		this.tex = texture;
		
		pixels = new int[width * height];
		// set some stuff
		
		int p = 0;
		for (int i=0; i<pixels.length; i++) {
			if (i % width  == 0) p++;
			//pixels[i] = (i+p) % 2 == 0 ? 0xff000000 : 0xffffffff;
			pixels[i] = 0xff000000;
		}
		//
		
		this.upload();
	}
	
	public void bind() {
		this.tex.bind();
	}
	
	public void setFilter(int filter) {
		this.bind();
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
	}
	
	public void upload() {		
		
		buffer.rewind();
		buffer.put(pixels);
		buffer.flip();
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
	}
	
	public int[] getPixels() {
		return pixels;
	}
	
	public int getSize() {
		return width;
	}
	
	// Lightmap packing algorithm: http://www.blackpawn.com/texts/lightmaps/
	
	private class Node {
		private Node lChild, rChild;
		private Box box;
		private boolean occupied = false;
		
		public Node(Box b) {
			this.box = b;
			this.occupied = false;
		}
	}
	
	class Box {
		int x, y, width, height;
		public Box(int x, int y, int w, int h) {
			this.x = x;
			this.y = y;
			this.width = w;
			this.height = h;
		}
		
		public Face face;
		@Override
		public String toString() {
			return "Box [x=" + x + ", y=" + y + ", width=" + width
					+ ", height=" + height + "]";
		}
	}
	
	private Node root;
	
	public void reset() {
		root = new Node(new Box(0, 0, 1024, 1024));
	}

	
	public Box getRegion(int width, int height) {
		Node n = _getRegion(width, height, root);
		clearBox(n.box);
		return n.box;
	}
	
	private Node _getRegion(int width, int height, Node node) {
		if (node.lChild != null || node.rChild != null) {
			Node newNode = _getRegion(width, height, node.lChild);
			if (newNode != null) return newNode;
			else return _getRegion(width, height, node.rChild);
		}
		else {
			if (node.occupied) {
				return null;
			}
			if (node.box.width < width || node.box.height < height) {
				return null;
			}
			if (node.box.width == width && node.box.height == height) {
				node.occupied = true;
				return node;
			}
			else {
				int dw = node.box.width - width;
				int dh = node.box.height - height;
						
				if (dw > dh) {
					node.lChild = new Node(new Box(node.box.x, node.box.y, width, node.box.height));
					node.rChild = new Node(new Box(node.box.x + width, node.box.y, node.box.width - width, node.box.height));
				}
				else {
					node.lChild = new Node(new Box(node.box.x, node.box.y, node.box.width, height));
					node.rChild = new Node(new Box(node.box.x, node.box.y + height, node.box.width, node.box.height - height));
				}
				return _getRegion(width, height, node.lChild);
			}
		}
	}
	
	private void clearBox(Box b) {
		for (int x=0; x<b.width; x++) {
			for (int y=0; y<b.height; y++) {
				pixels[(b.y + y) * this.getSize() + (b.x + x)] = 0x00000000;
			}
		}
	}
	
	private void bleedBox(Box b) {
		int[] pixels = this.getPixels();
	
		/*
		for (int x=0; x<b.width; x++) {
			// bottom
			pixels[((b.y)) * this.getSize() + (b.x + x)] = pixels[((b.y + 1)) * this.getSize() + (b.x + x)];
			// top
			pixels[((b.y + b.height - 1)) * this.getSize() + (b.x + x)] = pixels[((b.y + b.height - 2)) * this.getSize() + (b.x + x)];
		}
		
		for (int y=0; y<b.height; y++) {
			// left
			pixels[((b.y + y)) * this.getSize() + (b.x)] = pixels[((b.y + y)) * this.getSize() + (b.x + 1)];
			// right
			pixels[((b.y + y)) * this.getSize() + (b.x + b.width - 1)] = pixels[((b.y + y)) * this.getSize() + (b.x + b.width - 2)];
		}
		
*/		
		
		for (int x=0; x<b.width; x++) {
			// bottom
			pixels[((b.y)) * this.getSize() + (b.x + x)] = 0xffff00ff;
			// top
			pixels[((b.y + b.height - 1)) * this.getSize() + (b.x + x)] = 0xffff00ff;
		}
		
		for (int y=0; y<b.height; y++) {
			// left
			pixels[((b.y + y)) * this.getSize() + (b.x)] = 0xffff00ff;
			// right
			pixels[((b.y + y)) * this.getSize() + (b.x + b.width - 1)] = 0xffff00ff;
		}
		
	}
	
	public void postProcess() {
		postProcessNode(root);
	}
	
	private void postProcessNode(Node node) {
		if (node.lChild != null) postProcessNode(node.lChild);
		if (node.rChild != null) postProcessNode(node.rChild);
		if (node.box !=  null) {
			if (node.occupied) {
				if (Game.markedFaces.contains(node.box.face)) {
					bleedBox(node.box);
				}
			}
		}
	}

	public void writeToFile(File file) {
		BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		
		int[] opixels = new int[width * height];
		
		// ABGR -> ARGB
		for (int i=0; i<pixels.length; i++) {
			opixels[i] = (pixels[i] & 0xff000000) | (pixels[i] & 0x00ff0000) >> 16 | (pixels[i] & 0x0000ff00) | (pixels[i] & 0x000000ff) << 16; 
		}
		
		bi.setRGB(0, 0, width, height, opixels, 0, width);
		/*
		try {
			ImageIO.write(bi, "PNG", file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}*/
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
}
