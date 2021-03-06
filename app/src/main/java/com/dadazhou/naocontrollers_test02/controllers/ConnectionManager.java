package com.dadazhou.naocontrollers_test02.controllers;

import java.util.HashMap;

import org.jivesoftware.smack.packet.Presence;
import com.dadazhou.naocontrollers_test02.interfaces.INaoConnectionEvent;
import org.xmlrpc.android.XMLRPCException;

import android.os.Message;
import android.os.Handler.Callback;
import android.util.Log;

import com.naoqi.remotecomm.ALBroker;
import com.naoqi.remotecomm.ALProxy;
import com.naoqi.remotecomm.ALBroker.MethodCallListener;

public class ConnectionManager implements Callback {
	ALBroker fBroker = null;
	private INaoConnectionEvent m_event = null;
	
	//FIXME: wtf is this
	public static final int ID_UPDATE_STATUS = 101;
	
	public final String[] almodules = {
			"ALTextToSpeech",
			"ALAudioDevice",
			"ALMotion",
			"ALLeds",
			"ALLogger",
			"ALRobotPose",
			"ALBehaviorManager",
			"ALSubscribe",
			"ALMemory",
			"AWPreferences"
	};

	private final HashMap<String, ALProxy> proxies = new HashMap<String, ALProxy>();

	public enum connexion_state {
		STATE_OFFLINE,
		STATE_CONNECTING,
		STATE_CONNECTED,
		STATE_PRESENCE,
	};

	public connexion_state state = connexion_state.STATE_OFFLINE;

	private static ConnectionManager instance = null;

	private ConnectionManager(INaoConnectionEvent event) {
		this.m_event = event;
	}

	public final synchronized static ConnectionManager getInstance(INaoConnectionEvent event) {
        if (instance == null)
            instance = new ConnectionManager(event);
        return instance;
    }
	
	public void connect(String robotname, String password) {
		if (this.state == this.state.STATE_PRESENCE)
			return;
		connexion_event(connexion_state.STATE_CONNECTING);
		init_proxies(robotname, password);
	}
	
	private void init_proxies(String robotname, String password) {
		fBroker = new ALBroker(robotname, password, this);
		ALProxy proxy = null;

		for (String module_name : almodules) {
		try {
			proxy = new ALProxy(module_name, fBroker);
			proxies.put(module_name, proxy);
		}
		catch (XMLRPCException e) {
			e.printStackTrace();
			}
		}
		proxy = proxies.get("AWPreferences");
		if (null != proxy)
			proxy.setDestinationJabberId( "_preferences@xmpp.aldebaran-robotics.com" );
	}

	/**
	 * @return Return a proxy for a given module name
	 */
	public ALProxy getProxy(String module_name) {
		return proxies.get(module_name);
	}

	public void connexion_postCall( ALProxy proxy, String method, Object ... params ){

	    	if (fBroker==null) return;
	    	
		    try {
		    	proxy.postCall(method, params);
			} catch (Exception e) {
				connexion_exception(e);
			}
	    }

	public void connexion_asyncCall( long timeoutMillis, final ALProxy.MethodResponseListener listener, ALProxy proxy, final String method, Object ... params  ){
	    	if (fBroker==null)
	    		return;
	    	try {
	    		proxy.asyncCall(timeoutMillis, listener, method, params);
	    		} catch (XMLRPCException e) {
	    			connexion_exception(e);
	    	}
	    }

	 public void connexion_exception(Exception e){
			/// disconnect in case of problem
			connexion_event(connexion_state.STATE_OFFLINE);
		}

	public void connexion_event(connexion_state state) {
		if (this.state == state)
			return;
	   	Log.e("connexion event", String.format("state: %s event: %s", state2string(this.state), state2string(state)));
	    	// state machine
	    switch(state){
	    	case STATE_OFFLINE:
	    		state = connexion_state.STATE_OFFLINE;
	    		this.m_event.onErrorConnection();
	    		if (fBroker !=null) {
	    			fBroker.disconnect();
	    			fBroker = null;
	    		}
	    		proxies.clear();
	    		break;
	    	case STATE_CONNECTING:
	    		this.state = connexion_state.STATE_CONNECTING;
	    		break;
	    	case STATE_CONNECTED:
	    		this.state = connexion_state.STATE_CONNECTED;
	    		break;
	    	case STATE_PRESENCE:
	    		this.state = connexion_state.STATE_PRESENCE;
	    		this.m_event.onConnected();
	    		break;
    	}
	}

	/*
	 * to string method for connexion_state
	 * @ return a string for a connexion_state
	 */
	public String state2string(connexion_state state) {
		switch (state) {
		case STATE_OFFLINE:
			return "STATE_OFFLINE";
		case STATE_CONNECTING:
			return "STATE_CONNECTING";
		case STATE_CONNECTED:
			return "STATE_CONNECTED";
		case STATE_PRESENCE:
			return "STATE_PRESENCE";
		}
		return "UNKNOWN_STATE";
	}
	
	public void registerListener(String method, MethodCallListener listener) {
		fBroker.registerListener( "ImageViewer.fromData", listener);
	}
	
	@Override
	public boolean handleMessage(Message msg) {
		try {	
	    	if ( msg.what >= ALBroker.ID_CONNECT_ERROR
	    			&& msg.what <= ALBroker.ID_TIMEOUT )
	    		Log.i( "NAO", String.format("handleMessage %s %s", ALBroker.msg_name[msg.what], msg.obj));
	    	else
	    		Log.i( "NAO", String.format("handleMessage %d %s", msg.what, msg.obj));
	      
	    	switch( msg.what ){
	    		case ID_UPDATE_STATUS:
	    			return true;
	    		case ALBroker.ID_DISCONNECTED:
	    		case ALBroker.ID_CONNECT_ERROR:
	    			connexion_event(connexion_state.STATE_OFFLINE);
	    			return true;	        
	    			
	    		case ALBroker.ID_CONNECTED:
	    			//FIXME: presence ?
	    			connexion_event(connexion_state.STATE_CONNECTED);   
	    			return true;

	    		case ALBroker.ID_PRESENCE:
	    			if (fBroker==null)
	    				return true;
	    			Presence presence = (Presence)msg.obj;
	    			String from = presence.getFrom();
	    			String ressource = fBroker.extractRessource(from);
	    			if (presence.isAvailable() && ressource!= null && ressource.startsWith("nao")){
	    				connexion_event(connexion_state.STATE_PRESENCE);
	    			}	    				
			    	return true;
	    	}
	    	
	    	} catch (Exception e) {
	    		connexion_event(connexion_state.STATE_OFFLINE);
    			return true;
			}	      
		return false;
	}
}
