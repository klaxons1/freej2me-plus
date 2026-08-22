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

import java.util.ArrayList;

public class SkinnedMesh extends Mesh
{
	public Group skeleton;

	private VertexBuffer skinnedVertices;
	private ArrayList<BoneData> bones = new ArrayList<BoneData>();

	// Used to store bone information for vertex transforms.
	private static class BoneData
	{
		Node bone;
		int weight;
		int firstVertex;
		int numVertices;
		Transform initialTransform = new Transform();

		BoneData(Node bone, int weight, int firstVertex, int numVertices)
		{
			this.bone = bone;
			this.weight = weight;
			this.firstVertex = firstVertex;
			this.numVertices = numVertices;
		}
	}

	public SkinnedMesh(VertexBuffer vertices, IndexBuffer[] submeshes, Appearance[] appearances, Group skeleton)
	{
		super(vertices, submeshes, appearances);
		checkSkeleton(skeleton);
		this.skeleton = skeleton;
		this.skeleton.setParent(this);
		addReference(this.skeleton);
	}

	public SkinnedMesh(VertexBuffer vertices, IndexBuffer submeshes, Appearance appearances, Group skeleton)
	{
		super(vertices, submeshes, appearances);
		checkSkeleton(skeleton);
		this.skeleton = skeleton;
		this.skeleton.setParent(this);
		addReference(this.skeleton);
	}

	protected SkinnedMesh() { }

	protected Object3D duplicateImpl()
	{
		SkinnedMesh copy = (SkinnedMesh) super.duplicateImpl();

		copy.removeReference(this.skeleton);
		Group copySkeleton = (Group) this.skeleton.duplicate();
		copy.skeleton = copySkeleton;
		copy.skeleton.setParent(copy);
		copy.addReference(copySkeleton);
		copy.skinnedVertices = null;
		copy.bones = new ArrayList<BoneData>();

		for (BoneData b : this.bones)
		{
			Node clonedBone = findCorrespondingNode(this.skeleton, copySkeleton, b.bone);
			if (clonedBone != null)
			{
				BoneData copyBone = new BoneData(clonedBone, b.weight, b.firstVertex, b.numVertices);
				copyBone.initialTransform.set(b.initialTransform);
				copy.bones.add(copyBone);
			}
		}

		return copy;
	}

	public void addTransform(Node bone, int weight, int firstVertex, int numVertices)
	{
		if (bone == null) { throw new NullPointerException("Bone node cannot be null"); }
		if (weight <= 0) { throw new IllegalArgumentException("Weight must be positive"); }
		if (numVertices <= 0) { throw new IllegalArgumentException("NumVertices must be positive"); }
		if (firstVertex < 0 || (long) firstVertex + numVertices > 65535L)
			{ throw new IndexOutOfBoundsException("Bone vertex range is out of bounds"); }
		if (!isChildOf(this.skeleton, bone) && bone != this.skeleton)
			{ throw new IllegalArgumentException("Bone node must belong to the skeleton"); }

		BoneData data = new BoneData(bone, weight, firstVertex, numVertices);
		/* JSR-184 states that B is this.getTransformTo(bone) at the instant
		 * addTransform is called, not when the mesh is first rendered. */
		if (!this.getTransformTo(bone, data.initialTransform))
			{ throw new ArithmeticException("At-rest bone transform cannot be computed"); }

		bones.add(data);
		bone.hasBones = true;
		this.dirtyBits[1] = true;
	}

	public void getBoneTransform(Node bone, Transform transform)
	{
		if (bone == null || transform == null)
			{ throw new NullPointerException("Bone and Transform cannot be null"); }
		if (!isChildOf(this.skeleton, bone) && bone != this.skeleton)
			{ throw new IllegalArgumentException("Node is not in this skeleton"); }

		for (BoneData data : bones)
		{
			if (data.bone == bone)
			{
				transform.set(data.initialTransform);
				return;
			}
		}
		/* The value is explicitly undefined for a skeleton node without vertices. */
		transform.setIdentity();
	}

