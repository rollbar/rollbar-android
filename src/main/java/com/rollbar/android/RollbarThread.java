package com.rollbar.android;

import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONArray;
import org.json.JSONObject;

public class RollbarThread extends Thread {
    private ArrayList<JSONObject> queue;
    private Notifier notifier;
    
    private final Lock lock = new ReentrantLock();
    private final Condition ready = lock.newCondition();
    
    public RollbarThread(Notifier notifier) {
        this.notifier = notifier;
        queue = new ArrayList<JSONObject>();
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
                    // most likely dying soon
                    notifier.writeItems(items);
                }
                return;
            } finally {
                // Unlock if locked
                try { lock.unlock(); } catch (Exception e) {}
            }
        }
    }
    
    public void queueItem(JSONObject item) {
        lock.lock();
        queue.add(item);
        ready.signal();
        lock.unlock();
    }
}
