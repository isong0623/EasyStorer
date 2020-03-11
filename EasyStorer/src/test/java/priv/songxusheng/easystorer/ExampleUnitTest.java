package priv.songxusheng.easystorer;

import android.util.Log;

import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
//        EasyStorer.init();
        for(int i = 0;i<10;++i){
            final int start = i*100,end = (i+1)*100;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for(int i=start;i<end;++i){
                        final int taskId = i;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                debug(String.valueOf(taskId));
                            }
                        }).start();
                    }
                }
            }).start();
        }
    }

    private static final void debug(String databaseName){
//        EasyStorer.clear(databaseName);
        EasyStorer.put("Obj", new Obj(123,132),databaseName);
        EasyStorer.put("String","String",databaseName);
        EasyStorer.put("byte",(byte)10,databaseName);
        EasyStorer.put("Byte",new Byte((byte) 15),databaseName);
        EasyStorer.put("short",(short)10,databaseName);
        EasyStorer.put("Short",new Short((short)15),databaseName);
        EasyStorer.put("boolean",true,databaseName);
        EasyStorer.put("Boolean",new Boolean(true),databaseName);
        EasyStorer.put("int",10,databaseName);
        EasyStorer.put("Integer",15,databaseName);
        EasyStorer.put("long",10L,databaseName);
        EasyStorer.put("Long",new Long(15L),databaseName);
        EasyStorer.put("float",10f,databaseName);
        EasyStorer.put("Float",15f,databaseName);
        EasyStorer.put("double",10d,databaseName);
        EasyStorer.put("Double",new Double(15d),databaseName);
        EasyStorer.put("List",new ArrayList<>(Arrays.asList(new String[]{"123","456"})),databaseName);
        EasyStorer.put("Set",new HashSet<>(Arrays.asList(new String[]{"123","456"})),databaseName);
        EasyStorer.put("Map",new HashMap<>(new HashMap<String,String>(){{put("abc","edf");}}),databaseName);

        Log.e("TestEasyStorer"+databaseName,EasyStorer.get("Obj",new Obj(),databaseName)+"");
        Log.e("TestEasyStorer"+databaseName,EasyStorer.get("String","null",databaseName));
        Log.e("TestEasyStorer"+databaseName,EasyStorer.get("byte",(byte)0,databaseName)+"");
        Log.e("TestEasyStorer"+databaseName,EasyStorer.get("Byte",new Byte((byte)0),databaseName)+"");
        Log.e("TestEasyStorer"+databaseName,EasyStorer.get("short",(short)0,databaseName)+"");
        Log.e("TestEasyStorer"+databaseName,EasyStorer.get("Short",new Short((short)0),databaseName)+"");
        Log.e("TestEasyStorer"+databaseName,EasyStorer.get("boolean",false,databaseName)+"");
        Log.e("TestEasyStorer"+databaseName,EasyStorer.get("Boolean",new Boolean(false),databaseName)+"");
        Log.e("TestEasyStorer"+databaseName,EasyStorer.get("int",0,databaseName)+"");
        Log.e("TestEasyStorer"+databaseName,EasyStorer.get("Integer",new Integer(0),databaseName)+"");
        Log.e("TestEasyStorer"+databaseName,EasyStorer.get("long",0L,databaseName)+"");
        Log.e("TestEasyStorer"+databaseName,EasyStorer.get("Long",new Long(0L),databaseName)+"");
        Log.e("TestEasyStorer"+databaseName,EasyStorer.get("float",0f,databaseName)+"");
        Log.e("TestEasyStorer"+databaseName,EasyStorer.get("Float",new Float(0f),databaseName)+"");
        Log.e("TestEasyStorer"+databaseName,EasyStorer.get("double",0d,databaseName)+"");
        Log.e("TestEasyStorer"+databaseName,EasyStorer.get("Double",new Double(0d),databaseName)+"");
        Log.e("TestEasyStorer"+databaseName,EasyStorer.get("List",new ArrayList<String>(),databaseName).toArray()[0]+"");
        Log.e("TestEasyStorer"+databaseName,EasyStorer.get("Set",new HashSet<>(),databaseName).toArray()[0]+"");
        Log.e("TestEasyStorer"+databaseName,EasyStorer.get("Map",new HashMap<>(),databaseName).keySet().toArray()[0]+"");

        String tags[] = new String[]{"Obj","String","byte","short","Byte","Short","boolean","Boolean","int","Integer","long","Long","float","Float","double","Double","List","Set","Map"};
        for(String tag:tags){
            EasyStorer.remove(tag,databaseName);
        }
    }
}

class Obj implements Serializable {
    int a = 100;
    int b = 1000;
    //ObjA o;
    Obj(){}
    Obj(int a, int b){
        this.a = a;
        this.b = b;
    }

    @Override
    public String toString() {
        return String.format("val1:%d val2:%d\n",a,b);
    }
}
