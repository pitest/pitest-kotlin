package org.pitest.kotlin

import com.example.NotDestructuringBecauseIAmJava
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.pitest.mutationtest.build.InterceptorType
import org.pitest.mutationtest.build.intercept.javafeatures.FilterTester
import org.pitest.mutationtest.engine.gregor.mutators.NullMutateEverything

class KotlinSupportTest {

  private val testee = KotlinInterceptor()
  private val verifier = FilterTester("", testee, NullMutateEverything.asList())

  @Test
  fun `declares type as filter`() {
    assertThat(testee.type()).isEqualTo(InterceptorType.FILTER)
  }

  @Test
  fun `filters mutations to componentN method calls in destructure statements`() {
    verifier.assertFiltersMutationAtNLocations(3, Destructure::class.java)
  }

  @Test
  fun `does not filter mutations to methods called componentN with arguments`() {
    verifier.assertFiltersNMutationFromClass(0, NotDestructuring::class.java)
  }

  @Test
  fun `does not filter mutations in java classes`() {
    verifier.assertFiltersNMutationFromClass(0, NotDestructuringBecauseIAmJava::class.java)
  }

  @Test
  fun `filters mutations to !! null casts`() {
    // condtional and intrinsic method call
    verifier.assertFiltersMutationAtNLocations(2, HasNullCast::class.java)
  }

  @Test
  fun `filters mutations to safe null calls`() {
    // conditional jump and aconst
    verifier.assertFiltersMutationAtNLocations(2, HasSafeNullCall::class.java)
  }

  @Test
  fun `filters mutations to elvis operator when bytecode gives ICONST_M1`() {
    verifier.assertFiltersMutationAtNLocations(2, HasElvisWithStringM1::class.java)
  }

  @Test
  fun `filters mutations to elvis operator when bytecode gives ICONST_0`() {
    verifier.assertFiltersMutationAtNLocations(2, HasElvisWithString0::class.java)
  }

  @Test
  fun `filters mutations to elvis operator when bytecode gives SIPUSH`() {
    verifier.assertFiltersMutationAtNLocations(2, HasElvisWithSIPUSH::class.java)
  }

  @Test
  fun `filters mutations to elvis operator when bytecode gives LDC`() {
    verifier.assertFiltersMutationAtNLocations(2, HasElvisWithLDC::class.java)
  }

  @Test
  fun `doesn't yet filter mutations to elvis operator when right hand side is a method call`() {
    verifier.assertFiltersMutationAtNLocations(0, HasElvisWithMethodCall::class.java)
  }

  @Test
  fun `filters safe casts` () {
    verifier.assertFiltersMutationAtNLocations(2, HasSafeCast::class.java)
  }
}


data class DestructureMe(val a: Int, val b: String, val c: Int)

class Destructure {
  fun foo(d: DestructureMe) {
    val (a, b, c) = d
    println("" + a + b + c)
  }
}

class NotDestructuring {
  fun foo(i: Int) {
    component1(i)
  }

  private fun component1(i: Int) {
    println("hello$i")
  }
}

class HasNullCast {
  fun foo(maybeS: String?) {
    val s = maybeS!!
    println(s)
  }
}

class HasSafeNullCall {
  fun foo(b: String?): String? {
    println(b?.length)
    return b
  }
}

class HasElvisWithStringM1 {
  fun foo(b: String?): Int? {
    val l = b?.length ?: -1
    println(l)
    return l
  }
}

class HasElvisWithString0 {
  fun foo(b: String?): Int? {
    val l = b?.length ?: 0
    println(l)
    return l
  }
}

class HasElvisWithSIPUSH {
  fun foo(b: String?): Int? {
    val l = b?.length ?: 10000
    println(l)
    return l
  }
}

class HasElvisWithLDC {
  fun foo(b: LongReturn?): Long? {
    val l = b?.getALong() ?: Long.MAX_VALUE
    println(l)
    return l
  }
}

class HasElvisWithMethodCall {
  fun foo(b: LongReturn?): Long? {
    val l = b?.getALong() ?: aLong()
    println(l)
    return l
  }

  fun aLong() : Long {
    return 42
  }
}

class HasSafeCast {
  fun foo(o : Any) : String? {
    return o as? String
  }
}

interface LongReturn {
  fun getALong() : Long
}