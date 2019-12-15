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


import android.content.Context;
import android.content.Intent;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.regex.Pattern;

class EventListener implements View.OnKeyListener,
                               View.OnClickListener,
                               View.OnLongClickListener {
    Logic mHandler;
    ViewPager mPager;
    private Context context;

    void setHandler(Logic handler, ViewPager pager,Context context) {
        mHandler = handler;
        mPager = pager;
        this.context=context;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        mHandler.clearExpre();
        switch (id) {
        case R.id.del:
            mHandler.onDelete();
            break;

        case R.id.clear:
            mHandler.onClear();
            break;

        case R.id.equal:
            mHandler.onEnter();
            break;
        /*     case R.id.cb1:
                NumberInfo.setUserM("",context);
            break;
           case R.id.cb2:
                if (isNumeric(mHandler.getText())){
                 Long lll=   Long.parseLong(mHandler.getText());
                 if (!TextUtils.isEmpty(NumberInfo.getUserM(context))){
                     NumberInfo.setUserM((lll+ Long.parseLong(NumberInfo.getUserM(context)))+"",context);
                    }else{
                     NumberInfo.setUserM(lll.toString(),context);
                 }


                }
            break; case R.id.cb3:
                if (isNumeric(mHandler.getText())){
                    Long lll=   Long.parseLong(mHandler.getText());
                    if (!TextUtils.isEmpty(NumberInfo.getUserM(context))){
                        NumberInfo.setUserM((Long.parseLong(NumberInfo.getUserM(context))-lll)+"",context);
                    }else{
                        NumberInfo.setUserM("-"+lll,context);
                    }

                }
            break; case R.id.cb4:
                mHandler.insert(NumberInfo.getUserM(context));
                if (mPager != null && mPager.getCurrentItem() == Calculator.ADVANCED_PANEL) {
                    mPager.setCurrentItem(Calculator.BASIC_PANEL);
                }  break;
                case R.id.cb111://���ܹ���
                context.startActivity(new Intent(context,WanNengGongChiActivity.class));
            break;
            case R.id.cb11:
                if (isNumeric(mHandler.getText())){
                    int ii=  Integer.parseInt(mHandler.getText());
                    String strHex = Integer.toHexString(ii);
                    mHandler.onClear();
                    mHandler.insert(strHex);
                }else{
                    Toast.makeText(context, "��������", Toast.LENGTH_SHORT).show();
                }
                if (mPager != null && mPager.getCurrentItem() == Calculator.ADVANCED_PANEL) {
                    mPager.setCurrentItem(Calculator.BASIC_PANEL);
                }
            break;     */

        default:
            if (view instanceof Button) {
                String text = ((Button) view).getText().toString();
                if (text.length() >= 2) {
                    // add paren after sin, cos, ln, etc. from buttons
                    text += '(';
                }
                mHandler.insert(text);
                if (mPager != null && mPager.getCurrentItem() == Calculator.ADVANCED_PANEL) {
                    mPager.setCurrentItem(Calculator.BASIC_PANEL);
                }
            }
        }
    }

    /*
     * �ֽ�����ת16�����ַ���
     */
    public  String bytes2HexString(byte[] b) {
        String r = "";

        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            r += hex.toUpperCase();
        }

        return r;
    }

    //��������
    public static boolean isNumeric(String str){
        Pattern pattern = Pattern.compile("[0-9]*");
        return pattern.matcher(str).matches();
    }
    @Override
    public boolean onLongClick(View view) {
        int id = view.getId();
        if (id == R.id.del) {
            mHandler.onClear();
            return true;
        }
        return false;
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
        int action = keyEvent.getAction();

        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
            keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            boolean eat = mHandler.eatHorizontalMove(keyCode == KeyEvent.KEYCODE_DPAD_LEFT);
            return eat;
        }

        //Work-around for spurious key event from IME, bug #1639445
        if (action == KeyEvent.ACTION_MULTIPLE && keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            return true; // eat it
        }

        //Calculator.log("KEY " + keyCode + "; " + action);

        if (keyEvent.getUnicodeChar() == '=') {
            if (action == KeyEvent.ACTION_UP) {
                mHandler.onEnter();
            }
            return true;
        }

        if (keyCode != KeyEvent.KEYCODE_DPAD_CENTER &&
            keyCode != KeyEvent.KEYCODE_DPAD_UP &&
            keyCode != KeyEvent.KEYCODE_DPAD_DOWN &&
            keyCode != KeyEvent.KEYCODE_ENTER) {
            if (keyEvent.isPrintingKey() && action == KeyEvent.ACTION_UP) {
                // Tell the handler that text was updated.
                mHandler.onTextChanged();
            }
            return false;
        }

        /*
           We should act on KeyEvent.ACTION_DOWN, but strangely
           sometimes the DOWN event isn't received, only the UP.
           So the workaround is to act on UP...
           http://b/issue?id=1022478
         */

        if (action == KeyEvent.ACTION_UP) {
            switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                mHandler.onEnter();
                break;

            case KeyEvent.KEYCODE_DPAD_UP:
                mHandler.onUp();
                break;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                mHandler.onDown();
                break;
            }
        }
        return true;
    }
}
