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

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main);
        initView();
    }

    private void initView() {
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        List<String> items = new ArrayList<>();
        for(int i = 1; i <= 90; i++) {
            items.add("Item " + i);
        }

        recyclerView.setLayoutManager(new ShiftedGridLayoutManager(4, 64));
        recyclerView.setAdapter(new MainAdapter(items));
    }
}
