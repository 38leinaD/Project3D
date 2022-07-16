package de.fruitfly;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.GL_POINT_SPRITE;

import java.util.LinkedList;
import java.util.List;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.newdawn.slick.Color;

import de.fruitfly.light.Lighting;
import de.fruitfly.light.Lumen;
import de.fruitfly.light.PlanarMapping;
import de.fruitfly.map.Face;
import de.fruitfly.map.Plane;
import de.fruitfly.map.Vertex;

public class Sandbox {
	private static final int SCREEN_WIDTH = 800;
	private static final int SCREEN_HEIGHT = 600;
	private static final float WIDTH_TO_HEIGHT_RATIO = SCREEN_WIDTH/(float)SCREEN_HEIGHT;

	private static Camera cam;
	private static Input input;
	
	public static void main(String[] args) {
		try {
			Display.setDisplayMode(new DisplayMode(SCREEN_WIDTH, SCREEN_HEIGHT));
			//Display.setLocation(2000, 300);
			Display.setTitle("Sandbox");
			Display.create();
		} catch (LWJGLException e) {
			throw new RuntimeException(e);
		}
		
		init();
		
		while (!Display.isCloseRequested()) {
			tick();
			render();
			Display.update();
			Display.sync(60);
		}
		
		Display.destroy();
	}

	private static void render() {
		setupView();
		
		glLineWidth(1.0f);
		renderGrid();
		glLineWidth(2.0f);
		
		renderAxis(new Vector3f(0.0f, 0.0f, 0.0f), new Vector3f(1.0f, 0.0f, 0.0f), new Vector3f(0.0f, 1.0f, 0.0f), new Vector3f(0.0f, 0.0f, 1.0f));

		testRender();
		
	}
	
	private static void renderAxis(Vector3f origin, Vector3f x, Vector3f y, Vector3f z) {
			glDisable(GL_DEPTH_TEST);
			glBegin(GL_LINES);
				glColor3f(1.0f, 0.0f, 0.0f);
				glVertex3f(origin.x, origin.y, origin.z);
				glVertex3f(origin.x + x.x, origin.y + x.y, origin.z + x.z);
				
				glColor3f(0.0f, 1.0f, 0.0f);
				glVertex3f(origin.x, origin.y, origin.z);
				glVertex3f(origin.x + y.x, origin.y + y.y, origin.z + y.z);
				
				glColor3f(0.0f, 0.0f, 1.0f);
				glVertex3f(origin.x, origin.y, origin.z);
				glVertex3f(origin.x + z.x, origin.y + z.y, origin.z + z.z);
			glEnd();
			glEnable(GL_DEPTH_TEST);
		
	}

	private static void testRender() {
		Vector3f pNormal = new Vector3f(0.4f, 0.4f, 0.7f);
		pNormal.normalise();
		Plane plane = new Plane(pNormal, 1.0f);
		Face face = new Face(plane, 3, Color.red);
		renderFace(face);
		
		List<Vector3f> points = new LinkedList<Vector3f>();
		for (Vertex vtx : face.getVertices()) {
			points.add(vtx.getPosition());
		}
		
		PlanarMapping map = new PlanarMapping();

		Lighting.calculatePlanarMappingTransform(plane, points, map);
		
		Vector4f r = new Vector4f();
		List<Vector3f> trPoints = new LinkedList<Vector3f>();
		glColor3f(0.0f, 1.0f, 0.0f);
		glBegin(GL_TRIANGLE_FAN);
		for (Vector3f point : points) {
			Matrix4f.transform(map.worldToTex, MathUtil.getPoint4f(point), r);
			Vector3f rr = MathUtil.getPoint3f(r);
			glVertex3f(rr.x, rr.y, rr.z);
			trPoints.add(rr);
		}
		glEnd();
		
		renderAxis(map.axisOrigin, map.uAxis, map.vAxis, map.wAxis);
		
		glColor3f(0.0f, 0.0f, 1.0f);
		glBegin(GL_POINTS);
		for (Vector3f point : trPoints) {
			Matrix4f.transform(map.texToWorld, MathUtil.getPoint4f(point), r);
			Vector3f rr = MathUtil.getPoint3f(r);
			glVertex3f(rr.x, rr.y, rr.z);
		}
		glEnd();
		
		List<Lumen> lumen = new LinkedList<Lumen>();
		for (int x=0; x<5; x++) {
			for (int y=0; y<5; y++) {
				Vector3f world = new Vector3f();
				map.toWorld(x + 0.5f, y + 0.5f, world);
				lumen.add(new Lumen(face, x, y, world));
			}
		}
		
		renderLumen(lumen);
	}

