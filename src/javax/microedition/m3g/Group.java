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

public class Group extends Node
{
	public Node firstChild = null;
	public int numNonCullables = 0, numRenderables = 0;

	public Group() { }

	protected Object3D duplicateImpl()
	{
		Group copy = (Group) super.duplicateImpl();
		copy.firstChild = null;

		// We must Duplicate each child in the circular doubly-linked list
		if (this.firstChild != null)
		{
			Node curr = this.firstChild;
			do
			{
				Node childCopy = (Node) curr.duplicateImpl();
				copy.addChild(childCopy);
				curr = curr.right;
			}
			while (curr != this.firstChild);
		}

		return copy;
	}

	public void addChild(Node child)
	{
		if (child == null) { throw new NullPointerException("child cannot be null"); }
		if (child == this) { throw new IllegalArgumentException("cannot add self as child"); }
		if (child.getParent() != null) { throw new IllegalArgumentException("child already has parent"); }
		if (isAncestor(child)) { throw new IllegalArgumentException("Cannot add an ancestor as a child"); }

		if (firstChild == null)
		{
			firstChild = child;
			child.left = child;
			child.right = child;
		}
		else
		{
			Node lastChild = firstChild.left;

			lastChild.right = child;
			child.left = lastChild;

			child.right = firstChild;
			firstChild.left = child;
		}

		child.setParent(this);
		addReference(child);
	}

	public Node getChild(int idx)
	{
		if (idx < 0 || idx >= getChildCount())
			{ throw new IndexOutOfBoundsException("Negative child index"); }

		if (firstChild == null)
		{
			throw new IndexOutOfBoundsException("Group has no children");
		}

		Node n = firstChild;
		int count = 0;
		do
		{
			if (count == idx)
			{
				return n;
			}
			count++;
			n = n.right;
		}
		while (n != firstChild);

		throw new IndexOutOfBoundsException("Index " + idx + " out of bounds (child count is: " + count + ")");
	}

	public int getChildCount()
	{
		if (firstChild == null)
		{
			return 0;
		}

		int count = 0;
		Node child = firstChild;
		do
		{
			count++;
			child = child.right;
		}
		while (child != firstChild);

		return count;
	}

	public void removeChild(Node child)
	{
		if (child != null && firstChild != null)
		{
			Node n = firstChild;
			do
			{
				if (n == child)
				{
					if (n.right == n) // Only child in the list
					{
						firstChild = null;
					}
					else
					{
						n.right.left = n.left;
						n.left.right = n.right;

						if (firstChild == n)
						{
							firstChild = n.right;
						}
					}

					n.left = null;
					n.right = null;
					n.setParent(null);
					removeReference(child);
					return;
				}
				n = n.right;
			}
			while (n != firstChild);
		}
	}

	@Override
	boolean doAlign(Node ref)
	{
		if (!super.doAlign(ref))
		{
			return false;
		}

		Node child = firstChild;
		if (child != null)
		{
			do
			{
				if (!child.doAlign(ref))
				{
					return false;
				}
				child = child.right;
			}
			while (child != firstChild);
		}
		return true;
	}

	public boolean pick(int scope, float x, float y, Camera camera, RayIntersection ri)
	{
		if (camera == null) { throw new NullPointerException("Camera cannot be null"); }

		/*
		 * The M3G specification defines this ray by unprojecting the supplied
		 * viewport point at the near and far clipping planes. Keep the two
		 * camera-space points around as well: they are needed when testing scaled
		 * sprites, whose dimensions are defined in camera/NDC space.
		 */
		Transform projection = new Transform();
		camera.getProjection(projection);
		Transform inverseProjection = new Transform(projection);
		inverseProjection.invert();

		float[] cameraPoints = {
			2.0f * x - 1.0f, 1.0f - 2.0f * y, -1.0f, 1.0f,
			2.0f * x - 1.0f, 1.0f - 2.0f * y,  1.0f, 1.0f
		};
		inverseProjection.transform(cameraPoints);
		homogenize(cameraPoints);

		Transform cameraToGroup = new Transform();
		if (!camera.getTransformTo(this, cameraToGroup))
			{ throw new IllegalStateException("Camera and Group are not in the same scene graph"); }

		float[] groupPoints = (float[]) cameraPoints.clone();
		cameraToGroup.transform(groupPoints);
		homogenize(groupPoints);

		float[] ray = {
			groupPoints[0], groupPoints[1], groupPoints[2],
			groupPoints[4] - groupPoints[0],
			groupPoints[5] - groupPoints[1],
			groupPoints[6] - groupPoints[2]
		};

		PickResult result = pickInternal(scope, ray, camera, cameraPoints, projection, true);
		if (result.node == null) { return false; }
		if (ri != null)
		{
			ri.set(result.node, (float) result.distance, result.submesh, ray,
				result.normal, result.texS, result.texT);
		}
		return true;
	}

