package ai.platon.pulsar.agentic.skills

import ai.platon.pulsar.agentic.context.DefaultClassPathXmlAgenticContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Integration test for SkillBootstrap auto-loading in Spring context.
 *
 * This test verifies that skills are automatically loaded from classpath resources
 * (src/main/resources/skills/\*) when the AgenticContext is created.
 */
class SkillBootstrapIntegrationTest {

    private lateinit var context: DefaultClassPathXmlAgenticContext

    @BeforeEach
    fun setup() {
        // Create the Spring context which should trigger SkillBootstrap initialization
        context = DefaultClassPathXmlAgenticContext()
    }

    @AfterEach
    fun cleanup() {
        context.close()
    }

    @Test
    fun skillsAreAutoLoadedInSpringContext() {
        val registry = SkillRegistry.instance

        // At least the built-in resource skills should be present.
        assertTrue(registry.size() >= 3, "At least 3 resource skills should be auto-loaded")

        // Verify specific resource skills (defined by resources/skills/*/SKILL.md)
        assertTrue(registry.contains("web-scraping"), "Skill 'web-scraping' should be auto-loaded")
        assertTrue(registry.contains("form-filling"), "Skill 'form-filling' should be auto-loaded")
        assertTrue(registry.contains("data-validation"), "Skill 'data-validation' should be auto-loaded")
    }

    @Test
    fun skillsAreAccessibleAfterContextCreation() {
        val registry = SkillRegistry.instance

        val webScrapingSkill = registry.get("web-scraping")
        assertNotNull(webScrapingSkill)

        assertEquals("Web Scraping", webScrapingSkill!!.metadata.name)
        assertEquals("1.0.0", webScrapingSkill.metadata.version)
    }

    @Test
    fun skillDependenciesAreSatisfied() {
        val registry = SkillRegistry.instance

        val formFillingSkill = registry.get("form-filling")
        assertNotNull(formFillingSkill)

        val dependencies = formFillingSkill!!.metadata.dependencies
        assertTrue(dependencies.contains("web-scraping"))

        dependencies.forEach { depId ->
            assertTrue(registry.contains(depId), "Dependency $depId should be loaded")
        }
    }
}
