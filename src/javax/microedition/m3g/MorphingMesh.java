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

import org.recompile.mobile.Mobile;

public class MorphingMesh extends Mesh
{
	private VertexBuffer[] targets;
	private float[] weights;
	private VertexBuffer morphedVertices;

	private MorphingMesh() { }

	public MorphingMesh(VertexBuffer base, VertexBuffer[] targets, IndexBuffer[] submeshes, Appearance[] appearances)
	{
		super(base, submeshes, appearances);
		checkTargets(targets);

		this.targets = new VertexBuffer[targets.length];
		this.weights = new float[targets.length];

		for (int i = 0; i < targets.length; i++)
		{
			this.targets[i] = targets[i];
			addReference(this.targets[i]);
		}
	}

	public MorphingMesh(VertexBuffer base, VertexBuffer[] targets, IndexBuffer submeshes, Appearance appearances)
	{
		super(base, submeshes, appearances);
		checkTargets(targets);

		this.targets = new VertexBuffer[targets.length];
		this.weights = new float[targets.length];

		for (int i = 0; i < targets.length; i++)
		{
			this.targets[i] = targets[i];
			addReference(this.targets[i]);
		}
	}

	protected Object3D duplicateImpl()
	{
		MorphingMesh copy = (MorphingMesh) super.duplicateImpl();

		copy.targets = new VertexBuffer[this.targets.length];
		copy.weights = new float[this.targets.length];

		for (int i = 0; i < this.targets.length; i++)
		{
			copy.targets[i] = this.targets[i];
			copy.weights[i] = this.weights[i];
			copy.addReference(copy.targets[i]);
		}
		copy.morphedVertices = null;

		return copy;
	}

	public VertexBuffer getMorphTarget(int index)
	{
		if (index < 0 || index >= targets.length)
		{
			throw new IndexOutOfBoundsException("Morph target index out of bounds: " + index);
		}

		return targets[index];
	}

	public int getMorphTargetCount() { return targets.length; }

	public void setWeights(float[] weights)
	{
		if (weights == null)
		{
			throw new NullPointerException("Weights must not be null");
		}

		if (weights.length < getMorphTargetCount())
		{
			throw new IllegalArgumentException("Number of weights must be greater or equal to getMorphTargetCount()");
		}

		System.arraycopy(weights, 0, this.weights, 0, targets.length);
		this.dirtyBits[1] = true;
	}

	public void getWeights(float[] weights)
	{
		if (weights == null)
		{
			throw new NullPointerException("Weights must not be null");
		}
		if (weights.length < getMorphTargetCount())
		{
			throw new IllegalArgumentException("Number of weights must be greater or equal to getMorphTargetCount()");
		}

		System.arraycopy(this.weights, 0, weights, 0, this.weights.length);
	}

	private void checkTargets(VertexBuffer[] targets)
	{
		if (targets == null) { throw new NullPointerException("MorphingMesh has no Target array"); }
		if (targets.length == 0)
		{
			throw new IllegalArgumentException("Targets array is empty");
		}

		for (int i = 0; i < targets.length; i++)
		{
			if (targets[i] == null)
			{
				throw new NullPointerException("Morph target at index " + i + " is null");
			}
		}
		/* JSR-184 defers target layout and vertex-count validation until the
		 * resultant mesh is needed for rendering or picking. */
	}

	@Override
	public VertexBuffer getVertexBuffer()
	{
		VertexBuffer base = super.getVertexBuffer();
		if (targets == null || targets.length == 0 || base == null)
		{
			return base;
		}

		validateMorphStructure(base);
		/* Rebuild so changes to the base arrays and their scale or bias are visible. */
		createMorphedBuffer(base);

		/*
		 * JSR-184 states that the result is B + sum(wi(Ti-B)) for the default
		 * color and every array present in the targets. Re-evaluate on access so
		 * edits made through mutable target VertexArrays are visible when rendered.
		 */
		morphDefaultColor(base);
		morphArray(base.getPositions(null), morphedVertices.getPositions(null), 0, 0, false);
		morphArray(base.getNormals(), morphedVertices.getNormals(), 1, 0, false);
		morphArray(base.getColors(), morphedVertices.getColors(), 2, 0, true);
		for (int unit = 0; unit < Graphics3D.NUM_TEXTURE_UNITS; unit++)
		{
			morphArray(base.getTexCoords(unit, null), morphedVertices.getTexCoords(unit, null),
				3, unit, false);
		}
		this.dirtyBits[1] = false;
		return morphedVertices;
	}

	private void createMorphedBuffer(VertexBuffer base)
	{
		morphedVertices = new VertexBuffer();
		morphedVertices.setDefaultColor(base.getDefaultColor());

		float[] scaleBias = new float[4];
		VertexArray array = base.getPositions(scaleBias);
		if (array != null)
		{
			morphedVertices.setPositions((VertexArray) array.duplicate(), scaleBias[0],
				new float[] { scaleBias[1], scaleBias[2], scaleBias[3] });
		}

		array = base.getNormals();
		if (array != null) { morphedVertices.setNormals((VertexArray) array.duplicate()); }
		array = base.getColors();
		if (array != null) { morphedVertices.setColors((VertexArray) array.duplicate()); }

		for (int unit = 0; unit < Graphics3D.NUM_TEXTURE_UNITS; unit++)
		{
			array = base.getTexCoords(unit, scaleBias);
			if (array != null)
			{
				float[] bias = new float[array.getComponentCount()];
				System.arraycopy(scaleBias, 1, bias, 0, bias.length);
				morphedVertices.setTexCoords(unit, (VertexArray) array.duplicate(), scaleBias[0], bias);
			}
		}
	}

