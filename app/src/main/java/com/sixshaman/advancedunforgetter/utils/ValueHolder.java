package com.sixshaman.advancedunforgetter.utils;

public class ValueHolder<T>
{
    private T mValue;

    public ValueHolder(T value)
    {
        mValue = value;
    }

    public T getValue()
    {
        return mValue;
    }

    public void setValue(T value)
    {
        mValue = value;
    }
}
