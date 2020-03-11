package priv.songxusheng.easystorer;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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

public class EasyStorer {
    //region 单例
    private EasyStorer(){}
    private final static class InstanceHolder{
        private static EasyStorer instance = null;
        private synchronized static EasyStorer getInstance(){
            if(instance==null)  try {instance = new EasyStorer(); } catch (Exception e) { }
            return instance;
        }
    }
    private static final EasyStorer getInstance() {
        return InstanceHolder.getInstance();
    }
    //endregion

    //region 获取数据库
    private final Object locker = new Object();//数据库操作同步锁
    private final Map<String,SQLiteDatabase> dbProvider= new HashMap();
    private SQLiteDatabase getDatabase(){
        return getDatabase("_Easy_Storer_");
    }
    private SQLiteDatabase getDatabase(String databaseName){
        SQLiteDatabase db;
        synchronized (locker){
            if(dbProvider.get(databaseName) != null) return dbProvider.get(databaseName);
            File f = new File(context.getDatabasePath(databaseName+".db").getAbsolutePath());
            if(!f.getParentFile().exists()){
                f.getParentFile().mkdirs();
            }
            if(f.exists()&&f.isDirectory()){
                f.delete();
            }
            if(!f.exists()){
                try { f.createNewFile(); } catch (IOException e) { }
            }
            db = SQLiteDatabase.openOrCreateDatabase(f,null);
            //TODO
            // E/SQLiteDatabase: Failed to open database '/data/user/0/priv.songxusheng.testeasystorer/databases/901.db'.
            //      android.database.sqlite.SQLiteException: Failed to change locale for db '/data/user/0/priv.songxusheng.testeasystorer/databases/901.db' to 'zh_CN'.
            //           at android.database.sqlite.SQLiteConnection.setLocaleFromConfiguration(SQLiteConnection.java:401)
            //           at android.database.sqlite.SQLiteConnection.open(SQLiteConnection.java:226)

            //When I use 1000 thread to process the data at the same time, I meet this exception
            //For more information to see /priv/songxusheng/easystorer/ExampleUnitTest.java

            //Use next line to get database object.
            //SQLiteDatabase.openDatabase(f.getAbsolutePath(),null,SQLiteDatabase.NO_LOCALIZED_COLLATORS|SQLiteDatabase.CREATE_IF_NECESSARY);
            db.execSQL("Create Table if not exists " +
                    "EasyStorer(" +
                    "Tag varchar(100)," +
                    "ClassName varchar(100)," +
                    "pFileName varchar(1000)," +
                    "PRIMARY KEY(Tag,ClassName));");
            db.execSQL("Create Table if not exists EasyIndex(ID bigint);");
            Cursor cursor = db.rawQuery("Select Count(*) from EasyIndex;",null);
            if(!cursor.moveToNext()){
                db.execSQL("Insert into EasyIndex(ID) Values (0); ");
            }
            cursor.close();
            dbProvider.put(databaseName,db);
            return db;
        }
    }
    //endregion

    //region 增删查
    private String OBJECT_SAVE_PATH = context.getFilesDir().getAbsolutePath();
    private Object readObject(String tag,Object defaultValue,String databaseName){
        SQLiteDatabase db = getDatabase(databaseName);
        Cursor cursor = null;
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            //search the file save info
            synchronized (locker){
                cursor = db.rawQuery("Select pFileName from EasyStorer where Tag = ? and ClassName = ?",new String[]{tag,defaultValue.getClass().getName()});
            }
            cursor.moveToNext();
            fis = new FileInputStream(String.format("%s/%s/%s.es",OBJECT_SAVE_PATH,databaseName,String.valueOf(cursor.getLong(0))));
            ois = new ObjectInputStream(fis);
            return ois.readObject();
        } catch (Exception e) {
            Log.e("EasyStorer",e.getMessage());
        } finally {
            try { cursor.close(); } catch (Exception e) { }
            try { ois.close(); } catch (Exception e) { }
            try { fis.close(); } catch (Exception e) { }
        }

