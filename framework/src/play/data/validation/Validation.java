package play.data.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.oval.ConstraintViolation;
import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.FieldContext;
import play.exceptions.UnexpectedException;

public class Validation {

    static ThreadLocal<Validation> current = new ThreadLocal<Validation>();    // ~~~~
    public List<Error> errors = new ArrayList();
    public boolean keep = false;
    
    public static Validation current() {
        return current.get();
    }

    public static List<Error> errors() {
        return current.get().errors;
    }

    public static Map<String, List<Error>> errorsMap() {
        Map<String, List<Error>> result = new HashMap<String, List<Error>>();
        for (Error error : errors()) {
            result.put(error.key, errors(error.key));
        }
        return result;
    }
    
    public static void addError(String key, String message, String... variables) {
        Validation.current().errors.add(new Error(key, message, variables));
    }

    public static boolean hasErrors() {
        return current.get().errors.size() > 0;
    }

    public static Error error(String key) {
        for (Error error : current.get().errors) {
            if (error.key.equals(key)) {
                return error;
            }
        }
        return null;
    }

    public static List<Error> errors(String key) {
        List<Error> errors = new ArrayList<Error>();
        for (Error error : current.get().errors) {
            if (error.key.equals(key)) {
                errors.add(error);
            }
        }
        return errors;
    }

    public static void keep() {
        current.get().keep = true;
    }

    public static boolean hasError(String key) {
        return error(key) != null;
    }
    // ~~~~ Validations
    public static boolean checkRequired(String key, Object o, String... message) {
        RequiredCheck check = new RequiredCheck();
        return applyCheck(check, key, o, message);
    }

    public static boolean checkMin(String key, Object o, double min, String... message) {
        MinCheck check = new MinCheck();
        check.min = min;
        return applyCheck(check, key, o, message);
    }
    
    public static boolean check(String key, Object o, String... message) {
        ValidCheck check = new ValidCheck();
        check.key = key;
        return applyCheck(check, key, o, message);
    }

    static boolean applyCheck(AbstractAnnotationCheck check, String key, Object o, String... message) {
        try {
            if (!check.isSatisfied(o, o, null, null)) {
                Error error = new Error(key, message.length == 0 ? check.getClass().getDeclaredField("mes").get(null) + "" : message[0], check.getMessageVariables() == null ? new String[0] : check.getMessageVariables().values().toArray(new String[0]));
                Validation.current().errors.add(error);
                return false;
            }
            return true;
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
}