	public boolean pick(int scope, float ox, float oy, float oz, float dx, float dy, float dz, RayIntersection ri)
	{
		if (dx == 0.0f && dy == 0.0f && dz == 0.0f)
			{ throw new IllegalArgumentException("Ray direction vector cannot be zero"); }

		float[] ray = { ox, oy, oz, dx, dy, dz };
		PickResult result = pickInternal(scope, ray, null, null, null, false);
		if (result.node == null) { return false; }
		if (ri != null)
		{
			ri.set(result.node, (float) result.distance, result.submesh, ray,
				result.normal, result.texS, result.texT);
		}
		return true;
	}

	private PickResult pickInternal(int scope, float[] ray, Camera camera,
		float[] cameraPoints, Transform projection, boolean pickSprites)
	{
		PickResult result = new PickResult();

		/*
		 * Ancestors of the Group receiving pick are deliberately ignored. From
		 * this Group downwards, however, picking-enable is inherited normally.
		 */
		traverseForPick(this, true, scope, ray, camera, cameraPoints, projection,
			pickSprites, result);
		return result;
	}

	private void traverseForPick(Node node, boolean parentPicking, int scope, float[] ray,
		Camera camera, float[] cameraPoints, Transform projection, boolean pickSprites,
		PickResult result)
	{
		boolean picking = parentPicking && node.isPickingEnabled();
		if (!picking) { return; }

		if (node instanceof Mesh)
		{
			pickMesh((Mesh) node, scope, ray, result);

			/* A SkinnedMesh owns its skeleton Group. The skeleton is an ordinary
			 * scene-graph branch for picking purposes, and inherits this node's
			 * effective picking state but not its scope. */
			if (node instanceof SkinnedMesh)
			{
				traverseForPick(((SkinnedMesh) node).getSkeleton(), picking, scope, ray,
					camera, cameraPoints, projection, pickSprites, result);
			}
		}
		else if (pickSprites && node instanceof Sprite3D)
		{
			pickSprite((Sprite3D) node, scope, camera, cameraPoints, projection, result);
		}
		else if (node instanceof Group)
		{
			Node child = ((Group) node).firstChild;
			if (child != null)
			{
				do
				{
					traverseForPick(child, picking, scope, ray, camera, cameraPoints,
						projection, pickSprites, result);
					child = child.right;
				}
				while (child != ((Group) node).firstChild);
			}
		}
	}