	private void validateMorphStructure(VertexBuffer base)
	{
		validateArrayFamily(base.getPositions(null), 0, 0);
		validateArrayFamily(base.getNormals(), 1, 0);
		validateArrayFamily(base.getColors(), 2, 0);
		for (int unit = 0; unit < Graphics3D.NUM_TEXTURE_UNITS; unit++)
			{ validateArrayFamily(base.getTexCoords(unit, null), 3, unit); }
	}

	private void validateArrayFamily(VertexArray baseArray, int attribute, int unit)
	{
		VertexArray first = targetArray(targets[0], attribute, unit);
		for (int i = 1; i < targets.length; i++)
		{
			VertexArray other = targetArray(targets[i], attribute, unit);
			if ((first == null) != (other == null) ||
				(first != null && !sameLayout(first, other)))
			{
				// JSR-184 states that all targets have identical array sets and layouts.
				throw new IllegalStateException("Morph targets have incompatible vertex arrays");
			}
		}
		if (first != null && (baseArray == null || !sameLayout(first, baseArray)))
		{
			// JSR-184 states that the base VertexBuffer is a superset of every target.
			throw new IllegalStateException("Morph target array is incompatible with base");
		}
	}

	private static boolean sameLayout(VertexArray a, VertexArray b)
	{
		return b != null && a.getVertexCount() == b.getVertexCount() &&
			a.getComponentCount() == b.getComponentCount() &&
			a.getComponentType() == b.getComponentType();
	}

	private static VertexArray targetArray(VertexBuffer target, int attribute, int unit)
	{
		switch (attribute)
		{
			case 0: return target.getPositions(null);
			case 1: return target.getNormals();
			case 2: return target.getColors();
			default: return target.getTexCoords(unit, null);
		}
	}

	private void morphDefaultColor(VertexBuffer base)
	{
		final int baseColor = base.getDefaultColor();
		int result = 0;
		for (int shift = 0; shift <= 24; shift += 8)
		{
			final int baseComponent = (baseColor >>> shift) & 0xFF;
			float value = baseComponent;
			for (int i = 0; i < targets.length; i++)
			{
				final int targetComponent = (targets[i].getDefaultColor() >>> shift) & 0xFF;
				value += weights[i] * (targetComponent - baseComponent);
			}
			result |= (M3GMath.max(0, M3GMath.min(255, M3GMath.round(value))) << shift);
		}
		morphedVertices.setDefaultColor(result);
	}

	private void morphArray(VertexArray baseArray, VertexArray output, int attribute,
		int unit, boolean unsigned)
	{
		VertexArray firstTarget = targetArray(targets[0], attribute, unit);
		if (firstTarget == null || baseArray == null || output == null) { return; }

		final int vertices = baseArray.getVertexCount();
		final int elements = vertices * baseArray.getComponentCount();
		if (baseArray.getComponentType() == 1)
		{
			byte[] baseValues = new byte[elements];
			byte[][] targetValues = new byte[targets.length][elements];
			byte[] result = new byte[elements];
			baseArray.get(0, vertices, baseValues);
			for (int i = 0; i < targets.length; i++)
				{ targetArray(targets[i], attribute, unit).get(0, vertices, targetValues[i]); }

			for (int component = 0; component < elements; component++)
			{
				final int baseValue = unsigned ? Byte.toUnsignedInt(baseValues[component]) : baseValues[component];
				float value = baseValue;
				for (int i = 0; i < targets.length; i++)
				{
					final int targetValue = unsigned ? Byte.toUnsignedInt(targetValues[i][component]) : targetValues[i][component];
					value += weights[i] * (targetValue - baseValue);
				}
				int rounded = M3GMath.round(value);
				if (unsigned) { rounded = M3GMath.max(0, M3GMath.min(255, rounded)); }
				else { rounded = M3GMath.max(-128, M3GMath.min(127, rounded)); }
				result[component] = (byte) rounded;
			}
			output.set(0, vertices, result);
		}
		else
		{
			short[] baseValues = new short[elements];
			short[][] targetValues = new short[targets.length][elements];
			short[] result = new short[elements];
			baseArray.get(0, vertices, baseValues);
			for (int i = 0; i < targets.length; i++)
				{ targetArray(targets[i], attribute, unit).get(0, vertices, targetValues[i]); }

			for (int component = 0; component < elements; component++)
			{
				final int baseValue = baseValues[component];
				float value = baseValue;
				for (int i = 0; i < targets.length; i++)
					{ value += weights[i] * (targetValues[i][component] - baseValue); }
				result[component] = (short) M3GMath.max(-32768,
					M3GMath.min(32767, M3GMath.round(value)));
			}
			output.set(0, vertices, result);
		}
	}

	@Override
	public void updateProperty(int property, float[] value)
	{
		Mobile.log(Mobile.LOG_DEBUG, Graphics3D.class.getPackage().getName() + "." + Graphics3D.class.getSimpleName() + ": " + "AnimTrack updating MorphingMesh property");
		switch (property)
		{
			case AnimationTrack.MORPH_WEIGHTS:
				int count = Math.min(targets.length, value.length);
				for (int i = 0; i < count; i++) { weights[i] = value[i]; }
				for (int i = count; i < targets.length; i++) { weights[i] = 0.0f; }
				this.dirtyBits[1] = true;
				break;
			default:
				super.updateProperty(property, value);
		}
	}

	boolean animTrackCompatible(AnimationTrack track)
	{
		switch (track.getTargetProperty())
		{
			case AnimationTrack.MORPH_WEIGHTS:
				return true;
			default:
				return super.animTrackCompatible(track);
		}
	}
}
