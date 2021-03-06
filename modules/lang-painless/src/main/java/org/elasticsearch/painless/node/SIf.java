/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.painless.node;

import org.elasticsearch.painless.Location;
import org.elasticsearch.painless.phase.UserTreeVisitor;
import org.elasticsearch.painless.symbol.Decorations.AnyBreak;
import org.elasticsearch.painless.symbol.Decorations.AnyContinue;
import org.elasticsearch.painless.symbol.Decorations.InLoop;
import org.elasticsearch.painless.symbol.Decorations.LastLoop;
import org.elasticsearch.painless.symbol.Decorations.LastSource;
import org.elasticsearch.painless.symbol.Decorations.Read;
import org.elasticsearch.painless.symbol.Decorations.TargetType;
import org.elasticsearch.painless.symbol.SemanticScope;

import java.util.Objects;

/**
 * Represents an if block.
 */
public class SIf extends AStatement {

    private final AExpression conditionNode;
    private final SBlock ifblockNode;

    public SIf(int identifier, Location location, AExpression conditionNode, SBlock ifblockNode) {
        super(identifier, location);

        this.conditionNode = Objects.requireNonNull(conditionNode);
        this.ifblockNode = ifblockNode;
    }

    public AExpression getConditionNode() {
        return conditionNode;
    }

    public SBlock getIfblockNode() {
        return ifblockNode;
    }

    @Override
    public <Scope> void visit(UserTreeVisitor<Scope> userTreeVisitor, Scope scope) {
        userTreeVisitor.visitIf(this, scope);
    }

    @Override
    public <Scope> void visitChildren(UserTreeVisitor<Scope> userTreeVisitor, Scope scope) {
        conditionNode.visit(userTreeVisitor, scope);

        if (ifblockNode != null) {
            ifblockNode.visit(userTreeVisitor, scope);
        }
    }

    @Override
    void analyze(SemanticScope semanticScope) {
        semanticScope.setCondition(conditionNode, Read.class);
        semanticScope.putDecoration(conditionNode, new TargetType(boolean.class));
        AExpression.analyze(conditionNode, semanticScope);
        conditionNode.cast(semanticScope);

        if (conditionNode instanceof EBooleanConstant) {
            throw createError(new IllegalArgumentException("Extraneous if statement."));
        }

        if (ifblockNode == null) {
            throw createError(new IllegalArgumentException("Extraneous if statement."));
        }

        semanticScope.replicateCondition(this, ifblockNode, LastSource.class);
        semanticScope.replicateCondition(this, ifblockNode, InLoop.class);
        semanticScope.replicateCondition(this, ifblockNode, LastLoop.class);
        ifblockNode.analyze(semanticScope.newLocalScope());
        semanticScope.replicateCondition(ifblockNode, this, AnyContinue.class);
        semanticScope.replicateCondition(ifblockNode, this, AnyBreak.class);
    }
}
