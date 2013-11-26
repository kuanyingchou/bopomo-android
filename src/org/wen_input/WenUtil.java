package org.wen_input;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class WenUtil {

    public static String readTextFile(String path) {
        String everything="";
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(path));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
    
            while (line != null) {
                sb.append(line);
                sb.append('\n');
                line = br.readLine();
            }
            everything = sb.toString();
            br.close();
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
        }
        return everything;
    }

    public static String readTextFile(InputStream is) {
        String everything="";
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
    
            while (line != null) {
                sb.append(line);
                sb.append('\n');
                line = br.readLine();
            }
            everything = sb.toString();
            br.close();
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
        }
        return everything;
    }


    public static <T> List<T> subList(List<T> target, int size) {
        if(target.size() <= size) {
            return new ArrayList<T>(target);
        } else {
            return target.subList(0, size);
        }
    }
    
    public static String join(List<?> objs, String del) {
        return join(objs.toArray(), del);
    }
    
    public static <T> String join(
            T[] objs, String del) {
        return join(objs, del, "", "");
    }
    
    public static <T> String join(
            T[] objs, String del, String left, String right) {
        StringBuilder sb = new StringBuilder(left);
        if(objs.length > 1) {
            sb.append(objs[0].toString());
        }
        for(int i=1; i<objs.length; i++) {
            sb.append(del);
            sb.append(objs[i]);
        }
        sb.append(right);
        return sb.toString();
    }

}
