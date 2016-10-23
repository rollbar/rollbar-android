package com.rollbar.android;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

public class RollbarThread extends Thread {
    // default thread name if not provided
    private final String DEFAULT_ROLLBAR_THREAD_NAME = "RollbarThread";
    // que why we do not use fifo lifo deque???
    private final List<JSONObject> _queue;
    // notifier
    private final Notifier _notifier;
    // lock
    private final ReentrantLock _lock = new ReentrantLock();
    // condition for lock
    private final Condition _ready = _lock.newCondition();
    // thread stored name def
    private final String _rollbarThreadName;
    // get thread stored name defined at object creation
    public String getRollbarThreadName() {
        return _rollbarThreadName;
    }

    public RollbarThread(Notifier notifier) {
        // set notifier
        _notifier = notifier;
        // create que object
        _queue = new ArrayList<JSONObject>();
        // determine and store thread name
        // this is not perfect solution
        // as setName(String) can be called as it is finall public android thread method
        // but for now RollbarThread class is sealed from user...
        _rollbarThreadName = notifier.rollbarThreadName != null
                ? notifier.rollbarThreadName
                : DEFAULT_ROLLBAR_THREAD_NAME;
        // set thread name
        setName(_rollbarThreadName);
    }


    @Override
    public void run() {
        // log start
        Log.d(Rollbar.TAG, getRollbarThreadName() + " started...");
        // do main loop
        while (true) {
            try {
                // send
                postQue();
            } catch  // catch interruption
                    (InterruptedException e) {
                // on interrupt allow exit graceful
                // empty que
                emptyQueToFile();
                // log ending
                Log.d(Rollbar.TAG, getRollbarThreadName() + " finishing.");
                // restore interrupt state!
                interrupt();
                // allow thread dead
                return;
            }
        }
    }

    private void postQue() throws InterruptedException {
        // lock
        _lock.lock();
        // do work
        try {
            // await signal
            _ready.await();
            // if interrupted
            // check size
            if (_queue.size() > 0) {
                // log
                Log.d(Rollbar.TAG, getRollbarThreadName() + " posting que...");
                // make post object
                JSONArray items = new JSONArray(_queue);
                // post
                _notifier.postItems(items, null);
                // clear when we send data
                _queue.clear();
            }
        } finally {
            // interrupted or not, always release lock
            _lock.unlock();
        }
    }

    private void emptyQueToFile() {
        // lock
        _lock.lock();
        // do work
        try {
            // check size
            if (_queue.size() > 0) {
                //
                Log.d(Rollbar.TAG,  getRollbarThreadName() + " saving que...");
                // make save post object
                JSONArray items = new JSONArray(_queue);
                // Save all remaining items to disk because the process is
                // dying soon
                _notifier.writeItems(items);
                // then clear que
                _queue.clear();
            }
        } finally {
            // unlock
            _lock.unlock();
        }
    }

    public void queueItem(JSONObject item) {
        // lock
        _lock.lock();
        try {
            // log who is adding to which que
            Log.d(Rollbar.TAG, Thread.currentThread().getName() + " is adding data to " + getRollbarThreadName() + " que...");
            // try add
            _queue.add(item);
            // check we can signal
            if(_lock.isHeldByCurrentThread()) {
                // signal
                _ready.signal();
            }
        } finally {
            // unlock
            _lock.unlock();
        }
    }
}
