package play.mvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.*;
import play.Logger;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesSupport;
import play.exceptions.MailException;
import play.exceptions.TemplateNotFoundException;
import play.exceptions.UnexpectedException;
import play.libs.Mail;
import play.templates.Template;
import play.templates.TemplateLoader;

/**
 * Application mailer support
 */
public class Mailer implements LocalVariablesSupport {

    protected static ThreadLocal<HashMap<String, Object>> infos = new ThreadLocal<HashMap<String, Object>>();

    public static void setSubject(String subject, Object... args) {
        HashMap map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        map.put("subject", String.format(subject, args));
        infos.set(map);
    }

    public static void addRecipient(Object... recipients) {
        HashMap map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        List recipientsList = (List<String>) map.get("recipients");
        if (recipientsList == null) {
            recipientsList = new ArrayList<String>();
            map.put("recipients", recipientsList);
        }
        recipientsList.addAll(Arrays.asList(recipients));
        infos.set(map);
    }

    public static void addAttachment(EmailAttachment... attachments) {
        HashMap map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        List attachmentsList = (List) map.get("attachments");
        if (attachmentsList == null) {
            attachmentsList = new ArrayList<EmailAttachment>();
            map.put("attachments", attachmentsList);
        }
        attachmentsList.addAll(Arrays.asList(attachments));
        infos.set(map);
    }

    public static void setContentType(String contentType) {
        HashMap map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        map.put("contentType", contentType);
        infos.set(map);
    }

    /**
     * Can be of the form xxx <m@m.com>
     *
     * @param from
     */
    public static void setFrom(Object from) {
        HashMap map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        map.put("from", from);
        infos.set(map);
    }

    /**
     * Can be of the form xxx <m@m.com>
     *
     * @param replyTo
     */
    public static void setReplyTo(Object replyTo) {
        HashMap map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        map.put("replyTo", replyTo);
        infos.set(map);
    }

    public static void setCharset(String bodyCharset) {
        HashMap map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        map.put("charset", bodyCharset);
        infos.set(map);
    }

    public static void addHeader(String key, String value) {
        HashMap map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        HashMap<String, String> headers = (HashMap<String, String>) map.get("headers");
        if (headers == null) {
            headers = new HashMap<String, String>();
        }
        headers.put(key, value);
        map.put("headers", headers);
        infos.set(map);
    }

    public static Future<Boolean> send(Object... args) {
        try {
            final HashMap map = infos.get();
            if (map == null) {
                throw new UnexpectedException("Mailer not instrumented ?");
            }

            // Body character set
            final String charset = (String) infos.get().get("charset");

            // Headers
            final Map<String, String> headers = (Map<String, String>) infos.get().get("headers");

            // Subject
            final String subject = (String) infos.get().get("subject");

            String templateName = (String) infos.get().get("method");
            if (templateName.startsWith("notifiers.")) {
                templateName = templateName.substring("notifiers.".length());
            }
            if (templateName.startsWith("controllers.")) {
                templateName = templateName.substring("controllers.".length());
            }
            templateName = templateName.substring(0, templateName.indexOf("("));
            templateName = templateName.replace(".", "/");

            // overrides Template name
            if (args.length > 0 && args[0] instanceof String && LocalVariablesNamesTracer.getAllLocalVariableNames(args[0]).isEmpty()) {
                templateName = args[0].toString();
            }

            final Map<String, Object> templateHtmlBinding = new HashMap();
            final Map<String, Object> templateTextBinding = new HashMap();
            for (Object o : args) {
                List<String> names = LocalVariablesNamesTracer.getAllLocalVariableNames(o);
                for (String name : names) {
                    templateHtmlBinding.put(name, o);
                    templateTextBinding.put(name, o);
                }
            }

            // The rule is as follow: If we ask for text/plain, we don't care about the HTML
            // If we ask for HTML and there is a text/plain we add it as an alternative.
            // If contentType is not specified look at the template available:
            // - .txt only -> text/plain
            // else
            // -           -> text/html
            String contentType = (String) infos.get().get("contentType");
            String bodyHtml = null;
            String bodyText = "";
            try {
                Template templateHtml = TemplateLoader.load(templateName + ".html");
                bodyHtml = templateHtml.render(templateHtmlBinding);
            } catch (TemplateNotFoundException e) {
                if (contentType != null && !contentType.startsWith("text/plain")) {
                    throw e;
                }
            }

            try {
                Template templateText = TemplateLoader.load(templateName + ".txt");
                bodyText = templateText.render(templateTextBinding);
            } catch (TemplateNotFoundException e) {
                if (bodyHtml == null && (contentType == null || (contentType != null && contentType.startsWith("text/plain")))) {
                    throw e;
                }
            }

            // Content type

            if (contentType == null) {
                if (bodyHtml != null) {
                    contentType = "text/html";
                } else {
                    contentType = "text/plain";
                }
            }

            // Recipients
            final List<Object> recipientList = (List<Object>) infos.get().get("recipients");
            // From
            final Object from = infos.get().get("from");
            final Object replyTo = infos.get().get("replyTo");

            Email email = null;
            if (infos.get().get("attachments") == null) {
                if (StringUtils.isEmpty(bodyHtml)) {
                    email = new SimpleEmail();
                    email.setMsg(bodyText);
                } else {
                    HtmlEmail htmlEmail = new HtmlEmail();
                    htmlEmail.setHtmlMsg(bodyHtml);
                    if (!StringUtils.isEmpty(bodyText)) {
                        htmlEmail.setTextMsg(bodyText);
                    }
                    email = htmlEmail;
                }

            } else {
                if (StringUtils.isEmpty(bodyHtml)) {
                    email = new MultiPartEmail();
                    email.setMsg(bodyText);
                } else {
                    HtmlEmail htmlEmail = new HtmlEmail();
                    htmlEmail.setHtmlMsg(bodyHtml);
                    if (!StringUtils.isEmpty(bodyText)) {
                        htmlEmail.setTextMsg(bodyText);
                    }
                    email = htmlEmail;
                }
                MultiPartEmail multiPartEmail = (MultiPartEmail) email;
                List<EmailAttachment> objectList = (List<EmailAttachment>) infos.get().get("attachments");
                for (EmailAttachment object : objectList) {
                    multiPartEmail.attach(object);
                }
            }

            if (from != null) {
                email.setFrom(from.toString());
            }

            if (replyTo != null) {
                email.addReplyTo(replyTo.toString());
            }

            for (Object recipient : recipientList) {
                email.addTo(recipient.toString());
            }

            if (!StringUtils.isEmpty(charset)) {
                email.setCharset(charset);
            }

            email.setSubject(subject);
            email.updateContentType(contentType);

            if (headers != null) {
                for (String key : headers.keySet()) {
                    email.addHeader(key, headers.get(key));
                }
            }

            return Mail.send(email);
        } catch (EmailException ex) {
            throw new MailException("Cannot send email", ex);
        }
    }

    public static boolean sendAndWait(Object... args) throws EmailException {
        try {
            Future<Boolean> result = send(args);
            return result.get();
        } catch (InterruptedException e) {
            Logger.error(e, "Error while waiting Mail.send result");
        } catch (ExecutionException e) {
            Logger.error(e, "Error while waiting Mail.send result");
        }
        return false;
    }
}