        return defaultValue;
    }

    private boolean writeObject(String tag, Object obj, String databaseName){
        //declare variables
        SQLiteDatabase db = getDatabase(databaseName);
        Cursor cursor = null;
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        File wFile = null, pFiles[] = null;
        boolean flag = true;
        long index = 1L;

        synchronized (locker){
            try {
                db.beginTransaction();
                //delete the old file
                cursor = db.rawQuery("Select pFileName from EasyStorer where Tag = ? and ClassName = ?",new String[]{tag,obj.getClass().getName()});
                if(cursor.getCount()>0){
                    pFiles = new File[cursor.getCount()];
                    int i = 0;
                    while(cursor.moveToNext()){
                        pFiles[i++] = new File(String.format("%s/%s/%s.es",OBJECT_SAVE_PATH,databaseName,cursor.getLong(0)));
                    }
                }
                cursor.close();
                //search a new index for this object
                db.execSQL("Delete from EasyStorer where  Tag = ? and ClassName = ?",new String[]{tag,obj.getClass().getName()});
                cursor = db.rawQuery("Select ID from EasyIndex;",null);
                if(cursor.moveToNext()){
                    index = 1L + cursor.getLong(0);
                }
                //prepare the file to save object
                wFile = new File(String.format("%s/%s/%s.es",OBJECT_SAVE_PATH,databaseName,index));
                if(!wFile.getParentFile().exists()){
                    wFile.getParentFile().mkdirs();
                }
                if(!wFile.exists()){
                    wFile.createNewFile();
                }
                //write object data to file
                fos =  new FileOutputStream(wFile);
                oos =new ObjectOutputStream(fos);
                oos.writeObject(obj);
                //also execute the SQLite command to update record info
                db.execSQL("Delete from EasyIndex;");
                db.execSQL("Insert into EasyIndex(ID) values(?);",new Object[]{index});
                db.execSQL("Insert into EasyStorer(Tag,ClassName,pFileName) values(?,?,?);",new Object[]{tag,obj.getClass().getName(),String.valueOf(index)});
            } catch (Exception e) {
                flag = false;
            } finally {
                try { cursor.close(); } catch (Exception e) { }
                try { oos.close(); } catch (Exception e) { }
                try { fos.close(); } catch (Exception e) { }
                if(flag) {
                    if(pFiles!=null){
                        for(File f:pFiles){
                            try { f.delete(); } catch (Exception e) { }
                        }
                    }
                    db.setTransactionSuccessful();
                }
                else{
                    try { wFile.delete(); } catch (Exception e) { }
                }
                db.endTransaction();
            }
        }
        return flag;
    }

    private boolean removeItem(String tag,String databaseName){
        SQLiteDatabase db = getDatabase(databaseName);
        Cursor cursor = null;
        boolean flag = true;
        synchronized (locker){
            db.beginTransaction();
            try {
                cursor = db.rawQuery("Select pFileName,ClassName from EasyStorer where Tag = ?",new String[]{tag});
                while(cursor.moveToNext()){
                    db.execSQL("Delete from EasyStorer where Tag = ? and ClassName = ? and pFileName = ?",new String[]{tag,cursor.getString(1),cursor.getString(0)});
                    try { new File(String.format("%s/%s/%s.es",OBJECT_SAVE_PATH,databaseName,cursor.getLong(0))).delete(); } catch (Exception e) { }
                }
            } catch (Exception e) {
                flag = false;
            } finally {
                if(flag){
                    db.setTransactionSuccessful();
                }
                try { cursor.close(); } catch (Exception e) { }
                db.endTransaction();
            }
//            System.gc();
        }
        return flag;
    }

    private boolean removeItem(String tag,Class clazz,String databaseName){
        SQLiteDatabase db = getDatabase(databaseName);
        Cursor cursor = null;
        boolean flag = true;
        synchronized (locker){
            db.beginTransaction();
            try {
                cursor = db.rawQuery("Select pFileName,ClassName from EasyStorer where Tag = ?",new String[]{tag,clazz.getName()});
                while(cursor.moveToNext()){
                    db.execSQL("Delete from EasyStorer where Tag = ? and ClassName = ? and pFileName = ?",new String[]{tag,cursor.getString(1),cursor.getString(0)});
                    try { new File(String.format("%s/%s/%s.es",OBJECT_SAVE_PATH,databaseName,cursor.getLong(0))).delete(); } catch (Exception e) { }
                }
            } catch (Exception e) {
                flag = false;
            } finally {
                if(flag){
                    db.setTransactionSuccessful();
                }
                try { cursor.close(); } catch (Exception e) { }
                db.endTransaction();
            }
//            System.gc();
        }
        return flag;
    }

    private boolean clearAll(String databaseName){
        SQLiteDatabase db =  databaseName == null ?getDatabase():getDatabase(databaseName);
        boolean flag = true;
        synchronized (locker){
            try {
                db.beginTransaction();
                File file = new File(String.format("%s/%s/",OBJECT_SAVE_PATH,databaseName));
                if(file.exists()&&file.isDirectory()){
                    File files[] = file.listFiles();
                    for(File f:files){
                        try { f.delete(); } catch (Exception e) { }
                    }
                }
                db.execSQL("Delete from EasyStorer;");
                db.execSQL("Delete from EasyIndex;");
                db.execSQL("Insert into EasyIndex(ID) values(0);");
            } catch (Exception e) {
                flag = false;
            }
            finally {
                if(flag){
                    db.setTransactionSuccessful();
                    db.endTransaction();
                    String path = context.getDatabasePath(databaseName).getParent();
                    try { db.close(); } catch (Exception e) { }
                    try { new File(path+"/"+databaseName+"/").delete(); } catch (Exception e) { }
                    try { new File(path+"/"+databaseName+".db").delete(); } catch (Exception e) { }
                    try { new File(path+"/"+databaseName+".db-journal").delete(); } catch (Exception e) { }
                    dbProvider.remove(databaseName);
                }
                else {
                    db.endTransaction();
                }
//                System.gc();
            }
        }
        return flag;
    }
    //endregion

    //region 检测初始化及是否实现Serializable接口
    private static final Set<String> classSet = new HashSet<>();
    private static void CheckSerializable(final Class clazz){
        if(classSet.contains(clazz.getName())||clazz.isInterface()) return;
        classSet.add(clazz.getName());
        Log.e("CheckSerializable",clazz.getName());
        if(!Serializable.class.isAssignableFrom(clazz)){
            throw new RuntimeException(String.format("Class %s must implement Serializable!",clazz.getName()));
        }
        Set<Field> fields = new HashSet<>(new ArrayList(Arrays.asList(clazz.getFields())){{addAll(new ArrayList(Arrays.asList(clazz.getDeclaredFields())));}});
        for(Field field:fields){
            if(field.getDeclaringClass().isInterface()){
                CheckSerializable(field.getDeclaringClass());
                continue;
            }
            if(new HashSet(new ArrayList(Arrays.asList(new String[]{
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
            }))).contains(field.getType().getName())||field.getType().getName().startsWith("java.")){
                continue;
            }
            if(!Serializable.class.isAssignableFrom(field.getType())){
                throw new RuntimeException(String.format("Class %s must implement Serializable!",field.getType().getName()));
            }
            CheckSerializable(field.getType());
        }
    }

    private static void TraceCheck(Object obj){
        if(context == null){
            throw new RuntimeException("You can't use EasyStorer without initializing it!\nYou should call init(Context) first in Application!");
        }
        CheckSerializable(obj.getClass());
        classSet.clear();
//        System.gc();
    }
    //endregion


    //region 静态公有函数
    private static Context context = null;
    public static void init(Context context){EasyStorer.context = context;}

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
     * @param databaseName
     * @param <T>
     * @return
     */
    public static <T> T get(String tag,T defaultValue,String databaseName){
        TraceCheck(defaultValue);
        Object returnObject = getInstance().readObject(tag,defaultValue,databaseName==null?"_Easy_Storer_":databaseName);
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
     * @param databaseName
     * @return
     */
    public static boolean put(String tag,Object obj,String databaseName){
        TraceCheck(obj);
        return getInstance().writeObject(tag,obj,databaseName==null?"_Easy_Storer_":databaseName);
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
     * @param databaseName
     * @return
     */
    public static boolean remove(String tag,String databaseName){
        return getInstance().removeItem(tag,(databaseName==null||"".equals(databaseName))?"_Easy_Storer_":databaseName);
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
     * @param databaseName
     * @return
     */
    public static boolean remove(String tag,Class clazz, String databaseName){
        return getInstance().removeItem(tag,clazz,databaseName==null?"_Easy_Storer_":databaseName);
    }

    public static boolean clear(){
        return clear(null);
    }

    public static boolean clear(String databaseName){
        return getInstance().clearAll(databaseName==null?"_Easy_Storer_":databaseName);
    }
    //endregion
}