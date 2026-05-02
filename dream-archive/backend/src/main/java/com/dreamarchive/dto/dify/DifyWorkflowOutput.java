package com.dreamarchive.dto.dify;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Dify 工作流 outputs 解析结果：助手正文与是否违规。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DifyWorkflowOutput {

    private String answer;
    private boolean violation;
}
