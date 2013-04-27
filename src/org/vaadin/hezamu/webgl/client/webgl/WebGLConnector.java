package org.vaadin.hezamu.webgl.client.webgl;

import static gwt.g3d.client.math.MatrixStack.MODELVIEW;
import static gwt.g3d.client.math.MatrixStack.PROJECTION;
import gwt.g3d.client.gl2.GL2;
import gwt.g3d.client.gl2.GL2ContextHelper;
import gwt.g3d.client.gl2.GLDisposable;
import gwt.g3d.client.gl2.WebGLBuffer;
import gwt.g3d.client.gl2.WebGLContextAttributes;
import gwt.g3d.client.gl2.array.Float32Array;
import gwt.g3d.client.gl2.array.Uint16Array;
import gwt.g3d.client.gl2.enums.BeginMode;
import gwt.g3d.client.gl2.enums.BufferTarget;
import gwt.g3d.client.gl2.enums.BufferUsage;
import gwt.g3d.client.gl2.enums.ClearBufferMask;
import gwt.g3d.client.gl2.enums.DataType;
import gwt.g3d.client.gl2.enums.DepthFunction;
import gwt.g3d.client.gl2.enums.DrawElementsType;
import gwt.g3d.client.gl2.enums.EnableCap;
import gwt.g3d.client.shader.AbstractShader;
import gwt.g3d.client.shader.ShaderException;

import org.vaadin.hezamu.webgl.WebGL;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.MouseEventDetailsBuilder;
import com.vaadin.client.communication.RpcProxy;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.client.ui.SimpleManagedLayout;
import com.vaadin.shared.MouseEventDetails;
import com.vaadin.shared.ui.Connect;

