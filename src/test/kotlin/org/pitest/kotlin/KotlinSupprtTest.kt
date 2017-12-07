package org.pitest.kotlin

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.Assert.fail;

import org.pitest.classpath.ClassloaderByteArraySource;
import org.pitest.mutationtest.build.intercept.javafeatures.FilterTester
import org.pitest.mutationtest.engine.gregor.mutators.IncrementsMutator
import org.pitest.mutationtest.build.InterceptorType

class KotlinSupportTest {

	  val testee = KotlinInterceptor()
	
    val source = ClassloaderByteArraySource.fromContext();
	  val verifier = FilterTester("", testee, IncrementsMutator.INCREMENTS_MUTATOR);    
  
    
    @Test
    fun `declares type as filter`() {
      assertThat(testee.type()).isEqualTo(InterceptorType.FILTER)
    }

}