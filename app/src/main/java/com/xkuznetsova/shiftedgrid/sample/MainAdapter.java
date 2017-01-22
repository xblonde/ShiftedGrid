package com.xkuznetsova.shiftedgrid.sample;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * @author kuznetsova
 */

class MainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_HEADER = 1;

    private List<String> items;
    private List<Integer> headerPositions;

    private static class ItemViewHolder extends RecyclerView.ViewHolder {

        ItemViewHolder(View itemView) {
            super(itemView);
        }
    }

    private static class HeaderViewHolder extends RecyclerView.ViewHolder {

        HeaderViewHolder(View headerView) {
            super(headerView);
        }
    }

    MainAdapter(List<String> items, List<Integer> headerPositions) {
        super();
        this.items = items;
        this.headerPositions = headerPositions;
    }

    @Override
    public int getItemViewType(int position) {
        if (headerPositions.contains(position)) {
            return TYPE_HEADER;
        }
        return TYPE_ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            return new ItemViewHolder(new ItemView(parent.getContext()));
        } else { // header
            return new HeaderViewHolder(new HeaderView(parent.getContext()));
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (HeaderViewHolder.class.isInstance(holder)) {
            HeaderView headerView = (HeaderView) ((HeaderViewHolder) holder).itemView;
            headerView.setHeaderText(items.get(position));
            headerView.setPosition(position);
        } else { // item
            ItemView itemView = (ItemView) ((ItemViewHolder) holder).itemView;
            itemView.setItemNameText(items.get(position));
            itemView.setPosition(position);
            itemView.setHeaderIndex(findHeaderId(position));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private int findHeaderId(int itemPosition){
        for (int i = itemPosition - 1; i >= 0; i--) {
            if (headerPositions.contains(i)) {
                return i;
            }
        }
        return 0;
    }

}
