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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlrpc.android.XMLRPCCommon;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.Tag;
import org.xmlrpc.android.XMLRPCFault;

import android.util.Log;

/**
 * XMLRPC serialisation
 * 
 * @author bilock@google.com (JŽr™me Monceaux)
 *
 */
public class ALXmppSerializer extends XMLRPCCommon {
	
	/**
	 * ALXmppSerializer constructor. Serialize command using xmlrpc 
	 * @param Name of the NAOqi module
	 */
	public ALXmppSerializer() {
	}

	public String serializeCall(String toJid, ALMethodCall call) throws XMLRPCException
	{
		try 
		{
			// prepare POST body
			//String body = methodCall(call.getMethodName(), call.params);
			
			StringWriter bodyWriter = new StringWriter();
			serializer.setOutput(bodyWriter);
			//serializer.startDocument(null, null);
			serializer.startTag(null, Tag.METHOD_CALL);
			// set method name
			serializer.startTag(null, Tag.METHOD_NAME).text(call.methodName).endTag(null, Tag.METHOD_NAME);
			
			serializeParams(call.params);

			serializer.endTag(null, Tag.METHOD_CALL);
			serializer.endDocument();

			String body = bodyWriter.toString();
			
			String prev = "<iq xmlns=\"jabber:client\" to=\"" + toJid + "\"" 
				+ " id=\"" + call.uid + "\""
				+ " type=\"set\"><query xmlns=\"jabber:iq:rpc\">";
			String post = "</query></iq>";

			return prev+body+post;
		} /*catch (XMLRPCException e) 
		{
			// catch & propagate XMLRPCException/XMLRPCFault
			throw e;
		} */catch (Exception e) 
		{
			e.printStackTrace();
			// wrap any other Exception(s) around XMLRPCException
			throw new XMLRPCException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public String deserializeResponse(String response, ALMethodCall call) throws XMLRPCException
	{
		try{
			//Log.i("ALXmppSerializer", "deserializeResponse " + response);
			Log.i("ALXmppSerializer", "deserializeResponse " + limitString(300,response));
			
			XmlPullParser pullParser = XmlPullParserFactory.newInstance().newPullParser();
			pullParser.setInput(new StringReader ( response ));
	
			
			// lets start pulling...
			pullParser.nextTag();
			pullParser.require(XmlPullParser.START_TAG, null, "iq");
			
			String id = null;
			for (int index= 0; index<pullParser.getAttributeCount(); index++){			
				if (pullParser.getAttributeName(index).equals("id")){
					id = pullParser.getAttributeValue(index);
					break;
				}
			}
			//Log.i("ALXmppSerializer", "id = " + id);
			if (id==null)
				throw new XMLRPCException("no id attribute found in XMLRPC response");
			call.uid = id;
			
			
			
			pullParser.nextTag();
			pullParser.require(XmlPullParser.START_TAG, null, "query");

			pullParser.nextTag();
			//pullParser.require(XmlPullParser.START_TAG, null, Tag.METHOD_RESPONSE);
			pullParser.require(XmlPullParser.START_TAG, null, null);
			
			if (Tag.METHOD_CALL.equals(pullParser.getName())){
				
				call.isResponse = false;
				
				//pullParser.nextTag();
				//pullParser.require(XmlPullParser.START_TAG, null, Tag.METHOD_CALL);
				pullParser.nextTag();
				pullParser.require(XmlPullParser.START_TAG, null, Tag.METHOD_NAME);

				call.methodName = pullParser.nextText();

				pullParser.nextTag();
				pullParser.require(XmlPullParser.START_TAG, null, Tag.PARAMS);
				pullParser.nextTag(); // <param>
				
				List<Object> list = new ArrayList<Object>();
				
				do {
					//Log.d(Tag.LOG, "type=" + pullParser.getEventType() + ", tag=" + pullParser.getName());
					pullParser.require(XmlPullParser.START_TAG, null, Tag.PARAM);
					pullParser.nextTag(); // <value>

					Object param = iXMLRPCSerializer.deserialize(pullParser);
					//Log.d("ALXmppSerializer", "param=" + limitString(40,param.toString()));
					list.add(param); // add to return value

					pullParser.nextTag();
					pullParser.require(XmlPullParser.END_TAG, null, Tag.PARAM);
					pullParser.nextTag(); // <param> or </params>
					
				} while (!pullParser.getName().equals(Tag.PARAMS)); // </params>
				
				/*
				pullParser.require(XmlPullParser.START_TAG, null, Tag.METHOD_CALL);
				
				pullParser.nextTag();
				pullParser.require(XmlPullParser.START_TAG, null, Tag.METHOD_NAME);
				call.methodName = pullParser.nextText();
				/// ==> todo : something here
				///pullParser.nextTag();
				
				pullParser.nextTag();
				pullParser.require(XmlPullParser.START_TAG, null, Tag.PARAMS);
				
		
	
				pullParser.nextTag();
				List<Object> list = new ArrayList<Object>();
				while (pullParser.getName().equals(Tag.PARAM)) {
					list.add(iXMLRPCSerializer.deserialize(pullParser));
					pullParser.nextTag();
				}
				*/
				
				call.params = list.toArray();
				return Tag.METHOD_CALL;
				
			} else { // METHOD_RESPONSE
				
				call.isResponse = true;
				
				pullParser.require(XmlPullParser.START_TAG, null, Tag.METHOD_RESPONSE);
				
				pullParser.nextTag(); // either Tag.PARAMS (<params>) or Tag.FAULT (<fault>)  
				String tag = pullParser.getName();
				if (tag.equals(Tag.PARAMS)) {
					
						
					// normal response
					pullParser.nextTag(); // Tag.PARAM (<param>)
					pullParser.require(XmlPullParser.START_TAG, null, Tag.PARAM);
					pullParser.nextTag(); // Tag.VALUE (<value>)
					// no parser.require() here since its called in XMLRPCSerializer.deserialize() below
					
					// deserialize result
					call.result  = iXMLRPCSerializer.deserialize(pullParser);
					return Tag.METHOD_RESPONSE;
					
				} else if (tag.equals(Tag.FAULT)) {
					// fault response
					pullParser.nextTag(); // Tag.VALUE (<value>)
					// no parser.require() here since its called in XMLRPCSerializer.deserialize() below
		
					// deserialize fault result
					Map<String, Object> map = (Map<String, Object>) iXMLRPCSerializer.deserialize(pullParser);
					String faultString = (String) map.get(Tag.FAULT_STRING);
					int faultCode = (Integer) map.get(Tag.FAULT_CODE);
					throw new XMLRPCFault(faultString, faultCode);
				} else {
					throw new XMLRPCException("Bad tag <" + tag + "> in XMLRPC response - neither <params> nor <fault>");
				}
			}
			
			
			
		} catch (XMLRPCException e) {
			// catch & propagate XMLRPCException/XMLRPCFault
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			// wrap any other Exception(s) around XMLRPCException
			throw new XMLRPCException(e);
		}
	}

	public static void test_deserialize( String message ){
		
		ALXmppSerializer xmppSerializer =  new ALXmppSerializer();
		
		ALMethodCall call = new ALMethodCall();
		 
		Log.i("ALXmppSerializer.test", "message=" + message);

		try {
			xmppSerializer.deserializeResponse(message, call);
		} catch (XMLRPCException e) {
			e.printStackTrace();
		}
		
		Log.i("ALXmppSerializer.test", "call=" + call);
	}
	
	public static String limitString(int len, String text){
		if (text.length()<=len)
			return text;
		else
			return text.substring(0, len-4) + " ...";
	}
	
	
	/*
	public static void test(){
		
		//for testing purposes only
		
		test_deserialize( "<iq xmlns=\"jabber:client\" to=\"test@xmpp.aldebaran-robotics.com\" type=\"result\" id=\"77c16546-8eb3-4dd1-80ba-5429b18dc6f8\"><query xmlns=\"jabber:iq:rpc\">"
			+ "<methodResponse>"
			+ "<params>"
			+ "<param>"
			+ "<value><string>15</string></value>"
			+ "</param>"
			+ "</params>"
			+ "</methodResponse></query></iq>" 
			);
		
		test_deserialize( "<iq xmlns=\"jabber:client\" to=\"test@xmpp.aldebaran-robotics.com\" type=\"set\" id=\"bba32309-5968-4995-9e2d-42d523ca1275\"><query xmlns=\"jabber:iq:rpc\">"
			+ "<methodCall>"
			+ "<methodName>ALTablette.bumpers</methodName>"
			+ "<params>"
			+ "<param>"	
			+ "<value><string>RightBumperPressed</string></value>"
			+ "</param>"
			+ "<param>"
			+ "<value><double>1.0</double></value>"
			+ "</param>"
			+ "</params>"
			+ "</methodCall></query></iq>" 
			);

		
		
	}
  */
}
