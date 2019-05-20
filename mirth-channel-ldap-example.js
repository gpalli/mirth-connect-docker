importPackage(Packages.com.vinculomedico.sinergis.util);

try {
	var ldaputil = new LdapUtil("ou=Users,dc=vinculomedico,dc=com", "cn=admin,dc=vinculomedico,dc=com","*****","localhost","389");
  // ver el contenido de un usuario
	var attrs = ldaputil.getUserAttrs("teste", ["uid", "givenname", "sn", "createTimestamp", "mail", "entryUUID"]);
	logger.info(attrs);
  // modificar password
	ldaputil.generarPassword("teste","*****");
	logger.info("Ya lo cambie!");

} catch(e) {
  logger.error("ERROR NO lo cambie "+e)
}
