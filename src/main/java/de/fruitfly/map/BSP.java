package de.fruitfly.map;

import static org.lwjgl.opengl.GL11.*;

import java.util.LinkedList;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.Color;

import de.fruitfly.Game;
import de.fruitfly.map.Face.ClipResult;
import de.fruitfly.map.vis.Sector;

public class BSP {

	public class Stats {
		public int numNodes;
		public int numLeafes;
		public int numEmptyLeafs;
		public int maxDepth;
		@Override
		public String toString() {
			return "Stats [numNodes=" + numNodes + ", numLeafes=" + numLeafes
					+ ", numEmptyLeafs=" + numEmptyLeafs + ", maxDepth="
					+ maxDepth + "]";
		}
	}
	
	public class BSPTreeNode {
		public BSPTreeNode root;
		public Plane divider;
		public BSPTreeNode leftChild, rightChild;
		public BSPTreeNode parent;
		public List<Face> faces;
		private int depth;
		
		private Color color;
		public int leafId;
		
		// portals
		public boolean usedAsPortal = false;
		public Sector sector;
		
		// root only
		public Stats stats;
		public List<Face> fullFaceList;
		public List<BSPTreeNode> leafs;
		public int numLeafs;
		public List<BSPTreeNode> dividers;
		
		public BSPTreeNode(BSPTreeNode root, BSPTreeNode parent) {
			if (root == null) {
				this.root = this;
			}
			else {
				this.root = root;
			}
			this.parent = parent;
			this.faces = null;
			this.divider = null;
			this.leftChild = this.rightChild = null;
			this.color = new Color((float)Math.random(), (float)Math.random(), (float)Math.random(), 1.0f);
		}
		
		public void setFaces(List<Face> faces) {
			this.faces = faces;
		}
		
		public boolean isLeaf() {
			return this.divider == null;
		}

		public void walk(BSPTreeNode node, NodeOperation op) {
			// in-order walk
			if (node.leftChild != null) walk(node.leftChild, op);
			op.process(node);
			if (node.rightChild != null) walk(node.rightChild, op);
		}
		
		public void pushFace(BSPTreeNode node, Face face) {
			if (node.isLeaf()) {
				node.faces.add(face);
				node.root.fullFaceList.add(face);
			}
			else {
				Side side = node.divider.classifyFace(face);
				if (side == Side.Front || side == Side.On) {
					pushFace(node.leftChild, face);
				}
				else if (side == Side.Back) {
					pushFace(node.rightChild, face);
				}
				else {
					ClipResult clip = face.clip(node.divider);
					pushFace(node.leftChild, clip.front);
					pushFace(node.rightChild, clip.back);
				}
			}
		}
		
		public void collectFacesInRadius(Vector3f p, float radius, List<Face> result) {
			collectFacesInRadius_r(this, p, radius, result);
		}
		
		private void collectFacesInRadius_r(BSPTreeNode node, Vector3f p, float radius, List<Face> result) {
			if (node.isLeaf()) {
				for (Face f : node.faces) {
					if (f.intersectsSphere(p, radius)) {
						result.add(f);
					}
				}
			}
			else {
				float dist = Vector3f.dot(node.divider.getNormal(), p);
				if (node.leftChild != null && dist - node.divider.getDistance() > radius) {
					collectFacesInRadius_r(node.leftChild, p, radius, result);
				}
				else if (node.rightChild != null && dist - node.divider.getDistance() < - radius) {
					collectFacesInRadius_r(node.rightChild, p, radius, result);
				}
				else {
					if (node.leftChild != null) collectFacesInRadius_r(node.leftChild, p, radius, result);
					if (node.rightChild != null) collectFacesInRadius_r(node.rightChild, p, radius, result);
				}
			}
		}
		
