package com.odontoflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAlertResponse {
    private String id;
    private String type;
    private String severity;
    private String title;
    private long count;
    private String actionUrl;
}
