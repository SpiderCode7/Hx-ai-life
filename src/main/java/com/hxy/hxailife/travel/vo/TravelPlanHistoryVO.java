package com.hxy.hxailife.travel.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 历史表单详情及该表单的成功攻略，供前端一次加载后切换查看。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlanHistoryVO {

    private TravelPlanDetailVO form;
    private List<TravelPlanGenerationVO> generations;
}
