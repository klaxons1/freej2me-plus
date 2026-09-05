import java.lang.reflect.*;
public class LoadTest2 {
    public static void main(String[] a) throws Exception {
        Class lc = Class.forName("com.mascotcapsule.eruption.docomostar.Loader");
        Class bp = Class.forName("com.mascotcapsule.eruption.docomostar.BufferPool");
        Class arr = java.lang.reflect.Array.newInstance(bp, 0).getClass();
        Method m = lc.getMethod("load", new Class[]{ arr, byte[].class, Integer.TYPE });
        String[] tags = { "MCM_", "MCA_", "MCT_" };
        for (int t = 0; t < tags.length; t++) {
            byte[] d = new byte[128];
            for (int i=0;i<4;i++) d[i] = (byte) tags[t].charAt(i);
            Object r = m.invoke(null, new Object[]{ null, d, new Integer(0) });
            int n = java.lang.reflect.Array.getLength(r);
            System.out.println(tags[t] + " -> Object3D[" + n + "]");
            for (int i = 0; i < n; i++) {
                Object o = java.lang.reflect.Array.get(r, i);
                if (o == null) { System.out.println("      [" + i + "] null"); continue; }
                Method gct = o.getClass().getMethod("getClassType", null);
                System.out.println("      [" + i + "] " + o.getClass().getName() + "  classType=" + gct.invoke(o, null));
            }
        }
    }
}
