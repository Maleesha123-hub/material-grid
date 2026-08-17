package com.pixelMind.materialGrid.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
@Setter
public class CommonResponseDTO {

    private String message;
    private Object data;
    private HttpStatus status;

}
