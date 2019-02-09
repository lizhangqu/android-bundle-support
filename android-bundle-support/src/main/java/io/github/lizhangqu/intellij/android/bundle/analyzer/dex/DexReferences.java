/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.lizhangqu.intellij.android.bundle.analyzer.dex;

import com.android.annotations.NonNull;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.*;

import io.github.lizhangqu.intellij.android.bundle.analyzer.dex.tree.DexElementNodeFactory;
import org.jf.dexlib2.dexbacked.*;
import org.jf.dexlib2.dexbacked.reference.DexBackedFieldReference;
import org.jf.dexlib2.dexbacked.reference.DexBackedMethodReference;
import org.jf.dexlib2.dexbacked.reference.DexBackedTypeReference;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.instruction.DualReferenceInstruction;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.iface.reference.TypeReference;
import org.jf.dexlib2.iface.value.*;
import org.jf.dexlib2.immutable.reference.*;

public class DexReferences {

    private final Multimap<Reference, ImmutableReference> referenceReferences =
            HashMultimap.create();

    public DexReferences(DexBackedDexFile[] files) {
        gatherBackReferences(files);
    }

    /**
     * Goes through all the classes, methods, and fields, and gathers all possible references from
     * one type/method/field to another.
     *
     * @param files dex file
     */
    private void gatherBackReferences(@NonNull DexBackedDexFile[] files) {

        Map<Reference, ImmutableReference> immutableReferencesBin = new HashMap<>();

        for (DexBackedDexFile file : files) {
            //build a map from class names (String) to actual TypeReferences,
            //as this information is not readily available to query through
            //the dexlib2 API.
            Map<String, ImmutableTypeReference> typesByName = new HashMap<>();
            for (int i = 0, m = file.getTypeCount(); i < m; i++) {
                ImmutableTypeReference immutableTypeRef =
                        ImmutableTypeReference.of(new DexBackedTypeReference(file, i));
                typesByName.put(immutableTypeRef.getType(), immutableTypeRef);
            }

            //loop through all methods referenced in the dex file, mapping the following:
            for (int i = 0, m = file.getMethodCount(); i < m; i++) {
                MethodReference methodReference = new DexBackedMethodReference(file, i);
                //- return type => method
                ImmutableReference typeRef = typesByName.get(methodReference.getReturnType());
                addReference(typeRef, methodReference, immutableReferencesBin);
                //- all parameter types => method
                for (CharSequence parameterType : methodReference.getParameterTypes()) {
                    typeRef = typesByName.get(parameterType.toString());
                    addReference(typeRef, methodReference, immutableReferencesBin);
                }
            }

            //loop through all classes defined in the dex file, mapping the following:
            for (DexBackedClassDef classDef : file.getClasses()) {
                //- superclass => class
                ImmutableReference typeRef = typesByName.get(classDef.getSuperclass());
                addReference(typeRef, classDef, immutableReferencesBin);
                //- all implemented interfaces => class
                for (String iface : classDef.getInterfaces()) {
                    typeRef = typesByName.get(iface);
                    addReference(typeRef, classDef, immutableReferencesBin);
                }
                //map annotations => class
                for (Annotation annotation : classDef.getAnnotations()) {
                    addAnnotation(immutableReferencesBin, typesByName, classDef, annotation);
                }
                //loop through all the methods defined in this class
                for (DexBackedMethod method : classDef.getMethods()) {
                    //if the method has an implementation, loop through the bytecode
                    //mapping any references that exist in dex instructions to the method.
                    //Fortunately, dexlib2 marks every bytecode instruction that accepts
                    //a reference with one or two interfaces: ReferenceInstruction
                    //and DualReferenceInstruction.
                    DexBackedMethodImplementation impl = method.getImplementation();
                    if (impl != null) {
                        for (Instruction instruction : impl.getInstructions()) {
                            if (instruction instanceof ReferenceInstruction) {
                                Reference reference =
                                        ((ReferenceInstruction) instruction).getReference();
                                addReferenceAndEnclosingClass(
                                        immutableReferencesBin, typesByName, method, reference);
                            }
                            if (instruction instanceof DualReferenceInstruction) {
                                Reference reference =
                                        ((DualReferenceInstruction) instruction).getReference2();
                                addReferenceAndEnclosingClass(
                                        immutableReferencesBin, typesByName, method, reference);
                            }
                        }
                    }
                    //map annotations => method
                    for (Annotation annotation : method.getAnnotations()) {
                        addAnnotation(immutableReferencesBin, typesByName, method, annotation);
                    }
                }
                for (DexBackedField field : classDef.getFields()) {
                    //map annotations => field
                    for (Annotation annotation : field.getAnnotations()) {
                        addAnnotation(immutableReferencesBin, typesByName, field, annotation);
                    }
                }
            }

            //loop through all fields referenced in this dex file, creating
            //a mapping from the field type => field
            for (int i = 0, m = file.getFieldCount(); i < m; i++) {
                FieldReference fieldRef = new DexBackedFieldReference(file, i);
                ImmutableReference typeRef = typesByName.get(fieldRef.getType());
                addReference(typeRef, fieldRef, immutableReferencesBin);
            }
        }
    }

