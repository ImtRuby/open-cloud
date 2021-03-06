package com.github.lyd.gateway.provider.filter;

import com.github.lyd.base.client.entity.SystemApp;
import com.github.lyd.common.configuration.GatewayProperties;
import com.github.lyd.common.exception.OpenSignatureDeniedHandler;
import com.github.lyd.common.exception.OpenSignatureException;
import com.github.lyd.common.exception.SignatureDeniedHandler;
import com.github.lyd.common.model.ResultBody;
import com.github.lyd.common.security.OpenHelper;
import com.github.lyd.common.security.OpenUserAuth;
import com.github.lyd.common.utils.SignatureUtils;
import com.github.lyd.common.utils.WebUtils;
import com.github.lyd.gateway.provider.service.feign.SystemAppClient;
import com.google.common.collect.Lists;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 自定义签名过滤器,认证完成之后执行
 *
 * @author: liuyadu
 * @date: 2018/11/28 18:26
 * @description:
 */
public class SignatureFilter implements Filter {
    private SignatureDeniedHandler signatureDeniedHandler;
    private SystemAppClient systemAppClient;
    private GatewayProperties gatewayProperties;
    /**
     * 忽略签名
     */
    private final static List<RequestMatcher> NOT_SIGN = getIgnoreMatchers(
            "/sign",
            "/**/login/**",
            "/**/logout/**"
    );

    public SignatureFilter(SystemAppClient systemAppClient, GatewayProperties gatewayProperties) {
        this.systemAppClient = systemAppClient;
        this.gatewayProperties = gatewayProperties;
        this.signatureDeniedHandler = new OpenSignatureDeniedHandler();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    protected static List<RequestMatcher> getIgnoreMatchers(String... antPatterns) {
        List<RequestMatcher> matchers = Lists.newArrayList();
        for (String path : antPatterns) {
            matchers.add(new AntPathRequestMatcher(path));
        }
        return matchers;
    }

    protected boolean notSign(HttpServletRequest request) {
        for (RequestMatcher match : NOT_SIGN) {
            if (match.matches(request)) {
                return true;
            }
        }
        return false;
    }

    ;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        OpenUserAuth auth = OpenHelper.getUserAuth();
        if (isAuthenticated() && gatewayProperties.getEnabledValidateSign() && !notSign(request)) {
            try {
                //开始验证签名
                String appId = auth.getAuthAppId();
                if (systemAppClient != null && appId != null) {
                    Map params = WebUtils.getParameterMap(request);
                    // 验证请求参数
                    SignatureUtils.validateParams(params);
                    // 获取客户端信息
                    ResultBody<SystemApp> result = systemAppClient.getApp(appId);
                    SystemApp app = result.getData();
                    if (app == null) {
                        throw new OpenSignatureException("clientId无效");
                    }
                    // 强制覆盖请求参数clientId
                    params.put("clientId", app.getAppId());
                    // 服务器验证签名结果
                    if (!SignatureUtils.validateSign(params, app.getAppSecret())) {
                        throw new OpenSignatureException("签名验证失败!");
                    }
                }
            } catch (Exception ex) {
                signatureDeniedHandler.handle(request, response, ex);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        return true;
    }

    @Override
    public void destroy() {

    }

}
