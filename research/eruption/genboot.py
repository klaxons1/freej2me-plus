from jawa.cf import ClassFile
from jawa.assemble import assemble
import os

# Minimal JDK surface needed to compile the platform layer, emitted as Java 1.2 classfiles
# that ECJ 3.2 can actually read. At runtime the REAL JDK classes are used.
SPEC = {
 'java/lang/Object': ('', [('<init>','()V',0),('toString','()Ljava/lang/String;',0),
                           ('equals','(Ljava/lang/Object;)Z',0),('hashCode','()I',0),
                           ('getClass','()Ljava/lang/Class;',0)]),
 'java/lang/Class': ('java/lang/Object', [('getName','()Ljava/lang/String;',0)]),
 'java/lang/String': ('java/lang/Object', [('<init>','()V',0),('length','()I',0),
                           ('valueOf','(I)Ljava/lang/String;',1),('valueOf','(F)Ljava/lang/String;',1),
                           ('valueOf','(Z)Ljava/lang/String;',1),('valueOf','(J)Ljava/lang/String;',1),
                           ('valueOf','(Ljava/lang/Object;)Ljava/lang/String;',1),
                           ('concat','(Ljava/lang/String;)Ljava/lang/String;',0),('charAt','(I)C',0),('getBytes','()[B',0)]),
 'java/lang/StringBuffer': ('java/lang/Object', [('<init>','()V',0),('<init>','(Ljava/lang/String;)V',0),
                           ('append','(Ljava/lang/String;)Ljava/lang/StringBuffer;',0),
                           ('append','(I)Ljava/lang/StringBuffer;',0),('append','(F)Ljava/lang/StringBuffer;',0),
                           ('append','(J)Ljava/lang/StringBuffer;',0),('append','(Z)Ljava/lang/StringBuffer;',0),
                           ('append','(Ljava/lang/Object;)Ljava/lang/StringBuffer;',0),
                           ('toString','()Ljava/lang/String;',0)]),
 'java/lang/Throwable': ('java/lang/Object', [('<init>','()V',0),('<init>','(Ljava/lang/String;)V',0),
                           ('getMessage','()Ljava/lang/String;',0),('toString','()Ljava/lang/String;',0),
                           ('printStackTrace','()V',0)]),
 'java/lang/Exception': ('java/lang/Throwable', [('<init>','()V',0),('<init>','(Ljava/lang/String;)V',0)]),
 'java/lang/RuntimeException': ('java/lang/Exception', [('<init>','()V',0),('<init>','(Ljava/lang/String;)V',0)]),
 'java/lang/Error': ('java/lang/Throwable', [('<init>','()V',0),('<init>','(Ljava/lang/String;)V',0)]),
 'java/lang/System': ('java/lang/Object', [('arraycopy','(Ljava/lang/Object;ILjava/lang/Object;II)V',1),
                           ('currentTimeMillis','()J',1),('nanoTime','()J',1)]),
 'java/lang/Math': ('java/lang/Object', [('abs','(F)F',1),('abs','(I)I',1),('sqrt','(D)D',1),
                           ('sin','(D)D',1),('cos','(D)D',1),('max','(II)I',1),('min','(II)I',1)]),
 'java/io/PrintStream': ('java/lang/Object', [('println','(Ljava/lang/String;)V',0),('println','(I)V',0),
                           ('println','(J)V',0),('println','(F)V',0),('println','(Z)V',0),('println','()V',0),
                           ('print','(Ljava/lang/String;)V',0),('print','(I)V',0),('print','(F)V',0)]),
 'java/util/ArrayList': ('java/lang/Object', [('<init>','()V',0),('add','(Ljava/lang/Object;)Z',0),
                           ('get','(I)Ljava/lang/Object;',0),('size','()I',0),('clear','()V',0)]),
 'java/lang/Float': ('java/lang/Object', [('floatToIntBits','(F)I',1),('intBitsToFloat','(I)F',1),
                           ('parseFloat','(Ljava/lang/String;)F',1)]),
 'java/lang/Short': ('java/lang/Object', [('<init>','(S)V',0)]),
 'java/lang/Integer': ('java/lang/Object', [('<init>','(I)V',0),('parseInt','(Ljava/lang/String;)I',1),
                           ('toString','(I)Ljava/lang/String;',1)]),
 'java/lang/ref/WeakReference': ('java/lang/Object', [('<init>','(Ljava/lang/Object;)V',0),('get','()Ljava/lang/Object;',0)]),
 'java/util/Random': ('java/lang/Object', [('<init>','()V',0),('nextInt','(I)I',0),('nextFloat','()F',0)]),
 'java/util/Stack': ('java/lang/Object', [('<init>','()V',0),('push','(Ljava/lang/Object;)Ljava/lang/Object;',0),
                           ('pop','()Ljava/lang/Object;',0),('isEmpty','()Z',0)]),
 'java/util/Vector': ('java/lang/Object', [('<init>','()V',0),('addElement','(Ljava/lang/Object;)V',0),
                           ('elementAt','(I)Ljava/lang/Object;',0),('size','()I',0)]),
 'java/io/InputStream': ('java/lang/Object', [('<init>','()V',0),('read','()I',0),('read','([BII)I',0),('close','()V',0)]),
 'java/io/ByteArrayInputStream': ('java/io/InputStream', [('<init>','([B)V',0)]),
 'java/io/IOException': ('java/lang/Exception', [('<init>','()V',0),('<init>','(Ljava/lang/String;)V',0)]),
 'java/lang/IllegalArgumentException': ('java/lang/RuntimeException', [('<init>','()V',0),('<init>','(Ljava/lang/String;)V',0)]),
 'java/lang/IllegalStateException': ('java/lang/RuntimeException', [('<init>','()V',0),('<init>','(Ljava/lang/String;)V',0)]),
 'java/lang/NullPointerException': ('java/lang/RuntimeException', [('<init>','()V',0)]),
 'java/lang/IndexOutOfBoundsException': ('java/lang/RuntimeException', [('<init>','()V',0)]),
 'java/lang/ArithmeticException': ('java/lang/RuntimeException', [('<init>','()V',0)]),
 'java/lang/OutOfMemoryError': ('java/lang/Error', [('<init>','()V',0)]),
 'java/lang/StringBuilder': ('java/lang/Object', [('<init>','()V',0),
                           ('append','(Ljava/lang/String;)Ljava/lang/StringBuilder;',0),
                           ('toString','()Ljava/lang/String;',0)]),
}
FIELDS = {'java/lang/System': [('out','Ljava/io/PrintStream;'),('err','Ljava/io/PrintStream;')]}


