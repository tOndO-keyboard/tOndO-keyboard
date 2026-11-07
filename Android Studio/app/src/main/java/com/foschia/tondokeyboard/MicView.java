package com.foschia.tondokeyboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/** Voice input view: a big microphone button surrounded by rings that follow the audio level. */
public class MicView extends View implements SpeechListener
{
	public interface Callbacks
	{
		Palette palette();

		void onMicTapped();
	}

	private final Callbacks callbacks;
	private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Bitmap mic;
	private float level = 0f;      // 0..1 smoothed
	private float targetLevel = 0f;
	private boolean listening = false;
	private float unit = 1f;
	private long startTime = System.currentTimeMillis();

	public MicView(Context context, Callbacks callbacks)
	{
		super(context);
		this.callbacks = callbacks;
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inScaled = false;
		mic = BitmapFactory.decodeResource(getResources(), R.drawable.ic_microphone, o);
		paint.setStyle(Paint.Style.FILL);
	}

	public void setUnit(float unit)
	{
		this.unit = unit;
	}

	public void applyTheme()
	{
		setBackgroundColor(callbacks.palette().keyboardBackground);
		invalidate();
	}

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();
		NativeInterface.getInstance().RegisterSpeechListener(this);
	}

	@Override
	protected void onDetachedFromWindow()
	{
		NativeInterface.getInstance().UnregisterSpeechListener(this);
		super.onDetachedFromWindow();
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);
		Palette p = callbacks.palette();
		float cx = getWidth() / 2f, cy = getHeight() / 2f;
		float r = 150 * unit;
		level += (targetLevel - level) * 0.2f;
		if (listening)
		{
			for (int i = 0; i < 10; i++)
			{
				float ringR = r * (1.1f + 0.12f * i * (0.3f + level));
				int alpha = (int) (110 * (1f - i / 10f) * (0.25f + level));
				paint.setColor(p.glyphSelected);
				paint.setAlpha(Math.max(0, alpha));
				canvas.drawCircle(cx, cy, ringR, paint);
			}
		}
		paint.setAlpha(255);
		paint.setColor(p.shadow);
		canvas.drawCircle(cx, cy + 10 * unit, r, paint);
		paint.setColor(listening ? p.doneBackground : p.barButtonBackground);
		canvas.drawCircle(cx, cy, r, paint);
		paint.setColorFilter(new PorterDuffColorFilter(listening ? p.doneIcon : p.barButtonIcon, PorterDuff.Mode.SRC_IN));
		float s = 210 * unit;
		canvas.drawBitmap(mic, null, new RectF(cx - s / 2, cy - s / 2, cx + s / 2, cy + s / 2), paint);
		paint.setColorFilter(null);
		if (listening) postInvalidateOnAnimation();
	}

	@Override
	public boolean onTouchEvent(MotionEvent e)
	{
		if (e.getActionMasked() == MotionEvent.ACTION_UP)
		{
			NativeInterface.getInstance().Vibrate(10);
			callbacks.onMicTapped();
		}
		return true;
	}

	// ---- SpeechListener (called on the main thread by the recogniser)
	@Override
	public void onPartialData(String data) {}

	@Override
	public void onFinalData(String data) {}

	@Override
	public void onError(String errorMessage)
	{
		listening = false;
		targetLevel = 0;
		invalidate();
	}

	@Override
	public void onAudioLevelChange(float dB)
	{
		// rms in dB is roughly in [-2, 10]
		targetLevel = Math.max(0f, Math.min(1f, (dB + 2f) / 12f));
		invalidate();
	}

	@Override
	public void onReadyForSpeech()
	{
		listening = true;
		invalidate();
	}

	@Override
	public void onSpeechStart() {}

	@Override
	public void onSpeechEnd()
	{
		listening = false;
		targetLevel = 0;
		invalidate();
	}

	public boolean isListening()
	{
		return listening;
	}
}
