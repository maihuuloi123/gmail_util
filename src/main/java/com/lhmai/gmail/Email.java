package com.lhmai.gmail;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Email {
    private String[] tos;
    private String subject;
    private String text;
    private boolean isHtml = false;
    private List<String> carbonCopies = new ArrayList<>();

    public void setEmailTos(String... to) {
        tos = to;
    }

}
