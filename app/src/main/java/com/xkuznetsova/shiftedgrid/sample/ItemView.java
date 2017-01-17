package com.xkuznetsova.shiftedgrid.sample;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.xkuznetsova.shiftedgrid.R;
import com.xkuznetsova.shiftedgrid.component.IGridItem;

/**
 * @author kuznetsova
 */

public class ItemView extends FrameLayout implements IGridItem {

    private int headerIndex = IGridItem.NO_NEADER;
    private int position;
    private TextView textView;

    public ItemView(Context context) {
        this(context, null);
    }

    public ItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.view_item, this);
        textView = (TextView) findViewById(R.id.tv_item_name);
    }

    @Override
    public int getHeaderIndex() {
        return headerIndex;
    }

    @Override
    public void setHeaderIndex(int headerIndex) {
        this.headerIndex = headerIndex;
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public void setPosition(int position) {
        this.position = position;
    }

    public void setItemNameText(String nameText) {
        textView.setText(nameText);
    }
}
