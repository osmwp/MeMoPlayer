/*
 * Copyright (C) 2009 The J2AB Project
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

package javax.microedition.pim;

public class PIMException extends Exception {

    public static final int FEATURE_NOT_SUPPORTED = 1;
    public static final int GENERAL_ERROR = 2;
    public static final int LIST_CLOSED = 3;
    public static final int LIST_NOT_ACCESSIBLE = 4;
    public static final int MAX_CATEGORIES_EXCEEDED = 5;
    public static final int UNSUPPORTED_VERSION = 6;
    public static final int UPDATE_ERROR = 7;

    private int reason;

    public PIMException(String message) {
        this (message, GENERAL_ERROR);
    }

    public PIMException(String message, int reason) {
        super (message);
        this.reason = reason;
    }

    public int getReason () {
        return this.reason;
    }
}
