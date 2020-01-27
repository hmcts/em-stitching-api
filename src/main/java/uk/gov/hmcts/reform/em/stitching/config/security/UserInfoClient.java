package uk.gov.hmcts.reform.em.stitching.config.security;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.springframework.cloud.openfeign.FeignClient;

import java.util.Map;

@FeignClient
public interface UserInfoClient {

    @RequestLine("POST /userinfo")
    @Headers({
            "Content-Type: application/x-www-form-urlencoded",
            "Accept: application/json",
            "Authorization: {authorization}"
    })
    Map<String, Object> userInfo(@Param("authorization") String var1, @Param("claims") String var3);
}
