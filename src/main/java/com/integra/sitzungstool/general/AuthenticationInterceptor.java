package com.integra.sitzungstool.general;

import java.io.IOException;
import java.util.Base64;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthenticationInterceptor implements Interceptor {
    
    private String username;
    private String password;
    
    public AuthenticationInterceptor(String username, String password) {
        super();
        this.username = username;
        this.password = password;
    }
    
    @Override public Response intercept(Interceptor.Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", this.getAuthenticationHeader())
                .build();
        return chain.proceed(authenticatedRequest);
    }
    
    private String getAuthenticationHeader() {
        String stringToEncode = this.username + ":" + this.password;
        byte[] encodedBytes = Base64.getEncoder().encode(stringToEncode.getBytes());
        String encodedString = new String(encodedBytes);
        return "Basic " + encodedString;
    }
}
