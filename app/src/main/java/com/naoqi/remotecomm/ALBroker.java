/*
 * Copyright 2011 Aldebaran-robotics. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY <ALDEBARAN-ROBOTICS> ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL <ALDEBARAN-ROBOTICS> OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of Aldebaran-robotics.
 */
package com.naoqi.remotecomm;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.packet.Presence;
import org.xmlrpc.android.XMLRPCException;

import android.os.Handler;
import android.os.Message;
import android.util.Log;


/**
 * Partial implementation of the ALBroker in this android world....
 * 
 * @author bilock@google.com (JŽr™me Monceaux)
 *
 */
public class ALBroker implements AbstractCommChannel.CommMessageListener, Handler.Callback {

	  private XmppCommChannel fChannel;
	  
	  private ALXmppSerializer fXmppSerializer;
	  
	  // List of declared proxies
	  List<ALProxy> fProxies;
	  private HashMap<String, MethodCallListener> fListenerMap; // method => listener

	  
	  /// Handler for asynchronous message
	  public static final int ID_CONNECT_ERROR = 0,
	  						  ID_CONNECTED = 1,
	  						  ID_DISCONNECTED = 2,
	  						  ID_MESSAGE = 3,
	  						  ID_PRESENCE = 4,
	  						  ID_TIMEOUT = 5;
	  
	  public static final String msg_name[] = { 
		  "ID_CONNECT_ERROR", 
		  "ID_CONNECTED", 
		  "ID_DISCONNECTED", 
		  "ID_MESSAGE",
		  "ID_PRESENCE",
		  "ID_TIMEOUT",
	  };
		
		
	  private Handler handler ;
	  private Handler handler_listener ;
	  
	  private String fRobotName = "";
	  private String fRobotId = "";
	  private String fRobotRessource = "nao";
    public boolean fRobotAvailable = false;
	  
	  
	  public ALBroker( String robotname, String pwd, Handler.Callback callback  ) {
	
		  fProxies = new ArrayList<ALProxy>();
		  fXmppSerializer = new ALXmppSerializer();
		  fListenerMap = new HashMap<String, MethodCallListener>();
		    
	    //this.handler = handler;
		  this.handler = new Handler(callback);
		  this.handler_listener = new Handler(this);
	    
	    // transform password to sha1 which is the xmpp server password
	    MessageDigest sha1 = null;
	    try{
	    	sha1 = MessageDigest.getInstance("SHA-1");
	    }catch(NoSuchAlgorithmException ex)
	    {}
	    
	    sha1.update(pwd.getBytes());

	    byte[] digest = sha1.digest();
	    
	    
	    StringBuffer pwdBuf = new StringBuffer();

	    for(int i = 0; i < digest.length; i++) {
	    	pwdBuf.append(String.format("%02x", 0xFF & digest[i]));
	    }

	    fRobotId = robotname + "@xmpp.aldebaran-robotics.com";
	    fRobotName = robotname;
	    Log.i("Nao", 
	    		String.format("connecting with %s %s",fRobotId, pwdBuf.toString()));
	    
	    fChannel = new XmppCommChannel(
	    	fRobotId, pwdBuf.toString(), fRobotName, this, "xmpp");

	 
	    fChannel.connect();
	  }
	  

	  public void registerProxy(ALProxy proxy)
	  {
		  fProxies.add(proxy);
	  }
	  


	  // CommMessageListener interface
	    
		@Override
		public void onConnectError(String channelName, int channel) {
			// TODO Auto-generated method stub
			Log.i("ALBroker", "onConnectError " + channelName);
		
			Message.obtain(handler, ID_CONNECT_ERROR)  			   
					.sendToTarget();
		}
		


		@Override
		public void onConnected(String channelName, int channel) {
			// TODO Auto-generated method stub
			Log.i("Nao", "onConnected " + channelName);
			
			Message.obtain(handler, ID_CONNECTED)  			   
					.sendToTarget();
			
			
			// check presence
			if (fChannel==null) return;
			Roster roster = fChannel.getRoster();
		
			roster.addRosterListener(new RosterListener() {
			    public void entriesAdded(Collection<String> addresses) {}
			    public void entriesDeleted(Collection<String> addresses) {}
			    public void entriesUpdated(Collection<String> addresses) {}
			    public void presenceChanged(Presence presence) {
			    	
			    	String from = presence.getFrom();
			    	Log.i("XmppCommChannel", "Presence changed: " + from + " " + presence);
			    	
			    	if ( from.startsWith(fRobotId+"/nao")){
			    		fRobotRessource  = from.substring(fRobotId.length()+1);
			    		Log.i("XmppCommChannel", "using ressource : " + fRobotRessource);
			    		fRobotAvailable = presence.isAvailable();
			    	}
			    	
			    	Message.obtain(handler, ID_PRESENCE, presence)  			   
						.sendToTarget();
			    }
			});
		}



