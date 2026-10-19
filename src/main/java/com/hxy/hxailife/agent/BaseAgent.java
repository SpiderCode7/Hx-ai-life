package com.hxy.hxailife.agent;

import com.alibaba.cloud.ai.graph.agent.Agent;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class BaseAgent {

    private Agent agent;

    private String name;
}
