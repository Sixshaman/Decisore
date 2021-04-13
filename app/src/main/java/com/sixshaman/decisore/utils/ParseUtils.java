package com.sixshaman.decisore.utils;

public class ParseUtils
{
    public static int parseInt(String s, int defaultValue)
    {
        try
        {
            return Integer.parseInt(s);
        }
        catch(NumberFormatException e)
        {
            return defaultValue;
        }
    }
}
