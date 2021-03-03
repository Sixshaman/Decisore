package com.sixshaman.advancedunforgetter.utils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//Singleton class to provide utilities, for example, hold an instance of Random
public class RandomUtils
{
    private final Random mRandomGenerator;

    //Creates a new RandomUtils instance
    private RandomUtils()
    {
        mRandomGenerator = new Random(LocalDateTime.now().getNano());
    }

    //Initialization-on-demand holder idiom
    private static class RandomUtilsHolder
    {
        static final RandomUtils INSTANCE = new RandomUtils();
    }

    //Gets the singleton instance
    public static RandomUtils getInstance()
    {
        return RandomUtilsHolder.INSTANCE;
    }

    //Returns a new number using normal distribution with mean as mean and mean * stdDevPart as standard deviation
    public long getRandomGauss(long mean, float stdDevPart)
    {
        double gaussMean   = (double)mean;
        double gaussStdDev = gaussMean * stdDevPart;

        double gaussDistribResult = (mRandomGenerator.nextGaussian() * gaussStdDev + gaussMean);
        return (long)gaussDistribResult;
    }

    public long getRandomUniform(long min, long max)
    {
        return mRandomGenerator.nextLong() % (max - min + 1) + min;
    }
}