	private void pickMesh(Mesh mesh, int scope, float[] groupRay, PickResult result)
	{
		if ((scope & mesh.getScope()) == 0) { return; }

		/* A null Appearance disables its entire submesh. Do not touch a disabled
		 * mesh's vertex data, because deferred validation only applies to data that
		 * is actually used for picking. */
		boolean hasEnabledSubmesh = false;
		for (int submesh = 0; submesh < mesh.getSubmeshCount(); submesh++)
		{
			if (mesh.getAppearance(submesh) != null)
			{
				hasEnabledSubmesh = true;
				break;
			}
		}
		if (!hasEnabledSubmesh) { return; }

		VertexBuffer vertices = mesh.getVertexBuffer();
		if (vertices == null) { throw new IllegalStateException("Pickable Mesh has no VertexBuffer"); }

		float[] positionScaleBias = new float[4];
		VertexArray positions = vertices.getPositions(positionScaleBias);
		if (positions == null) { throw new IllegalStateException("Pickable Mesh has no positions"); }
		if (positions.getComponentCount() != 3 || positions.getVertexCount() != vertices.getVertexCount())
			{ throw new IllegalStateException("Invalid Mesh position array"); }

		/*
		 * Intersect in the Group's coordinate system instead of transforming the
		 * ray into every mesh. The ray parameter t is then directly the distance
		 * reported by RayIntersection, and evaluating face culling here also keeps
		 * winding correct for reflected (negative-scale) node transforms.
		 */
		Transform meshToGroup = new Transform();
		if (!mesh.getTransformTo(this, meshToGroup))
			{ throw new IllegalStateException("Mesh and Group are not in the same scene graph"); }

		int vertexCount = positions.getVertexCount();
		float[] rawPositions = readVertexArray(positions);
		float[] localPositions = new float[vertexCount * 3];
		float[] transformedPositions = new float[vertexCount * 4];
		for (int vertex = 0; vertex < vertexCount; vertex++)
		{
			int source = vertex * 3;
			int local = vertex * 3;
			int target = vertex * 4;
			localPositions[local] = rawPositions[source] * positionScaleBias[0] + positionScaleBias[1];
			localPositions[local + 1] = rawPositions[source + 1] * positionScaleBias[0] + positionScaleBias[2];
			localPositions[local + 2] = rawPositions[source + 2] * positionScaleBias[0] + positionScaleBias[3];
			transformedPositions[target] = localPositions[local];
			transformedPositions[target + 1] = localPositions[local + 1];
			transformedPositions[target + 2] = localPositions[local + 2];
			transformedPositions[target + 3] = 1.0f;
		}
		meshToGroup.transform(transformedPositions);

		float[] groupPositions = new float[vertexCount * 3];
		for (int vertex = 0; vertex < vertexCount; vertex++)
		{
			int source = vertex * 4;
			int target = vertex * 3;
			float inverseW = 1.0f / transformedPositions[source + 3];
			groupPositions[target] = transformedPositions[source] * inverseW;
			groupPositions[target + 1] = transformedPositions[source + 1] * inverseW;
			groupPositions[target + 2] = transformedPositions[source + 2] * inverseW;
		}

		VertexArray normals = vertices.getNormals();
		float[] normalValues = null;
		if (normals != null)
		{
			if (normals.getComponentCount() != 3 || normals.getVertexCount() != vertexCount)
				{ throw new IllegalStateException("Invalid Mesh normal array"); }
			normalValues = readVertexArray(normals);
		}

		for (int submesh = 0; submesh < mesh.getSubmeshCount(); submesh++)
		{
			Appearance appearance = mesh.getAppearance(submesh);
			if (appearance == null) { continue; }

			IndexBuffer buffer = mesh.getIndexBuffer(submesh);
			if (buffer == null) { throw new IllegalStateException("Mesh has a null IndexBuffer"); }
			int indexCount = buffer.getIndexCount();
			if (indexCount < 0 || (indexCount % 3) != 0)
				{ throw new IllegalStateException("Invalid triangle index buffer"); }

			int[] indices = new int[indexCount];
			buffer.getIndices(indices);

			PolygonMode polygonMode = appearance.getPolygonMode();
			int culling = polygonMode != null ? polygonMode.getCulling() : PolygonMode.CULL_BACK;
			int winding = polygonMode != null ? polygonMode.getWinding() : PolygonMode.WINDING_CCW;

			for (int triangle = 0; triangle < indexCount; triangle += 3)
			{
				int ia = indices[triangle];
				int ib = indices[triangle + 1];
				int ic = indices[triangle + 2];
				if (ia < 0 || ib < 0 || ic < 0 || ia >= vertexCount || ib >= vertexCount || ic >= vertexCount)
					{ throw new IllegalStateException("Triangle index exceeds VertexBuffer"); }

				intersectTriangle(mesh, vertices, appearance, submesh, winding, culling,
					groupRay, groupPositions, localPositions, normalValues, ia, ib, ic, result);
			}
		}
	}

