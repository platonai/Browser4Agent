package ai.platon.pulsar.agentic.mcp

import ai.platon.pulsar.agentic.context.AgenticContexts
import ai.platon.pulsar.agentic.event.AgenticEvents
import ai.platon.pulsar.agentic.model.ActionDescription
import ai.platon.pulsar.agentic.model.ToolCall
import ai.platon.pulsar.agentic.model.ToolSpec
import ai.platon.pulsar.agentic.tools.CustomToolRegistry
import ai.platon.pulsar.agentic.tools.builtin.AbstractToolExecutor
import ai.platon.pulsar.agentic.tools.specs.ToolCallSpecificationProvider
import ai.platon.pulsar.common.event.EventBus
import ai.platon.pulsar.external.ChatModelFactory
import kotlinx.coroutines.delay
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

/**
 * End-to-end test for MCP Agent integration.
 *
 * This test validates the complete MCP agent integration by:
 * 1. Creating a MockMCPToolExecutor that simulates MCP tool behavior
 * 2. Registering the executor with CustomToolRegistry under the MCP domain
 * 3. Creating an agent that can use MCP tools
 * 4. Running a task that invokes MCP tools via natural language
 * 5. Actually calling the LLM to process the task
 * 6. Capturing the ON_DID_GENERATE event to verify the MCP tool was called
 * 7. Validating that the actionDescription.toolCall is MCP-related
 *
 * This test pattern is based on SkillRegistrationAndInvocationE2ETest but adapted for MCP tools.
 *
 * ## Prerequisites
 *
 * Agent-based tests require LLM configuration. Set the following in application.properties:
 * ```
 * llm.provider=openai
 * llm.apiKey=your-api-key
 * ```
 *
 * Tests that require LLM will be automatically skipped if not configured.
 *
 * Test coverage:
 * - MCP tool registration with CustomToolRegistry
 * - Agent loading MCP tools from registered executor
 * - Agent calling MCP tools through natural language tasks
 * - LLM correctly generating tool calls for MCP domain
 * - Event-based verification of tool execution
 */
@Tag("E2ETest")
@Tag("mcp")
class MCPAgentE2ETest {

    companion object {
        private const val SERVER_NAME = "test-mcp-server"
        private const val MCP_DOMAIN = "mcp.$SERVER_NAME"
        private val capturedEvents = ConcurrentHashMap<String, MutableList<Map<String, Any?>>>()
        private val toolExecutionCount = AtomicInteger(0)
        private val echoToolCalled = AtomicBoolean(false)
        private val addToolCalled = AtomicBoolean(false)
        private val multiplyToolCalled = AtomicBoolean(false)

        @BeforeAll
        @JvmStatic
        fun setupEventHandlers() {
            // Register event handler for ON_DID_GENERATE to capture tool calls
            EventBus.register(AgenticEvents.ContextToAction.ON_DID_GENERATE) { payload ->
                val map = payload as? Map<String, Any?> ?: return@register null

                capturedEvents.computeIfAbsent(AgenticEvents.ContextToAction.ON_DID_GENERATE) {
                    mutableListOf()
                }.add(map)

                val actionDescription = map["actionDescription"] as? ActionDescription ?: return@register payload
                // Complete the action to allow test progression
                actionDescription.complete("Completed by MCP Agent E2E test handler")

                payload
            }
        }

        @AfterAll
        @JvmStatic
        fun cleanup() {
            // Unregister event handler
            EventBus.unregister(AgenticEvents.ContextToAction.ON_DID_GENERATE)
            // Unregister the MCP tool executor
            CustomToolRegistry.instance.unregister(MCP_DOMAIN)
        }
    }

    /**
     * A mock MCP tool executor that simulates MCP tool behavior for testing.
     * This executor implements the same tools as MockMCPServer: echo, add, multiply.
     */
    class MockMCPToolExecutor : AbstractToolExecutor(), ToolCallSpecificationProvider {
        override val domain: String = MCP_DOMAIN
        override val targetClass: KClass<*> = Any::class

        init {
            // Register tool specifications
            toolSpec["echo"] = ToolSpec(
                domain = domain,
                method = "echo",
                arguments = listOf(ToolSpec.Arg("message", "String")),
                returnType = "String",
                description = "Echoes back the input message"
            )
            toolSpec["add"] = ToolSpec(
                domain = domain,
                method = "add",
                arguments = listOf(
                    ToolSpec.Arg("a", "Double"),
                    ToolSpec.Arg("b", "Double")
                ),
                returnType = "Double",
                description = "Adds two numbers together"
            )
            toolSpec["multiply"] = ToolSpec(
                domain = domain,
                method = "multiply",
                arguments = listOf(
                    ToolSpec.Arg("a", "Double"),
                    ToolSpec.Arg("b", "Double")
                ),
                returnType = "Double",
                description = "Multiplies two numbers together"
            )
        }

