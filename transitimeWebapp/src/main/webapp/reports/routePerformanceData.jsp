<%@ page import="org.transitime.reports.RoutePerformanceQuery" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="org.json.JSONArray" %>
<%@ page contentType="application/json" %>
<%

String agency = request.getParameter("a");
String beginDateStr = request.getParameter("beginDate");
String endDateStr = request.getParameter("endDate");
String beginTimeStr = request.getParameter("beginTime");
String endTimeStr = request.getParameter("endTime");
double allowableEarly = Double.parseDouble(request.getParameter("allowableEarly"));
double allowableLate = Double.parseDouble(request.getParameter("allowableLate"));
String predictionType = request.getParameter("predictionType");
String predictionSource = request.getParameter("source");

if (beginTimeStr == "")
  beginTimeStr = "00:00:00";
else
  beginTimeStr += ":00";
if (endTimeStr == "")
  endTimeStr = "23:59:59";
else
  endTimeStr += ":00";

SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
Date beginDate = dateFormat.parse(beginDateStr + " " + beginTimeStr);
Date endDate = dateFormat.parse(endDateStr + " " + endTimeStr);

List<Object[]> results = new RoutePerformanceQuery().query(agency, beginDate, endDate, allowableEarly, allowableLate, predictionType, predictionSource);

JSONArray json = new JSONArray(results);
json.write(out);

%>