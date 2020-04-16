<%@ page contentType="text/html; charset=utf-8" language="java"
     import="org.ecocean.*,
java.util.List,
java.util.ArrayList,
java.util.Map,
java.util.HashMap,
java.io.File,
org.json.JSONObject,

org.ecocean.configuration.*,

org.ecocean.media.*
              "
%><html><head>
<title>Configuration Test</title>
<style>
body {
    font-family: sans, arial;
}
pre {
    display: inline-block;
    padding: 10px;
    margin: 6px;
    background-color: #CCC;
}
.value {
    border-radius: 4px;
    padding: 6px;
    margin: 2px;
    display: inline-block;
    color: #FFF;
    background-color: #8AF;
}
</style>
</html><body><%

Shepherd myShepherd = new Shepherd("context0");
myShepherd.beginDBTransaction();

ConfigurationUtil.init();   //not required, but clears cache for us (for testing)

String id = request.getParameter("id");

if (id == null) {
    Map<String,JSONObject> meta = ConfigurationUtil.getMeta();
    out.println("<ul>");
    for (String k : meta.keySet()) {
        out.println("<li><a href=\"configTest.jsp?id=" + k + "\">" + k + "</a></li>");
    }
    out.println("</ul>");
    return;
}

out.println("<h1>" + id + "</h1>");
String root = null;
List<String> path = ConfigurationUtil.idPath(id);
if (path.size() > 1) {
    root = path.get(0);
    path.remove(path.size() - 1);
    String up = String.join(".", path);
    out.println("<p><i>Up to <a href=\"configTest.jsp?id=" + up + "\">" + up + "</a></i></p>");
} else {
    out.println("<p><i>Up to <a href=\"configTest.jsp\">[TOP]</a></i></p>");
}

Configuration conf = ConfigurationUtil.getConfiguration(myShepherd, id);
if (conf == null) {
    out.println("<p>unknown id <b>" + id + "</b></p>");
    return;
}

try {
    out.println("<div class=\"value\">" + conf.getValue() + "</div>");
} catch (Exception ex) {}

//JSONObject meta = ConfigurationUtil.getMeta(id);
out.println("<p>" + conf + "</p>");
if (conf.getMeta() != null) out.println("<p>our <b>meta</b>:</p><pre>" + conf.getMeta().toString(8) + "</pre>");

out.println("<p>for <b>front end</b>:</p><pre>" + conf.toFrontEndJSONObject(myShepherd).toString(8) + "</pre>");
out.println("<ul>");
for (String k : conf.getChildKeys()) {
    out.println("<li><a href=\"configTest.jsp?id=" + id + "." + k + "\">" + id + "." + k + "</a></li>");
}
out.println("</ul>");


if (root != null) {
    Configuration top = ConfigurationUtil.getConfiguration(myShepherd, root);
    out.println("<div class=\"value\">" + top.getContent().toString(8) + "</div>");
}

/*
//out.println("<p>" + ConfigurationUtil.getConfiguration(myShepherd, "cache.bar") + "</p>");

//out.println(ConfigurationUtil.getConfiguration(myShepherd, "cache.bar").toJSONObject());


out.println(ConfigurationUtil.setConfigurationValue(myShepherd, "test.fu.barr", 2170));


//out.println(ConfigurationUtil.getConfiguration(myShepherd, "cache.bar").toJSONObject());

//out.println(  ConfigurationUtil.getConfigurationValue(myShepherd, "cache.bar")  );



JSONObject jobj = new JSONObject();
List<String> path = ConfigurationUtil.idPath("test.foo");
out.println(ConfigurationUtil.setDeepJSONObject(jobj, path, 2170));

*/

myShepherd.commitDBTransaction();

%>
</body></html>
