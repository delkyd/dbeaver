/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.db2.model.dict;

import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterType;

/**
 * DB2 Routine Rowtype
 * 
 * @author Denis Forveille
 * 
 */
public enum DB2RoutineRowType {
   B("B (Both input and output parameter )", DBSProcedureParameterType.INOUT),

   C("C (Result after casting)", DBSProcedureParameterType.RETURN),

   O("O (Output parameter)", DBSProcedureParameterType.OUT),

   P("P (Input parameter)", DBSProcedureParameterType.IN),

   R("F (Result before casting)", DBSProcedureParameterType.RETURN);

   private String                    description;
   private DBSProcedureParameterType parameterType;

   // -----------
   // Constructor
   // -----------

   private DB2RoutineRowType(String description, DBSProcedureParameterType parameterType) {
      this.description = description;
      this.parameterType = parameterType;
   }

   // ----------------
   // Standard Getters
   // ----------------

   public String getDescription() {
      return description;
   }

   public DBSProcedureParameterType getParameterType() {
      return parameterType;
   }

}