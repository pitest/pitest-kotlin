package org.pitest.kotlin;

import java.util.Collection;
import java.util.regex.Pattern;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.pitest.bytecode.analysis.ClassTree;
import org.pitest.bytecode.analysis.MethodMatchers;
import org.pitest.bytecode.analysis.MethodTree;
import org.pitest.classinfo.ClassName;
import org.pitest.functional.F;
import org.pitest.functional.FCollection;
import org.pitest.functional.prelude.Prelude;
import org.pitest.mutationtest.build.InterceptorType;
import org.pitest.mutationtest.build.MutationInterceptor;
import org.pitest.mutationtest.engine.Mutater;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.sequence.*;

import static org.pitest.bytecode.analysis.InstructionMatchers.*;
import static org.pitest.bytecode.analysis.InstructionMatchers.anIntegerConstant;

public class KotlinInterceptor implements MutationInterceptor {

  private ClassTree currentClass;
  private boolean isKotlinClass;


  private static final boolean DEBUG = true;
  
  private static final Match<AbstractInsnNode> IGNORE = isA(LineNumberNode.class)
    .or(isA(FrameNode.class)
    );

  private static final Slot<AbstractInsnNode> MUTATED_INSTRUCTION = Slot.create(AbstractInsnNode.class);
  private static final Slot<Boolean> FOUND = Slot.create(Boolean.class);

  static final SequenceMatcher<AbstractInsnNode> KOTLIN_JUNK = QueryStart
    .match(Match.<AbstractInsnNode>never())
    .zeroOrMore(QueryStart.match(anyInstruction()))
    .or(destructuringCall())
    .or(nullCast())
    .or(safeNullCallOrElvis())
    .or(safeCast())
    .then(containMutation(FOUND))
    .zeroOrMore(QueryStart.match(anyInstruction()))
    .compile(QueryParams.params(AbstractInsnNode.class)
      .withIgnores(IGNORE)
      .withDebug(DEBUG)
    );

  private static SequenceQuery<AbstractInsnNode> nullCast() {
    return QueryStart
      .any(AbstractInsnNode.class)
      .then(opCode(Opcodes.IFNONNULL).and(mutationPoint()))
      .then(methodCallTo(ClassName.fromString("kotlin/jvm/internal/Intrinsics"), "throwNpe").and(mutationPoint()));
  }

  private static SequenceQuery<AbstractInsnNode> safeCast() {
    Slot<LabelNode> nullJump = Slot.create(LabelNode.class);
    return QueryStart
      .any(AbstractInsnNode.class)
      .then(opCode(Opcodes.INSTANCEOF).and(mutationPoint()))
      .then(opCode(Opcodes.IFNE).and(jumpsTo(nullJump.write()).and(mutationPoint())))
      .then(opCode(Opcodes.POP))
      .then(opCode(Opcodes.ACONST_NULL))
      .then(labelNode(nullJump.read()));
  }


  private static SequenceQuery<AbstractInsnNode> destructuringCall() {
    return QueryStart
      .any(AbstractInsnNode.class)
      .then(aComponentNCall().and(mutationPoint()));
  }

  private static SequenceQuery<AbstractInsnNode> safeNullCallOrElvis() {
    Slot<LabelNode> nullJump = Slot.create(LabelNode.class);
    return QueryStart
      .any(AbstractInsnNode.class)
      .zeroOrMore(QueryStart.match(anyInstruction()))
      .then(opCode(Opcodes.IFNULL).and(jumpsTo(nullJump.write())).and(mutationPoint()))
      .oneOrMore(QueryStart.match(anyInstruction()))
      .then(opCode(Opcodes.GOTO))
      .then(labelNode(nullJump.read()))
      .then(opCode(Opcodes.POP))
      .then(aConstant().and(mutationPoint()));
  }

  private static Match<AbstractInsnNode> aConstant() {
    return opCode(Opcodes.ACONST_NULL).or(anIntegerConstant().or(opCode(Opcodes.SIPUSH)).or(opCode(Opcodes.LDC)));
  }

  private static Match<AbstractInsnNode> aComponentNCall() {
    final Pattern componentPattern = Pattern.compile("component\\d");
    return new Match<AbstractInsnNode>() {
      @Override
      public boolean test(Context<AbstractInsnNode> c, AbstractInsnNode abstractInsnNode) {
        if (abstractInsnNode instanceof MethodInsnNode) {
          MethodInsnNode call = (MethodInsnNode) abstractInsnNode;
          return isDestructuringCall(call) && takesNoArgs(call);
        }
        return false;
      }

      private boolean isDestructuringCall(MethodInsnNode call) {
        return takesNoArgs(call) && isComponentNCall(call);
      }

      private boolean isComponentNCall(MethodInsnNode call) {
        return componentPattern.matcher(call.name).matches();
      }

      private boolean takesNoArgs(MethodInsnNode call) {
        return call.desc.startsWith("()");
      }
    };
  }

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
        Context<AbstractInsnNode> context = Context.start(method.instructions(), DEBUG);
        context.store(MUTATED_INSTRUCTION.write(), mutatedInstruction);
        return KOTLIN_JUNK.matches(method.instructions(), context);
      }
    };
  }

  private static F<AnnotationNode, Boolean> metaData() {
    return new  F<AnnotationNode, Boolean>() {
      @Override
      public Boolean apply(AnnotationNode annotationNode) {
        return annotationNode.desc.equals("Lkotlin/Metadata;");
      }
    };
  };

  private static Match<AbstractInsnNode> mutationPoint() {
    return recordTarget(MUTATED_INSTRUCTION.read(), FOUND.write());
  }

  private static Match<AbstractInsnNode> containMutation(final Slot<Boolean> found) {
    return new Match<AbstractInsnNode>() {
      @Override
      public boolean test(Context<AbstractInsnNode> c, AbstractInsnNode t) {
        return c.retrieve(found.read()).hasSome();
      }
    };
  }
}


