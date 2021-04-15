/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

#ifndef SHARE_GC_G1_G1FULLCOLLECTOR_INLINE_HPP
#define SHARE_GC_G1_G1FULLCOLLECTOR_INLINE_HPP

#include "gc/g1/g1FullCollector.hpp"
#include "gc/g1/g1FullGCHeapRegionAttr.hpp"
#include "oops/oopsHierarchy.hpp"


bool G1FullCollector::is_compacted(oop obj) const {
  return _region_attr_table.is_compacted(cast_from_oop<HeapWord*>(obj));
}

bool G1FullCollector::is_compacted_or_skip_marking(uint region_index) const {
  return _region_attr_table.is_compacted_or_skip_marking(region_index);
}

bool G1FullCollector::is_skip_marking(oop obj) const {
  return _region_attr_table.is_skip_marking(cast_from_oop<HeapWord*>(obj));
}

#endif // SHARE_GC_G1_G1FULLCOLLECTOR_INLINE_HPP

