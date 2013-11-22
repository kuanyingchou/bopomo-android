package com.example.android.softkeyboard;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

public class MyCandidateView extends LinearLayout {
    
    private InputMethodService inputMethodService;
    private List<String> list = new ArrayList<String>();
    private LinearLayout listView;
    
    public MyCandidateView(final Context context) {
        super(context);
        
        setBackgroundColor(Color.GRAY);

        listView = new LinearLayout(context);
        
        final TextView label = new TextView(context);
        final ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(5, 50);
        label.setLayoutParams(lp);
        label.setText(" ");
        addView(label);

        final HorizontalScrollView scroller = new HorizontalScrollView(context);
        scroller.addView(listView);
        addView(scroller);
      
    }
    
    public void setService(InputMethodService service) {
        inputMethodService = service;
    }
    
    public void setCandidates(List<String> c) {
        list = c == null? new ArrayList<String>(): c;
        listView.removeAllViews();
        for(final String s: list) {
            listView.addView(createElement(s));
        }
    }
    
    private View createElement(final String s) {
        final Button elmt = new Button(getContext());
        final ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(50, 50);
        elmt.setLayoutParams(lp);
        elmt.setTextSize(20);
        elmt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                System.out.println("choose "+s);
                final InputConnection ic = 
                        inputMethodService.getCurrentInputConnection();
                ic.setComposingText("", 0);
                ic.commitText(s, 1);
            }
        });
        elmt.setText(s);
        return elmt;
    }

    public void clear() {
        setCandidates(new ArrayList<String>());
    }
}
