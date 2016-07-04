package com.yckir.bluetoothchat.recycler;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class RecyclerDivider extends RecyclerView.ItemDecoration{
    private final int mVerticalHeight;
    public RecyclerDivider(int verticalHeight){
        mVerticalHeight = verticalHeight;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        if(parent.getChildAdapterPosition(view) != parent.getAdapter().getItemCount() - 1){
            outRect.bottom = mVerticalHeight;
        }
    }
}
