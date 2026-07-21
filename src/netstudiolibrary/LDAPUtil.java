/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package netstudiolibrary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.PagedResultsControl;

import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.PagedResultsResponseControl;

/**
 *
 * @author clgarcias
 */
public class LDAPUtil {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        //String test = ValidateGroupMembers("10.110.7.156", 389, "ou=groups,o=idv", "cn=admin,ou=noc,o=idv", "Gl@ss12f1ll", "(cn=AccountExemption)");
        // System.out.println("Test:" + test);

    }

    /**
     * This method add, set, or remove attribute values from objects returned
     * from query
     *
     * @param ldapServer IP or Hostname of ldap server
     * @param ldapPort Connection porf of the lpda instance
     * @param ldapConnDN DN of the user connection
     * @param ldapConnPwd Password of the user connection
     * @param ldapBaseDN Base OU where the search the object to modify is
     * excecuted
     * @param ldapFilter Ldap filter to search the object to modify
     * @param operation Ldap operation to performe on attribute add, set, or
     * remove.
     * @param ldapAttribute Ldap attribute to set
     * @param ldapAttributeValue Ldap attribute value to set
     * @return Return "1" if process was performed successfully or "0-" with the
     * derail of the error.
     */
    String RemoveEntitlementFromUsers(String ldapServer, int ldapPort, int countLimit, String ldapBaseDN, String ldapConnDN, String ldapConnPwd, String ldapFilter, String entitlementDN, int entitlementAssigned, String entitlementPolicySlashDN, String entitlementValueSlashDN, String entitlementValueDN) {

        int countEntlpolicyMembers = 0;
        int countMembersOfGroup = 0;
        int pageSize = 100;
        String result = "1";
        ArrayList<String> objectsUpdated = new ArrayList<>();

        try {
            System.out.println("Validating...");
            System.out.println("Entitlement:" + entitlementDN);
            System.out.println("Policy:" + entitlementPolicySlashDN);
            System.out.println("Value:" + entitlementValueSlashDN);
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, "ldap://" + ldapServer + ":" + ldapPort);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            if (ldapPort == 636) {
                env.put(Context.SECURITY_PROTOCOL, "ssl");
            }
            env.put(Context.SECURITY_PRINCIPAL, ldapConnDN);
            env.put(Context.SECURITY_CREDENTIALS, ldapConnPwd);

            LdapContext ctx = new InitialLdapContext(env, null);
            ctx.setRequestControls(new Control[]{new PagedResultsControl(pageSize, Control.NONCRITICAL)});
            int page = 0;
            byte[] cookie = null;
            do {
                // Realizar la búsqueda
                SearchControls searchControls = new SearchControls();
                searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
                //searchControls.setReturningAttributes(new String[]{"*", "+"}); // Obtener todos los atributos
                searchControls.setReturningAttributes(new String[]{"DirXML-EntitlementRef", "DirXML-EntitlementResult", "groupMembership"});
                if (countLimit > 0) {
                    searchControls.setCountLimit(countLimit);
                }

                if (ldapFilter.equals("")) {
                    String filter = "(DirXML-EntitlementRef=" + entitlementDN + "#" + entitlementAssigned + "#<ref><src>RBE</src><id>" + entitlementPolicySlashDN.replace("\\", "\\\\") + "</id><param>" + entitlementValueSlashDN.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)") + "</param></ref>)";
                    filter = "(&(!(groupMembership=" + entitlementValueDN.replace("/", "\\/").replace("(", "\\(").replace(")", "\\)") + "))" + filter + ")";
                    ldapFilter = filter;
                }
                System.out.println("Filter:" + ldapFilter);
                String entitlementValue = entitlementDN + "#" + entitlementAssigned + "#<ref><src>RBE</src><id>" + entitlementPolicySlashDN + "</id><param>" + entitlementValueSlashDN + "</param></ref>";

                NamingEnumeration<?> results = ctx.search(ldapBaseDN, ldapFilter, searchControls);
                while (results.hasMore()) {
                    // Procesar un resultado
                    SearchResult sr = (javax.naming.directory.SearchResult) results.next();
                    String objectDN = sr.getName() + "," + ldapBaseDN;
                    objectsUpdated.add(objectDN);
                    System.out.println("\r\n" + objectDN);
                    Attributes attributes = sr.getAttributes();

                    Attribute attrGroupMembership = attributes.get("groupMembership");
                    if (attrGroupMembership != null) {
                        for (int i = 0; i < attrGroupMembership.size(); i++) {
                            if (attrGroupMembership.get(i).toString().equals(entitlementValueDN)) {
                                System.out.println("Removing group: " + attrGroupMembership.get(i).toString());
                                ModificationItem[] mods = new ModificationItem[1];
                                Attribute attrGrpMembership = new BasicAttribute("groupMembership", attrGroupMembership.get(i).toString());
                                mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, attrGrpMembership);
                                ctx.modifyAttributes(objectDN, mods);
                            }
                        }
                    }

                    Attribute attrDirXMLEntitlementResult = attributes.get("DirXML-EntitlementResult");
                    if (attrDirXMLEntitlementResult != null) {
                        for (int i = 0; i < attrDirXMLEntitlementResult.size(); i++) {
                            String entitlementResult = attrDirXMLEntitlementResult.get(i).toString();
                            String entitlementResultDN = entitlementResult.substring(0, entitlementResult.indexOf("</dn>")).replace("<result><dn>", "");
                            if (entitlementResultDN.equalsIgnoreCase(entitlementDN)) {
                                String entitlementResultPolicyDN = entitlementResult.substring(entitlementResult.indexOf("<id>"), entitlementResult.indexOf("</id>")).replace("<id>", "");
                                String entitlementResultValueDN = entitlementResult.substring(entitlementResult.indexOf("<param>"), entitlementResult.indexOf("</param>")).replace("<param>", "");
                                //System.out.println(entitlementPolicySlashDN  + "|" + entitlementPolicySlashDN.replaceAll("\\p{C}", "").length());
                                if (entitlementResultPolicyDN.trim().equalsIgnoreCase(entitlementPolicySlashDN.trim())) {
                                    //if (entitlementValueSlashDN.equalsIgnoreCase(entitlementResultValueDN)) {
                                    System.out.println("Removing entitlement result: " + entitlementResult);
                                    ModificationItem[] mods = new ModificationItem[1];
                                    Attribute attrEntitlementResult = new BasicAttribute("DirXML-EntitlementResult", entitlementResult);
                                    mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, attrEntitlementResult);
                                    ctx.modifyAttributes(objectDN, mods);
                                    //}
                                }
                            }
                            //System.out.println(entitlementResult);
                        }
                    }

                    Attribute attrDirXMLEntitlementRef = attributes.get("DirXML-EntitlementRef");
                    if (attrDirXMLEntitlementRef != null) {
                        for (int i = 0; i < attrDirXMLEntitlementRef.size(); i++) {
                            if (attrDirXMLEntitlementRef.get(i).toString().equals(entitlementValue)) {
                                System.out.println("Removing entitlement reference: " + attrDirXMLEntitlementRef.get(i).toString());
                                ModificationItem[] mods = new ModificationItem[1];
                                Attribute attrEntitlementRef = new BasicAttribute("DirXML-EntitlementRef", attrDirXMLEntitlementRef.get(i).toString());
                                mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, attrEntitlementRef);
                                ctx.modifyAttributes(objectDN, mods);
                            } else {
                                String auxEntitlementDN = attrDirXMLEntitlementRef.get(i).toString();
                                //System.out.println(entitlementValue);
                                //System.out.println(auxEntitlementDN);
                                String auxntitlementValueDN = auxEntitlementDN.substring(0, auxEntitlementDN.indexOf("#"));
                                if (auxntitlementValueDN.equals(entitlementDN)) {
                                    String auxEntitlementPolicySlashDN = auxEntitlementDN.substring(auxEntitlementDN.indexOf("<id>"), auxEntitlementDN.indexOf("</id>")).replace("<id>", "").replace("</id>", "");
                                    //System.out.println(auxntitlementValueDN + "|" + auxEntitlementPolicySlashDN);
                                    if (auxEntitlementPolicySlashDN.equals(entitlementPolicySlashDN)) {
                                        //System.out.println(auxEntitlementPolicySlashDN);
                                        //System.out.println(auxentitlementValueSlashDN);
                                        //System.out.println(entitlementValueSlashDN);
                                        System.out.println("Removing entitlement reference: " + attrDirXMLEntitlementRef.get(i).toString());
                                        ModificationItem[] mods = new ModificationItem[1];
                                        Attribute attrEntitlementRef = new BasicAttribute("DirXML-EntitlementRef", attrDirXMLEntitlementRef.get(i).toString());
                                        mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, attrEntitlementRef);
                                        ctx.modifyAttributes(objectDN, mods);
                                    } else if (entitlementPolicySlashDN.startsWith(auxEntitlementPolicySlashDN)) {
                                        System.out.println("Removing entitlement reference: " + attrDirXMLEntitlementRef.get(i).toString());
                                        ModificationItem[] mods = new ModificationItem[1];
                                        Attribute attrEntitlementRef = new BasicAttribute("DirXML-EntitlementRef", attrDirXMLEntitlementRef.get(i).toString());
                                        mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, attrEntitlementRef);
                                        ctx.modifyAttributes(objectDN, mods);
                                    }
                                }
                            }
                        }
                    }
                }
                results.close();

                // Obtener el siguiente cookie y verificar si hay más resultados
                Control[] controls = ctx.getResponseControls();
                if (controls != null) {
                    for (Control control : controls) {
                        if (control instanceof PagedResultsResponseControl) {
                            PagedResultsResponseControl pagedResultsResponseControl = (PagedResultsResponseControl) control;
                            cookie = pagedResultsResponseControl.getCookie();
                            // Imprimir el tamaño de la página devuelta
                            //System.out.println("Tamaño de página devuelto: " + pagedResultsResponseControl.getResultSize());
                        }
                    }
                }

                // Establecer el cookie para la siguiente página
                if (cookie != null) {
                    ctx.setRequestControls(new Control[]{new PagedResultsControl(pageSize, cookie, Control.NONCRITICAL)});
                }

                page++;

            } while (cookie != null);
            ctx.close();
            System.out.println("\r\nProcessed users:");
            objectsUpdated.forEach(objectUpdated -> {
                System.out.println(objectUpdated);
            });
        } catch (NamingException | IOException ex) {
            result = "0-" + ex.getMessage();
            System.out.println("\r\nProcessed users:");
            objectsUpdated.forEach(objectUpdated -> {
                System.out.println(objectUpdated);
            });
        }
        return result;
    }

    /**
     * This method validate the relation between meber, equivalnteToMe and
     * users' groupMembership.
     *
     * @param ldapServer IP or Hostname of ldap server
     * @param ldapPort Connection porf of the lpda instance
     * @param ldapConnDN DN of the user connection
     * @param ldapConnPwd Password of the user connection
     * @param ldapGrpBaseDN Base OU where groups are.
     * @param ldapUsrBaseDN Base OU where usres are.
     * @param ldapFilter Ldap filter to search the object to modify
     * @return Return "1" if process was performed successfully or "0-" with the
     * derail of the error.
     */
    String ValidateGroupMembers(String ldapServer, int ldapPort, String ldapGrpBaseDN, String ldapUsrBaseDN, String ldapConnDN, String ldapConnPwd, String ldapFilter) {

        String grpDN = "";
        String usrDN = "";
        String result = "1";

        ArrayList<String> members = new ArrayList<>();
        ArrayList<String> equivalentsToMe = new ArrayList<>();
        ArrayList<String> users = new ArrayList<>();
        ArrayList<String> securityEquals = new ArrayList<>();

        DirContext ctx = null;

        try {

            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, "ldap://" + ldapServer + ":" + ldapPort);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            if (ldapPort == 636) {
                env.put(Context.SECURITY_PROTOCOL, "ssl");
            }
            env.put(Context.SECURITY_PRINCIPAL, ldapConnDN);
            env.put(Context.SECURITY_CREDENTIALS, ldapConnPwd);

            ctx = new InitialDirContext(env);

            //DN des zu ändernden Objekts abrufen
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setReturningAttributes(new String[]{"member", "equivalentToMe"});
            NamingEnumeration<SearchResult> results = ctx.search(ldapGrpBaseDN, ldapFilter, searchControls);
            while (results.hasMore()) {
                SearchResult sr = results.next();
                grpDN = sr.getName() + "," + ldapGrpBaseDN;
                System.out.println("Starting validating group " + grpDN);
                System.out.println("Getting Group Members ...");
                Attributes attributes = sr.getAttributes();
                Attribute attrMember = attributes.get("member");
                if (attrMember != null) {
                    for (int i = 0; i < attrMember.size(); i++) {
                        members.add(attrMember.get(i).toString());
                    }
                }
                System.out.println("Group Members returned:" + members.size());
                System.out.println("Getting Group equivalentToMe ...");
                Attribute attr = attributes.get("equivalentToMe");
                if (attr != null) {
                    for (int i = 0; i < attr.size(); i++) {
                        equivalentsToMe.add(attr.get(i).toString());
                    }
                }
                System.out.println("Group equivalentToMe returned:" + equivalentsToMe.size());
            }
            ctx.close();
            System.out.println("Getting users with Group ...");
            ctx = new InitialDirContext(env);
            searchControls.setReturningAttributes(new String[]{"cn"});
            results = ctx.search(ldapGrpBaseDN.substring(ldapGrpBaseDN.indexOf("o=")), "(GroupMembership=" + grpDN + ")", searchControls);
            while (results.hasMore()) {
                SearchResult sr = results.next();
                usrDN = sr.getName() + "," + ldapGrpBaseDN.substring(ldapGrpBaseDN.indexOf("o="));
                users.add(usrDN);
            }
            System.out.println("Users returned:" + users.size());
            ctx.close();
            System.out.println("Getting users with SecurityEquals ...");
            ctx = new InitialDirContext(env);
            searchControls.setReturningAttributes(new String[]{"cn"});
            results = ctx.search(ldapGrpBaseDN.substring(ldapGrpBaseDN.indexOf("o=")), "(securityEquals=" + grpDN + ")", searchControls);
            while (results.hasMore()) {
                SearchResult sr = results.next();
                usrDN = sr.getName() + "," + ldapGrpBaseDN.substring(ldapGrpBaseDN.indexOf("o="));
                securityEquals.add(usrDN);
            }
            System.out.println("Security equals returned:" + securityEquals.size());
            ctx.close();

            System.out.println("\nValidating securityEquals with no membership ...");
            ctx = new InitialDirContext(env);
            searchControls.setReturningAttributes(new String[]{"cn"});
            results = ctx.search(ldapGrpBaseDN.substring(ldapGrpBaseDN.indexOf("o=")), "(&(!(GroupMembership=" + grpDN + "))(securityEquals=" + grpDN + "))", searchControls);
            while (results.hasMore()) {
                try {
                    SearchResult sr = results.next();
                    usrDN = sr.getName() + "," + ldapGrpBaseDN.substring(ldapGrpBaseDN.indexOf("o="));
                    System.out.println("User " + usrDN + " has securityEquals but not membership.");
                    System.out.println("    Removing group from securityEquals ...");
                    ModificationItem[] mods = new ModificationItem[1];
                    Attribute securityEqualsAttr = new BasicAttribute("securityEquals", grpDN);
                    mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, securityEqualsAttr);
                    ctx.modifyAttributes(usrDN, mods);
                } catch (NamingException ex) {
                    System.out.println("Error removing group from securityEquals: " + ex.toString());
                    ctx = new InitialDirContext(env);
                }
            }
            ctx.close();

            System.out.println("\nValidating members with no securityEquals ...");
            ctx = new InitialDirContext(env);
            searchControls.setReturningAttributes(new String[]{"cn"});
            results = ctx.search(ldapGrpBaseDN.substring(ldapGrpBaseDN.indexOf("o=")), "(&(!(securityEquals=" + grpDN + "))(GroupMembership=" + grpDN + "))", searchControls);
            while (results.hasMore()) {
                try {
                    SearchResult sr = results.next();
                    usrDN = sr.getName() + "," + ldapGrpBaseDN.substring(ldapGrpBaseDN.indexOf("o="));
                    System.out.println("User " + usrDN + " has group but not securityEquals.");
                    System.out.println("    Adding group to securityEquals ...");
                    ModificationItem[] mods = new ModificationItem[1];
                    Attribute securityEqualsAttr = new BasicAttribute("securityEquals", grpDN);
                    mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, securityEqualsAttr);
                    ctx.modifyAttributes(usrDN, mods);
                } catch (NamingException ex) {
                    System.out.println("Error removing group from securityEquals: " + ex.toString());
                    ctx = new InitialDirContext(env);
                }
            }
            ctx.close();
            System.out.println("    Getting users with SecurityEquals ...");
            securityEquals.clear();
            ctx = new InitialDirContext(env);
            searchControls.setReturningAttributes(new String[]{"cn"});
            results = ctx.search(ldapGrpBaseDN.substring(ldapGrpBaseDN.indexOf("o=")), "(securityEquals=" + grpDN + ")", searchControls);
            while (results.hasMore()) {
                SearchResult sr = results.next();
                usrDN = sr.getName() + "," + ldapGrpBaseDN.substring(ldapGrpBaseDN.indexOf("o="));
                securityEquals.add(usrDN);
            }
            System.out.println("    Security equals returned:" + securityEquals.size());
            ctx.close();

            System.out.println("    Getting Group Members ...");
            members.clear();
            equivalentsToMe.clear();
            ctx = new InitialDirContext(env);
            searchControls.setReturningAttributes(new String[]{"member", "equivalentToMe"});
            results = ctx.search(ldapGrpBaseDN, ldapFilter, searchControls);
            while (results.hasMore()) {
                SearchResult sr = results.next();
                grpDN = sr.getName() + "," + ldapGrpBaseDN;
                Attributes attributes = sr.getAttributes();
                Attribute attrMember = attributes.get("member");
                if (attrMember != null) {
                    for (int i = 0; i < attrMember.size(); i++) {
                        members.add(attrMember.get(i).toString());
                    }
                }
                System.out.println("    Group Members returned:" + members.size());
                System.out.println("    Getting Group equivalentToMe ...");
                Attribute attr = attributes.get("equivalentToMe");
                if (attr != null) {
                    for (int i = 0; i < attr.size(); i++) {
                        equivalentsToMe.add(attr.get(i).toString());
                    }
                }
                System.out.println("    Group equivalentToMe returned:" + equivalentsToMe.size());
            }
            ctx.close();

            System.out.println("\nValidating attribute member and groupMembership ...");
            for (String user : users) {
                int index = members.indexOf(user);
                if (index == -1) {
                    System.out.println("Group " + grpDN + " doesn't have the member " + user + " assigned.");
                    System.out.println("    Adding user to member attribute...");
                    ctx = new InitialDirContext(env);
                    ModificationItem[] mods = new ModificationItem[1];
                    Attribute equivalentToMeAttr = new BasicAttribute("member", user);
                    mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, equivalentToMeAttr);
                    ctx.modifyAttributes(grpDN, mods);
                    ctx.close();
                }
            }
            for (String member : members) {
                int index = users.indexOf(member);
                if (index == -1) {
                    System.out.println("User " + member + " doesn't have the group " + grpDN + " assigned.");
                    System.out.println("    Removing user from member attribute ...");
                    ctx = new InitialDirContext(env);
                    ModificationItem[] mods = new ModificationItem[1];
                    Attribute memberAttr = new BasicAttribute("member", member);
                    mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, memberAttr);
                    ctx.modifyAttributes(grpDN, mods);
                    ctx.close();
                }
            }
            System.out.println("    Getting Group Members ...");
            members.clear();
            equivalentsToMe.clear();
            ctx = new InitialDirContext(env);
            searchControls.setReturningAttributes(new String[]{"member", "equivalentToMe"});
            results = ctx.search(ldapGrpBaseDN, ldapFilter, searchControls);
            while (results.hasMore()) {
                SearchResult sr = results.next();
                grpDN = sr.getName() + "," + ldapGrpBaseDN;
                Attributes attributes = sr.getAttributes();
                Attribute attrMember = attributes.get("member");
                if (attrMember != null) {
                    for (int i = 0; i < attrMember.size(); i++) {
                        members.add(attrMember.get(i).toString());
                    }
                }
                System.out.println("    Group Members returned:" + members.size());
                System.out.println("    Getting Group equivalentToMe ...");
                Attribute attr = attributes.get("equivalentToMe");
                if (attr != null) {
                    for (int i = 0; i < attr.size(); i++) {
                        equivalentsToMe.add(attr.get(i).toString());
                    }
                }
                System.out.println("    Group equivalentToMe returned:" + equivalentsToMe.size());
            }
            ctx.close();

            System.out.println("\nValidating attributes member vs equivalentToMe...");
            ArrayList<String> addEquivalentToMe = new ArrayList<>();
            for (String member : members) {
                int index = equivalentsToMe.indexOf(member);
                if (index == -1) {
                    addEquivalentToMe.add(member);
                    //System.out.println("Group " + grpDN + " doesn't have the equivalentToMe " + member + " assigned.");
                    //System.out.println("    Adding member to equivalentToMe ...");
                    /*ctx = new InitialDirContext(env);
                    ModificationItem[] mods = new ModificationItem[1];
                    Attribute equivalentToMeAttr = new BasicAttribute("equivalentToMe", member);
                    mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, equivalentToMeAttr);
                    ctx.modifyAttributes(grpDN, mods);
                    ctx.close();*/
                }
            }
            if (addEquivalentToMe.size() > 0) {
                ctx = new InitialDirContext(env);
                for (String equivalentToMe : addEquivalentToMe) {
                    System.out.println("Group " + grpDN + " doesn't have the equivalentToMe " + equivalentToMe + " assigned.");
                    System.out.println("    Adding member to equivalentToMe ...");
                    ModificationItem[] mods = new ModificationItem[1];
                    Attribute equivalentToMeAttr = new BasicAttribute("equivalentToMe", equivalentToMe);
                    mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, equivalentToMeAttr);
                    ctx.modifyAttributes(grpDN, mods);
                }
                ctx.close();
            }
            for (String equivalentToMe : equivalentsToMe) {
                int index = users.indexOf(equivalentToMe);
                if (index == -1) {
                    System.out.println("User " + equivalentToMe + " doesn't have the group " + grpDN + " assigned.");
                    System.out.println("    Removing user from equivalentToMe ...");
                    ctx = new InitialDirContext(env);
                    ModificationItem[] mods = new ModificationItem[1];
                    Attribute equivalentToMeAttr = new BasicAttribute("equivalentToMe", equivalentToMe);
                    mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, equivalentToMeAttr);
                    ctx.modifyAttributes(grpDN, mods);
                    ctx.close();
                }
            }
            System.out.println("    Getting Group Members ...");
            members.clear();
            equivalentsToMe.clear();
            ctx = new InitialDirContext(env);
            searchControls.setReturningAttributes(new String[]{"member", "equivalentToMe"});
            results = ctx.search(ldapGrpBaseDN, ldapFilter, searchControls);
            while (results.hasMore()) {
                SearchResult sr = results.next();
                grpDN = sr.getName() + "," + ldapGrpBaseDN;
                Attributes attributes = sr.getAttributes();
                Attribute attrMember = attributes.get("member");
                if (attrMember != null) {
                    for (int i = 0; i < attrMember.size(); i++) {
                        members.add(attrMember.get(i).toString());
                    }
                }
                System.out.println("    Group Members returned:" + members.size());
                System.out.println("    Getting Group equivalentToMe ...");
                Attribute attr = attributes.get("equivalentToMe");
                if (attr != null) {
                    for (int i = 0; i < attr.size(); i++) {
                        equivalentsToMe.add(attr.get(i).toString());
                    }
                }
                System.out.println("    Group equivalentToMe returned:" + equivalentsToMe.size());
            }
            ctx.close();

            System.out.println("\nValidating attribute member and securityEqulas ...");
            //Validate attribute member
            members.clear();
            ctx = new InitialDirContext(env);
            searchControls.setReturningAttributes(new String[]{"member"});
            results = ctx.search(ldapGrpBaseDN, ldapFilter, searchControls);
            while (results.hasMore()) {
                SearchResult sr = results.next();
                grpDN = sr.getName() + "," + ldapGrpBaseDN;
                System.out.println(" Getting Group Members ...");
                Attributes attributes = sr.getAttributes();
                Attribute attrMember = attributes.get("member");
                if (attrMember != null) {
                    for (int i = 0; i < attrMember.size(); i++) {
                        members.add(attrMember.get(i).toString());
                    }
                }
                System.out.println(" Group Members returned:" + members.size());
            }
            ctx.close();

            for (String member : members) {
                int index = users.indexOf(member);
                if (index == -1) {
                    System.out.println("User " + member + " doesn't have the group " + grpDN + " assigned.");
                    System.out.println("    Removing user from member attribute ...");
                    ctx = new InitialDirContext(env);
                    ModificationItem[] mods = new ModificationItem[1];
                    Attribute memberAttr = new BasicAttribute("member", member);
                    mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, memberAttr);
                    ctx.modifyAttributes(grpDN, mods);
                    ctx.close();
                } else {
                    index = securityEquals.indexOf(member);
                    if (index == -1) {
                        System.out.println("User " + member + " doesn't have the securityEquals " + grpDN + " assigned.");
                        System.out.println("    Adding group to securityEquals ...");
                        try {
                            ctx = new InitialDirContext(env);
                            ModificationItem[] mods = new ModificationItem[1];
                            Attribute securityEqualsAttr = new BasicAttribute("securityEquals", grpDN);
                            mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, securityEqualsAttr);
                            ctx.modifyAttributes(member, mods);
                            ctx.close();
                        } catch (NamingException ex) {
                            System.out.println("Error updating user " + member + ":" + ex.toString());
                        }
                    }
                }
            }

            System.out.println("\nValidating attribute EquivalentToMe...");
            //Validate attribute EquivalentToMe
            for (String equivalentToMe : equivalentsToMe) {
                int index = users.indexOf(equivalentToMe);
                if (index == -1) {
                    System.out.println("User " + equivalentToMe + " doesn't have the group " + grpDN + " assigned.");
                    System.out.println("    Removing user from equivalentToMe ...");
                    ctx = new InitialDirContext(env);
                    ModificationItem[] mods = new ModificationItem[1];
                    Attribute equivalentToMeAttr = new BasicAttribute("equivalentToMe", equivalentToMe);
                    mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, equivalentToMeAttr);
                    ctx.modifyAttributes(grpDN, mods);
                    ctx.close();
                }
            }

            System.out.println("\nValidating attributes member vs equivalentToMe...");
            //Compare member with equivalentToMe
            members.clear();
            equivalentsToMe.clear();
            ctx = new InitialDirContext(env);
            searchControls.setReturningAttributes(new String[]{"member", "equivalentToMe"});
            results = ctx.search(ldapGrpBaseDN, ldapFilter, searchControls);
            while (results.hasMore()) {
                SearchResult sr = results.next();
                grpDN = sr.getName() + "," + ldapGrpBaseDN;
                System.out.println(" Getting Group Members ...");
                Attributes attributes = sr.getAttributes();
                Attribute attrMember = attributes.get("member");
                if (attrMember != null) {
                    for (int i = 0; i < attrMember.size(); i++) {
                        members.add(attrMember.get(i).toString());
                    }
                }
                System.out.println(" Group Members returned:" + members.size());
                System.out.println(" Getting Group equivalentToMe ...");
                Attribute attr = attributes.get("equivalentToMe");
                if (attr != null) {
                    for (int i = 0; i < attr.size(); i++) {
                        equivalentsToMe.add(attr.get(i).toString());
                    }
                }
                System.out.println(" Group equivalentToMe returned:" + equivalentsToMe.size());
            }
            ctx.close();
            for (String member : members) {
                int index = equivalentsToMe.indexOf(member);
                if (index == -1) {
                    System.out.println("Group " + grpDN + " doesn't have the equivalentToMe " + member + " assigned.");
                    System.out.println("    Adding member to equivalentToMe ...");
                    ctx = new InitialDirContext(env);
                    ModificationItem[] mods = new ModificationItem[1];
                    Attribute equivalentToMeAttr = new BasicAttribute("equivalentToMe", member);
                    mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, equivalentToMeAttr);
                    ctx.modifyAttributes(grpDN, mods);
                    ctx.close();
                }
            }
            result = "1";
            System.out.println("Validating group " + grpDN + " finished.");
        } catch (NamingException e) {
            System.err.println("Error:" + e.toString());
            result = "0-" + "Error:" + e.toString();
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    System.err.println("Error:" + e.toString());
                    result = "0-" + "Error:" + e.toString();
                }
            }
        }
        return result;
    }

    /**
     * This method get the value in attribute.
     *
     * @param ldapServer IP or Hostname of ldap server
     * @param ldapPort Connection porf of the lpda instance
     * @param ldapConnDN DN of the user connection
     * @param ldapConnPwd Password of the user connection
     * @param ldapBaseDN Base OU where the search the object to modify is
     * excecuted
     * @param ldapFilter Ldap filter to search the object to modify
     * @param ldapAttribute Ldap attribute to set
     * @param ldapAttributeValue Ldap attribute value to set
     * @return Return "1" if process was performed successfully or "0-" with the
     * derail of the error.
     */
    String LDAPConnect(String ldapServer, int ldapPort, String ldapBaseDN, String ldapConnDN, String ldapConnPwd, String ldapFilter, String ldapAttribute) {

        String result = "0-No value";
        DirContext ctx = null;

        try {

            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, "ldaps://" + ldapServer + ":" + "636");
            env.put(Context.SECURITY_PROTOCOL, "ssl");
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, ldapConnDN);
            env.put(Context.SECURITY_CREDENTIALS, ldapConnPwd);

            ctx = new InitialDirContext(env);

            //DN des zu ändernden Objekts abrufen
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            // Specify attributes to retrieve (e.g., mail, sn, cn)
            searchControls.setReturningAttributes(new String[]{ldapAttribute});

            NamingEnumeration<SearchResult> results = ctx.search(ldapBaseDN, ldapFilter, searchControls);
            while (results.hasMore()) {
                SearchResult sr = results.next();
                Attributes attributes = sr.getAttributes();

                Attribute attr = attributes.get(ldapAttribute);
                if (attr != null) {
                    result = attr.get().toString();
                    System.out.println("Attribute " + ldapAttribute + ": " + result);
                }
            }
            ctx.close();
        } catch (NamingException e) {
            System.err.println("Error:" + e.toString());
            result = "0-" + "Error:" + e.toString();
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    System.err.println("Error:" + e.toString());
                    result = "0-" + "Error:" + e.toString();
                }
            }
        }
        return result;
    }

    /**
     * This method set a value in attribute
     *
     * @param ldapServer IP or Hostname of ldap server
     * @param ldapPort Connection porf of the lpda instance
     * @param ldapConnDN DN of the user connection
     * @param ldapConnPwd Password of the user connection
     * @param ldapBaseDN Base OU where the search the object to modify is
     * excecuted
     * @param ldapFilter Ldap filter to search the object to modify
     * @param ldapAttribute Ldap attribute to set
     * @param ldapAttributeValue Ldap attribute value to set
     * @return Return "1" if process was performed successfully or "0-" with the
     * derail of the error.
     */
    String SetAttributeValue(String ldapServer, int ldapPort, String ldapBaseDN, String ldapConnDN, String ldapConnPwd, String ldapFilter, String ldapAttribute, String ldapAttributeValue) {

        String result = "1";
        String destDN = null;
        DirContext ctx = null;

        try {

            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, "ldap://" + ldapServer + ":" + ldapPort);
            if (ldapPort == 636) {
                env.put(Context.SECURITY_PROTOCOL, "ssl");
            }
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, ldapConnDN);
            env.put(Context.SECURITY_CREDENTIALS, ldapConnPwd);

            ctx = new InitialDirContext(env);
            ModificationItem[] mods = new ModificationItem[1];
            Attribute newAttribute = new BasicAttribute(ldapAttribute, ldapAttributeValue);
            mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, newAttribute);

            //DN des zu ändernden Objekts abrufen
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            // Specify attributes to retrieve (e.g., mail, sn, cn)
            searchControls.setReturningAttributes(new String[]{"DN"});

            NamingEnumeration<SearchResult> results = ctx.search(ldapBaseDN, ldapFilter, searchControls);
            while (results.hasMore()) {
                SearchResult sr = results.next();
                destDN = sr.getName() + "," + ldapBaseDN;
            }
            if (destDN != null) {
                ctx.modifyAttributes(destDN, mods);
            } else {
                System.err.println("Error: Object " + ldapFilter + " not found.");
                result = "0-" + "Error: Object " + ldapFilter + " not found.";
            }
            ctx.close();
        } catch (NamingException e) {
            System.err.println("Error:" + e.toString());
            result = "0-" + "Error:" + e.toString();
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    System.err.println("Error:" + e.toString());
                    result = "0-" + "Error:" + e.toString();
                }
            }
        }
        return result;
    }

    /**
     * This method get the value in attribute.
     *
     * @param ldapServer IP or Hostname of ldap server
     * @param ldapPort Connection porf of the lpda instance
     * @param ldapConnDN DN of the user connection
     * @param ldapConnPwd Password of the user connection
     * @param ldapBaseDN Base OU where the search the object to modify is
     * excecuted
     * @param ldapFilter Ldap filter to search the object to modify
     * @param ldapAttribute Ldap attribute to set
     * @param ldapAttributeValue Ldap attribute value to set
     * @return Return "1" if process was performed successfully or "0-" with the
     * derail of the error.
     */
    String GetAttributeValue(String ldapServer, int ldapPort, String ldapBaseDN, String ldapConnDN, String ldapConnPwd, String ldapFilter, String ldapAttribute) {

        String result = "0-No value";
        DirContext ctx = null;

        try {

            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, "ldap://" + ldapServer + ":" + ldapPort);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, ldapConnDN);
            env.put(Context.SECURITY_CREDENTIALS, ldapConnPwd);

            ctx = new InitialDirContext(env);

            //DN des zu ändernden Objekts abrufen
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            // Specify attributes to retrieve (e.g., mail, sn, cn)
            searchControls.setReturningAttributes(new String[]{ldapAttribute});

            NamingEnumeration<SearchResult> results = ctx.search(ldapBaseDN, ldapFilter, searchControls);
            while (results.hasMore()) {
                SearchResult sr = results.next();
                Attributes attributes = sr.getAttributes();

                Attribute attr = attributes.get(ldapAttribute);
                if (attr != null) {
                    result = attr.get().toString();
                    System.out.println("Attribute " + ldapAttribute + ": " + result);
                }
            }
            ctx.close();
        } catch (NamingException e) {
            System.err.println("Error:" + e.toString());
            result = "0-" + "Error:" + e.toString();
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    System.err.println("Error:" + e.toString());
                    result = "0-" + "Error:" + e.toString();
                }
            }
        }
        return result;
    }

    /**
     * This method remove the value in attribute.
     *
     * @param ldapServer IP or Hostname of ldap server
     * @param ldapPort Connection porf of the lpda instance
     * @param ldapConnDN DN of the user connection
     * @param ldapConnPwd Password of the user connection
     * @param ldapBaseDN Base OU where the search the object to modify is
     * excecuted
     * @param ldapFilter Ldap filter to search the object to modify
     * @param ldapAttribute Ldap attribute to set
     * @param ldapAttributeValue Ldap attribute value to set
     * @return Return "1" if process was performed successfully or "0-" with the
     * derail of the error.
     */
    String RemoveAttributeValue(String ldapServer, int ldapPort, String ldapBaseDN, String ldapConnDN, String ldapConnPwd, String ldapFilter, String ldapAttribute, String ldapAttributeValue) {

        String result = "1";
        String destDN = null;
        DirContext ctx = null;

        try {

            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, "ldap://" + ldapServer + ":" + ldapPort);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, ldapConnDN);
            env.put(Context.SECURITY_CREDENTIALS, ldapConnPwd);

            ctx = new InitialDirContext(env);
            ModificationItem[] mods = new ModificationItem[1];
            Attribute newAttribute = new BasicAttribute(ldapAttribute, ldapAttributeValue);
            mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, newAttribute);

            //DN des zu ändernden Objekts abrufen
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            // Specify attributes to retrieve (e.g., mail, sn, cn)
            searchControls.setReturningAttributes(new String[]{"DN"});

            NamingEnumeration<SearchResult> results = ctx.search(ldapBaseDN, ldapFilter, searchControls);
            while (results.hasMore()) {
                SearchResult sr = results.next();
                destDN = sr.getName() + "," + ldapBaseDN;
            }
            if (destDN != null) {
                ctx.modifyAttributes(destDN, mods);
            } else {
                System.err.println("Error: Object " + ldapFilter + " not found.");
                result = "0-" + "Error: Object " + ldapFilter + " not found.";
            }
            ctx.close();
        } catch (NamingException e) {
            System.err.println("Error:" + e.toString());
            result = "0-" + "Error:" + e.toString();
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    System.err.println("Error:" + e.toString());
                    result = "0-" + "Error:" + e.toString();
                }
            }
        }
        return result;
    }

    /**
     * This method add, set, or remove attribute values from objects returned
     * from query
     *
     * @param ldapServer IP or Hostname of ldap server
     * @param ldapPort Connection porf of the lpda instance
     * @param ldapConnDN DN of the user connection
     * @param ldapConnPwd Password of the user connection
     * @param ldapBaseDN Base OU where the search the object to modify is
     * excecuted
     * @param ldapFilter Ldap filter to search the object to modify
     * @param operation Ldap operation to performe on attribute add, set, or
     * remove.
     * @param ldapAttribute Ldap attribute to set
     * @param ldapAttributeValue Ldap attribute value to set
     * @return Return "1" if process was performed successfully or "0-" with the
     * derail of the error.
     */
    String UpdateObjectsFromQuery(String ldapServer, int ldapPort, String ldapBaseDN, String ldapConnDN, String ldapConnPwd, String ldapFilter, String operation, String ldapAttribute, String ldapAttributeValue) {

        int attrRmv = 0;
        String destDN;
        String result = "1";

        DirContext ctx = null;

        try {

            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, "ldap://" + ldapServer + ":" + ldapPort);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, ldapConnDN);
            env.put(Context.SECURITY_CREDENTIALS, ldapConnPwd);

            ctx = new InitialDirContext(env);

            //DN des zu ändernden Objekts abrufen
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            // Specify attributes to retrieve (e.g., mail, sn, cn)
            searchControls.setReturningAttributes(new String[]{ldapAttribute});

            NamingEnumeration<SearchResult> results = ctx.search(ldapBaseDN, ldapFilter, searchControls);
            while (results.hasMore()) {
                SearchResult sr = results.next();
                destDN = sr.getName() + "," + ldapBaseDN;
                //Attributes attributes = sr.getAttributes();
                //Attribute attr = attributes.get(ldapAttribute);
                //if (attr != null) {
                ModificationItem[] mods = new ModificationItem[1];
                Attribute oprAttribute = new BasicAttribute(ldapAttribute, ldapAttributeValue);
                switch (operation) {
                    case "add":
                        mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, oprAttribute);
                        ctx.modifyAttributes(destDN, mods);
                        break;
                    case "set":
                        mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, oprAttribute);
                        if (ldapAttribute.equalsIgnoreCase("GroupMembership")) {
                            ModificationItem[] modGroup = new ModificationItem[2];
                            Attribute grpMember = new BasicAttribute("member", destDN);
                            Attribute grpEquivalentToMe = new BasicAttribute("equivalentToMe", destDN);
                            modGroup[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, grpMember);
                            modGroup[1] = new ModificationItem(DirContext.ADD_ATTRIBUTE, grpEquivalentToMe);
                            try {
                                ctx.modifyAttributes(ldapAttributeValue, modGroup);
                                System.out.println("Object " + ldapAttribute + " was updated. Value \"" + destDN + "\" was added to attribute member.");
                                System.out.println("Object " + ldapAttribute + " was updated. Value \"" + destDN + "\" was added to attribute equivalentToMe.");
                            } catch (NamingException ex) {
                                System.err.println("Object " + ldapAttribute + " was not updated. Value \"" + destDN + "\" was not to from attribute member.");
                                System.err.println("Object " + ldapAttribute + " was not updated. Value \"" + destDN + "\" was not to from attribute equivalentToMe.");
                            }

                        }
                        break;
                    case "remove":
                        Attributes attributes = sr.getAttributes();
                        Attribute attr = attributes.get(ldapAttribute);
                        if (attr != null) {
                            for (int i = 0; i < attr.size(); i++) {
                                if (attr.get(i).toString().equals(ldapAttributeValue)) {
                                    mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, oprAttribute);
                                    if (ldapAttribute.equalsIgnoreCase("GroupMembership")) {
                                        ModificationItem[] modGroup = new ModificationItem[2];
                                        Attribute grpMember = new BasicAttribute("member", destDN);
                                        Attribute grpEquivalentToMe = new BasicAttribute("equivalentToMe", destDN);
                                        modGroup[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, grpMember);
                                        modGroup[1] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, grpEquivalentToMe);
                                        try {
                                            ctx.modifyAttributes(ldapAttributeValue, modGroup);
                                            System.out.println("Object " + ldapAttribute + " was updated. Value \"" + destDN + "\" was removed from attribute member.");
                                            System.out.println("Object " + ldapAttribute + " was updated. Value \"" + destDN + "\" was removed from attribute equivalentToMe.");
                                        } catch (NamingException ex) {
                                            System.err.println("Object " + ldapAttribute + " was not updated. Value \"" + destDN + "\" was not removed from attribute member.");
                                            System.err.println("Object " + ldapAttribute + " was not updated. Value \"" + destDN + "\" was not removed from attribute equivalentToMe.");
                                        }

                                    }
                                }
                            }
                        } else {
                            System.err.println("Object " + ldapAttribute + " was not updated. Object doesn't have attrbiute " + ldapAttribute);
                            attrRmv = 1;
                        }
                        break;
                    default:
                        System.out.println("Operation  " + operation + " is not valid.");
                        return "Operation  " + operation + " is not valid.";
                }
                if (mods[0] != null) {
                    ctx.modifyAttributes(destDN, mods);
                } else {
                    operation = "";
                }

                switch (operation) {
                    case "add":
                        System.out.println("Object " + destDN + " was updated. Value \"" + ldapAttributeValue + "\" was added to attribute " + ldapAttribute + ".");
                        break;
                    case "set":
                        System.out.println("Object " + destDN + " was updated. Value \"" + ldapAttributeValue + "\" was set to attribute " + ldapAttribute + ".");
                        break;
                    case "remove":
                        if (attrRmv == 0) {
                            System.out.println("Object " + destDN + " was updated. Value \"" + ldapAttributeValue + "\" was removed from attribute " + ldapAttribute + ".");
                        }
                        break;
                }
            }
            ctx.close();
            result = "1";
        } catch (NamingException e) {
            System.err.println("Error:" + e.toString());
            result = "0-" + "Error:" + e.toString();
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    System.err.println("Error:" + e.toString());
                    result = "0-" + "Error:" + e.toString();
                }
            }
        }
        return result;
    }

    /**
     * This method get the DN of objects from query
     *
     * @param ldapServer IP or Hostname of ldap server
     * @param ldapPort Connection porf of the lpda instance
     * @param ldapConnDN DN of the user connection
     * @param ldapConnPwd Password of the user connection
     * @param ldapBaseDN Base OU where the search the object to modify is
     * excecuted
     * @param ldapFilter Ldap filter to search the object to modify
     * @return Return "1" if process was performed successfully or "0-" with the
     * derail of the error.
     */
    ArrayList GetObjectsDNFromQuery(String ldapServer, int ldapPort, String ldapBaseDN, String ldapConnDN, String ldapConnPwd, String ldapFilter) {

        ArrayList<String> objects = new ArrayList<>();
        DirContext ctx = null;

        try {

            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, "ldap://" + ldapServer + ":" + ldapPort);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, ldapConnDN);
            env.put(Context.SECURITY_CREDENTIALS, ldapConnPwd);

            ctx = new InitialDirContext(env);

            //DN des zu ändernden Objekts abrufen
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            // Specify attributes to retrieve (e.g., mail, sn, cn)
            searchControls.setReturningAttributes(new String[]{"DN"});

            NamingEnumeration<SearchResult> results = ctx.search(ldapBaseDN, ldapFilter, searchControls);
            while (results.hasMore()) {
                SearchResult sr = results.next();
                objects.add(sr.getName() + "," + ldapBaseDN);
            }
            ctx.close();
        } catch (NamingException e) {
            System.err.println("Error:" + e.toString());
            objects.add("0-" + "Error:" + e.toString());
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    System.err.println("Error:" + e.toString());
                    objects.add("0-" + "Error:" + e.toString());
                }
            }
        }
        return objects;
    }

    /**
     * This method get the count of objects returned from query
     *
     * @param ldapServer IP or Hostname of ldap server
     * @param ldapPort Connection porf of the lpda instance
     * @param ldapConnDN DN of the user connection
     * @param ldapConnPwd Password of the user connection
     * @param ldapBaseDN Base OU where the search the object to modify is
     * excecuted
     * @param ldapFilter Ldap filter to search the object to modify
     * @return Return the number of objects returned. if process was performed
     * successfully or "0-" with the derail of the error.
     */
    String GetObjectsCountFromQuery(String ldapServer, int ldapPort, String ldapBaseDN, String ldapConnDN, String ldapConnPwd, String ldapFilter) {

        int count = 0;
        String result;
        DirContext ctx = null;

        try {

            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, "ldap://" + ldapServer + ":" + ldapPort);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, ldapConnDN);
            env.put(Context.SECURITY_CREDENTIALS, ldapConnPwd);

            ctx = new InitialDirContext(env);

            //DN des zu ändernden Objekts abrufen
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            // Specify attributes to retrieve (e.g., mail, sn, cn)
            searchControls.setReturningAttributes(new String[]{"DN"});

            NamingEnumeration<SearchResult> results = ctx.search(ldapBaseDN, ldapFilter, searchControls);
            while (results.hasMore()) {
                count++;
                results.next();
            }
            ctx.close();
            result = Integer.toString(count);
        } catch (NamingException e) {
            System.err.println("Error:" + e.toString());
            result = "0-" + e.toString();
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    System.err.println("Error:" + e.toString());
                    result = "0-" + e.toString();
                }
            }
        }
        return result;
    }

    /**
     * This method get the value in attribute.
     *
     * @param ldapServer IP or Hostname of ldap server
     * @param ldapPort Connection porf of the lpda instance
     * @param ldapConnDN DN of the user connection
     * @param ldapConnPwd Password of the user connection
     * @param objectDN DN of the object to modify
     * @param ldapAttributes list the attributes to search
     * @return Return "1" if process was performed successfully or "0-" with the
     * derail of the error.
     */
    String GetAttributesValueFromObject(String ldapServer, int ldapPort, String ldapConnDN, String ldapConnPwd, String objectDN, String[] ldapAttributes) {

        String result = "0-No value";
        DirContext ctx = null;

        try {

            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, "ldap://" + ldapServer + ":" + ldapPort);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, ldapConnDN);
            env.put(Context.SECURITY_CREDENTIALS, ldapConnPwd);

            ctx = new InitialDirContext(env);

            Attributes attributes = ctx.getAttributes(objectDN, ldapAttributes);
            if (attributes != null) {
                result = objectDN;
                for (String strAttribute : ldapAttributes) {
                    Attribute attribute = attributes.get(strAttribute);
                    if (attribute != null) {
                        result = result + "|" + attribute.toString();
                    } else {
                        result = result + "|" + strAttribute + ":NULL";
                    }
                }
            }

            ctx.close();
        } catch (NamingException e) {
            System.err.println("Error:" + e.toString());
            result = "0-" + "Error:" + e.toString();
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    System.err.println("Error:" + e.toString());
                    result = "0-" + "Error:" + e.toString();
                }
            }
        }
        return result;
    }

    /**
     * This method set a value in attribute
     *
     * @param ldapServer IP or Hostname of ldap server
     * @param ldapPort Connection porf of the lpda instance
     * @param ldapConnDN DN of the user connection
     * @param ldapConnPwd Password of the user connection
     * @param objectDN DN object to modify
     * @param ldapAttribute Ldap attribute to set
     * @param ldapAttributeValue Ldap attribute value to set
     * @return Return "1" if process was performed successfully or "0-" with the
     * derail of the error.
     */
    String SetAttributeValueByDN(String ldapServer, int ldapPort, String ldapConnDN, String ldapConnPwd, String objectDN, String ldapAttribute, String ldapAttributeValue) {

        String result = "1";
        DirContext ctx = null;

        try {
            objectDN = objectDN.replace("\uFEFF", "").trim();
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, "ldap://" + ldapServer + ":" + ldapPort);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, ldapConnDN);
            env.put(Context.SECURITY_CREDENTIALS, ldapConnPwd);

            ctx = new InitialDirContext(env);
            ModificationItem[] mods = new ModificationItem[1];
            Attribute newAttribute = new BasicAttribute(ldapAttribute, ldapAttributeValue);
            mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, newAttribute);
                
            ctx.modifyAttributes(objectDN, mods);

            ctx.close();
        } catch (NamingException e) {
            System.err.println("Error:" + e.toString());
            result = "0-" + "Error:" + e.toString();
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    System.err.println("Error:" + e.toString());
                    result = "0-" + "Error:" + e.toString();
                }
            }
        }
        return result;
    }

    /**
     * This method add a value in attribute
     *
     * @param ldapServer IP or Hostname of ldap server
     * @param ldapPort Connection porf of the lpda instance
     * @param ldapConnDN DN of the user connection
     * @param ldapConnPwd Password of the user connection
     * @param objectDN DN object to modify
     * @param ldapAttribute Ldap attribute to set
     * @param ldapAttributeValue Ldap attribute value to set
     * @return Return "1" if process was performed successfully or "0-" with the
     * derail of the error.
     */
    String AddAttributeValueByDN(String ldapServer, int ldapPort, String ldapConnDN, String ldapConnPwd, String objectDN, String ldapAttribute, String ldapAttributeValue) {

        String result = "1";
        DirContext ctx = null;

        try {

            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, "ldap://" + ldapServer + ":" + ldapPort);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, ldapConnDN);
            env.put(Context.SECURITY_CREDENTIALS, ldapConnPwd);

            ctx = new InitialDirContext(env);
            ModificationItem[] mods = new ModificationItem[1];
            Attribute newAttribute = new BasicAttribute(ldapAttribute, ldapAttributeValue);
            mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, newAttribute);

            ctx.modifyAttributes(objectDN, mods);

            ctx.close();
        } catch (NamingException e) {
            System.err.println("Error:" + e.toString());
            result = "0-" + "Error:" + e.toString();
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    System.err.println("Error:" + e.toString());
                    result = "0-" + "Error:" + e.toString();
                }
            }
        }
        return result;
    }

    /**
     * This method add a value in attribute
     *
     * @param ldapServer IP or Hostname of ldap server
     * @param ldapPort Connection porf of the lpda instance
     * @param ldapConnDN DN of the user connection
     * @param ldapConnPwd Password of the user connection
     * @param objectDN DN object to modify
     * @param ldapAttribute Ldap attribute to set
     * @param ldapAttributeValue Ldap attribute value to set
     * @return Return "1" if process was performed successfully or "0-" with the
     * derail of the error.
     */
    String ClearAttributeValueByDN(String ldapServer, int ldapPort, String ldapConnDN, String ldapConnPwd, String objectDN, String ldapAttribute) {

        String result = "1";
        DirContext ctx = null;

        try {

            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, "ldap://" + ldapServer + ":" + ldapPort);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, ldapConnDN);
            env.put(Context.SECURITY_CREDENTIALS, ldapConnPwd);

            ctx = new InitialDirContext(env);
            ModificationItem[] mods = new ModificationItem[1];
            Attribute newAttribute = new BasicAttribute(ldapAttribute);
            mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, newAttribute);

            ctx.modifyAttributes(objectDN, mods);

            ctx.close();
        } catch (NamingException e) {
            System.err.println("Error:" + e.toString());
            result = "0-" + "Error:" + e.toString();
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    System.err.println("Error:" + e.toString());
                    result = "0-" + "Error:" + e.toString();
                }
            }
        }
        return result;
    }

    /**
     * This method get the associatino value for specific driver
     *
     * @param ldapServer IP or Hostname of ldap server
     * @param ldapPort Connection porf of the lpda instance
     * @param ldapConnDN DN of the user connection
     * @param ldapConnPwd Password of the user connection
     * @param objectDN DN object to modify
     * @param driver Ldap attribute to set
     * @return Return "1-The asscociation" or "1" if process was performed
     * successfully but user doesn't have assocciaiton or "0-" with the derail
     * of the error.
     */
    String GetAssociationDiverValueByDN(String ldapServer, int ldapPort, String ldapConnDN, String ldapConnPwd, String objectDN, String driver) {

        String result = "1";
        DirContext ctx = null;

        try {

            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, "ldap://" + ldapServer + ":" + ldapPort);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, ldapConnDN);
            env.put(Context.SECURITY_CREDENTIALS, ldapConnPwd);

            ctx = new InitialDirContext(env);
            objectDN = objectDN.replace("\uFEFF", "").trim();
            LdapName dn = new LdapName(objectDN);
            Attributes attrs = ctx.getAttributes(dn);
            Attribute dirXMLAssociations = attrs.get("DirXML-Associations");

            if (dirXMLAssociations != null) {
                NamingEnumeration<?> allValues = dirXMLAssociations.getAll();
                while (allValues.hasMore()) {
                    Object value = allValues.next();
                    String stringValue = String.valueOf(value);
                    if (stringValue.contains("cn=GPOS,cn=DriverSet40,ou=idm,ou=noc,o=idv")) {                        
                        result = "1-" + stringValue;
                    }                    
                }
                allValues.close();
            }
            ctx.close();
        } catch (NamingException e) {
            System.err.println("Error:" + e.toString());
            result = "0-" + "Error:" + e.toString();
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    System.err.println("Error:" + e.toString());
                    result = "0-" + "Error:" + e.toString();
                }
            }
        }
        return result;
    }
}
