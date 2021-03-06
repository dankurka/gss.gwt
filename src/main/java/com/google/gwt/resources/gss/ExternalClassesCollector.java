/*
 * Copyright 2013 Julien Dramaix.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.resources.gss;

import com.google.common.css.compiler.ast.CssClassSelectorNode;
import com.google.common.css.compiler.ast.CssCompilerPass;
import com.google.common.css.compiler.ast.CssCompositeValueNode;
import com.google.common.css.compiler.ast.CssStringNode;
import com.google.common.css.compiler.ast.CssUnknownAtRuleNode;
import com.google.common.css.compiler.ast.CssValueNode;
import com.google.common.css.compiler.ast.DefaultTreeVisitor;
import com.google.common.css.compiler.ast.MutatingVisitController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class ExternalClassesCollector extends DefaultTreeVisitor implements CssCompilerPass {
  public static final String EXTERNAL_AT_RULE = "external";
  private static final String STAR_PREFIX = "*";

  private final MutatingVisitController visitController;

  private SortedSet<String> classNames;
  private Set<String> externalClassNames;
  private List<String> externalClassPrefixes;
  private boolean matchAll;

  public ExternalClassesCollector(MutatingVisitController visitController) {
    this.visitController = visitController;
  }

  @Override
  public void runPass() {
    externalClassNames = new HashSet<String>();
    externalClassPrefixes = new ArrayList<String>();
    classNames = new TreeSet<String>();

    visitController.startVisit(this);
  }

  @Override
  public void leaveUnknownAtRule(CssUnknownAtRuleNode node) {
    if (EXTERNAL_AT_RULE.equals(node.getName().getValue())) {
      if (!matchAll) {
        processParameters(node.getParameters());
      }
      visitController.removeCurrentNode();
    }
  }

  @Override
  public void leaveClassSelector(CssClassSelectorNode classSelector) {
    classNames.add(classSelector.getRefinerName());
  }

  public Set<String> getExternalClassNames() {
    if (matchAll) {
      return classNames;
    }

    for (String prefix : externalClassPrefixes) {
      for (String styleClass : classNames.tailSet(prefix)) {
        if (styleClass.startsWith(prefix)) {
          externalClassNames.add(styleClass);
        } else {
          break;
        }
      }
    }
    return externalClassNames;
  }

  private void processParameters(List<CssValueNode> values) {
    for (CssValueNode value : values) {
      if (value instanceof CssCompositeValueNode) {
        processParameters(((CssCompositeValueNode) value).getValues());
      } else if (value instanceof CssStringNode) {
        String selector = ((CssStringNode) value).getConcreteValue();
        if (STAR_PREFIX.equals(selector)) {
          matchAll = true;
        } else if (selector.endsWith(STAR_PREFIX)) {
          externalClassPrefixes.add(selector.substring(0, selector.length()-1));
        } else {
          externalClassNames.add(selector);
        }
      } else {
        externalClassNames.add(value.getValue());
      }
    }
  }
}
