/*
	This file is part of FreeJ2ME.

	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	FreeJ2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/
*/
package javax.microedition.m3g;

import java.util.Vector;
import org.recompile.mobile.Mobile;

public abstract class Node extends Transformable
{
	public static final int NONE  = 144;
	public static final int ORIGIN  = 145;
	public static final int X_AXIS  = 146;
	public static final int Y_AXIS  = 147;
	public static final int Z_AXIS  = 148;

	public Node parent = null;
	public Node left;
	public Node right;

	private Node yRef = null;
	private Node zRef = null;
	private int yTarget = NONE;
	private int zTarget = NONE;
	private float alphaFactor = 1.0f;
	private boolean picking = true;
	private boolean rendering = true;
	private int scope = -1;

	boolean hasRenderables = false;
	boolean hasBones = false;
	boolean[] dirtyBits = new boolean[2]; // {renderablesBit, bonesBit}

	protected Object3D duplicateImpl()
	{
		Node copy = (Node) super.duplicateImpl();
		copy.parent = null;
		copy.left = null;
		copy.right = null;
		copy.yRef = null;
		copy.zRef = null;
		copy.yTarget = NONE;
		copy.zTarget = NONE;
		copy.dirtyBits = new boolean[]{ false, false };
		return copy;
	}

	boolean doAlign(Node ref) { return computeAlignment(ref == null ? this : ref); }

	public final void align(Node reference)
	{
		if (reference != null && this.getRootNode() != reference.getRootNode())
			{ throw new IllegalArgumentException("Alignment reference is not in the same scene graph"); }
		if (!doAlign(reference == null ? this : reference))
			{ throw new IllegalStateException("Unable to resolve node alignment"); }
	}

	/*
	 * Alignment is evaluated in coordinate system A.  A differs from this
	 * node's parent coordinate system only by this node's translation T; its
	 * rotation, scale and generic transform are deliberately ignored.  This is
	 * the coordinate system defined by JSR-184 for Node alignment.
	 */
	boolean computeAlignment(Node refNode)
	{
		if (zTarget == NONE && yTarget == NONE) { return true; }

		Node root = getRootNode();
		if (zRef != null && (isChildOf(this, zRef) || zRef.getRootNode() != root)) { return false; }
		if (yRef != null && (isChildOf(this, yRef) || yRef.getRootNode() != root)) { return false; }

		/* Validate dynamic references as well as the fixed references above. */
		Node zTargetNode = null;
		Node yTargetNode = null;
		if (zTarget != NONE)
		{
			zTargetNode = (zRef != null) ? zRef : refNode;
			if (zTargetNode == this || isChildOf(this, zTargetNode)) { return false; }
		}
		if (yTarget != NONE)
		{
			yTargetNode = (yRef != null) ? yRef : refNode;
			if (yTargetNode == this || isChildOf(this, yTargetNode)) { return false; }
		}

		/* A root has no parent coordinate system in which to align. */
		if (parent == null) { return true; }

		float[] translation = new float[3];
		getTranslation(translation);
		float[] zVector = null;
		float[] yVector = null;
		if (zTargetNode != null)
		{
			zVector = getAlignmentVector(zTargetNode, zTarget, translation);
			if (zVector == null) { return false; }
		}
		if (yTargetNode != null)
		{
			yVector = getAlignmentVector(yTargetNode, yTarget, translation);
			if (yVector == null) { return false; }
		}

		float[] orientation = new float[]{ 0.0f, 0.0f, 0.0f, 1.0f };
		if (zVector != null)
		{
			/* First rotate local +Z to the transformed Z alignment target. */
			if (normalizeAlignmentVector(zVector))
			{
				orientation = alignmentRotation(new float[]{ 0.0f, 0.0f, 1.0f }, zVector);
			}

			if (yVector != null)
			{
				/*
				 * Express the Y target in the frame after the Z rotation, discard
				 * its Z component, then rotate local +Y around the resulting +Z.
				 * Thus the second rotation cannot disturb the Z alignment.
				 */
				float[] yInZFrame = rotateVectorByInverse(orientation, yVector);
				yInZFrame[2] = 0.0f;
				if (normalizeAlignmentVector(yInZFrame))
				{
					float[] yRotation = alignmentRotationAroundZ(yInZFrame);
					orientation = multiplyQuaternions(orientation, yRotation);
				}
			}
		}
		else if (yVector != null && normalizeAlignmentVector(yVector))
		{
			/* With no Z target, align local +Y directly to its target. */
			orientation = alignmentRotation(new float[]{ 0.0f, 1.0f, 0.0f }, yVector);
		}

		setAlignmentOrientation(orientation);
		return true;
	}

