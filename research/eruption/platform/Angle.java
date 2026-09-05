import java.lang.reflect.*;
public class Angle {
    public static void main(String[] a) throws Exception {
        Class u = Class.forName("com.mascotcapsule.eruption.docomostar.Util3D");
        Method sin = u.getMethod("sin", new Class[]{ Float.TYPE });
        Method cos = u.getMethod("cos", new Class[]{ Float.TYPE });
        Method atan2 = u.getMethod("atan2", new Class[]{ Float.TYPE, Float.TYPE });
        System.out.println("angle-unit probe: find x where sin(x)==1 (quarter turn)");
        float[] probes = { 0f, 0.25f, 0.5f, 1f, 90f, 128f, 256f, 512f, 1024f, 4096f, 16384f, 65536f };
        for (int i=0;i<probes.length;i++) {
            Object s = sin.invoke(null, new Object[]{ new Float(probes[i]) });
            Object c = cos.invoke(null, new Object[]{ new Float(probes[i]) });
            System.out.println("  x="+probes[i]+"  sin="+s+"  cos="+c);
        }
        System.out.println("atan2(1,1) = " + atan2.invoke(null,new Object[]{new Float(1f),new Float(1f)}) + "   (expect quarter of a turn/8)");
        System.out.println("atan2(0,1) = " + atan2.invoke(null,new Object[]{new Float(0f),new Float(1f)}));
        System.out.println("atan2(1,0) = " + atan2.invoke(null,new Object[]{new Float(1f),new Float(0f)}));
    }
}
