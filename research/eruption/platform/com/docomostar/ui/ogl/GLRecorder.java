package com.docomostar.ui.ogl;

import java.util.ArrayList;


/** Recording GL: captures the exact command stream eruption emits. */
public class GLRecorder implements GraphicsOGL {
    public final ArrayList log = new ArrayList();
    public int drawElements, drawArrays, triangles, texBinds, bufferBinds;
    private void rec(String s) { log.add(s); }

    public void glActiveTexture(int a0) {
        rec("glActiveTexture("+a0+")");
    }
    public void glAlphaFunc(int a0, float a1) {
        rec("glAlphaFunc("+a0+", "+a1+")");
    }
    public void glBindBuffer(int a0, int a1) {
        rec("glBindBuffer("+a0+", "+a1+")");
        bufferBinds++;
    }
    public void glBindTexture(int a0, int a1) {
        rec("glBindTexture("+a0+", "+a1+")");
        texBinds++;
    }
    public void glBlendFunc(int a0, int a1) {
        rec("glBlendFunc("+a0+", "+a1+")");
    }
    public void glBufferData(int a0, com.docomostar.ui.ogl.DirectBuffer a1, int a2) {
        rec("glBufferData("+a0+", "+a1+", "+a2+")");
    }
    public void glBufferSubData(int a0, int a1, com.docomostar.ui.ogl.DirectBuffer a2) {
        rec("glBufferSubData("+a0+", "+a1+", "+a2+")");
    }
    public void glClear(int a0) {
        rec("glClear("+a0+")");
    }
    public void glClearColor(float a0, float a1, float a2, float a3) {
        rec("glClearColor("+a0+", "+a1+", "+a2+", "+a3+")");
    }
    public void glClientActiveTexture(int a0) {
        rec("glClientActiveTexture("+a0+")");
    }
    public void glColor4f(float a0, float a1, float a2, float a3) {
        rec("glColor4f("+a0+", "+a1+", "+a2+", "+a3+")");
    }
    public void glColorMask(boolean a0, boolean a1, boolean a2, boolean a3) {
        rec("glColorMask("+a0+", "+a1+", "+a2+", "+a3+")");
    }
    public void glColorPointer(int a0, int a1, int a2, int a3) {
        rec("glColorPointer("+a0+", "+a1+", "+a2+", "+a3+")");
    }
    public void glColorPointer(int a0, int a1, int a2, com.docomostar.ui.ogl.DirectBuffer a3) {
        rec("glColorPointer("+a0+", "+a1+", "+a2+", "+a3+")");
    }
    public void glCompressedTexImage2D(int a0, int a1, int a2, int a3, int a4, int a5, com.docomostar.ui.ogl.ByteBuffer a6) {
        rec("glCompressedTexImage2D("+a0+", "+a1+", "+a2+", "+a3+", "+a4+", "+a5+", "+a6+")");
    }
    public void glCopyTexImage2D(int a0, int a1, int a2, int a3, int a4, int a5, int a6, int a7) {
        rec("glCopyTexImage2D("+a0+", "+a1+", "+a2+", "+a3+", "+a4+", "+a5+", "+a6+", "+a7+")");
    }
    public void glCullFace(int a0) {
        rec("glCullFace("+a0+")");
    }
    public void glCurrentPaletteMatrixOES(int a0) {
        rec("glCurrentPaletteMatrixOES("+a0+")");
    }
    public void glDeleteBuffers(int[] a0) {
        rec("glDeleteBuffers("+a0+")");
    }
    public void glDeleteTextures(int[] a0) {
        rec("glDeleteTextures("+a0+")");
    }
    public void glDepthFunc(int a0) {
        rec("glDepthFunc("+a0+")");
    }
    public void glDepthMask(boolean a0) {
        rec("glDepthMask("+a0+")");
    }
    public void glDisable(int a0) {
        rec("glDisable("+a0+")");
    }
    public void glDisableClientState(int a0) {
        rec("glDisableClientState("+a0+")");
    }
    public void glDrawArrays(int a0, int a1, int a2) {
        rec("glDrawArrays("+a0+", "+a1+", "+a2+")");
        drawArrays++;
    }
    public void glDrawElements(int a0, int a1, int a2, int a3) {
        rec("glDrawElements("+a0+", "+a1+", "+a2+", "+a3+")");
        drawElements++; if (a0==4) triangles += a1/3;
    }
    public void glDrawElements(int a0, int a1, com.docomostar.ui.ogl.DirectBuffer a2) {
        rec("glDrawElements("+a0+", "+a1+", "+a2+")");
        drawElements++; if (a0==4) triangles += a1/3;
    }
    public void glEnable(int a0) {
        rec("glEnable("+a0+")");
    }
    public void glEnableClientState(int a0) {
        rec("glEnableClientState("+a0+")");
    }
    public void glFlush() {
        rec("glFlush()");
    }
    public void glFogf(int a0, float a1) {
        rec("glFogf("+a0+", "+a1+")");
    }
    public void glFogfv(int a0, float[] a1) {
        rec("glFogfv("+a0+", "+a1+")");
    }
    public void glFrontFace(int a0) {
        rec("glFrontFace("+a0+")");
    }
    public void glGenBuffers(int[] a0) {
        rec("glGenBuffers("+a0+")");
    }
    public void glGenTextures(int[] a0) {
        rec("glGenTextures("+a0+")");
    }
    public int glGetError() {
        rec("glGetError()");
        return 0;
    }
    public void glGetIntegerv(int a0, int[] a1) {
        rec("glGetIntegerv("+a0+", "+a1+")");
    }
    public void glHint(int a0, int a1) {
        rec("glHint("+a0+", "+a1+")");
    }
    public void glLightModelf(int a0, float a1) {
        rec("glLightModelf("+a0+", "+a1+")");
    }
    public void glLightModelfv(int a0, float[] a1) {
        rec("glLightModelfv("+a0+", "+a1+")");
    }
    public void glLightf(int a0, int a1, float a2) {
        rec("glLightf("+a0+", "+a1+", "+a2+")");
    }
    public void glLightfv(int a0, int a1, float[] a2) {
        rec("glLightfv("+a0+", "+a1+", "+a2+")");
    }
    public void glLoadMatrixf(float[] a0) {
        rec("glLoadMatrixf("+a0+")");
    }
    public void glMaterialf(int a0, int a1, float a2) {
        rec("glMaterialf("+a0+", "+a1+", "+a2+")");
    }
    public void glMaterialfv(int a0, int a1, float[] a2) {
        rec("glMaterialfv("+a0+", "+a1+", "+a2+")");
    }
    public void glMatrixIndexPointerOES(int a0, int a1, int a2, int a3) {
        rec("glMatrixIndexPointerOES("+a0+", "+a1+", "+a2+", "+a3+")");
    }
    public void glMatrixMode(int a0) {
        rec("glMatrixMode("+a0+")");
    }
    public void glMultMatrixf(float[] a0) {
        rec("glMultMatrixf("+a0+")");
    }
    public void glNormalPointer(int a0, int a1, int a2) {
        rec("glNormalPointer("+a0+", "+a1+", "+a2+")");
    }
    public void glNormalPointer(int a0, int a1, com.docomostar.ui.ogl.DirectBuffer a2) {
        rec("glNormalPointer("+a0+", "+a1+", "+a2+")");
    }
    public void glPixelStorei(int a0, int a1) {
        rec("glPixelStorei("+a0+", "+a1+")");
    }
    public void glPolygonOffset(float a0, float a1) {
        rec("glPolygonOffset("+a0+", "+a1+")");
    }
    public void glPopMatrix() {
        rec("glPopMatrix()");
    }
    public void glPushMatrix() {
        rec("glPushMatrix()");
    }
    public void glScissor(int a0, int a1, int a2, int a3) {
        rec("glScissor("+a0+", "+a1+", "+a2+", "+a3+")");
    }
    public void glShadeModel(int a0) {
        rec("glShadeModel("+a0+")");
    }
    public void glTexCoordPointer(int a0, int a1, int a2, int a3) {
        rec("glTexCoordPointer("+a0+", "+a1+", "+a2+", "+a3+")");
    }
    public void glTexCoordPointer(int a0, int a1, int a2, com.docomostar.ui.ogl.DirectBuffer a3) {
        rec("glTexCoordPointer("+a0+", "+a1+", "+a2+", "+a3+")");
    }
    public void glTexEnvf(int a0, int a1, float a2) {
        rec("glTexEnvf("+a0+", "+a1+", "+a2+")");
    }
    public void glTexEnvfv(int a0, int a1, float[] a2) {
        rec("glTexEnvfv("+a0+", "+a1+", "+a2+")");
    }
    public void glTexImage2D(int a0, int a1, int a2, int a3, int a4, int a5, int a6, int a7, com.docomostar.ui.ogl.DirectBuffer a8) {
        rec("glTexImage2D("+a0+", "+a1+", "+a2+", "+a3+", "+a4+", "+a5+", "+a6+", "+a7+", "+a8+")");
    }
    public void glTexParameterf(int a0, int a1, float a2) {
        rec("glTexParameterf("+a0+", "+a1+", "+a2+")");
    }
    public void glTexSubImage2D(int a0, int a1, int a2, int a3, int a4, int a5, int a6, int a7, com.docomostar.ui.ogl.DirectBuffer a8) {
        rec("glTexSubImage2D("+a0+", "+a1+", "+a2+", "+a3+", "+a4+", "+a5+", "+a6+", "+a7+", "+a8+")");
    }
    public void glVertexPointer(int a0, int a1, int a2, int a3) {
        rec("glVertexPointer("+a0+", "+a1+", "+a2+", "+a3+")");
    }
    public void glVertexPointer(int a0, int a1, int a2, com.docomostar.ui.ogl.DirectBuffer a3) {
        String extra = "";
        if (a3 instanceof DirectBufferFactory.SB) {
            short[] d = ((DirectBufferFactory.SB) a3).data;
            StringBuffer sb = new StringBuffer(" verts=[");
            for (int i = 0; i < d.length && i < 12; i++) { sb.append(d[i]); if (i<11 && i<d.length-1) sb.append(","); }
            extra = sb.append("]").toString();
        }
        rec("glVertexPointer(size="+a0+", type="+a1+", stride="+a2+")"+extra);
    }
    public void glViewport(int a0, int a1, int a2, int a3) {
        rec("glViewport("+a0+", "+a1+", "+a2+", "+a3+")");
    }
    public void glWeightPointerOES(int a0, int a1, int a2, int a3) {
        rec("glWeightPointerOES("+a0+", "+a1+", "+a2+", "+a3+")");
    }
}
