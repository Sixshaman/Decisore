package com.sixshaman.advancedunforgetter.utils;

import java.util.concurrent.LinkedBlockingQueue;

//This is the only class that can write to config files
public class EventDispatcher
{
    private enum EventType
    {
        EVENT_TYPE_ADD,
        EVENT_TYPE_UPDATE,
        EVENT_TYPE_FINISH
    }

    private class Event
    {
        private EventType mEventType;
    }

    private class AddEvent extends Event
    {

    }

    //More events here

    private String mSchedulerFilePath;
    private String mListFilePath;
    private String mArchiveFilePath;

    private LinkedBlockingQueue<Event> mEventQueue;

    public EventDispatcher(String schedulerFilePath, String listFilePath, String archiveFilePath)
    {
        mSchedulerFilePath = schedulerFilePath;
        mListFilePath      = listFilePath;

        mEventQueue = new LinkedBlockingQueue<Event>(); //???
    }

    private void dispatchEvents()
    {
        try
        {
            for(Event event: mEventQueue)
            {
                switch(event.mEventType)
                {
                    case EVENT_TYPE_ADD:
                    {
                        AddEvent addEvent = (AddEvent)mEventQueue.take();
                        dispatchAddEvent(addEvent);
                        break;
                    }

                    case EVENT_TYPE_UPDATE:
                        break;

                    case EVENT_TYPE_FINISH:
                        break;
                }
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private void dispatchAddEvent(AddEvent addEvent)
    {
        LockedFile schedulerLockedFile = new LockedFile(mSchedulerFilePath);


    }
}
