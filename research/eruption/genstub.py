import json, os
from jawa.cf import ClassFile
from jawa.assemble import assemble

need = json.load(open('/tmp/need_erup.json'))
INTERFACES = {'com/docomostar/ui/ogl/GraphicsOGL','com/docomostar/ui/ogl/DirectBuffer',
              'com/docomostar/ui/ogl/ByteBuffer','com/docomostar/ui/ogl/FloatBuffer',
              'com/docomostar/ui/ogl/ShortBuffer'}
SUPER = {'com/docomostar/lang/IllegalStateException':'java/lang/RuntimeException'}

def ret_of(desc): return desc[desc.index(')')+1:]
def nargs(desc):
    s=desc[1:desc.index(')')]; i=0; n=0
    while i < len(s):
        while s[i]=='[': i+=1
        if s[i]=='L': i=s.index(';',i)+1
        else: i+=1
        n+=1 if s[max(0,i-1)] not in 'JD' else 1
    # recount slots properly
    s=desc[1:desc.index(')')]; i=0; slots=0
    while i < len(s):
        arr=False
        while s[i]=='[': i+=1; arr=True
        c=s[i]
        if c=='L': i=s.index(';',i)+1
        else: i+=1
        slots += 2 if (c in 'JD' and not arr) else 1
    return slots

RET_INS = {'V':[('return',)], 'I':[('iconst_0',),('ireturn',)], 'Z':[('iconst_0',),('ireturn',)],
           'B':[('iconst_0',),('ireturn',)], 'S':[('iconst_0',),('ireturn',)], 'C':[('iconst_0',),('ireturn',)],
           'J':[('lconst_0',),('lreturn',)], 'F':[('fconst_0',),('freturn',)], 'D':[('dconst_0',),('dreturn',)]}

count=0
for cn, members in need.items():
    is_iface = cn in INTERFACES
    cf = ClassFile.create(cn, super_=SUPER.get(cn,'java/lang/Object'))
    cf.access_flags.acc_public = True
    if is_iface:
        cf.access_flags.acc_interface = True
        cf.access_flags.acc_abstract = True
        cf.access_flags.acc_super = False
    else:
        cf.access_flags.acc_super = True

    has_ctor=False
    for kind, name, desc in members:
        if kind=='F':
            f = cf.fields.create(name, desc)
            f.access_flags.acc_public = True
            continue
        if is_iface and name=='<init>': continue
        if name=='<init>': has_ctor=True
        m = cf.methods.create(name, desc, code=not is_iface)
        m.access_flags.acc_public = True
        if is_iface:
            m.access_flags.acc_abstract = True
            m.attributes._table.clear()
            continue
        # statics
        if (cn.endswith('DirectBufferFactory') and name=='getFactory') or (cn=='com/docomostar/ui/Image' and name=='createImage'):
            m.access_flags.acc_static = True
        c = m.code; c.max_stack=4; c.max_locals=nargs(desc)+2
        r = ret_of(desc)
        ins=[]
        if name=='<init>':
            ins += [('aload_0',), ('invokespecial', cf.constants.create_method_ref(SUPER.get(cn,'java/lang/Object'),'<init>','()V')), ('return',)]
        else:
            ins += RET_INS.get(r, [('aconst_null',),('areturn',)])
        c.assemble(assemble(ins))
    if not is_iface and not has_ctor:
        m = cf.methods.create('<init>','()V', code=True)
        m.access_flags.acc_public=True
        c=m.code; c.max_stack=2; c.max_locals=1
        c.assemble(assemble([('aload_0',),('invokespecial', cf.constants.create_method_ref(SUPER.get(cn,'java/lang/Object'),'<init>','()V')),('return',)]))

    p = 'out/'+cn+'.class'
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p,'wb') as fh: cf.save(fh)
    count+=1
print("generated", count, "stub classes")
