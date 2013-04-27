package org.vaadin.hezamu.webgl.client.webgl;

import com.vaadin.shared.MouseEventDetails;
import com.vaadin.shared.communication.ServerRpc;

public interface WebGLServerRpc extends ServerRpc {
	public void mouseMoved(MouseEventDetails mouseDetails);
}
