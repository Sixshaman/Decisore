package com.sixshaman.decisore.utils;

import org.junit.Assert;
import org.junit.Test;


public class TwoSidedArrayListTest
{
    @Test
    public void get()
    {
        TwoSidedArrayList<Integer> twoSidedArrayList = new TwoSidedArrayList<>(3);
        Assert.assertEquals(twoSidedArrayList.size(), 0);

        twoSidedArrayList.addFront(1);
        twoSidedArrayList.addBack(2);
        twoSidedArrayList.addFront(3);
        twoSidedArrayList.addFront(4);
        twoSidedArrayList.addBack(5);
        twoSidedArrayList.addBack(6);

        twoSidedArrayList.removeFront();
        Assert.assertEquals(twoSidedArrayList.get(0), Integer.valueOf(3));
        Assert.assertEquals(twoSidedArrayList.get(1), Integer.valueOf(1));
        Assert.assertEquals(twoSidedArrayList.get(2), Integer.valueOf(2));
        Assert.assertEquals(twoSidedArrayList.get(3), Integer.valueOf(5));
        Assert.assertEquals(twoSidedArrayList.get(4), Integer.valueOf(6));
    }

    @Test
    public void size()
    {
        TwoSidedArrayList<Integer> twoSidedArrayList = new TwoSidedArrayList<>(3);
        Assert.assertEquals(twoSidedArrayList.size(), 0);

        twoSidedArrayList.addFront(1);
        Assert.assertEquals(twoSidedArrayList.size(), 1);

        twoSidedArrayList.addBack(2);
        Assert.assertEquals(twoSidedArrayList.size(), 2);

        twoSidedArrayList.removeFront();
        Assert.assertEquals(twoSidedArrayList.size(), 1);

        twoSidedArrayList.removeBack();
        Assert.assertEquals(twoSidedArrayList.size(), 0);
    }

    @Test
    public void addFront()
    {
        TwoSidedArrayList<Integer> twoSidedArrayList = new TwoSidedArrayList<>(3);

        twoSidedArrayList.addFront(1);
        Assert.assertEquals(twoSidedArrayList.getFront(), Integer.valueOf(1));

        twoSidedArrayList.addFront(2);
        Assert.assertEquals(twoSidedArrayList.getFront(), Integer.valueOf(2));

        twoSidedArrayList.addFront(3);
        Assert.assertEquals(twoSidedArrayList.getFront(), Integer.valueOf(3));

        twoSidedArrayList.addFront(4);
        Assert.assertEquals(twoSidedArrayList.getFront(), Integer.valueOf(4));

        twoSidedArrayList.addFront(5);
        Assert.assertEquals(twoSidedArrayList.getFront(), Integer.valueOf(5));
    }

    @Test
    public void addBack()
    {
        TwoSidedArrayList<Integer> twoSidedArrayList = new TwoSidedArrayList<>(3);

        twoSidedArrayList.addBack(1);
        Assert.assertEquals(twoSidedArrayList.getBack(), Integer.valueOf(1));

        twoSidedArrayList.addBack(2);
        Assert.assertEquals(twoSidedArrayList.getBack(), Integer.valueOf(2));

        twoSidedArrayList.addBack(3);
        Assert.assertEquals(twoSidedArrayList.getBack(), Integer.valueOf(3));

        twoSidedArrayList.addBack(4);
        Assert.assertEquals(twoSidedArrayList.getBack(), Integer.valueOf(4));

        twoSidedArrayList.addBack(5);
        Assert.assertEquals(twoSidedArrayList.getBack(), Integer.valueOf(5));
    }

    @Test
    public void removeFront()
    {
        TwoSidedArrayList<Integer> twoSidedArrayList = new TwoSidedArrayList<>(3);

        twoSidedArrayList.addBack(1);
        twoSidedArrayList.addBack(2);
        twoSidedArrayList.addFront(3);
        twoSidedArrayList.addFront(4);
        twoSidedArrayList.addBack(5);

        Assert.assertEquals(twoSidedArrayList.removeFront(), Integer.valueOf(4));
        Assert.assertEquals(twoSidedArrayList.removeFront(), Integer.valueOf(3));
        Assert.assertEquals(twoSidedArrayList.removeFront(), Integer.valueOf(1));
        Assert.assertEquals(twoSidedArrayList.removeFront(), Integer.valueOf(2));
        Assert.assertEquals(twoSidedArrayList.removeFront(), Integer.valueOf(5));

        Assert.assertEquals(twoSidedArrayList.size(), 0);
    }

    @Test
    public void removeBack()
    {
        TwoSidedArrayList<Integer> twoSidedArrayList = new TwoSidedArrayList<>(3);

        twoSidedArrayList.addBack(1);
        twoSidedArrayList.addBack(2);
        twoSidedArrayList.addFront(3);
        twoSidedArrayList.addFront(4);
        twoSidedArrayList.addBack(5);

        Assert.assertEquals(twoSidedArrayList.removeBack(), Integer.valueOf(5));
        Assert.assertEquals(twoSidedArrayList.removeBack(), Integer.valueOf(2));
        Assert.assertEquals(twoSidedArrayList.removeBack(), Integer.valueOf(1));
        Assert.assertEquals(twoSidedArrayList.removeBack(), Integer.valueOf(3));
        Assert.assertEquals(twoSidedArrayList.removeBack(), Integer.valueOf(4));

        Assert.assertEquals(twoSidedArrayList.size(), 0);
    }

    @Test
    public void isEmpty()
    {
        TwoSidedArrayList<Integer> twoSidedArrayList = new TwoSidedArrayList<>(3);
        Assert.assertTrue(twoSidedArrayList.isEmpty());

        twoSidedArrayList.addBack(1);
        Assert.assertFalse(twoSidedArrayList.isEmpty());

        twoSidedArrayList.addBack(2);
        Assert.assertFalse(twoSidedArrayList.isEmpty());

        twoSidedArrayList.removeFront();
        twoSidedArrayList.removeBack();
        Assert.assertTrue(twoSidedArrayList.isEmpty());
    }
}