		/*
		 * Back-to-front rendering; no z-buffer needed
		 */
		public void render(BSPTreeNode node, Vector3f position) {
			if (Game.lightmap != null) {
			//	Game.lightmap.bind();
			}
			glColor3f(1.0f, 1.0f, 1.0f);
			if (node.isLeaf()) {
				for (Face f : node.faces) {

					if (Game.shading == Game.FaceShading.BSPFace) glColor3f(f.markColor.r, f.markColor.g, f.markColor.b);
					else if (Game.shading == Game.FaceShading.ColorFace) glColor3f(f.color.r, f.color.g, f.color.b);
					//glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE); 

					if (Game.dynamicllyLitFaces.contains(f) && Game.shading != Game.FaceShading.BSPFace) {
						Game.lightmap.bind();
						
						glDepthMask(false);
			
						glBegin(GL_TRIANGLE_FAN);
							for (Vertex vtx : f.getVertices()) {
								Vector3f v = vtx.getPosition();
								if (Game.shading != Game.FaceShading.BSPFace) glTexCoord2f(vtx.getLightUV().x, vtx.getLightUV().y);
								glVertex3f(v.x, v.y, v.z);
							}
						glEnd();
				
						glEnable(GL_BLEND);
						glBlendFunc(GL_ONE, GL_ONE);
//						glEnable(GL_POLYGON_OFFSET_FILL); 
//						glPolygonOffset(0,1);
						Game.dynamicLightmap.bind();
						glDepthMask(true);
						glBegin(GL_TRIANGLE_FAN);
							for (Vertex vtx : f.getVertices()) {
								Vector3f v = vtx.getPosition();
								glTexCoord2f(vtx.getDyLightUV().x, vtx.getDyLightUV().y);
								glVertex3f(v.x, v.y, v.z);
							}
						glEnd();
//						glDisable(GL_POLYGON_OFFSET_FILL); 
						glDisable(GL_BLEND);
					}
					else {
					
						if (Game.shading != Game.FaceShading.BSPFace) Game.lightmap.bind();
							glBegin(GL_TRIANGLE_FAN);
							for (Vertex vtx : f.getVertices()) {
								Vector3f v = vtx.getPosition();
								if (Game.shading != Game.FaceShading.BSPFace) glTexCoord2f(vtx.getLightUV().x, vtx.getLightUV().y);
								glVertex3f(v.x, v.y, v.z);
							}
						glEnd();
					
					}
					

					/*
					if (Game.shading == Game.FaceShading.BSPFace) {
						glColor3f(0.5f, 0.5f, 0.5f);
						glBegin(GL_LINE_LOOP);
						for (Vertex vtx : f.getVertices()) {
							Vector3f v = vtx.getPosition();
							glVertex3f(v.x, v.y, v.z);
						}
						glEnd();
					}
					*/
				}
			}
			else {
				if (node.divider.classifyPoint(position) == Side.Front) {
					render(node.rightChild, position);	
					render(node.leftChild, position);
				}
				else {
					render(node.leftChild, position);
					render(node.rightChild, position);	
				}
			}			
		}
		
		public BSPTreeNode findLeaf(Vector3f pos) {
			BSPTreeNode node = this;
			while (true) {
				if (node.isLeaf()) return node;
				
				Side side = node.divider.classifyPoint(pos);
				if (side == Side.Back) {
					node = node.rightChild;
				}
				else {
					node = node.leftChild;
				}
			}
		}
		
		public class PickResult {
			private Face face;
			private float distance;
			
			public PickResult(Face face, float distance) {
				this.face = face;
				this.distance = distance;
			}
			
			public Face getFace() {
				return face;
			}
		}
		
		public PickResult pick(Ray ray, BSPTreeNode node) {
			Vector3f p0 = new Vector3f(ray.getOrigin());
			Vector3f p1 = new Vector3f(ray.getDirection());
			if (ray.positiveInfinity) {
				p1.scale(10000.0f);
			}
			Vector3f.add(p1, p0, p1);
			
			if (node.isLeaf()) {
				Face minFace = null;
				float minDist = Float.MAX_VALUE;
				for (Face f : node.faces) {
					Float dist = ray.intersects(f);
					if (dist != null && dist > 0.0f && dist < minDist) {
						minDist = dist;
						minFace = f;
					}
				}
				if (minFace != null) {
					return new PickResult(minFace, minDist);
				}
				else {
					return new PickResult(null, Float.MAX_VALUE);
				}
			}
			else {
				Side p0Side = node.divider.classifyPoint(p0);
				Side p1Side = node.divider.classifyPoint(p1);
				
				if ((p0Side == Side.Back && p1Side == Side.Front) || (p0Side == Side.Front && p1Side == Side.Back)) {
					ray.setInfinite();
					PickResult frontPick = pick(ray, node.leftChild);
					PickResult backPick = pick(ray, node.rightChild);
					
					if (frontPick.distance < backPick.distance) {
						return frontPick;
					}
					else {
						return backPick;
					}
				}
				else {
					if (p0Side == Side.Back && p1Side == Side.Back) {
						return pick(ray, node.rightChild);
					}
					else {
						return pick(ray, node.leftChild);
					}
				}
			}
		}

		@Override
		public String toString() {
			return "BSPTreeNode [divider=" + divider + "]";
		}
	}
	
	public BSPTreeNode generateTree(List<Face> faces) {
		BSPTreeNode root = new BSPTreeNode(null, null);
		root.root = root;
		root.depth = 0;
		
		_generateTree(root, faces);
		
		calculatedState(root);
		
		return root;
	}
	
