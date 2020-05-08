package com.lhmai.gmail;

import java.io.File;

public interface MailerService {
    boolean send(Email email);

    boolean send(Email email, File file, String fileName);

}
