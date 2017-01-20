package com.xkuznetsova.shiftedgrid.component;

/**
 * @author kuznetsova
 */

import android.content.Context;
import android.graphics.PointF;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;

/**
 * Places items' views in a two dimensional grid with additional left offset
 * for every second row. Supports header elements.
 *
 * @author kuznetsova
 */
public class ShiftedGridLayoutManager extends RecyclerView.LayoutManager {

    private static final int NO_HEADER = -1;

    /* Scroll directions */
    private static final int DIRECTION_NONE = 0;
    private static final int DIRECTION_UP = 1;
    private static final int DIRECTION_DOWN = 2;

    /* Consistent size applied to all child views */
    private int decoratedChildWidth;
    private int decoratedChildHeight;
    private int decoratedHeaderHeight;
    private int decoratedFooterHeight;

    /* Top left position on the screen */
    private int firstVisiblePosition;

    /* Columns number */
    private int columnCount;

    /* The offset that is applied to every second row*/
    private int offsetInPixels;
    private boolean forceClearOffset;

    /* Total items count on the screen at the moment */
    private int totalVisibleItems;

    /* Header indices mapped to number of items under them */
    private SparseIntArray datasetInfo = new SparseIntArray();
    /* Footer flag */
    private boolean footerExists;

    private boolean allItemsFitInScreen;
    private int totalItemsCount = -1;

    /**
     * @param columnCount - number of columns in grid
     * @param offsetInPixels - size of additional left offset
     */
    public ShiftedGridLayoutManager(int columnCount, int offsetInPixels) {
        this.columnCount = columnCount;
        this.offsetInPixels = offsetInPixels;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public boolean checkLayoutParams(RecyclerView.LayoutParams lp) {
        return lp != null && LayoutParams.class.isInstance(lp);
    }

    public LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        return new LayoutParams(lp.height);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        removeAllViews();
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        decoratedChildWidth = getHorizontalSpaceForItemsRow() / columnCount; // do this here because in onMeaure it doesn't work correctly

        // Scrap all views if dataset is empty
        if (state.getItemCount() == 0) {
            detachAndScrapAttachedViews(recycler);
            return;
        }

        if (state.isPreLayout() && getChildCount() == 0) { // do nothing if the list is empty on prelayout
            return;
        }

        achieveDatasetInfo(recycler, state);

        if (getChildCount() == 0) { // First layout - save item's and header's and footer's heights
            View scrap = recycler.getViewForPosition(0);
            if (IGridHeader.class.isInstance(scrap)) {
                addView(scrap); // measure header's height
                measureChildWithMargins(scrap, 0, 0);
                decoratedHeaderHeight = getDecoratedMeasuredHeight(scrap);
                detachAndScrapView(scrap, recycler);

                // measure item's height
                scrap = recycler.getViewForPosition(1);
            }

            if (IGridItem.class.isInstance(scrap)) {
                addView(scrap);
                measureChildWithMargins(scrap, 0, 0);
                decoratedChildHeight = getDecoratedMeasuredHeight(scrap);
                // for prompt
                detachAndScrapView(scrap, recycler);
            }

            if (getItemCount() != 0) {
                scrap = recycler.getViewForPosition(getItemCount() - 1); // footer view
                if (IGridFooter.class.isInstance(scrap)) {
                    addView(scrap); // measure footer's height
                    measureChildWithMargins(scrap, 0, 0);
                    decoratedFooterHeight = getDecoratedMeasuredHeight(scrap);
                    detachAndScrapView(scrap, recycler);
                }
            }
        }

        int childTopCoord;
        if (getChildCount() == 0) { // First or empty layout
            firstVisiblePosition = 0;
            childTopCoord = 0;
        } else { // Adapter data set changes
            View topChild = getChildAt(0);
            if (forceClearOffset) {
                childTopCoord = 0;
                forceClearOffset = false;
            } else {
                childTopCoord = getDecoratedTop(topChild);
            }

            if (!state.isPreLayout()) {
                childTopCoord = fixChildTopCoord(childTopCoord);
            }

            fixFirstVisiblePosition();
        }

        //Clear all attached views into the recycle bin
        detachAndScrapAttachedViews(recycler);

        //Fill the grid for the initial layout of views
        fillGrid(DIRECTION_NONE, childTopCoord, recycler, state, 0);
    }