	/*
	 * Moller-Trumbore ray/triangle intersection. The input positions and ray
	 * are both in Group space, so t is invariant under every node transform
	 * between the Group and the Mesh.
	 */
	private static void intersectTriangle(Mesh mesh, VertexBuffer vertices, Appearance appearance,
		int submesh, int winding, int culling, float[] ray, float[] positions,
		float[] localPositions, float[] normalValues, int ia, int ib, int ic, PickResult result)
	{
		int a = 3 * ia;
		int b = 3 * ib;
		int c = 3 * ic;

		double e1x = positions[b] - positions[a];
		double e1y = positions[b + 1] - positions[a + 1];
		double e1z = positions[b + 2] - positions[a + 2];
		double e2x = positions[c] - positions[a];
		double e2y = positions[c + 1] - positions[a + 1];
		double e2z = positions[c + 2] - positions[a + 2];

		/* p = direction x edge2; determinant = edge1 . p */
		double px = ray[4] * e2z - ray[5] * e2y;
		double py = ray[5] * e2x - ray[3] * e2z;
		double pz = ray[3] * e2y - ray[4] * e2x;
		double determinant = e1x * px + e1y * py + e1z * pz;
		if (!isFinite(determinant) || determinant == 0.0) { return; }

		/* For CCW winding determinant > 0 means the face looks toward the
		 * incoming pick ray. Apply the same front/back rules as PolygonMode. */
		boolean frontFacing = determinant > 0.0;
		if (winding == PolygonMode.WINDING_CW) { frontFacing = !frontFacing; }
		if ((culling == PolygonMode.CULL_BACK && !frontFacing) ||
			(culling == PolygonMode.CULL_FRONT && frontFacing)) { return; }

		double tx = ray[0] - positions[a];
		double ty = ray[1] - positions[a + 1];
		double tz = ray[2] - positions[a + 2];
		double inverseDeterminant = 1.0 / determinant;

		double u = (tx * px + ty * py + tz * pz) * inverseDeterminant;
		if (!isFinite(u) || u < 0.0 || u > 1.0) { return; }

		/* q = (origin - A) x edge1 */
		double qx = ty * e1z - tz * e1y;
		double qy = tz * e1x - tx * e1z;
		double qz = tx * e1y - ty * e1x;
		double v = (ray[3] * qx + ray[4] * qy + ray[5] * qz) * inverseDeterminant;
		if (!isFinite(v) || v < 0.0 || u + v > 1.0) { return; }

		double distance = (e2x * qx + e2y * qy + e2z * qz) * inverseDeterminant;
		if (!isFinite(distance) || distance < 0.0 || distance >= result.distance) { return; }

		float weightA = (float) (1.0 - u - v);
		float weightB = (float) u;
		float weightC = (float) v;

		result.node = mesh;
		result.distance = distance;
		result.submesh = submesh;
		computeNormal(normalValues, localPositions, ia, ib, ic, weightA, weightB, weightC, result.normal);
		computeTextureCoordinates(vertices, appearance, ia, ib, ic, weightA, weightB, weightC,
			result.texS, result.texT);
	}

	private static void computeNormal(float[] normalValues, float[] localPositions, int ia, int ib, int ic,
		float weightA, float weightB, float weightC, float[] normal)
	{
		double nx, ny, nz;
		if (normalValues != null)
		{
			nx = weightA * normalValues[3 * ia] + weightB * normalValues[3 * ib] + weightC * normalValues[3 * ic];
			ny = weightA * normalValues[3 * ia + 1] + weightB * normalValues[3 * ib + 1] + weightC * normalValues[3 * ic + 1];
			nz = weightA * normalValues[3 * ia + 2] + weightB * normalValues[3 * ib + 2] + weightC * normalValues[3 * ic + 2];
		}
		else
		{
			/* Mesh normals are technically undefined without vertex normals. A
			 * geometric fallback is nevertheless useful and stays in the required
			 * Mesh-local coordinate system, unlike the Group-space intersection data. */
			int a = 3 * ia;
			int b = 3 * ib;
			int c = 3 * ic;
			double e1x = localPositions[b] - localPositions[a];
			double e1y = localPositions[b + 1] - localPositions[a + 1];
			double e1z = localPositions[b + 2] - localPositions[a + 2];
			double e2x = localPositions[c] - localPositions[a];
			double e2y = localPositions[c + 1] - localPositions[a + 1];
			double e2z = localPositions[c + 2] - localPositions[a + 2];
			nx = e1y * e2z - e1z * e2y;
			ny = e1z * e2x - e1x * e2z;
			nz = e1x * e2y - e1y * e2x;
		}

		double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
		if (!isFinite(length) || length == 0.0)
		{
			normal[0] = 0.0f;
			normal[1] = 0.0f;
			normal[2] = 1.0f;
			return;
		}

		normal[0] = (float) (nx / length);
		normal[1] = (float) (ny / length);
		normal[2] = (float) (nz / length);
	}

