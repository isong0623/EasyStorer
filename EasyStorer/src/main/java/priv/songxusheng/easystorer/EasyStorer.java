package priv.songxusheng.easystorer;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class EasyStorer {
    //region 单例
    private EasyStorer(){}
    private final static class InstanceHolder{
        private static EasyStorer instance = null;
        private synchronized static EasyStorer getInstance(){
            if(instance==null) {
                instance = new EasyStorer();
                if(InstanceHolder.instance.lockHolder==null){
                    InstanceHolder.instance.lockHolder = new HashMap<>();
                }
                if(OBJECT_SAVE_PATH==null){
                    if(context == null)
                        throw new RuntimeException("You can't use EasyStorer without initializing it!\nYou should call EasyStorer.init() first in Application!:");
                    OBJECT_SAVE_PATH = context.getFilesDir().getAbsolutePath();
                }
                else{
                    File f = new File(OBJECT_SAVE_PATH+"/");
                    if(!f.exists()){
                        if(!f.getParentFile().exists())
                            f.getParentFile().mkdirs();
                        if(!f.exists())
                            throw new RuntimeException("You can't use EasyStorer path with an unreadable position!");
                    }
                    else if(!f.isDirectory())
                        throw new RuntimeException("You can't use EasyStorer path with a file!");
                }
            }
            return instance;
        }
    }
    private static final EasyStorer getInstance() {
        return InstanceHolder.getInstance();
    }

    protected static final void releaseInstance(){//for test
        ReentrantReadWriteLock.WriteLock lock = holderLock.writeLock();
        lock.lock();
        try {
            if( InstanceHolder.instance == null) {
                System.gc();
                return;
            }
            Set<Map.Entry<String, Map<String, Map<String,ReentrantReadWriteLock> > > > groups = InstanceHolder.instance.lockHolder.entrySet();
            for(Map.Entry<String, Map<String, Map<String,ReentrantReadWriteLock> > > group:groups){
                Set<Map.Entry<String, Map<String,ReentrantReadWriteLock> > > tags = group.getValue().entrySet();
                for(Map.Entry<String, Map<String,ReentrantReadWriteLock> > tag:tags){
                    Set<Map.Entry<String,ReentrantReadWriteLock> > clzs = tag.getValue().entrySet();
                    for(Map.Entry<String,ReentrantReadWriteLock> clz:clzs){
                        tag.getValue().remove(clz.getKey());
                    }
                    tag.getValue().clear();
                    group.getValue().remove(tag.getKey());
                }
                group.getValue().clear();
                InstanceHolder.instance.lockHolder.remove(group.getKey());
            }
            InstanceHolder.instance.lockHolder.clear();
            InstanceHolder.instance.lockHolder = null;
            InstanceHolder.instance = null;
            System.gc();
        } finally {
            lock.unlock();
        }
    }
    //endregion

    //region 锁
    private volatile Map<String, Map<String, Map<String,ReentrantReadWriteLock> > > lockHolder = new HashMap<>();
    private volatile static ReentrantReadWriteLock holderLock = new ReentrantReadWriteLock(true);
    final private ReentrantReadWriteLock getLock(String group, String classPath, String tag){
        ReentrantReadWriteLock.ReadLock lock = holderLock.readLock();//share&fair lock

        lock.lock();
        try {
            Map<String, Map<String,ReentrantReadWriteLock> > mGroup = lockHolder.get(group);
            Map<String,ReentrantReadWriteLock> mTag;
            ReentrantReadWriteLock retLock;
            if(mGroup == null){
                synchronized (EasyStorer.class){
                    mGroup = lockHolder.get(group);
                    if(mGroup==null){
                        mGroup = new HashMap<>();
                        mTag = new HashMap<>();
                        retLock = new ReentrantReadWriteLock(true);
                        mTag.put(classPath,retLock);
                        mGroup.put(tag,mTag);
                        lockHolder.put(group,mGroup);
                        return retLock;
                    }
                }
            }
            mTag = mGroup.get(tag);
            if(mTag==null){
                synchronized (mGroup){
                    mTag = mGroup.get(tag);
                    if(mTag == null){
                        mTag = new HashMap<>();//unique one
                        retLock = new ReentrantReadWriteLock(true);
                        mTag.put(classPath,retLock);
                        mGroup.put(tag,mTag);
                        return retLock;
                    }
                }
            }
            retLock = mTag.get(classPath);
            if(retLock==null){
               synchronized (mTag){
                   retLock = mTag.get(classPath);
                   if(retLock==null){
                       retLock = new ReentrantReadWriteLock(true);
                       mTag.put(classPath,retLock);
                   }
               }
            }
            return retLock;
        } finally {
            lock.unlock();
        }

    }

    private final void removeLock(String group, String classPath, String tag){
        ReentrantReadWriteLock.WriteLock lock = holderLock.writeLock();
        lock.lock();
        try {
            Map<String, Map<String,ReentrantReadWriteLock> > mGroup = lockHolder.get(group);
            if(mGroup == null) {
                lockHolder.remove(group);
                return;
            }
            synchronized (mGroup){
                Map<String,ReentrantReadWriteLock> mTag = mGroup.get(tag);
                if(mTag == null) return;
                synchronized (mTag){
                    mTag.remove(classPath);
                    if(mTag.size()==0){
                        mGroup.remove(tag);
                        if(mGroup.size()==0){
                            lockHolder.remove(group);
                        }
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    //endregion

    //region 增删查
    private static String OBJECT_SAVE_PATH = null;
    private Object readObject(String tag,Object defaultValue,String readGroup){
        if(!new File(String.format("%s/%s/%s/%s.es",OBJECT_SAVE_PATH,readGroup,defaultValue.getClass().getName().replaceAll("\\.", "/"),tag)).exists()) {
            removeLock(readGroup,defaultValue.getClass().getName(),tag);
            return defaultValue;
        }
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        final ReentrantReadWriteLock.ReadLock lock = getLock(readGroup,defaultValue.getClass().getName(),tag).readLock();

        lock.lock();
        if(!getLock(readGroup,defaultValue.getClass().getName(),tag).readLock().equals(lock)){
            lock.unlock();
            return readObject(tag,defaultValue,readGroup);
        }
        try {
            fis = new FileInputStream(String.format("%s/%s/%s/%s.es",OBJECT_SAVE_PATH,readGroup,defaultValue.getClass().getName().replaceAll("\\.", "/"),tag));
            ois = new ObjectInputStream(fis);
            return ois.readObject();
        } catch (Exception e) {
        } finally {
            try { ois.close(); } catch (Exception e) { }
            try { fis.close(); } catch (Exception e) { }
            lock.unlock();
        }

        return defaultValue;
    }

    private boolean writeObject(String tag, Object obj, String group){
        //declare variables
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        final ReentrantReadWriteLock.WriteLock lock = getLock(group,obj.getClass().getName(),tag).writeLock();
        File wFile = null;
        boolean flag = true;

        lock.lock();
        if(!getLock(group,obj.getClass().getName(),tag).writeLock().equals(lock)){
            lock.unlock();
            return writeObject(tag,obj,group);
        }
        try {
            wFile = new File(String.format("%s/%s/%s/%s.es",OBJECT_SAVE_PATH,group,obj.getClass().getName().replaceAll("\\.", "/"),tag));
            if(!wFile.getParentFile().exists()){
                wFile.getParentFile().mkdirs();
            }
            if(!wFile.exists()){
                wFile.createNewFile();
            }
            fos =  new FileOutputStream(wFile);
            oos =new ObjectOutputStream(fos);
            oos.writeObject(obj);
        } catch (Exception e) {
            flag = false;
        } finally {
            try { oos.close(); } catch (Exception e) { }
            try { fos.close(); } catch (Exception e) { }
            try { if(!flag)  wFile.delete(); } catch (Exception e) { }
            lock.unlock();
        }

        return flag;
    }

    private boolean deleteOne(File f,final String group) {
        boolean flag = true;
        String fileName = f.getAbsolutePath();
        String tag = new StringBuilder(f.getName().substring(0,f.getName().length()-".es".length())).toString();
        File fCls = new File(OBJECT_SAVE_PATH+"/");
        String className = new String(fileName.substring(fCls.getAbsolutePath().length()));
        className = new String(className.substring(group.length()+2,className.length() - f.getName().length()-1)).replaceAll("\\/",".");

        final ReentrantReadWriteLock.WriteLock lock = getLock(group,className,tag).writeLock();
        lock.lock();
        if(!getLock(group,className,tag).writeLock().equals(lock)){
            lock.unlock();
            return deleteOne(f,group);
        }
        try { f.delete(); } catch (Exception e) { flag = false; }
        lock.unlock();
        removeLock(group,className,tag);
        return flag;
    }

    private boolean deleteGroup(File f,final String group){
        boolean flag = true;

        if(f!=null&&f.exists()){
            File fs[] = f.listFiles();
            if(fs!=null){
                for(File _f:fs) {
                    flag &= deleteGroup(_f,group);
                }
                f.delete();
            }
            else {
                flag &= deleteOne(f,group);
            }
        }

        return flag;
    }

    private boolean deleteTag(File f,String group,String tag){
        boolean flag = true;

        if(f!=null&&f.exists()){
            File fs[] = f.listFiles();
            if(fs!=null){
                for(File _f:fs) {
                    flag &= deleteTag(_f,group,tag);
                }
                f.delete();
            }
            else if(f.getName().equals(tag+".es")){
                flag &= deleteOne(f,group);
            }
        }

        return flag;
    }

    private boolean removeItem(String tag,String group){
        return deleteTag(new File(String.format("%s/%s/",OBJECT_SAVE_PATH,group)),group,tag);
    }

    private boolean removeItem(String tag,Class clazz,String group){
        final ReentrantReadWriteLock.WriteLock lock = getLock(group,clazz.getName(),tag).writeLock();
        boolean flag = false;

        lock.lock();
        if(!getLock(group,clazz.getName(),tag).writeLock().equals(lock)){
            lock.unlock();
            return removeItem(tag,clazz,group);
        }
        try {
            File f = new File(String.format("%s/%s/%s/%s.es",
                    OBJECT_SAVE_PATH,
                    group,
                    clazz.getName().replaceAll("\\.", "/"),tag));
            flag = f.exists()?f.delete():true;
        } catch (Exception e) {}
        lock.unlock();
        removeLock(group,clazz.getName().replaceAll("\\.", "/"),tag);
        return flag;
    }

    private boolean clearAll(String group){
        return deleteGroup(new File(String.format("%s/%s/", OBJECT_SAVE_PATH, group)),group);
    }
    //endregion

    //region 检测初始化及是否实现Serializable接口
    private static void CheckSerializable(final Class clazz,Set<String> classSet){
        if(classSet.contains(clazz.getName())||clazz.isInterface()) return;
        classSet.add(clazz.getName());
        Log.e("CheckSerializable",clazz.getName());
        if(!Serializable.class.isAssignableFrom(clazz)){
            throw new RuntimeException(String.format("Class %s must implement Serializable!",clazz.getName()));
        }
        Set<Field> fields = new HashSet<>(new ArrayList(Arrays.asList(clazz.getFields())){{addAll(new ArrayList(Arrays.asList(clazz.getDeclaredFields())));}});
        for(Field field:fields){
            if(field.getDeclaringClass().isInterface()){
                CheckSerializable(field.getDeclaringClass(),classSet);
                continue;
            }
            if(classSet.contains(field.getType().getName())||field.getType().getName().startsWith("java.")){
                continue;
            }
            if(!Serializable.class.isAssignableFrom(field.getType())){
                throw new RuntimeException(String.format("Class %s must implement Serializable!",field.getType().getName()));
            }
            CheckSerializable(field.getType(),classSet);
        }
    }

    private static void TraceCheck(Object obj){
        CheckSerializable(obj.getClass(),new HashSet(new ArrayList(Arrays.asList(new String[]{
                "int",
                "short",
                "long",
                "double",
                "char",
                "byte",
                "float",
                "boolean",
                "java.lang.Integer",
                "java.lang.Short",
                "java.lang.Long",
                "java.lang.Double",
                "java.lang.Character",
                "java.lang.Byte",
                "java.lang.Float",
                "java.lang.Boolean",
                "java.lang.Object"
        }))));
    }
    //endregion


    //region 静态公有函数
    private static Context context = null;
    public static void init(Context context){EasyStorer.context = context;}
    public static void init(String savePath){ OBJECT_SAVE_PATH = savePath; }
    /**
     *
     * @param tag 标签
     * @param defaultValue 取不到时返回的值
     * @param <T>
     * @return 在默认库（）中取出标签为tag且类型为T的对象
     */
    public static <T> T get(String tag,T defaultValue){
        return get(tag,defaultValue,null);
    }

    /**
     *
     * @param tag
     * @param defaultValue
     * @param group
     * @param <T>
     * @return
     */
    public static <T> T get(String tag,T defaultValue,String group){
        TraceCheck(defaultValue);
        Object returnObject = getInstance().readObject(tag,defaultValue,group==null?"_Easy_Storer_":group);
        return (T)(returnObject == null?defaultValue:returnObject);
    }

    /**
     *
     * @param tag
     * @param obj
     * @return
     */
    public static boolean put(String tag,Object obj){
        return put(tag,obj,null);
    }

    /**
     *
     * @param tag
     * @param obj
     * @param group
     * @return
     */
    public static boolean put(String tag,Object obj,String group){
        TraceCheck(obj);
        return getInstance().writeObject(tag,obj,group==null?"_Easy_Storer_":group);
    }

    /**
     *
     * @param tag
     * @return
     */
    public static boolean remove(String tag){
        return remove(tag,"");
    }

    /**
     *
     * @param tag 标签
     * @param group
     * @return
     */
    public static boolean remove(String tag,String group){
        return getInstance().removeItem(tag,(group==null||"".equals(group))?"_Easy_Storer_":group);
    }

    /**
     *
     * @param tag
     * @param clazz
     * @return
     */
    public static boolean remove(String tag,Class clazz){
        return remove(tag,clazz,null);
    }

    /**
     *
     * @param tag
     * @param clazz
     * @param group
     * @return
     */
    public static boolean remove(String tag,Class clazz, String group){
        return getInstance().removeItem(tag,clazz,group==null?"_Easy_Storer_":group);
    }

    public static boolean clear(){
        return clear(null);
    }

    public static boolean clear(String group){
        return getInstance().clearAll(group==null?"_Easy_Storer_":group);
    }
    //endregion
}