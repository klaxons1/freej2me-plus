/*
	This file is part of FreeJ2ME.

	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	FreeJ2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/
*/
package org.recompile.mobile;

import java.util.concurrent.atomic.AtomicLong;

public class MIDletEnhancements 
{
    private static final AtomicLong curNanoTime = new AtomicLong(0);
    private static final AtomicLong curTimeMillis = new AtomicLong(0);
    private static long lastNanoTime = System.nanoTime();
    private static long lastMillisTime = System.currentTimeMillis();

    // Simulated time will start from the current system time, for better compatibility
    private static final long startMillisTime = lastMillisTime;
    private static final long startNanoTime = lastNanoTime;

    public static void drawSleep(long millis) throws InterruptedException
    {
        if (Mobile.unlockFramerateHack == 0 && !MobilePlatform.pressedKeys[20]) { Thread.sleep(millis); } 
        else { Thread.sleep(1); }
    }

    public static void sleep(long millis) throws InterruptedException
    {
        if (Mobile.unlockFramerateHack == 0 && !MobilePlatform.pressedKeys[20]) { Thread.sleep(millis); } 
        else { Thread.sleep(1); }
    }

    public static long currentTimeMillis() 
    {
        long now = System.currentTimeMillis();
        long elapsedMillis = now - lastMillisTime;

        if (MobilePlatform.pressedKeys[20])
        {
            float multiplier = Mobile.fastForwardMultiplier;
            if (multiplier <= 0.0f) { multiplier = 20.0f; }
            curTimeMillis.addAndGet((long) (elapsedMillis * multiplier));
        }
        else if (Mobile.unlockFramerateHack > 2) { curTimeMillis.addAndGet((long) (elapsedMillis * (Mobile.limitFPS == 0 ? 20 : (float) Mobile.limitFPS / 10f))); } 
        else { curTimeMillis.addAndGet(elapsedMillis); }

        lastMillisTime = now;
        return startMillisTime + curTimeMillis.get();
    }

    public static long nanoTime() 
    {
        long now = System.nanoTime();
        long elapsedNanos = now - lastNanoTime;

        if (MobilePlatform.pressedKeys[20])
        {
            float multiplier = Mobile.fastForwardMultiplier;
            if (multiplier <= 0.0f) { multiplier = 20.0f; }
            curNanoTime.addAndGet((long) (elapsedNanos * multiplier));
        }
        else if (Mobile.unlockFramerateHack > 2) { curNanoTime.addAndGet((long) (elapsedNanos * (Mobile.limitFPS == 0 ? 20 : (float) Mobile.limitFPS / 10f))); } 
        else { curNanoTime.addAndGet(elapsedNanos); }

        lastNanoTime = now;
        return startNanoTime + curNanoTime.get();
    }

    /* Helps with jars that spam GC calls, causing cpu usage spikes */
    public static void noGC() { }

    /* Can reduce cpu usage in some games, and even helps fix others like Super Action Hero (pulled from J2ME-Loader) */
    public static void yieldOverride() throws InterruptedException { Thread.sleep(1); }

    public static int readFully(java.io.InputStream is, byte[] b) throws java.io.IOException
    {
        int offset = 0;
        int remaining = b.length;
        while (remaining > 0)
        {
            int read = is.read(b, offset, remaining);
            if (read == -1) { break; }
            offset += read;
            remaining -= read;
        }
        return offset == 0 ? -1 : offset;
    }

    public static int readFully(java.io.InputStream is, byte[] b, int off, int len) throws java.io.IOException
    {
        int offset = off;
        int remaining = len;
        int total = 0;
        while (remaining > 0)
        {
            int read = is.read(b, offset, remaining);
            if (read == -1) { break; }
            offset += read;
            remaining -= read;
            total += read;
        }
        return total == 0 ? -1 : total;
    }

    public static int readFully(java.io.DataInputStream dis, byte[] b) throws java.io.IOException
    {
        int offset = 0;
        int remaining = b.length;
        while (remaining > 0)
        {
            int read = dis.read(b, offset, remaining);
            if (read == -1) { break; }
            offset += read;
            remaining -= read;
        }
        return offset == 0 ? -1 : offset;
    }

    public static int readFully(java.io.DataInputStream dis, byte[] b, int off, int len) throws java.io.IOException
    {
        int offset = off;
        int remaining = len;
        int total = 0;
        while (remaining > 0)
        {
            int read = dis.read(b, offset, remaining);
            if (read == -1) { break; }
            offset += read;
            remaining -= read;
            total += read;
        }
        return total == 0 ? -1 : total;
    }
}