package com.hxy.hxailife.travel.dto;

import com.hxy.hxailife.travel.vo.TravelPlanGenerationVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 攻略微调时传给应用层的上下文。
 *
 * <p>表单和当前攻略由 Service 层查询后组装，PlanApp 只负责把上下文交给大模型，
 * 避免应用层直接依赖 MySQL 查询逻辑。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlanRefineContextDTO {

    /** 生成当前攻略时使用的原始表单。 */
    private TravelPlanSaveDTO form;

    /** 当前正在查看的攻略版本。 */
    private TravelPlanGenerationVO currentGeneration;

    /** 本次微调输入；生成接口可以为空，此时使用已有聊天记忆。 */
    private String userMessage;
}
