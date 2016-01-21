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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.xmlrpc.android.XMLRPCCommon;
import org.xmlrpc.android.XMLRPCException;

/**
 * Partial implementation of the ALProxy in this android world....
 * 
 * @author bilock@google.com (JŽr™me Monceaux)
 *
 */
public class ALProxy extends XMLRPCCommon {
	
	private String fModuleName;
	private String fTo;
	private ALBroker fBroker;
	private HashMap<String, MethodResponseListener> fListenerMap;
	private List<String> fWaitingUuid;

	
	/**
	 * ALProxy constructor. Serialize command using xmlrpc 
	 * @param Name of the NAOqi module
	 * @throws XMLRPCException 
	 */
	public ALProxy(String moduleName, ALBroker broker) throws XMLRPCException {
		fModuleName = moduleName;
		fBroker = broker;
		fListenerMap = new HashMap<String, MethodResponseListener>();
		fWaitingUuid = new ArrayList<String>();
		fTo = "";
		// register proxy
		fBroker.registerProxy(this);
		
		// get method list 
		
		// since getMethodHelp is returning an non empty return value description/
		// then we can not use to separate void return than non-void return methods
		/*
		Object[] methodList = (Object[])call("getMethodList");
		Object[][] methodHelp = (Object[][])call("getMethodHelp", methodList[0].toString());
		if( (methodHelp[0][3] == "")||(methodHelp[0][3] == "") )
			return;
		// Create a table with void method
		*/
	}

	public void setDestinationJabberId( String to)
	{
		fTo = to;
	}
	
	public boolean isWaitingForUID(String uuid)
	{
		return fWaitingUuid.contains(uuid);
	}
	
	public MethodResponseListener hasListenerForUID(String uuid)
	{
		MethodResponseListener listener = null;
		synchronized(fListenerMap) {
			listener = fListenerMap.get(uuid);
			fListenerMap.remove(uuid);
		}
		return listener;
	}
	

	public void postCall(String method, Object ... params) throws XMLRPCException {
		try {
			
			// prepare POST body
			String id = UUID.randomUUID().toString();
						
			fBroker.sendXmppCall(id, fModuleName + ".post." + method, params, fTo);
		} catch (XMLRPCException e) {
			// catch & propagate XMLRPCException/XMLRPCFault
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			// wrap any other Exception(s) around XMLRPCException
			throw new XMLRPCException(e);
		}	
	}
	

	public void asyncCall(MethodResponseListener listener, String method, Object ... params) throws XMLRPCException {
		asyncCall(0, listener, method, params);
	}

	public void asyncCall(long timeoutMillis, MethodResponseListener listener, String method, Object ... params) throws XMLRPCException {
		try {
			
			// prepare POST body
			String id = UUID.randomUUID().toString();
			
			synchronized(fListenerMap) {
				fListenerMap.put(id, listener);
			}
			
			fBroker.sendXmppCall(timeoutMillis, id ,  fModuleName + "." + method, params, fTo);
			
		} catch (XMLRPCException e) {
			// catch & propagate XMLRPCException/XMLRPCFault
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			// wrap any other Exception(s) around XMLRPCException
			throw new XMLRPCException(e);
		}
	}
	
	// signal timeout to listener
	public void timeout()
	{
		for ( MethodResponseListener listener : fListenerMap.values() ){
			listener.onResponse(null);		
		}
		fListenerMap.clear();
	}
	
	
	public interface MethodResponseListener {
		
		public void onResponse( Object result ); 
	}
	
	
}
