package com.xkuznetsova.shiftedgrid.sample;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.xkuznetsova.shiftedgrid.R;
import com.xkuznetsova.shiftedgrid.component.IGridHeader;

/**
 * @author kuznetsova
 */

public class HeaderView extends FrameLayout implements IGridHeader {

    private int position;
    private TextView textView;

    public HeaderView(Context context) {
        this(context, null);
    }

    public HeaderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.view_header, this);
        textView = (TextView) findViewById(R.id.tv_header_text);
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public void setPosition(int position) {
        this.position = position;
    }

    public void setHeaderText(String nameText) {
        textView.setText(nameText);
    }

}
