package org.keycloak.testsuite.broker;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.broker.oidc.mappers.ExternalKeycloakRoleToRoleMapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.SuiteContext;

import java.util.List;
import java.util.Map;

import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

public class KcOidcBrokerPromptParameterTest extends AbstractBrokerTest {

    private static final String PROMPT_CONSENT = "consent";
    private static final String PROMPT_LOGIN = "login";

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration2();
    }

    @Override
    protected Iterable<IdentityProviderMapperRepresentation> createIdentityProviderMappers() {
        IdentityProviderMapperRepresentation attrMapper1 = new IdentityProviderMapperRepresentation();
        attrMapper1.setName("manager-role-mapper");
        attrMapper1.setIdentityProviderMapper(ExternalKeycloakRoleToRoleMapper.PROVIDER_ID);
        attrMapper1.setConfig(ImmutableMap.<String,String>builder()
                .put("external.role", "manager")
                .put("role", "manager")
                .build());

        IdentityProviderMapperRepresentation attrMapper2 = new IdentityProviderMapperRepresentation();
        attrMapper2.setName("user-role-mapper");
        attrMapper2.setIdentityProviderMapper(ExternalKeycloakRoleToRoleMapper.PROVIDER_ID);
        attrMapper2.setConfig(ImmutableMap.<String,String>builder()
                .put("external.role", "user")
                .put("role", "user")
                .build());

        return Lists.newArrayList(attrMapper1, attrMapper2);
    }

    @Override
    protected void loginUser() {
        driver.navigate().to(getAccountUrl(bc.consumerRealmName()));

        driver.navigate().to(driver.getCurrentUrl() + "&" + OIDCLoginProtocol.PROMPT_PARAM + "=" + PROMPT_CONSENT);

        log.debug("Clicking social " + bc.getIDPAlias());
        accountLoginPage.clickSocial(bc.getIDPAlias());

        waitForPage(driver, "log in to");

        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));

        Assert.assertFalse(OIDCLoginProtocol.PROMPT_PARAM + "=" + PROMPT_LOGIN + " should not be part of the url",
                driver.getCurrentUrl().contains(OIDCLoginProtocol.PROMPT_PARAM + "=" + PROMPT_LOGIN));

        Assert.assertTrue(OIDCLoginProtocol.PROMPT_PARAM + "=" + PROMPT_CONSENT + " should be part of the url",
                driver.getCurrentUrl().contains(OIDCLoginProtocol.PROMPT_PARAM + "=" + PROMPT_CONSENT));

        log.debug("Logging in");
        accountLoginPage.login(bc.getUserLogin(), bc.getUserPassword());

        waitForPage(driver, "update account information");

        updateAccountInformationPage.assertCurrent();
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));


        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");

        UsersResource consumerUsers = adminClient.realm(bc.consumerRealmName()).users();

        int userCount = consumerUsers.count();
        Assert.assertTrue("There must be at least one user", userCount > 0);

        List<UserRepresentation> users = consumerUsers.search("", 0, userCount);

        boolean isUserFound = false;
        for (UserRepresentation user : users) {
            if (user.getUsername().equals(bc.getUserLogin()) && user.getEmail().equals(bc.getUserEmail())) {
                isUserFound = true;
                break;
            }
        }

        Assert.assertTrue("There must be user " + bc.getUserLogin() + " in realm " + bc.consumerRealmName(),
                isUserFound);
    }

    private class KcOidcBrokerConfiguration2 extends KcOidcBrokerConfiguration {
        protected void applyDefaultConfiguration(final SuiteContext suiteContext, final Map<String, String> config) {
            super.applyDefaultConfiguration(suiteContext, config);
            config.remove("prompt");
        }
    }
}