	public void calculatedState(BSPTreeNode root) {
		
		// Side Data for faces
		root.walk(root, new NodeOperation() {
			public void process(BSPTreeNode node) {
				if (node.isLeaf()) {
					for (Face f : node.faces) {
						f.calcSideData();
					}
				}
			}
		});
		
		final List<Face> fullList = new LinkedList<Face>();

		final List<BSPTreeNode> leafs = new LinkedList<BSPTreeNode>();
		final List<BSPTreeNode> splitter = new LinkedList<BSPTreeNode>();

		root.walk(root, new NodeOperation() {
			public void process(BSPTreeNode node) {
				if (node.isLeaf()) {
					leafs.add(node);
					for (Face f : node.faces) {
						f.markColor = node.color;
					}
					fullList.addAll(node.faces);
				}
				else {
					splitter.add(node);
				}
			}
		});
		
		root.fullFaceList = fullList;
		root.leafs = leafs;
		root.dividers = splitter;
		
		int i=0;
		for (BSPTreeNode leaf : leafs) {
			leaf.leafId = i++;
		}
		root.numLeafs = i;
		
		// Stats
		final Stats stats = new Stats();
		root.walk(root, new NodeOperation() {
			public void process(BSPTreeNode node) {
				if (node.isLeaf()) {
					stats.numLeafes++;
					if (node.faces.size() == 0) {
						stats.numEmptyLeafs++;
					}
				}
				stats.numNodes++;
				if (node.depth > stats.maxDepth) {
					stats.maxDepth = node.depth;
				}
			}
		});
		root.stats = stats;
	}

	private Face chooseRandomDivider(List<Face> faces) {
		Face dividingFace = null;
		while (true) {
			int i = (int) Math.floor(Math.random() * faces.size());
			dividingFace = faces.get(i);
			if (!dividingFace.usedAsDivider) {
				break;
			}
		}
		return dividingFace;
	}
	
	private Face chooseGoodDivider(List<Face> faces) {
		if (Face.isConvexSet(faces)) {
			return null;
		}
		float minRelation = 0.5f;
		Face bestFace = null;
		int leastSplits = Integer.MAX_VALUE;
		float bestRelation = 0.0f;
		
		while (bestFace == null) {
			for (Face face : faces) {
				if (!face.usedAsDivider) {
					int numPositive = 0;
					int numNegative = 0;
					int numSpanning = 0;
					
					for (Face face2 : faces) {
						if (face == face2) continue;
						Side side = face.classifyFace(face2);
						if (side == Side.Front) {
							numPositive++;
						}
						else if (side == Side.Back) {
							numNegative++;
						}
						else if (side == Side.Spanning) {
							numSpanning++;
						}
						
						float relation;
						if (numPositive < numNegative) {
							relation = numPositive / (float)numNegative;
						}
						else {
							relation = numNegative / (float)numPositive;
						}
						
						if (relation >= minRelation &&
								(numSpanning < leastSplits ||
										(numSpanning == leastSplits &&
										relation > bestRelation))) {
							bestFace = face;
							leastSplits = numSpanning;
							bestRelation = relation;
						}
					}
				}
			}
			final float MIN_RELATION_SCALE = 2.0f;
			minRelation /= MIN_RELATION_SCALE;
		}
		return bestFace;
	}
	
	public void _generateTree(BSPTreeNode node, List<Face> faces) {
		if (Face.isConvexSet(faces)) {
			node.setFaces(faces);
			return;
		}

		Face dividingFace = chooseGoodDivider(faces);
		
		Plane divider = dividingFace.getPlane();

		dividingFace.usedAsDivider = true;
		faces.remove(dividingFace);

		List<Face> frontFaces = new LinkedList<Face>();
		List<Face> backFaces = new LinkedList<Face>();

		frontFaces.add(dividingFace);
		
		for (Face f : faces) {
			Side side = divider.classifyFace(f);
			if (side == Side.Front) {
				frontFaces.add(f);
			}
			else if (side == Side.Back) {
				backFaces.add(f);
			}
			else if (side == Side.On) {
				if (Vector3f.dot(f.getPlane().getNormal(), divider.getNormal()) > 0.1f) { // \approx 1
					f.usedAsDivider = true;
					frontFaces.add(f);
				}
				else { // \approx -1
					f.usedAsDivider = true; // they are on the plane, so it makes no sense to divide here again
					backFaces.add(f);
				}
			}
			else if (side == Side.Spanning) {
				ClipResult clip = f.clip(divider);
				frontFaces.add(clip.front);
				backFaces.add(clip.back);
			}
		}
		
		node.divider = divider;
		node.leftChild = new BSPTreeNode(node.root, node);
		node.leftChild.depth = node.depth + 1;
		node.rightChild = new BSPTreeNode(node.root, node);
		node.rightChild.depth = node.depth + 1;
		_generateTree(node.leftChild, frontFaces);
		_generateTree(node.rightChild, backFaces);
	}
}