        override suspend fun callFunctionOn(
            domain: String,
            functionName: String,
            args: Map<String, Any?>,
            target: Any
        ): Any? {
            toolExecutionCount.incrementAndGet()

            return when (functionName) {
                "echo" -> {
                    echoToolCalled.set(true)
                    val message = paramString(args, "message", functionName)
                    message
                }
                "add" -> {
                    addToolCalled.set(true)
                    val a = paramDouble(args, "a", functionName) ?: 0.0
                    val b = paramDouble(args, "b", functionName) ?: 0.0
                    a + b
                }
                "multiply" -> {
                    multiplyToolCalled.set(true)
                    val a = paramDouble(args, "a", functionName) ?: 0.0
                    val b = paramDouble(args, "b", functionName) ?: 0.0
                    a * b
                }
                else -> throw IllegalArgumentException("Unknown method: $functionName")
            }
        }

        override fun getToolCallSpecifications(): List<ToolSpec> {
            return toolSpec.values.toList()
        }
    }

    @BeforeEach
    fun setup() {
        // Clear captured events and counters
        capturedEvents.clear()
        toolExecutionCount.set(0)
        echoToolCalled.set(false)
        addToolCalled.set(false)
        multiplyToolCalled.set(false)

        // Register the mock MCP tool executor
        if (!CustomToolRegistry.instance.contains(MCP_DOMAIN)) {
            CustomToolRegistry.instance.register(MockMCPToolExecutor())
        }
    }

    @AfterEach
    fun tearDown() {
        // Clean up after each test
    }

    /**
     * Test that the agent can load MCP tools and invoke them through natural language.
     *
     * This test:
     * 1. Registers the MockMCPToolExecutor
     * 2. Creates an agent session
     * 3. Instructs the agent to use the MCP echo tool
     * 4. Verifies that the LLM generates the correct MCP tool call
     * 5. Validates the tool call domain is MCP-related
     *
     * Note: This test requires LLM configuration and will be skipped if not configured.
     */
    @Test
    suspend fun testAgentLoadsMCPAndCallsEchoTool() {
        // Verify tool executor is registered
        assertTrue(CustomToolRegistry.instance.contains(MCP_DOMAIN),
            "MCP tool executor should be registered")

        // Create agent session
        val session = AgenticContexts.getOrCreateSession()

        // Check if LLM is configured, skip test if not
        val isLLMConfigured = ChatModelFactory.isModelConfigured(session.sessionConfig)
        Assumptions.assumeTrue(isLLMConfigured,
            "Skipping test: LLM not configured. See docs/config/llm/llm-config.md")

        val driver = session.createBoundDriver()
        val agent = session.companionAgent
        assertNotNull(agent, "Agent should be created")

        // Open a simple page (required for agent context)
        val url = "https://example.com"
        driver.open(url)

        // Define task using MCP echo tool
        val testMessage = "Hello from MCP E2E Test"
        val task = """
            Use the $MCP_DOMAIN.echo tool to echo the following message: "$testMessage"
        """.trimIndent()

        // Run the agent task (this actually calls the LLM)
        val history = agent.run(task)

        // Allow time for event processing
        delay(500)

        // Verify the agent ran and completed
        assertNotNull(history, "History should not be null")
        assertTrue(history.size > 0, "History should contain at least one execution")

        // Verify ON_DID_GENERATE event was captured
        val events = capturedEvents[AgenticEvents.ContextToAction.ON_DID_GENERATE]
        assertNotNull(events, "ON_DID_GENERATE events should be captured")
        assertTrue(events.isNotEmpty(), "At least one ON_DID_GENERATE event should be captured")

        // Extract actionDescription from the captured events
        val actionDescriptions = events.mapNotNull { it["actionDescription"] as? ActionDescription }
        assertTrue(actionDescriptions.isNotEmpty(), "At least one ActionDescription should be present")

        // Verify that at least one actionDescription contains an MCP-related toolCall
        val mcpRelatedToolCalls = actionDescriptions.mapNotNull { actionDescription ->
            val toolCall = actionDescription.toolCall
            if (toolCall != null && isMCPRelatedToolCall(toolCall)) toolCall else null
        }

        assertTrue(mcpRelatedToolCalls.isNotEmpty(),
            "At least one ActionDescription should have an MCP-related toolCall. " +
            "Found ${actionDescriptions.size} ActionDescriptions, " +
            "but none had MCP-related toolCalls. " +
            "Domains found: ${actionDescriptions.mapNotNull { it.toolCall?.domain }}")

        // Additional verification: ensure the toolCall has the expected MCP domain
        val hasCorrectDomain = mcpRelatedToolCalls.any { it.domain.startsWith("mcp") }
        assertTrue(hasCorrectDomain,
            "At least one MCP toolCall should have domain starting with 'mcp'. " +
            "Found domains: ${mcpRelatedToolCalls.map { it.domain }}")
    }

