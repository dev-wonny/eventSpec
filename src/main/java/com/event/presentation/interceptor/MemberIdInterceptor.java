package com.event.presentation.interceptor;

import com.event.presentation.header.ApiHeaderNames;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class MemberIdInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String rawMemberId = request.getHeader(ApiHeaderNames.X_MEMBER_ID);
        if (rawMemberId == null || rawMemberId.isBlank()) {
            return true;
        }

        try {
            request.setAttribute(ApiHeaderNames.X_MEMBER_ID, Long.valueOf(rawMemberId));
            return true;
        } catch (NumberFormatException ex) {
            MethodParameter methodParameter = handler instanceof HandlerMethod handlerMethod
                    ? findMemberIdParameter(handlerMethod)
                    : null;
            throw new MethodArgumentTypeMismatchException(
                    rawMemberId,
                    Long.class,
                    ApiHeaderNames.X_MEMBER_ID,
                    methodParameter,
                    ex
            );
        }
    }

    private MethodParameter findMemberIdParameter(HandlerMethod handlerMethod) {
        for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
            if (parameter.hasParameterAnnotation(com.event.presentation.resolver.MemberId.class)) {
                return parameter;
            }
        }

        return null;
    }
}
