package com.dadazhou.naocontrollers_test02.interfaces;

public interface IAccelerometerEvent 
{
	public void onAccelerometerStart();
	public void onAccelerometerStop();
	public void onAccelerometerNoSupported();
	public void onAccelerometerUpdate(float x, float y, float z);
}
