package com.foschia.tondokeyboard;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/** The clipboard button plus the pinned clipboards ("notes"). */
public class MacrosView extends FrameLayout
{
	public interface Callbacks
	{
		void onCommit(String text);

		Palette palette();
	}

	private final Callbacks callbacks;
	private final ScrollView scroll;
	private final LinearLayout column;
	private float unit = 1f;
	private String activeFunctionText; // text whose pin/delete icon is shown
	private String clipboard = "";
	private final Runnable storeListener = this::rebuild;
	private final NativeInterface.ImeEventListener imeListener = new NativeInterface.ImeEventListener()
	{
		@Override
		public void onClipboardChanged(String content)
		{
			clipboard = content == null ? "" : content;
			post(MacrosView.this::rebuild);
		}
	};

	public MacrosView(Context context, Callbacks callbacks)
	{
		super(context);
		this.callbacks = callbacks;
		scroll = new ScrollView(context);
		column = new LinearLayout(context);
		column.setOrientation(LinearLayout.VERTICAL);
		scroll.addView(column, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		addView(scroll, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
	}

	public void setUnit(float unit)
	{
		this.unit = unit;
		rebuild();
	}

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();
		MacroStore.get().addListener(storeListener);
		NativeInterface.getInstance().addImeListener(imeListener);
		clipboard = NativeInterface.getInstance().GetClipboardContent();
		rebuild();
	}

	@Override
	protected void onDetachedFromWindow()
	{
		MacroStore.get().removeListener(storeListener);
		NativeInterface.getInstance().removeImeListener(imeListener);
		super.onDetachedFromWindow();
	}

	/** Refreshes the clipboard content each time the view is shown. */
	public void refresh()
	{
		clipboard = NativeInterface.getInstance().GetClipboardContent();
		activeFunctionText = null;
		rebuild();
	}

	public void applyTheme()
	{
		setBackgroundColor(callbacks.palette().keyboardBackground);
		rebuild();
	}

	private View button(final String text, boolean clipboardButton)
	{
		final Palette p = callbacks.palette();
		final boolean functionActive = text.equals(activeFunctionText);
		FrameLayout frame = new FrameLayout(getContext());
		GradientDrawable bg = new GradientDrawable();
		bg.setColor(p.barButtonBackground);
		bg.setCornerRadius(30 * unit);
		frame.setBackground(bg);

		TextView tv = new TextView(getContext());
		tv.setText(text.trim());
		tv.setMaxLines(clipboardButton ? 3 : 2);
		tv.setEllipsize(TextUtils.TruncateAt.END);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, 34 * unit);
		tv.setTextColor(p.glyphConsonant);
		tv.setGravity(Gravity.CENTER_VERTICAL);
		tv.setPadding((int) (24 * unit), (int) (8 * unit), (int) (24 * unit), (int) (8 * unit));
		tv.setAlpha(functionActive ? 0f : 1f);
		frame.addView(tv, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		if (functionActive)
		{
			ImageView icon = new ImageView(getContext());
			icon.setImageResource(clipboardButton ? R.drawable.ic_pin_icon : R.drawable.ic_macro_delete);
			icon.setColorFilter(p.glyphSelected, PorterDuff.Mode.SRC_IN);
			icon.setPadding((int) (14 * unit), (int) (14 * unit), (int) (14 * unit), (int) (14 * unit));
			frame.addView(icon, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		}

		frame.setOnClickListener(v -> {
			NativeInterface.getInstance().Vibrate(10);
			if (functionActive)
			{
				activeFunctionText = null;
				if (clipboardButton)
				{
					MacroStore store = MacroStore.get();
					if (!store.contains(text))
					{
						if (store.canAddMore()) store.add(text, false);
						else
							NativeInterface.getInstance().showToast(Localization.Instance.localizeOrDefault("too_many_macros_toast", "Unlock the Pro version to get more notes."));
					}
				}
				else
				{
					MacroStore.get().remove(text);
				}
				rebuild();
				return;
			}
			callbacks.onCommit(text);
			MacroStore.get().use(text);
		});
		frame.setOnLongClickListener(v -> {
			NativeInterface.getInstance().Vibrate(10);
			activeFunctionText = functionActive ? null : text;
			rebuild();
			return true;
		});
		return frame;
	}

	private void rebuild()
	{
		column.removeAllViews();
		int pad = (int) (20 * unit);
		column.setPadding(pad, pad, pad, pad);

		if (clipboard != null && !clipboard.trim().isEmpty())
		{
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (150 * unit));
			lp.bottomMargin = pad;
			column.addView(button(clipboard, true), lp);
		}

		List<String> macros = MacroStore.get().list();
		for (int i = 0; i < macros.size(); i += 2)
		{
			LinearLayout row = new LinearLayout(getContext());
			row.setOrientation(LinearLayout.HORIZONTAL);
			for (int k = 0; k < 2; k++)
			{
				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, (int) (110 * unit), 1);
				lp.rightMargin = k == 0 ? pad / 2 : 0;
				lp.leftMargin = k == 1 ? pad / 2 : 0;
				if (i + k < macros.size()) row.addView(button(macros.get(i + k), false), lp);
				else row.addView(new View(getContext()), lp);
			}
			LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			rlp.bottomMargin = pad / 2;
			column.addView(row, rlp);
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b)
	{
		scroll.layout(0, 0, r - l, b - t);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		scroll.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
	}
}