SPEC.update({
 'java/lang/reflect/Method': ('java/lang/Object', [('invoke','(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;',0),('getName','()Ljava/lang/String;',0)]),
 'java/lang/reflect/Constructor': ('java/lang/Object', [('newInstance','([Ljava/lang/Object;)Ljava/lang/Object;',0)]),
 'java/lang/reflect/Field': ('java/lang/Object', [('get','(Ljava/lang/Object;)Ljava/lang/Object;',0)]),
 'java/lang/Float2': ('java/lang/Object', []),
 'java/lang/Boolean': ('java/lang/Object', [('<init>','(Z)V',0)]),
 'java/lang/Double': ('java/lang/Object', [('<init>','(D)V',0)]),
})
SPEC['java/lang/Class'] = ('java/lang/Object', [
  ('getName','()Ljava/lang/String;',0),
  ('forName','(Ljava/lang/String;)Ljava/lang/Class;',1),
  ('getMethod','(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;',0),
  ('getConstructor','([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;',0),
  ('getField','(Ljava/lang/String;)Ljava/lang/reflect/Field;',0),
])
SPEC['java/lang/Float'] = ('java/lang/Object', [('<init>','(F)V',0),('floatToIntBits','(F)I',1),
   ('intBitsToFloat','(I)F',1),('parseFloat','(Ljava/lang/String;)F',1),('floatValue','()F',0)])
