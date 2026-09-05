import java.lang.reflect.*;
public class LoadTest {
    static Object load(byte[] d) throws Exception {
        Class lc = Class.forName("com.mascotcapsule.eruption.docomostar.Loader");
        Class bp = Class.forName("com.mascotcapsule.eruption.docomostar.BufferPool");
        Class arr = java.lang.reflect.Array.newInstance(bp, 0).getClass();
        Method m = lc.getMethod("load", new Class[]{ arr, byte[].class, Integer.TYPE });
        return m.invoke(null, new Object[]{ null, d, new Integer(0) });
    }
    static void probe(String label, byte[] d) {
        try { Object r = load(d); System.out.println(label + " -> returned " + r); }
        catch (InvocationTargetException e) {
            Throwable c = e.getCause();
            System.out.println(label + " -> " + c.getClass().getName() + ": " + c.getMessage());
            StackTraceElement[] st = c.getStackTrace();
            for (int i = 0; i < st.length && i < 3; i++) System.out.println("        at " + st[i]);
        }
        catch (Exception e) { System.out.println(label + " -> " + e); }
    }
    public static void main(String[] a) throws Exception {
        System.out.println("=== Loader format dispatch (is the parser Java?) ===");
        probe("garbage 'XXXX'", new byte[]{ 'X','X','X','X', 0,0,0,0, 0,0,0,0 });
        byte[] mcm = new byte[64]; mcm[0]='M'; mcm[1]='C'; mcm[2]='M'; mcm[3]='_';
        probe("magic  'MCM_'", mcm);
        byte[] mca = new byte[64]; mca[0]='M'; mca[1]='C'; mca[2]='A'; mca[3]='_';
        probe("magic  'MCA_'", mca);
        byte[] mct = new byte[64]; mct[0]='M'; mct[1]='C'; mct[2]='T'; mct[3]='_';
        probe("magic  'MCT_'", mct);
    }
}
