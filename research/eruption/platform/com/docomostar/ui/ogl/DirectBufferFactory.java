package com.docomostar.ui.ogl;

public class DirectBufferFactory {
    private static final DirectBufferFactory INSTANCE = new DirectBufferFactory();
    public static DirectBufferFactory getFactory() { return INSTANCE; }

    public ByteBuffer allocateByteBuffer(int n) { return new BB(new byte[n]); }
    public ByteBuffer allocateByteBuffer(byte[] a) { return new BB(a); }
    public FloatBuffer allocateFloatBuffer(int n) { return new FB(new float[n]); }
    public FloatBuffer allocateFloatBuffer(float[] a) { return new FB(a); }
    public ShortBuffer allocateShortBuffer(int n) { return new SB(new short[n]); }
    public ShortBuffer allocateShortBuffer(short[] a) { return new SB(a); }

    public static class BB implements ByteBuffer {
        public byte[] data; public int segOff, segLen;
        BB(byte[] d){ data=d; segLen=d.length; }
        public void setSegment(int o,int l){ segOff=o; segLen=l; }
        public byte[] get(int p, byte[] dst, int off, int len){ System.arraycopy(data,p,dst,off,len); return dst; }
        public void put(int p, byte[] src, int off, int len){ System.arraycopy(src,off,data,p,len); }
    }
    public static class FB implements FloatBuffer {
        public float[] data; public int segOff, segLen;
        FB(float[] d){ data=d; segLen=d.length; }
        public void setSegment(int o,int l){ segOff=o; segLen=l; }
        public float[] get(int p, float[] dst, int off, int len){ System.arraycopy(data,p,dst,off,len); return dst; }
        public void put(int p, float[] src, int off, int len){ System.arraycopy(src,off,data,p,len); }
    }
    public static class SB implements ShortBuffer {
        public short[] data; public int segOff, segLen;
        SB(short[] d){ data=d; segLen=d.length; }
        public void setSegment(int o,int l){ segOff=o; segLen=l; }
        public short[] get(int p, short[] dst, int off, int len){ System.arraycopy(data,p,dst,off,len); return dst; }
        public void put(int p, short[] src, int off, int len){ System.arraycopy(src,off,data,p,len); }
    }
}
