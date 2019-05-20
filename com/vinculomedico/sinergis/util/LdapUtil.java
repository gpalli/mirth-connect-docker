package com.vinculomedico.sinergis.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

/**
 * Helper para acceder a las interfaces de LDAP mediante JNDI
 *
 * @version $Revision: 1.6.4.1 $ $Date: 2006/10/09 15:43:24 $
 *
 * @author $Author: aperez $
 */
public class LdapUtil {

    /** Contexto raiz de LDAP */
    private DirContext rootCtx = null;
    /** Default Root Context */
    private static String rootCtxName = null;
    /** Directory Server host name */
    private static String ldapHostName = "localhost";
    /** Directory instance  port */
    private static String ldapPort = "386";
    /** Directory instance  SSL port */
    private static String ldapSSLPort = "636";
    /** Username Root */
    private static String rootUserName = "";
    /** Password Root */
    private static String rootPassword = "";
    /** Indica si uso SSL */
    private static boolean ldapSSL = false;
    /** Default security autentication metod */
    private static String ldapAuth = "simple";
    /** Especifica como busco al usuario CN (default) o UID */
    private static String ldapUserContext = "cn";

    /** Default constructor */
    public LdapUtil(String rootName,
                    String userName,
                    String password,
                    String hostName,
                    String port) {

        rootCtxName = rootName;
        rootUserName = userName;
        rootPassword = password;
        ldapHostName = hostName;
        ldapPort = port;
    }


    private static String encryptPassword(String password)
    	{
    		MessageDigest digest = null;
    		String algorithm = "SSHA";
    		byte[] data;
    		try {
    			data = password.getBytes("UTF-8");
    			if (algorithm.equalsIgnoreCase("SSHA") || algorithm.equalsIgnoreCase("SHA")) {
    				digest = MessageDigest.getInstance("SHA-1");
    			} else if (algorithm.equalsIgnoreCase("SMD5") || algorithm.equalsIgnoreCase("MD5")) {
    				digest = MessageDigest.getInstance("MD5");
    			}
    		} catch (NoSuchAlgorithmException e) {
    			throw new RuntimeException("Could not find MessageDigest algorithm (" + algorithm + ") implementation");
    		} catch (UnsupportedEncodingException e) {
    			throw new RuntimeException("Could not convert password to byte[]");
    		}
    		if (digest == null) {
    			throw new RuntimeException("Unsupported hash algorithm: " + algorithm);
    		}

    		byte[] salt = {};

    		if (algorithm.equalsIgnoreCase("SSHA") || algorithm.equalsIgnoreCase("SMD5")) {
    			Random rand = new Random();
    			rand.setSeed(System.currentTimeMillis());
    			// A RSA whitepaper
    			// <http://www.rsasecurity.com/solutions/developers/whitepapers/Article3-PBE.pdf>
    			// suggested the salt length be the same as the output of the
    			// hash function being used. The adapter uses the length of the
    			// input,
    			// hoping that it is close enough an approximation.
    			salt = new byte[password.length()];
    			rand.nextBytes(salt);
    		}

    		digest.reset();
    		digest.update(data);
    		digest.update(salt);
    		byte[] hash = digest.digest();

    		byte[] hashPlusSalt = new byte[hash.length + salt.length];
    		System.arraycopy(hash, 0, hashPlusSalt, 0, hash.length);
    		System.arraycopy(salt, 0, hashPlusSalt, hash.length, salt.length);

    		StringBuilder result = new StringBuilder(algorithm.length() + hashPlusSalt.length);
    		result.append('{');
    		result.append(algorithm);
    		result.append('}');

    		result.append(Base64.encode(hashPlusSalt));

    		return result.toString();
    	}

    public void generarPassword(String username, String password) throws Exception {
        //String[] ATTRS_WRITE = {"uid", "givenname", "sn", "mail"};
        //Map attrs = getUserAttrs(username, ATTRS_WRITE);
        Map attrs = new HashMap();
        attrs.put("userpassword", encryptPassword(password));
        rootCtx = getDirectoryContext(rootUserName, rootPassword);
        modifyEntry(ldapUserContext + "=" + username + "," + rootCtxName, attrs);
        rootCtx.close();
    }

