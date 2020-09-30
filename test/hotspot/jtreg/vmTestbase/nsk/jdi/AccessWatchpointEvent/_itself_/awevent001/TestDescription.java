/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 */


/*
 * @test
 *
 * @summary converted from VM Testbase nsk/jdi/AccessWatchpointEvent/_itself_/awevent001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks that the JDI method
 *     com.sun.jdi.request.EventRequestManager.accessWatchpointRequests()
 *     properly returns all AccessWatchpointRequest objects when:
 *      - event requests are disabled;
 *      - event requests are enabled.
 * COMMENTS
 *     EventHandler was added as workaround for the bug 4430096.
 *     This prevents the target VM from potential hangup.
 *     The test was updated due to the bug 4455653.
 *     --------------------------------------------
 *     To fix the bug 4509034,
 *     the check if
 *                  vm.canWatchFieldAccess() == true
 *     is added to the beginning of the test.
 *     -------------------------------------
 *     To fix the RFE 4615225,
 *     the test suite has been moved here.
 *     -------------------------------------
 *     Test updated to prevent possible VMDisconnectedException on VMDeathEvent:
 *     - quit on VMDeathEvent added to events handling loop
 *     Fixed according to 6290118 bug.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.AccessWatchpointEvent._itself_.awevent001
 *        nsk.jdi.AccessWatchpointEvent._itself_.awevent001t
 *
 * @comment make sure awevent001t is compiled with full debug info
 * @clean nsk.jdi.AccessWatchpointEvent._itself_.awevent001t
 * @compile -g:lines,source,vars ../awevent001t.java
 *
 * @run main/othervm
 *      nsk.jdi.AccessWatchpointEvent._itself_.awevent001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

