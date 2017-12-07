package org.pitest.kotlin

import com.example.NotDestructuringBecauseIAmJava
import com.example.Result
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.pitest.classpath.ClassloaderByteArraySource;
import org.pitest.mutationtest.build.intercept.javafeatures.FilterTester
import org.pitest.mutationtest.engine.gregor.mutators.IncrementsMutator
import org.pitest.mutationtest.build.InterceptorType
import org.pitest.mutationtest.engine.gregor.config.Mutator

class KotlinSupportTest {

    val testee = KotlinInterceptor()
    val verifier = FilterTester("", testee, Mutator.all())
  
    
    @Test
    fun `declares type as filter`() {
      assertThat(testee.type()).isEqualTo(InterceptorType.FILTER)
    }

    @Test
    fun `filters mutations to componentN method calls in destructure statements`() {
        verifier.assertFiltersNMutationFromClass(3, Destructure::class.java)
    }

    @Test
    fun `does not filter mutations to methods called componentN with arguments`() {
        verifier.assertFiltersNMutationFromClass(0, NotDestructuring::class.java)
    }

    @Test
    fun `does not filter mutations in java classes`() {
        verifier.assertFiltersNMutationFromClass(0, NotDestructuringBecauseIAmJava::class.java)
    }

}

data class DestructureMe(val a : Int, val b : String, val c : Int)

class Destructure {
    fun foo(d : DestructureMe) {
        val (a,b,c) = d
        println("" + a + b + c)
    }
}

class NotDestructuring {

    fun foo(i : Int) {
        component1(i);
    }

    private fun component1(i : Int) {
        println("hello" + i)
    }
}

