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


/**
 * Use xmlrpc to encode message to be send to the robot
 * 
 * @author bilock@google.com (JŽr™me Monceaux)
 *
 */
public class ALMethodCall {
	
	public String uid;	
	public String methodName;
	public Object[] params;
	public Object result;
	public boolean isResponse = false;
	
	public static String limitString(int len, String text){
		if (text.length()<=len)
			return text;
		else
			return text.substring(0, len-4) + " ...";
	}
	
	public String toString(){
		StringBuilder text = new StringBuilder();	
		text.append(String.format("ALMethodCall %s",methodName));
		
    	if (params!=null){
	    	for (Object object : params){
	    		if (object instanceof java.lang.String) 
	    			text.append(String.format(" \"%s\"",limitString(40, object.toString())));
	    		else
	    			text.append(String.format(" %s",limitString(40, object.toString())));
	    	}
    	}
    	if (result!=null){
    		text.append(String.format(" => %s",limitString(40, result.toString())));
    	}
		return text.toString();
	}
}