	private static void computeTextureCoordinates(VertexBuffer vertices, Appearance appearance,
		int ia, int ib, int ic, float weightA, float weightB, float weightC,
		float[] texS, float[] texT)
	{
		for (int unit = 0; unit < texS.length; unit++)
		{
			/* Coordinates of disabled texturing units are undefined. Clearing them
			 * makes sure they cannot leak from an earlier, nearer candidate. */
			texS[unit] = 0.0f;
			texT[unit] = 0.0f;

			Texture2D texture = appearance.getTexture(unit);
			if (texture == null) { continue; }

			float[] scaleBias = new float[4];
			VertexArray coords = vertices.getTexCoords(unit, scaleBias);
			if (coords == null) { continue; }

			int components = coords.getComponentCount();
			if ((components != 2 && components != 3) || coords.getVertexCount() <= Math.max(ia, Math.max(ib, ic)))
				{ continue; }

			float[] values = readVertexArray(coords);
			float[] coordinate = { 0.0f, 0.0f, 0.0f, 1.0f };
			for (int component = 0; component < components; component++)
			{
				coordinate[component] = scaleBias[0] *
					(weightA * values[components * ia + component] +
					 weightB * values[components * ib + component] +
					 weightC * values[components * ic + component]) + scaleBias[component + 1];
			}

			/* Texture transformation is homogeneous. Applying it after barycentric
			 * interpolation is equivalent to transforming each vertex first. */
			Transform textureTransform = new Transform();
			texture.getCompositeTransform(textureTransform);
			textureTransform.transform(coordinate);
			if (coordinate[3] != 0.0f && isFinite(coordinate[3]))
			{
				float s = coordinate[0] / coordinate[3];
				float t = coordinate[1] / coordinate[3];
				if (isFinite(s) && isFinite(t))
				{
					texS[unit] = s;
					texT[unit] = t;
				}
			}
		}
	}

