package org.vaadin.hezamu.webgl.client.webgl;

@SuppressWarnings("serial")
public class WebGLState extends com.vaadin.shared.AbstractComponentState {
	public String vertexShaderSource;
	public String fragmentShaderSource;
	public float[] clearColor;
	public float[] viewPort;
	public float[] translation;
	public Shape[] shapes;
	public int mouseTrackFPS;
	public float fov;
	public float minDist;
	public float maxDist;
}