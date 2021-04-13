package com.sixshaman.decisore.utils;

import java.io.Serializable;
import java.util.*;

public class TwoSidedArrayList<T> extends AbstractList<T> implements List<T>, RandomAccess, Cloneable, Serializable
{
    private static final int INITIAL_CAPACITY = 16;

    private int dataBegin;
    private int pastDataEnd;

    private Object[] data;

    TwoSidedArrayList()
    {
        dataBegin   = 0;
        pastDataEnd = 1 % (INITIAL_CAPACITY + 1);

        data = new Object[INITIAL_CAPACITY];
    }

    TwoSidedArrayList(Collection<? extends T> c)
    {
        int capacity = Math.max(c.size(), 1);
        data         = new Object[capacity];

        Iterator<? extends T> it = c.iterator();

        int pos = 0;
        while(it.hasNext())
        {
            data[pos++] = it.next();
        }

        dataBegin   = 0;
        pastDataEnd = (pos + 1) % (capacity + 1);
    }

    TwoSidedArrayList(int initialCapacity)
    {
        int capacity = Math.max(initialCapacity, 1);
        data         = new Object[capacity];

        dataBegin   = 0;
        pastDataEnd = 1 % (capacity + 1);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get(int index)
    {
        int realIndex = (dataBegin + index) % data.length;
        return (T)data[realIndex];
    }

    @Override
    public int size()
    {
        return (data.length + pastDataEnd - dataBegin) % data.length;
    }

    public void addFront(T element)
    {
        if(dataBegin == pastDataEnd)
        {
            reallocate();
        }

        dataBegin = (data.length + dataBegin - 1) % data.length;
        data[dataBegin] = element;
    }

    public void addBack(T element)
    {
        if(dataBegin == pastDataEnd)
        {
            reallocate();
        }

        pastDataEnd = (pastDataEnd + 1) % (data.length + 1);
        data[pastDataEnd] = element;
    }

    public T removeFront()
    {

    }

    public T removeBack()
    {

    }

    private void reallocate()
    {
        int capacity     = data.length;
        int newCapacity  = capacity * 2;
        Object[] newData = new Object[newCapacity];

        int currentDataSize = size();
        for(int i = 0; i < currentDataSize; i++)
        {
            int pos = (i + dataBegin) % capacity;
            newData[i] = data[pos];
        }

        dataBegin   = 0;
        pastDataEnd = currentDataSize;

        data = newData;
    }
}