    /**
     * Modifica un subcontexto al contexto raiz
     * @param dn ubicacion X.500
     * @param map attributos
     * @throws NamingException
     */
    private void modifyEntry(String dn, Map map) throws NamingException {
            // Get the attribute,newvalue mapping
            Iterator attrsIter = map.entrySet().iterator();
            // Construct the attributes list
            Attributes attrs = new BasicAttributes(true); // case-ignore
            while (attrsIter.hasNext()) {
                Map.Entry attr = (Map.Entry) attrsIter.next();
                attrs.put(new BasicAttribute((String) attr.getKey(), attr.getValue()));
            }
            // Replace the existing attribute values with the specified new values
            rootCtx.modifyAttributes(dn, DirContext.REPLACE_ATTRIBUTE, attrs);
    }


    /**
     * Busca en el directorio bajo un contexto especifico y devuelve
     * el resultado de la busqueda. Solo devuelve un solo nivel.
     * @param ctxname el contexto
     * @param filter los filtros
     * @return la lista de resultados
     * @throws NamingException
     */

    public Map getUserAttrs(String username, String[] attrs) {
    Map rta = new HashMap();
		DirContext ctx = null;
		NamingEnumeration results = null;

		try {
			String ldapurl = "ldap";
			if (ldapSSL) ldapurl = "ldaps";
		    ldapurl = ldapurl + "://" + ldapHostName + ":" + ldapPort;
	        Hashtable env = new Hashtable();
	        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
	        env.put(Context.PROVIDER_URL, ldapurl);
	        env.put(Context.SECURITY_AUTHENTICATION, ldapAuth);
	        env.put(Context.SECURITY_PRINCIPAL, rootUserName);
	        env.put(Context.SECURITY_CREDENTIALS, rootPassword);

	        // Bind and initialize the Directory context
			ctx = new InitialDirContext(env);
			SearchControls controls = new SearchControls();
			controls.setReturningAttributes(new String[] {"*","+"});
			controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			results = ctx.search(rootCtxName, "(" + ldapUserContext + "=" + username + ")", controls);
			while (results.hasMore()) {
				SearchResult searchResult = (SearchResult) results.next();
				Attributes attributes = searchResult.getAttributes();

				for (int x = 0; x < attrs.length; x++) {
 				     Attribute attr = attributes.get(attrs[x]);
			         rta.put(attrs[x], attr.get());
				}
			}
		} catch (NamingException e) {
			throw new RuntimeException(e);
		} finally {
			if (results != null) {
				try {
					results.close();
				} catch (Exception e) {
				}
			}
			if (ctx != null) {
				try {
					ctx.close();
				} catch (Exception e) {
				}
			}
		}
		ctx = null;
        return rta;
    }
    /**
     * Obtiene una referencia al contexto de LDAP autenticado por el usuario y password
     * @param userName
     * @param password
     * @return un DirContext
     * @throws AuthenticationException
     * @throws NamingException
     */
    private DirContext getDirectoryContext(String userName, String password)
        throws AuthenticationException, NamingException {
        Hashtable env = new Hashtable();
        //Build the LDAP url
        String port = ldapPort;
        if (ldapSSL) {
            port = ldapSSLPort;
            env.put(Context.SECURITY_PROTOCOL, "ssl");
        }
        String ldapurl = "ldap";
		if (ldapSSL) ldapurl = "ldaps";
	    ldapurl = ldapurl + "://" + ldapHostName + ":" + port;
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapurl);
        env.put(Context.SECURITY_AUTHENTICATION, ldapAuth);
        env.put(Context.SECURITY_PRINCIPAL, userName);
        env.put(Context.SECURITY_CREDENTIALS, password);
        // Bind and initialize the Directory context
        DirContext dirCtx = new InitialDirContext(env);
        return dirCtx;
    }
    /* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    protected void finalize() throws Throwable {
        if (rootCtx != null) {
            rootCtx.close();
            rootCtx = null;
        }
        super.finalize();
    }
}
