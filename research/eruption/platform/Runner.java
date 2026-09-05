import java.lang.reflect.*;

public class Runner {
    static void p(String s) { System.out.println(s); }
    static Class C(String n) throws Exception { return Class.forName("com.mascotcapsule.eruption.docomostar."+n); }

    public static void main(String[] args) throws Exception {
        p("=== 1. Util3D  (trig/sqrt implemented where?) ===");
        Class u = C("Util3D");
        Method sqrt = u.getMethod("sqrt", new Class[]{ Float.TYPE });
        Method sin  = u.getMethod("sin",  new Class[]{ Float.TYPE });
        Method cos  = u.getMethod("cos",  new Class[]{ Float.TYPE });
        Method atan2= u.getMethod("atan2",new Class[]{ Float.TYPE, Float.TYPE });
        p("  Util3D.sqrt(2)   = " + sqrt.invoke(null, new Object[]{ new Float(2f) }));
        p("  Util3D.sin(0.5)  = " + sin.invoke(null,  new Object[]{ new Float(0.5f) }));
        p("  Util3D.cos(0.5)  = " + cos.invoke(null,  new Object[]{ new Float(0.5f) }));
        p("  Util3D.atan2(1,1)= " + atan2.invoke(null,new Object[]{ new Float(1f), new Float(1f) }));

        p("=== 2. Vector3D ===");
        Class vc = C("Vector3D");
        Constructor v3 = vc.getConstructor(new Class[]{ Float.TYPE, Float.TYPE, Float.TYPE });
        Object a = v3.newInstance(new Object[]{ new Float(3f), new Float(4f), new Float(0f) });
        Object b = v3.newInstance(new Object[]{ new Float(1f), new Float(0f), new Float(0f) });
        Method len = vc.getMethod("length", new Class[]{ Float.TYPE, Float.TYPE, Float.TYPE });
        p("  length(3,4,0)    = " + len.invoke(null, new Object[]{ new Float(3f), new Float(4f), new Float(0f) }));
        Method dot = vc.getMethod("dot", new Class[]{ vc });
        p("  dot((3,4,0),(1,0,0)) = " + dot.invoke(a, new Object[]{ b }));
        Method cross = vc.getMethod("cross", new Class[]{ vc });
        cross.invoke(a, new Object[]{ b });
        Method gx = vc.getMethod("getX", null), gy = vc.getMethod("getY", null), gz = vc.getMethod("getZ", null);
        p("  cross result     = (" + gx.invoke(a,null) + ", " + gy.invoke(a,null) + ", " + gz.invoke(a,null) + ")");

        p("=== 3. Transform: rotate 90deg about Z, then transform a point ===");
        Class tc = C("Transform");
        Object t = tc.getConstructor(null).newInstance(null);
        Object axis = v3.newInstance(new Object[]{ new Float(0f), new Float(0f), new Float(1f) });
        Method setRotate = tc.getMethod("setRotate", new Class[]{ vc, Float.TYPE });
        setRotate.invoke(t, new Object[]{ axis, new Float((float)(Math.PI/2)) });
        Object pt = v3.newInstance(new Object[]{ new Float(1f), new Float(0f), new Float(0f) });
        Method transPos = tc.getMethod("transPosition", new Class[]{ vc });
        transPos.invoke(t, new Object[]{ pt });
        p("  (1,0,0) rot90Z   = (" + gx.invoke(pt,null) + ", " + gy.invoke(pt,null) + ", " + gz.invoke(pt,null) + ")");

        Method get = tc.getMethod("get", new Class[]{ float[].class });
        float[] m = new float[16];
        get.invoke(t, new Object[]{ m });
        StringBuffer sb = new StringBuffer("  matrix           = [");
        for (int i=0;i<16;i++) { sb.append(m[i]); if (i<15) sb.append(", "); }
        p(sb.append("]").toString());

        p("=== 4. Graphics3D + GL capture ===");
        Class glc = Class.forName("com.docomostar.ui.ogl.GraphicsOGL");
        Class rec = Class.forName("com.docomostar.ui.ogl.GLRecorder");
        Object gl = rec.getConstructor(null).newInstance(null);
        Class g3c = C("Graphics3D");
        Object g3 = g3c.getConstructor(new Class[]{ glc, Integer.TYPE, Integer.TYPE })
                       .newInstance(new Object[]{ gl, new Integer(240), new Integer(320) });
        p("  Graphics3D built = " + (g3 != null));
        Method setViewport = g3c.getMethod("setViewport", new Class[]{ glc, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE });
        setViewport.invoke(g3, new Object[]{ gl, new Integer(0), new Integer(0), new Integer(240), new Integer(320) });
        Method clear = g3c.getMethod("clear", new Class[]{ glc, Integer.TYPE, Integer.TYPE });
        clear.invoke(g3, new Object[]{ gl, new Integer(3), new Integer(0x000000) });

        java.lang.reflect.Field logf = rec.getField("log");
        java.util.ArrayList log = (java.util.ArrayList) logf.get(gl);
        p("  GL commands emitted = " + log.size());
        for (int i = 0; i < log.size(); i++) p("     " + log.get(i));
    }
}
