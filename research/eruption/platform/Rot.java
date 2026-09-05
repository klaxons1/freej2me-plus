import java.lang.reflect.*;
public class Rot {
    public static void main(String[] a) throws Exception {
        Class vc = Class.forName("com.mascotcapsule.eruption.docomostar.Vector3D");
        Class tc = Class.forName("com.mascotcapsule.eruption.docomostar.Transform");
        Constructor v3 = vc.getConstructor(new Class[]{Float.TYPE,Float.TYPE,Float.TYPE});
        Method gx=vc.getMethod("getX",null), gy=vc.getMethod("getY",null), gz=vc.getMethod("getZ",null);
        Object t = tc.getConstructor(null).newInstance(null);
        Object axis = v3.newInstance(new Object[]{new Float(0f),new Float(0f),new Float(1f)});
        // 0.25 turn == 90 degrees
        tc.getMethod("setRotate", new Class[]{vc, Float.TYPE}).invoke(t, new Object[]{axis, new Float(0.25f)});
        Object pt = v3.newInstance(new Object[]{new Float(1f),new Float(0f),new Float(0f)});
        tc.getMethod("transPosition", new Class[]{vc}).invoke(t, new Object[]{pt});
        System.out.println("(1,0,0) rotated 0.25 turn about Z = ("+gx.invoke(pt,null)+", "+gy.invoke(pt,null)+", "+gz.invoke(pt,null)+")   [expect (0,1,0)]");
        float[] m=new float[16];
        tc.getMethod("get",new Class[]{float[].class}).invoke(t,new Object[]{m});
        System.out.print("matrix = [");
        for(int i=0;i<16;i++){ System.out.print(m[i]); if(i<15) System.out.print(", "); }
        System.out.println("]");
    }
}
