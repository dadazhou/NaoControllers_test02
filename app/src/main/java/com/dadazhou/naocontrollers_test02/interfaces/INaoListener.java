package com.dadazhou.naocontrollers_test02.interfaces;

import android.graphics.Bitmap;

public interface INaoListener 
{
	void ongetInstalledBehaviors(String[] behaviors);
	void ongetVolume(int vol);
	void onpictureAvailable(Bitmap picture);
}
