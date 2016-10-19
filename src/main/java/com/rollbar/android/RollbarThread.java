package com.rollbar.android;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

public class RollbarThread extends Thread {

    private final String DEFAULT_ROLLBAR_THREAD_NAME = "RollbarThread";

    private final List<JSONObject> queue;
    private final Notifier notifier;
    
    private final Lock lock = new ReentrantLock();
    private final Condition ready = lock.newCondition();
    
    public RollbarThread(Notifier notifier) {
        this.notifier = notifier;
        queue = new ArrayList<JSONObject>();
        setName(notifier.rollbarThreadName !=null
                ? notifier.rollbarThreadName
                : DEFAULT_ROLLBAR_THREAD_NAME);
    }

    @Override
    public void run() {
        for (;;) {
            lock.lock();
            
            try {
                if (queue.isEmpty()) {
                    ready.await();
                }

                JSONArray items = new JSONArray(queue);
                queue.clear();

                lock.unlock();

                notifier.postItems(items, null);
            } catch (InterruptedException e) {
                if (!queue.isEmpty()) {
                    JSONArray items = new JSONArray(queue);
                    queue.clear();

                    // Save all remaining items to disk because the process is
                    // dying soon
                    notifier.writeItems(items);
                }

                break;
            }
        }

        Log.d(Rollbar.TAG, "Rollbar thread finishing.");
    }
    
    public void queueItem(JSONObject item) {
        lock.lock();
        queue.add(item);
        ready.signal();
        lock.unlock();
    }
}
