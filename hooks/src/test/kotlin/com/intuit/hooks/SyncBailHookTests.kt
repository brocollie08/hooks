package com.intuit.hooks

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SyncBailHookTests {
    class Hook1<T1, R : Any?> : SyncBailHook<T1, (HookContext, T1) -> BailResult<R>, R>() {
        fun call(p1: T1, default: ((T1) -> Unit)? = null) = super.call({ f, context -> f(context, p1) }, default, p1)
    }

    @Test
    fun `null bail taps work`() {
        val calledA = mockk<(HookContext, String) -> BailResult<String>>()
        val calledB = mockk<(HookContext, String) -> BailResult<String>>()

        every { calledA.invoke(any(), any()) } returns BailResult.Continue()
        every { calledB.invoke(any(), any()) } returns BailResult.Continue()

        val h = Hook1<String, String>()
        h.tap("continue", calledA)
        h.tap("continue again", calledB)
        val result = h.call("Kian")

        Assertions.assertNull(result)

        verify(exactly = 1) {
            calledA.invoke(any(), any())
            calledB.invoke(any(), any())
        }
    }

    @Test
    fun `bail taps bail early`() {
        val h = Hook1<String, String>()
        h.tap("bail") { _, _ -> BailResult.Bail("bail now") }
        h.tap("continue again") { _, _ -> Assertions.fail("Should never have gotten here!") }

        val result = h.call("Kian")
        Assertions.assertEquals("bail now", result)
    }

    @Test
    fun `bail taps can bail without return value`() {
        val h = Hook1<String, Unit>()
        h.tap("continue") { _, _ -> BailResult.Continue() }
        h.tap("bail") { _, _ -> BailResult.Bail(Unit) }
        h.tap("continue again") { _, _ -> Assertions.fail("Should never have gotten here!") }

        Assertions.assertEquals(Unit, h.call("David"))
    }

    @Test
    fun `bail call with default handler invokes without taps bailing`() {
        val h = Hook1<String, String>()
        h.tap("continue") { _, _ -> BailResult.Continue() }
        h.tap("continue again") { _, _ -> BailResult.Continue() }

        var bailResult: String? = null
        h.call("David") {
            bailResult = it
        }

        Assertions.assertEquals("David", bailResult)
    }

    @Test
    fun `bail call with default handler does not invoke with bail`() {
        val h = Hook1<String, String>()
        h.tap("continue") { _, _ -> BailResult.Continue() }
        h.tap("bail") { _, _ -> BailResult.Bail("bailing") }
        h.tap("continue again") { _, _ -> Assertions.fail("Should never have gotten here!") }

        var bailResult: String? = null
        h.call("David") {
            bailResult = it
        }

        Assertions.assertEquals(null, bailResult)
    }
}
