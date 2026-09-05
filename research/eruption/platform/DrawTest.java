import java.lang.reflect.*;
public class DrawTest {
    public static void main(String[] a) throws Exception {
        Class glc = Class.forName("com.docomostar.ui.ogl.GraphicsOGL");
        Class rec = Class.forName("com.docomostar.ui.ogl.GLRecorder");
        Object gl = rec.getConstructor(null).newInstance(null);
        Class g3c = Class.forName("com.mascotcapsule.eruption.docomostar.Graphics3D");
        Object g3 = g3c.getConstructor(new Class[]{glc, Integer.TYPE, Integer.TYPE})
                       .newInstance(new Object[]{gl, new Integer(240), new Integer(320)});
        Class apc = Class.forName("com.mascotcapsule.eruption.docomostar.Appearance");
        Object ap = apc.getConstructor(null).newInstance(null);
        Field logf = rec.getField("log");
        java.util.ArrayList log = (java.util.ArrayList) logf.get(gl);
        int before = log.size();
        Method dr = g3c.getMethod("drawRect", new Class[]{ glc, Short.TYPE, Short.TYPE, Short.TYPE, Short.TYPE,
                                                           Float.TYPE, Integer.TYPE, apc,
                                                           Class.forName("[Lcom.mascotcapsule.eruption.docomostar.RegionF;") });
        dr.invoke(g3, new Object[]{ gl, new Short((short)10), new Short((short)10), new Short((short)100),
                                    new Short((short)50), new Float(0f), new Integer(0xFFFFFFFF), ap, null });
        Method flush = g3c.getMethod("flush", new Class[]{ glc });
        flush.invoke(g3, new Object[]{ gl });
        System.out.println("GL commands emitted by drawRect+flush = " + (log.size()-before));
        for (int i = before; i < log.size(); i++) System.out.println("   " + log.get(i));
        System.out.println("drawArrays=" + rec.getField("drawArrays").get(gl)
                         + " drawElements=" + rec.getField("drawElements").get(gl));
    }
}
