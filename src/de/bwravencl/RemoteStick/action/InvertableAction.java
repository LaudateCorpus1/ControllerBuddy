package de.bwravencl.RemoteStick.action;

public abstract class InvertableAction {

	protected boolean invert = false;

	public boolean isInvert() {
		return invert;
	}

	public void setInvert(boolean invert) {
		this.invert = invert;
	}
	
}
