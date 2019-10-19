/* Copyright (C) 2019  Matteo Hausner
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License along
* with this program; if not, write to the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.AxisValueEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.LongPressEditorBuilder;

public final class ButtonToRelativeAxisReset extends ToAxisAction<Byte> implements IButtonToAction {

	private static final float DEFAULT_RESET_VALUE = 0f;

	@ActionProperty(label = "RESET_VALUE", editorBuilder = AxisValueEditorBuilder.class)
	private float resetValue = DEFAULT_RESET_VALUE;

	@ActionProperty(label = "LONG_PRESS", editorBuilder = LongPressEditorBuilder.class)
	private boolean longPress = DEFAULT_LONG_PRESS;

	@Override
	public void doAction(final Input input, Byte value) {
		value = handleLongPress(input, value);

		if (value != 0 ^ invert)
			input.setAxis(virtualAxis, resetValue, false, null);
	}

	public float getResetValue() {
		return resetValue;
	}

	@Override
	public boolean isLongPress() {
		return longPress;
	}

	@Override
	public void setLongPress(final boolean longPress) {
		this.longPress = longPress;
	}

	public void setResetValue(final float resetValue) {
		this.resetValue = resetValue;
	}

	@Override
	public String toString() {
		return rb.getString("BUTTON_TO_RELATIVE_AXIS_RESET_ACTION");
	}

}
