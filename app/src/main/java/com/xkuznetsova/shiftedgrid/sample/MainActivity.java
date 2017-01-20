package com.xkuznetsova.shiftedgrid.sample;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;

import com.xkuznetsova.shiftedgrid.R;
import com.xkuznetsova.shiftedgrid.component.ShiftedGridLayoutManager;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kuznetsova
 */

public class MainActivity extends Activity {

    private static final String ITEM_TEXT = "Item ";
    private static final int ITEMS_NUMBER = 100;
    private static final int COLUMNS_NUMBER = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main);
        initView();
    }

    private void initView() {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        List<String> items = new ArrayList<>();
        for(int i = 0; i < ITEMS_NUMBER; i++) {
            items.add(ITEM_TEXT + (i + 1));
        }

        recyclerView.setLayoutManager(new ShiftedGridLayoutManager(COLUMNS_NUMBER,
                getResources().getDimensionPixelSize(R.dimen.offset_size)));
        recyclerView.setAdapter(new MainAdapter(items));
    }
}
