package com.lhmai.gmail;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GmailCredential {
    private String appName;
    private String hostMail;
    private String clientSecret;
    private String clientId;
}