    private int fixChildTopCoord(int childTopCoord) {
        int spaceForWholeDataset = countSpaceForTotalDataset();
        if (getVerticalSpace() > spaceForWholeDataset) {
            // When data set is too small to scroll vertically, adjust vertical offset
            // and shift position to the first row
            childTopCoord = 0;
            firstVisiblePosition = 0;
        } else if (childTopCoord < 0) {
            // If top coord is too far above for the dataset - shift it down
            int availableSpace = Math.abs(childTopCoord) + getVerticalSpace();
            if (availableSpace > spaceForWholeDataset) {
                childTopCoord = -(Math.abs(childTopCoord) - (availableSpace - spaceForWholeDataset));
            }
        }
        return childTopCoord;
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0) { // empty RecyclerView - return 0
            return 0;
        }

        View topView = getChildAt(0);
        View bottomView = getChildAt(getChildCount() - 1);

        int topOfTopView = getDecoratedTop(topView);
        int bottomOfBottomView = getDecoratedBottom(bottomView);

        int consumedSpace = bottomOfBottomView - topOfTopView;
        if (getChildCount() == getItemCount() && consumedSpace < getVerticalSpace()) {
            return 0;
        }

        int topViewPosition = ((IGridElement) topView).getPosition();
        int bottomViewPosition = ((IGridElement) bottomView).getPosition();

        boolean topBoundReached = topViewPosition == 0;
        boolean bottomBoundReached = bottomViewPosition >= state.getItemCount() - 1;

        int delta;

        if (dy > 0) { // content scrolls to the top
            if (bottomBoundReached) {
                int bottomOffset;
                if (bottomOfBottomView <= getHeight() - getPaddingBottom()) {
                    bottomOffset = getVerticalSpace() - getDecoratedBottom(bottomView) + getPaddingBottom();
                } else {
                    bottomOffset = getVerticalSpace() - (getDecoratedBottom(bottomView) + decoratedChildHeight)
                            + getPaddingBottom();
                }
                delta = Math.max(-dy, bottomOffset);
            } else {
                delta = -dy;
            }
        } else { // content scrolls to the bottom
            if (topBoundReached) {
                int topOffset = -getDecoratedTop(topView) + getPaddingTop();
                delta = Math.min(-dy, topOffset);
            } else {
                delta = -dy;
            }
        }
        offsetChildrenVertical(delta);

        int absDelta = Math.abs(delta);
        if (dy > 0) {
            if (getDecoratedBottom(topView) < 0 && !bottomBoundReached) {
                fillGrid(DIRECTION_DOWN, recycler, state, absDelta);
            } else if (!bottomBoundReached) {
                fillGrid(DIRECTION_NONE, recycler, state, absDelta);
            }
        } else {
            if (getDecoratedTop(topView) > 0 && !topBoundReached) {
                fillGrid(DIRECTION_UP, recycler, state, absDelta);
            } else if (!topBoundReached) {
                fillGrid(DIRECTION_NONE, recycler, state, absDelta);
            }
        }

        return -delta;
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        if (getChildCount() == 0) {
            return;
        }

