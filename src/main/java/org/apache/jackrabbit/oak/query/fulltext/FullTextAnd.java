/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.jackrabbit.oak.query.fulltext;

import java.util.ArrayList;

/**
 * A fulltext "and" condition.
 */
public class FullTextAnd extends FullTextExpression {
    
    public ArrayList<FullTextExpression> list = new ArrayList<FullTextExpression>();

    @Override
    public boolean evaluate(String value) {
        for (FullTextExpression e : list) {
            if (!e.evaluate(value)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public FullTextExpression simplify() {
        return list.size() == 1 ? list.get(0) : this;
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder();
        int i = 0;
        for (FullTextExpression e : list) {
            if (i++ > 0) {
                buff.append(' ');
            }
            if (e.getPrecedence() < getPrecedence()) {
                buff.append('(');
            }                
            buff.append(e.toString());
            if (e.getPrecedence() < getPrecedence()) {
                buff.append(')');
            }
        }
        return buff.toString();
    }
    
    @Override
    public int getPrecedence() {
        return PRECEDENCE_AND;
    }

}