    private void addAnnotation(
            Map<Reference, ImmutableReference> immutableReferencesBin,
            Map<String, ImmutableTypeReference> typesByName,
            Reference ref,
            Annotation annotation) {
        ImmutableReference typeRef = typesByName.get(annotation.getType());
        addReference(typeRef, ref, immutableReferencesBin);
        Set<? extends AnnotationElement> elements = annotation.getElements();
        for (AnnotationElement element : elements) {
            EncodedValue value = element.getValue();
            addEncodedValue(immutableReferencesBin, typesByName, ref, value);
        }
    }

    private void addEncodedValue(
            Map<Reference, ImmutableReference> immutableReferencesBin,
            Map<String, ImmutableTypeReference> typesByName,
            Reference ref,
            EncodedValue value) {
        if (value instanceof AnnotationEncodedValue) {
            ImmutableTypeReference typeRef =
                    typesByName.get(((AnnotationEncodedValue) value).getType());
            addReference(typeRef, ref, immutableReferencesBin);
            for (AnnotationElement element : ((AnnotationEncodedValue) value).getElements()) {
                addEncodedValue(immutableReferencesBin, typesByName, ref, element.getValue());
            }
        } else if (value instanceof ArrayEncodedValue) {
            for (EncodedValue encodedValue : ((ArrayEncodedValue) value).getValue()) {
                addEncodedValue(immutableReferencesBin, typesByName, ref, encodedValue);
            }
        } else if (value instanceof EnumEncodedValue) {
            addReferenceAndEnclosingClass(
                    immutableReferencesBin,
                    typesByName,
                    ref,
                    ((EnumEncodedValue) value).getValue());
        } else if (value instanceof TypeEncodedValue) {
            ImmutableTypeReference typeRef = typesByName.get(((TypeEncodedValue) value).getValue());
            addReference(typeRef, ref, immutableReferencesBin);
        }
    }

    private void addReferenceAndEnclosingClass(
            Map<Reference, ImmutableReference> immutableReferencesBin,
            Map<String, ImmutableTypeReference> typesByName,
            Reference ref,
            Reference memberReference) {
        addReference(memberReference, ref, immutableReferencesBin);

        //also map enclosing class of referenced method/field => this reference
        if (memberReference instanceof MethodReference) {
            addReference(
                    typesByName.get(((MethodReference) memberReference).getDefiningClass()),
                    ref,
                    immutableReferencesBin);
        } else if (memberReference instanceof FieldReference) {
            addReference(
                    typesByName.get(((FieldReference) memberReference).getDefiningClass()),
                    ref,
                    immutableReferencesBin);
        }
    }

    //we want to reuse immutable references, not keep creating them
    //use the map as a bag of ImmutableReferences for reuse
    private void addReference(
            Reference ref1,
            Reference ref2,
            Map<Reference, ImmutableReference> immutableReferencesBin) {
        ImmutableReference immutableRef1 = immutableReferencesBin.get(ref1);
        if (immutableRef1 == null) {
            immutableRef1 = ImmutableReferenceFactory.of(ref1);
            immutableReferencesBin.put(immutableRef1, immutableRef1);
        }

        ImmutableReference immutableRef2 = immutableReferencesBin.get(ref2);
        if (immutableRef2 == null) {
            immutableRef2 = ImmutableReferenceFactory.of(ref2);
            immutableReferencesBin.put(immutableRef2, immutableRef2);
        }

        if (immutableRef1 instanceof TypeReference) {
            String definingType2 = null;
            if (immutableRef2 instanceof MethodReference) {
                definingType2 = ((MethodReference) immutableRef2).getDefiningClass();
            } else if (immutableRef2 instanceof FieldReference) {
                definingType2 = ((FieldReference) immutableRef2).getDefiningClass();
            }
            //we don't want to map a class => member of that class
            //as it only creates noise
            if (((TypeReference) immutableRef1).getType().equals(definingType2)) {
                return;
            }
        }

        referenceReferences.put(immutableRef1, immutableRef2);
    }

    /**
     * Build a full reference tree for the given DEX reference.
     *
     * @param referenced the dex element you wish to find references for
     * @return the root of the reference tree
     */
    public io.github.lizhangqu.intellij.android.bundle.analyzer.dex.tree.DexElementNode getReferenceTreeFor(@NonNull Reference referenced) {
        return getReferenceTreeFor(referenced, false);
    }

