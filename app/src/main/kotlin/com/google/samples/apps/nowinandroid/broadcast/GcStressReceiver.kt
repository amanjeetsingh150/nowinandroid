/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.nowinandroid.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import gcstress.gc.GCStress
import java.util.Random

class GcStressReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("GcStressReceiver", "Hello")
        // start the timer also at max priority
        // start thread to hammer heap memory at normal priority
        // start thread to hammer heap memory at normal priority
        val opt_capacity = 2000000
        val opt_maxsize = 256
        val opt_sleepTime = 100L
        val opt_samples = 300

        val hammer = GCHammer(opt_capacity, opt_maxsize)
        val hammerThrd = Thread(hammer, "GCStress Hammer")
        hammerThrd.isDaemon = true
        hammerThrd.start()

        // boost ourselves to max priority

        // boost ourselves to max priority
        val priority = Thread.MAX_PRIORITY
        Thread.currentThread().priority = priority
        val gcstress = GCStress(
            opt_sleepTime,
            opt_samples,
            "gcstress.csv"
        )

        val timerThrd = Thread(gcstress, "GCStress Timer")
        timerThrd.priority = priority
        timerThrd.isDaemon = true
        timerThrd.start()

        try {
            // wait for the timer to finish
            timerThrd.join()
        } catch (ie: InterruptedException) {
            /* no-op */
        }
    }

    internal class LinkedHashMapWithCapacity<K, V>(private val capacity: Int) :
        LinkedHashMap<K, V>(capacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<K, V>?): Boolean {
            return this.size > capacity
        }

        companion object {
            private const val serialVersionUID = 1L
        }
    }

    internal class GCHammer(private val capacity: Int, private val maxsize: Int) : Runnable {
        private val map: LinkedHashMapWithCapacity<Int, Any> = LinkedHashMapWithCapacity(capacity)
        private var stop = false

        fun stop() {
            stop = true
        }

        override fun run() {
            val rand = Random()
            while (!stop) {
                val key: Int = rand.nextInt(capacity)
                val size: Int = rand.nextInt(maxsize)
                var value = map[key] as ByteArray?
                if (value == null) {
                    // if the cache entry is empty, fill it
                    map[key] = ByteArray(size)
                } else {
                    // otherwise, remove it
                    value = map.remove(key) as ByteArray?
                }
            }
        }
    }
}