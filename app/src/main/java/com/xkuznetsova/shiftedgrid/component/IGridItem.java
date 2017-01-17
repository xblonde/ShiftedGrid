package com.xkuznetsova.shiftedgrid.component;

/**
 * @author kuznetsova
 */

public interface IGridItem extends IGridElement {

    int NO_NEADER = -1;

    int getHeaderIndex();
    void setHeaderIndex(int headerIndex);
}
