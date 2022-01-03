/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.oidc;

import com.evolveum.midpoint.authentication.impl.module.configurer.OidcModuleWebSecurityConfigurer;

import com.evolveum.midpoint.authentication.impl.saml.MidpointSaml2LoginConfigurer;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.*;
import org.springframework.util.Assert;

public final class OidcLoginConfigurer<B extends HttpSecurityBuilder<B>>
        extends AbstractAuthenticationFilterConfigurer<B, OidcLoginConfigurer<B>, OidcLoginAuthenticationFilter> {

    private ClientRegistrationRepository clientRegistrations;
    private String authorizationRequestBaseUri;
    private String loginProcessingUrl = OAuth2LoginAuthenticationFilter.DEFAULT_FILTER_PROCESSES_URI;
    private AuthenticationManager authenticationManager;

    public OidcLoginConfigurer<B> authenticationManager(AuthenticationManager authenticationManager) {
        Assert.notNull(authenticationManager, "authenticationManager cannot be null");
        this.authenticationManager = authenticationManager;
        return this;
    }

    public OidcLoginConfigurer<B> clientRegistrationRepository(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrations = clientRegistrationRepository;
        return this;
    }

    public OidcLoginConfigurer<B> authorizationRequestBaseUri(String authorizationRequestBaseUri) {
        this.authorizationRequestBaseUri = authorizationRequestBaseUri;
        return this;
    }

    @Override
    public OidcLoginConfigurer<B> loginProcessingUrl(String loginProcessingUrl) {
        Assert.hasText(loginProcessingUrl, "loginProcessingUrl cannot be empty");
        this.loginProcessingUrl = loginProcessingUrl;
        return this;
    }

    @Override
    public void init(B http) throws Exception {
        OidcLoginAuthenticationFilter authenticationFilter = new OidcLoginAuthenticationFilter(
                clientRegistrations, this.loginProcessingUrl);
        if (this.authenticationManager != null) {
            authenticationFilter.setAuthenticationManager(this.authenticationManager);
        }
        this.setAuthenticationFilter(authenticationFilter);

        super.loginProcessingUrl(this.loginProcessingUrl);
        super.loginPage(OidcModuleWebSecurityConfigurer.OIDC_LOGIN_PATH);
        super.init(http);
    }

    @Override
    public void configure(B http) throws Exception {
        OAuth2AuthorizationRequestRedirectFilter authorizationRequestFilter = new OAuth2AuthorizationRequestRedirectFilter(
                clientRegistrations, this.authorizationRequestBaseUri);
//        if (this.authorizationEndpointConfig.authorizationRequestRepository != null) {
//            authorizationRequestFilter
//                    .setAuthorizationRequestRepository(this.authorizationEndpointConfig.authorizationRequestRepository);
//        }
        RequestCache requestCache = http.getSharedObject(RequestCache.class);
        if (requestCache != null) {
            authorizationRequestFilter.setRequestCache(requestCache);
        }
        http.addFilter(this.postProcess(authorizationRequestFilter));
//        OidcLoginAuthenticationFilter authenticationFilter = this.getAuthenticationFilter();
//        if (this.authorizationEndpointConfig.authorizationRequestRepository != null) {
//            authenticationFilter
//                    .setAuthorizationRequestRepository(this.authorizationEndpointConfig.authorizationRequestRepository);
//        }
        super.configure(http);
    }

    @Override
    protected RequestMatcher createLoginProcessingUrlMatcher(String loginProcessingUrl) {
        return new AntPathRequestMatcher(loginProcessingUrl);
    }

//    @SuppressWarnings("unchecked")
//    private JwtDecoderFactory<ClientRegistration> getJwtDecoderFactoryBean() {
//        ResolvableType type = ResolvableType.forClassWithGenerics(JwtDecoderFactory.class, ClientRegistration.class);
//        String[] names = this.getBuilder().getSharedObject(ApplicationContext.class).getBeanNamesForType(type);
//        if (names.length > 1) {
//            throw new NoUniqueBeanDefinitionException(type, names);
//        }
//        if (names.length == 1) {
//            return (JwtDecoderFactory<ClientRegistration>) this.getBuilder().getSharedObject(ApplicationContext.class)
//                    .getBean(names[0]);
//        }
//        return null;
//    }
}
