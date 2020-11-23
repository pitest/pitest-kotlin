package org.pitest.kotlin;

import static org.pitest.bytecode.analysis.InstructionMatchers.anIntegerConstant;
import static org.pitest.bytecode.analysis.InstructionMatchers.anyInstruction;
import static org.pitest.bytecode.analysis.InstructionMatchers.jumpsTo;
import static org.pitest.bytecode.analysis.InstructionMatchers.labelNode;
import static org.pitest.bytecode.analysis.InstructionMatchers.methodCallTo;
import static org.pitest.bytecode.analysis.InstructionMatchers.notAnInstruction;
import static org.pitest.bytecode.analysis.InstructionMatchers.opCode;
import static org.pitest.bytecode.analysis.InstructionMatchers.recordTarget;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.pitest.bytecode.analysis.ClassTree;
import org.pitest.bytecode.analysis.MethodMatchers;
import org.pitest.bytecode.analysis.MethodTree;
import org.pitest.classinfo.ClassName;
import org.pitest.functional.FCollection;
import org.pitest.mutationtest.build.InterceptorType;
import org.pitest.mutationtest.build.MutationInterceptor;
import org.pitest.mutationtest.engine.Mutater;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.sequence.Context;
import org.pitest.sequence.Match;
import org.pitest.sequence.QueryParams;
import org.pitest.sequence.QueryStart;
import org.pitest.sequence.SequenceMatcher;
import org.pitest.sequence.SequenceQuery;
import org.pitest.sequence.Slot;

public class KotlinInterceptor implements MutationInterceptor {

  private ClassTree currentClass;
  private boolean isKotlinClass;

  private static final boolean DEBUG = false;
  
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
      .withIgnores(notAnInstruction())
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
    return FCollection.filter(mutations, isKotlinJunkMutation(currentClass).negate());
  }
  
  @Override
  public InterceptorType type() {
    return InterceptorType.FILTER;
  }

  @Override
  public void begin(ClassTree clazz) {
    currentClass = clazz;
    isKotlinClass = clazz.annotations().stream()
        .filter(annotationNode -> annotationNode.desc.equals("Lkotlin/Metadata;"))
        .findFirst()
        .isPresent();
  }

  @Override
  public void end() {
    currentClass = null;
  }
  
  private static Predicate<MutationDetails> isKotlinJunkMutation(final ClassTree currentClass) {
    return a -> {
        int instruction = a.getInstructionIndex();
        MethodTree method = currentClass.methods().stream()
            .filter(MethodMatchers.forLocation(a.getId().getLocation()))
            .findFirst()
            .get();
        AbstractInsnNode mutatedInstruction = method.instruction(instruction);
        Context<AbstractInsnNode> context = Context.start(method.instructions(), DEBUG);
        context.store(MUTATED_INSTRUCTION.write(), mutatedInstruction);
        return KOTLIN_JUNK.matches(method.instructions(), context);
    };
  }

  private static Match<AbstractInsnNode> mutationPoint() {
    return recordTarget(MUTATED_INSTRUCTION.read(), FOUND.write());
  }

  private static Match<AbstractInsnNode> containMutation(final Slot<Boolean> found) {
    return (context, node) ->  context.retrieve(found.read()).isPresent();
  }
}


