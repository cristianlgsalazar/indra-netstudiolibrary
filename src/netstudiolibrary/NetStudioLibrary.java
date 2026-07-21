/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package netstudiolibrary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author clgarcias
 */
public class NetStudioLibrary {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        LDAPUtil ldapUtil = new LDAPUtil();
        TextUitl textUtil = new TextUitl();
        /**
         * Pager Values: SAPRP1:SYNC-SAP-RP1|RMV-SAP-RP1-ROLE|GET-SAP-RP1-ROLE
         * IDT-Entitlement:SET-SAP-RP1-CLIENT
         * ADELSETCORP:elCorpExtAttribute10=EdirSync
         */

        //AdministrativeTermination
        //wp_lxc_csc_users
        //wp_lxc_us_users
        //wp_lxc_lc_all        
        //wp_lxc_pv_corp_all
        //SGH_Dashboard      
        //wp_lxc_field_users
        //wp_lxc_lc_field_users                
        //wp_lxc_pv_field_users
        //wp_lo_empl_users        
        //wp_lo_users        
        //wp_lxc_can_users
        //sgh_reflexis_administrators (No Members)
        //sgh_dashboard_override
        //sgh_reflexis_fullaccess
        //lc_dashboard_override        
        //lc_dashboard_administrators        
        //lc_dashboard_fullaccess        
        //so_dashboard_override        
        //to_dashboard_override
        //AccountExemption        
        String processSGH0365Users;
        String validateGroupMembers;
        String entitlementPolicy;
        String entitlementGroup;
        String removeEntitlementFromUsers;
        String count;
        
