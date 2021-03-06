/*******************************************************************************
 * Copyright (c) 2014 antoniomariasanchez at gmail.com.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     antoniomaria - initial API and implementation
 ******************************************************************************/
package net.sf.gazpachoquest.types;

public enum ResearchAccessType {
    OPEN_ACCESS("O"), // Open Access. Allow anyone to take the
    // questionnairDefinition.
    BY_INVITATION("P"); // By PersonalInvitation Only.

    private String code;

    ResearchAccessType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ResearchAccessType fromCode(String code) {
        for (ResearchAccessType status : ResearchAccessType.values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Type " + code.toString() + " not supported");
    }
}