        LinearSmoothScroller scroller = new LinearSmoothScroller(recyclerView.getContext()) {
            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                int intermediateHeaders = 0;
                int verticalItemsDelta = 0;

                int targetHeaderId = findHeaderId(targetPosition);
                int currentHeaderId = findHeaderId(firstVisiblePosition);

                int lowerHeader = Math.max(targetPosition, firstVisiblePosition);
                int upperHeader = Math.min(targetPosition, firstVisiblePosition);

                // go through all intermediate groups
                for (int headerIndex = datasetInfo.indexOfKey(upperHeader) + 1;
                     headerIndex < datasetInfo.indexOfKey(lowerHeader); headerIndex++) {
                    int itemsUnderHeader = datasetInfo.get(datasetInfo.keyAt(headerIndex));
                    verticalItemsDelta += itemsUnderHeader / columnCount;
                    intermediateHeaders++;
                }

                int curPosInGroup = firstVisiblePosition - currentHeaderId;
                int targetPosInGroup = targetPosition - targetHeaderId;
                if (targetHeaderId > currentHeaderId) { // target is lower than current
                    int itemsUnderCurrentHeader = datasetInfo.get(currentHeaderId);

                    verticalItemsDelta += ((itemsUnderCurrentHeader - curPosInGroup) / columnCount
                            + targetPosInGroup / columnCount);
                    intermediateHeaders++;
                } else { // target is upper
                    int itemsUnderTargetHeader = datasetInfo.get(currentHeaderId);

                    verticalItemsDelta += curPosInGroup / columnCount + (itemsUnderTargetHeader - targetPosInGroup) / columnCount;
                    intermediateHeaders++;
                    intermediateHeaders = -intermediateHeaders;

                    verticalItemsDelta = -verticalItemsDelta;
                }

                return new PointF(0, verticalItemsDelta * decoratedChildHeight + intermediateHeaders * decoratedHeaderHeight + (footerExists ? decoratedFooterHeight : 0));
            }
        };

        scroller.setTargetPosition(position);
        startSmoothScroll(scroller);
    }

    @Override
    public void scrollToPosition(int position) {
        if (position >= getItemCount()) {
            return;
        }

        // Ignore current scroll offset
        forceClearOffset = true;
        // Set requested position as first visible
        firstVisiblePosition = position;
        // Trigger a new view layout
        requestLayout();
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
        return false;
    }

    private void fillGrid(int direction, RecyclerView.Recycler recycler, RecyclerView.State state, int dy) {
        fillGrid(direction, 0, recycler, state, dy);
    }

    private void fillGrid(int direction, int top, RecyclerView.Recycler recycler, RecyclerView.State state, int dy) {
        fixFirstVisiblePosition();

        SparseArray<View> viewCache = new SparseArray<>(getChildCount());
        int startLeftOffset = getPaddingLeft();
        int startTopOffset = getPaddingTop() + top;

        if (getChildCount() != 0) {
            final View topView = getChildAt(0);
            startTopOffset = getDecoratedTop(topView) + getPaddingTop();

            // Cache all views by their existing position
            for (int i = 0; i < getChildCount(); i++) {
                int position = positionOfIndex(i);
                final View child = getChildAt(i);
                viewCache.put(position, child);
            }

            // Temporarily detach all views.
            for (int i = 0; i < viewCache.size(); i++) {
                detachView(viewCache.valueAt(i));
            }

            switch (direction) {
                case DIRECTION_UP:
                    startTopOffset = getPaddingTop() + countTopOffsetDeltaUp(getDecoratedTop(topView));
                    break;
                case DIRECTION_DOWN:
                    startTopOffset = getPaddingTop() + countTopOffsetDeltaDown(getDecoratedTop(topView));
                    break;
                default:
                    // do nothing
            }
        }

        // Layout visible items
        int leftOffset = startLeftOffset;
        int topOffset = startTopOffset;

        int index = 0;
        int visibleItems = countCurrentVisibleItemCount(topOffset);

        for (int i = 0; i < visibleItems; i++) {
            int nextPosition = positionOfIndex(index++);

            if (nextPosition < 0 || nextPosition >= state.getItemCount()) {
                //Item space beyond the data set, don't attempt to add a view
                break;
            }

            // Layout this position
            View currentView = viewCache.get(nextPosition);

            if (currentView == null) {
                currentView = recycler.getViewForPosition(nextPosition);

                if (IGridHeader.class.isInstance(currentView)) {
                    if (nextPosition != firstVisiblePosition) { // header must be lower than previous items
                        topOffset += decoratedChildHeight;
                    }
                    layoutHeaderView(currentView, getPaddingLeft(), topOffset);
                } else if (IGridFooter.class.isInstance(currentView)) {
                    topOffset += decoratedChildHeight;
                    layoutFooterView(currentView, getPaddingLeft(), topOffset);
                } else { // item
                    int positionInGroup;
                    int headerId = ((IGridItem) currentView).getHeaderIndex();

                    if (datasetInfo.size() == 0 || headerId == NO_HEADER) { // headers disabled
                        positionInGroup = nextPosition;
                    } else {
                        positionInGroup = nextPosition - (headerId + 1);
                    }

                    if (positionInGroup % columnCount == 0) { // first elem in a row
                        if (nextPosition != firstVisiblePosition) {
                            if (positionInGroup == 0) {
                                topOffset += decoratedHeaderHeight;
                            } else {
                                topOffset += decoratedChildHeight;
                            }
                        }
                        leftOffset = startLeftOffset;
                        if ((positionInGroup / columnCount) % 2 != 0) { // first elem in every second row
                            leftOffset += offsetInPixels;
                        }
                    } else {
                        leftOffset += decoratedChildWidth;
                    }

                    layoutItemView(currentView, leftOffset, topOffset);
                }

            } else {
                // Re-attach the cached view at its new index
                attachView(currentView);
                viewCache.remove(nextPosition);

                topOffset = getDecoratedTop(currentView);
                leftOffset = getDecoratedLeft(currentView);
            }

        }

        totalVisibleItems = index - 1;

        // Recycle all views that are not reattached
        for (int i=0; i < viewCache.size(); i++) {
            final View removingView = viewCache.valueAt(i);
            recycler.recycleView(removingView);
        }

    }

    private int countTopOffsetDeltaDown(int topOffset) {
        int position = firstVisiblePosition;

        int lastHeight = getItemHeight(firstVisiblePosition);
        int k = topOffset + lastHeight;

        while (k < 0) {
            lastHeight = getItemHeight(position);
            k += lastHeight;
            position += countPositionOffset(DIRECTION_DOWN, position);
        }

        k -= lastHeight;
        firstVisiblePosition = position;
        fixFirstVisiblePosition();

        return k;
    }

    private int countTopOffsetDeltaUp(int topOffset) {
        int k = topOffset;
        int position = firstVisiblePosition;

        while (k > 0) {
            if (position == 0) {
                firstVisiblePosition = 0;
                return k;
            }
            position += countPositionOffset(DIRECTION_UP, position);
            k -= getItemHeight(position);
        }

        firstVisiblePosition = position;

        return k;
    }

    private boolean isHeader(int position) {
        return datasetInfo.get(position, NO_HEADER) >= 0;
    }

    private boolean isFooter(int position) {
        return footerExists && position == getItemCount() - 1;
    }

    private int getItemHeight(int position) {
        if (isHeader(position)) {
            return decoratedHeaderHeight;
        }
        if (isFooter(position)) {
            return decoratedFooterHeight;
        }
        return decoratedChildHeight;
    }

    private void layoutItemView(View itemView, int leftOffset, int topOffset) {
        addView(itemView);

        measureChildWithMargins(itemView, 0, 0);
        layoutDecorated(itemView, leftOffset, topOffset,
                leftOffset + decoratedChildWidth,
                topOffset + decoratedChildHeight);
    }

    private void layoutHeaderView(View view, int leftOffset, int topOffset) {
        addView(view);

        LayoutParams lp = (LayoutParams) view.getLayoutParams();

        lp.width = LayoutParams.MATCH_PARENT;
        lp.height = LayoutParams.WRAP_CONTENT;

        measureChildWithMargins(view, 0, 0);
        layoutDecorated(view, leftOffset, topOffset,
                getHorizontalSpaceForHeaderOrFooter(),
                topOffset + decoratedHeaderHeight);
    }

    private void layoutFooterView(View view, int leftOffset, int topOffset) {
        addView(view);

        LayoutParams lp = (LayoutParams) view.getLayoutParams();

        lp.width = LayoutParams.MATCH_PARENT;
        lp.height = LayoutParams.WRAP_CONTENT;

        measureChildWithMargins(view, 0, 0);
        layoutDecorated(view, leftOffset, topOffset,
                getHorizontalSpaceForHeaderOrFooter(),
                topOffset + decoratedFooterHeight);
    }

    /*
    * If the new item count in an adapter is much smaller than it was before,
    * move firstVisiblePosition to correct place.
    *
    * If firstVisiblePosition is not the first in a row move it to the first in a row
    */
    private void fixFirstVisiblePosition() {
        if (firstVisiblePosition < 0) {
            firstVisiblePosition = 0;
            return;
        }

        int maxFirstIndex = getItemCount() - (getTotalVisibleItems() - 1);

        if (maxFirstIndex < 0) {
            // we now have less items in adapter then there were on the screen
            // in the previous layout pass - go to the beginning of the list
            firstVisiblePosition = 0;
            return;
        }

        if (firstVisiblePosition > maxFirstIndex) {
            firstVisiblePosition = maxFirstIndex;
        }

        // correct first visible position (if it is not first in row)
        if (datasetInfo.size() == 0) {
            int res = firstVisiblePosition % columnCount;
            if (res != 0) {
                firstVisiblePosition -= res;
            }
        } else {
            int headerId = findHeaderId(firstVisiblePosition);
            int posInGroup = firstVisiblePosition - headerId - 1;
            int res = posInGroup % columnCount;
            if (headerId != firstVisiblePosition && res != 0) {
                firstVisiblePosition -= res;
            }
        }
    }

    private int countCurrentVisibleItemCount(int topOffset) {
        int availableSpace = getVerticalSpace() + decoratedChildHeight - topOffset;

        if (datasetInfo.size() == 0) {
            return ((int) Math.ceil((float) availableSpace / decoratedChildHeight) + 1) * columnCount;
        }

        int index = firstVisiblePosition;
        int curItemsCount = 0;

        while (availableSpace > 0) {
            if (index == getItemCount() - 1 && footerExists) { // footer
                index++;
                break;
            } else if (datasetInfo.get(index, NO_HEADER) >= 0) { // header
                curItemsCount = datasetInfo.get(index);
                availableSpace -= decoratedHeaderHeight;
                index++;
            } else { // item
                if (curItemsCount == 0) {
                    curItemsCount = datasetInfo.get(findHeaderId(index));
                }
                availableSpace -= decoratedChildHeight;
                if (curItemsCount > columnCount) {
                    index += columnCount;
                    curItemsCount -= columnCount;
                } else {
                    index += curItemsCount;
                }
            }
        }

        return index - firstVisiblePosition;
    }

    private int getTotalVisibleItems() {
        if (datasetInfo.size() == 0) {
            return (int) Math.ceil((float) getVerticalSpace() / decoratedChildWidth * columnCount);
        }
        return totalVisibleItems;
    }

    // Map view index to adapter's position
    private int positionOfIndex(int childIndex) {
        return firstVisiblePosition + childIndex;
    }

    private int getVerticalSpace() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    private int getHorizontalSpaceForItemsRow() {
        return getHorizontalSpaceForHeaderOrFooter() - offsetInPixels;
    }

    private int getHorizontalSpaceForHeaderOrFooter() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    private int countPositionOffset(int direction, int startPosition) {
        if (direction == DIRECTION_NONE) {
            return 0;
        }

        if (datasetInfo.size() == 0) { // no headers
            return direction == DIRECTION_DOWN ? columnCount : -columnCount;
        }

        boolean isHeader = datasetInfo.indexOfKey(startPosition) >= 0;

        if (direction == DIRECTION_DOWN) {
            if (isHeader) {
                return 1;
            } else {
                for (int i = 1; i < columnCount; i++) {
                    if (datasetInfo.indexOfKey(startPosition + i) >= 0) { // header within next columnCount - 1 elems
                        return i;
                    }
                }
                return columnCount;
            }
        } else { // direction up
            if (isHeader) { // current is header
                int prevHeaderIndex = datasetInfo.indexOfKey(startPosition) - 1;
                if (prevHeaderIndex < 0) {
                    return 0;
                }
                int prevItemsNumber = datasetInfo.get(datasetInfo.keyAt(prevHeaderIndex));
                int rest = prevItemsNumber % columnCount;
                if (rest == 0) {
                    return -columnCount;
                } else {
                    return -rest;
                }
            } else { // current is item
                if (datasetInfo.indexOfKey(startPosition - 1) < 0) { // previous is item
                    return -columnCount;
                } else { // previous is header
                    return -1;
                }
            }
        }
    }

    private void achieveDatasetInfo(RecyclerView.Recycler recycler, RecyclerView.State state) {
        datasetInfo.clear();
        footerExists = false;

        int curHeaderPosition = NO_HEADER;
        for (int i = 0; i < state.getItemCount(); i++) {
            View view = recycler.getViewForPosition(i);
            if (IGridHeader.class.isInstance(view)) {
                curHeaderPosition = i;
                datasetInfo.put(curHeaderPosition, 0);
            } else if (IGridItem.class.isInstance(view)) {
                if (curHeaderPosition != NO_HEADER) {
                    datasetInfo.put(curHeaderPosition, datasetInfo.get(curHeaderPosition) + 1);
                }
            } else if (IGridFooter.class.isInstance(view)) {
                footerExists = true;
            }
        }
    }

    private int countSpaceForTotalDataset() {
        int totalSpace;
        if (datasetInfo.size() == 0) { // no headers
            totalSpace = getChildCount() * decoratedChildHeight;
        } else {
            totalSpace = datasetInfo.size() * decoratedHeaderHeight;
            for (int i = 0; i < datasetInfo.size(); i++) {
                int key = datasetInfo.keyAt(i);
                totalSpace += (Math.ceil((float) datasetInfo.get(key) / columnCount) * decoratedChildHeight);
            }
        }

        if (footerExists) {
            totalSpace += decoratedFooterHeight;
        }

        return totalSpace;
    }

    private int findHeaderId(int itemId) {
        if (datasetInfo.indexOfKey(itemId) >= 0) {
            return itemId;
        }

        for (int i = 0; i + 1 < datasetInfo.size(); i++) {
            if (datasetInfo.keyAt(i) < itemId && datasetInfo.keyAt(i + 1) > itemId) {
                return datasetInfo.keyAt(i);
            }
        }

        return datasetInfo.keyAt(datasetInfo.size() - 1);
    }

    private class LayoutParams extends RecyclerView.LayoutParams {

        LayoutParams(int height) {
            // use the same width for all items
            super(decoratedChildWidth, height);
        }
    }

}
