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


import android.app.Activity;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

public class WanNengGongChiActivity extends Activity {

private EditText tz1;
private EditText tz;
private TextView expre;
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        setContentView(R.layout.main1);
        tz1=(EditText)findViewById(R.id.tz1);
        expre=(TextView)findViewById(R.id.expre);
        tz=(EditText)findViewById(R.id.tz);
        findViewById(R.id.jisuan).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                    if (!TextUtils.isEmpty(tz1.getText().toString().trim())&&!TextUtils.isEmpty(tz.getText().toString().trim())){
                     Double dd=   Double.parseDouble(tz1.getText().toString().trim());
                     Double ss=Double.parseDouble(tz.getText().toString().trim());

                     Double ww=ss/(dd*dd);

                        expre.setText(ww.toString());
                    }
            }
        });
    }


}