@SuppressWarnings("serial")
@Connect(WebGL.class)
public class WebGLConnector extends AbstractComponentConnector implements
		GLDisposable, SimpleManagedLayout {

	WebGLServerRpc rpc = RpcProxy.create(WebGLServerRpc.class, this);

	private GL2 gl;
	private AbstractShader shader;
	private int vertexPositionAttribute, vertexColorAttribute;
	private WebGLBuffer[] vertices;
	private WebGLBuffer[] colors;
	private WebGLBuffer[] indices;
	private long time;

	public WebGLConnector() {
		getWidget().addMouseMoveHandler(new MouseMoveHandler() {
			@Override
			public void onMouseMove(MouseMoveEvent event) {
				final MouseEventDetails md = MouseEventDetailsBuilder
						.buildMouseEventDetails(event.getNativeEvent(),
								getWidget().getElement());

				if (getState().trackMouse
						&& System.currentTimeMillis() - time > 33) {
					rpc.mouseMoved(md);
					time = System.currentTimeMillis();
				}
			}
		});
	}

	@Override
	protected Widget createWidget() {
		return Canvas.createIfSupported();
	}

	@Override
	public Canvas getWidget() {
		return (Canvas) super.getWidget();
	}

	@Override
	public WebGLState getState() {
		return (WebGLState) super.getState();
	}

	@Override
	public void onStateChanged(StateChangeEvent sce) {
		super.onStateChanged(sce);

		if (sce.hasPropertyChanged("vertexShaderSource")
				|| sce.hasPropertyChanged("fragmentShaderSource")
				|| sce.hasPropertyChanged("clearColor")
				|| sce.hasPropertyChanged("viewPort")
				|| sce.hasPropertyChanged("translation")
				|| sce.hasPropertyChanged("fov")
				|| sce.hasPropertyChanged("minDist")
				|| sce.hasPropertyChanged("maxDist")
				|| sce.hasPropertyChanged("width")
				|| sce.hasPropertyChanged("height")) {

			dispose(); // Free previous buffers and shaders, if any

			// Due to some strange timing problem we can't do this immediately
			new Timer() {
				@Override
				public void run() {
					if (initWebGLContext()) {
						drawShapes();
					} else {
						Window.alert("WebGL initialization failed.");
					}
				}
			}.schedule(0);
		} else {
			drawShapes();
		}
	}

	/**
	 * Create and initialize the WebGL context.
	 */
	public final boolean initWebGLContext() {
		if (gl == null) {
			gl = GL2ContextHelper.getGL2(getWidget(),
					new WebGLContextAttributes() {
						{
							setStencilEnable(true);
						}
					});

			if (gl == null) {
				return false;
			}
		}

		float[] cc = getState().clearColor;
		gl.clearColor(cc[0], cc[1], cc[2], cc[3]);
		gl.clearDepth(1);

		gl.enable(EnableCap.DEPTH_TEST);
		gl.depthFunc(DepthFunction.LEQUAL);
		gl.clear(ClearBufferMask.COLOR_BUFFER_BIT,
				ClearBufferMask.DEPTH_BUFFER_BIT);

		try {
			shader = new AbstractShader() {
				@Override
				protected void initImpl() throws ShaderException {
					initProgram(getState().vertexShaderSource,
							getState().fragmentShaderSource);
				}
			};

			shader.init(gl);
			shader.bind();
		} catch (ShaderException e) {
			Window.alert(e.getMessage());
			return false;
		}

		vertexPositionAttribute = shader
				.getAttributeLocation("aVertexPosition");
		gl.enableVertexAttribArray(vertexPositionAttribute);

		vertexColorAttribute = shader.getAttributeLocation("aVertexColor");
		gl.enableVertexAttribArray(vertexColorAttribute);

		PROJECTION.pushIdentity();
		PROJECTION.perspective(getState().fov, 1, getState().minDist,
				getState().maxDist);
		gl.uniformMatrix(shader.getUniformLocation("uPMatrix"),
				PROJECTION.get());
		PROJECTION.pop();

		buildBuffers(getState().shapes);

		return true;
	}

	/**
	 * Initialize the WebGL buffers for our shapes.
	 */
	private void buildBuffers(Shape[] shapes) {
		if (shapes != null) {
			vertices = new WebGLBuffer[shapes.length];
			colors = new WebGLBuffer[shapes.length];
			indices = new WebGLBuffer[shapes.length];

			for (int i = 0; i < shapes.length; ++i) {
				vertices[i] = gl.createBuffer();
				gl.bindBuffer(BufferTarget.ARRAY_BUFFER, vertices[i]);
				gl.bufferData(BufferTarget.ARRAY_BUFFER,
						Float32Array.create(shapes[i].vertices),
						BufferUsage.STATIC_DRAW);
				gl.vertexAttribPointer(vertexPositionAttribute, 3,
						DataType.FLOAT, false, 0, 0);

				colors[i] = gl.createBuffer();
				gl.bindBuffer(BufferTarget.ARRAY_BUFFER, colors[i]);
				gl.bufferData(BufferTarget.ARRAY_BUFFER,
						Float32Array.create(shapes[i].colors),
						BufferUsage.STATIC_DRAW);
				gl.vertexAttribPointer(vertexColorAttribute, 4, DataType.FLOAT,
						false, 0, 0);

				if (shapes[i].indexes != null) {
					indices[i] = gl.createBuffer();
					gl.bindBuffer(BufferTarget.ELEMENT_ARRAY_BUFFER, indices[i]);
					gl.bufferData(BufferTarget.ELEMENT_ARRAY_BUFFER,
							Uint16Array.create(shapes[i].indexes),
							BufferUsage.STATIC_DRAW);
				}
			}
		} else {
			Window.alert("No shapes found");
		}
	}

	public void drawShapes() {
		gl.clear(ClearBufferMask.COLOR_BUFFER_BIT,
				ClearBufferMask.DEPTH_BUFFER_BIT);

		MODELVIEW.push();
		float[] gt = getState().translation;
		if (gt != null) {
			MODELVIEW.translate(gt[0], gt[1], gt[2]);
		}

		for (int i = 0; i < vertices.length; ++i) {
			WebGLState s = getState();

			MODELVIEW.push();
			MODELVIEW.translate(s.shapes[i].tx, s.shapes[i].ty, s.shapes[i].tz);

			MODELVIEW.rotateX(s.shapes[i].rx);
			MODELVIEW.rotateY(s.shapes[i].ry);
			MODELVIEW.rotateZ(s.shapes[i].rz);

			setMatrixUniforms();
			MODELVIEW.pop();

			gl.bindBuffer(BufferTarget.ARRAY_BUFFER, vertices[i]);
			gl.vertexAttribPointer(vertexPositionAttribute, 3, DataType.FLOAT,
					false, 0, 0);

			gl.bindBuffer(BufferTarget.ARRAY_BUFFER, colors[i]);
			gl.vertexAttribPointer(vertexColorAttribute, 4, DataType.FLOAT,
					false, 0, 0);

			if (indices[i] != null) {
				gl.bindBuffer(BufferTarget.ELEMENT_ARRAY_BUFFER, indices[i]);
				gl.drawElements(BeginMode.TRIANGLES, 36,
						DrawElementsType.UNSIGNED_SHORT, 0);
			} else {
				gl.drawArrays(BeginMode.TRIANGLE_STRIP, 0, 3);
			}
		}

		MODELVIEW.pop();
	}

	private void setMatrixUniforms() {
		gl.uniformMatrix(shader.getUniformLocation("uMVMatrix"),
				MODELVIEW.get());
	}

	@Override
	public void dispose() {
		if (shader != null) {
			shader.dispose();
			for (WebGLBuffer b : vertices) {
				gl.deleteBuffer(b);
			}
			for (WebGLBuffer b : colors) {
				gl.deleteBuffer(b);
			}
			for (WebGLBuffer b : indices) {
				if (b != null) {
					gl.deleteBuffer(b);
				}
			}
		}
	}

	@Override
	public void layout() {
		int newHt = getWidget().getElement().getOffsetHeight();
		if (newHt != getWidget().getCoordinateSpaceHeight()) {
			getWidget().setCoordinateSpaceHeight(newHt);
		}

		int newWt = getWidget().getElement().getOffsetWidth();
		if (newWt != getWidget().getCoordinateSpaceWidth()) {
			getWidget().setCoordinateSpaceWidth(newWt);
		}
	}
}
