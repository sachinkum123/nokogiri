/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nokogiri;

import static nokogiri.internals.NokogiriHelpers.getLocalNameForNamespace;
import static nokogiri.internals.NokogiriHelpers.getLocalPart;
import static nokogiri.internals.NokogiriHelpers.getPrefix;
import static nokogiri.internals.NokogiriHelpers.isNamespace;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nokogiri.internals.SaveContext;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;

/**
 *
 * @author sergio
 */
public class XmlDocumentFragment extends XmlNode {

    public XmlDocumentFragment(Ruby ruby) {
        this(ruby, (RubyClass) ruby.getClassFromPath("Nokogiri::XML::DocumentFragment"));
    }

    public XmlDocumentFragment(Ruby ruby, RubyClass klazz) {
        super(ruby, klazz);
    }

//    @JRubyMethod(name="new", meta = true)
//    public static IRubyObject rbNew(ThreadContext context, IRubyObject cls, IRubyObject doc) {
//        IRubyObject[] argc = new IRubyObject[1];
//        argc[0] = doc;
//        return rbNew(context, cls, argc);
//    }

    @JRubyMethod(name="new", meta = true, required=1, optional=1)
    public static IRubyObject rbNew(ThreadContext context, IRubyObject cls, IRubyObject[] argc) {
        
        if(argc.length < 1) {
            throw context.getRuntime().newArgumentError(argc.length, 1);
        }

        if(!(argc[0] instanceof XmlDocument)){
            throw context.getRuntime().newArgumentError("first parameter must be a Nokogiri::XML::Document instance");
        }

        XmlDocument doc = (XmlDocument) argc[0];
        
        // To ignore invalid namespace, this text processing was added.
        if (argc.length > 1 && argc[1] instanceof RubyString) {
            argc[1] = JavaUtil.convertJavaToRuby(context.getRuntime(), ignoreNamespaceIfNeeded(doc, (String)argc[1].toJava(String.class)));
        }
        
        XmlDocumentFragment fragment = new XmlDocumentFragment(context.getRuntime(), (RubyClass) cls);      
        fragment.setDocument(doc);
        fragment.setNode(doc.getDocument().createDocumentFragment());

        //TODO: Get namespace definitions from doc.

        RuntimeHelpers.invoke(context, fragment, "initialize", argc);

        return fragment;
    }
    
    private static Pattern pattern = Pattern.compile("[a-zA-Z]+:[a-zA-Z]+");
    
    private static String ignoreNamespaceIfNeeded(XmlDocument doc, String tags) {
        if (doc.getDocument() == null) return tags;
        if (doc.getDocument().getDocumentElement() == null) return tags;
        Matcher matcher = pattern.matcher(tags);
        Map<String, String> rewriteTable = new HashMap<String, String>();
        while(matcher.find()) {
            String qName = matcher.group();
            NamedNodeMap nodeMap = doc.getDocument().getDocumentElement().getAttributes();
            if (!isNamespaceDefined(qName, nodeMap)) {
                rewriteTable.put(qName, getLocalPart(qName));
            }
        }
        Set<String> keys = rewriteTable.keySet();
        for (String key : keys) {
            tags = tags.replace(key, rewriteTable.get(key));
        }
        return tags;
    }
    
    private static boolean isNamespaceDefined(String qName, NamedNodeMap nodeMap) {
        for (int i=0; i < nodeMap.getLength(); i++) {
            Attr attr = (Attr)nodeMap.item(i);
            if (isNamespace(attr.getNodeName())) {
                String localPart = getLocalNameForNamespace(attr.getNodeName());
                if (getPrefix(qName).equals(localPart)) {
                    return true;
                }
            }
        }
        return false;
    }

    //@Override
    public void add_child(ThreadContext context, XmlNode child) {
        // Some magic for DocumentFragment

        Ruby ruby = context.getRuntime();
        XmlNodeSet children = (XmlNodeSet) child.children(context);

        long length = children.length();

        RubyArray childrenArray = children.convertToArray();

        if(length != 0) {
            for(int i = 0; i < length; i++) {
                XmlNode item = (XmlNode) ((XmlNode) childrenArray.aref(ruby.newFixnum(i))).dup(context);
                add_child(context, item);
            }
        }
    }

    @Override
    public void relink_namespace(ThreadContext context) {
        ((XmlNodeSet) children(context)).relink_namespace(context);
    }

    @Override
    public void saveContent(ThreadContext context, SaveContext ctx) {
        saveNodeListContent(context, (XmlNodeSet) children(context), ctx);
    }

}