	private static void renderLumen(List<Lumen> lumen) {
		glDisable(GL_DEPTH_TEST);

		glColor3f(0.0f, 1.0f, 1.0f);
		glBegin(GL_POINTS);
		for (Lumen l : lumen) {
			glVertex3f(l.getWorldCoord().x, l.getWorldCoord().y, l.getWorldCoord().z);
		}
		glEnd();
		glEnable(GL_DEPTH_TEST);

		
	}

	private static void renderFace(Face f) {
		glColor3f(f.color.r, f.color.g, f.color.b);
		glBegin(GL_TRIANGLE_FAN);
			for (Vertex vtx : f.getVertices()) {
				Vector3f v = vtx.getPosition();
				glVertex3f(v.x, v.y, v.z);
			}
		glEnd();
	}
	
	private static void renderGrid() {
		glColor3f(0.5f, 0.5f, 0.5f);

		glLineWidth(1.0f);

		glBegin(GL_LINES);
		for (int y=-10; y<=10; y++) {
			if (y==0) continue;
			glVertex3f(-10.0f, y, 0.0f);
			glVertex3f(10.0f, y, 0.0f);
		}
		
		for (int x=-10; x<=10; x++) {
			if (x==0) continue;
			glVertex3f(x, -10.0f, 0.0f);
			glVertex3f(x, 10.0f, 0.0f);
		}
		glEnd();
		
		glLineWidth(2.0f);
		glBegin(GL_LINES);
		glVertex3f(-10.0f, 0, 0.0f);
		glVertex3f(10.0f, 0, 0.0f);
		glVertex3f(0, -10.0f, 0.0f);
		glVertex3f(0, 10.0f, 0.0f);
		glEnd();

	}
	
	private static void setupView() {
		glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glEnable(GL_POINT_SPRITE);
		glEnable(GL_DEPTH_TEST);

		glPointSize(4);
		glViewport(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
		
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		GLU.gluPerspective((float)(cam.getFrustum().getFovy() / Math.PI * 180.0f), WIDTH_TO_HEIGHT_RATIO, 0.01f, 1000.0f);
		
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();

		glRotatef(-90.0f, 1.0f, 0.0f, 0.0f);
		glRotatef(90.0f, 0.0f, 0.0f, 1.0f);
		
		glRotatef((float)(cam.getPitch()/Math.PI * 180.0f), 0.0f, 1.0f, 0.0f);
		glRotatef(-(float)(cam.getYaw()/Math.PI * 180.0f), 0.0f, 0.0f, 1.0f);
		glTranslatef(-cam.getPosition().x, -cam.getPosition().y, -cam.getPosition().z);
		
		glLineWidth(2.0f);
		
	}

	private static void tick() {
		input.fetchEvents();
		cam.tick();
	}

	private static void init() {
		float fovy = (float) (75.0f / 180.0f * Math.PI);
		float fovx = (float) WIDTH_TO_HEIGHT_RATIO * fovy;
		
		input = new Input();
		input.init();
		
		cam = new Camera(fovx, fovy, input);
		cam.getPosition().set(-5.0f, 0.0f, 0.0f);
		cam.setDirectControl(true);
	}
}
