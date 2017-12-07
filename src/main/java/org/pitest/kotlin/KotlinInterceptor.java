package org.pitest.kotlin;

import java.util.Collection;

import org.pitest.bytecode.analysis.ClassTree;
import org.pitest.functional.F;
import org.pitest.functional.FCollection;
import org.pitest.functional.prelude.Prelude;
import org.pitest.mutationtest.build.InterceptorType;
import org.pitest.mutationtest.build.MutationInterceptor;
import org.pitest.mutationtest.engine.Mutater;
import org.pitest.mutationtest.engine.MutationDetails;

public class KotlinInterceptor implements MutationInterceptor {


  @Override
  public Collection<MutationDetails> intercept(
      Collection<MutationDetails> mutations, Mutater m) {
    return mutations; //FCollection.filter(mutations, Prelude.not(isKotlinJunkMutation()));
  }
  
  @Override
  public InterceptorType type() {
    return InterceptorType.FILTER;
  }

  @Override
  public void begin(ClassTree clazz) {
    // noop  
  }

  @Override
  public void end() {
    // noop   
  }
  
  private static F<MutationDetails, Boolean> isKotlinJunkMutation() {
    return new  F<MutationDetails, Boolean>() {
      @Override
      public Boolean apply(MutationDetails a) {
        return a.getFilename().toLowerCase().endsWith(".kt") && a.getLineNumber() == 0;
      }
    };
  }
}


