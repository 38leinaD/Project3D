package de.fruitfly;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.GL_POINT_SPRITE;
import static org.lwjgl.opengl.GL30.*;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.DebugGraphics;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.Util;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.TrueTypeFont;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;

import de.fruitfly.ani.Animation;
import de.fruitfly.light.DiffuseLightEquation;
import de.fruitfly.light.DynamicLighting;
import de.fruitfly.light.FlickerLight;
import de.fruitfly.light.Light;
import de.fruitfly.light.LightAttachment;
import de.fruitfly.light.Lighting;
import de.fruitfly.light.Lightmap;
import de.fruitfly.light.Lumen;
import de.fruitfly.light.RadiosityLightEquation;
import de.fruitfly.light.Surface;
import de.fruitfly.map.BSP;
import de.fruitfly.map.BSP.BSPTreeNode;
import de.fruitfly.map.BSP.BSPTreeNode.PickResult;
import de.fruitfly.map.CSG;
import de.fruitfly.map.Face;
import de.fruitfly.map.MBrushEntity;
import de.fruitfly.map.MapFile;
import de.fruitfly.map.MapFile.BrushEntity;
import de.fruitfly.map.MapFile.FlameEntity;
import de.fruitfly.map.MapFile.LightEntity;
import de.fruitfly.map.MapFile.PlayerSpawnEntity;
import de.fruitfly.map.Merger;
import de.fruitfly.map.Plane;
import de.fruitfly.map.Ray;
import de.fruitfly.map.Side;
import de.fruitfly.map.Vertex;
import de.fruitfly.map.vis.Portal;
import de.fruitfly.map.vis.Portalizer;
import de.fruitfly.map.vis.Sector;

public class Game {

	public static int ticker = 0;
	
	public static int SCREEN_WIDTH = 800, SCREEN_HEIGHT = 600;
	public static int RT_SCALE = 2;
	public static int RT_WIDTH = SCREEN_WIDTH/RT_SCALE, RT_HEIGHT = SCREEN_HEIGHT/RT_SCALE;
	public static float wh_ratio = SCREEN_WIDTH/(float)SCREEN_HEIGHT;
	public static float fovy;
	public static float fovx;
	
	private static MBrushEntity worldEntity;
	private static List<Face> faces;
	
	public static Camera viewCam, freeCam, playerCam;
	public static BSPTreeNode bspTree;
	public static List<Face> markedFaces = new LinkedList<Face>();
	public static List<Face> _debugFaces = new LinkedList<Face>();
	public static List<Face> _debugFaces2 = new LinkedList<Face>();
	
	public static List<Vector3f> _debugPoints = new LinkedList<Vector3f>();
	public static List<Vector3f> _debugPoints2 = new LinkedList<Vector3f>();
	
	public static List<Face> _opBuffer = new LinkedList<Face>();
	
	public static List<Frustum2D> clipFrustums = new LinkedList<Frustum2D>();

	private static de.fruitfly.Entity player;
	public static Input input;
	//public static List<Portal> portals;
	
	public static List<Sector> sectors;
	public static Lightmap lightmap;
	public static Lightmap dynamicLightmap;

	public static boolean texFilterLinear = false;
	
	public static List<Light> lights;
	public static List<Light> dynamicLights = new LinkedList<Light>();
	
	public enum Binding {
		Player, Camera
	}
	
	private static Font font;
	//private static UnicodeFont uFont;
	public static Texture parTex;
	
	public static Binding controlBinding = Binding.Player;
	public static Binding viewBinding = Binding.Player;
	
	public static Lighting lighting = null;
	
	public enum FaceShading {
		Light, BSPFace, ColorFace
	}
	public static FaceShading shading = FaceShading.ColorFace;
	
	private static Animation anim;
	private static ParticleSystem ps;
	
	private static MD2Model mdl;
	private static MD2Model mdl2;
	private static OBJModel obj;
	private static Skybox sky;

	public static List<Surface> surfaces;
	
	private static DynamicLighting dynamicLighting;
	public static Set<Face> dynamicllyLitFaces = new HashSet<Face>();
	public static Set<Flame> flames = new HashSet<Flame>();
	
	public static List<Plasma> plasmas = new LinkedList<Plasma>();

	
	public static FlickerLight _debugLight = new FlickerLight(new Vector3f(), Color.red, 5.0f);
	
