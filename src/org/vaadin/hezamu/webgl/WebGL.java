package org.vaadin.hezamu.webgl;

import java.util.ArrayList;
import java.util.List;

import org.vaadin.hezamu.webgl.client.webgl.WebGLServerRpc;
import org.vaadin.hezamu.webgl.client.webgl.WebGLState;

import com.vaadin.shared.MouseEventDetails;

/**
 * A Vaadin WebGL proxy widget.
 * 
 * @author henri@vaadin.com
 */
@SuppressWarnings("serial")
public class WebGL extends com.vaadin.ui.AbstractComponent {

	private final List<MouseMoveListener> listeners = new ArrayList<MouseMoveListener>();

	private final WebGLServerRpc rpc = new WebGLServerRpc() {
		@Override
		public void mouseMoved(MouseEventDetails med) {
			fireMouseMoved(med);
		}
	};

	public WebGL() {
		registerRpc(rpc);
	}

	@Override
	public WebGLState getState() {
		return (WebGLState) super.getState();
	}

	public interface MouseMoveListener {
		public void mouseMoved(MouseEventDetails med);
	}

	public void addMouseMoveListener(MouseMoveListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public void removeMouseMoveListener(MouseMoveListener listener) {
		if (listeners.contains(listener)) {
			listeners.remove(listener);
		}
	}

	private void fireMouseMoved(MouseEventDetails med) {
		for (MouseMoveListener listener : listeners) {
			listener.mouseMoved(med);
		}
	}
}
