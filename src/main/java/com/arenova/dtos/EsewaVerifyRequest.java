package com.arenova.dtos;

import lombok.Data;

@Data
public class EsewaVerifyRequest {

    /** Base64-encoded callback payload from eSewa success redirect. */
    private String data;
}
