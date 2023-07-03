package com.aureliennioche.mapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GenericResponse {
    public final static String SUBJECT_LOGIN = "login";
    public final static String SUBJECT_EXERCISE = "update";
    public String subject;
}
