package com.example.successtudy.logic;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface BackendApi {

    class VerificationRequest {
        public String email;
        public String code;

        public VerificationRequest(String email, String code) {
            this.email = email;
            this.code = code;
        }
    }

    class VerificationResponse {
        public boolean success;
        public String message;
        public String error;
    }

    @POST("/send-verification")
    Call<VerificationResponse> sendVerificationEmail(@Body VerificationRequest request);
}
