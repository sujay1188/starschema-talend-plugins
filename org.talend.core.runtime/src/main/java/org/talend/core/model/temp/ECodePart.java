// ============================================================================
//
// Copyright (C) 2006-2012 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.core.model.temp;

/**
 * Code parts are used by the Code Generator to generate differents components codes in a different order depending the
 * order of the components in the process.
 * 
 * $Id: ECodePart.java 38013 2010-03-05 14:21:59Z mhirt $
 * 
 */
public enum ECodePart {
    MAIN("main"), //$NON-NLS-1$
    BEGIN("begin"), //$NON-NLS-1$
    END("end"); //$NON-NLS-1$

    private ECodePart(String name) {
        this.name = name;
    }

    private String name;

    /**
     * Getter for name.
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     * 
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return getName();
    }

    public static ECodePart getCodePartByName(String name) {
        for (ECodePart part : values()) {
            if (part.getName().equals(name)) {
                return part;
            }
        }
        return null;
    }
}
