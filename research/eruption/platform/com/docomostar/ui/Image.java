package com.docomostar.ui;
public class Image {
    public int w, h;
    public static Image createImage(int a, int b) { Image i = new Image(); i.w=a; i.h=b; return i; }
    public Graphics getGraphics() { return new Graphics(); }
}
