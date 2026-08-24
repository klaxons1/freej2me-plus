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

public class World extends Group
{

	private Camera camera;
	private Background background;

	public World() { }

	@Override
	protected Object3D duplicateImpl()
	{
		World copy = (World) super.duplicateImpl();
		copy.camera = null;
		copy.background = null;

		for (int i = 0; i < copy.getChildCount(); i++)
		{
			Node child = copy.getChild(i);
			if (child instanceof Camera && copy.camera == null) {
				copy.camera = (Camera) child;
				copy.addReference(copy.camera);
			} else if (child instanceof Background && copy.background == null) {
				copy.background = (Background) child;
				copy.addReference(copy.background);
			}
		}
		return copy;
	}

	public Camera getActiveCamera() { return camera; }

	public void setActiveCamera(Camera camera)
	{
		removeReference(this.camera);
		this.camera = camera;
		addReference(this.camera);
	}

	public Background getBackground() { return background; }

	public void setBackground(Background background)
	{
		removeReference(this.background);
		this.background = background;
		addReference(this.background);
	}

}