    /**
     * Test that the agent can call the MCP add tool through natural language.
     *
     * Note: This test requires LLM configuration and will be skipped if not configured.
     */
    @Test
    suspend fun testAgentCallsMCPAddTool() {
        // Verify tool executor is registered
        assertTrue(CustomToolRegistry.instance.contains(MCP_DOMAIN),
            "MCP tool executor should be registered")

        // Create agent session
        val session = AgenticContexts.getOrCreateSession()

        // Check if LLM is configured, skip test if not
        val isLLMConfigured = ChatModelFactory.isModelConfigured(session.sessionConfig)
        Assumptions.assumeTrue(isLLMConfigured,
            "Skipping test: LLM not configured. See docs/config/llm/llm-config.md")

        val driver = session.createBoundDriver()
        val agent = session.companionAgent
        assertNotNull(agent, "Agent should be created")

        // Open a simple page
        driver.open("https://example.com")

        // Task: Use MCP add tool
        val task = """
            Use the $MCP_DOMAIN.add tool to add 42 and 58 together.
        """.trimIndent()

        // Run the agent task
        val history = agent.run(task)

        // Allow time for event processing
        delay(500)

        // Verify execution
        assertNotNull(history, "History should not be null")
        assertTrue(history.size > 0, "History should contain at least one execution")

        // Verify events were captured
        val events = capturedEvents[AgenticEvents.ContextToAction.ON_DID_GENERATE]
        assertNotNull(events, "ON_DID_GENERATE events should be captured")
        assertTrue(events.isNotEmpty(), "At least one ON_DID_GENERATE event should be captured")

        // Verify MCP tool calls
        val actionDescriptions = events.mapNotNull { it["actionDescription"] as? ActionDescription }
        val mcpToolCalls = actionDescriptions.mapNotNull { actionDescription ->
            val toolCall = actionDescription.toolCall
            if (toolCall != null && isMCPRelatedToolCall(toolCall)) toolCall else null
        }

        assertTrue(mcpToolCalls.isNotEmpty(),
            "At least one ActionDescription should have an MCP-related toolCall")
    }

    /**
     * Test that the agent can call the MCP multiply tool through natural language.
     *
     * Note: This test requires LLM configuration and will be skipped if not configured.
     */
    @Test
    suspend fun testAgentCallsMCPMultiplyTool() {
        // Verify tool executor is registered
        assertTrue(CustomToolRegistry.instance.contains(MCP_DOMAIN),
            "MCP tool executor should be registered")

        // Create agent session
        val session = AgenticContexts.getOrCreateSession()

        // Check if LLM is configured, skip test if not
        val isLLMConfigured = ChatModelFactory.isModelConfigured(session.sessionConfig)
        Assumptions.assumeTrue(isLLMConfigured,
            "Skipping test: LLM not configured. See docs/config/llm/llm-config.md")

        val driver = session.createBoundDriver()
        val agent = session.companionAgent
        assertNotNull(agent, "Agent should be created")

        // Open a simple page
        driver.open("https://example.com")

        // Task: Use MCP multiply tool
        val task = """
            Use the $MCP_DOMAIN.multiply tool to multiply 12 by 8.
        """.trimIndent()

        // Run the agent task
        val history = agent.run(task)

        // Allow time for event processing
        delay(500)

        // Verify execution
        assertNotNull(history, "History should not be null")
        assertTrue(history.size > 0, "History should contain at least one execution")

        // Verify events were captured
        val events = capturedEvents[AgenticEvents.ContextToAction.ON_DID_GENERATE]
        assertNotNull(events, "ON_DID_GENERATE events should be captured")
        assertTrue(events.isNotEmpty(), "At least one ON_DID_GENERATE event should be captured")

        // Verify MCP tool calls
        val actionDescriptions = events.mapNotNull { it["actionDescription"] as? ActionDescription }
        val mcpToolCalls = actionDescriptions.mapNotNull { actionDescription ->
            val toolCall = actionDescription.toolCall
            if (toolCall != null && isMCPRelatedToolCall(toolCall)) toolCall else null
        }

        assertTrue(mcpToolCalls.isNotEmpty(),
            "At least one ActionDescription should have an MCP-related toolCall")
    }

