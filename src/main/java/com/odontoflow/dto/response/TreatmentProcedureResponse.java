package com.odontoflow.dto.response;

import com.odontoflow.entity.TreatmentProcedure;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TreatmentProcedureResponse {
    private UUID id;
    private String tooth;
    private String name;
    private BigDecimal value;
    private boolean paid;
    private boolean done;

    public static TreatmentProcedureResponse from(TreatmentProcedure p) {
        return new TreatmentProcedureResponse(
                p.getId(),
                p.getTooth(),
                p.getName(),
                p.getValue(),
                p.isPaid(),
                p.isDone()
        );
    }
}
