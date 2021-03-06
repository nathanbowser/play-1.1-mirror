package play.data.binding;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import play.data.Upload;
import play.mvc.Http.Request;

/**
 * Bind file form multipart/form-data request.
 */
public class UploadBinder implements SupportedType<Upload> {

    @SuppressWarnings("unchecked")
    public Upload bind(Annotation[] annotations, String value) {
        List<Upload> uploads = (List<Upload>)Request.current().args.get("__UPLOADS");
        for(Upload upload : uploads) {
            if(upload.getFieldName().equals(value)) {
                return upload;
            }
        }
        return null;
    }

}