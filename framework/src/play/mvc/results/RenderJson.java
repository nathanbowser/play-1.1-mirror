package play.mvc.results;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.exceptions.UnexpectedException;

/**
 * 200 OK with application/json
 */
public class RenderJson extends Result {

    String json;

    public RenderJson(Object o) {
        json = new Gson().toJson(o);
    }
    
    public RenderJson(Object o, JsonSerializer... adapters) {
        GsonBuilder gson = new GsonBuilder();
        for(Object adapter : adapters) {
            Type t = getMethod(adapter.getClass(), "serialize").getParameterTypes()[0];;
            gson.registerTypeAdapter(t, adapter);
        }
        json = gson.create().toJson(o);
    }
    
    public RenderJson(String jsonString) {
        json = jsonString;
    }

    public void apply(Request request, Response response) {
        try {
            setContentTypeIfNotSet(response, "application/json; charset=utf-8");
            response.out.write(json.getBytes("utf-8"));
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
    
    //
    
    static Method getMethod(Class clazz, String name) {
        for(Method m : clazz.getDeclaredMethods()) {
            if(m.getName().equals(name)) {
                return m;
            }
        }
        return null;
    }
} 
