package org.apereo.cas.pm;

import org.apereo.cas.audit.spi.config.CasCoreAuditConfiguration;
import org.apereo.cas.authentication.credential.UsernamePasswordCredential;
import org.apereo.cas.config.CasCoreUtilConfiguration;
import org.apereo.cas.config.LdapPasswordManagementConfiguration;
import org.apereo.cas.pm.config.PasswordManagementConfiguration;
import org.apereo.cas.util.junit.EnabledIfContinuousIntegration;
import org.apereo.cas.util.junit.EnabledIfPortOpen;

import lombok.SneakyThrows;
import lombok.val;
import org.apereo.inspektr.common.web.ClientInfo;
import org.apereo.inspektr.common.web.ClientInfoHolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is {@link ADPasswordManagementServiceTests}.
 * Change password handling is different for Active Directory and this test exercises that AD specific code branch.
 *
 * @author Hal Deadman
 * @since 6.1.0
 */
@Tag("Ldap")
@SpringBootTest(classes = {
    RefreshAutoConfiguration.class,
    LdapPasswordManagementConfiguration.class,
    PasswordManagementConfiguration.class,
    CasCoreAuditConfiguration.class,
    CasCoreUtilConfiguration.class
    }, properties = {
    "cas.authn.pm.reset.sms.attributeName=telephoneNumber",
    "cas.authn.pm.ldap.ldapUrl=ldap://localhost:10636",
    "cas.authn.pm.ldap.bindDn=CN=admin,CN=Users,DC=cas,DC=example,DC=org",
    "cas.authn.pm.ldap.bindCredential=P@ssw0rd",
    "cas.authn.pm.ldap.baseDn=CN=Users,DC=cas,DC=example,DC=org",
    "cas.authn.pm.ldap.searchFilter=cn={user}",
    "cas.authn.pm.ldap.useSsl=true",
    "cas.authn.pm.ldap.type=AD",
    "cas.authn.pm.ldap.securityQuestionsAttributes.department=company",
    "cas.authn.pm.ldap.securityQuestionsAttributes.description=physicalDeliveryOfficeName",
    "cas.authn.pm.ldap.providerClass=org.ldaptive.provider.unboundid.UnboundIDProvider",
    "cas.authn.pm.ldap.trustStore=file:/tmp/adcacerts.jks",
    "cas.authn.pm.ldap.trustStoreType=JKS",
    "cas.authn.pm.ldap.trustStorePassword=changeit",
    "cas.authn.pm.ldap.minPoolSize=0",
    "cas.authn.pm.ldap.hostnameVerifier=DEFAULT"
})
@DirtiesContext
@EnabledIfContinuousIntegration
@EnabledIfPortOpen(port = 10390)
public class ADPasswordManagementServiceTests {

    @Autowired
    @Qualifier("passwordChangeService")
    private PasswordManagementService passwordChangeService;

    @BeforeAll
    @SneakyThrows
    public static void bootstrap() {
        ClientInfoHolder.setClientInfo(new ClientInfo(new MockHttpServletRequest()));
    }

    @Test
    public void verifyPasswordChange() {
        val credential = new UsernamePasswordCredential("changepassword", "P@ssw0rd");
        val bean = new PasswordChangeRequest();
        bean.setConfirmedPassword("P@ssw0rdMellon");
        bean.setPassword("P@ssw0rdMellon");
        bean.setUsername(credential.getUsername());
        assertTrue(passwordChangeService.change(credential, bean));
    }


    @Test
    public void verifyPasswordReset() {
        val credential = new UsernamePasswordCredential("changepassword", "P@ssw0rd");
        val bean = new PasswordChangeRequest();
        bean.setConfirmedPassword("P@ssw0rdMellon");
        bean.setPassword("P@ssw0rdMellon");
        bean.setUsername(credential.getUsername());
        assertTrue(passwordChangeService.change(credential, bean));
    }

    @Test
    public void verifyFindEmail() {
        val email = passwordChangeService.findEmail("changepassword");
        assertEquals("changepassword@example.org", email);
    }

    @Test
    public void verifyFindPhone() {
        val ph = passwordChangeService.findPhone("changepassword");
        assertEquals("1234567890", ph);
    }

    @Test
    public void verifyFindSecurityQuestions() {
        val questions = passwordChangeService.getSecurityQuestions("changepassword");
        assertEquals(2, questions.size());
        assertTrue(questions.containsKey("DepartmentQuestion"));
        assertEquals("CompanyAnswer", questions.get("DepartmentQuestion"));
        assertTrue(questions.containsKey("DescriptionQuestion"));
        assertEquals("PhysicalDeliveryOfficeAnswer", questions.get("DescriptionQuestion"));
    }
}
