package com.xkuznetsova.shiftedgrid.sample;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * @author kuznetsova
 */

public class MainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<String> items;

    private static class ItemViewHolder extends RecyclerView.ViewHolder {

        ItemViewHolder(View itemView) {
            super(itemView);
        }
    }

    public MainAdapter(List<String> items) {
        super();
        this.items = items;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ItemViewHolder(new ItemView(parent.getContext()));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ItemView itemView = (ItemView) ((ItemViewHolder) holder).itemView;
        itemView.setItemNameText(items.get(position));
        itemView.setPosition(position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
