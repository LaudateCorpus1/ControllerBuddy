module de.bwravencl.controllerbuddy {

	exports de.bwravencl.controllerbuddy.gui;

	opens de.bwravencl.controllerbuddy.input to com.google.gson;
	opens de.bwravencl.controllerbuddy.input.action to com.google.gson;

	requires batik.swing;
	requires com.formdev.flatlaf;
	requires com.google.gson;
	requires com.sun.jna;
	requires com.sun.jna.platform;
	requires commons.cli;
	requires io.github.classgraph;
	requires transitive java.desktop;
	requires java.logging;
	requires transitive java.prefs;
	requires jdk.xml.dom;
	requires org.lwjgl;
	requires org.lwjgl.natives;
	requires org.lwjgl.glfw;
	requires org.lwjgl.opengl;
	requires org.lwjgl.openvr;
	requires purejavahidapi;
}