	public int getBoneVertices(Node bone, int[] indices, float[] weights)
	{
		if (bone == null) { throw new NullPointerException("Bone node cannot be null"); }
		if (!isChildOf(this.skeleton, bone) && bone != this.skeleton)
			{ throw new IllegalArgumentException("Node is not in this skeleton"); }

		int vertexLimit = 0;
		for (BoneData data : bones)
			{ vertexLimit = M3GMath.max(vertexLimit, data.firstVertex + data.numVertices); }
		int[][] selected = selectInfluences(vertexLimit, false);
		int count = 0;
		for (int vertex = 0; vertex < vertexLimit; vertex++)
			{ if (selectedWeight(selected[vertex], bone) > 0.0f) { count++; } }
		if ((indices != null && indices.length < count) || (weights != null && weights.length < count))
			{ throw new IllegalArgumentException("Result array is too short"); }

		int result = 0;
		for (int vertex = 0; vertex < vertexLimit; vertex++)
		{
			float boneWeight = selectedWeight(selected[vertex], bone);
			if (boneWeight <= 0.0f) { continue; }
			if (indices != null) { indices[result] = vertex; }
			if (weights != null) { weights[result] = boneWeight / totalWeight(selected[vertex]); }
			result++;
		}
		return count;
	}

	public Group getSkeleton() { return skeleton; }

	private void checkSkeleton(Group skeleton)
	{
		if (skeleton == null) { throw new NullPointerException("Skeleton cannot be null"); }
		if (skeleton instanceof World) { throw new IllegalArgumentException("Skeleton cannot be a World"); }
		if (skeleton.getParent() != null) { throw new IllegalArgumentException("Skeleton already has a parent"); }
	}

	private static Node findCorrespondingNode(Node original, Node copy, Node target)
	{
		if (original == target) { return copy; }
		if (original instanceof Group && copy instanceof Group)
		{
			Group originalGroup = (Group) original;
			Group copyGroup = (Group) copy;
			for (int i = 0; i < originalGroup.getChildCount(); i++)
			{
				Node found = findCorrespondingNode(originalGroup.getChild(i),
					copyGroup.getChild(i), target);
				if (found != null) { return found; }
			}
		}
		return null;
	}

	@Override
	public VertexBuffer getVertexBuffer()
	{
		VertexBuffer base = super.getVertexBuffer();
		if (bones.isEmpty() || base == null) { return base; }

		VertexArray positions = base.getPositions(null);
		if (positions == null) { return base; }
		for (BoneData data : bones)
		{
			/* JSR-184 defers these checks because VertexBuffer length can change. */
			if ((long) data.firstVertex + data.numVertices > positions.getVertexCount())
				{ throw new IllegalStateException("Bone vertex range exceeds the VertexBuffer"); }
		}

		createSkinnedBuffer(base);
		applySkinning(base);
		return skinnedVertices;
	}

	private void createSkinnedBuffer(VertexBuffer base)
	{
		skinnedVertices = new VertexBuffer();
		skinnedVertices.setDefaultColor(base.getDefaultColor());
		float[] scaleBias = new float[4];
		VertexArray array = base.getPositions(scaleBias);
		if (array != null)
		{
			/* A zero source scale still permits bones to separate coincident vertices;
			 * use unit output quantization so those transformed positions remain representable. */
			final float outputScale = (scaleBias[0] != 0.0f) ? scaleBias[0] : 1.0f;
			skinnedVertices.setPositions((VertexArray) array.duplicate(), outputScale,
				new float[] { scaleBias[1], scaleBias[2], scaleBias[3] });
		}
		array = base.getNormals();
		if (array != null) { skinnedVertices.setNormals((VertexArray) array.duplicate()); }
		array = base.getColors();
		if (array != null) { skinnedVertices.setColors(array); }
		for (int unit = 0; unit < Graphics3D.NUM_TEXTURE_UNITS; unit++)
		{
			array = base.getTexCoords(unit, scaleBias);
			if (array != null)
			{
				float[] bias = new float[array.getComponentCount()];
				System.arraycopy(scaleBias, 1, bias, 0, bias.length);
				skinnedVertices.setTexCoords(unit, array, scaleBias[0], bias);
			}
		}
	}

