package ai.platon.pulsar.agentic.event

/**
 * Centralized event type definitions for the agentic module.
 *
 * This object provides a unified location for all event types used with DangerousEventBus,
 * making it easier to discover, maintain, and document the events in the system.
 */
object AgenticEvents {

    /**
     * AgentEventBus event type constants for agent lifecycle events.
     * Used with AgentEventBus.emitAgentEvent().
     */
    object AgentEventTypes {
        const val ON_WILL_RUN = "agent.onWillRun"
        const val ON_DID_RUN = "agent.onDidRun"
        const val ON_WILL_OBSERVE = "agent.onWillObserve"
        const val ON_DID_OBSERVE = "agent.onDidObserve"
        const val ON_WILL_ACT = "agent.onWillAct"
        const val ON_DID_ACT = "agent.onDidAct"
        const val ON_WILL_EXTRACT = "agent.onWillExtract"
        const val ON_DID_EXTRACT = "agent.onDidExtract"
        const val ON_WILL_SUMMARIZE = "agent.onWillSummarize"
        const val ON_DID_SUMMARIZE = "agent.onDidSummarize"
    }

    /**
     * AgentEventBus event type constants for inference events.
     * Used with AgentEventBus.emitInferenceEvent().
     */
    object InferenceEventTypes {
        const val ON_WILL_INFER = "inference.onWillInfer"
        const val ON_DID_INFER = "inference.onDidInfer"
        const val ON_WILL_EXTRACT_INFER = "inference.onWillExtractInfer"
        const val ON_DID_EXTRACT_INFER = "inference.onDidExtractInfer"
        const val ON_WILL_SUMMARIZE_INFER = "inference.onWillSummarizeInfer"
        const val ON_DID_SUMMARIZE_INFER = "inference.onDidSummarizeInfer"
    }

    /**
     * AgentEventBus event type constants for tool execution events.
     * Used with AgentEventBus.emitToolEvent().
     */
    object ToolEventTypes {
        const val ON_WILL_EXECUTE_TOOL = "tool.onWillExecuteTool"
        const val ON_DID_EXECUTE_TOOL = "tool.onDidExecuteTool"
        const val ON_TOOL_ERROR = "tool.onToolError"
    }

    /**
     * AgentEventBus event type constants for MCP (Model Context Protocol) events.
     * Used with AgentEventBus.emitMCPEvent().
     */
    object MCPEventTypes {
        const val ON_WILL_CALL_MCP = "mcp.onWillCallMCP"
        const val ON_DID_CALL_MCP = "mcp.onDidCallMCP"
        const val ON_MCP_CONNECTED = "mcp.onMCPConnected"
        const val ON_MCP_DISCONNECTED = "mcp.onMCPDisconnected"
        const val ON_MCP_ERROR = "mcp.onMCPError"
    }

    /**
     * AgentEventBus event type constants for skill execution events.
     * Used with AgentEventBus.emitSkillEvent().
     */
    object SkillEventTypes {
        const val ON_WILL_RUN_SKILL = "skill.onWillRunSkill"
        const val ON_DID_RUN_SKILL = "skill.onDidRunSkill"
        const val ON_SKILL_ACTIVATED = "skill.onSkillActivated"
        const val ON_SKILLS_LISTED = "skill.onSkillsListed"
        const val ON_SKILL_ERROR = "skill.onSkillError"
    }

    /**
     * Events emitted by PerceptiveAgent implementations.
     */
    object PerceptiveAgent {
        /**
         * Emitted before executing the run method.
         * Payload: Map containing "action" (ActionOptions), "uuid" (UUID)
         */
        const val ON_WILL_RUN = "PerceptiveAgent.onWillRun"

        /**
         * Emitted after executing the run method.
         * Payload: Map containing "action" (ActionOptions), "uuid" (UUID),
         *          "result" (ActResult), "stateHistory" (AgentHistory)
         */
        const val ON_DID_RUN = "PerceptiveAgent.onDidRun"

        /**
         * Emitted before executing the observe method.
         * Payload: Map containing "options" (ObserveOptions), "uuid" (UUID)
         */
        const val ON_WILL_OBSERVE = "PerceptiveAgent.onWillObserve"

        /**
         * Emitted after executing the observe method.
         * Payload: Map containing "options" (ObserveOptions), "uuid" (UUID),
         *          "observeResults" (List<ObserveResult>), "actionDescription" (ActionDescription)
         */
        const val ON_DID_OBSERVE = "PerceptiveAgent.onDidObserve"

        /**
         * Emitted before executing the act method.
         * Payload: Map containing "action" (ActionOptions), "uuid" (UUID)
         */
        const val ON_WILL_ACT = "PerceptiveAgent.onWillAct"