	private static void init() throws IOException {
		
		initFBO(RT_WIDTH, RT_HEIGHT);
		
		fovy = (float) (75.0f / 180.0f * Math.PI);
		fovx = (float) wh_ratio * fovy;
		
		input = new Input();
		input.init();
		
		R.init();

		player = new de.fruitfly.Entity();
		
		try {
			Texture tex = TextureLoader.getTexture("JPG", Game.class.getResourceAsStream("/skyboxsun5deg2_tn.png"), true);
			sky = new Skybox(tex, 1000);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		MapFile mf = new MapFile(Game.class.getResourceAsStream("/radtest3.map"));
		
		lights = new LinkedList<Light>();
		for (MapFile.Entity e : mf.getEntities()) {
			if (e instanceof MapFile.LightEntity) {
				LightEntity le = (LightEntity) e;
				lights.add(new Light(le.getPosition(), le.getColor(), le.getDistance()));
			}
			else if (e instanceof MapFile.PlayerSpawnEntity) {
				PlayerSpawnEntity pse = (PlayerSpawnEntity) e;
				player.setPosition(pse.getPosition());
			}
			else if (e instanceof MapFile.FlameEntity) {
				Flame f = new Flame(((MapFile.FlameEntity) e).getPosition());
				flames.add(f);
			}
		}
		
		worldEntity = new MBrushEntity((BrushEntity) mf.getEntities().get(0));
		
		System.out.println("CSG structural brushes");
		CSG csg = new CSG();
		csg.process(worldEntity.getStructuralBrushes());
		faces = csg.getFaces();
		
		Merger merger = new Merger();
		//merger.process(faces);
		
		BSP bsp = new BSP();
		bspTree = bsp.generateTree(faces);
		
		System.out.println("BSP stats: " + bspTree.stats);
		/*
		System.out.println("Portalize BSP");
		Portalizer portalizer = new Portalizer();
		portalizer.process(bspTree);
		System.out.println("Floodfill");
		portalizer.floodFillClean(player.getPosition(), bspTree);
		System.out.println("Merge portals");
		portalizer.mergePortals();
		
		bsp.calculatedState(bspTree);
		System.out.println("New BSP stats: " + bspTree.stats);

		//portals  = portalizer.getPortals();
		sectors = portalizer.getSectors();
		System.out.println("Number of sectors: " + sectors.size());
		
		int numPortals=0;
		for (Sector s : sectors) {
			numPortals+=s.getPortals().size();
		}
		System.out.println("Number of portals: " + numPortals);
*/
		System.out.println("CSG Detail brushes");
		csg.process(worldEntity.getDetailBrushes());
		List<Face> detailFaces = csg.getFaces();

		for (Face f : detailFaces) {
			bspTree.pushFace(bspTree, f);
		}

		bsp.calculatedState(bspTree);
		
		// lessons learned: integer grid next time!
		// Everythin back on the grid
		for (Face f : bspTree.fullFaceList) {
			for (Vertex v : f.getVertices()) {
				Vector3f vv = v.getPosition();
							
				// snap to 0.5f steps
				float xd = (float) (Math.abs(vv.x) - Math.floor(Math.abs(vv.x)) - 0.5f);
				if (xd >= -0.001f && xd <= 0.001f) {
					if (vv.x < 0.0f) vv.x = (float) (Math.ceil(vv.x) - 0.5f);
					else vv.x = (float) (Math.floor(vv.x) + 0.5f);
				}
				float yd = (float) (Math.abs(vv.y) - Math.floor(Math.abs(vv.y)) - 0.5f);
				if (yd >= -0.001f && yd <= 0.001f) {
					if (vv.y < 0.0f) vv.y = (float) (Math.ceil(vv.y) - 0.5f);
					else vv.y = (float) (Math.floor(vv.y) + 0.5f);
				}
				float zd = (float) (Math.abs(vv.z) - Math.floor(Math.abs(vv.z)) - 0.5f);
				if (zd >= -0.001f && zd <= 0.001f) {
					if (vv.z < 0.0f) vv.z = (float) (Math.ceil(vv.z) - 0.5f);
					else vv.z = (float) (Math.floor(vv.z) + 0.5f);
				}
				
				// snap to 1.0f steps
				if (Math.floor(Math.abs(vv.x)) - Math.abs(vv.x) > -0.001f) {
					vv.x = (float) Math.floor(vv.x);
				}
				else if (Math.ceil(Math.abs(vv.x)) - Math.abs(vv.x) < 0.001f) {
					vv.x = (float) Math.ceil(vv.x);
				}
				if (Math.floor(Math.abs(vv.y)) - Math.abs(vv.y) > -0.001f) {
					vv.y = (float) Math.floor(vv.y);
				}
				else if (Math.ceil(Math.abs(vv.y)) - Math.abs(vv.y) < 0.001f) {
					vv.y = (float) Math.ceil(vv.y);
				}
				if (Math.floor(Math.abs(vv.z)) - Math.abs(vv.z) > -0.001f) {
					vv.z = (float) Math.floor(vv.z);
				}
				else if (Math.ceil(Math.abs(vv.z)) - Math.abs(vv.z) < 0.001f) {
					vv.z = (float) Math.ceil(vv.z);
				}
			}
		}
		
		System.out.println("BSP stats: " + bspTree.stats);
		
		System.out.println("Generating Edge Links and Surfaces");
		//surfaces = Surface.generateSurfaces(bspTree.fullFaceList);
		
		freeCam = new Camera(fovx, fovy, Game.input);
		playerCam = new Camera(fovx, fovy, Game.input);
		
		lightmap = new Lightmap();
		lighting = new Lighting();
		font = new TrueTypeFont(new java.awt.Font("Verdana", java.awt.Font.PLAIN, 20), true);
		/*uFont = new UnicodeFont(new java.awt.Font("Verdana", java.awt.Font.PLAIN, 16));
		uFont.getEffects().add(new ColorEffect(java.awt.Color.ORANGE));
        try 
        {
        	uFont.loadGlyphs();
        } 
        
        catch (SlickException e) 
        {
            e.printStackTrace();// TODO Auto-generated catch block
        }*/
		anim = new Animation(Game.class.getResourceAsStream("/anim.txt"), playerCam);

		parTex = TextureLoader.getTexture("PNG", Game.class.getResourceAsStream("/particle.png"));
		parTex.bind();
		
		ps = new ParticleSystem(1000);
		
		//mdl = new MD2Model(new File("resources/tris.MD2"));
		//mdl2 = new MD2Model(new File("resources/hand.md2"));

		obj = new OBJModel(new File("./src/main/resources/AK_OBJ.obj"));
		

		dynamicLightmap = new Lightmap();
		dynamicLighting = new DynamicLighting();
		
		dynamicLights.add(_debugLight);
	}

	private static boolean buttonWasDown = false;
	
	private static boolean buttonMWasDown = false;
	
	private static int framebufferID;
	private static int colorTextureID;
	
    private static void initFBO(int surfaceWidth, int surfaceHeight) {
        framebufferID = glGenFramebuffers();                                                                                
        colorTextureID = glGenTextures();                                                                                               
        int depthRenderBufferID = glGenRenderbuffers();                                                                  

        System.out.println("Creating offscreen render target of dimensions " + surfaceWidth + "x" + surfaceHeight);
        
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferID);                                               

        // initialize color texture
        glBindTexture(GL_TEXTURE_2D, colorTextureID);                                                                  
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);                               
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, surfaceWidth, surfaceHeight, 0,GL_RGBA, GL_INT, (java.nio.ByteBuffer) null); 
        //glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        //glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D, colorTextureID, 0);

        // initialize depth renderbuffer
        glBindRenderbuffer(GL_RENDERBUFFER, depthRenderBufferID);                               
        glRenderbufferStorage(GL_RENDERBUFFER, GL14.GL_DEPTH_COMPONENT24, surfaceWidth, surfaceHeight);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_RENDERBUFFER, depthRenderBufferID); 

        glBindFramebuffer(GL_FRAMEBUFFER, 0);     
        glBindTexture(GL_TEXTURE_2D, 0); 
    }

    public static int lightIndex = 0;
	
	private static void tick() {
		if (ticker == 0) {
			lighting.bake(lightmap, bspTree.fullFaceList, lights, new DiffuseLightEquation());
		}
		
		ticker++;
		input.fetchEvents();
		
		lighting.tick();
		
		player.tick();
		playerCam.attachTo(player);
		
		_debugLight.getPosition().set(player.getPosition());
		_debugLight.getPosition().z += player.getEyeHeight();

		if (freeCam == viewCam) {
			//anim.tick();
		}
		freeCam.tick();
		viewCam = viewBinding == Binding.Player ? playerCam : freeCam;
		
		ps.tick();
		//mdl.tick();
		
		for (int i=0; i<plasmas.size(); i++) {
			plasmas.get(i).tick();
		}
		
		for (Flame f : flames) {
			f.tick();
		}
		
		for (Light l : dynamicLights) {
			if (l instanceof FlickerLight) {
				((FlickerLight) l).tick();
			}
		}
		
		if (!buttonWasDown && Mouse.isButtonDown(0)) {
			buttonWasDown = true;
			
			if (l1 == null || l2 == null) return;
			
			Ray r = Ray.fromPoints(l1, l2);
			r.setInfinite();
			PickResult pr = bspTree.pick(r, bspTree);
			System.out.println(pr);
			Face minFace = pr != null ? pr.getFace() : null;

			if (minFace != null) {
				if (!markedFaces.contains(minFace)) {
					markedFaces.add(minFace);
				}
				else {
					markedFaces.remove(minFace);
				}
			}
		}
		else if (!Mouse.isButtonDown(0)) {
			buttonWasDown = false;
		}

		// only bake dynamic lightmaps when static is not baking. using the same lumen...
		if (!lighting.isBaking() && dynamicLights.size() > 0) {
			dynamicLighting.bake(dynamicLightmap, bspTree, dynamicLights, new DiffuseLightEquation(), dynamicllyLitFaces);
		}

		if (Game.input.keyJustPressed(Input.ToggleControl)) {
			if (controlBinding == Binding.Player) {
				controlBinding = Binding.Camera;
				playerCam.setDirectControl(true);
				freeCam.setDirectControl(true);
			}
			else {
				controlBinding = Binding.Player;
				playerCam.setDirectControl(false);
				freeCam.setDirectControl(false);
			}
			System.out.println("Toggled control to " + controlBinding);
		}
		
		if (Game.input.keyJustPressed(Input.ToggleView)) {
			if (viewBinding == Binding.Player) {
				viewBinding = Binding.Camera;
			}
			else {
				viewBinding = Binding.Player;
			}
			System.out.println("Toggled view to " + viewBinding);
		}
		
		if (Game.input.keyJustPressed(Input.ToggleFaceColor)) {
			if (shading == FaceShading.Light) shading = FaceShading.BSPFace;
			else if (shading == FaceShading.BSPFace) shading = FaceShading.ColorFace;
			else if (shading == FaceShading.ColorFace) shading = FaceShading.Light;
			System.out.println("Toggled shading to " + shading);
		}
		
		if (Game.input.keyJustPressed(Input.ToggleTextureFilter)) {
			if (texFilterLinear) {
				lightmap.setFilter(GL_NEAREST);
				dynamicLightmap.setFilter(GL_NEAREST);
			}
			else {
				lightmap.setFilter(GL_LINEAR);
				dynamicLightmap.setFilter(GL_LINEAR);
			}
			texFilterLinear = !texFilterLinear;
		}
		
		if (Game.input.keyJustPressed(Input.ToggleLight)) {
			//lightmap = new Lightmap();
			/*
			for (Face f : markedFaces) {
				for (int i=0; i<4; i++) {
					if (i==0) f.getVertices().get(i).getLightUV().set(1.0f, 1.0f);
					if (i==1) f.getVertices().get(i).getLightUV().set(0.0f, 1.0f);
					if (i==2) f.getVertices().get(i).getLightUV().set(0.0f, 0.0f);
					if (i==3) f.getVertices().get(i).getLightUV().set(1.0f, 0.0f);
				}
			}*/

			lighting.bake(lightmap, bspTree.fullFaceList, lights, new DiffuseLightEquation());
			
			//System.out.println("Toggled light");
		}
			
		if (Game.input.keyJustPressed(Input.SpawnLight)) {
			Vector3f p = new Vector3f(player.getPosition());
			p.z += 1.0;
			Light light = new FlickerLight(p, new Color((float)Math.random(), (float)Math.random(), (float)Math.random(), 1.0f), 5.0f);
			dynamicLights.add(light);
			System.out.println("Spawned light " + light);
		}
		
		if (Game.input.keyJustPressed(Input.Dec)) {
			lightIndex = (lights.size() - 1) % lights.size();
			Light l = lights.get(lightIndex);
			_debugPoints2.clear();
			_debugPoints2.add(l.getPosition());
			System.out.println(lightIndex);

		}
		
		if (Game.input.keyJustPressed(Input.Inc)) {
			lightIndex = (lightIndex + 1) % lights.size();
			Light l = lights.get(lightIndex);
			_debugPoints2.clear();
			_debugPoints2.add(l.getPosition());
			System.out.println(lightIndex);
		}
		
		if (Game.input.keyJustPressed(Input.Do)) {
			System.out.println("Do it");
			List<Portal> ff = sectors.get(0).getPortals();
			Face f = ff.get(cc).face;
			_opBuffer.add(f);
		}
		
		if (Game.input.keyJustPressed(Input.Clr)) {
			System.out.println("Clr");
			_opBuffer.clear();
		}
		
		if (Game.input.keyJustPressed(Input.Action)) {
			
			Plasma p = new Plasma(playerCam.getPosition(), playerCam.getDirection(), Color.red);
			plasmas.add(p);
			
		}
		/*
		if (ticker % 5 == 0) {
			List<Particle> particles = ps.getParticles(1);
			
			for (Particle p : particles) {
				p.p.set(player.getPosition());

				p.v.set(playerCam.getDirection());
				p.c.r = 1.0f;
				p.c.g = 0.0f;
				p.c.b = 0.0f;
				p.lifetime = 200;
			}
		}
		*/
	}
	
	static int cc=0;
	
	private static FloatBuffer modelViewFB = BufferUtils.createFloatBuffer(16);
	private static FloatBuffer projectionFB = BufferUtils.createFloatBuffer(16);

	public static Matrix4f modelViewMatrix = new Matrix4f();
	public static Matrix4f projectionMatrix = new Matrix4f();
	public static Matrix4f modelViewProjectionMatrix = new Matrix4f();

	private static IntBuffer viewport = BufferUtils.createIntBuffer(16);

	private static Vector3f l1, l2;
	
	private static void render() {
		
        Util.checkGLError();
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferID);
        Util.checkGLError();
		
		//glEnable(GL_DEPTH_TEST);
		//glEnable(GL_TEXTURE_2D);
		
		
		
		glEnable(GL_DEPTH_TEST);
		glDisable(GL_COLOR_MATERIAL);

		glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glEnable(GL_POINT_SPRITE);
		//glEnable(GL_CULL_FACE);

		glPointSize(4);
		glViewport(0, 0, RT_WIDTH, RT_HEIGHT);
		
		viewport.rewind();
		glGetInteger(GL_VIEWPORT, viewport);
		
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		GLU.gluPerspective((float)(fovy / Math.PI * 180.0f), wh_ratio, 0.01f, 1000.0f);
		
		projectionFB.rewind();
		glGetFloat(GL_PROJECTION_MATRIX, projectionFB);
		projectionMatrix.load(projectionFB);
		
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();

		glRotatef(-90.0f, 1.0f, 0.0f, 0.0f);
		glRotatef(90.0f, 0.0f, 0.0f, 1.0f);
		
		glRotatef((float)(viewCam.getPitch()/Math.PI * 180.0f), 0.0f, 1.0f, 0.0f);
		glRotatef(-(float)(viewCam.getYaw()/Math.PI * 180.0f), 0.0f, 0.0f, 1.0f);
		glTranslatef(-viewCam.getPosition().x, -viewCam.getPosition().y, -viewCam.getPosition().z);
		
		glLineWidth(2.0f);
		
		//glTranslatef(0.0f, 0.0f, -10.0f);

		modelViewFB.rewind();
		glGetFloat(GL_MODELVIEW_MATRIX, modelViewFB);
		modelViewMatrix.load(modelViewFB);

		Matrix4f.mul(projectionMatrix, modelViewMatrix, modelViewProjectionMatrix);
		
		FloatBuffer pos = BufferUtils.createFloatBuffer(3);
		pos.rewind();
		modelViewFB.rewind();
		projectionFB.rewind();
		viewport.rewind();
		if (GLU.gluUnProject(RT_WIDTH/2, RT_HEIGHT/2, 0.1f, modelViewFB, projectionFB, viewport, pos)) {
			pos.rewind();
			l1 = new Vector3f(pos.get(), pos.get(), pos.get());
			
			
			float dx = (float) (100.0f * Math.cos(viewCam.getYaw()) * Math.cos(viewCam.getPitch()));
			float dy = (float) (100.0f * Math.sin(viewCam.getYaw()) * Math.cos(viewCam.getPitch()));
			float dz = (float) (100.0f * Math.sin(viewCam.getPitch()));
			l2 = new Vector3f(dx, dy, dz);
			Vector3f.add(l2, l1, l2);
		}
		else {
			l1 = l2 = null;
		}
		/*
		//for (Brush b : world.getBrushes()) {
			for (Face f : faces) {
				glColor3f(f.color.r, f.color.g, f.color.b);
				glBegin(GL_TRIANGLE_FAN);
					for (Vector3f v : f.getVertices()) {
						glVertex3f(v.x, v.y, v.z);
					}
				glEnd();
			}
			
			glDisable(GL_DEPTH_TEST);
			
			for (Face f : markedFaces) {
				glColor3f(1.0f, 0.0f, 0.0f);
				glBegin(GL_LINE_LOOP);
					for (Vector3f v : f.getVertices()) {
						glVertex3f(v.x, v.y, v.z);
					}
				glEnd();
			}
		//}
		*/
		
		/*
		if (markedFaces.size() > 0) {
			bspTree.marker = markedFaces.get(0);
		}
		*/
		
		glEnable(GL_TEXTURE_2D);	
		
		sky.render();
		
		if (lightmap != null && Game.shading != Game.FaceShading.BSPFace) {
			glEnable(GL_TEXTURE_2D);
			lightmap.bind();
		}
		else {
			glDisable(GL_TEXTURE_2D);	
		}
		//renderMap(playerCam.getFrustum(), playerCam.getPosition());
		bspTree.render(bspTree, viewCam.getPosition());
		
		glDisable(GL_TEXTURE_2D);	

		/*
		for (Surface s : surfaces) {
			s._debugRender();
		}
*/		
		for (Flame f : flames) {
			f.render();
		}

		for (int i=0; i<plasmas.size(); i++) {
			plasmas.get(i).render(); 
		}
		
		glDisable(GL_BLEND);
		glColor3f(1.0f, 1.0f, 1.0f);
		//mdl.render();
		//mdl2.render();
		ps.render();		

		
		glDisable(GL_TEXTURE_2D);
		glEnable(GL_DEPTH_TEST);

		/*
		glEnable(GL_BLEND);
		glBlendFunc(GL_DST_COLOR, GL_SRC_ALPHA);
		
		int i=0;
		for (Portal p : portals) {
			if (i++>4) break;
			Face f = p.face;
			glColor4f(f.color.r, f.color.g, f.color.b, 0.6f);
			glBegin(GL_TRIANGLE_FAN);
				for (Vertex vtx : f.getVertices()) {
					Vector3f v = vtx.getPosition();
					glVertex3f(v.x, v.y, v.z);
				}
			glEnd();
			
		}
		glDisable(GL_BLEND);
*/
		
		/*
		glEnable(GL_BLEND);
		glBlendFunc(GL_DST_COLOR, GL_SRC_ALPHA);
		
		BSPTreeNode node = bspTree.findLeaf(player.getPosition());
		//for (int j=0; j<sectors.size(); j++) {
			Sector s = node.sector;
			//if (j!=18) continue;
			for (int i=0; i<s.getPortals().size(); i++) {
				Face f = s.getPortals().get(i).face;
				glColor4f(1.0f, 0.0f, 0.0f, 0.4f);
				glBegin(GL_TRIANGLE_FAN);
					for (Vertex vtx : f.getVertices()) {
						Vector3f v = vtx.getPosition();
						glVertex3f(v.x, v.y, v.z);
					}
				glEnd();
				
			}
		//}
		
		glDisable(GL_BLEND);
		glDisable(GL_DEPTH_TEST);
*/
		
		//anim.render();
		
		glDisable(GL_DEPTH_TEST);

		
		int i=0;
		for (Face f : _debugFaces2) {

			glColor3f(0.0f, 0.0f, 1.0f);
			
			glBegin(GL_LINE_LOOP);
				for (Vertex vtx : f.getVertices()) {
					Vector3f v = vtx.getPosition();
					glVertex3f(v.x, v.y, v.z);
				}
			glEnd();
			
			for (Vertex vtx : f.getVertices()) {
				Vector3f v = vtx.getPosition();
				glPointSize(2.0f);
				glBegin(GL_POINTS);
				glVertex3f(v.x, v.y, v.z);
				glEnd();
			}
		}
		
		
		for (Face f : markedFaces) {
			glColor3f(1.0f, 0.0f, 0.0f);
			glBegin(GL_LINE_LOOP);
				for (Vertex vtx : f.getVertices()) {
					Vector3f v = vtx.getPosition();
					glVertex3f(v.x, v.y, v.z);
				}
			glEnd();
			
			Vector3f v = f.getVertices().get(0).getPosition();
			glPointSize(2.0f);
			glBegin(GL_POINTS);
			glVertex3f(v.x, v.y, v.z);
			glEnd();
		
		
			// axis
			if (f.axisA != null && f.axisB != null && f.axisOrigin != null) {
				Vector3f v1 = new Vector3f(f.axisA);
				Vector3f v2 = new Vector3f(f.axisB);
				v1.scale(0.1f);
				v2.scale(0.1f);
				renderVectorAtPoint(v1, f.axisOrigin);
				renderVectorAtPoint(v2, f.axisOrigin);
				Vector3f vv = new Vector3f();
				Vector3f.cross(f.axisA, f.axisB, vv);
				vv.scale(0.1f);
				renderVectorAtPoint(vv, f.axisOrigin);
			}
		}
		
		glPointSize(2.0f);
		for (Light light : dynamicLights) {
			glColor3f(light.getColor().r, light.getColor().g, light.getColor().b);
			glBegin(GL_POINTS);
			glVertex3f(light.getPosition().x, light.getPosition().y, light.getPosition().z);
			glEnd();
		}
		
		
		glBegin(GL_POINTS);
		glColor3f(0.0f, 1.0f, 1.0f);
		for (Vector3f v : _debugPoints) {
			glVertex3f(v.x, v.y, v.z);
		}
		
		glColor3f(1.0f, 0.0f, 1.0f);
		for (Vector3f v : _debugPoints2) {
			glVertex3f(v.x, v.y, v.z);
		}
		glEnd();
		
		for (Face f : markedFaces) {
			if (f.getLightAttachment() == null) continue;
			LightAttachment la = f.getLightAttachment();
			for (Lumen lumen : la.getLumen()) {
				glColor3f(0.0f, 1.0f, 0.0f);
				glBegin(GL_POINTS);
				glVertex3f(lumen.getWorldCoord().x, lumen.getWorldCoord().y, lumen.getWorldCoord().z);
				glEnd();
			}
			
			glColor3f(1.0f, 1.0f, 1.0f);
			glBegin(GL_POINTS);
			glVertex3f(f.center.x, f.center.y, f.center.z);
			glEnd();
		}
		
		glDisable(GL_DEPTH_TEST);
		
		// debug
		for (Face f : _debugFaces) {
			glColor3f(0.0f, 1.0f, 0.0f);
			glBegin(GL_LINE_LOOP);
				for (Vertex vtx : f.getVertices()) {
					Vector3f v = vtx.getPosition();
					glVertex3f(v.x, v.y, v.z);
				}
			glEnd();
		}
		
		if (l1 != null && l2 != null) {
			glBegin(GL_LINES);
				glColor3f(0.0f, 0.0f, 1.0f);
				glVertex3f(l1.x, l1.y, l1.z);
				glColor3f(0.0f, 1.0f, 0.0f);
				glVertex3f(l2.x, l2.y, l2.z);
			glEnd();
		}
			
		glBegin(GL_LINES);
			glColor3f(1.0f, 0.0f, 0.0f);
			glVertex3f(0.0f, 0.0f, 0.0f);
			glVertex3f(0.5f, 0.0f, 0.0f);
			
			glColor3f(0.0f, 1.0f, 0.0f);
			glVertex3f(0.0f, 0.0f, 0.0f);
			glVertex3f(0.0f, 0.5f, 0.0f);
			
			glColor3f(0.0f, 0.0f, 1.0f);
			glVertex3f(0.0f, 0.0f, 0.0f);
			glVertex3f(0.0f, 0.0f, 0.5f);
		glEnd();
		
		/*
		BSPTreeNode node = bspTree.leafs.get(9);
		BSPTreeNode parent = node.parent;
		BSPTreeNode otherChild = node == parent.leftChild ? parent.rightChild : parent.leftChild;
		for (Face f : node.faces) {
			
			if (Game.faceColorToggle) {
				glColor3f(1.0f, 1.0f, 1.0f);
				glBegin(GL_LINE_LOOP);
				for (Vertex vtx : f.getVertices()) {
					Vector3f v = vtx.getPosition();
					glVertex3f(v.x, v.y, v.z);
				}
				glEnd();
			}
		}
		*/
		
		if (viewBinding == Binding.Camera) player.renderCollider();
		
		playerCam.getFrustum().render(playerCam);


		
		
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();

		
		glTranslatef(2.8f, -2.0f, -5.0f);
		glRotatef(180.0f, 0.0f, 1.0f, 0.0f);
		glScalef(0.05f, 0.05f, 0.05f);

		glColor3f(1.0f, 1.0f, 1.0f);
		//obj.render();
		
		glEnable(GL_DEPTH_TEST);
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		float scale = 500.0f;
		glOrtho(-scale, scale, scale / wh_ratio, -scale / wh_ratio, -10.0f, 10.0f);
				
		
		glLineWidth(1.0f);
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		
		glColor3f(1.0f, 1.0f, 1.0f);
		
		glBegin(GL_LINES);
			glVertex2f(-15.0f, 0.f);
			glVertex2f(10.0f, 0.0f);
			
			glVertex2f(0.0f, -10.0f);
			glVertex2f(0.0f, 15.0f);
		glEnd();
		
		for (Vector3f v : _debugPoints) {
			Vector4f p = Matrix4f.transform(modelViewProjectionMatrix, new Vector4f(v.x, v.y, v.z, 1.0f), null);
			p.scale(1.0f/p.w);
			font.drawString(p.x * scale, -p.y * scale, "(" + v.x + ", " + v.y + ", " + v.z + ")");
		}
		//glEnable(GL_BLEND);
		//glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		

		glDisable(GL_TEXTURE_2D);
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		
		glColor3f(0.0f, 1.0f, 1.0f);

		for (Frustum2D f : clipFrustums) {
			f.render();
		}
		
		// if debug should be rendered to high-res, also the debpth buffer would need to be copied over...
		renderFBO();
	}
	
	private static void renderFBO() {
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
        
        glPushAttrib(GL_ALL_ATTRIB_BITS);

        glMatrixMode(GL_PROJECTION);        
        glLoadIdentity();
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glDisable(GL_DEPTH_TEST);
    
        glBindTexture(GL_TEXTURE_2D, colorTextureID);   
        
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

        glViewport(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
		glClear(GL_COLOR_BUFFER_BIT);
        glEnable(GL_TEXTURE_2D);
        glColor3f(1.0f, 1.0f, 1.0f);
        glBegin(GL_QUADS);
        	glTexCoord2f(-1.0f, 0.0f);
        	glVertex2f(-1.0f, -1.0f);
        	glTexCoord2f(1.0f, 0.0f);
        	glVertex2f(1.0f, -1.0f);
        	glTexCoord2f(1.0f, 1.0f);
        	glVertex2f(1.0f, 1.0f);
        	glTexCoord2f(0.0f, 1.0f);
        	glVertex2f(-2.0f, 1.0f);
        glEnd();
        
        glPopAttrib();
	}

	private static void renderMap(Frustum frustum, Vector3f location) {
		if (Game.lightmap != null) {
			Game.lightmap.bind();
		}
		
		BSPTreeNode node = bspTree.findLeaf(location);
		/*if (node.faces.size() == 0) {
			// outside of real world
			bspTree.render(bspTree, location);
		}
		else {*/
			startRender();
			//glEnable(GL_SCISSOR_TEST);

			Frustum2D initialFrustum = new Frustum2D(-1.0f, -1.0f, 1.0f, 1.0f);
			initialFrustum.zRef = -1.0f;
			List<Frustum2D> frustumList = new LinkedList<Frustum2D>();
			frustumList.add(initialFrustum);
			renderSector(node.sector, frustumList);

			//glDisable(GL_SCISSOR_TEST);
			//}
	}
	
	private static int frame = 0;
	private static void startRender() {
		frame++;
		clipFrustums.clear();
	}
	
	private static Plane frontPlane;
	
	// this is not 100% right; if we get into a sector via different portals, the visibility of connected sectors is done based on first
	private static void renderSector(Sector s, List<Frustum2D> frustums) {
		//if (s.frame == frame) return;
		
		clipFrustums.addAll(frustums);
		
		Frustum2D join = Frustum2D.join(frustums);
		//glScissor((int)((join.x + join.w/2.0f) * Game.SCREEN_WIDTH), (int)((join.y + join.h/2.0f) * Game.SCREEN_HEIGHT), (int)(join.w * Game.SCREEN_WIDTH), (int)(join.h * Game.SCREEN_HEIGHT));
		
		glColor3f(1.0f, 1.0f, 1.0f);
		for (Face f : s.bspLeaf.faces) {
			if (Game.shading == Game.FaceShading.BSPFace) glColor3f(f.markColor.r, f.markColor.g, f.markColor.b);
			else if (Game.shading == Game.FaceShading.ColorFace) glColor3f(f.color.r, f.color.g, f.color.b);

			glBegin(GL_TRIANGLE_FAN);
				for (Vertex vtx : f.getVertices()) {
					Vector3f v = vtx.getPosition();
					glTexCoord2f(vtx.getLightUV().x, vtx.getLightUV().y);
					glVertex3f(v.x, v.y, v.z);
				}
			glEnd();
			
			if (Game.shading == Game.FaceShading.BSPFace) {
				glColor3f(1.0f, 1.0f, 1.0f);
				glBegin(GL_LINE_LOOP);
				for (Vertex vtx : f.getVertices()) {
					Vector3f v = vtx.getPosition();
					glVertex3f(v.x, v.y, v.z);
				}
				glEnd();
			}
		}
		
		//s.frame = frame;
		Vector4f a = new Vector4f();
		//List<Frustum2D> newFrustums = new LinkedList<Frustum2D>();
		Map<Sector, List<Frustum2D>> secFrustums = new HashMap<Sector, List<Frustum2D>>();
		
next:	for (Portal p : s.getPortals()) {
			int otherIndex = 0;
			if (p.sectors[0] == s) otherIndex = 1;
			
			Sector conSec = p.sectors[otherIndex];
			
			float xmin = Float.POSITIVE_INFINITY;
			float ymin = Float.POSITIVE_INFINITY;
			float xmax = Float.NEGATIVE_INFINITY;
			float ymax = Float.NEGATIVE_INFINITY;
			float zRef = Float.POSITIVE_INFINITY;
			
			boolean inFront = false;
			for (int i=0; i<p.face.getVertices().size(); i++) {
				Vector3f v = p.face.getVertices().get(i).getPosition();
				a.set(v.x, v.y, v.z, 1.0f);
				
				Matrix4f.transform(modelViewProjectionMatrix, a, a);
				a.x /= a.w;
				a.y /= a.w;
				a.z /= a.w;
				a.w = 1.0f;
				
				
				if (playerCam.getFrustum().frontPlane.classifyPoint(v) == Side.Front) {
					if (a.z < zRef) zRef = a.z;
					if (a.x < xmin) xmin = a.x;
					if (a.x > xmax) xmax = a.x;
					if (a.y < ymin) ymin = a.y;
					if (a.y > ymax) ymax = a.y;
					inFront = true;
				}
				else {
					if (-a.z > zRef) zRef = -a.z;
					if (-a.x < xmin) xmin = -a.x;
					if (-a.x > xmax) xmax = -a.x;
					if (-a.y < ymin) ymin = -a.y;
					if (-a.y > ymax) ymax = -a.y;
				}
				
				/*if (playerCam.getFrustum().contains(p.face.getVertices().get(i).getPosition())) {
					renderSector(conSec, frustum);
					continue next;
				}*/
			}
			
			if (inFront) {
				Frustum2D newFrustum = new Frustum2D(xmin, ymin, xmax, ymax);
				newFrustum.zRef = zRef;
				for (Frustum2D f : frustums) {
					Frustum2D clipFrustum = newFrustum.clipAgainst(f);
					if (clipFrustum != null && clipFrustum.zRef > f.zRef) {
						
						if (secFrustums.get(conSec) == null) {
							secFrustums.put(conSec, new LinkedList<Frustum2D>());
						}
						secFrustums.get(conSec).add(clipFrustum);
					}
				}
			}
		}
		
		for (Sector sec : secFrustums.keySet()) {
			renderSector(sec, secFrustums.get(sec));
		}
	}
	
	private static void renderVectorAtPoint(Vector3f direction, Vector3f position) {
		glBegin(GL_LINES);
		glColor3f(0.0f, 0.0f, 1.0f);
		glVertex3f(position.x, position.y, position.z);
		glColor3f(1.0f, 0.0f, 0.0f);
		Vector3f head = new Vector3f(direction);
		head.scale(10.0f);
		Vector3f.add(head, position, head);
		glVertex3f(head.x, head.y, head.z);
		glEnd();
	}

	public static void main(String[] args) throws Exception {

		Display.setDisplayMode(new DisplayMode(SCREEN_WIDTH, SCREEN_HEIGHT));
		Display.setTitle("Project");
		Display.setLocation(2100, 410);
		Display.create();
		
		init();
		
		while (!Display.isCloseRequested()) {
			tick();
			//System.out.println("->" + Display.getX() + " , " + Display.getY());
			render();
			Display.update();
			Display.sync(60);
		}
		
		Display.destroy();
	}
}
