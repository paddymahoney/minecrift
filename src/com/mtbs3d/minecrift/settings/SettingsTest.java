/**
 * Copyright 2013 Mark Browning, StellaArtois
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package com.mtbs3d.minecrift.settings;

import java.io.File;

public class SettingsTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		VRSettings set = new VRSettings(null, new File(""));
		Float testValue = 10.0f;
		Float tv = testValue;
		tv = 4.0f;
		System.out.println(testValue);
		return;
		/*
		System.out.println( "Before:"+testValue);
		FloatValue v = new FloatValue( testValue, 5,"test","%.02f",0,100);
		System.out.println( "Construct:"+testValue);
		v.setValue(4);
		System.out.println( "setValue:"+testValue);
		
		testValue = 5.0f;
		
		System.out.println("Label: "+ v.getDisplayString());
		*/

	}

}