        /**
         * Emitted after executing the act method.
         * Payload: Map containing "action" (ActionOptions), "uuid" (UUID), "result" (ActResult)
         */
        const val ON_DID_ACT = "PerceptiveAgent.onDidAct"

        /**
         * Emitted before executing the extract method.
         * Payload: Map containing "options" (ExtractOptions), "uuid" (UUID)
         */
        const val ON_WILL_EXTRACT = "PerceptiveAgent.onWillExtract"

        /**
         * Emitted after executing the extract method.
         * Payload: Map containing "options" (ExtractOptions), "uuid" (UUID), "result" (ExtractResult)
         */
        const val ON_DID_EXTRACT = "PerceptiveAgent.onDidExtract"

        /**
         * Emitted before executing the summarize method.
         * Payload: Map containing "instruction" (String?), "selector" (String?), "uuid" (UUID)
         */
        const val ON_WILL_SUMMARIZE = "PerceptiveAgent.onWillSummarize"

        /**
         * Emitted after executing the summarize method.
         * Payload: Map containing "instruction" (String?), "selector" (String?),
         *          "uuid" (UUID), "result" (String)
         */
        const val ON_DID_SUMMARIZE = "PerceptiveAgent.onDidSummarize"
    }

    /**
     * Events emitted by InferenceEngine.
     */
    object InferenceEngine {
        /**
         * Emitted before observe inference in BasicBrowserAgent init block.
         * Payload: Map containing "messages" (AgentMessageList)
         */
        const val ON_WILL_OBSERVE = "InferenceEngine.onWillObserve"

        /**
         * Emitted after observe inference in BasicBrowserAgent init block.
         * Payload: Map containing "actionDescription" (ActionDescription)
         */
        const val ON_DID_OBSERVE = "InferenceEngine.onDidObserve"

        /**
         * Emitted before extract inference.
         * Payload: Map containing "params" (ExtractParams)
         */
        const val ON_WILL_EXTRACT = "InferenceEngine.onWillExtract"

        /**
         * Emitted after extract inference.
         * Payload: Map containing "params" (ExtractParams), "result" (ObjectNode),
         *          "extractedNode" (ObjectNode), "metaNode" (ObjectNode)
         */
        const val ON_DID_EXTRACT = "InferenceEngine.onDidExtract"

        /**
         * Emitted before summarize inference.
         * Payload: Map containing "instruction" (String?), "messages" (AgentMessageList),
         *          "textContent" (String)
         */
        const val ON_WILL_SUMMARIZE = "InferenceEngine.onWillSummarize"

        /**
         * Emitted after summarize inference.
         * Payload: Map containing "instruction" (String?), "textContentLength" (Int),
         *          "result" (String), "tokenUsage" (TokenUsage)
         */
        const val ON_DID_SUMMARIZE = "InferenceEngine.onDidSummarize"
    }

    /**
     * Events emitted by ContextToAction during action generation.
     */
    object ContextToAction {
        /**
         * Emitted before generating action from context.
         * Payload: Map containing "context" (ExecutionContext), "messages" (AgentMessageList)
         */
        const val ON_WILL_GENERATE = "ContextToAction.onWillGenerate"

        /**
         * Emitted after generating action from context.
         * Payload: Map containing "context" (ExecutionContext), "messages" (AgentMessageList),
         *          "actionDescription" (ActionDescription)
         */
        const val ON_DID_GENERATE = "ContextToAction.onDidGenerate"
    }

    /**
     * Returns all event types as a list for easy iteration.
     */
    fun getAllEventTypes(): List<String> = listOf(
        // PerceptiveAgent events
        PerceptiveAgent.ON_WILL_RUN,
        PerceptiveAgent.ON_DID_RUN,
        PerceptiveAgent.ON_WILL_OBSERVE,
        PerceptiveAgent.ON_DID_OBSERVE,
        PerceptiveAgent.ON_WILL_ACT,
        PerceptiveAgent.ON_DID_ACT,
        PerceptiveAgent.ON_WILL_EXTRACT,
        PerceptiveAgent.ON_DID_EXTRACT,
        PerceptiveAgent.ON_WILL_SUMMARIZE,
        PerceptiveAgent.ON_DID_SUMMARIZE,
        // InferenceEngine events
        InferenceEngine.ON_WILL_OBSERVE,
        InferenceEngine.ON_DID_OBSERVE,
        InferenceEngine.ON_WILL_EXTRACT,
        InferenceEngine.ON_DID_EXTRACT,
        InferenceEngine.ON_WILL_SUMMARIZE,
        InferenceEngine.ON_DID_SUMMARIZE,
        // ContextToAction events
        ContextToAction.ON_WILL_GENERATE,
        ContextToAction.ON_DID_GENERATE
    )

}
