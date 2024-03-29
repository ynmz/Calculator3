/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.will.calculator;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import android.widget.Toast;
import com.google.common.collect.ImmutableMap;

public class CalculatorEditText extends EditText {

	private static final String LOG_TAG = "Calculator2";
	private static final int CUT = 0;
	private static final int COPY = 1;
	private static final int PASTE = 2;
	private String[] mMenuItemsStrings;
	private ImmutableMap<String, String> sReplacementTable;
	private String[] sOperators;
	private Context mContext;

	public CalculatorEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		setCustomSelectionActionModeCallback(new NoTextSelectionMode());
		setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		mContext = context;
		addTextChangedListener(new MaxLengthWatcher(250,
				CalculatorEditText.this));
	}

	public class MaxLengthWatcher implements TextWatcher {
		private int maxLength = 0;
		private EditText editText = null;

		public MaxLengthWatcher(int maxLen, EditText editText) {
			this.maxLength = maxLen;
			this.editText = editText;
		}

		public void afterTextChanged(Editable arg0) {

		}

		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {

		}

		public void onTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {
			Editable editable = editText.getText();
			int length = editable.length();

			if (length > maxLength) {
				int selEndIndex = Selection.getSelectionEnd(editable);
				String str = editable.toString();
				// Cut out new string
				String newString = str.substring(0, maxLength);
				editText.setText(newString);
				editable = editText.getText();

				// The length of new string
				int newLength = editable.length();
				// Old cursor's position is longer than max length
				if (selEndIndex > newLength) {
					selEndIndex = editable.length();
					Toast.makeText(
							mContext,
							mContext.getResources().getString(
									R.string.input_too_long), Toast.LENGTH_LONG)
							.show();
				}
				// Set the new cursor's position
				Selection.setSelection(editable, selEndIndex);
			}
		}
	}

	// [BUGFIX]-Mod-END by TSNJ,jing.su,

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getActionMasked() == MotionEvent.ACTION_UP) {
			// Hack to prevent keyboard and insertion handle from showing.
			cancelLongPress();
		}
		return super.onTouchEvent(event);
	}

	@Override
	public boolean performLongClick() {
		showContextMenu();
		return true;
	}

	@Override
	public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
		super.onInitializeAccessibilityEvent(event);
		String mathText = mathParse(getText().toString());
		// Parse the string into something more "mathematical" sounding.
		if (!TextUtils.isEmpty(mathText)) {
			event.getText().clear();
			event.getText().add(mathText);
			setContentDescription(mathText);
		}
	}

	@Override
	public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
		super.onInitializeAccessibilityNodeInfo(info);
		info.setText(mathParse(getText().toString()));
	}

	@Override
	public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
		// Do nothing.
	}

	private String mathParse(String plainText) {
		String parsedText = plainText;
		if (!TextUtils.isEmpty(parsedText)) {
			// Initialize replacement table.
			initializeReplacementTable();
			for (String operator : sOperators) {
				if (sReplacementTable.containsKey(operator)) {
					parsedText = parsedText.replace(operator,
							sReplacementTable.get(operator));
				}
			}
		}
		return parsedText;
	}

	private synchronized void initializeReplacementTable() {
		if (sReplacementTable == null) {
			ImmutableMap.Builder<String, String> builder = ImmutableMap
					.builder();
			Resources res = getContext().getResources();
			sOperators = res.getStringArray(R.array.operators);
			String[] descs = res.getStringArray(R.array.operatorDescs);
			int pos = 0;
			for (String key : sOperators) {
				builder.put(key, descs[pos]);
				pos++;
			}
			sReplacementTable = builder.build();
		}
	}

	private class MenuHandler implements MenuItem.OnMenuItemClickListener {
		public boolean onMenuItemClick(MenuItem item) {
			return onTextContextMenuItem(item.getTitle());
		}
	}

	public boolean onTextContextMenuItem(CharSequence title) {
		boolean handled = false;
		if (TextUtils.equals(title, mMenuItemsStrings[CUT])) {
			cutContent();
			handled = true;
		} else if (TextUtils.equals(title, mMenuItemsStrings[COPY])) {
			copyContent();
			handled = true;
		} else if (TextUtils.equals(title, mMenuItemsStrings[PASTE])) {
			pasteContent();
			handled = true;
		}
		return handled;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu) {
		MenuHandler handler = new MenuHandler();
		if (mMenuItemsStrings == null) {
			Resources resources = getResources();
			mMenuItemsStrings = new String[3];
			mMenuItemsStrings[CUT] = resources.getString(android.R.string.cut);
			mMenuItemsStrings[COPY] = resources
					.getString(android.R.string.copy);
			mMenuItemsStrings[PASTE] = resources
					.getString(android.R.string.paste);
		}
		for (int i = 0; i < mMenuItemsStrings.length; i++) {
			menu.add(Menu.NONE, i, i, mMenuItemsStrings[i])
					.setOnMenuItemClickListener(handler);
		}
		if (getText().length() == 0) {
			menu.getItem(CUT).setVisible(false);
			menu.getItem(COPY).setVisible(false);
		}
		ClipData primaryClip = getPrimaryClip();
		if (primaryClip == null
				|| primaryClip.getItemCount() == 0
				|| !canPaste(primaryClip.getItemAt(0)
						.coerceToText(getContext()))) {
			menu.getItem(PASTE).setVisible(false);
		}
	}

	private void setPrimaryClip(ClipData clip) {
		ClipboardManager clipboard = (ClipboardManager) getContext()
				.getSystemService(Context.CLIPBOARD_SERVICE);
		clipboard.setPrimaryClip(clip);
	}

	private void copyContent() {
		final Editable text = getText();
		int textLength = text.length();
		setSelection(0, textLength);
		ClipboardManager clipboard = (ClipboardManager) getContext()
				.getSystemService(Context.CLIPBOARD_SERVICE);
		clipboard.setPrimaryClip(ClipData.newPlainText(null, text));
		Toast.makeText(getContext(), R.string.text_copied_toast,
				Toast.LENGTH_SHORT).show();
		setSelection(textLength);
	}

	private void cutContent() {
		final Editable text = getText();
		int textLength = text.length();
		setSelection(0, textLength);
		setPrimaryClip(ClipData.newPlainText(null, text));
		((Editable) getText()).delete(0, textLength);
		setSelection(0);
	}

	private ClipData getPrimaryClip() {
		ClipboardManager clipboard = (ClipboardManager) getContext()
				.getSystemService(Context.CLIPBOARD_SERVICE);
		return clipboard.getPrimaryClip();
	}

	private void pasteContent() {
		ClipData clip = getPrimaryClip();
		if (clip != null) {
			for (int i = 0; i < clip.getItemCount(); i++) {
				CharSequence paste = clip.getItemAt(i).coerceToText(
						getContext());
				if (canPaste(paste)) {
					((Editable) getText()).insert(getSelectionEnd(), paste);
				}
			}
		}
	}

	private boolean canPaste(CharSequence paste) {
		boolean canPaste = true;
		try {
			Float.parseFloat(paste.toString());
		} catch (NumberFormatException e) {
			Log.e(LOG_TAG, "Error turning string to integer. Ignoring paste.",
					e);
			canPaste = false;
		}
		return canPaste;
	}

	class NoTextSelectionMode implements ActionMode.Callback {
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			copyContent();
			// Prevents the selection action mode on double tap.
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}
	}
}