        File gposFile = new File("C:\\Users\\clgarcias\\Downloads\\GPOS_TR_User_to_be_pushed.csv");
        int countGposUsers = 1;
        try (Scanner scanner = new Scanner(new FileInputStream(gposFile))) {
            while (scanner.hasNextLine()) {
                if (countGposUsers == 100 |countGposUsers == 200 |countGposUsers == 300 ) {
                    System.out.println("Migration waiting for 10 minutes...");
                    TimeUnit.MINUTES.sleep(10);
                    System.out.println("Migration resumed.");
                }
                String line = scanner.nextLine();
                String dnUser = line.split(";")[0];
                String setAttributeValueByDN = "";                                                
                setAttributeValueByDN = ldapUtil.SetAttributeValueByDN("10.110.7.156", 389, "cn=admin,ou=noc,o=idv", "Gl@ss12f1ll", dnUser, "roomNumber", "GPOSResync");
                System.out.println(dnUser + ":" + setAttributeValueByDN);
                countGposUsers++;
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(NetStudioLibrary.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println(ex);
        } catch (Exception ex) {
            Logger.getLogger(NetStudioLibrary.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println(ex);
        }
        System.out.println("\nValidating associations...");
        ArrayList usersPushed = new ArrayList();
        usersPushed.add("user DN;GPOS Association");
        try (Scanner scanner = new Scanner(new FileInputStream(gposFile))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String dnUser = line.split(";")[0];                                          
                String gposAssociation = ldapUtil.GetAssociationDiverValueByDN("10.110.7.156", 389, "cn=admin,ou=noc,o=idv", "Gl@ss12f1ll", dnUser, "cn=GPOS,cn=DriverSet40,ou=idm,ou=noc,o=idv");
                if (gposAssociation.startsWith("1-")) {
                    gposAssociation = gposAssociation.replace("1-", "");
                } else {
                    gposAssociation = "User is not associated.";
                }
                System.out.println(dnUser + ": " + gposAssociation);
                usersPushed.add(dnUser + ";" + gposAssociation);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(NetStudioLibrary.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println(ex);
        } catch (Exception ex) {
            Logger.getLogger(NetStudioLibrary.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println(ex);
        }
        String rptUsersPushed = textUtil.WriteListToFile("C:\\Users\\clgarcias\\Downloads\\GPOS_TR_Users_Pushed.csv", usersPushed);
        System.out.println(rptUsersPushed);
        
        //String pushUSUsersToADELCORP = PushUSUsersToADELCORP("C:\\Users\\clgarcias\\Downloads\\20260218_NAM_C3_ELCORP_Adoption_Final.csv", "10.110.7.156", 389, "o=idv", "cn=admin,ou=noc,o=idv", "Gl@ss12f1ll", "(&(CN=PVF*02)(|(!(Sitelocation=PEARLE VISION*))(!(Title=PLO_ASSOC))(!(l=PEARLELO))(!(sn=Store))(!(givenName=Licensed))))");
        File file = new File("C:\\Users\\clgarcias\\Documents\\Projekte\\NetStudio\\Support\\Transformations\\Validate Attibutes with multiple values\\EmployeeStatusTerminated.txt");
        try (Scanner scanner = new Scanner(new FileInputStream(file))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String cn = line.split(";")[1];
                String filter = "cn=" + cn;
                //System.out.println(line + "|" + filter);
                String updateObjectsFromQuery;
                updateObjectsFromQuery = ldapUtil.UpdateObjectsFromQuery("10.110.7.156", 389, "ou=people,o=idv", "cn=admin,ou=noc,o=idv", "Gl@ss12f1ll", filter, "set", "employeeStatus", "Terminated");
                System.out.println(updateObjectsFromQuery);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(NetStudioLibrary.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println(ex);
        } catch (Exception ex) {
            Logger.getLogger(NetStudioLibrary.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println(ex);
        }
        
        file = new File("C:\\Users\\clgarcias\\Documents\\Projekte\\NetStudio\\Support\\Transformations\\Validate Attibutes with multiple values\\XTEmployeeStatusTerminated.txt");
        try (Scanner scanner = new Scanner(new FileInputStream(file))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String cn = line.split(";")[1];
                String filter = "cn=" + cn;
                //System.out.println(line + "|" + filter);
                String updateObjectsFromQuery;
                updateObjectsFromQuery = ldapUtil.UpdateObjectsFromQuery("10.110.7.156", 389, "ou=people,o=idv", "cn=admin,ou=noc,o=idv", "Gl@ss12f1ll", filter, "set", "roomNumber", "Set-EmployeeStatus");
                System.out.println(updateObjectsFromQuery);                
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(NetStudioLibrary.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println(ex);
        } catch (Exception ex) {
            Logger.getLogger(NetStudioLibrary.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println(ex);
        }
        //String updateObjectsFromQuery;
        //updateObjectsFromQuery = ldapUtil.UpdateObjectsFromQuery("imanageridt.luxgroup.net", 389, "ou=inactive, o=luxotticaretail", "cn=admin,ou=noc,o=luxotticaretail", "Gl@ss12f1ll", "(idmSAPClient=BP1-500)", "remove", "idmSAPClient","BP1-500");
        //System.out.println(updateObjectsFromQuery);
        //validateGroupMembers = ldapUtil.ValidateGroupMembers("10.110.7.156", 389, "ou=groups,o=idv", "ou=people,o=idv", "cn=admin,ou=noc,o=idv", "Gl@ss12f1ll",
        //        "(cn=Comms-Wholesale employees)");
        //System.out.println("Test:" + validateGroupMembers);
        //String ldapSSL = ldapUtil.LDAPConnect("lcedidmesp01.luxgroup.net", 0, "ou=people,o=idv", "cn=admin,ou=noc,o=idv", "Gl@ss12f1ll", "(cn=test)", "cn");
        //String validateIdmSAPClientConfiguration = ValidateIdmSAPClientConfiguration("C:\\Users\\clgarcias\\Documents\\Projekte\\NetStudio\\Support\\Cleanup RP1 Roles\\IdmSAPClientValidationConfiguration.csv", "imanageridt.luxgroup.net", 389, "ou=people,o=luxotticaretail", "cn=admin,ou=noc,o=luxotticaretail", "Gl@ss12f1ll", "", "");
        //System.out.println(validateIdmSAPClientConfiguration);
        //String getAttrubute = FixPVFUsers02("10.110.7.156", 389, "o=idv", "cn=admin,ou=noc,o=idv", "Gl@ss12f1ll", "(&(CN=PVF*02)(|(!(Sitelocation=PEARLE VISION*))(!(Title=PLO_ASSOC))(!(l=PEARLELO))(!(sn=Store))(!(givenName=Licensed))))");
        //System.out.println(getAttrubute);
        //String getUsersForSGH0365 = GetUsersForSGH0365("C:\\Users\\clgarcias\\Downloads\\CN_Users_to_be_process 3.csv", "C:\\Users\\clgarcias\\Downloads\\Users to process 1.csv", "10.110.7.156", 389, "o=idv", "cn=admin,ou=noc,o=idv", "Gl@ss12f1ll", "idmUserBrand");
        //System.out.println(getUsersForSGH0365);
        //String getAttrubute = ldapUtil.GetAttributeValue("10.110.7.156", 389, "o=idv", "cn=admin,ou=noc,o=idv", "Gl@ss12f1ll", "(cn=ZAZA006316)", "idmUserBrand");
        //System.out.println(getAttrubute);
        //String setAttrubute = ldapUtil.SetAttributeValue("10.110.7.156", 389, "o=idv", "cn=admin,ou=noc,o=idv", "Gl@ss12f1ll", "(cn=ZAZA006316)", "pager", "SET-SGH-0365-EMAIL");
        //System.out.println(setAttrubute);        
        //String concatFiles = textUtil.ConcatFiles("C:\\Users\\clgarcias\\Downloads\\files", "C:\\Users\\clgarcias\\Downloads\\files", "updates.csv");
        //System.out.println(concatFiles);
        //String countEvents = textUtil.CountLogEvents("C:\\Users\\clgarcias\\Desktop\\adelcorp.log", "Channel:  Publisher");
        //System.out.println(countEvents);
        //String splitFile = textUtil.SplitFile("C:\\Users\\clgarcias\\Downloads\\Usuarios\\EmailExtractWithoutMatchingOffice365_TO_Mailbox 1.txt", "C:\\Users\\clgarcias\\Downloads\\Usuarios\\", 400);
        //System.out.println(splitFile);
        /*
        entitlementPolicy = "LuxNA_DL_Atlanta_Manufacturing";
        entitlementGroup = "Comms-Atlanta Manufacturing employees ";
        removeEntitlementFromUsers = ldapUtil.RemoveEntitlementFromUsers("10.110.7.156", 389, 80, "o=idv", "cn=admin,ou=noc,o=idv", "Gl@ss12f1ll",
        "(DirXML-EntitlementRef=cn=LuxNA_DL_Groups,cn=Entitlement Loopback,cn=DriverSet40,ou=idm,ou=noc,o=idv#0#\\<ref><src>RBE</src><id>idv\\\\noc\\\\idm\\\\DriverSet40\\\\Entitlement Policies\\\\LuxNA_DL_Atlanta_Manufacturing</id><param>\\\\LUXIDV\\\\idv\\\\groups\\\\Lux-NA-DL\\\\Comms-Atlanta Manufacturing employees*</param></ref>)",
        "cn=LuxNA_DL_Groups,cn=Entitlement Loopback,cn=DriverSet40,ou=idm,ou=noc,o=idv",
        0,
        "idv\\noc\\idm\\DriverSet40\\Entitlement Policies\\" + entitlementPolicy,
        "\\LUXIDV\\idv\\groups\\Lux-NA-DL\\" + entitlementGroup,
        "cn=" + entitlementGroup + ",ou=Lux-NA-DL,ou=groups,o=idv"
        );
        count = ldapUtil.GetObjectsCountFromQuery("10.110.7.156", 389, "o=idv", "cn=admin,ou=noc,o=idv", "Gl@ss12f1ll", "(DirXML-EntitlementRef=cn=LuxNA_DL_Groups,cn=Entitlement Loopback,cn=DriverSet40,ou=idm,ou=noc,o=idv#0#\\<ref><src>RBE</src><id>idv\\\\noc\\\\idm\\\\DriverSet40\\\\Entitlement Policies\\\\LuxNA_DL_Atlanta_Manufacturing</id><param>\\\\LUXIDV\\\\idv\\\\groups\\\\Lux-NA-DL\\\\Comms-Atlanta Manufacturing employees*</param></ref>)");
        System.out.println("Users in entitlement policy: " + count);
        count = ldapUtil.GetObjectsCountFromQuery("10.110.7.156", 389, "o=idv", "cn=admin,ou=noc,o=idv", "Gl@ss12f1ll", "(groupMemberShip=cn=" + entitlementGroup + ",ou=Lux-NA-DL,ou=groups,o=idv)");
        System.out.println("Users in group: " + count);*/
    }

    /**
     * This method read a file with users and filter which ones can be processed
     * by driver SGH-035 by theur brand.
     *
     * @param filePath path of file with users to process.
     * @param ldapServer IP or Hostname of ldap server
     * @param ldapPort Connection porf of the lpda instance
     * @param ldapConnDN DN of the user connection
     * @param ldapConnPwd Password of the user connection
     * @param ldapBaseDN Base OU where the search the object to modify is
     * excecuted
     * @param ldapAttribute pager attribute.
     * @param ldapAttributeValue SET-SGH-0365-EMAIL.
     * @return "1" if process was performed successfully or "0-" with the derail
     * of the error.
     */
    public static String ProcessSGH0365Users(String filePath, String ldapServer, int ldapPort, String ldapBaseDN, String ldapConnDN, String ldapConnPwd, String ldapAttribute, String ldapAttributeValue) {

        int count = 0;
        String result;
        LDAPUtil ldapUtil = new LDAPUtil();

        try {

            File file = new File(filePath);
            try (Scanner scanner = new Scanner(file)) {
                while (scanner.hasNextLine()) {
                    String cn = scanner.nextLine().split(";")[0];
                    String setPager = ldapUtil.SetAttributeValue(ldapServer, ldapPort, ldapBaseDN, ldapConnDN, ldapConnPwd, "(cn=" + cn + ")", ldapAttribute, ldapAttributeValue);
                    System.out.println("User Processed:" + cn);
                    if (setPager.equals("1")) {
                        count++;
                        if (count == 200) {
                            Date currentDate = new Date();
                            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            String formattedTime = formatter.format(currentDate);
                            System.out.println(formattedTime + " Sleeping...");
                            System.out.println(formattedTime + " Waiting 1 Hour...");
                            //Thread.sleep(3600000);
                            currentDate = new Date();
                            formattedTime = formatter.format(currentDate);
                            System.out.println(formattedTime);
                            count = 0;
                        }
                    }
                }
            }
            result = "1";
        } catch (FileNotFoundException ex) {
            System.err.println("Error:" + ex.toString());
            result = "0-" + "Error:" + ex.toString();
        }
        return result;
    }

    /**
     * This method read a file with users and filter which ones can be processed
     * by driver SGH-035 by theur brand.
     *
     * @param filePath path of file with users to validate.
     * @param reportPath path of file to write the valid users.
     * @param ldapServer IP or Hostname of ldap server
     * @param ldapPort Connection porf of the lpda instance
     * @param ldapConnDN DN of the user connection
     * @param ldapConnPwd Password of the user connection
     * @param ldapBaseDN Base OU where the search the object to modify is
     * excecuted
     * @param ldapAttribute idmUserBrand attribute.
     * @return "1" if process was performed successfully or "0-" with the derail
     * of the error.
     */
    public static String GetUsersForSGH0365(String filePath, String reportPath, String ldapServer, int ldapPort, String ldapBaseDN, String ldapConnDN, String ldapConnPwd, String ldapAttribute) {

        String result;
        List<String> lines = new ArrayList<>();
        LDAPUtil ldapUtil = new LDAPUtil();
        TextUitl textUitl = new TextUitl();

        try {

            File file = new File(filePath);
            try (Scanner scanner = new Scanner(file)) {
                while (scanner.hasNextLine()) {
                    String cn = scanner.nextLine();
                    String idmUserBrand = ldapUtil.GetAttributeValue("10.110.7.156", 389, "o=idv", "cn=admin,ou=noc,o=idv", "Gl@ss12f1ll", "(cn=" + cn + ")", "idmUserBrand");
                    if (idmUserBrand.equalsIgnoreCase("Sunglass Hut") | idmUserBrand.equalsIgnoreCase("Lenscrafters") | idmUserBrand.equalsIgnoreCase("Oakley") | idmUserBrand.equalsIgnoreCase("Team Vision") | idmUserBrand.equalsIgnoreCase("Target Optical")
                            | idmUserBrand.equalsIgnoreCase("OP Retail") | idmUserBrand.equalsIgnoreCase("Optical Shops of Aspen") | idmUserBrand.equalsIgnoreCase("Persol") | idmUserBrand.equalsIgnoreCase("Alain Mikli") | idmUserBrand.equalsIgnoreCase("Oliver Peoples")
                            | idmUserBrand.equalsIgnoreCase("For Eyes")) {
                        lines.add(cn + ";" + idmUserBrand);
                        System.out.println(cn + ";" + idmUserBrand);
                    }
                }
            }
            result = textUitl.WriteListToFile(reportPath, (ArrayList<String>) lines);
        } catch (Exception ex) {
            System.err.println("Error:" + ex.toString());
            result = "0-" + "Error:" + ex.toString();
        }
        return result;
    }

    /**
     * This method read a file with users and filter which ones can be processed
     * by driver SGH-035 by theur brand.
     *
     * @param filePath path of file with users to process.
     * @param ldapServer IP or Hostname of ldap server
     * @param ldapPort Connection porf of the lpda instance
     * @param ldapConnDN DN of the user connection
     * @param ldapConnPwd Password of the user connection
     * @param ldapBaseDN Base OU where the search the object to modify is
     * excecuted
     * @param ldapAttribute pager attribute.
     * @param ldapAttributeValue SET-SGH-0365-EMAIL.
     * @return "1" if process was performed successfully or "0-" with the derail
     * of the error.
     */
    public static String ValidateIdmSAPClientConfiguration(String filePath, String ldapServer, int ldapPort, String ldapBaseDN, String ldapConnDN, String ldapConnPwd, String ldapAttribute, String ldapAttributeValue) {

        String result;
        String idmUserBrand;
        String idmWorkCountry;
        String jobCode;
        String idmJobFamily;
        String idmLOCNumber;
        String title;
        String luxCostCenter;
        String workforceID;
        String idmSAPClient;

        ArrayList<String> mpNoUsrs = new ArrayList<>();
        ArrayList<String> mpdblIdmSAPCLients = new ArrayList<>();
        ArrayList<String> mpNoUsrsNoIdmSAPClient = new ArrayList<>();

        ArrayList<String> idmSAPClients = new ArrayList<>();
        ArrayList<String> fltIdmSAPCLient = new ArrayList<>();
        ArrayList<String> dblIdmSAPCLients = new ArrayList<>();

        LDAPUtil ldapUtil = new LDAPUtil();

        try {

            File file = new File(filePath);
            try (Scanner scanner = new Scanner(file)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    idmUserBrand = line.split(";")[0].replace("N/A", "");
                    idmWorkCountry = line.split(";")[1].replace("N/A", "");
                    jobCode = line.split(";")[2].replace("N/A", "");
                    idmJobFamily = line.split(";")[3].replace("N/A", "");
                    idmLOCNumber = line.split(";")[4].replace("N/A", "");
                    title = line.split(";")[5].replace("N/A", "");
                    luxCostCenter = line.split(";")[6].replace("N/A", "");
                    workforceID = line.split(";")[7].replace("N/A", "");
                    idmSAPClient = line.split(";")[8];
                    idmSAPClients.add(idmSAPClient);

                    String ldapFilter = "(&";
                    if (!idmUserBrand.equals("")) {
                        idmUserBrand = "(idmUserBrand=" + idmUserBrand + ")";
                        ldapFilter = ldapFilter + idmUserBrand;
                    }
                    if (!idmWorkCountry.equals("")) {
                        idmWorkCountry = "(idmWorkCountry=" + idmWorkCountry + ")";
                        ldapFilter = ldapFilter + idmWorkCountry;
                    }
                    if (!jobCode.equals("")) {
                        jobCode = "(jobCode=" + jobCode + ")";
                        ldapFilter = ldapFilter + jobCode;
                    }
                    if (!idmJobFamily.equals("")) {
                        idmJobFamily = "(idmJobFamily=" + idmJobFamily + ")";
                        ldapFilter = ldapFilter + idmJobFamily;
                    }
                    if (!idmLOCNumber.equals("")) {
                        idmLOCNumber = "(idmLOCNumber=" + idmLOCNumber + ")";
                        ldapFilter = ldapFilter + idmLOCNumber;
                    }
                    if (!title.equals("")) {
                        title = "(title=" + title + ")";
                        ldapFilter = ldapFilter + title;
                    }
                    if (!luxCostCenter.equals("")) {
                        luxCostCenter = "(luxCostCenter=" + luxCostCenter + ")";
                        ldapFilter = ldapFilter + luxCostCenter;
                    }
                    if (!workforceID.equals("")) {
                        workforceID = "(workforceID=" + workforceID + ")";
                        ldapFilter = ldapFilter + workforceID;
                    }
                    ldapFilter = ldapFilter + ")";
                    if (!ldapFilter.equals("(&)")) {
                        dblIdmSAPCLients.add(idmSAPClient + ":" + ldapFilter);
                        fltIdmSAPCLient.add(idmSAPClient + ":" + ldapFilter);
                    }
                }
            }

            System.out.println("Validaitng idmSAPClient mappings...");
            Set<String> s = new HashSet<>();
            dblIdmSAPCLients.stream().filter(dblIdmSAPCLient -> (s.add(dblIdmSAPCLient) == false)).forEachOrdered(dblIdmSAPCLient -> {
                //System.out.println(dblIdmSAPCLient);
                mpNoUsrs.add(dblIdmSAPCLient);
            });

            System.out.println("Validaitng idmSAPClient mappings...");
            //Collections.sort(fltIdmSAPCLient);
            fltIdmSAPCLient.forEach(filer -> {
                String count = ldapUtil.GetObjectsCountFromQuery(ldapServer, ldapPort, ldapBaseDN, ldapConnDN, ldapConnPwd, filer.split(":")[1]);
                if (count.equals("0")) {
                    System.out.print("Number of users returned by mapping:" + filer + ":" + count + ", ");
                    System.out.print("Users with idmSAPClient " + filer.split(":")[0] + ":");
                    String cntUsrAssn = ldapUtil.GetObjectsCountFromQuery(ldapServer, ldapPort, ldapBaseDN, ldapConnDN, ldapConnPwd, "(idmSAPClient=" + filer.split(":")[0] + ")");
                    System.out.print(cntUsrAssn + "\n");
                    if (cntUsrAssn.equals("0")) {
                        mpNoUsrsNoIdmSAPClient.add(filer);
                    } else {
                        mpdblIdmSAPCLients.add(filer);
                    }
                } else {
                    System.out.print("Number of users returned by mapping:" + filer + ":" + count + "\n");
                }
            });

            System.out.println("Validating idmSAPCLient assignment ...");
            Set<String> set = new LinkedHashSet<>();
            set.addAll(idmSAPClients);
            idmSAPClients.clear();
            idmSAPClients.addAll(set);
            idmSAPClients.forEach(sapClient -> {
                System.out.println("Users by idmSAPClient's mapping " + sapClient);
            });

            System.out.println("Result:");
            System.out.println("Duplicate mappings:" + mpNoUsrs.size());
            mpNoUsrs.forEach(mapping -> {
                System.out.println(mapping);
            });

            System.out.println("Mapping with no users and no idmSAPClient assigned:" + mpNoUsrs.size());
            mpNoUsrsNoIdmSAPClient.forEach(mapping -> {
                System.out.println(mapping);
            });

            System.out.println("Mapping with no users:" + mpNoUsrs.size());
            mpdblIdmSAPCLients.forEach(mapping -> {
                System.out.println(mapping);
            });
            result = "1";
        } catch (FileNotFoundException e) {
            System.err.println("Error:" + e.toString());
            result = "0-" + "Error:" + e.toString();
        }
        return result;
    }

    public static String FixPVFUsers01(String ldapServer, int ldapPort, String ldapBaseDN, String ldapConnDN, String ldapConnPwd, String ldapFilter) {

        String result;
        String[] requestedAttributes = {"givenName", "sn", "title", "l", "siteLocation", "LuxCostCenter"};
        LDAPUtil ldapUtil = new LDAPUtil();
        try {
            ArrayList<String> users = ldapUtil.GetObjectsDNFromQuery(ldapServer, ldapPort, ldapBaseDN, ldapConnDN, ldapConnPwd, ldapFilter);
            for (String userDN : users) {
                String user = ldapUtil.GetAttributesValueFromObject(ldapServer, ldapPort, ldapConnDN, ldapConnPwd, userDN, requestedAttributes);
                if (user != null) {
                    String givenName;
                    String sn;
                    String title;
                    String l;
                    String siteLocation = "";
                    String newSiteLocation = "";
                    String luxCostCenter;
                    for (String attr : user.split("\\|")) {
                        String attrName = attr.split(":")[0];
                        switch (attrName) {
                            case "givenName":
                                givenName = attr.split("givenName:")[1].trim();
                                if (!givenName.equals("Licensed")) {
                                    String updateAttr = ldapUtil.SetAttributeValueByDN(ldapServer, ldapPort, ldapConnDN, ldapConnPwd, userDN, "givenName", "Licensed");
                                    if (updateAttr.equals("1")) {
                                        System.out.println("User " + userDN + " was updated. Attribute givenName (" + givenName + ") was updated with value \"Licensed\".");
                                    }
                                }
                                break;
                            case "sn":
                                sn = attr.split("sn:")[1].trim();
                                if (!sn.equals("Owner")) {
                                    String updateAttr = ldapUtil.SetAttributeValueByDN(ldapServer, ldapPort, ldapConnDN, ldapConnPwd, userDN, "sn", "Owner");
                                    if (updateAttr.equals("1")) {
                                        System.out.println("User " + userDN + " was updated. Attribute sn (" + sn + ") was updated with value \"Owner\".");
                                    }
                                }
                                break;
                            case "title":
                                title = attr.split("title:")[1].trim();
                                if (!title.equals("PLO_MGR")) {
                                    String updateAttr = ldapUtil.SetAttributeValueByDN(ldapServer, ldapPort, ldapConnDN, ldapConnPwd, userDN, "title", "PLO_MGR");
                                    if (updateAttr.equals("1")) {
                                        System.out.println("User " + userDN + " was updated. Attribute title (" + title + ") was updated with value \"PLO_MGR\".");
                                    }
                                }
                                break;
                            case "l":
                                l = attr.split("l:")[1].trim();
                                if (!l.equals("PEARLELO")) {
                                    String updateAttr = ldapUtil.SetAttributeValueByDN(ldapServer, ldapPort, ldapConnDN, ldapConnPwd, userDN, "l", "PEARLELO");
                                    if (updateAttr.equals("1")) {
                                        System.out.println("User " + userDN + " was updated. Attribute l (" + l + ") was updated with value \"PEARLELO\".");
                                    }
                                }
                                break;
                            case "siteLocation":
                                siteLocation = attr.split("siteLocation:")[1];
                                break;
                            case "LuxCostCenter":
                                luxCostCenter = attr.split("LuxCostCenter:")[1];
                                newSiteLocation = "PEARLE VISION " + luxCostCenter.replace("costCenter=", "").replace(",ou=places,o=idv", "");
                                break;
                        }
                    }
                    if (!siteLocation.equals(newSiteLocation)) {
                        String updateAttr = ldapUtil.SetAttributeValueByDN(ldapServer, ldapPort, ldapConnDN, ldapConnPwd, userDN, "siteLocation", newSiteLocation);
                        if (updateAttr.equals("1")) {
                            System.out.println("User " + userDN + " was updated. Attribute siteLocation (" + siteLocation + ") was updated with value \"" + newSiteLocation + "\".");
                        }
                    }
                }
            }
            result = "1";
        } catch (Exception ex) {
            result = "0" + ex.getMessage();
        }
        return result;
    }

    public static String FixPVFUsers02(String ldapServer, int ldapPort, String ldapBaseDN, String ldapConnDN, String ldapConnPwd, String ldapFilter) {

        String result;
        String[] requestedAttributes = {"givenName", "sn", "title", "l", "siteLocation", "LuxCostCenter"};
        LDAPUtil ldapUtil = new LDAPUtil();
        try {
            ArrayList<String> users = ldapUtil.GetObjectsDNFromQuery(ldapServer, ldapPort, ldapBaseDN, ldapConnDN, ldapConnPwd, ldapFilter);
            for (String userDN : users) {
                String user = ldapUtil.GetAttributesValueFromObject(ldapServer, ldapPort, ldapConnDN, ldapConnPwd, userDN, requestedAttributes);
                if (user != null) {
                    String givenName;
                    String sn;
                    String title;
                    String l;
                    String siteLocation = "";
                    String newSiteLocation = "";
                    String luxCostCenter;
                    for (String attr : user.split("\\|")) {
                        String attrName = attr.split(":")[0];
                        switch (attrName) {
                            case "givenName":
                                givenName = attr.split("givenName:")[1].trim();
                                if (!givenName.equals("Licensed")) {
                                    String updateAttr = ldapUtil.SetAttributeValueByDN(ldapServer, ldapPort, ldapConnDN, ldapConnPwd, userDN, "givenName", "Licensed");
                                    if (updateAttr.equals("1")) {
                                        System.out.println("User " + userDN + " was updated. Attribute givenName (" + givenName + ") was updated with value \"Licensed\".");
                                    }
                                }
                                break;
                            case "sn":
                                sn = attr.split("sn:")[1].trim();
                                if (!sn.equals("Store")) {
                                    String updateAttr = ldapUtil.SetAttributeValueByDN(ldapServer, ldapPort, ldapConnDN, ldapConnPwd, userDN, "sn", "Store");
                                    if (updateAttr.equals("1")) {
                                        System.out.println("User " + userDN + " was updated. Attribute sn (" + sn + ") was updated with value \"Store\".");
                                    }
                                }
                                break;
                            case "title":
                                title = attr.split("title:")[1].trim();
                                if (!title.equals("PLO_ASSOC")) {
                                    String updateAttr = ldapUtil.SetAttributeValueByDN(ldapServer, ldapPort, ldapConnDN, ldapConnPwd, userDN, "title", "PLO_ASSOC");
                                    if (updateAttr.equals("1")) {
                                        System.out.println("User " + userDN + " was updated. Attribute title (" + title + ") was updated with value \"PLO_ASSOC\".");
                                    }
                                }
                                break;
                            case "l":
                                l = attr.split("l:")[1].trim();
                                if (!l.equals("PEARLELO")) {
                                    String updateAttr = ldapUtil.SetAttributeValueByDN(ldapServer, ldapPort, ldapConnDN, ldapConnPwd, userDN, "l", "PEARLELO");
                                    if (updateAttr.equals("1")) {
                                        System.out.println("User " + userDN + " was updated. Attribute l (" + l + ") was updated with value \"PEARLELO\".");
                                    }
                                }
                                break;
                            case "siteLocation":
                                siteLocation = attr.split("siteLocation:")[1];
                                break;
                            case "LuxCostCenter":
                                luxCostCenter = attr.split("LuxCostCenter:")[1];
                                newSiteLocation = "PEARLE VISION " + luxCostCenter.replace("costCenter=", "").replace(",ou=places,o=idv", "");
                                break;
                        }
                    }
                    if (!siteLocation.equals(newSiteLocation)) {
                        String updateAttr = ldapUtil.SetAttributeValueByDN(ldapServer, ldapPort, ldapConnDN, ldapConnPwd, userDN, "siteLocation", newSiteLocation);
                        if (updateAttr.equals("1")) {
                            System.out.println("User " + userDN + " was updated. Attribute siteLocation (" + siteLocation + ") was updated with value \"" + newSiteLocation + "\".");
                        }
                    }
                }
            }
            result = "1";
        } catch (Exception ex) {
            result = "0" + ex.getMessage();
        }
        return result;
    }

    public static String PushUSUsersToADELCORP(String pathFile, String ldapServer, int ldapPort, String ldapBaseDN, String ldapConnDN, String ldapConnPwd, String ldapFilter) {

        String result = "1";
        ArrayList<String> usersToPush = new ArrayList<>();
        ArrayList<String> usersNotFund = new ArrayList<>();
        ArrayList<String> usersClrMail = new ArrayList<>();
        ArrayList<String> invalidUsers = new ArrayList<>();
        ArrayList<String> usersAddPwdPolicy = new ArrayList<>();
        String[] requestedAttributes = {"employeeStatus", "loginDisabled", "mail", "CN", "title", "ou"};
        LDAPUtil ldapUtil = new LDAPUtil();
        //1.Getting existing and valid users
        File file = new File(pathFile);
        try (Scanner scanner = new Scanner(file)) {
            System.out.println("1.Getting users from file...");
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String workForceID = line.split(";")[8];
                if (!workForceID.equals("eDir accountName")) {
                    //System.out.println("Line:" + line);
                    //System.out.println("workForceID:" + workForceID);   
                    ArrayList<String> users = ldapUtil.GetObjectsDNFromQuery(ldapServer, ldapPort, ldapBaseDN, ldapConnDN, ldapConnPwd, "(workFOrceID=" + workForceID + ")");
                    if (users.isEmpty()) {
                        users = ldapUtil.GetObjectsDNFromQuery(ldapServer, ldapPort, ldapBaseDN, ldapConnDN, ldapConnPwd, "(workFOrceID=0" + workForceID + ")");
                    }
                    if (users.isEmpty()) {
                        //System.out.println("User " + workForceID + " not found.");
                        usersNotFund.add(workForceID);
                    }
                    for (String userDN : users) {
                        String cn = "";
                        String ou = "";
                        String mail = "";
                        String title = "";
                        String loginDisabled = "";
                        String employeeStatus = "";
                        boolean validUser = true;
                        boolean clearMail = false;
                        //System.out.println("workForceID:" + workForceID + "|DN:" + userDN);
                        String ldapAttributes = ldapUtil.GetAttributesValueFromObject(ldapServer, ldapPort, ldapConnDN, ldapConnPwd, userDN, requestedAttributes);
                        for (String attr : ldapAttributes.split("\\|")) {
                            String attrName = attr.split(":")[0];
                            switch (attrName) {
                                case "employeeStatus":
                                    employeeStatus = attr.split("employeeStatus:")[1].trim();
                                    if (!employeeStatus.equalsIgnoreCase("Active")) {
                                        validUser = false;
                                    }
                                    break;
                                case "loginDisabled":
                                    loginDisabled = attr.split("loginDisabled:")[1].trim();
                                    if (!loginDisabled.equalsIgnoreCase("FALSE")) {
                                        validUser = false;
                                    }
                                    break;
                                case "mail":
                                    mail = attr.split("mail:")[1].trim();
                                    if (!mail.equals("NULL")) {
                                        clearMail = true;
                                    }
                                    break;
                                case "CN":
                                    cn = attr.split("CN:")[1].trim();
                                    if (cn.contains(", ")) {
                                        validUser = false;
                                    }
                                    break;
                                case "title":
                                    title = attr.split("title:")[1].trim();
                                    if (title.contains(", ")) {
                                        validUser = false;
                                    }
                                    break;
                                case "ou":
                                    ou = attr.split("ou:")[1].trim();
                                    if (ou.contains(", ")) {
                                        validUser = false;
                                    }
                                    break;
                            }
                        }
                        //System.out.println("workForceID:" + workForceID + "|DN:" + userDN + "|employeeStatus:" + employeeStatus + "|loginDisabled:" + loginDisabled);
                        if (validUser) {
                            usersToPush.add(userDN);
                            String strCont = ldapUtil.GetObjectsCountFromQuery(ldapServer, ldapPort, "cn=Global Policy No GraceLogin, cn=Password Policies, cn=Security", ldapConnDN, ldapConnPwd, "(nsimAssignments=" + userDN + ")");
                            if (Integer.getInteger(strCont) == 0) {
                                usersAddPwdPolicy.add(userDN);
                            }
                        } else {
                            invalidUsers.add(userDN + "|CN:" + cn + "|workForceID:" + workForceID + "|employeeStatus:" + employeeStatus + "|loginDisabled:" + loginDisabled + "|Title:" + title + "|OU:" + ou);
                        }
                        if (clearMail && validUser) {
                            usersClrMail.add(userDN);
                        }
                    }
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(NetStudioLibrary.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println(ex);
            result = "0-" + ex.getMessage();
        }
        System.out.println("2.Processing users...");
        //2.Process users:
        System.out.println("2.1.Adding users to Password Policy 'Global Policy No GraceLogin'...");
        //2.1.Add user to Global Policy No GraceLogin.Password Policies.Security:
        usersAddPwdPolicy.forEach(userAddPwdPolicy -> {
            String addAttributeValueByDN = ldapUtil.AddAttributeValueByDN(ldapServer, ldapPort, ldapConnDN, ldapConnPwd, "cn=Global Policy No GraceLogin, cn=Password Policies, cn=Security", "nsimAssignments", userAddPwdPolicy);
        });
        //2.2.Set attribute idmAzurePwdCheckStatus with value "tocheck":
        System.out.println("2.2.Setting attribute idmAzurePwdCheckStatus with value 'tocheck'...");
        usersToPush.forEach(userToPush -> {
            String setAttributeValueByDN = ldapUtil.SetAttributeValueByDN(ldapServer, ldapPort, ldapConnDN, ldapConnPwd, userToPush, "idmAzurePwdCheckStatus", "tocheck");
        });
        //2.3.Run Job "User Password Compliane Trigger" in Driver LuxIDV2LuxGroup.
        System.out.println("2.3.Waiting job excecution...");
        Scanner sc = new Scanner(System.in);
        String scInput = sc.nextLine();
        //2.4.Clear attribute mail
        System.out.println("2.4.Clearing attribute mail...");
        usersClrMail.forEach(userClrMail -> {
            String clearAttributeValueByDN = ldapUtil.ClearAttributeValueByDN(ldapServer, ldapPort, ldapConnDN, ldapConnPwd, userClrMail, "mail");
        });
        //2.5.Set attribute idmADExtensionAttribute with value "ADSyncCloud"
        System.out.println("2.4.Setting attribute idmADExtensionAttribute with value 'ADSyncCloud'...");
        usersToPush.forEach(userToPush -> {
            String setAttributeValueByDN = ldapUtil.SetAttributeValueByDN(ldapServer, ldapPort, ldapConnDN, ldapConnPwd, userToPush, "idmADExtensionAttribute", "ADSyncCloud");
        });

        //3.Final report:
        System.out.println("=================================================");
        System.out.println("3.Final report:");
        //Users not found
        System.out.println("3.1.Users not found in LDAP:");
        if (!usersNotFund.isEmpty()) {
            usersNotFund.forEach(useroNotFound -> {
                System.out.println(" workForceID:" + useroNotFound);
            });
        } else {
            System.out.println(" All users were found in LDAP.");
        }
        //Terminated or Disabled users
        System.out.println("3.2.Users Terminated or Disabled or with invalid data");
        if (!invalidUsers.isEmpty()) {
            invalidUsers.forEach(userTerminated -> {
                System.out.println(" DN:" + userTerminated);
            });
        } else {
            System.out.println(" All users were active in LDAP.");
        }
        //Processed users
        System.out.println("3.3.Processed users:");
        if (!usersToPush.isEmpty()) {
            usersToPush.forEach(userToPush -> {
                System.out.println(" DN:" + userToPush);
            });
        } else {
            System.out.println(" No user was processed.");
        }
        System.out.println("End.");
        System.out.println("=================================================");
        return result;
    }       
}
