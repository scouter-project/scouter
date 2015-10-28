/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
package scouter.client.xlog;

import org.eclipse.swt.SWT;

public enum ProfileTableEnum {
	PARENT_INDEX("Parent #", 30, SWT.CENTER, true, true, true), //
    INDEX("#", 30, SWT.CENTER, true, true, true), //
    TIME("Time", 70, SWT.RIGHT, true, true, true),
	TYPE("Type", 30, SWT.CENTER, true, true, false),
	CONTENTS("Contents", 200, SWT.LEFT, true, true, false),
	ELAPSED_TIME("Elapsed(ms)", 30, SWT.RIGHT, true, true, true),
	ERROR("Error", 50, SWT.LEFT, true, true, false);

    private final String title;
    private final int width;
    private final int alignment;
    private final boolean resizable;
    private final boolean moveable;
    private final boolean isNumber;

    private ProfileTableEnum(String text, int width, int alignment, boolean resizable, boolean moveable, boolean isNumber) {
        this.title = text;
        this.width = width;
        this.alignment = alignment;
        this.resizable = resizable;
        this.moveable = moveable;
        this.isNumber = isNumber;
    }
    
    public String getTitle(){
        return title;
    }

    public int getAlignment(){
        return alignment;
    }

    public boolean isResizable(){
        return resizable;
    }

    public boolean isMoveable(){
        return moveable;
    }

	public int getWidth() {
		return width;
	}
	
	public boolean isNumber() {
		return this.isNumber;
	}
}
