/*
 * The Shepherd Project - A Mark-Recapture Framework
 * Copyright (C) 2019 Jason Holmberg
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.ecocean.servlet;

import org.ecocean.StudySite;
import org.ecocean.Encounter;
import org.ecocean.CommonConfiguration;
import org.ecocean.Shepherd;
import org.ecocean.ShepherdProperties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Properties;
import java.util.ArrayList;

import java.io.*;

public class StudySiteMerge extends HttpServlet {

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doPost(request, response);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String context = ServletUtilities.getContext(request);
    Shepherd myShepherd = new Shepherd(context);
    myShepherd.setAction("StudySiteDelete.class");
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    boolean locked = false;

    String langCode=ServletUtilities.getLanguageCode(request);
    Properties stuprops = ShepherdProperties.getProperties("studySite.properties", langCode, context); 

    // output in english or spanish
    String mergeSuccess = stuprops.getProperty("mergeSuccess");
    String mergeFail = stuprops.getProperty("mergeFail");
    String mergeMissing = stuprops.getProperty("mergeMissing");
    String rtnToWildbook = stuprops.getProperty("rtnToWildbook");
    String siteString = request.getParameter("StudySite");

    String siteName1 = request.getParameter("studySiteId1").trim();
    String siteName2 = request.getParameter("studySiteId2").trim();

    boolean bothAvailable = false;
    try {
        System.out.println("name1 is site? : "+myShepherd.isStudySiteWithName(siteName1));
        System.out.println("name2 is site? : "+myShepherd.isStudySiteWithName(siteName2));
        if (myShepherd.isStudySiteWithName(siteName1)&&myShepherd.isStudySiteWithName(siteName2)) {
            bothAvailable = true;
        }
    } catch (Exception e){
        e.printStackTrace();
    }

    System.out.println("StudySite 1 : " + siteName1 );  
    System.out.println("StudySite 2 : " + siteName2 );  

    if (bothAvailable) {
        try {
            myShepherd.beginDBTransaction();
            StudySite site1 = myShepherd.getStudySiteByName(siteName1);
            StudySite site2 = myShepherd.getStudySiteByName(siteName2);

            siteName1 = site1.getName();
            siteName2 = site2.getName();

            //We only need the encs we are reassigning
            ArrayList<Encounter> encs = myShepherd.getAllEncountersForStudySite(site2);
            for (Encounter enc : encs) {
                enc.clearStudySiteData(myShepherd);
                enc.setStudySite(site1);
                myShepherd.commitDBTransaction();
                myShepherd.beginDBTransaction();
            }
            myShepherd.throwAwayStudySite(site2);
            myShepherd.beginDBTransaction();

        } catch (Exception e) {
            locked = true;
            e.printStackTrace();
            myShepherd.rollbackDBTransaction();
            myShepherd.closeDBTransaction();
        }
        if (!locked) {
            myShepherd.commitDBTransaction();
            myShepherd.closeDBTransaction();
            out.println(ServletUtilities.getHeader(request));
            out.println("<p>"+mergeSuccess+"</p>\n");
            out.println("<p><a href=\"http://" + CommonConfiguration.getURLLocation(request)+ " "+rtnToWildbook+"</p>\n");
            out.println(ServletUtilities.getFooter(context));
        } else {
            out.println(ServletUtilities.getHeader(request));
            out.println("<p>"+mergeFail+"</p>\n");
            out.println("<p><a href=\"http://" + CommonConfiguration.getURLLocation(request) +" "+rtnToWildbook+"</p>\n");
            out.println(ServletUtilities.getFooter(context));
        }
    } else {
      myShepherd.rollbackDBTransaction();
      myShepherd.closeDBTransaction();
      out.println(ServletUtilities.getHeader(request));
      out.println("<p>"+mergeMissing+"</p>\n");
      out.println("<p><b>"+siteString+" 1: "+siteName1+"</b></p>\n");
      out.println("<p><b>"+siteString+" 2: "+siteName2+"</b></p>\n");
      out.println("<p><a href=\"http://" + CommonConfiguration.getURLLocation(request) +" "+rtnToWildbook+"</p>\n");

      out.println(ServletUtilities.getFooter(context));
    }
    out.close();
  }
}