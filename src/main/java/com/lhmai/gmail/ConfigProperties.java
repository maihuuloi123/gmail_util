package com.lhmai.gmail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConfigProperties {
    private String credentialFile;
    private String dataStoreDir;
    private String appName;
    private String hostMail;
}
