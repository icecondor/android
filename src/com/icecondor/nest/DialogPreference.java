package com.icecondor.nest;

import android.content.Context;
import android.util.AttributeSet;

// android.preference.DialogPreference is an abstract class but SDK 1.5 and earlier
// allowed for its instantiation. Apps are required to make their own subclass to use.
public class DialogPreference extends android.preference.DialogPreference {
	public DialogPreference(Context oContext, AttributeSet attrs)
	{
		super(oContext, attrs);		
	}
}
