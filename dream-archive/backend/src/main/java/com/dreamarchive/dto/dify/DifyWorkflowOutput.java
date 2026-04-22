package com.dreamarchive.dto.dify;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DifyWorkflowOutput {

    private String answer;
    private boolean violation;
}
