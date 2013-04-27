package org.vaadin.hezamu.webgl;

import org.vaadin.hezamu.webgl.WebGL.MouseMoveListener;
import org.vaadin.hezamu.webgl.client.webgl.Shape;

import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.MouseEventDetails;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

/**
 * A simple application that demonstrates the capabilities of the Vaadin WebGL
 * proxy widget.
 * 
 * @author henri@vaadin.com
 */
@SuppressWarnings("serial")
public class WebGLUI extends UI {

	private WebGL webgl;
	private TextArea vs, fs;
	private CheckBox trackMouse;
	private TextField clearColor, translation, fov, minDist, maxDist;
	private long time;

	private final ValueChangeListener updater = new ValueChangeListener() {
		@Override
		public void valueChange(com.vaadin.data.Property.ValueChangeEvent event) {
			updateState();
		}
	};

	@Override
	protected void init(VaadinRequest request) {
		setContent(new HorizontalLayout() {
			{
				setMargin(true);
				addComponent(webgl = new WebGL());
				webgl.setWidth("500px");
				webgl.setHeight("500px");
				setWidth("100%");
				addComponent(new VerticalLayout() {
					{
						setWidth("100%");
						addComponent(new HorizontalLayout() {
							{
								setSpacing(true);
								setWidth("100%");
								addComponent(vs = new TextArea() {
									{
										setCaption("Vertex shader");
										setHeight("250px");
										setWidth("100%");
										setImmediate(true);
										setValue("attribute vec3 aVertexPosition;\n"
												+ "attribute vec4 aVertexColor;\n\n"
												+ "uniform mat4 uMVMatrix;\n"
												+ "uniform mat4 uPMatrix;\n\n"
												+ "varying vec4 vColor;\n\n"
												+ "void main(void) {\n"
												+ "  gl_Position = uPMatrix * uMVMatrix * vec4(aVertexPosition, 1.0);\n"
												+ "  vColor = aVertexColor;\n"
												+ "}");
										addValueChangeListener(updater);
									}
								});

								addComponent(fs = new TextArea() {
									{
										setCaption("Fragment shader");
										setHeight("250px");
										setWidth("100%");
										setImmediate(true);
										setValue("#ifdef GL_ES\n"
												+ "precision highp float;\n"
												+ "#endif\n\n"
												+ "varying vec4 vColor;\n\n"
												+ "void main(void) {\n"
												+ "  gl_FragColor = vColor;\n"
												+ "}");
										addValueChangeListener(updater);
									}
								});
							}
						});

						addComponent(clearColor = createTF(
								"Background color (r,g,b,a)", "0, 0, 0, 1"));
						addComponent(translation = createTF(
								"Scene translation (x,y,z)", "0, 0, -8"));
						addComponent(fov = createTF("Field of view (degrees)",
								"45"));
						addComponent(minDist = createTF("Min distance", "0.1"));
						addComponent(maxDist = createTF("Max distance", "100"));

						addComponent(trackMouse = new CheckBox() {
							{
								setCaption("Track mouse");
								addValueChangeListener(updater);
							}
						});
					}

					private TextField createTF(final String caption,
							final String initialValue) {
						return new TextField() {
							{
								setCaption(caption);
								setValue(initialValue);
								setImmediate(true);
								addValueChangeListener(updater);
							}
						};
					}
				});
			}
		});

		createShapes();

		webgl.addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMoved(MouseEventDetails med) {
				System.out.println("Frame len "
						+ (System.currentTimeMillis() - time) + "ms");
				time = System.currentTimeMillis();
				// Triangle rotation follows mostly horizontal movement
				webgl.getState().shapes[0].rx = med.getClientY() / 250f;
				webgl.getState().shapes[0].ry = med.getClientX() / 50f;

				// Box rotation follows mostly vertical movement
				webgl.getState().shapes[1].rx = med.getClientY() / 50f;
				webgl.getState().shapes[1].ry = med.getClientX() / 250f;
			}
		});

		updateState();
	}

	private void createShapes() {
		// First a simple triangle
		Shape triangle = new Shape();
		triangle.vertices = new float[] { 0, 1.2f, 0, -1.2f, -1.2f, 0, 1.2f,
				-1.2f, 0 };

		// Primary colors at each corner
		triangle.colors = new float[] { 1, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1 };

		triangle.tx = -1.5f; // Translate it a little to the left

		// Then a more complex shape: a 3D box.
		Shape box = new Shape();
		box.vertices = new float[] { -1, -1, 1, 1, -1, 1, 1, 1, 1, -1, 1, 1, // Front
																				// face
				-1, -1, -1, -1, 1, -1, 1, 1, -1, 1, -1, -1, // Back face
				-1, 1, -1, -1, 1, 1, 1, 1, 1, 1, 1, -1, // Top face
				-1, -1, -1, 1, -1, -1, 1, -1, 1, -1, -1, 1, // Bottom face
				1, -1, -1, 1, 1, -1, 1, 1, 1, 1, -1, 1, // Right face
				-1, -1, -1, -1, -1, 1, -1, 1, 1, -1, 1, -1 // Left face
		};

		float[][] faceColors = { { 1, 0, 0, 1 }, // Front face
				{ 1, 1, 0, 1 }, // Back face
				{ 0, 1, 0, 1 }, // Top face
				{ 1, 0.5f, 0.5f, 1 }, // Bottom face
				{ 1, 0, 1, 1 }, // Right face
				{ 0, 0, 1, 1 }, // Left face
		};

		// 4 vertices per face * 4 color components * number of faces.
		box.colors = new float[4 * 4 * faceColors.length];
		int index = 0;
		for (float[] faceColor : faceColors) {
			for (int i = 0; i < 4; i++) {
				for (float v : faceColor) {
					box.colors[index++] = v;
				}
			}
		}

		// The box is built of 12 triangles, two for each face. Here we specify
		// how the vertices are mapped to the triangles
		box.indexes = new int[] { 0, 1, 2, 0, 2, 3, // Front face
				4, 5, 6, 4, 6, 7, // Back face
				8, 9, 10, 8, 10, 11, // Top face
				12, 13, 14, 12, 14, 15, // Bottom face
				16, 17, 18, 16, 18, 19, // Right face
				20, 21, 22, 20, 22, 23 // Left face
		};

		box.tx = 1.5f; // Translate the box a little to the right

		webgl.getState().shapes = new Shape[] { triangle, box };
	}

	private void updateState() {
		webgl.getState().vertexShaderSource = vs.getValue();
		webgl.getState().fragmentShaderSource = fs.getValue();
		webgl.getState().trackMouse = trackMouse.getValue();

		webgl.getState().clearColor = parseTf(clearColor);
		webgl.getState().translation = parseTf(translation);
		webgl.getState().fov = parseTf(fov)[0];
		webgl.getState().minDist = parseTf(minDist)[0];
		webgl.getState().maxDist = parseTf(maxDist)[0];
	}

	// Convenience method to build the text fields
	private float[] parseTf(TextField tf) {
		String[] comps = tf.getValue().split(",");
		float[] result = new float[comps.length];

		for (int i = 0; i < comps.length; ++i) {
			result[i] = Float.parseFloat(comps[i]);
		}
		return result;
	}
}