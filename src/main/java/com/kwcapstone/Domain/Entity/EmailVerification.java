package com.kwcapstone.Domain.Entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "email_verification")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class EmailVerification {
    @Id
    private ObjectId emailId;
    private String email;
    private Integer verificationCode;
    private LocalDateTime expirationTime;
    private boolean verified = false;

    public EmailVerification(String email, Integer verificationCode, LocalDateTime expirationTime) {
        this.email = email;
        this.verificationCode = verificationCode;
        this.expirationTime = expirationTime;
        this.verified = false;
    }
}