SPEC['java/lang/Integer'] = ('java/lang/Object', [('<init>','(I)V',0),('parseInt','(Ljava/lang/String;)I',1),
   ('toString','(I)Ljava/lang/String;',1),('intValue','()I',0)])
FIELDS.update({
 'java/lang/Float': [('TYPE','Ljava/lang/Class;')],
 'java/lang/Integer': [('TYPE','Ljava/lang/Class;')],
 'java/lang/Boolean': [('TYPE','Ljava/lang/Class;')],
 'java/lang/Double': [('TYPE','Ljava/lang/Class;')],
})
SPEC['java/util/ArrayList'] = ('java/lang/Object', [('<init>','()V',0),('add','(Ljava/lang/Object;)Z',0),
   ('get','(I)Ljava/lang/Object;',0),('size','()I',0),('clear','()V',0)])
SPEC['java/lang/Math'] = ('java/lang/Object', [('abs','(F)F',1),('abs','(I)I',1),('sqrt','(D)D',1),
   ('sin','(D)D',1),('cos','(D)D',1),('max','(II)I',1),('min','(II)I',1)])
FIELDS['java/lang/Math'] = [('PI','D')]

SPEC['java/lang/reflect/Array'] = ('java/lang/Object', [('newInstance','(Ljava/lang/Class;I)Ljava/lang/Object;',1)])
SPEC['java/lang/reflect/InvocationTargetException'] = ('java/lang/Exception', [('getCause','()Ljava/lang/Throwable;',0)])
SPEC['java/lang/StackTraceElement'] = ('java/lang/Object', [('toString','()Ljava/lang/String;',0)])
SPEC['java/lang/Throwable'] = ('java/lang/Object', [('<init>','()V',0),('<init>','(Ljava/lang/String;)V',0),
   ('getMessage','()Ljava/lang/String;',0),('toString','()Ljava/lang/String;',0),('printStackTrace','()V',0),
   ('getStackTrace','()[Ljava/lang/StackTraceElement;',0),('getCause','()Ljava/lang/Throwable;',0)])
SPEC['java/lang/reflect/Array'] = ('java/lang/Object', [('newInstance','(Ljava/lang/Class;I)Ljava/lang/Object;',1),
   ('getLength','(Ljava/lang/Object;)I',1),('get','(Ljava/lang/Object;I)Ljava/lang/Object;',1)])
SPEC['java/lang/Short'] = ('java/lang/Object', [('<init>','(S)V',0),('shortValue','()S',0)])
FIELDS['java/lang/Short'] = [('TYPE','Ljava/lang/Class;')]
out='boot'
n=0
for cn,(sup,methods) in SPEC.items():
    cf=ClassFile.create(cn, super_=sup) if sup else ClassFile.create(cn)
    if not sup: cf._super = 0
    cf.access_flags.acc_public=True; cf.access_flags.acc_super=True
    for fn,fd in FIELDS.get(cn,[]):
        f=cf.fields.create(fn,fd); f.access_flags.acc_public=True; f.access_flags.acc_static=True
    for name,desc,is_static in methods:
        m=cf.methods.create(name,desc,code=False)
        m.attributes._table.clear()
        m.access_flags.acc_public=True
        if is_static: m.access_flags.acc_static=True
        else: m.access_flags.acc_abstract=False
        # no Code attribute -> mark native so the verifier/ECJ accepts a bodyless method
        m.access_flags.acc_native=True
    p=os.path.join(out,cn+'.class'); os.makedirs(os.path.dirname(p),exist_ok=True)
    with open(p,'wb') as fh: cf.save(fh)
    n+=1
print("boot stubs:",n)