	private int[][] selectInfluences(int vertexCount, boolean validate)
	{
		final int limit = Graphics3D.MAX_TRANSFORMS_PER_VERTEX;
		int[][] selected = new int[vertexCount][limit];
		for (int vertex = 0; vertex < vertexCount; vertex++)
			{ for (int slot = 0; slot < limit; slot++) { selected[vertex][slot] = -1; } }

		for (int influence = 0; influence < bones.size(); influence++)
		{
			BoneData data = bones.get(influence);
			int end = data.firstVertex + data.numVertices;
			if (validate && end > vertexCount)
				{ throw new IllegalStateException("Bone vertex range exceeds the VertexBuffer"); }
			end = M3GMath.min(end, vertexCount);
			for (int vertex = data.firstVertex; vertex < end; vertex++)
			{
				int slot = 0;
				while (slot < limit && selected[vertex][slot] >= 0 &&
					bones.get(selected[vertex][slot]).weight >= data.weight) { slot++; }
				if (slot == limit) { continue; }
				for (int move = limit - 1; move > slot; move--)
					{ selected[vertex][move] = selected[vertex][move - 1]; }
				selected[vertex][slot] = influence;
			}
		}
		return selected;
	}

	private void applySkinning(VertexBuffer base)
	{
		float[] scaleBias = new float[4];
		VertexArray basePositions = base.getPositions(scaleBias);
		VertexArray outPositions = skinnedVertices.getPositions(null);
		final int vertexCount = basePositions.getVertexCount();
		final int[][] selected = selectInfluences(vertexCount, true);

		float[][] positionMatrices = new float[bones.size()][16];
		float[][] normalMatrices = new float[bones.size()][16];
		Transform boneToMesh = new Transform();
		Transform skin = new Transform();
		for (int i = 0; i < bones.size(); i++)
		{
			BoneData data = bones.get(i);
			if (!data.bone.getTransformTo(this, boneToMesh))
				{ throw new ArithmeticException("Current bone transform cannot be computed"); }
			skin.set(boneToMesh);
			skin.postMultiply(data.initialTransform);
			skin.get(positionMatrices[i]);
			try
			{
				skin.invert();
				skin.transpose();
			}
			catch (ArithmeticException undefinedNormalTransform)
			{
				/* Singular skinning leaves normals undefined; keep a deterministic
				 * direction transform without failing otherwise valid rendering. */
				skin.set(boneToMesh);
				skin.postMultiply(data.initialTransform);
			}
			skin.get(normalMatrices[i]);
		}

		float[] sourcePositions = readSigned(basePositions);
		float[] resultPositions = new float[sourcePositions.length];
		final float scale = scaleBias[0];
		final float biasX = scaleBias[1], biasY = scaleBias[2], biasZ = scaleBias[3];
		for (int vertex = 0; vertex < vertexCount; vertex++)
		{
			int offset = 3 * vertex;
			float x = sourcePositions[offset] * scale + biasX;
			float y = sourcePositions[offset + 1] * scale + biasY;
			float z = sourcePositions[offset + 2] * scale + biasZ;
			float totalWeight = totalWeight(selected[vertex]);
			if (totalWeight == 0.0f)
			{
				// No explicit bone means an implicit identity association with this mesh.
				resultPositions[offset] = x;
				resultPositions[offset + 1] = y;
				resultPositions[offset + 2] = z;
			}
			else
			{
				for (int slot = 0; slot < Graphics3D.MAX_TRANSFORMS_PER_VERTEX; slot++)
				{
					int influence = selected[vertex][slot];
					if (influence < 0) { break; }
					float weight = bones.get(influence).weight / totalWeight;
					float[] m = positionMatrices[influence];
					/* Transform stores row-major matrices and multiplies column vectors. */
					resultPositions[offset] += weight * (m[0]*x + m[1]*y + m[2]*z + m[3]);
					resultPositions[offset + 1] += weight * (m[4]*x + m[5]*y + m[6]*z + m[7]);
					resultPositions[offset + 2] += weight * (m[8]*x + m[9]*y + m[10]*z + m[11]);
				}
			}
			final float outputScale = (scale != 0.0f) ? scale : 1.0f;
			resultPositions[offset] = (resultPositions[offset] - biasX) / outputScale;
			resultPositions[offset + 1] = (resultPositions[offset + 1] - biasY) / outputScale;
			resultPositions[offset + 2] = (resultPositions[offset + 2] - biasZ) / outputScale;
		}
		writeSigned(outPositions, resultPositions);

		VertexArray baseNormals = base.getNormals();
		VertexArray outNormals = skinnedVertices.getNormals();
		if (baseNormals == null || outNormals == null) { return; }
		float[] sourceNormals = readSigned(baseNormals);
		float[] resultNormals = new float[sourceNormals.length];
		for (int vertex = 0; vertex < vertexCount; vertex++)
		{
			int offset = 3 * vertex;
			float totalWeight = totalWeight(selected[vertex]);
			if (totalWeight == 0.0f)
			{
				resultNormals[offset] = sourceNormals[offset];
				resultNormals[offset + 1] = sourceNormals[offset + 1];
				resultNormals[offset + 2] = sourceNormals[offset + 2];
				continue;
			}
			for (int slot = 0; slot < Graphics3D.MAX_TRANSFORMS_PER_VERTEX; slot++)
			{
				int influence = selected[vertex][slot];
				if (influence < 0) { break; }
				float weight = bones.get(influence).weight / totalWeight;
				float[] m = normalMatrices[influence];
				float x = sourceNormals[offset], y = sourceNormals[offset + 1], z = sourceNormals[offset + 2];
				resultNormals[offset] += weight * (m[0]*x + m[1]*y + m[2]*z);
				resultNormals[offset + 1] += weight * (m[4]*x + m[5]*y + m[6]*z);
				resultNormals[offset + 2] += weight * (m[8]*x + m[9]*y + m[10]*z);
			}
		}
		writeSigned(outNormals, resultNormals);
	}

