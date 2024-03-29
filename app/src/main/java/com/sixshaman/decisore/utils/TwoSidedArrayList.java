package com.sixshaman.decisore.utils;

import java.io.Serializable;
import java.util.*;

public class TwoSidedArrayList<T> extends AbstractList<T> implements List<T>, RandomAccess, Cloneable, Serializable
{
    private static final int INITIAL_CAPACITY = 16;

    private int freeIndexFront; //Always points to a free element and never to the same one as freeIndexBack
    private int freeIndexBack;  //Always points to a free element and never to the same one as freeIndexFront

    private Object[] data;

    public TwoSidedArrayList()
    {
        freeIndexFront = 0;
        freeIndexBack  = 1;

        data = new Object[INITIAL_CAPACITY];
    }

    TwoSidedArrayList(Collection<? extends T> c)
    {
        int capacity = c.size() + 2;
        data         = new Object[capacity];

        Iterator<? extends T> it = c.iterator();

        int pos = 1; //One free space, because freeIndexFront should always point out onto a free space
        while(it.hasNext())
        {
            data[pos++] = it.next();
        }

        freeIndexFront = 0;
        freeIndexBack  = capacity - 1;
    }

    TwoSidedArrayList(int initialCapacity)
    {
        int capacity = Math.max(initialCapacity, 2);
        data         = new Object[capacity];

        freeIndexFront = 0;
        freeIndexBack  = 1;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get(int index)
    {
        if(isEmpty())
        {
            return null;
        }

        int realIndex = (freeIndexFront + index + 1) % data.length;
        return (T)data[realIndex];
    }

    @Override
    @SuppressWarnings("unchecked")
    public T remove(int index)
    {
        if(isEmpty())
        {
            return null;
        }

        int realIndex = (freeIndexFront + index + 1) % data.length;
        T returnValue = (T)data[realIndex];

        freeIndexBack = (freeIndexBack + data.length - 1) % data.length;
        for(int i = realIndex; i < freeIndexBack; i = (i + 1) % data.length)
        {
            int nextIndex = (i + 1) % data.length;
            data[i] = data[nextIndex];
        }

        return returnValue;
    }

    @Override
    public int size()
    {
        return (data.length + freeIndexBack - freeIndexFront - 1) % data.length;
    }

    public void addFront(T element)
    {
        if(freeIndexFront == freeIndexBack)
        {
            reallocate();
        }

        data[freeIndexFront] = element;
        freeIndexFront = (data.length + freeIndexFront - 1) % data.length;
    }

    public void addBack(T element)
    {
        if(freeIndexFront == freeIndexBack)
        {
            reallocate();
        }

        data[freeIndexBack] = element;
        freeIndexBack = (freeIndexBack + 1) % data.length;
    }

    @SuppressWarnings("unchecked")
    public T getFront()
    {
        if(isEmpty())
        {
            return null;
        }

        int elementIndex = (freeIndexFront + 1) % data.length;
        return (T)data[elementIndex];
    }

    @SuppressWarnings("unchecked")
    public T getBack()
    {
        if(isEmpty())
        {
            return null;
        }

        int elementIndex = (data.length + freeIndexBack - 1) % data.length;
        return (T)data[elementIndex];
    }

    public T removeFront()
    {
        if(isEmpty())
        {
            return null;
        }

        T element = getFront();
        freeIndexFront = (freeIndexFront + 1) % data.length;

        return element;
    }

    public T removeBack()
    {
        if(isEmpty())
        {
            return null;
        }

        T element = getBack();
        freeIndexBack = (data.length + freeIndexBack - 1) % data.length;

        return element;
    }

    @Override
    public boolean isEmpty()
    {
        return (freeIndexFront + 1) % data.length == freeIndexBack;
    }

    private void reallocate()
    {
        int capacity     = data.length;
        int newCapacity  = capacity * 2 + 2;
        Object[] newData = new Object[newCapacity];

        int currentDataSize = size();
        for(int i = 0; i < currentDataSize; i++)
        {
            int pos = (freeIndexFront + i + 1) % capacity;
            newData[i + 1] = data[pos];
        }

        freeIndexFront = 0;
        freeIndexBack  = currentDataSize + 1;

        data = newData;
    }
}