    /**
     * Test that MCP tools are correctly discovered and registered.
     */
    @Test
    fun testMCPToolDiscoveryAndRegistration() {
        // Verify tool executor is registered
        assertTrue(CustomToolRegistry.instance.contains(MCP_DOMAIN),
            "MCP tool executor should be registered")

        // Get the tool executor
        val toolExecutor = CustomToolRegistry.instance.get(MCP_DOMAIN)
        assertNotNull(toolExecutor, "Tool executor should be available")

        // Verify tools are registered via help text
        val helpText = toolExecutor.help()
        assertTrue(helpText.contains("echo") || helpText.contains("Echoes"),
            "Help text should mention echo tool")
        assertTrue(helpText.contains("add") || helpText.contains("Adds"),
            "Help text should mention add tool")
        assertTrue(helpText.contains("multiply") || helpText.contains("Multiplies"),
            "Help text should mention multiply tool")

        // Verify domain format
        assertEquals(MCP_DOMAIN, toolExecutor.domain,
            "Tool executor domain should be '$MCP_DOMAIN'")

        // Verify tool specs are available
        val specs = CustomToolRegistry.instance.getToolCallSpecifications(MCP_DOMAIN)
        assertEquals(3, specs.size, "Should have 3 tool specs registered")

        val specNames = specs.map { it.method }.toSet()
        assertTrue(specNames.containsAll(setOf("echo", "add", "multiply")),
            "All expected tools should be registered as specs")
    }

    /**
     * Test that MCP tool executor can execute tools directly.
     */
    @Test
    suspend fun testMCPToolDirectExecution() {
        // Get the tool executor
        val toolExecutor = CustomToolRegistry.instance.get(MCP_DOMAIN) as? MockMCPToolExecutor
        assertNotNull(toolExecutor, "Tool executor should be a MockMCPToolExecutor")

        // Test echo tool
        val echoCall = ToolCall(
            domain = MCP_DOMAIN,
            method = "echo",
            arguments = mutableMapOf("message" to "Hello Test")
        )
        val echoResult = toolExecutor.callFunctionOn(echoCall, Any())
        assertNotNull(echoResult.value, "Echo result should not be null")
        assertEquals("Hello Test", echoResult.value, "Echo should return the input message")
        assertTrue(echoToolCalled.get(), "Echo tool should have been called")

        // Test add tool
        val addCall = ToolCall(
            domain = MCP_DOMAIN,
            method = "add",
            arguments = mutableMapOf("a" to 10.0, "b" to 20.0)
        )
        val addResult = toolExecutor.callFunctionOn(addCall, Any())
        assertNotNull(addResult.value, "Add result should not be null")
        assertEquals(30.0, addResult.value, "Add should return the sum")
        assertTrue(addToolCalled.get(), "Add tool should have been called")

        // Test multiply tool
        val multiplyCall = ToolCall(
            domain = MCP_DOMAIN,
            method = "multiply",
            arguments = mutableMapOf("a" to 6.0, "b" to 7.0)
        )
        val multiplyResult = toolExecutor.callFunctionOn(multiplyCall, Any())
        assertNotNull(multiplyResult.value, "Multiply result should not be null")
        assertEquals(42.0, multiplyResult.value, "Multiply should return the product")
        assertTrue(multiplyToolCalled.get(), "Multiply tool should have been called")

        // Verify execution count
        assertEquals(3, toolExecutionCount.get(), "Should have executed 3 tool calls")
    }

    /**
     * Determines if a toolCall is related to MCP.
     * A toolCall is considered MCP-related if:
     * - The domain starts with "mcp"
     * - The domain contains the MCP server name
     */
    private fun isMCPRelatedToolCall(toolCall: ToolCall): Boolean {
        return toolCall.domain.startsWith("mcp") ||
               toolCall.domain.contains(SERVER_NAME)
    }
}
