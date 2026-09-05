package com.docomostar.ui.ogl.math;

public class Matrix4f {
    public float[] m = new float[16];
    public Matrix4f() { setIdentity(); }
    public void setIdentity() {
        for (int i = 0; i < 16; i++) m[i] = 0f;
        m[0] = m[5] = m[10] = m[15] = 1f;
    }
    public void mul(Matrix4f a, Matrix4f b) {
        float[] r = new float[16];
        for (int c = 0; c < 4; c++)
            for (int rw = 0; rw < 4; rw++) {
                float s = 0f;
                for (int k = 0; k < 4; k++) s += a.m[k*4+rw] * b.m[c*4+k];
                r[c*4+rw] = s;
            }
        System.arraycopy(r, 0, m, 0, 16);
    }
    public void invert() {
        // general 4x4 inverse (Gauss-Jordan), column-major
        float[] a = new float[16]; System.arraycopy(m,0,a,0,16);
        float[] inv = new float[16];
        for (int i=0;i<16;i++) inv[i]=0f;
        inv[0]=inv[5]=inv[10]=inv[15]=1f;
        for (int col=0; col<4; col++) {
            int piv=col;
            for (int r=col+1;r<4;r++) if (Math.abs(a[col*4+r])>Math.abs(a[col*4+piv])) piv=r;
            if (Math.abs(a[col*4+piv])<1e-12f) continue;
            if (piv!=col) for (int c=0;c<4;c++){
                float t=a[c*4+col]; a[c*4+col]=a[c*4+piv]; a[c*4+piv]=t;
                t=inv[c*4+col]; inv[c*4+col]=inv[c*4+piv]; inv[c*4+piv]=t;
            }
            float d=a[col*4+col];
            for (int c=0;c<4;c++){ a[c*4+col]/=d; inv[c*4+col]/=d; }
            for (int r=0;r<4;r++){
                if (r==col) continue;
                float f=a[col*4+r];
                if (f==0f) continue;
                for (int c=0;c<4;c++){ a[c*4+r]-=f*a[c*4+col]; inv[c*4+r]-=f*inv[c*4+col]; }
            }
        }
        System.arraycopy(inv,0,m,0,16);
    }
}
