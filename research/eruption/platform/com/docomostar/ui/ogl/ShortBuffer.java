package com.docomostar.ui.ogl;
public interface ShortBuffer extends DirectBuffer {
    short[] get(int a0, short[] a1, int a2, int a3);
    void put(int a0, short[] a1, int a2, int a3);
    void setSegment(int a0, int a1);
}
