package org.pitest.kotlin;

import org.pitest.mutationtest.build.InterceptorParameters;
import org.pitest.mutationtest.build.MutationInterceptor;
import org.pitest.mutationtest.build.MutationInterceptorFactory;
import org.pitest.plugin.Feature;

public class KotlinInterceptorFactory implements MutationInterceptorFactory {
  @Override
  public MutationInterceptor createInterceptor(InterceptorParameters interceptorParameters) {
    return new KotlinInterceptor();
  }

  @Override
  public Feature provides() {
    return Feature.named("KOTLIN")
      .withOnByDefault(true)
      .withDescription("Improves support of kotlin language");
  }

  @Override
  public String description() {
    return "Kotlin language support";
  }
}
