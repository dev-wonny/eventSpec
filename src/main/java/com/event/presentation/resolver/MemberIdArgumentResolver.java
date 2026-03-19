package com.event.presentation.resolver;

import com.event.presentation.header.ApiHeaderNames;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class MemberIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(MemberId.class)
                && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) throws Exception {
        MemberId memberId = parameter.getParameterAnnotation(MemberId.class);
        Long resolvedMemberId = (Long) webRequest.getAttribute(ApiHeaderNames.X_MEMBER_ID, NativeWebRequest.SCOPE_REQUEST);

        if (Objects.nonNull(resolvedMemberId)) {
            return resolvedMemberId;
        }

        if (Objects.nonNull(memberId) && memberId.required()) {
            throw new MissingRequestHeaderException(ApiHeaderNames.X_MEMBER_ID, parameter);
        }

        return null;
    }
}
