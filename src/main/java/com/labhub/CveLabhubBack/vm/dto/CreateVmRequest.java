package com.labhub.CveLabhubBack.vm.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateVmRequest {
    private String vmName;
    private String instanceType;  // t2.micro, t2.small 등
}