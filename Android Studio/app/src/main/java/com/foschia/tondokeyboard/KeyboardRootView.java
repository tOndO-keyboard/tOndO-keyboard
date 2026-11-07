package com.foschia.tondokeyboard;

import android.content.Context;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/** Container of the whole native keyboard: the main canvas plus the emoji / macros / voice views. */
public class KeyboardRootView extends FrameLayout implements KeyboardCanvasView.Host, EmojiView.Callbacks, MacrosView.Callbacks, MicView.Callbacks
{
	public enum Content {MAIN, EMOJI, MIC, MACROS}

	private final KeyboardCanvasView canvas;
	private final EmojiView emojiView;
	private final MacrosView macrosView;
	private final MicView micView;
	private Content content = Content.MAIN;
	private boolean dark = false;
	private boolean listening = false;
	private Content requestedDuringSpeech = Content.MAIN;

	private final SpeechListener speechListener = new SpeechListener()
	{
		@Override
		public void onPartialData(String data) {}

		@Override
		public void onFinalData(String data)
		{
			canvas.onExternalCommit(false);
		}

		@Override
		public void onError(String errorMessage)
		{
			listening = false;
			Content c = requestedDuringSpeech;
			requestedDuringSpeech = Content.MAIN;
			post(() -> show(c));
		}

		@Override
		public void onAudioLevelChange(float dB) {}

		@Override
		public void onReadyForSpeech()
		{
			listening = true;
		}

		@Override
		public void onSpeechStart() {}

		@Override
		public void onSpeechEnd()
		{
			listening = false;
			post(() -> show(Content.MAIN));
		}
	};

	private final NativeInterface.ImeEventListener imeListener = new NativeInterface.ImeEventListener()
	{
		@Override
		public void onStartInputView(String precedingCharacter)
		{
			show(Content.MAIN);
			canvas.setBar(KeyboardCanvasView.Bar.MAIN);
			requestProportion();
		}

		@Override
		public void onWindowHidden()
		{
			if (listening) NativeInterface.getInstance().CancelTranscribingSpeech();
		}
	};