	/* Return a target point or axis expressed in alignment coordinate system A. */
	private float[] getAlignmentVector(Node targetNode, int target, float[] thisTranslation)
	{
		Transform targetToParent = new Transform();
		if (!targetNode.getTransformTo(parent, targetToParent)) { return null; }

		float[] targetVector = new float[]{ 0.0f, 0.0f, 0.0f, 0.0f };
		switch (target)
		{
			case ORIGIN: targetVector[3] = 1.0f; break;
			case X_AXIS: targetVector[0] = 1.0f; break;
			case Y_AXIS: targetVector[1] = 1.0f; break;
			case Z_AXIS: targetVector[2] = 1.0f; break;
			default: return null;
		}

		targetToParent.transform(targetVector);
		if (target == ORIGIN)
		{
			/* A has the parent axes, but its origin is this node's T. */
			targetVector[0] -= thisTranslation[0];
			targetVector[1] -= thisTranslation[1];
			targetVector[2] -= thisTranslation[2];
		}

		return new float[]{ targetVector[0], targetVector[1], targetVector[2] };
	}

	private static boolean normalizeAlignmentVector(float[] vector)
	{
		float lengthSquared = vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2];
		if (lengthSquared <= 1.0e-12f) { return false; }

		float inverseLength = (float) (1.0 / Math.sqrt(lengthSquared));
		vector[0] *= inverseLength;
		vector[1] *= inverseLength;
		vector[2] *= inverseLength;
		return true;
	}

	/* Return the shortest unit quaternion that rotates source to target. */
	private static float[] alignmentRotation(float[] source, float[] target)
	{
		float dot = source[0] * target[0] + source[1] * target[1] + source[2] * target[2];
		if (dot > 1.0f) { dot = 1.0f; }
		else if (dot < -1.0f) { dot = -1.0f; }

		if (dot > 1.0f - 1.0e-6f)
			{ return new float[]{ 0.0f, 0.0f, 0.0f, 1.0f }; }

		if (dot < -1.0f + 1.0e-6f)
		{
			/* Pick a stable perpendicular axis for a 180 degree rotation. */
			float[] basis = (Math.abs(source[0]) < Math.abs(source[1]) && Math.abs(source[0]) < Math.abs(source[2]))
				? new float[]{ 1.0f, 0.0f, 0.0f }
				: ((Math.abs(source[1]) < Math.abs(source[2]))
					? new float[]{ 0.0f, 1.0f, 0.0f }
					: new float[]{ 0.0f, 0.0f, 1.0f });
			float[] axis = new float[]{
				source[1] * basis[2] - source[2] * basis[1],
				source[2] * basis[0] - source[0] * basis[2],
				source[0] * basis[1] - source[1] * basis[0]
			};
			normalizeAlignmentVector(axis);
			return new float[]{ axis[0], axis[1], axis[2], 0.0f };
		}

		float[] cross = new float[]{
			source[1] * target[2] - source[2] * target[1],
			source[2] * target[0] - source[0] * target[2],
			source[0] * target[1] - source[1] * target[0]
		};
		float scale = (float) (1.0 / Math.sqrt((1.0 + dot) * 2.0));
		return new float[]{ cross[0] * scale, cross[1] * scale, cross[2] * scale,
			(float) Math.sqrt((1.0 + dot) * 0.5) };
	}

	/*
	 * The constrained Y rotation must be about local +Z, including when the
	 * projected target is exactly -Y. A general shortest-arc rotation would
	 * choose an arbitrary perpendicular axis in that latter case and undo the
	 * Z alignment.
	 */
	private static float[] alignmentRotationAroundZ(float[] target)
	{
		float targetY = target[1];
		if (targetY > 1.0f) { targetY = 1.0f; }
		else if (targetY < -1.0f) { targetY = -1.0f; }

		if (targetY > 1.0f - 1.0e-6f)
			{ return new float[]{ 0.0f, 0.0f, 0.0f, 1.0f }; }
		if (targetY < -1.0f + 1.0e-6f)
			{ return new float[]{ 0.0f, 0.0f, 1.0f, 0.0f }; }

		float scale = (float) (1.0 / Math.sqrt((1.0 + targetY) * 2.0));
		/* (0,1,0) x (target.x,target.y,0) = (0,0,-target.x). */
		return new float[]{ 0.0f, 0.0f, -target[0] * scale,
			(float) Math.sqrt((1.0 + targetY) * 0.5) };
	}

	/* Rotate a vector by the inverse of a unit quaternion. */
	private static float[] rotateVectorByInverse(float[] rotation, float[] vector)
	{
		float qx = -rotation[0];
		float qy = -rotation[1];
		float qz = -rotation[2];
		float qw = rotation[3];
		float tx = 2.0f * (qy * vector[2] - qz * vector[1]);
		float ty = 2.0f * (qz * vector[0] - qx * vector[2]);
		float tz = 2.0f * (qx * vector[1] - qy * vector[0]);

		return new float[]{
			vector[0] + qw * tx + qy * tz - qz * ty,
			vector[1] + qw * ty + qz * tx - qx * tz,
			vector[2] + qw * tz + qx * ty - qy * tx
		};
	}

	/* For column vectors, first * second applies second, then first. */
	private static float[] multiplyQuaternions(float[] first, float[] second)
	{
		float[] result = new float[]{
			first[3] * second[0] + first[0] * second[3] + first[1] * second[2] - first[2] * second[1],
			first[3] * second[1] + first[1] * second[3] + first[2] * second[0] - first[0] * second[2],
			first[3] * second[2] + first[2] * second[3] + first[0] * second[1] - first[1] * second[0],
			first[3] * second[3] - first[0] * second[0] - first[1] * second[1] - first[2] * second[2]
		};
		normalizeQuaternion(result);
		return result;
	}

	private static void normalizeQuaternion(float[] quaternion)
	{
		float lengthSquared = quaternion[0] * quaternion[0] + quaternion[1] * quaternion[1]
			+ quaternion[2] * quaternion[2] + quaternion[3] * quaternion[3];
		if (lengthSquared <= 1.0e-12f)
		{
			quaternion[0] = 0.0f;
			quaternion[1] = 0.0f;
			quaternion[2] = 0.0f;
			quaternion[3] = 1.0f;
			return;
		}

		float inverseLength = (float) (1.0 / Math.sqrt(lengthSquared));
		quaternion[0] *= inverseLength;
		quaternion[1] *= inverseLength;
		quaternion[2] *= inverseLength;
		quaternion[3] *= inverseLength;
	}

	private void setAlignmentOrientation(float[] quaternion)
	{
		normalizeQuaternion(quaternion);
		/* q and -q are equivalent; selecting q.w >= 0 gives angle <= 180 degrees. */
		if (quaternion[3] < 0.0f)
		{
			quaternion[0] = -quaternion[0];
			quaternion[1] = -quaternion[1];
			quaternion[2] = -quaternion[2];
			quaternion[3] = -quaternion[3];
		}

		float sinHalfAngle = (float) Math.sqrt(quaternion[0] * quaternion[0]
			+ quaternion[1] * quaternion[1] + quaternion[2] * quaternion[2]);
		if (sinHalfAngle <= 1.0e-6f)
		{
			setOrientation(0.0f, 0.0f, 1.0f, 0.0f);
			return;
		}

		float angle = (float) (2.0 * Math.atan2(sinHalfAngle, quaternion[3]) * 180.0 / Math.PI);
		setOrientation(angle, quaternion[0] / sinHalfAngle, quaternion[1] / sinHalfAngle, quaternion[2] / sinHalfAngle);
	}

	static boolean isChildOf(Node parent, Node child)
	{
		for (Node n = child; n != null; n = n.parent)
		{
			if (n.parent == parent) { return true; }
		}

		return false;
	}

	/*
	 * Returns the node in dstRoot occupying the same relative position as
	 * srcNode in srcRoot, assuming both trees have an identical structure.
	 * Used to rewire intra-tree references (e.g. bones, the active camera)
	 * after duplicating a scene graph branch. Returns null if srcNode is
	 * not a descendant of srcRoot.
	 */
	static Node matchingNode(Node srcRoot, Node srcNode, Node dstRoot)
	{
		if (srcNode == srcRoot) { return dstRoot; }

		// Compute the depth of srcNode relative to the tree root.
		int depth = 0;
		for (Node n = srcNode; n != srcRoot; n = n.getParent())
		{
			if (n == null) { return null; }
			depth++;
		}

		// Collect the child indices from the root down to the node.
		int[] indices = new int[depth];
		int i = depth;
		for (Node n = srcNode; n != srcRoot; )
		{
			Node parent = n.getParent();
			if (!(parent instanceof Group)) { return null; }

			Group g = (Group) parent;
			int idx = -1;
			int children = g.getChildCount();
			for (int c = 0; c < children; c++)
			{
				if (g.getChild(c) == n) { idx = c; break; }
			}
			if (idx < 0) { return null; }

			indices[--i] = idx;
			n = parent;
		}

		// Walk the same path in the destination tree.
		Node dst = dstRoot;
		for (i = 0; i < indices.length; i++)
		{
			if (!(dst instanceof Group) || indices[i] >= ((Group) dst).getChildCount()) { return null; }
			dst = ((Group) dst).getChild(indices[i]);
		}

		return dst;
	}

	public Node getAlignmentReference(int axis)
	{
		if(axis == Y_AXIS) { return this.yRef; }
		else if(axis == Z_AXIS) { return this.zRef; }

		/* If it's not Y_AXIS or Z_AXIS, throw IllegalArgumentException as per JSR-184. */
		throw new IllegalArgumentException("Tried requesting alignment reference on invalid axis.");
	}

	public int getAlignmentTarget(int axis)
	{
		if(axis == Y_AXIS) { return this.yTarget; }
		else if(axis == Z_AXIS) { return this.zTarget; }

		/* If it's not Y_AXIS or Z_AXIS, throw IllegalArgumentException as per JSR-184. */
		throw new IllegalArgumentException("Tried requesting alignment target on invalid axis.");
	}

	public float getAlphaFactor() { return this.alphaFactor; }

	public Node getParent() { return this.parent; }

	/* Mostly used so we can find whether a child node*/
	public Node getRootNode()
	{
		Node root = this;
		while (root.parent != null)
		{
			root = root.parent;
		}
		return root;
	}

	public int getScope() { return this.scope; }

	public boolean getTransformTo(Node target, Transform transform)
	{
		if (target == null) { throw new NullPointerException("Target node cannot be null"); }
		if (transform == null) { throw new NullPointerException("Transform object cannot be null"); }

		if (target == this)
		{
			transform.setIdentity();
			return true;
		}

		Vector<Node> pathThis = new Vector<Node>();
		for (Node n = this; n != null; n = n.parent)
			{ pathThis.addElement(n); }

		Vector<Node> pathTarget = new Vector<Node>();
		for (Node n = target; n != null; n = n.parent)
			{ pathTarget.addElement(n); }

		// Check this and target share the same root.
		if (pathThis.elementAt(pathThis.size() - 1) != pathTarget.elementAt(pathTarget.size() - 1))
			{ return false; }

		int i = pathThis.size() - 1;
		int j = pathTarget.size() - 1;
		while (i >= 0 && j >= 0 && pathThis.elementAt(i) == pathTarget.elementAt(j))
		{
			i--;
			j--;
		}
		int lcaIndexThis = i + 1;
		int lcaIndexTarget = j + 1;

		// We accumulate transforms from this node all the way to the
		// lowest ancestor common to both nodes
		Transform thisToRoot = new Transform();
		Transform temp = new Transform();
		for (int k = lcaIndexThis - 1; k >= 0; k--)
		{
			Node n = (Node) pathThis.elementAt(k);
			n.getCompositeTransform(temp);
			thisToRoot.postMultiply(temp);
		}

		// Same for the target
		Transform targetToRoot = new Transform();
		for (int k = lcaIndexTarget - 1; k >= 0; k--)
		{
			Node n = (Node) pathTarget.elementAt(k);
			n.getCompositeTransform(temp);
			targetToRoot.postMultiply(temp);
		}

		// Will throw exception if matrix is not invertible
		targetToRoot.invert();

		targetToRoot.postMultiply(thisToRoot);
		transform.set(targetToRoot);
		return true;
	}

	public boolean isPickingEnabled() { return this.picking; }

	public boolean isRenderingEnabled() { return this.rendering; }

	public void setAlignment(Node zRef, int zTarget, Node yRef, int yTarget)
	{
		/*
		 * As per JSR-184, throw IllegalArgumentException if:
		 * yTarget or zTarget is not one of the symbolic constants listed above
		 * (zRef == yRef) && (zTarget == yTarget != NONE)
		 * zRef or yRef is this Node.
		 */
		if ((zTarget != NONE && zTarget != X_AXIS && zTarget != Y_AXIS && zTarget != Z_AXIS && zTarget != ORIGIN) ||
			(yTarget != NONE && yTarget != X_AXIS && yTarget != Y_AXIS && yTarget != Z_AXIS && yTarget != ORIGIN))
			{ throw new IllegalArgumentException("Node target axis is invalid."); }
		/* Sharing an alignment reference is valid when the two local axes target
		 * different axes of it. JSR-184 only rejects the ambiguous case where
		 * both alignments use the same reference and the same non-NONE target;
		 * this also correctly covers two null (runtime) references. */
		if (zRef == yRef && zTarget == yTarget && zTarget != NONE)
			{ throw new IllegalArgumentException("Tried to align with two references having the same axis."); }
		if (zRef == this || yRef == this)
			{ throw new IllegalArgumentException("Tried to use this node as one of the reference nodes."); }

		this.zRef = (zTarget != NONE) ? zRef : null;
		this.yRef = (yTarget != NONE) ? yRef : null;
		this.zTarget = zTarget;
		this.yTarget = yTarget;
	}

	public void setAlphaFactor(float alphaFactor)
	{
		/* As per JSR-184, throw IllegalArgumentException if factor < 0 or factor > 1.0.*/
		if (alphaFactor < 0 || alphaFactor > 1)
			{ throw new IllegalArgumentException("Tried to set AlphaFactor with out of range value."); }

		this.alphaFactor = alphaFactor;
	}

	public void setPickingEnable(boolean enable) { this.picking = enable; }

	public void setRenderingEnable(boolean enable) { this.rendering = enable; }

	public void setScope(int scope) { this.scope = scope; }

	void setParent(Node parent)
	{
		int nonCullableChange = getNonCullableCount();
		int renderableChange = getRenderableCount();

		if (this.parent != null)
		{
			this.parent.updateNodeCounters(-nonCullableChange, -renderableChange);
			if (renderableChange != 0)
			{
				this.parent.invalidateNode(new boolean[]{ true, true });
			}
		}

		this.parent = parent;

		if (parent != null)
		{
			boolean[] flags = new boolean[]{ renderableChange != 0, hasBones };
			parent.updateNodeCounters(nonCullableChange, renderableChange);
			parent.invalidateNode(flags);
		}
	}

	void invalidateNode(boolean[] flags)
	{
		Node node = this;
		while (node != null && (node.dirtyBits[0] != flags[0] || node.dirtyBits[1] != flags[1]))
		{
			System.arraycopy(flags, 0, node.dirtyBits, 0, 2);
			node = node.parent;
		}
	}

	void updateNodeCounters(int nonCullableChange, int renderableChange)
	{
		boolean hasRenderables = (renderableChange > 0);
		Node node = this;
		while (node != null)
		{
			if (node instanceof Group || node instanceof World)
			{
				((Group) node).numNonCullables += nonCullableChange;
				((Group) node).numRenderables += renderableChange;
				hasRenderables = ((Group) node).numRenderables > 0;
			}
			node.hasRenderables = hasRenderables;
			node = node.parent;
		}
	}

	// Getters for renderable/cullable count in Group objects.
	int getRenderableCount() { return 0; }
	int getNonCullableCount() { return 0; }

	@Override
	void updateProperty(int property, float[] value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Node.class.getPackage().getName() + "." + Node.class.getSimpleName() + ": " + "AnimTrack updating Node property");
		switch (property)
		{
			case AnimationTrack.ALPHA:
				setAlphaFactor(M3GMath.max(0.0f, M3GMath.min(1.0f, value[0])));
				break;
			case AnimationTrack.PICKABILITY:
				setPickingEnable(value[0] >= 0.5f);
				break;
			case AnimationTrack.VISIBILITY:
				setRenderingEnable(value[0] >= 0.5f);
				this.dirtyBits[0] = true;
				break;
			default:
				super.updateProperty(property, value);
		}
	}


	boolean animTrackCompatible(AnimationTrack track)
	{
		switch (track.getTargetProperty())
		{
			case AnimationTrack.ALPHA:
			case AnimationTrack.VISIBILITY:
			case AnimationTrack.PICKABILITY:
				return true;
			default:
				return super.animTrackCompatible(track);
		}
	}
}