		@Override
		public void onDisconnected(String channelName, int channel) {
			// TODO Auto-generated method stub
			Log.i("ALBroker", "onDisconnected " + channelName);
			
			Message.obtain(handler, ID_DISCONNECTED)  			   
					.sendToTarget();
		}


		public static String limitString(int len, String text){
				if (text.length()<=len)
					return text;
				else
					return text.substring(0, len-4) + " ...";
		}
		    
		
		@Override
		public void onMessage(CommMessage msg) {
			
			Log.i( "ALBroker", String.format("onMessage %s", limitString(40,msg.getMessage())));
		    
			
			try {
				ALMethodCall call = new ALMethodCall();
				
				fXmppSerializer.deserializeResponse(msg.getMessage(), call);

				Message.obtain(handler_listener, ID_MESSAGE, call)  			   
					.sendToTarget();

				
			} catch (XMLRPCException e) {

				e.printStackTrace();
			} catch (Exception e) {

				e.printStackTrace();
			}  
			
		}
		
		public void disconnect() {
			if (fChannel!=null)
				fChannel.disconnect();
			fChannel = null;
		}
		
		
		/// commandes
		
		public void sendXmppCall( String uid, String method, Object[] params, String to ) throws XMLRPCException
		{
			if( to.equals(""))
				to = fRobotId+"/"+fRobotRessource;
			

			ALMethodCall call = new ALMethodCall();
			call.uid = uid ;
			call.methodName = method ;
			call.params = params ;
			Log.i("ALBroker.java","sendXmppCall : "  + call);
			String message = fXmppSerializer.serializeCall(to, call);
			//Log.i("ALBroker.java","sendXmppCall : "  + message);
			//fChannel.sendMessage( message, "text/text");
			fChannel.send( to, message );
			
			
		}
		
		public void sendXmppCall( long timeoutMillis, String uid, String method, Object[] params, String to ) throws XMLRPCException
		{
			sendXmppCall(uid, method, params, to);
			
			// setup timeout
			if (timeoutMillis>0){
				
				handler_listener.sendEmptyMessageDelayed(ID_TIMEOUT,  timeoutMillis);
			}
			
		}
		
		
		// interface Handler.Callback
	  @Override
	  public boolean handleMessage(Message msg) {
    
	  	try {
	  		
  	  	if ( msg.what >= ID_CONNECT_ERROR
  	      		&& msg.what <= ID_TIMEOUT )
  	  		Log.i( "ALBroker", String.format("handleMessage %s", msg_name[msg.what]));
  	  	else
  	  		Log.i( "ALBroker", String.format("handleMessage %d", msg.what));
	  	
	  	
  	  	switch( msg.what ){
  	  		case ID_CONNECT_ERROR:
  	  			break;
	      
  	  		case ID_CONNECTED:
  	  			break;
	      
  	  		case ID_DISCONNECTED:
  	  			break;
	      
  	  		case ID_MESSAGE:
    
  	  			ALMethodCall call = (ALMethodCall)msg.obj;
	  			
  	  			if (call.isResponse){ // response
  	  				handler_listener.removeMessages(ID_TIMEOUT); 
	  				
    					for(ALProxy proxy : fProxies)
    					{
    						ALProxy.MethodResponseListener listener = proxy.hasListenerForUID(call.uid);
    						if (listener!=null){
    							listener.onResponse(call.result);
    						}
    					}
    					
  	  			} else { // call
	  			
  	  				MethodCallListener listener = null;
      				synchronized(fListenerMap) {
      					listener = fListenerMap.get(call.methodName);
      				}
  	  				if (listener!=null)
  	  					listener.onCall(call);
	  				
  	  			}
  	  			break;      
	      
  	  		case ID_TIMEOUT:
	  			
  	  			// send timeout to all listener
  	  			for(ALProxy proxy : fProxies)
  				  {	
  	  				proxy.timeout();
  				  }
	  			
  	  			break;	      
  	  	}
	  	
	  	} catch(Exception e){
	  		e.printStackTrace();
	  	}
	    
	  	return true;
	  }
	  
	  
	  //////////
	  public interface MethodCallListener {
			
			public void onCall( ALMethodCall call ); 
		}
	    
    public void registerListener( String methodName, MethodCallListener listener  ){
    	synchronized(fListenerMap) {
			  fListenerMap.put(methodName, listener);
		  }
    }
	    
    public void unregisterListener( String methodName  ){
    	synchronized(fListenerMap) {
				fListenerMap.remove(methodName);
			}
    }
	    
    ////
    
    public String extractRessource(String from){
    	if (from.startsWith(fRobotId+"/")){
    		return from.substring(fRobotId.length()+1);
    	}
    	return null;
    }
	    
}