	private float selectedWeight(int[] selected, Node bone)
	{
		float total = 0.0f;
		for (int slot = 0; slot < selected.length && selected[slot] >= 0; slot++)
		{
			BoneData data = bones.get(selected[slot]);
			if (data.bone == bone) { total += data.weight; }
		}
		return total;
	}

	private float totalWeight(int[] selected)
	{
		float total = 0.0f;
		for (int slot = 0; slot < selected.length && selected[slot] >= 0; slot++)
			{ total += bones.get(selected[slot]).weight; }
		return total;
	}

	private static float[] readSigned(VertexArray array)
	{
		int count = array.getVertexCount();
		int elements = count * array.getComponentCount();
		float[] result = new float[elements];
		if (array.getComponentType() == 1)
		{
			byte[] values = new byte[elements];
			array.get(0, count, values);
			for (int i = 0; i < elements; i++) { result[i] = values[i]; }
		}
		else
		{
			short[] values = new short[elements];
			array.get(0, count, values);
			for (int i = 0; i < elements; i++) { result[i] = values[i]; }
		}
		return result;
	}

	private static void writeSigned(VertexArray array, float[] values)
	{
		int count = array.getVertexCount();
		if (array.getComponentType() == 1)
		{
			byte[] result = new byte[values.length];
			for (int i = 0; i < values.length; i++)
				{ result[i] = (byte) M3GMath.max(-128, M3GMath.min(127, M3GMath.round(values[i]))); }
			array.set(0, count, result);
		}
		else
		{
			short[] result = new short[values.length];
			for (int i = 0; i < values.length; i++)
				{ result[i] = (short) M3GMath.max(-32768, M3GMath.min(32767, M3GMath.round(values[i]))); }
			array.set(0, count, result);
		}
	}

}
