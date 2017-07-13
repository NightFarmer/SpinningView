package com.nightfarmer.spinningview;

import android.view.View;
import android.view.ViewGroup;

/**
 * Created by zhangfan on 17-7-11.
 */

public abstract class SpinningAdapter {

    public abstract View onCreateItemView(ViewGroup parent, int position);

    public abstract int getItemCount();
}