	private void pickSprite(Sprite3D sprite, int scope, Camera camera, float[] cameraPoints,
		Transform projection, PickResult result)
	{
		if (!sprite.isScaled() || sprite.getAppearance() == null ||
			(scope & sprite.getScope()) == 0) { return; }

		Image2D image = sprite.getImage();
		if (image == null) { return; }

		int cropWidth = sprite.getCropWidth();
		int cropHeight = sprite.getCropHeight();
		if (cropWidth == 0 || cropHeight == 0) { return; }
		boolean flipX = cropWidth < 0;
		boolean flipY = cropHeight < 0;
		if (flipX) { cropWidth = -cropWidth; }
		if (flipY) { cropHeight = -cropHeight; }

		/* Sprite3D defines its dimensions by the camera-space lengths of its
		 * local unit X and Y axes, then re-aligns those lengths to camera X/Y
		 * before projection. This follows the specification literally, including
		 * generic (not just ordinary perspective) projection matrices. */
		Transform spriteToCamera = new Transform();
		if (!sprite.getTransformTo(camera, spriteToCamera))
			{ throw new IllegalStateException("Sprite and Camera are not in the same scene graph"); }

		float[] eyePoints = { 0.0f, 0.0f, 0.0f, 1.0f,
			1.0f, 0.0f, 0.0f, 1.0f,
			0.0f, 1.0f, 0.0f, 1.0f };
		spriteToCamera.transform(eyePoints);
		if (!hasValidW(eyePoints[3]) || !hasValidW(eyePoints[7]) || !hasValidW(eyePoints[11])) { return; }

		double ox = eyePoints[0] / eyePoints[3];
		double oy = eyePoints[1] / eyePoints[3];
		double oz = eyePoints[2] / eyePoints[3];
		double xref = eyePoints[4] / eyePoints[7];
		double yref = eyePoints[5] / eyePoints[7];
		double zref = eyePoints[6] / eyePoints[7];
		double width = Math.sqrt((xref - ox) * (xref - ox) + (yref - oy) * (yref - oy) + (zref - oz) * (zref - oz));
		xref = eyePoints[8] / eyePoints[11];
		yref = eyePoints[9] / eyePoints[11];
		zref = eyePoints[10] / eyePoints[11];
		double height = Math.sqrt((xref - ox) * (xref - ox) + (yref - oy) * (yref - oy) + (zref - oz) * (zref - oz));
		if (!isFinite(width) || !isFinite(height) || width == 0.0 || height == 0.0) { return; }

		/* Keep the original homogeneous origin for the projection step. The
		 * reference-axis distances were calculated after normalizing to W=1,
		 * but the Sprite3D formula projects o' itself and adds the distances as
		 * vectors (with W=0). This distinction matters for a non-affine modelview
		 * transform. */
		float[] spriteClip = { eyePoints[0], eyePoints[1], eyePoints[2], eyePoints[3],
			eyePoints[0] + (float) width, eyePoints[1], eyePoints[2], eyePoints[3],
			eyePoints[0], eyePoints[1] + (float) height, eyePoints[2], eyePoints[3] };
		projection.transform(spriteClip);
		if (!hasPositiveW(spriteClip[3]) || !hasPositiveW(spriteClip[7]) || !hasPositiveW(spriteClip[11])) { return; }

		double centerX = spriteClip[0] / spriteClip[3];
		double centerY = spriteClip[1] / spriteClip[3];
		double centerZ = spriteClip[2] / spriteClip[3];
		double projectedX = spriteClip[4] / spriteClip[7];
		double projectedY = spriteClip[5] / spriteClip[7];
		double projectedZ = spriteClip[6] / spriteClip[7];
		double halfWidth = 0.5 * Math.sqrt((projectedX - centerX) * (projectedX - centerX) +
			(projectedY - centerY) * (projectedY - centerY) + (projectedZ - centerZ) * (projectedZ - centerZ));
		projectedX = spriteClip[8] / spriteClip[11];
		projectedY = spriteClip[9] / spriteClip[11];
		projectedZ = spriteClip[10] / spriteClip[11];
		double halfHeight = 0.5 * Math.sqrt((projectedX - centerX) * (projectedX - centerX) +
			(projectedY - centerY) * (projectedY - centerY) + (projectedZ - centerZ) * (projectedZ - centerZ));
		if (!isFinite(centerX) || !isFinite(centerY) || !isFinite(centerZ) ||
			!isFinite(halfWidth) || !isFinite(halfHeight) || halfWidth == 0.0 || halfHeight == 0.0) { return; }

		/* The sprite has a constant projected depth. Solve the homogeneous
		 * projection of the camera-space pick ray for that depth, so this works
		 * for perspective, parallel, and generic Camera projections alike. */
		float[] clipRay = {
			cameraPoints[0], cameraPoints[1], cameraPoints[2], 1.0f,
			cameraPoints[4] - cameraPoints[0], cameraPoints[5] - cameraPoints[1],
			cameraPoints[6] - cameraPoints[2], 0.0f
		};
		projection.transform(clipRay);
		double denominator = clipRay[6] - centerZ * clipRay[7];
		if (!isFinite(denominator) || denominator == 0.0) { return; }
		double distance = (centerZ * clipRay[3] - clipRay[2]) / denominator;
		if (!isFinite(distance) || distance < 0.0 || distance >= result.distance) { return; }

		double hitW = clipRay[3] + distance * clipRay[7];
		if (!hasValidW(hitW)) { return; }
		double hitX = (clipRay[0] + distance * clipRay[4]) / hitW;
		double hitY = (clipRay[1] + distance * clipRay[5]) / hitW;
		if (!isFinite(hitX) || !isFinite(hitY) ||
			hitX < centerX - halfWidth || hitX > centerX + halfWidth ||
			hitY < centerY - halfHeight || hitY > centerY + halfHeight) { return; }

		/* The full crop rectangle occupies the sprite; parts outside the image
		 * are imaginary transparent pixels. The alpha test is intentionally done
		 * before the inherited alpha factor, as specified for Sprite3D picking. */
		double s = (hitX - (centerX - halfWidth)) / (2.0 * halfWidth);
		double t = ((centerY + halfHeight) - hitY) / (2.0 * halfHeight);
		int sampleX = sprite.getCropX() + cropSample(s, cropWidth, flipX);
		int sampleY = sprite.getCropY() + cropSample(t, cropHeight, flipY);
		if (sampleX < 0 || sampleX >= image.getWidth() || sampleY < 0 || sampleY >= image.getHeight()) { return; }

		CompositingMode compositingMode = sprite.getAppearance().getCompositingMode();
		float alphaThreshold = compositingMode != null ? compositingMode.getAlphaThreshold() : 0.0f;
		float alpha = ((image.getPixel(sampleX, sampleY) >>> 24) & 0xFF) / 255.0f;
		if (alpha < alphaThreshold) { return; }

		result.node = sprite;
		result.distance = distance;
		result.submesh = 0;
		result.normal[0] = 0.0f;
		result.normal[1] = 0.0f;
		result.normal[2] = 1.0f;
		for (int unit = 0; unit < result.texS.length; unit++)
		{
			result.texS[unit] = 0.0f;
			result.texT[unit] = 0.0f;
		}
		if (result.texS.length > 0)
		{
			/* Returned Sprite3D coordinates span the complete sprite rectangle;
			 * crop offsets and flips affect sampling only, not these values. */
			result.texS[0] = (float) s;
			result.texT[0] = (float) t;
		}
	}

