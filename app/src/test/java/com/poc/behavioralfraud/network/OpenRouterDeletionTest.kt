package com.poc.behavioralfraud.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Tests verifying that OpenRouterClient.kt has been deleted (TASK-003 REQ-04).
 *
 * Security property: The legacy LLM client class must not exist at runtime.
 * If someone accidentally restores it, these tests catch it.
 */
class OpenRouterDeletionTest {

    @Test
    fun `REQ-04 - OpenRouterClient class does not exist at runtime`() {
        try {
            Class.forName("com.poc.behavioralfraud.network.OpenRouterClient")
            fail(
                "OpenRouterClient must be deleted (REQ-04). " +
                    "Class.forName should throw ClassNotFoundException but the class was found."
            )
        } catch (e: ClassNotFoundException) {
            assertNotNull(
                "ClassNotFoundException should have a message",
                e.message
            )
            assertTrue(
                "Exception message should reference the class name",
                e.message!!.contains("OpenRouterClient")
            )
        }
    }

    @Test
    fun `REQ-04 - OpenRouterClient class not loadable under any expected package variant`() {
        val possiblePackages = listOf(
            "com.poc.behavioralfraud.network.OpenRouterClient",
            "com.poc.behavioralfraud.OpenRouterClient",
            "com.poc.behavioralfraud.data.OpenRouterClient",
            "com.poc.behavioralfraud.api.OpenRouterClient"
        )

        for (fqcn in possiblePackages) {
            try {
                Class.forName(fqcn)
                fail(
                    "OpenRouterClient must be completely deleted (REQ-04), " +
                        "but it was found at: $fqcn"
                )
            } catch (e: ClassNotFoundException) {
                // Expected for each package variant
            }
        }
    }

    @Test
    fun `REQ-04 - network package contains BackendClient but not OpenRouterClient`() {
        val backendClientClass = try {
            Class.forName("com.poc.behavioralfraud.network.BackendClient")
        } catch (e: ClassNotFoundException) {
            null
        }

        val openRouterClientClass = try {
            Class.forName("com.poc.behavioralfraud.network.OpenRouterClient")
        } catch (e: ClassNotFoundException) {
            null
        }

        assertNotNull(
            "BackendClient must exist in the network package (sanity check)",
            backendClientClass
        )
        assertNull(
            "OpenRouterClient must NOT exist in the network package (REQ-04)",
            openRouterClientClass
        )
    }

    @Test
    fun `REQ-04 - BackendClient has no field or method referencing OpenRouter`() {
        val backendClientClass = BackendClient::class.java

        val allMethods = backendClientClass.declaredMethods
        for (method in allMethods) {
            assertFalse(
                "BackendClient method '${method.name}' must not reference OpenRouter",
                method.name.contains("openRouter", ignoreCase = true) ||
                    method.name.contains("openrouter", ignoreCase = true)
            )
        }

        val allFields = backendClientClass.declaredFields
        for (field in allFields) {
            assertFalse(
                "BackendClient field '${field.name}' must not reference OpenRouter",
                field.name.contains("openRouter", ignoreCase = true) ||
                    field.name.contains("openrouter", ignoreCase = true)
            )
        }
    }
}
