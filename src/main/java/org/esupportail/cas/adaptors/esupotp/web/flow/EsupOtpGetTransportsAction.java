package org.esupportail.cas.adaptors.esupotp.web.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apereo.cas.web.flow.actions.AbstractMultifactorAuthenticationAction;
import org.apereo.cas.web.support.WebUtils;
import org.esupportail.cas.adaptors.esupotp.EsupOtpCredential;
import org.esupportail.cas.adaptors.esupotp.EsupOtpMethod;
import org.esupportail.cas.adaptors.esupotp.EsupOtpMultifactorAuthenticationProvider;
import org.esupportail.cas.adaptors.esupotp.EsupOtpService;
import org.esupportail.cas.adaptors.esupotp.EsupOtpUser;
import org.esupportail.cas.config.EsupOtpConfigurationProperties;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.webflow.action.EventFactorySupport;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.execution.RequestContextHolder;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * This is {@link EsupOtpGetTransportsAction}.
 *
 * @author Alex Bouskine
 * @since 5.0.0
 */
@Slf4j
@AllArgsConstructor
public class EsupOtpGetTransportsAction extends AbstractMultifactorAuthenticationAction<EsupOtpMultifactorAuthenticationProvider> {

    private EsupOtpConfigurationProperties esupOtpConfigurationProperties;

    EsupOtpService esupOtpService;
    
    @Override
    protected Event doExecute(final RequestContext requestContext) throws Exception {
        final RequestContext context = RequestContextHolder.getRequestContext();
        final String uid = WebUtils.getAuthentication(context).getPrincipal().getId();
        String userHash = esupOtpService.getUserHash(uid);
        String waitingForMethodName = "push";
        
        WebUtils.putCredential(requestContext, new EsupOtpCredential());

        requestContext.getFlowScope().put("uid", uid);
        requestContext.getFlowScope().put("userHash", userHash);
        requestContext.getFlowScope().put("divNoCodeDisplay", false);

        JSONObject userInfos = esupOtpService.getUserInfos(uid);
        List<EsupOtpMethod> listMethods = new ArrayList<EsupOtpMethod>();
        EsupOtpUser user = new EsupOtpUser(uid, userHash);

        try {
            JSONObject methods = (JSONObject) ((JSONObject) userInfos.get("user")).get("methods");
            JSONObject transports = (JSONObject) ((JSONObject) userInfos.get("user")).get("transports");

            Boolean defaultWaitingFor = false;
            for (String method : methods.keySet()) {
                if ("waitingFor".equals(method)) {
                    defaultWaitingFor = (Boolean) methods.get(method);
                } else if ("codeRequired".equals(method)) {
                	Boolean codeRequired = (Boolean) methods.get(method);
                	log.debug("codeRequired : {}", codeRequired);
                } else {
                    listMethods.add(new EsupOtpMethod(method, (JSONObject) methods.get(method)));
                }
            }

            // will give order to the page to display only WaitingFor block if needed
            requestContext.getFlowScope().put("divNoCodeDisplay", defaultWaitingFor);
            requestContext.getFlowScope().put("waitingForMethodName", waitingForMethodName);
            
            user = new EsupOtpUser(uid, userHash, listMethods, transports);
        } catch (JSONException e) {
        	log.error("JSONException ...", e);
        }
        List<Map<String, String>> listTransports = esupOtpService.getTransports(listMethods);

        requestContext.getFlowScope().put("apiUrl", esupOtpConfigurationProperties.getUrlApi());
        requestContext.getFlowScope().put("listTransports", listTransports);
        requestContext.getFlowScope().put("user", user);

        return new EventFactorySupport().event(this, "authWithCode");
    }

  
}