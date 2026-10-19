package com.hxy.hxailife.travel;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

/** 攻略实际推荐节点，不复用表示用户输入的 TravelPlanItem。 */
@Data
@TableName("travel_plan_generation_items")
public class TravelPlanGenerationItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long generationId;
    @TableField("item_type")
    private GenerationItemType type;
    private Integer sortOrder;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer peopleNum;
    private String name;
    private String address;
    private String placeInfo;
    /** 高德 POI 详情中的首张地点图片 URL；起点和终点为空。 */
    private String imageUrl;
    /** POI ID 必须与来源一起比较；没有可靠 ID 时使用名称和地址辅助匹配。 */
    private String poiSource;
    private String poiId;
    /** 对应用户明确指定的地点或餐厅时为 true，不参与强制更换。 */
    private Boolean fixedByUser;
    private BigDecimal costPerPerson;
    /** 从上一节点到本节点的通勤方案；起点通常为空。 */
    private String transitPlan;
    private Integer transitMinutes;
    private BigDecimal transitCostPerPerson;
    private String xhsSearchQuery;
    private String xhsSearchUrl;
    private String dianpingUrl;
    private String remark;
}
