package com.event.presentation.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.event.domain.exception.BusinessException;
import com.event.domain.exception.code.CommonCode;
import com.event.domain.exception.code.EventCode;
import com.event.presentation.dto.response.BaseResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnDomainCodeWhenBusinessExceptionOccurs() throws Exception {
        mockMvc.perform(get("/test/global-exception/business"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("이벤트가 존재하지 않습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void shouldReturnFieldErrorsWhenRequestBodyValidationFails() throws Exception {
        mockMvc.perform(post("/test/global-exception/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.data.name").value("name is required"));
    }

    @Test
    void shouldReturnHeaderErrorWhenRequestHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/test/global-exception/header"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.data['X-Test-Header']").value("X-Test-Header 헤더는 필수입니다."));
    }

    @Test
    void shouldReturnInvalidRequestWhenJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/test/global-exception/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    @Test
    void shouldReturnInvalidRequestWhenTypeConversionFails() throws Exception {
        mockMvc.perform(get("/test/global-exception/type/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.data.eventId").value("eventId 값의 형식이 올바르지 않습니다."));
    }

    @Test
    void shouldReturnInternalErrorWhenUnexpectedExceptionOccurs() throws Exception {
        mockMvc.perform(get("/test/global-exception/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다."));
    }

    @Test
    void shouldMapConstraintViolationToInvalidRequest() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);

        when(violation.getPropertyPath()).thenReturn(path);
        when(path.toString()).thenReturn("memberId");
        when(violation.getMessage()).thenReturn("must be positive");

        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<BaseResponse<Map<String, String>>> response =
                handler.handleConstraintViolationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(CommonCode.INVALID_REQUEST.getCode());
        assertThat(response.getBody().message()).isEqualTo(CommonCode.INVALID_REQUEST.getMessage());
        assertThat(response.getBody().data()).containsEntry("memberId", "must be positive");
    }

    @RestController
    @RequestMapping("/test/global-exception")
    public static class TestController {

        @GetMapping("/business")
        void business() {
            throw BusinessException.from(EventCode.EVENT_NOT_FOUND);
        }

        @PostMapping("/validation")
        void validation(@Valid @RequestBody TestValidationRequest request) {
        }

        @GetMapping("/header")
        void header(@RequestHeader("X-Test-Header") String header) {
        }

        @PostMapping("/json")
        void json(@RequestBody TestValidationRequest request) {
        }

        @GetMapping("/type/{eventId}")
        void type(@PathVariable Long eventId) {
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("boom");
        }
    }

    record TestValidationRequest(
            @NotBlank(message = "name is required")
            String name
    ) {
    }
}