	private static int cropSample(double coordinate, int length, boolean flipped)
	{
		double sampledCoordinate = flipped ? 1.0 - coordinate : coordinate;
		int sample = (int) (sampledCoordinate * length);
		if (sample < 0) { return 0; }
		if (sample >= length) { return length - 1; }
		return sample;
	}

	private static float[] readVertexArray(VertexArray array)
	{
		int vertexCount = array.getVertexCount();
		int elements = vertexCount * array.getComponentCount();
		float[] values = new float[elements];
		if (array.getComponentType() == 1)
		{
			byte[] source = new byte[elements];
			array.get(0, vertexCount, source);
			for (int i = 0; i < elements; i++) { values[i] = source[i]; }
		}
		else
		{
			short[] source = new short[elements];
			array.get(0, vertexCount, source);
			for (int i = 0; i < elements; i++) { values[i] = source[i]; }
		}
		return values;
	}

	private static void homogenize(float[] points)
	{
		for (int offset = 0; offset < points.length; offset += 4)
		{
			float inverseW = 1.0f / points[offset + 3];
			points[offset] *= inverseW;
			points[offset + 1] *= inverseW;
			points[offset + 2] *= inverseW;
			points[offset + 3] = 1.0f;
		}
	}

	private static boolean hasValidW(double value) { return isFinite(value) && value != 0.0; }
	private static boolean hasPositiveW(double value) { return isFinite(value) && value > 0.0; }
	private static boolean isFinite(double value) { return !Double.isNaN(value) && !Double.isInfinite(value); }

	private static class PickResult
	{
		Node node;
		double distance = Double.POSITIVE_INFINITY;
		int submesh;
		float[] normal = { 0.0f, 0.0f, 1.0f };
		float[] texS = new float[Graphics3D.NUM_TEXTURE_UNITS];
		float[] texT = new float[Graphics3D.NUM_TEXTURE_UNITS];
	}

	private boolean isAncestor(Node potentialChild)
	{
		Node p = this.getParent();
		while (p != null)
		{
			if (p == potentialChild)
			{
				return true;
			}
			p = p.getParent();
		}
		return false;
	}

	int getRenderableCount() { return this.numRenderables; }
	int getNonCullableCount() { return this.numNonCullables; }
}
