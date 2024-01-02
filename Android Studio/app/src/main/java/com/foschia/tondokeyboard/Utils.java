package com.foschia.tondokeyboard;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class Utils
{
	public static final String LOG_PREFIX = "[TondoKeyboard]";

	public static enum LogType
	{
		ERROR,
		WARNING,
		INFO
	}

	public static void DebugLog(LogType type, String message)
	{
		switch (type)
		{
			case INFO:
				if (BuildConfig.DEBUG_MODE) Log.i(LOG_PREFIX, message);
				break;
			case WARNING:
				Log.w(LOG_PREFIX, message);
				break;
			case ERROR:
				Log.e(LOG_PREFIX, message);
				break;
		}
	}

	public static int streamToStream(InputStream in, OutputStream out) throws IOException
	{
		int copied = 0;
		byte[] buffer = new byte[512];
		int read = 0;
		do
		{
			read = in.read(buffer);
			if (read >= 0)
			{
				copied += read;
				out.write(buffer, 0, read);
			}
		}
		while (read == buffer.length);
		return copied;
	}

	public static byte[] intToBytesArray(int value)
	{
		if (value == 0) return new byte[]{0, 0, 0, 0};
		byte[] result = new byte[Integer.BYTES];
		for (int i = 0; i < result.length; i++)
		{
			result[i] = (byte) (value >>> (Byte.SIZE * (result.length - 1 - i)));
		}
		return result;
	}
}