	public KeyboardRootView(Context context)
	{
		super(context);
		setClipChildren(false);
		setClipToPadding(false);
		canvas = new KeyboardCanvasView(context, this);
		addView(canvas, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		emojiView = new EmojiView(context, this);
		macrosView = new MacrosView(context, this);
		micView = new MicView(context, this);
		addView(emojiView);
		addView(macrosView);
		addView(micView);
		emojiView.setVisibility(GONE);
		macrosView.setVisibility(GONE);
		micView.setVisibility(GONE);
		NativeInterface.getInstance().addImeListener(imeListener);
		NativeInterface.getInstance().RegisterSpeechListener(speechListener);
		applyTheme(false);
	}

	@Override
	protected void onDetachedFromWindow()
	{
		super.onDetachedFromWindow();
	}

	public void applyTheme(boolean dark)
	{
		applyTheme(dark, false);
	}

	public void applyTheme(boolean dark, boolean dynamicColors)
	{
		this.dark = dark;
		Palette p = dynamicColors ? Palette.dynamic(getContext(), dark) : dark ? Palette.DARK : Palette.LIGHT;
		canvas.setPalette(p);
		emojiView.applyTheme();
		macrosView.applyTheme();
		micView.applyTheme();
	}

	public int getWindowBackgroundColor(boolean barOnBottom)
	{
		Palette p = canvas.getPalette();
		return barOnBottom ? p.barBackground : p.keyboardBackground;
	}

	public void setLandscapeMode(boolean landscape)
	{
		canvas.setPlatformLandscape(landscape);
	}

	private void requestProportion()
	{
		canvas.reloadSettings();
		NativeInterface.getInstance().SetKeyboardProportion(canvas.getSettings().topBarHidden ? 1.82f : 1.5f);
	}

	// ---- layout of the overlays over the content area

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int w = getMeasuredWidth(), h = getMeasuredHeight();
		canvas.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
		float unit = Math.min(h / (KeyboardCanvasView.BOTTOM_HEIGHT + canvas.getBarHeightUnits()), w / KeyboardCanvasView.REF_WIDTH);
		int contentH = Math.round(KeyboardCanvasView.BOTTOM_HEIGHT * unit);
		for (View v : new View[]{emojiView, macrosView, micView})
			v.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(contentH, MeasureSpec.EXACTLY));
		emojiView.setUnit(unit);
		macrosView.setUnit(unit);
		micView.setUnit(unit);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b)
	{
		int w = r - l, h = b - t;
		canvas.layout(0, 0, w, h);
		float unit = Math.min(h / (KeyboardCanvasView.BOTTOM_HEIGHT + canvas.getBarHeightUnits()), w / KeyboardCanvasView.REF_WIDTH);
		int contentH = Math.round(KeyboardCanvasView.BOTTOM_HEIGHT * unit);
		int top = canvas.getSettings().topBarWentBottom ? 0 : Math.round(canvas.getBarHeightUnits() * unit);
		for (View v : new View[]{emojiView, macrosView, micView}) v.layout(0, top, w, top + contentH);
	}

	// ---- view switching

	private void show(Content c)
	{
		boolean wasMic = content == Content.MIC;
		if (wasMic && c != Content.MIC && listening)
		{
			requestedDuringSpeech = c;
			NativeInterface.getInstance().CancelTranscribingSpeech();
		}
		content = c;
		emojiView.setVisibility(c == Content.EMOJI ? VISIBLE : GONE);
		macrosView.setVisibility(c == Content.MACROS ? VISIBLE : GONE);
		micView.setVisibility(c == Content.MIC ? VISIBLE : GONE);
		if (c == Content.EMOJI)
		{
			emojiView.scrollToTop();
			canvas.setBar(KeyboardCanvasView.Bar.SKIN_TONES);
		}
		else if (canvas.getBar() == KeyboardCanvasView.Bar.SKIN_TONES)
		{
			canvas.setBar(KeyboardCanvasView.Bar.MAIN);
		}
		if (c == Content.MACROS) macrosView.refresh();
		if (c == Content.MIC && !listening) startSpeechOrRequestPermission();
		canvas.invalidate();
	}

	private void startSpeechOrRequestPermission()
	{
		NativeInterface ni = NativeInterface.getInstance();
		if (ni.HasRecordAudioPermissions()) ni.StartTranscribingSpeech();
		else ni.RequestRecordAudioPermissions();
	}

	// ---- Host
	@Override
	public void onViewRequested(Content requested)
	{
		show(content == requested ? Content.MAIN : requested);
	}

	@Override
	public Content getContent()
	{
		return content;
	}

	@Override
	public boolean isEmojiMode()
	{
		return content == Content.EMOJI;
	}

	@Override
	public void onTopBarHiddenChanged(boolean hidden)
	{
		NativeInterface.getInstance().SetKeyboardProportion(hidden ? 1.82f : 1.5f);
	}

	@Override
	public boolean isPro()
	{
		return NativeInterface.getInstance().isPro();
	}

	// ---- Callbacks
	@Override
	public Palette palette()
	{
		return canvas.getPalette();
	}

	@Override
	public void onEmoji(String emoji)
	{
		NativeInterface.getInstance().CommitEmoji(emoji);
		canvas.onExternalCommit(true);
	}

	@Override
	public void onBackspace()
	{
		NativeInterface.getInstance().CommitBackspace();
		canvas.onExternalCommit(false);
	}

	@Override
	public void onCommit(String text)
	{
		NativeInterface.getInstance().CommitString(text);
		canvas.onExternalCommit(false);
	}

	@Override
	public void onMicTapped()
	{
		NativeInterface ni = NativeInterface.getInstance();
		if (listening) ni.CancelTranscribingSpeech();
		else startSpeechOrRequestPermission();
	}
}
