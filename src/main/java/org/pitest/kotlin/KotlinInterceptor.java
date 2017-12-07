package org.pitest.kotlin;

import java.util.Collection;
import java.util.regex.Pattern;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.pitest.bytecode.analysis.ClassTree;
import org.pitest.bytecode.analysis.MethodMatchers;
import org.pitest.bytecode.analysis.MethodTree;
import org.pitest.functional.F;
import org.pitest.functional.FCollection;
import org.pitest.functional.prelude.Prelude;
import org.pitest.mutationtest.build.InterceptorType;
import org.pitest.mutationtest.build.MutationInterceptor;
import org.pitest.mutationtest.engine.Mutater;
import org.pitest.mutationtest.engine.MutationDetails;

public class KotlinInterceptor implements MutationInterceptor {

  private static final Pattern componentPattern = Pattern.compile("component\\d");

  private ClassTree currentClass;
  private boolean isKotlinClass;

  @Override
  public Collection<MutationDetails> intercept(
      Collection<MutationDetails> mutations, Mutater m) {
    if(!isKotlinClass) {
      return mutations;
    }
    return FCollection.filter(mutations, Prelude.not(isKotlinJunkMutation(currentClass)));
  }
  
  @Override
  public InterceptorType type() {
    return InterceptorType.FILTER;
  }

  @Override
  public void begin(ClassTree clazz) {
    currentClass = clazz;
    isKotlinClass = clazz.annotations().contains(metaData());
  }

  @Override
  public void end() {
    currentClass = null;
  }
  
  private static F<MutationDetails, Boolean> isKotlinJunkMutation(final ClassTree currentClass) {
    return new  F<MutationDetails, Boolean>() {
      @Override
      public Boolean apply(MutationDetails a) {
        int instruction = a.getInstructionIndex();
        MethodTree method = currentClass.methods().findFirst(MethodMatchers.forLocation(a.getId().getLocation())).value();
        AbstractInsnNode mutatedInstruction = method.instructions().get(instruction);

        if (mutatedInstruction instanceof MethodInsnNode) {
          MethodInsnNode call = (MethodInsnNode) mutatedInstruction;
          if (takesNoArgs(call) && isComponentNCall(call)) {
            return true;
          }
        }

        return false;
      }
    };
  }

  private static boolean isComponentNCall(MethodInsnNode call) {
    return componentPattern.matcher(call.name).matches();
  }

  private static boolean takesNoArgs(MethodInsnNode call) {
    return call.desc.startsWith("()");
  }

  private static F<AnnotationNode, Boolean> metaData() {
    return new  F<AnnotationNode, Boolean>() {
      @Override
      public Boolean apply(AnnotationNode annotationNode) {
        return annotationNode.desc.equals("Lkotlin/Metadata;");
      }
    };
  };
}


