Vaadin WebGL proxy
==================

A simple demonstration how to drive a WebGL context from the server side 
with Vaadin.

The demo app includes a setting to animate the scene when the cursor moves over
it. Since rendering is completely controlled by the server every change requires
a server round trip. Even though this method works suprisingly well (I get
~30fps on localhost, around 7fps from Jelastic) it's definitely not recommended
to run high framerate updates through the server. It's just a silly hack.