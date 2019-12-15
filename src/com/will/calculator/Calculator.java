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


import com.will.calculator.R;

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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

public class Calculator extends Activity implements PanelSwitcher.Listener,
        Logic.Listener, OnClickListener, OnMenuItemClickListener {
    EventListener mListener = new EventListener();
    private CalculatorDisplay mDisplay;
    private Persist mPersist;
    private History mHistory;
    private Logic mLogic;
    private ViewPager mPager;
    private View mClearButton;
    private View mBackspaceButton;
    private View mOverflowMenuButton;
    private View mAdvancedCalButton;
    private View mBasicCalButton;
    private TextView mExpreTextView;

    static final int BASIC_PANEL = 0;
    static final int ADVANCED_PANEL = 1;

    private static final String LOG_TAG = "Calculator";
    private static final boolean DEBUG = false;
    private static final boolean LOG_ENABLED = false;
    private static final String STATE_CURRENT_VIEW = "state-current-view";
     
    @Override
    public void onCreate(Bundle state) {           //
        super.onCreate(state);

        setContentView(R.layout.main);
        mPager = (ViewPager) findViewById(R.id.panelswitch);
        if (mPager != null) {
            mPager.setAdapter(new PageAdapter(mPager));
        } else {
            // Single page UI
            final TypedArray buttons = getResources().obtainTypedArray(
                    R.array.buttons);
            for (int i = 0; i < buttons.length(); i++) {
                setOnClickListener(null, buttons.getResourceId(i, 0));
            }
            buttons.recycle();
        }

        mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {                                       //onPageSelected
                if (position == 0) {
                    if (!getAdvancedVisibility() && mPager != null) {
                        mBasicCalButton.setVisibility(View.GONE);
                        mAdvancedCalButton.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (!getBasicVisibility() && mPager != null) {
                        mAdvancedCalButton.setVisibility(View.GONE);
                        mBasicCalButton.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {                       //onPageScrolled
                // TODO Auto-generated method stub
            }

            @Override
            public void onPageScrollStateChanged(int arg0) {                                    //onPageScrollStateChanged
                // TODO Auto-generated method stub
            }

        });
        if (mClearButton == null) {
            mClearButton = findViewById(R.id.clear);
            mClearButton.setOnClickListener(mListener);
            mClearButton.setOnLongClickListener(mListener);
        }


        if (mBackspaceButton == null) {
            mBackspaceButton = findViewById(R.id.del);
            mBackspaceButton.setOnClickListener(mListener);
            mBackspaceButton.setOnLongClickListener(mListener);
        }
        if (mAdvancedCalButton == null) {
            mAdvancedCalButton = findViewById(R.id.advanced_cal);
            mAdvancedCalButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    if (!getAdvancedVisibility() && mPager != null) {
                        mPager.setCurrentItem(ADVANCED_PANEL, true);
                        mAdvancedCalButton.setVisibility(View.GONE);
                        mBasicCalButton.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
        if (mBasicCalButton == null) {
            mBasicCalButton = findViewById(R.id.basic_cal);
            mBasicCalButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {                                                          //onClick
                    // TODO Auto-generated method stub
                    if (!getBasicVisibility() && mPager != null) {
                        mPager.setCurrentItem(BASIC_PANEL, true);
                        mBasicCalButton.setVisibility(View.GONE);
                        mAdvancedCalButton.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        mExpreTextView = (TextView) findViewById(R.id.expre);
        mExpreTextView.setMovementMethod(ScrollingMovementMethod.getInstance());

        mPersist = new Persist(this);
        mPersist.load();

        mHistory = mPersist.history;

        mDisplay = (CalculatorDisplay) findViewById(R.id.display);

        mLogic = new Logic(this, mHistory, mDisplay, mExpreTextView);
        mLogic.setListener(this);

        mLogic.setDeleteMode(mPersist.getDeleteMode());
        mLogic.setLineLength(mDisplay.getMaxDigits());

        String mHistroyValue = mHistory.current().getEdited();
        if (TextUtils.equals(mHistroyValue, mLogic.ERROR)) {
            mHistory.update(this.getResources().getString(R.string.error));
        }

        HistoryAdapter historyAdapter = new HistoryAdapter(this, mHistory,
                mLogic);
        mHistory.setObserver(historyAdapter);

        if (mPager != null) {
            mPager.setCurrentItem(state == null ? 0 : state.getInt(
                    STATE_CURRENT_VIEW, 0));
        }

        mListener.setHandler(mLogic, mPager,this);
        mDisplay.setOnKeyListener(mListener);

        if (!ViewConfiguration.get(this).hasPermanentMenuKey()) {
            createFakeMenu();
        }

        mLogic.resumeWithHistory();
        updateDeleteMode();
    }

    private void updateDeleteMode() {                                                             //updateDeleteMode
        if (mLogic.getDeleteMode() == Logic.DELETE_MODE_BACKSPACE) {
            // mClearButton.setVisibility(View.GONE);
            // mBackspaceButton.setVisibility(View.VISIBLE);
        }
        else {
            // mClearButton.setVisibility(View.VISIBLE);
            // mBackspaceButton.setVisibility(View.GONE);
        }
    }

    void setOnClickListener(View root, int id) {                                             //setClickListener
        final View target = root != null ? root.findViewById(id)
                : findViewById(id);
        target.setOnClickListener(mListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {                                                  //onCreateOptionsMenu
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {                                                 //onPrepareOptionsMenu
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.basic).setVisible(!getBasicVisibility());
        menu.findItem(R.id.advanced).setVisible(!getAdvancedVisibility());
        return true;
    }

    private void createFakeMenu() {                                                                     //createFakeMenu
        mOverflowMenuButton = findViewById(R.id.overflow_menu);
        if (mOverflowMenuButton != null) {
            mOverflowMenuButton.setVisibility(View.VISIBLE);
            mOverflowMenuButton.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {                                                                       //onclick
        switch (v.getId()) {
        case R.id.overflow_menu:
            PopupMenu menu = constructPopupMenu();
            if (menu != null) {
                menu.show();
            }
            break;
        }
    }

    private PopupMenu constructPopupMenu() {                                                                 //PopupMenu constructPopupMenu
        final PopupMenu popupMenu = new PopupMenu(this, mOverflowMenuButton);
        final Menu menu = popupMenu.getMenu();
        popupMenu.inflate(R.menu.menu);
        popupMenu.setOnMenuItemClickListener(this);
        onPrepareOptionsMenu(menu);
        return popupMenu;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {                                                                 //onMenuIteClick
        return onOptionsItemSelected(item);
    }

    private boolean getBasicVisibility() {                                                                               //getBasicVisibility
        return mPager != null && mPager.getCurrentItem() == BASIC_PANEL;
    }

    private boolean getAdvancedVisibility() {                                                                     //getAdvancedVisbility
        return mPager != null && mPager.getCurrentItem() == ADVANCED_PANEL;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {                                                    //onOptionsItemSelected
        switch (item.getItemId()) {
        case R.id.clear_history:
            mHistory.clear();
            mLogic.onClear();
            break;

        case R.id.basic:
            if (!getBasicVisibility() && mPager != null) {
                mPager.setCurrentItem(BASIC_PANEL, true);
            }
            break;

        case R.id.advanced:
            if (!getAdvancedVisibility() && mPager != null) {
                mPager.setCurrentItem(ADVANCED_PANEL, true);
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {                                                //onSaveInstanceState
        super.onSaveInstanceState(state);
        if (mPager != null) {
            state.putInt(STATE_CURRENT_VIEW, mPager.getCurrentItem());
        }
    }

    @Override
    public void onPause() {                                                                           //onPause
        super.onPause();
        mLogic.updateHistory();
        mPersist.setDeleteMode(mLogic.getDeleteMode());
        mPersist.save();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {                                              //onKeyDown
        if (keyCode == KeyEvent.KEYCODE_BACK && getAdvancedVisibility()
                && mPager != null) {
            mPager.setCurrentItem(BASIC_PANEL);
            return true;
        } else {
            return super.onKeyDown(keyCode, keyEvent);
        }
    }

    static void log(String message) {                                                                 //log
        if (LOG_ENABLED) {
            Log.v(LOG_TAG, message);
        }
    }

    @Override
    public void onChange() {
        invalidateOptionsMenu();
    }                                             //onChange

    @Override
    public void onDeleteModeChange() {                                                    //onDeleteModeChange
        updateDeleteMode();
    }

    class PageAdapter extends PagerAdapter {                                         //PageAdapter
        private View mSimplePage;
        private View mAdvancedPage;

        public PageAdapter(ViewPager parent) {                                         //PageAdapter
            final LayoutInflater inflater = LayoutInflater.from(parent
                    .getContext());
            final View simplePage = inflater.inflate(R.layout.simple_pad,
                    parent, false);
            final View advancedPage = inflater.inflate(R.layout.advanced_pad,
                    parent, false);
            mSimplePage = simplePage;
            mAdvancedPage = advancedPage;

            final Resources res = getResources();
            final TypedArray simpleButtons = res
                    .obtainTypedArray(R.array.simple_buttons);
            for (int i = 0; i < simpleButtons.length(); i++) {
                setOnClickListener(simplePage,
                        simpleButtons.getResourceId(i, 0));
            }
            simpleButtons.recycle();

            final TypedArray advancedButtons = res
                    .obtainTypedArray(R.array.advanced_buttons);
            for (int i = 0; i < advancedButtons.length(); i++) {
                setOnClickListener(advancedPage,
                        advancedButtons.getResourceId(i, 0));
            }
            advancedButtons.recycle();

            final View clearButton = simplePage.findViewById(R.id.clear);
            if (clearButton != null) {
                mClearButton = clearButton;
            }

            final View backspaceButton = simplePage.findViewById(R.id.del);
            if (backspaceButton != null) {
                mBackspaceButton = backspaceButton;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }                                              //getCount

        @Override
        public void startUpdate(View container) {                                       //startUpdate
        }

        @Override
        public Object instantiateItem(View container, int position) {                        //instantiateItem
            final View page = position == 0 ? mSimplePage : mAdvancedPage;
            ((ViewGroup) container).addView(page);
            return page;
        }

        @Override
        public void destroyItem(View container, int position, Object object) {                     //destroyItem
            ((ViewGroup) container).removeView((View) object);
        }

        @Override
        public void finishUpdate(View container) {                                                  //finishUpdate
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }       //isViewFromObject

        @Override
        public Parcelable saveState() {
            return null;
        }                                               //Parcelable

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {                      //restorState
        }

    }
}
