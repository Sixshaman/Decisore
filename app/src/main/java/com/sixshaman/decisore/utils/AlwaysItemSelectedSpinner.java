package com.sixshaman.decisore.utils;

import android.content.Context;
import android.util.AttributeSet;

//The spinner which always calls onItemSelected, even if the same item is selected
public class AlwaysItemSelectedSpinner extends androidx.appcompat.widget.AppCompatSpinner
{
    //Have to use it because otherwise I won't be able to use super.setSelection for default call
    private int mLastPosition;

    public AlwaysItemSelectedSpinner(Context context, AttributeSet attributes)
    {
        super(context, attributes);

        mLastPosition = 0;
    }

    @Override
    public void setSelection(int position)
    {
        super.setSelection(position);

        if(mLastPosition == position)
        {
            OnItemSelectedListener itemSelectedListener = getOnItemSelectedListener();
            if(itemSelectedListener != null)
            {
                itemSelectedListener.onItemSelected(this, getSelectedView(), position, getSelectedItemId());
            }
        }

        mLastPosition = position;
    }
}
