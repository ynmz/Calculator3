/*
 * Copyright (C) 2008 The Android Open Source Project
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

import com.will.calculator.CalculatorDisplay.Scroll;

import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.TextView;
import android.content.Context;
import android.content.res.Resources;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import org.javia.arity.Symbols;
import org.javia.arity.SyntaxException;

import java.util.ArrayList;

class Logic {
    private CalculatorDisplay mDisplay;
    private Symbols mSymbols = new Symbols();
    private History mHistory;
    public String mResult = "";
    private boolean mIsError = false;
    private int mLineLength = 0;

    private static final String INFINITY_UNICODE = "\u221e";

    public static final String MARKER_EVALUATE_ON_RESUME = "?";
    public static final String ERROR = "error";

    // the two strings below are the result of Double.toString() for Infinity &
    // NaN
    // they are not output to the user and don't require internationalization
    private static final String INFINITY = "Infinity";
    private static final String NAN = "NaN";

    static final char MINUS = '\u2212';

    private final String mErrorString;

    public final static int DELETE_MODE_BACKSPACE = 0;
    public final static int DELETE_MODE_CLEAR = 1;

    private int mDeleteMode = DELETE_MODE_BACKSPACE;

    private static final String LEFT_BRACKETS_STR = "(";
    private static final String RIGHT_BRACKETS_STR = ")";
    private static final char LEFT_BRACKETS_CHAR = '(';
    private static final char RIGHT_BRACKETS_CHAR = ')';
    private static final String[] CORNOR_TYPE = { "sin", "cos", "tan" };
    private static final String PI = "\u03c0\u00f7180";

    private TextView mExpreTextView;


    public interface Listener {
        void onDeleteModeChange();
    }

    private Listener mListener;
    private Context mContext;
    private Set<Entry<String, String>> mTranslationsSet;

    Logic(Context context, History history, CalculatorDisplay display,
            TextView expreTextView) {
        mContext = context;
        mExpreTextView = expreTextView;
        mErrorString = mContext.getResources().getString(R.string.error);
        mHistory = history;
        mDisplay = display;
        mDisplay.setLogic(this);
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public void setDeleteMode(int mode) {
        if (mDeleteMode != mode) {
            mDeleteMode = mode;
            mListener.onDeleteModeChange();
        }
    }

    public int getDeleteMode() {
        return mDeleteMode;
    }

    void setLineLength(int nDigits) {
        mLineLength = nDigits;
    }

    boolean eatHorizontalMove(boolean toLeft) {
        EditText editText = mDisplay.getEditText();
        int cursorPos = editText.getSelectionStart();
        return toLeft ? cursorPos == 0 : cursorPos >= editText.length();
    }

    public String getText() {
        return mDisplay.getText().toString();
    }

    void insert(String delta) {
        mDisplay.insert(delta);
        setDeleteMode(DELETE_MODE_BACKSPACE);
    }

    public void onTextChanged() {
        setDeleteMode(DELETE_MODE_BACKSPACE);
    }

    public void resumeWithHistory() {
        clearWithHistory(false);
    }

    private void clearWithHistory(boolean scroll) {
        String text = mHistory.getText();
        if (MARKER_EVALUATE_ON_RESUME.equals(text)) {
            if (!mHistory.moveToPrevious()) {
                text = "";
            }
            text = mHistory.getText();
            this.mExpreTextView.setText(text);
            evaluateAndShowResult(text, CalculatorDisplay.Scroll.NONE);
        } else {
            mResult = "";
            mDisplay.setText(text, scroll ? CalculatorDisplay.Scroll.UP
                    : CalculatorDisplay.Scroll.NONE);
            mIsError = false;
        }
    }

    private void clear(boolean scroll) {
        mExpreTextView.setText("");
        mHistory.enter("");
        mDisplay.setText("", scroll ? CalculatorDisplay.Scroll.UP
                : CalculatorDisplay.Scroll.NONE);
        cleared();
    }

    void cleared() {
        mResult = "";
        mIsError = false;
        updateHistory();

        setDeleteMode(DELETE_MODE_BACKSPACE);
    }

    boolean acceptInsert(String delta) {
        String text = getText();
        return !mIsError
                && (!mResult.equals(text) || isOperator(delta) || mDisplay
                        .getSelectionStart() != text.length());
    }

    void onDelete() {
        if (getText().replace(",", "").equals(mResult) || mIsError) {
            clear(false);
        } else {
            mDisplay.dispatchKeyEvent(new KeyEvent(0, KeyEvent.KEYCODE_DEL));
            mResult = "";
        }
    }

    void onClear() {
        clear(mDeleteMode == DELETE_MODE_CLEAR);
    }

    void onEnter() {
        if (mDeleteMode == DELETE_MODE_CLEAR) {
            clearWithHistory(false); // clear after an Enter on result
        } else {
            mExpreTextView.setText(getText());
            evaluateAndShowResult(getText(), CalculatorDisplay.Scroll.UP);
        }
    }

    public void evaluateAndShowResult(String text, Scroll scroll) {
        try {
            String result = "";// evaluate(text);
            if (!isAngle(text)) {
                result = evaluate(text);
            } else {
                result = evaluate(changeradiantoangle(text));
            }
            if (!text.equals(result)) {
                mHistory.enter(text);
                mResult = result;
                mDisplay.setText(formatResult(mResult), scroll);
                setDeleteMode(DELETE_MODE_CLEAR);
            }
        } catch (SyntaxException e) {
            mIsError = true;
            mResult = mErrorString;
            mDisplay.setText(mResult, scroll);
            setDeleteMode(DELETE_MODE_CLEAR);
        }
    }

    
    
    
    void onUp() {
        String text = getText();
        if (!text.equals(mResult)) {
            mHistory.update(text);
        }
        if (mHistory.moveToPrevious()) {
            mDisplay.setText(mHistory.getText(), CalculatorDisplay.Scroll.DOWN);
        }
    }

    void onDown() {
        String text = getText();
        if (!text.equals(mResult)) {
            mHistory.update(text);
        }
        if (mHistory.moveToNext()) {
            mDisplay.setText(mHistory.getText(), CalculatorDisplay.Scroll.UP);
        }
    }

    void updateHistory() {
        String text = mDisplay.deformatResult(getText());
        // Don't set the ? marker for empty text or the error string.
        // There is no need to evaluate those later.
        if (!TextUtils.isEmpty(text) && !TextUtils.equals(text, mErrorString)
                && text.equals(mResult)) {
            mHistory.update(MARKER_EVALUATE_ON_RESUME);
        } else if (TextUtils.equals(text, mErrorString)) {
            mHistory.update(ERROR);
        } else {
            mHistory.update(getText());
        }
    }

    String evaluate(String input) throws SyntaxException {
        if (input.trim().equals("")) {
            return "";
        }

        // drop final infix operators (they can only result in error)
        int size = input.length();
        while (size > 0 && isOperator(input.charAt(size - 1))) {
            input = input.substring(0, size - 1);
            --size;
        }
        // Find and replace any translated mathematical functions.
        input = replaceTranslations(input);
        double value = mSymbols.eval(input);

        String result = "";
        for (int precision = mLineLength; precision > 6; precision--) {
            result = tryFormattingWithPrecision(value, precision);
            if (result.length() <= mLineLength) {
                break;
            }
        }
        return result.replace('-', MINUS).replace(INFINITY, INFINITY_UNICODE);
    }

    private void addTranslation(HashMap<String, String> map, int t, int m) {
        Resources res = mContext.getResources();
        String translated = res.getString(t);
        String math = res.getString(m);
        if (!TextUtils.equals(translated, math)) {
            map.put(translated, math);
        }
    }

    private String replaceTranslations(String input) {
        if (mTranslationsSet == null) {
            HashMap<String, String> map = new HashMap<String, String>();
            addTranslation(map, R.string.sin, R.string.sin_mathematical_value);
            addTranslation(map, R.string.cos, R.string.cos_mathematical_value);
            addTranslation(map, R.string.tan, R.string.tan_mathematical_value);
            addTranslation(map, R.string.e, R.string.e_mathematical_value);
            addTranslation(map, R.string.ln, R.string.ln_mathematical_value);
            addTranslation(map, R.string.lg, R.string.lg_mathematical_value);
            mTranslationsSet = map.entrySet();
        }
        for (Entry<String, String> entry : mTranslationsSet) {
            input = input.replace(entry.getKey(), entry.getValue());
        }
        return input;
    }

    private String tryFormattingWithPrecision(double value, int precision) {
        // The standard scientific formatter is basically what we need. We will
        // start with what it produces and then massage it a bit.
        String result = String.format(Locale.US, "%" + mLineLength + "."
                + precision + "g", value);
        if (result.equals(NAN)) { // treat NaN as Error
            mIsError = true;
            return mErrorString;
        }
        String mantissa = result;
        String exponent = null;
        int e = result.indexOf('e');
        if (e != -1) {
            mantissa = result.substring(0, e);

            // Strip "+" and unnecessary 0's from the exponent
            exponent = result.substring(e + 1);
            if (exponent.startsWith("+")) {
                exponent = exponent.substring(1);
            }
            exponent = String.valueOf(Integer.parseInt(exponent));
        } else {
            mantissa = result;
        }

        int period = mantissa.indexOf('.');
        if (period == -1) {
            period = mantissa.indexOf(',');
        }
        if (period != -1) {
            // Strip trailing 0's
            while (mantissa.length() > 0 && mantissa.endsWith("0")) {
                mantissa = mantissa.substring(0, mantissa.length() - 1);
            }
            if (mantissa.length() == period + 1) {
                mantissa = mantissa.substring(0, mantissa.length() - 1);
            }
        }

        if (exponent != null) {
            result = mantissa + 'e' + exponent;
        } else {
            result = mantissa;
        }
        return result;
    }

    static boolean isOperator(String text) {
        return text.length() == 1 && isOperator(text.charAt(0));
    }

    static boolean isOperator(char c) {
        // plus minus times div
        return "+\u2212\u00d7\u00f7/*".indexOf(c) != -1;
    }

    public static String changeradiantoangle(String text) {
        int bracketsNum = 0;
        String oldtextStr;

        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == LEFT_BRACKETS_CHAR) {
                bracketsNum++;
            } else if (text.charAt(i) == RIGHT_BRACKETS_CHAR) {
                bracketsNum--;
            }
        }

        if (bracketsNum > 0) {
            for (int i = 0; i < bracketsNum; i++) {
                text = text + RIGHT_BRACKETS_STR;
            }
        }

        for (int k = 0; k < CORNOR_TYPE.length; k++) {
            ArrayList<Integer> index = new ArrayList<Integer>();
            int n;
            oldtextStr = text;
            n = oldtextStr.split(CORNOR_TYPE[k]).length;
            if (n > 1) {
                for (int i = n; i > 1; i--) {
                    int lastCornerIndex = oldtextStr
                            .lastIndexOf(CORNOR_TYPE[k]);
                    if (lastCornerIndex >= 0) {
                        index.add(lastCornerIndex);
                        oldtextStr = oldtextStr.substring(0, lastCornerIndex);
                    }
                }

                for (int i = 0; i < index.size(); i++) {
                    String beginString, endString;
                    boolean findLastBra = false;
                    int leftBraNum = 0;
                    int lastRightBraIndes = 0;
                    beginString = text.substring(0, index.get(i));
                    endString = text.substring(index.get(i) + 3, text.length());
                    for (int j = 0; j < endString.length(); j++) {
                        if (!findLastBra
                                && endString.charAt(j) == LEFT_BRACKETS_CHAR) {
                            leftBraNum++;
                        } else if (!findLastBra
                                && endString.charAt(j) == RIGHT_BRACKETS_CHAR) {
                            if (leftBraNum == 1) {
                                lastRightBraIndes = j;
                                findLastBra = true;
                            } else {
                                leftBraNum--;
                            }
                        }
                    }
                    endString = LEFT_BRACKETS_STR
                            + endString.substring(0, lastRightBraIndes + 1)
                            + PI
                            + RIGHT_BRACKETS_STR
                            + endString.substring(lastRightBraIndes + 1,
                                    endString.length());
                    text = beginString + CORNOR_TYPE[k] + endString;
                }
            }
        }
        return text;
    }

    public boolean isAngle(String text) {
        int n;
        for (int i = 0; i < CORNOR_TYPE.length; i++) {
            n = text.split(CORNOR_TYPE[i]).length;
            if (n > 1) {
                return true;
            }
        }
        return false;
    }

    public void clearExpre(){
        this.mExpreTextView.setText("");
    }
    
    
    private String formatResult(String result){
        
        Log.i("info", "------------------------" + result) ;
        String intStr = "";
        String decStr = "";
        DecimalFormat df = new DecimalFormat(",###") ;
        if(result.contains(".")){
            intStr = result.substring(0,result.indexOf(".")) ;
            decStr = result.substring(result.indexOf("."),result.length()) ;
            return df.format(Long.parseLong(intStr)) + decStr;
        }else{
            intStr = result ;
            return df.format(Long.parseLong(intStr)) ;
        }
        
    }
}