    /**
     * Build a reference tree for the given DEX reference. Depending on the {@code shallow}
     * parameter this returns the full tree root or just the root with first level references
     * evaluated.
     *
     * <p>You can then lazy load deeper references using {@link #addReferencesForNode(io.github.lizhangqu.intellij.android.bundle.analyzer.dex.tree.DexElementNode
     * node, Reference referenced, boolean shallow)}.
     *
     * <p>To check if references were already loaded for a node use {@link
     * #isAlreadyLoaded(io.github.lizhangqu.intellij.android.bundle.analyzer.dex.tree.DexElementNode)}
     *
     * @param referenced the dex element you wish to find references for
     * @param shallow false to to build the full tree, true to evaluate just the first level
     * @return the root of the reference tree
     */
    public io.github.lizhangqu.intellij.android.bundle.analyzer.dex.tree.DexElementNode getReferenceTreeFor(@NonNull Reference referenced, boolean shallow) {
        io.github.lizhangqu.intellij.android.bundle.analyzer.dex.tree.DexElementNode rootNode =
                io.github.lizhangqu.intellij.android.bundle.analyzer.dex.tree.DexElementNodeFactory.from(ImmutableReferenceFactory.of(referenced));
        addReferencesForNode(rootNode, shallow);
        rootNode.sort(NODE_COMPARATOR);
        return rootNode;
    }

    /**
     * Finds references to {@code referenced} and attaches them as tree nodes under {@code node}
     *
     * <p>Depending on the {@code shallow} parameter this attaches the full reference tree or stops
     * evaluation at the first level
     *
     * @param node the root node under which you wish to attach references
     * @param shallow false to to build the full tree, true to evaluate just the first level
     */
    public void addReferencesForNode(@NonNull io.github.lizhangqu.intellij.android.bundle.analyzer.dex.tree.DexElementNode node, boolean shallow) {
        Reference referenced = node.getReference();
        node.removeAllChildren();
        Collection<? extends ImmutableReference> references = referenceReferences.get(referenced);
        for (ImmutableReference ref : references) {
            if (ref instanceof MethodReference
                    || ref instanceof TypeReference
                    || ref instanceof FieldReference) {
                io.github.lizhangqu.intellij.android.bundle.analyzer.dex.tree.DexElementNode parentNode = node;
                boolean hasCycle = false;
                while (parentNode != null) {
                    if (ref.equals(parentNode.getReference())) {
                        hasCycle = true;
                    }
                    parentNode = parentNode.getParent();
                }
                if (hasCycle) {
                    continue;
                }
                io.github.lizhangqu.intellij.android.bundle.analyzer.dex.tree.DexElementNode newNode = DexElementNodeFactory.from(ref);
                node.setAllowsChildren(true);
                node.add(newNode);
                if (!shallow) {
                    addReferencesForNode(newNode, false);
                } else {
                    newNode.setAllowsChildren(true);
                    //DexPackageNodes are never normally used in a reference tree
                    //so we'll use them as a sentinel to mark nodes
                    //that haven't been resolved yet (for lazy loading)
                    newNode.add(new io.github.lizhangqu.intellij.android.bundle.analyzer.dex.tree.DexPackageNode("", null));
                }
            }
        }
    }

    /**
     * Checks if the specified dex reference tree node has had its references evaluated and attached
     * (if any) or if it still needs to be passed to {@link #addReferencesForNode(io.github.lizhangqu.intellij.android.bundle.analyzer.dex.tree.DexElementNode,
     * Reference, boolean)} for lazy evaluation
     *
     * @param node the reference tree node to check
     * @return true if this node is already evaluated
     */
    public static boolean isAlreadyLoaded(io.github.lizhangqu.intellij.android.bundle.analyzer.dex.tree.DexElementNode node) {
        //as per the comment in addReferencesForNode:
        //we're checking if the node contains a sentinel (single DexPackageNode child)
        return !(node.getChildCount() == 1 && node.getFirstChild() instanceof io.github.lizhangqu.intellij.android.bundle.analyzer.dex.tree.DexPackageNode);
    }

    public static final Comparator<io.github.lizhangqu.intellij.android.bundle.analyzer.dex.tree.DexElementNode> NODE_COMPARATOR =
            Comparator.comparing(
                    o -> {
                        if (o instanceof io.github.lizhangqu.intellij.android.bundle.analyzer.dex.tree.DexClassNode) {
                            return o.getName();
                        } else if (o instanceof io.github.lizhangqu.intellij.android.bundle.analyzer.dex.tree.DexMethodNode) {
                            assert o.getReference() != null;
                            return ((MethodReference) o.getReference()).getDefiningClass()
                                    + " "
                                    + o.getName();
                        } else if (o instanceof io.github.lizhangqu.intellij.android.bundle.analyzer.dex.tree.DexFieldNode) {
                            assert o.getReference() != null;
                            return ((FieldReference) o.getReference()).getDefiningClass()
                                    + " "
                                    + o.getName();
                        }
                        return "";
                    });
}
